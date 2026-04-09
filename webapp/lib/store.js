import { createDefaultDraft, getThemeMode, sortJobsForStorage } from "./logic.js";

const STORAGE_KEYS = {
  jobs: "fieldledger.web.jobs",
  settings: "fieldledger.web.settings",
  installPromptDismissed: "fieldledger.web.installPromptDismissed",
};

function safeJsonParse(rawValue, fallbackValue) {
  if (!rawValue) return fallbackValue;
  try {
    return JSON.parse(rawValue);
  } catch (_error) {
    return fallbackValue;
  }
}

function getStorage() {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage;
  } catch (_error) {
    return null;
  }
}

function createInstallationId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `fieldledger-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

function defaultSettings() {
  return {
    installationId: createInstallationId(),
    currencyCode: "USD",
    onboardingComplete: false,
    themeMode: "light",
    companyName: "",
    logoDataUrl: null,
  };
}

function loadSettings() {
  const storage = getStorage();
  const fallback = defaultSettings();
  const parsed = safeJsonParse(storage?.getItem(STORAGE_KEYS.settings), fallback);
  return {
    ...fallback,
    ...parsed,
    installationId: parsed?.installationId || fallback.installationId,
    themeMode: getThemeMode(parsed?.themeMode).storageValue,
    logoDataUrl: parsed?.logoDataUrl || null,
  };
}

function saveSettings(settings) {
  const storage = getStorage();
  if (!storage) return;
  storage.setItem(STORAGE_KEYS.settings, JSON.stringify(settings));
}

function loadJobs() {
  const storage = getStorage();
  const parsed = safeJsonParse(storage?.getItem(STORAGE_KEYS.jobs), []);
  if (!Array.isArray(parsed)) return [];
  return sortJobsForStorage(parsed);
}

function saveJobs(jobs) {
  const storage = getStorage();
  if (!storage) return;
  storage.setItem(STORAGE_KEYS.jobs, JSON.stringify(sortJobsForStorage(jobs)));
}

function loadDraft() {
  return createDefaultDraft();
}

function loadInstallPromptDismissed() {
  const storage = getStorage();
  return storage?.getItem(STORAGE_KEYS.installPromptDismissed) === "true";
}

function saveInstallPromptDismissed(value) {
  const storage = getStorage();
  if (!storage) return;
  storage.setItem(STORAGE_KEYS.installPromptDismissed, value ? "true" : "false");
}

export {
  defaultSettings,
  loadDraft,
  loadInstallPromptDismissed,
  loadJobs,
  loadSettings,
  saveInstallPromptDismissed,
  saveJobs,
  saveSettings,
};
