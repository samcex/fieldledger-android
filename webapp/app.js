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
  loadCloudSyncState,
  loadJobs,
  loadSettings,
  saveCloudSyncState,
  saveInstallPromptDismissed,
  saveJobs,
  saveSettings,
} from "./lib/store.js";
import { downloadInvoicePdf } from "./lib/pdf.js";
import { createReminderEngine } from "./lib/reminders.js";
import {
  authErrorMessage,
  compareIsoTimes,
  deleteCloudJob,
  deserializeCloudJob,
  fetchCloudProfile,
  getCurrentUser,
  hasAuthConfig,
  listCloudJobs,
  onAuthChange,
  randomId,
  signInWithEmail,
  signInWithGoogle,
  signOut,
  signUpWithEmail,
  upsertCloudJob,
  upsertCloudProfile,
} from "./lib/cloud.js";

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
  cloudSync: loadCloudSyncState(),
  publicConfig: {
    supabaseUrl: null,
    supabaseAnonKey: null,
  },
  billing: createBillingState(),
  auth: {
    isConfigured: false,
    isReady: false,
    isBusy: false,
    isSyncing: false,
    user: null,
    statusMessage: "Account sync is optional.",
    lastSyncedAt: null,
    remoteJobCount: 0,
    unsubscribe: null,
    jobsSyncTimer: null,
    profileSyncTimer: null,
  },
  authForm: {
    email: "",
    password: "",
  },
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

function persistCloudState() {
  saveCloudSyncState(state.cloudSync);
}

function persistJobs() {
  saveJobs(state.jobs);
  reminderEngine.sync(state.jobs);
  queueJobsSync();
}

function hasCloudAuth() {
  return hasAuthConfig(state.publicConfig);
}

function authRedirectUrl() {
  const url = new URL(window.location.origin);
  url.searchParams.set("tab", TABS.SETTINGS);
  return url.toString();
}

function nextLocalJobIdFrom(jobs) {
  return jobs.reduce((highest, job) => Math.max(highest, Number(job.id ?? 0)), 0) + 1;
}

function normalizeJobForSync(job, fallbackId = 0) {
  return enrichJob({
    ...job,
    id: Number(job.id ?? fallbackId),
    cloudId: job.cloudId ? String(job.cloudId) : null,
    updatedAt: job.updatedAt ? String(job.updatedAt) : new Date().toISOString(),
  });
}

function replaceAllJobs(nextJobs, { persist = true } = {}) {
  state.jobs = sortJobsForStorage(nextJobs.map((job, index) => normalizeJobForSync(job, index + 1)));
  if (persist) {
    saveJobs(state.jobs);
    reminderEngine.sync(state.jobs);
  }
}

function queueProfileSync() {
  if (!state.auth.user) return;
  if (state.auth.profileSyncTimer) {
    clearTimeout(state.auth.profileSyncTimer);
  }
  state.auth.profileSyncTimer = setTimeout(() => {
    state.auth.profileSyncTimer = null;
    syncProfileToCloud();
  }, 700);
}

function queueJobsSync() {
  if (!state.auth.user) return;
  if (state.auth.jobsSyncTimer) {
    clearTimeout(state.auth.jobsSyncTimer);
  }
  state.auth.jobsSyncTimer = setTimeout(() => {
    state.auth.jobsSyncTimer = null;
    syncJobsWithCloud();
  }, 500);
}

function applyRemoteProfileToSettings(profile) {
  if (!profile) return;
  state.settings = {
    ...state.settings,
    currencyCode: profile.currency_code || state.settings.currencyCode,
    themeMode: profile.theme_mode || state.settings.themeMode,
    companyName: profile.company_name || "",
  };
  persistSettings();
}

async function syncProfileToCloud() {
  if (!state.auth.user) return false;

  try {
    await upsertCloudProfile(state.publicConfig, {
      userId: state.auth.user.id,
      email: state.auth.user.email || "",
      companyName: state.settings.companyName,
      currencyCode: state.settings.currencyCode,
      themeMode: state.settings.themeMode,
    });
    return true;
  } catch (error) {
    state.auth.statusMessage = authErrorMessage(error, "Could not sync account settings.");
    queueRender();
    return false;
  }
}

async function flushPendingDeletes() {
  if (!state.auth.user || state.cloudSync.pendingDeleteCloudIds.length === 0) {
    return;
  }

  const remaining = [];
  for (const cloudId of state.cloudSync.pendingDeleteCloudIds) {
    try {
      await deleteCloudJob(state.publicConfig, state.auth.user.id, cloudId);
    } catch (_error) {
      remaining.push(cloudId);
    }
  }

  state.cloudSync = {
    ...state.cloudSync,
    pendingDeleteCloudIds: remaining,
  };
  persistCloudState();
}

async function syncJobsWithCloud() {
  if (!state.auth.user || state.auth.isSyncing) return false;

  state.auth.isSyncing = true;
  state.auth.statusMessage = "Syncing jobs to the cloud...";
  queueRender();

  try {
    const remoteProfile = await fetchCloudProfile(state.publicConfig, state.auth.user.id);
    if (remoteProfile) {
      applyRemoteProfileToSettings(remoteProfile);
    } else {
      await syncProfileToCloud();
    }

    await flushPendingDeletes();

    const remoteRows = await listCloudJobs(state.publicConfig, state.auth.user.id);
    const remoteByCloudId = new Map(remoteRows.map((row) => [String(row.id), row]));
    const normalizedLocalJobs = state.jobs.map((job) => normalizeJobForSync(job));
    const mergedJobs = [];
    let nextLocalId = nextLocalJobIdFrom(normalizedLocalJobs);

    for (const localJob of normalizedLocalJobs) {
      if (!localJob.cloudId) {
        const syncedRow = await upsertCloudJob(state.publicConfig, state.auth.user.id, {
          ...localJob,
          cloudId: randomId(),
        });
        const syncedJob = normalizeJobForSync(
          {
            ...localJob,
            cloudId: syncedRow.id,
            updatedAt: syncedRow.updated_at,
          },
          localJob.id,
        );
        mergedJobs.push(syncedJob);
        remoteByCloudId.set(syncedJob.cloudId, syncedRow);
        continue;
      }

      const remoteRow = remoteByCloudId.get(localJob.cloudId);
      if (!remoteRow) {
        const syncedRow = await upsertCloudJob(state.publicConfig, state.auth.user.id, localJob);
        mergedJobs.push(
          normalizeJobForSync(
            {
              ...localJob,
              updatedAt: syncedRow.updated_at,
            },
            localJob.id,
          ),
        );
        remoteByCloudId.set(localJob.cloudId, syncedRow);
        continue;
      }

      const remoteUpdatedAt = String(remoteRow.updated_at ?? "");
      if (compareIsoTimes(localJob.updatedAt, remoteUpdatedAt) >= 0) {
        if (compareIsoTimes(localJob.updatedAt, remoteUpdatedAt) > 0) {
          const syncedRow = await upsertCloudJob(state.publicConfig, state.auth.user.id, localJob);
          mergedJobs.push(
            normalizeJobForSync(
              {
                ...localJob,
                updatedAt: syncedRow.updated_at,
              },
              localJob.id,
            ),
          );
          remoteByCloudId.set(localJob.cloudId, syncedRow);
        } else {
          mergedJobs.push(localJob);
        }
      } else {
        mergedJobs.push(deserializeCloudJob(remoteRow, localJob.id));
      }
    }

    const seenCloudIds = new Set(mergedJobs.map((job) => job.cloudId).filter(Boolean));
    for (const remoteRow of remoteByCloudId.values()) {
      if (seenCloudIds.has(String(remoteRow.id))) continue;
      mergedJobs.push(deserializeCloudJob(remoteRow, nextLocalId));
      nextLocalId += 1;
    }

    replaceAllJobs(mergedJobs);
    state.cloudSync = {
      ...state.cloudSync,
      lastSyncedAt: new Date().toISOString(),
    };
    persistCloudState();

    state.auth.remoteJobCount = remoteByCloudId.size;
    state.auth.lastSyncedAt = state.cloudSync.lastSyncedAt;
    state.auth.statusMessage = `Cloud sync active for ${state.auth.user.email || "this account"}.`;
    return true;
  } catch (error) {
    state.auth.statusMessage = authErrorMessage(error, "Could not sync jobs right now.");
    return false;
  } finally {
    state.auth.isSyncing = false;
    queueRender();
  }
}

async function loadPublicConfig() {
  const fallbackConfig = {
    supabaseUrl: null,
    supabaseAnonKey: null,
  };

  try {
    const response = await fetch("/api/public-config");
    if (!response.ok) {
      throw new Error("Config request failed.");
    }
    const data = await response.json();
    state.publicConfig = {
      supabaseUrl: data.supabaseUrl || null,
      supabaseAnonKey: data.supabaseAnonKey || null,
    };
  } catch (_error) {
    state.publicConfig = fallbackConfig;
  }

  state.auth.isConfigured = hasCloudAuth();
  if (!state.auth.isConfigured) {
    state.auth.isReady = true;
    state.auth.statusMessage = "Set Supabase public keys to enable account sync.";
  }
  queueRender();
}

async function handleAuthUserChanged(user, message = null) {
  state.auth.user = user;
  state.auth.isReady = true;
  state.auth.remoteJobCount = user ? state.auth.remoteJobCount : 0;
  state.auth.statusMessage =
    message ||
    (user
      ? `Signed in as ${user.email || "account user"}.`
      : state.auth.isConfigured
        ? "Sign in to back up jobs across devices."
        : "Set Supabase public keys to enable account sync.");
  queueRender();

  if (!user) {
    return;
  }

  await syncJobsWithCloud();
}

async function initializeAuth() {
  if (!state.auth.isConfigured) return;

  state.auth.isReady = false;
  state.auth.statusMessage = "Checking account session...";
  queueRender();

  if (!state.auth.unsubscribe) {
    state.auth.unsubscribe = await onAuthChange(state.publicConfig, (user) => {
      handleAuthUserChanged(user);
    });
  }

  try {
    const user = await getCurrentUser(state.publicConfig);
    await handleAuthUserChanged(user);
  } catch (error) {
    state.auth.isReady = true;
    state.auth.statusMessage = authErrorMessage(error, "Could not check the current account session.");
    queueRender();
  }
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
  if (["currencyCode", "themeMode", "companyName"].includes(field)) {
    queueProfileSync();
  }
  queueRender();
}

function findJob(jobId) {
  return state.jobs.find((job) => Number(job.id) === Number(jobId));
}

function replaceJob(updatedJob) {
  const nextJobs = [...state.jobs];
  const normalizedJob = normalizeJobForSync(updatedJob, nextJobId(state.jobs));
  const index = nextJobs.findIndex((job) => Number(job.id) === Number(normalizedJob.id));
  if (index >= 0) {
    nextJobs[index] = normalizedJob;
  } else {
    nextJobs.push(normalizedJob);
  }
  state.jobs = sortJobsForStorage(nextJobs);
  persistJobs();
}

function removeJob(jobId) {
  const existingJob = findJob(jobId);
  if (existingJob?.cloudId && !state.cloudSync.pendingDeleteCloudIds.includes(existingJob.cloudId)) {
    state.cloudSync = {
      ...state.cloudSync,
      pendingDeleteCloudIds: [...state.cloudSync.pendingDeleteCloudIds, existingJob.cloudId],
    };
    persistCloudState();
  }
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

function validateAuthForm() {
  const email = String(state.authForm.email ?? "").trim();
  const password = String(state.authForm.password ?? "");

  if (!email) {
    throw new Error("Add an email address.");
  }
  if (!password) {
    throw new Error("Add a password.");
  }
  if (password.length < 6) {
    throw new Error("Use a password with at least 6 characters.");
  }

  return { email, password };
}

async function handleEmailSignIn() {
  try {
    const { email, password } = validateAuthForm();
    state.auth.isBusy = true;
    state.auth.statusMessage = "Signing in...";
    queueRender();
    await signInWithEmail(state.publicConfig, email, password);
    showToast("Signed in.");
  } catch (error) {
    const message = authErrorMessage(error, "Could not sign in.");
    state.auth.statusMessage = message;
    showToast(message);
  } finally {
    state.auth.isBusy = false;
    queueRender();
  }
}

async function handleEmailSignUp() {
  try {
    const { email, password } = validateAuthForm();
    state.auth.isBusy = true;
    state.auth.statusMessage = "Creating account...";
    queueRender();
    const result = await signUpWithEmail(state.publicConfig, email, password, authRedirectUrl());
    if (result.hasSession) {
      showToast("Account created.");
    } else {
      showToast("Account created. Check your email to confirm it if confirmation is enabled.");
    }
  } catch (error) {
    const message = authErrorMessage(error, "Could not create the account.");
    state.auth.statusMessage = message;
    showToast(message);
  } finally {
    state.auth.isBusy = false;
    queueRender();
  }
}

async function handleGoogleSignIn() {
  try {
    state.auth.isBusy = true;
    state.auth.statusMessage = "Redirecting to Google...";
    queueRender();
    await signInWithGoogle(state.publicConfig, authRedirectUrl());
  } catch (error) {
    const message = authErrorMessage(error, "Could not start Google sign-in.");
    state.auth.statusMessage = message;
    state.auth.isBusy = false;
    showToast(message);
    queueRender();
  }
}

async function handleAccountSignOut() {
  try {
    state.auth.isBusy = true;
    state.auth.statusMessage = "Signing out...";
    queueRender();
    await signOut(state.publicConfig);
    showToast("Signed out.");
  } catch (error) {
    const message = authErrorMessage(error, "Could not sign out.");
    state.auth.statusMessage = message;
    showToast(message);
  } finally {
    state.auth.isBusy = false;
    queueRender();
  }
}

async function handleManualCloudSync() {
  const didSync = await syncJobsWithCloud();
  showToast(didSync ? "Cloud sync complete." : state.auth.statusMessage);
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
  const accountLabel = state.auth.user
    ? state.auth.user.email || "Cloud sync on"
    : state.auth.isConfigured
      ? "Offline only"
      : "No account sync";
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
        ${renderPill(accountLabel, state.auth.user ? "success" : "soft")}
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

function renderAuthPanel() {
  const actionDisabled = state.auth.isBusy || state.auth.isSyncing ? "disabled" : "";

  if (!state.auth.isConfigured) {
    return `
      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Account sync</h3>
            <p>Google login and email login are ready, but this deploy still needs Supabase public keys.</p>
          </div>
        </div>
        <p class="subtle-copy">Set <code>SUPABASE_URL</code> and <code>SUPABASE_ANON_KEY</code> in Netlify for production, and in the local preview backend if you want to test sign-in outside Netlify.</p>
      </article>
    `;
  }

  if (!state.auth.user) {
    return `
      <article class="panel">
        <div class="section-header">
          <div>
            <h3>Account sync</h3>
            <p>Sign in with Google first, or use email and password. Jobs stay local until you connect an account.</p>
          </div>
        </div>
        <div class="field-grid two-up">
          <label class="field-label">
            <span>Email</span>
            <input
              class="field-input"
              type="email"
              value="${escapeHtml(state.authForm.email)}"
              data-auth-field="email"
              autocomplete="email"
            />
          </label>
          <label class="field-label">
            <span>Password</span>
            <input
              class="field-input"
              type="password"
              value="${escapeHtml(state.authForm.password)}"
              data-auth-field="password"
              autocomplete="current-password"
            />
          </label>
        </div>
        <div class="action-row auth-row">
          ${renderButton("Continue with Google", `class="button button-primary" data-action="auth-google" ${actionDisabled}`)}
          ${renderButton("Sign in", `class="button button-secondary" data-action="auth-sign-in" ${actionDisabled}`)}
          ${renderButton("Create account", `class="button button-secondary" data-action="auth-sign-up" ${actionDisabled}`)}
        </div>
        <p class="subtle-copy">${escapeHtml(state.auth.statusMessage)}</p>
      </article>
    `;
  }

  return `
    <article class="panel">
      <div class="section-header">
        <div>
          <h3>Account sync</h3>
          <p>Jobs are backed up to your account and can be pulled onto another device.</p>
        </div>
      </div>
      <div class="metric-grid">
        ${renderMetricTile("Signed in", state.auth.user.email || "Google account", "Your cloud sync identity")}
        ${renderMetricTile("Cloud jobs", String(state.auth.remoteJobCount), "Rows stored in the jobs table")}
      </div>
      <p class="subtle-copy">${escapeHtml(state.auth.statusMessage)}</p>
      <p class="subtle-copy">Last synced: ${escapeHtml(state.auth.lastSyncedAt ? new Date(state.auth.lastSyncedAt).toLocaleString() : "Not yet")}</p>
      <p class="subtle-copy">Logo files still stay on this device for now. Jobs, company name, currency, and theme sync to the account.</p>
      <div class="action-row auth-row">
        ${renderButton("Sync now", `class="button button-primary" data-action="auth-sync" ${actionDisabled}`)}
        ${renderButton("Sign out", `class="button button-secondary" data-action="auth-sign-out" ${actionDisabled}`)}
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

      ${renderAuthPanel()}

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
    case "auth-google":
      handleGoogleSignIn();
      break;
    case "auth-sign-in":
      handleEmailSignIn();
      break;
    case "auth-sign-up":
      handleEmailSignUp();
      break;
    case "auth-sync":
      handleManualCloudSync();
      break;
    case "auth-sign-out":
      handleAccountSignOut();
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
  const authField = target.dataset.authField;
  if (authField) {
    state.authForm = {
      ...state.authForm,
      [authField]: target.value,
    };
    return;
  }
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
  state.billing = createBillingState();
  normalizeUrlState();
  await loadPublicConfig();
  await initializeAuth();
  reminderEngine.sync(state.jobs);
  render();
}

start();
