import {
  APP_NAME,
  CURRENCY_OPTIONS,
  FALLBACK_OFFERS,
  FEATURE_BULLETS,
  FREE_JOB_LIMIT,
  INVOICE_STATUSES,
  PRICING_MODES,
  TABS,
  THEME_MODES,
  createBillingState,
  createDashboardSnapshot,
  createDefaultDraft,
  createTabMeta,
  currencyPickerLabel,
  displayCurrencyLabel,
  enrichJob,
  formatCurrency,
  formatHours,
  formatShortDate,
  getRemainingFreeEntries,
  hasMoneyValue,
  isEditingDraft,
  jobToDraft,
  nextJobId,
  previewDraft,
  sortJobsForHistory,
  sortJobsForStorage,
  validateDraft,
} from "./lib/logic.js";
import {
  loadInstallPromptDismissed,
  loadJobs,
  loadSettings,
  saveInstallPromptDismissed,
  saveJobs,
  saveSettings,
} from "./lib/store.js";
import { downloadInvoicePdf } from "./lib/pdf.js";
import { createReminderEngine } from "./lib/reminders.js";

const appElement = document.querySelector("#app");
const logoInput = document.createElement("input");
logoInput.type = "file";
logoInput.accept = "image/png,image/jpeg,image/webp";
logoInput.hidden = true;
document.body.appendChild(logoInput);

const reminderEngine = createReminderEngine({
  onToast: (message) => showToast(message),
});

const state = {
  jobs: sortJobsForStorage(loadJobs()),
  settings: loadSettings(),
  billing: createBillingState(),
  selectedTab: TABS.DASHBOARD,
  draft: createDefaultDraft(),
  formUi: deriveFormUi(createDefaultDraft()),
  serverConfig: {},
  toastMessage: "",
  installPromptEvent: null,
  installPromptDismissed: loadInstallPromptDismissed(),
  renderQueued: false,
  toastTimer: null,
};

function deriveFormUi(draft) {
  return {
    showJobDetails: Boolean(draft.siteAddress || draft.workSummary),
    showMoreAmounts:
      hasMoneyValue(draft.extraChargeText) ||
      hasMoneyValue(draft.materialsCostText) ||
      hasMoneyValue(draft.travelCostText),
    showDueDate: Boolean(draft.dueDateText),
    showReminder: Boolean(draft.reminderDateText || draft.reminderNote),
  };
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function showToast(message) {
  state.toastMessage = message;
  queueRender();
  if (state.toastTimer) {
    clearTimeout(state.toastTimer);
  }
  state.toastTimer = setTimeout(() => {
    state.toastMessage = "";
    queueRender();
  }, 3600);
}

function persistSettings() {
  saveSettings(state.settings);
}

function setBillingCustomerId(customerId) {
  state.settings = {
    ...state.settings,
    billingCustomerId: customerId || null,
  };
  persistSettings();
}

function persistJobs() {
  saveJobs(state.jobs);
  reminderEngine.sync(state.jobs);
}

async function postJson(url, payload) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify(payload),
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || data.message || "Request failed.");
  }
  return data;
}

function billingStateFromSnapshot(snapshot = {}, fallbackMessage = null) {
  const baseState = createBillingState(state.serverConfig);
  return {
    ...baseState,
    isLoading: false,
    isConnected: baseState.checkoutEnabled || baseState.isConnected,
    isPro: Boolean(snapshot.active ?? baseState.isPro),
    verificationSource: snapshot.source || baseState.verificationSource,
    verifiedExpiryTime: snapshot.currentPeriodEnd || null,
    statusMessage:
      snapshot.message ||
      fallbackMessage ||
      (Boolean(snapshot.active) ? "Pro verified by Stripe." : baseState.statusMessage),
  };
}

function normalizeUrlState() {
  const url = new URL(window.location.href);
  const requestedTab = url.searchParams.get("tab");
  if (requestedTab && Object.values(TABS).includes(requestedTab)) {
    state.selectedTab = requestedTab;
  }
  return url;
}

function clearCheckoutQueryState(url) {
  url.searchParams.delete("checkout");
  url.searchParams.delete("session_id");
  window.history.replaceState({}, "", url.toString());
}

function queueRender() {
  if (state.renderQueued) return;
  state.renderQueued = true;
  requestAnimationFrame(() => {
    state.renderQueued = false;
    render();
  });
}

function captureFocus() {
  const active = document.activeElement;
  if (!(active instanceof HTMLElement)) return null;
  const field = active.getAttribute("data-field");
  const scope = active.getAttribute("data-scope");
  if (!field && !scope) return null;
  return {
    field,
    scope,
    selectionStart: active.selectionStart ?? null,
    selectionEnd: active.selectionEnd ?? null,
  };
}

function restoreFocus(focusState) {
  if (!focusState) return;
  let selector = "";
  if (focusState.field) {
    selector += `[data-field="${focusState.field}"]`;
  }
  if (focusState.scope) {
    selector += `[data-scope="${focusState.scope}"]`;
  }
  const target = selector ? appElement.querySelector(selector) : null;
  if (!(target instanceof HTMLElement)) return;
  target.focus({ preventScroll: true });
  if (
    typeof target.setSelectionRange === "function" &&
    focusState.selectionStart != null &&
    focusState.selectionEnd != null
  ) {
    target.setSelectionRange(focusState.selectionStart, focusState.selectionEnd);
  }
}

function recentJobs() {
  return sortJobsForStorage(state.jobs).slice(0, 4).map(enrichJob);
}

function dashboardSnapshot() {
  return createDashboardSnapshot(state.jobs);
}

function remainingFreeEntries() {
  return getRemainingFreeEntries(state.jobs.length);
}

function canSaveNewJob() {
  return state.billing.isPro || isEditingDraft(state.draft) || state.jobs.length < FREE_JOB_LIMIT;
}

function syncFormUiFromDraft() {
  state.formUi = deriveFormUi(state.draft);
}

function resetDraft() {
  state.draft = createDefaultDraft();
  syncFormUiFromDraft();
}

function startFreshDraft() {
  const wasEditing = isEditingDraft(state.draft);
  const hadTypedValues = [
    state.draft.clientName,
    state.draft.jobName,
    state.draft.siteAddress,
    state.draft.workSummary,
    state.draft.dueDateText,
    state.draft.reminderDateText,
    state.draft.reminderNote,
    state.draft.fixedPriceText,
  ].some((value) => String(value ?? "").trim().length > 0);

  resetDraft();
  state.selectedTab = TABS.ADD_JOB;
  showToast(wasEditing ? "Started a new job draft." : hadTypedValues ? "Draft cleared." : "Ready for a new job.");
}

function updateDraft(field, value) {
  state.draft = {
    ...state.draft,
    [field]: value,
  };
  queueRender();
}

function updateSetting(field, value) {
  state.settings = {
    ...state.settings,
    [field]: value,
  };
  persistSettings();
  queueRender();
}

function findJob(jobId) {
  return state.jobs.find((job) => Number(job.id) === Number(jobId));
}

function replaceJob(updatedJob) {
  const nextJobs = [...state.jobs];
  const index = nextJobs.findIndex((job) => Number(job.id) === Number(updatedJob.id));
  if (index >= 0) {
    nextJobs[index] = updatedJob;
  } else {
    nextJobs.push(updatedJob);
  }
  state.jobs = sortJobsForStorage(nextJobs);
  persistJobs();
}

function removeJob(jobId) {
  state.jobs = sortJobsForStorage(state.jobs.filter((job) => Number(job.id) !== Number(jobId)));
  persistJobs();
}

function handleSaveDraft() {
  if (!canSaveNewJob()) {
    state.selectedTab = TABS.PRO;
    showToast("Free plan limit reached. Upgrade to keep adding jobs.");
    return;
  }

  const validation = validateDraft(state.draft);
  if (!validation.job) {
    showToast(validation.errorMessage || "Could not save the job.");
    return;
  }

  const job = {
    ...validation.job,
    id: validation.job.id || nextJobId(state.jobs),
  };
  const wasEditing = isEditingDraft(state.draft);
  replaceJob(job);
  resetDraft();
  state.selectedTab = wasEditing ? TABS.HISTORY : TABS.DASHBOARD;
  showToast(wasEditing ? "Job updated." : "Job saved.");
}

function handleEditJob(jobId) {
  const job = findJob(jobId);
  if (!job) {
    showToast("Could not open that job.");
    return;
  }
  state.draft = jobToDraft(job);
  syncFormUiFromDraft();
  state.selectedTab = TABS.ADD_JOB;
  showToast(`Editing ${job.jobName}.`);
}

function handleDeleteJob(jobId) {
  removeJob(jobId);
  showToast("Job deleted.");
}

function handleScheduleReminderTomorrow(jobId) {
  const rawJob = findJob(jobId);
  if (!rawJob) return;
  const job = enrichJob(rawJob);
  if (!job.isOutstanding) {
    showToast("Paid jobs do not need reminders.");
    return;
  }

  reminderEngine.requestPermission().finally(() => {
    const reminderPayload = reminderEngine.scheduleTomorrowMessage(job);
    replaceJob({
      ...job,
      reminderDate: reminderPayload.reminderDate,
      reminderNote: reminderPayload.reminderNote,
    });
    showToast("Reminder set for tomorrow morning.");
  });
}

function handleClearReminder(jobId) {
  const rawJob = findJob(jobId);
  if (!rawJob) return;
  const job = enrichJob(rawJob);
  replaceJob({
    ...job,
    reminderDate: null,
    reminderNote: "",
  });
  showToast("Reminder cleared.");
}

function handleMarkPaid(jobId) {
  const rawJob = findJob(jobId);
  if (!rawJob) return;
  const job = enrichJob(rawJob);
  if (job.invoiceStatus === "Paid") {
    showToast("Job is already marked paid.");
    return;
  }

  replaceJob({
    ...job,
    invoiceStatus: "Paid",
    reminderDate: null,
    reminderNote: "",
  });
  showToast("Job marked paid.");
}

function markInvoiceSent(jobId, notify = true) {
  const rawJob = findJob(jobId);
  if (!rawJob) return;
  const job = enrichJob(rawJob);
  if (job.invoiceStatus === "Paid") return;
  if (job.invoiceStatus === "InvoiceSent") {
    if (notify) {
      showToast("Invoice is already marked sent.");
    }
    return;
  }
  replaceJob({
    ...job,
    invoiceStatus: "InvoiceSent",
  });
  if (notify) {
    showToast("Invoice marked sent.");
  }
}

async function handleExport(jobId) {
  const job = findJob(jobId);
  if (!job) return;

  try {
    await downloadInvoicePdf({
      job,
      currencyCode: state.settings.currencyCode,
      companyName: state.settings.companyName,
      logoDataUrl: state.settings.logoDataUrl,
      isPro: state.billing.isPro,
    });
    markInvoiceSent(jobId, false);
    showToast("Invoice PDF ready to send.");
  } catch (_error) {
    showToast("Could not export invoice PDF.");
  }
}

function handleContinueOnboarding() {
  updateSetting("onboardingComplete", true);
  reminderEngine.requestPermission();
  state.selectedTab = TABS.DASHBOARD;
  showToast("Ready to log the first job.");
}

async function loadServerConfig() {
  const fallbackConfig = {
    forcePro: false,
    checkoutEnabled: false,
    offers: FALLBACK_OFFERS,
    portalEnabled: false,
  };

  try {
    const response = await fetch("/api/web-config");
    if (!response.ok) {
      throw new Error("Config request failed.");
    }
    state.serverConfig = await response.json();
  } catch (_error) {
    state.serverConfig = fallbackConfig;
  }
  state.billing = createBillingState(state.serverConfig);
  queueRender();
}

async function refreshBillingStatus(options = {}) {
  const { silent = false } = options;

  if (state.serverConfig.forcePro) {
    state.billing = createBillingState(state.serverConfig);
    queueRender();
    return true;
  }

  if (!state.settings.billingCustomerId) {
    state.billing = createBillingState(state.serverConfig);
    queueRender();
    if (!silent && state.billing.checkoutEnabled) {
      showToast("No Stripe subscription is linked to this device yet.");
    }
    return false;
  }

  state.billing = {
    ...state.billing,
    isLoading: true,
    statusMessage: "Checking Stripe subscription status...",
  };
  queueRender();

  try {
    const snapshot = await postJson("/api/stripe/billing-status", {
      customerId: state.settings.billingCustomerId,
      installationId: state.settings.installationId,
    });
    state.billing = billingStateFromSnapshot(snapshot);
    queueRender();
    if (!silent) {
      showToast(snapshot.active ? "Stripe subscription confirmed." : snapshot.message || "No active Stripe subscription found.");
    }
    return Boolean(snapshot.active);
  } catch (caught) {
    state.billing = billingStateFromSnapshot({}, caught instanceof Error ? caught.message : "Could not load Stripe billing status.");
    queueRender();
    if (!silent) {
      showToast(state.billing.statusMessage);
    }
    return false;
  }
}

async function verifyCheckoutSession(sessionId) {
  state.billing = {
    ...state.billing,
    isLoading: true,
    statusMessage: "Verifying Stripe checkout...",
  };
  queueRender();

  try {
    const snapshot = await postJson("/api/stripe/checkout-status", {
      installationId: state.settings.installationId,
      sessionId,
    });
    if (snapshot.customerId) {
      setBillingCustomerId(snapshot.customerId);
    }
    state.billing = billingStateFromSnapshot(snapshot);
    queueRender();
    showToast(snapshot.active ? "Payment confirmed. Pro is active." : snapshot.message || "Stripe checkout finished, but Pro is not active yet.");
    return Boolean(snapshot.active);
  } catch (caught) {
    state.billing = billingStateFromSnapshot({}, caught instanceof Error ? caught.message : "Could not verify the Stripe checkout.");
    queueRender();
    showToast(state.billing.statusMessage);
    return false;
  }
}

async function startCheckout(productId) {
  if (!state.billing.checkoutEnabled) {
    showToast("Stripe checkout is not configured on this deploy yet.");
    return;
  }

  state.billing = {
    ...state.billing,
    isLoading: true,
    statusMessage: "Opening Stripe Checkout...",
  };
  queueRender();

  try {
    const payload = await postJson("/api/stripe/create-checkout-session", {
      installationId: state.settings.installationId,
      productId,
    });
    if (!payload.url) {
      throw new Error("Stripe did not return a checkout URL.");
    }
    window.location.assign(payload.url);
  } catch (caught) {
    state.billing = billingStateFromSnapshot({}, caught instanceof Error ? caught.message : "Could not start Stripe Checkout.");
    queueRender();
    showToast(state.billing.statusMessage);
  }
}

async function openBillingPortal() {
  if (!state.settings.billingCustomerId) {
    showToast("No Stripe billing customer is linked to this device.");
    return;
  }

  try {
    const payload = await postJson("/api/stripe/create-portal-session", {
      customerId: state.settings.billingCustomerId,
      returnUrl: `${window.location.origin}/?tab=Pro`,
    });
    if (!payload.url) {
      throw new Error("Stripe did not return a billing portal URL.");
    }
    window.location.assign(payload.url);
  } catch (caught) {
    showToast(caught instanceof Error ? caught.message : "Could not open the Stripe billing portal.");
  }
}

async function restoreBillingFromUrl() {
  const url = normalizeUrlState();
  const checkoutState = url.searchParams.get("checkout");
  const sessionId = url.searchParams.get("session_id");

  if (checkoutState === "cancel") {
    state.selectedTab = TABS.PRO;
    clearCheckoutQueryState(url);
    showToast("Checkout canceled.");
    await refreshBillingStatus({ silent: true });
    return;
  }

  if (checkoutState === "success" && sessionId) {
    state.selectedTab = TABS.PRO;
    await verifyCheckoutSession(sessionId);
    clearCheckoutQueryState(url);
    return;
  }

  await refreshBillingStatus({ silent: true });
}

function applyTheme() {
  const theme = THEME_MODES.find((mode) => mode.storageValue === state.settings.themeMode) ?? THEME_MODES[0];
  document.documentElement.dataset.theme = theme.isDark ? "dark" : "light";
}

function registerServiceWorker() {
  if (!("serviceWorker" in navigator)) return;
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/sw.js").catch(() => {});
  });
}

function renderButton(label, attributes = "") {
  return `<button type="button" ${attributes}>${escapeHtml(label)}</button>`;
}

function renderHeroMetric(label, value) {
  return `
    <div class="hero-metric">
      <span>${escapeHtml(label.toUpperCase())}</span>
      <strong>${escapeHtml(value)}</strong>
    </div>
  `;
}

function renderMetricTile(label, value, supporting) {
  return `
    <article class="metric-tile">
      <span class="metric-label">${escapeHtml(label)}</span>
      <strong class="metric-value">${escapeHtml(value)}</strong>
      <p>${escapeHtml(supporting)}</p>
    </article>
  `;
}

function renderPill(label, tone = "default", extraAttributes = "") {
  return `<span class="pill pill-${tone}" ${extraAttributes}>${escapeHtml(label)}</span>`;
}

function renderPillButton(label, tone = "default", extraAttributes = "") {
  return `
    <button
      type="button"
      class="pill pill-${tone} pill-button"
      ${extraAttributes}
    >
      ${escapeHtml(label)}
    </button>
  `;
}

function renderEmptyCard(title, body) {
  return `
    <article class="panel empty-card">
      <h3>${escapeHtml(title)}</h3>
      <p>${escapeHtml(body)}</p>
    </article>
  `;
}

function renderCurrencySelect(scope, selectedCode) {
  return `
    <label class="field-label">
      <span>Currency</span>
      <select data-scope="${escapeHtml(scope)}" class="field-input">
        ${CURRENCY_OPTIONS.map(
          (option) => `
            <option value="${option.code}" ${option.code === selectedCode ? "selected" : ""}>
              ${escapeHtml(currencyPickerLabel(option.code))}
            </option>
          `,
        ).join("")}
      </select>
    </label>
  `;
}

function renderTopBar() {
  const meta = createTabMeta(state.selectedTab, isEditingDraft(state.draft));
  return `
    <header class="topbar">
      <div>
        <div class="eyebrow">FIELDLEDGER</div>
        <h1>${escapeHtml(meta.title)}</h1>
        <p>${escapeHtml(meta.subtitle)}</p>
      </div>
      <div class="topbar-pills">
        ${renderPill(state.settings.currencyCode, "primary")}
        ${renderPill(state.billing.isPro ? "Pro active" : "Starter", state.billing.isPro ? "success" : "accent")}
        ${
          state.installPromptEvent && !state.installPromptDismissed
            ? `<button type="button" class="install-link" data-action="install-app">Install app</button>`
            : ""
        }
      </div>
    </header>
  `;
}

function navItems() {
  return [
    { tab: TABS.DASHBOARD, label: "Home" },
    { tab: TABS.ADD_JOB, label: "New" },
    { tab: TABS.HISTORY, label: "Jobs" },
    { tab: TABS.SETTINGS, label: "Settings" },
    { tab: TABS.PRO, label: "Pro" },
  ];
}

function renderNav(className) {
  return `
    <nav class="${className}">
      ${navItems()
        .map(
          (item) => `
            <button
              type="button"
              class="nav-item ${item.tab === state.selectedTab ? "is-selected" : ""}"
              data-action="select-tab"
              data-tab="${item.tab}"
            >
              <span>${escapeHtml(item.label)}</span>
              ${
                item.tab === TABS.PRO && !state.billing.isPro
                  ? '<span class="nav-dot" aria-hidden="true"></span>'
                  : ""
              }
            </button>
          `,
        )
        .join("")}
    </nav>
  `;
}

function renderDashboardScreen() {
  const snapshot = dashboardSnapshot();
  const remaining = remainingFreeEntries();
  const jobs = recentJobs();
  const maxTrendValue = Math.max(...snapshot.trend.map((item) => item.value), 1);

  return `
    <section class="screen">
      <article class="hero-card">
        ${renderPill("This week", "inverse")}
        <h2>${escapeHtml(formatCurrency(snapshot.weekRevenue, state.settings.currencyCode))}</h2>
        <p>See what you billed this week, what is unpaid, and your latest jobs.</p>
        <div class="pill-row">
          ${renderPill(`${snapshot.followUpCount} unpaid`, "inverse")}
          ${renderPill(`Top client ${snapshot.topClient}`, "inverse")}
        </div>
        <div class="hero-metrics">
          ${renderHeroMetric("Profit", formatCurrency(snapshot.weekProfit, state.settings.currencyCode))}
          ${renderHeroMetric("Hours", formatHours(snapshot.weekHours))}
          ${renderHeroMetric("Outstanding", formatCurrency(snapshot.unpaidAmount, state.settings.currencyCode))}
        </div>
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>At a glance</h3>
            <p>The main numbers most people check first.</p>
          </div>
        </div>
        <div class="metric-grid">
          ${renderMetricTile("This month", formatCurrency(snapshot.monthRevenue, state.settings.currencyCode), "Total billed in the last 30 days")}
          ${renderMetricTile("Average job", formatCurrency(snapshot.averageJobValue, state.settings.currencyCode), "Helpful when pricing similar work")}
          ${renderMetricTile("Costs", formatCurrency(snapshot.weekCosts, state.settings.currencyCode), "Materials and travel this week")}
          ${renderMetricTile("Unpaid jobs", String(snapshot.followUpCount), "Jobs that still need follow-up")}
        </div>
      </article>

      ${
        !state.billing.isPro
          ? `
            <article class="panel panel-accent">
              <div class="section-header">
                <div>
                  <h3>Free plan</h3>
                  <p>You can save up to 15 jobs on the free plan.</p>
                </div>
                ${renderPill(`${remaining} left`, "soft")}
              </div>
              <p>${state.jobs.length} jobs saved so far.</p>
              ${renderButton("See Pro plans", 'class="button button-primary" data-action="open-pro"')}
            </article>
          `
          : ""
      }

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Last 4 weeks</h3>
            <p>${state.billing.isPro ? "A simple view of how your weekly totals are moving." : "The free plan shows a preview. Pro unlocks the full chart."}</p>
          </div>
        </div>
        ${
          state.billing.isPro
            ? `
              <div class="trend-board">
                ${snapshot.trend
                  .map((item) => {
                    const height = Math.max(18, Math.round((item.value / maxTrendValue) * 128));
                    return `
                      <div class="trend-column">
                        <div class="trend-bar-wrap">
                          <div class="trend-bar" style="height:${height}px"></div>
                        </div>
                        <strong>${escapeHtml(item.label)}</strong>
                        <span>${escapeHtml(formatCurrency(item.value, state.settings.currencyCode))}</span>
                      </div>
                    `;
                  })
                  .join("")}
              </div>
            `
            : renderEmptyCard("4-week chart locked", "Upgrade to Pro to see the full weekly trend.")
        }
      </article>

      <div class="section-header">
        <div>
          <h3>Recent jobs</h3>
          <p>${jobs.length === 0 ? "Save your first job and it will show up here." : "Your latest saved jobs."}</p>
        </div>
      </div>

      ${
        jobs.length === 0
          ? renderEmptyCard("No jobs yet", "Save your first job to see totals and unpaid jobs here.")
          : jobs.map((job) => renderRecentJobCard(job)).join("")
      }
    </section>
  `;
}

function renderRecentJobCard(job) {
  return `
    <article class="panel recent-job-card">
      <div class="job-card-head">
        <div>
          <h3>${escapeHtml(job.jobName)}</h3>
          <p>${escapeHtml(job.clientName)}</p>
        </div>
        ${renderStatusBadge(job)}
      </div>
      <strong class="money-main">${escapeHtml(formatCurrency(job.invoiceTotal, state.settings.currencyCode))}</strong>
      <p>Profit ${escapeHtml(formatCurrency(job.estimatedProfit, state.settings.currencyCode))}</p>
      <span class="muted-row">${escapeHtml(formatShortDate(job.date))} • ${escapeHtml(job.scheduleSummary)}</span>
    </article>
  `;
}

function renderStatusBadge(job) {
  const tone = job.invoiceStatus === "Paid" ? "success" : job.invoiceStatus === "InvoiceSent" ? "primary" : "accent";
  return `<span class="status-badge status-${tone}">${escapeHtml(job.statusLabel)}</span>`;
}

function renderFormField({ label, field, value, type = "text", min = null, step = null, rows = 1 }) {
  if (rows > 1) {
    return `
      <label class="field-label">
        <span>${escapeHtml(label)}</span>
        <textarea data-field="${escapeHtml(field)}" class="field-input" rows="${rows}">${escapeHtml(value)}</textarea>
      </label>
    `;
  }

  return `
    <label class="field-label">
      <span>${escapeHtml(label)}</span>
      <input
        data-field="${escapeHtml(field)}"
        class="field-input"
        type="${escapeHtml(type)}"
        value="${escapeHtml(value)}"
        ${min != null ? `min="${escapeHtml(min)}"` : ""}
        ${step != null ? `step="${escapeHtml(step)}"` : ""}
      />
    </label>
  `;
}

function renderChipSelector({ items, selectedValue, action, valueKey = "value", labelKey = "label" }) {
  return `
    <div class="chip-row">
      ${items
        .map((item) => {
          const value = item[valueKey];
          const label = item[labelKey];
          return `
            <button
              type="button"
              class="chip ${value === selectedValue ? "is-selected" : ""}"
              data-action="${escapeHtml(action)}"
              data-value="${escapeHtml(value)}"
            >
              ${escapeHtml(label)}
            </button>
          `;
        })
        .join("")}
    </div>
  `;
}

function renderJobFormScreen() {
  const preview = previewDraft(state.draft);
  const remaining = remainingFreeEntries();
  const limitReached = !state.billing.isPro && !isEditingDraft(state.draft) && remaining === 0;

  return `
    <section class="screen">
      <article class="hero-card">
        ${renderPillButton(
          isEditingDraft(state.draft) ? "Edit job" : "New job",
          "inverse",
          'data-action="start-fresh-draft" title="Start with a blank job form"',
        )}
        <h2>${escapeHtml(formatCurrency(preview.invoiceTotal, state.settings.currencyCode))}</h2>
        <p>Add the customer, amount, and reminder in a few simple steps.</p>
        ${
          isEditingDraft(state.draft)
            ? "<p class=\"hero-note\">You are updating an existing saved job.</p>"
            : ""
        }
        <div class="pill-row">
          ${renderPill(`Currency ${state.settings.currencyCode}`, "inverse")}
          ${renderPill(state.billing.isPro ? "Pro active" : `${remaining} free left`, "inverse")}
        </div>
        <div class="hero-metrics">
          ${renderHeroMetric("Profit", formatCurrency(preview.estimatedProfit, state.settings.currencyCode))}
          ${
            state.draft.pricingMode === "fixed"
              ? renderHeroMetric("Base", formatCurrency(preview.laborTotal, state.settings.currencyCode))
              : renderHeroMetric("Hours", formatHours(preview.hours))
          }
          ${renderHeroMetric("Stage", INVOICE_STATUSES.find((status) => status.name === state.draft.status)?.label || "Quote")}
        </div>
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Summary</h3>
            <p>Check the total while you fill in the job.</p>
          </div>
        </div>
        <div class="metric-grid">
          ${renderMetricTile("Invoice total", formatCurrency(preview.invoiceTotal, state.settings.currencyCode), state.draft.pricingMode === "fixed" ? "Job price, materials, callout, and extras" : "Labor, materials, callout, and extras")}
          ${renderMetricTile("Total costs", formatCurrency(preview.totalCosts, state.settings.currencyCode), "Materials cost plus travel")}
        </div>
        ${
          !state.billing.isPro
            ? `<p class="subtle-copy">${state.jobs.length} of 15 starter jobs used.</p>`
            : ""
        }
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>1. Basic details</h3>
            <p>Start with the fields most jobs need.</p>
          </div>
        </div>
        <p class="field-help">How are you charging for this job?</p>
        ${renderChipSelector({
          items: PRICING_MODES.map((mode) => ({ value: mode.storageValue, label: mode.label })),
          selectedValue: state.draft.pricingMode,
          action: "select-pricing-mode",
        })}
        <p class="subtle-copy">
          ${
            state.draft.pricingMode === "fixed"
              ? "Use one total amount when the customer is paying a single agreed price."
              : "Use an hourly rate and the start and end time to calculate the invoice."
          }
        </p>
        <div class="field-grid two-up">
          ${renderFormField({ label: "Customer name", field: "clientName", value: state.draft.clientName })}
          ${renderFormField({ label: "Job name", field: "jobName", value: state.draft.jobName })}
        </div>
        <div class="field-grid two-up">
          ${renderFormField({ label: "Work date", field: "dateText", value: state.draft.dateText, type: "date" })}
          ${renderFormField({
            label: state.draft.pricingMode === "fixed" ? "Job price" : "Hourly rate",
            field: state.draft.pricingMode === "fixed" ? "fixedPriceText" : "laborRateText",
            value: state.draft.pricingMode === "fixed" ? state.draft.fixedPriceText : state.draft.laborRateText,
            type: "number",
            step: "0.01",
          })}
        </div>
        <div class="field-grid two-up">
          ${renderFormField({ label: "Start time", field: "startTimeText", value: state.draft.startTimeText, type: "time", step: "60" })}
          ${renderFormField({ label: "End time", field: "endTimeText", value: state.draft.endTimeText, type: "time", step: "60" })}
        </div>
        ${
          state.draft.pricingMode === "fixed"
            ? '<p class="subtle-copy">Time stays on the job record, but the invoice uses the fixed job price.</p>'
            : ""
        }
        ${renderButton(
          state.formUi.showJobDetails ? "Hide address and notes" : "Add address and notes",
          'class="chip toggle-chip" data-action="toggle-job-details"',
        )}
        ${
          state.formUi.showJobDetails
            ? `
              <div class="field-grid">
                ${renderFormField({ label: "Address", field: "siteAddress", value: state.draft.siteAddress })}
                ${renderFormField({ label: "Notes", field: "workSummary", value: state.draft.workSummary, rows: 4 })}
              </div>
            `
            : ""
        }
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>2. Amounts</h3>
            <p>Enter what you will charge for this job.</p>
          </div>
        </div>
        <div class="field-grid two-up">
          ${renderFormField({ label: "Materials to charge", field: "materialsBilledText", value: state.draft.materialsBilledText, type: "number", step: "0.01" })}
          ${renderFormField({ label: "Callout fee", field: "calloutFeeText", value: state.draft.calloutFeeText, type: "number", step: "0.01" })}
        </div>
        ${renderButton(
          state.formUi.showMoreAmounts ? "Hide extra amounts" : "Add extra amounts",
          'class="chip toggle-chip" data-action="toggle-more-amounts"',
        )}
        ${
          state.formUi.showMoreAmounts
            ? `
              <div class="field-grid two-up">
                ${renderFormField({ label: "Extra charge", field: "extraChargeText", value: state.draft.extraChargeText, type: "number", step: "0.01" })}
                ${renderFormField({ label: "Materials cost", field: "materialsCostText", value: state.draft.materialsCostText, type: "number", step: "0.01" })}
              </div>
              <div class="field-grid">
                ${renderFormField({ label: "Travel cost", field: "travelCostText", value: state.draft.travelCostText, type: "number", step: "0.01" })}
              </div>
            `
            : ""
        }
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>3. Invoice</h3>
            <p>Set the job status and add dates only if you need them.</p>
          </div>
        </div>
        ${renderChipSelector({
          items: INVOICE_STATUSES.map((status) => ({ value: status.name, label: status.label })),
          selectedValue: state.draft.status,
          action: "select-status",
        })}
        <div class="chip-row">
          ${renderButton(
            state.formUi.showDueDate ? "Hide due date" : "Add due date",
            'class="chip toggle-chip" data-action="toggle-due-date"',
          )}
          ${
            state.draft.status !== "Paid"
              ? renderButton(
                  state.formUi.showReminder ? "Hide reminder" : "Add reminder",
                  'class="chip toggle-chip" data-action="toggle-reminder"',
                )
              : ""
          }
        </div>
        ${
          state.formUi.showDueDate
            ? `<div class="field-grid">${renderFormField({ label: "Due date", field: "dueDateText", value: state.draft.dueDateText, type: "date" })}</div>`
            : ""
        }
        ${
          state.draft.status !== "Paid" && state.formUi.showReminder
            ? `
              <div class="field-grid">
                ${renderFormField({ label: "Reminder date", field: "reminderDateText", value: state.draft.reminderDateText, type: "date" })}
                ${renderFormField({ label: "Reminder note", field: "reminderNote", value: state.draft.reminderNote, rows: 3 })}
              </div>
            `
            : ""
        }
      </article>

      <article class="panel panel-primary">
        <div class="section-header">
          <div>
            <h3>${isEditingDraft(state.draft) ? "Update job" : "Save job"}</h3>
            <p>${isEditingDraft(state.draft) ? "Review the main numbers before updating the saved job." : "Check the main numbers before you save."}</p>
          </div>
        </div>
        <p>Total ${escapeHtml(formatCurrency(preview.invoiceTotal, state.settings.currencyCode))} • Costs ${escapeHtml(formatCurrency(preview.totalCosts, state.settings.currencyCode))} • Profit ${escapeHtml(formatCurrency(preview.estimatedProfit, state.settings.currencyCode))}</p>
        <p class="subtle-copy">Due ${escapeHtml(preview.dueDateText || "not set")} • Reminder ${escapeHtml(preview.reminderDateText || "not set")}</p>
        ${
          limitReached
            ? `
              <p>The free plan limit is reached. Move to Pro to keep saving jobs.</p>
              ${renderButton("Unlock Pro", 'class="button button-primary" data-action="open-pro"')}
            `
            : `
              ${renderButton(isEditingDraft(state.draft) ? "Update job" : "Save job", 'class="button button-primary" data-action="save-job"')}
              ${
                isEditingDraft(state.draft)
                  ? renderButton("Cancel edit", 'class="button button-secondary" data-action="cancel-edit"')
                  : ""
              }
            `
        }
      </article>
    </section>
  `;
}

function renderHistoryScreen() {
  const jobs = sortJobsForHistory(state.jobs).map(enrichJob);
  const openJobs = jobs.filter((job) => job.isOutstanding).length;
  const paidJobs = jobs.length - openJobs;
  const outstandingValue = jobs.filter((job) => job.isOutstanding).reduce((sum, job) => sum + job.invoiceTotal, 0);

  return `
    <section class="screen">
      <article class="hero-card">
        ${renderPill("Jobs", "inverse")}
        <h2>${escapeHtml(formatCurrency(outstandingValue, state.settings.currencyCode))}</h2>
        <p>Unpaid jobs stay at the top so they are easy to follow up.</p>
        <div class="hero-metrics">
          ${renderHeroMetric("Total", String(jobs.length))}
          ${renderHeroMetric("Open", String(openJobs))}
          ${renderHeroMetric("Paid", String(paidJobs))}
        </div>
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Summary</h3>
            <p>A quick look at unpaid jobs and reminders.</p>
          </div>
        </div>
        <div class="metric-grid">
          ${renderMetricTile("Outstanding", formatCurrency(outstandingValue, state.settings.currencyCode), "Still open across quotes and invoices")}
          ${renderMetricTile("Reminders", `${jobs.filter((job) => job.hasReminder).length} active`, "Jobs with a reminder set")}
        </div>
      </article>

      <div class="section-header">
        <div>
          <h3>Jobs</h3>
          <p>${jobs.length === 0 ? "No work has been saved yet." : "Unpaid jobs stay at the top. You can edit, download, remind, mark paid, or delete each job."}</p>
        </div>
      </div>

      ${
        jobs.length === 0
          ? renderEmptyCard("No jobs yet", "Save a job and it will show up here.")
          : jobs.map((job) => renderHistoryJobCard(job)).join("")
      }
    </section>
  `;
}

function renderHistoryJobCard(job) {
  return `
    <article class="panel history-job-card">
      <div class="job-card-head">
        <div>
          <h3>${escapeHtml(job.jobName)}</h3>
          <p>${escapeHtml(job.clientName)}</p>
        </div>
        ${renderStatusBadge(job)}
      </div>
      <strong class="money-main">${escapeHtml(formatCurrency(job.invoiceTotal, state.settings.currencyCode))}</strong>
      <p>Profit ${escapeHtml(formatCurrency(job.estimatedProfit, state.settings.currencyCode))} • Costs ${escapeHtml(formatCurrency(job.totalCosts, state.settings.currencyCode))}</p>
      <span class="muted-row">${escapeHtml(formatShortDate(job.date))} • ${escapeHtml(job.scheduleSummary)}</span>
      <div class="pill-row">
        ${
          job.paymentDueDate
            ? renderPill(`Due ${formatShortDate(job.paymentDueDate)}`, "soft")
            : ""
        }
        ${
          job.hasReminder && job.reminderDate
            ? renderPill(`Reminder ${formatShortDate(job.reminderDate)}`, "soft")
            : ""
        }
      </div>
      ${job.siteAddress ? `<p>${escapeHtml(job.siteAddress)}</p>` : ""}
      ${job.workSummary ? `<p class="subtle-copy">${escapeHtml(job.workSummary)}</p>` : ""}
      <div class="action-row">
        ${renderButton("Edit", `class="action-pill" data-action="edit-job" data-job-id="${job.id}"`)}
        ${renderButton(job.isOutstanding ? "Send PDF" : "PDF", `class="action-pill" data-action="export-job" data-job-id="${job.id}"`)}
        ${
          job.isOutstanding
            ? renderButton(job.hasReminder ? "Clear" : "Remind", `class="action-pill" data-action="${job.hasReminder ? "clear-reminder" : "schedule-reminder"}" data-job-id="${job.id}"`)
            : ""
        }
        ${
          job.isOutstanding
            ? renderButton("Paid", `class="action-pill" data-action="mark-paid" data-job-id="${job.id}"`)
            : ""
        }
        ${renderButton("Delete", `class="action-pill action-pill-danger" data-action="delete-job" data-job-id="${job.id}"`)}
      </div>
    </article>
  `;
}

function renderOfferCard(offer) {
  const purchaseEnabled = Boolean(offer.checkoutEnabled ?? state.billing.checkoutEnabled) && !state.billing.isPro;
  const highlighted = offer.productId === "field_ledger_pro_yearly";
  return `
    <article class="panel offer-card ${highlighted ? "offer-card-highlighted" : ""}">
      <div class="section-header">
        <div>
          ${
            highlighted
              ? renderPill("Best value", "primary")
              : ""
          }
          <h3>${escapeHtml(offer.title)}</h3>
          <strong class="offer-price">${escapeHtml(offer.price)}</strong>
          <p>${escapeHtml(
            offer.productId === "field_ledger_pro_yearly"
              ? "Best for operators who invoice every week and want the habit to stick."
              : offer.productId === "field_ledger_pro_monthly"
                ? "Lower commitment while you prove the workflow saves enough time to justify itself."
                : offer.description,
          )}</p>
        </div>
      </div>
      ${renderButton(
        state.billing.isPro
          ? "Pro already active"
          : purchaseEnabled
            ? "Open Stripe Checkout"
            : "Waiting for Stripe setup",
        `class="button button-primary" data-action="purchase-offer" data-product-id="${offer.productId}" ${purchaseEnabled ? "" : "disabled"}`,
      )}
    </article>
  `;
}

function renderPaywallScreen() {
  const offers = state.billing.offers?.length ? state.billing.offers : FALLBACK_OFFERS;

  return `
    <section class="screen">
      <article class="hero-card">
        ${renderPill(state.billing.isPro ? "Pro active" : "Subscription", "inverse")}
        <h2>${escapeHtml(state.billing.isPro ? "The paid workflow is unlocked in the web preview." : "Upgrade when the workflow saves enough admin time to earn its keep.")}</h2>
        <p>${escapeHtml(state.billing.isPro ? "Unlimited capture, cleaner follow-up, and a tool that can stay open all week." : "Unlimited jobs, recurring follow-up tools, and a fuller revenue view are what this upgrade is for.")}</p>
        ${state.billing.statusMessage ? `<p class="hero-note">${escapeHtml(state.billing.statusMessage)}</p>` : ""}
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>What Pro adds</h3>
            <p>Three reasons the subscription exists in the product at all.</p>
          </div>
        </div>
        <div class="feature-list">
          ${FEATURE_BULLETS.map((feature) => `<div class="feature-item">${escapeHtml(feature)}</div>`).join("")}
        </div>
      </article>

      ${offers.map((offer) => renderOfferCard(offer)).join("")}

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Launch checklist</h3>
            <p>${escapeHtml(
              state.serverConfig.forcePro
                ? "Preview mode is forcing Pro open for testing."
                : state.billing.checkoutEnabled
                  ? "Stripe checkout is configured. Complete a checkout on this device, then use Refresh billing status if needed."
                  : "Set STRIPE_SECRET_KEY, STRIPE_MONTHLY_PRICE_ID, and STRIPE_YEARLY_PRICE_ID in Netlify to enable payment.",
            )}</p>
          </div>
        </div>
        ${
          state.billing.verificationSource
            ? `<p class="subtle-copy">Verification source: ${escapeHtml(state.billing.verificationSource)}</p>`
            : ""
        }
        ${
          state.billing.verifiedExpiryTime
            ? `<p class="subtle-copy">Current period ends ${escapeHtml(new Date(state.billing.verifiedExpiryTime).toLocaleDateString())}</p>`
            : ""
        }
        <div class="action-row">
          ${renderButton("Refresh billing status", 'class="button button-secondary" data-action="refresh-billing"')}
          ${
            state.billing.isPro && state.billing.portalEnabled && state.settings.billingCustomerId
              ? renderButton("Manage billing", 'class="button button-primary" data-action="open-customer-portal"')
              : ""
          }
        </div>
      </article>
    </section>
  `;
}

function renderSettingsScreen() {
  const logoState = state.settings.logoDataUrl
    ? `<img class="logo-preview-image" src="${escapeHtml(state.settings.logoDataUrl)}" alt="Invoice logo preview" />`
    : `<div class="logo-preview-fallback">${escapeHtml(state.settings.companyName || "No logo yet")}</div>`;

  return `
    <section class="screen">
      <article class="hero-card">
        ${renderPill("Settings", "inverse")}
        <h2>${escapeHtml(state.settings.companyName || "Invoice branding")}</h2>
        <p>Choose your currency, color mode, and the branding customers see on each PDF invoice.</p>
        <div class="pill-row">
          ${renderPill(displayCurrencyLabel(state.settings.currencyCode), "inverse")}
          ${renderPill(THEME_MODES.find((mode) => mode.storageValue === state.settings.themeMode)?.label || "Light", "inverse")}
          ${renderPill(
            state.billing.isPro
              ? state.settings.logoDataUrl
                ? "Logo ready"
                : "Name only"
              : "Logo in Pro",
            "inverse",
          )}
        </div>
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Invoice branding</h3>
            <p>Company name is free. Custom logo and full white-label PDF export are Pro.</p>
          </div>
        </div>
        <div class="field-grid">
          ${renderFormField({ label: "Company name", field: "companyName", value: state.settings.companyName })}
        </div>
        <div class="logo-preview-card">
          ${logoState}
          <div>
            <h4>${escapeHtml(state.settings.companyName || "ShiftLedger")}</h4>
            <p>${escapeHtml(
              state.billing.isPro
                ? state.settings.logoDataUrl
                  ? "Your exported PDF will use the selected logo."
                  : "Your exported PDF will use the company name only."
                : "Free invoices use your company name plus a small ShiftLedger credit.",
            )}</p>
          </div>
        </div>
        <div class="action-row">
          ${renderButton(
            state.billing.isPro
              ? state.settings.logoDataUrl
                ? "Replace logo"
                : "Choose logo"
              : "Unlock logo",
            `class="button button-primary" data-action="${state.billing.isPro ? "choose-logo" : "open-pro"}"`,
          )}
          ${
            state.settings.logoDataUrl
              ? renderButton("Remove logo", 'class="button button-secondary" data-action="remove-logo"')
              : ""
          }
        </div>
        <p class="subtle-copy">
          ${
            state.billing.isPro
              ? "PNG, JPG, or WEBP work best. If you skip the logo, the PDF still uses your company name."
              : "Free invoices can use your company name. Pro unlocks the logo and removes the ShiftLedger credit."
          }
        </p>
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Currency</h3>
            <p>This changes how money is shown across the app and inside the PDF.</p>
          </div>
        </div>
        <div class="sample-card">
          <span class="metric-label">Current format</span>
          <strong>${escapeHtml(displayCurrencyLabel(state.settings.currencyCode))}</strong>
          <p>Sample invoice ${escapeHtml(formatCurrency(1285.5, state.settings.currencyCode))}</p>
        </div>
        ${renderCurrencySelect("settings-currency", state.settings.currencyCode)}
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Appearance</h3>
            <p>Use light mode or dark mode.</p>
          </div>
        </div>
        ${renderChipSelector({
          items: THEME_MODES.map((mode) => ({ value: mode.storageValue, label: mode.label })),
          selectedValue: state.settings.themeMode,
          action: "select-theme",
        })}
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Browser reminders</h3>
            <p>Web reminders use browser notifications instead of Android alarms.</p>
          </div>
        </div>
        <p>Notification permission: <strong>${escapeHtml(reminderEngine.permission())}</strong></p>
        ${renderButton("Enable browser notifications", 'class="button button-secondary" data-action="request-notifications"')}
      </article>

      ${
        state.installPromptEvent && !state.installPromptDismissed
          ? `
            <article class="panel">
              <div class="section-header">
                <div>
                  <h3>Install the app</h3>
                  <p>Installing the PWA gives the web version a more app-like shell for daily use.</p>
                </div>
              </div>
              <div class="action-row">
                ${renderButton("Install FieldLedger", 'class="button button-primary" data-action="install-app"')}
                ${renderButton("Maybe later", 'class="button button-secondary" data-action="dismiss-install"')}
              </div>
            </article>
          `
          : ""
      }

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Preview</h3>
            <p>A quick look at how the PDF details will read.</p>
          </div>
        </div>
        <strong>${escapeHtml(state.settings.companyName || "No company name added yet")}</strong>
        <p>Revenue ${escapeHtml(formatCurrency(3210.4, state.settings.currencyCode))}</p>
        <p>Costs ${escapeHtml(formatCurrency(685.1, state.settings.currencyCode))}</p>
        <p>Profit ${escapeHtml(formatCurrency(2525.3, state.settings.currencyCode))}</p>
        <p class="subtle-copy">
          ${
            state.settings.logoDataUrl
              ? state.billing.isPro
                ? "The invoice header will include the selected logo."
                : "The saved logo is reserved for Pro invoices."
              : state.billing.isPro
                ? "The invoice header will use text only."
                : "Free invoices use your company name plus a small ShiftLedger credit."
          }
        </p>
      </article>
    </section>
  `;
}

function renderOnboardingScreen() {
  return `
    <section class="screen onboarding-screen">
      <article class="hero-card">
        ${renderPill("Quick setup", "inverse")}
        <h2>Track jobs and invoices in one place.</h2>
        <p>Add jobs, see what is unpaid, and set reminders without turning your phone into a spreadsheet.</p>
        <div class="pill-row">
          ${renderPill("Jobs", "inverse")}
          ${renderPill("Invoices", "inverse")}
          ${renderPill(state.settings.currencyCode, "inverse")}
        </div>
      </article>

      <div class="feature-grid">
        <article class="panel feature-card">
          <h3>Save a job fast</h3>
          <p>Add the customer, time, and amount in a few taps.</p>
        </article>
        <article class="panel feature-card">
          <h3>See what is unpaid</h3>
          <p>Home shows this week's total and any jobs still waiting on payment.</p>
        </article>
        <article class="panel feature-card">
          <h3>Set reminders</h3>
          <p>Choose a due date and reminder when you need one.</p>
        </article>
      </div>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Choose your currency</h3>
            <p>You can change this later in Settings.</p>
          </div>
        </div>
        ${renderCurrencySelect("onboarding-currency", state.settings.currencyCode)}
        <p class="subtle-copy">Current selection: ${escapeHtml(displayCurrencyLabel(state.settings.currencyCode))}</p>
      </article>

      ${renderButton("Continue", 'class="button button-primary button-block" data-action="continue-onboarding"')}
    </section>
  `;
}

function renderScreen() {
  if (!state.settings.onboardingComplete) {
    return renderOnboardingScreen();
  }

  switch (state.selectedTab) {
    case TABS.DASHBOARD:
      return renderDashboardScreen();
    case TABS.ADD_JOB:
      return renderJobFormScreen();
    case TABS.HISTORY:
      return renderHistoryScreen();
    case TABS.PRO:
      return renderPaywallScreen();
    case TABS.SETTINGS:
      return renderSettingsScreen();
    default:
      return renderDashboardScreen();
  }
}

function renderToast() {
  if (!state.toastMessage) return "";
  return `<div class="toast">${escapeHtml(state.toastMessage)}</div>`;
}

function render() {
  applyTheme();
  const focusState = captureFocus();
  const shellClass = state.settings.onboardingComplete ? "app-shell" : "app-shell onboarding-mode";

  appElement.innerHTML = `
    <div class="${shellClass}">
      <div class="ambient ambient-one"></div>
      <div class="ambient ambient-two"></div>
      ${
        state.settings.onboardingComplete
          ? `
            ${renderNav("sidebar-nav")}
            <div class="app-main">
              ${renderTopBar()}
              <main class="content">${renderScreen()}</main>
            </div>
            ${renderNav("bottom-nav")}
          `
          : `
            <main class="content onboarding-content">${renderScreen()}</main>
          `
      }
      <div class="toast-layer">${renderToast()}</div>
    </div>
  `;

  restoreFocus(focusState);
}

function setSelectedTab(tab) {
  state.selectedTab = tab;
  queueRender();
}

function handleActionClick(action, element) {
  const jobId = Number(element.dataset.jobId);
  const value = element.dataset.value;
  switch (action) {
    case "select-tab":
      setSelectedTab(element.dataset.tab);
      break;
    case "open-pro":
      setSelectedTab(TABS.PRO);
      break;
    case "continue-onboarding":
      handleContinueOnboarding();
      break;
    case "start-fresh-draft":
      startFreshDraft();
      break;
    case "toggle-job-details":
      state.formUi.showJobDetails = !state.formUi.showJobDetails;
      queueRender();
      break;
    case "toggle-more-amounts":
      state.formUi.showMoreAmounts = !state.formUi.showMoreAmounts;
      queueRender();
      break;
    case "toggle-due-date":
      state.formUi.showDueDate = !state.formUi.showDueDate;
      if (!state.formUi.showDueDate) {
        state.draft.dueDateText = "";
      }
      queueRender();
      break;
    case "toggle-reminder":
      state.formUi.showReminder = !state.formUi.showReminder;
      if (!state.formUi.showReminder) {
        state.draft.reminderDateText = "";
        state.draft.reminderNote = "";
      }
      queueRender();
      break;
    case "select-pricing-mode": {
      const nextMode = value;
      const preview = previewDraft(state.draft);
      state.draft.pricingMode = nextMode;
      if (nextMode === "fixed" && !state.draft.fixedPriceText) {
        state.draft.fixedPriceText = String(preview.laborTotal || "").replace(/\.0+$/, "");
      }
      queueRender();
      break;
    }
    case "select-status":
      state.draft.status = value;
      if (value === "Paid") {
        state.formUi.showReminder = false;
      }
      queueRender();
      break;
    case "save-job":
      handleSaveDraft();
      break;
    case "cancel-edit":
      if (!isEditingDraft(state.draft)) return;
      resetDraft();
      state.selectedTab = TABS.HISTORY;
      showToast("Edit cancelled.");
      break;
    case "edit-job":
      handleEditJob(jobId);
      break;
    case "delete-job":
      handleDeleteJob(jobId);
      break;
    case "schedule-reminder":
      handleScheduleReminderTomorrow(jobId);
      break;
    case "clear-reminder":
      handleClearReminder(jobId);
      break;
    case "mark-paid":
      handleMarkPaid(jobId);
      break;
    case "export-job":
      handleExport(jobId);
      break;
    case "refresh-billing":
      loadServerConfig().then(() => refreshBillingStatus());
      break;
    case "purchase-offer": {
      startCheckout(element.dataset.productId);
      break;
    }
    case "open-customer-portal":
      openBillingPortal();
      break;
    case "choose-logo":
      logoInput.click();
      break;
    case "remove-logo":
      updateSetting("logoDataUrl", null);
      showToast("Logo removed.");
      break;
    case "select-theme":
      updateSetting("themeMode", value);
      break;
    case "request-notifications":
      reminderEngine.requestPermission().then((permission) => {
        showToast(
          permission === "granted"
            ? "Browser notifications enabled."
            : permission === "unsupported"
              ? "This browser does not support notifications."
              : "Notifications were not enabled.",
        );
      });
      break;
    case "install-app":
      if (state.installPromptEvent) {
        state.installPromptEvent.prompt();
        state.installPromptEvent.userChoice.finally(() => {
          state.installPromptEvent = null;
          queueRender();
        });
      }
      break;
    case "dismiss-install":
      state.installPromptDismissed = true;
      saveInstallPromptDismissed(true);
      queueRender();
      break;
    default:
      break;
  }
}

appElement.addEventListener("click", (event) => {
  const target = event.target instanceof Element ? event.target.closest("[data-action]") : null;
  if (!(target instanceof HTMLElement)) return;
  handleActionClick(target.dataset.action, target);
});

appElement.addEventListener("input", (event) => {
  const target = event.target;
  if (!(target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement)) return;
  const field = target.dataset.field;
  if (field && state.selectedTab === TABS.SETTINGS) {
    updateSetting(field, target.value);
    return;
  }
  if (field) {
    updateDraft(field, target.value);
  }
});

appElement.addEventListener("change", (event) => {
  const target = event.target;
  if (!(target instanceof HTMLSelectElement)) return;
  const scope = target.dataset.scope;
  if (scope === "onboarding-currency" || scope === "settings-currency") {
    updateSetting("currencyCode", target.value);
    if (scope === "onboarding-currency") {
      queueRender();
    }
  }
});

logoInput.addEventListener("change", async () => {
  const [file] = logoInput.files ?? [];
  if (!file || !state.billing.isPro) return;
  const dataUrl = await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error("Could not load the logo."));
    reader.readAsDataURL(file);
  }).catch(() => null);

  if (typeof dataUrl === "string") {
    updateSetting("logoDataUrl", dataUrl);
    showToast("Logo updated.");
  }
  logoInput.value = "";
});

window.addEventListener("beforeinstallprompt", (event) => {
  event.preventDefault();
  state.installPromptEvent = event;
  state.installPromptDismissed = false;
  saveInstallPromptDismissed(false);
  queueRender();
});

window.addEventListener("focus", () => {
  reminderEngine.sync(state.jobs);
});

async function start() {
  registerServiceWorker();
  await loadServerConfig();
  await restoreBillingFromUrl();
  reminderEngine.sync(state.jobs);
  render();
}

start();
