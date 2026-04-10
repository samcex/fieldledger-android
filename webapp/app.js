import {
  APP_NAME,
  CURRENCY_OPTIONS,
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

function persistJobs() {
  saveJobs(state.jobs);
  reminderEngine.sync(state.jobs);
}

function normalizeUrlState() {
  const url = new URL(window.location.href);
  const requestedTab = url.searchParams.get("tab");
  if (requestedTab && Object.values(TABS).includes(requestedTab)) {
    state.selectedTab = requestedTab;
  }
  return url;
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

function canSaveNewJob() {
  return true;
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
  state.billing = createBillingState();
  queueRender();
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
        ${renderPill("All features free", "success")}
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
            </button>
          `,
        )
        .join("")}
    </nav>
  `;
}

function renderDashboardScreen() {
  const snapshot = dashboardSnapshot();
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

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Last 4 weeks</h3>
            <p>A simple view of how your weekly totals are moving.</p>
          </div>
        </div>
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
          ${renderPill("Ready to save", "inverse")}
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
        ${renderButton(isEditingDraft(state.draft) ? "Update job" : "Save job", 'class="button button-primary" data-action="save-job"')}
        ${
          isEditingDraft(state.draft)
            ? renderButton("Cancel edit", 'class="button button-secondary" data-action="cancel-edit"')
            : ""
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
          ${renderPill(state.settings.logoDataUrl ? "Logo ready" : "Name only", "inverse")}
        </div>
      </article>

      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Invoice branding</h3>
            <p>Company name and custom logo both appear on exported invoices.</p>
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
              state.settings.logoDataUrl
                ? "Your exported PDF will use the selected logo."
                : "Your exported PDF will use the company name only.",
            )}</p>
          </div>
        </div>
        <div class="action-row">
          ${renderButton(
            state.settings.logoDataUrl ? "Replace logo" : "Choose logo",
            'class="button button-primary" data-action="choose-logo"',
          )}
          ${
            state.settings.logoDataUrl
              ? renderButton("Remove logo", 'class="button button-secondary" data-action="remove-logo"')
              : ""
          }
        </div>
        <p class="subtle-copy">
          PNG, JPG, or WEBP work best. If you skip the logo, the PDF still uses your company name.
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
              ? "The invoice header will include the selected logo."
              : "The invoice header will use text only."
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
  if (!file) return;
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
  normalizeUrlState();
  reminderEngine.sync(state.jobs);
  render();
}

start();
