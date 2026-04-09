const APP_NAME = "FieldLedger";
const FREE_JOB_LIMIT = 15;

const CURRENCY_OPTIONS = [
  { code: "USD", label: "US Dollar", locale: "en-US" },
  { code: "EUR", label: "Euro", locale: "de-DE" },
  { code: "GBP", label: "British Pound", locale: "en-GB" },
  { code: "CAD", label: "Canadian Dollar", locale: "en-CA" },
  { code: "AUD", label: "Australian Dollar", locale: "en-AU" },
  { code: "CHF", label: "Swiss Franc", locale: "de-CH" },
  { code: "INR", label: "Indian Rupee", locale: "en-IN" },
];

const THEME_MODES = [
  { storageValue: "light", label: "Light", isDark: false },
  { storageValue: "amoled_dark", label: "Dark", isDark: true },
];

const PRICING_MODES = [
  {
    name: "Hourly",
    storageValue: "hourly",
    label: "Hourly",
    inputLabel: "Hourly rate",
    summaryLabel: "Labor",
  },
  {
    name: "Fixed",
    storageValue: "fixed",
    label: "Fixed price",
    inputLabel: "Job price",
    summaryLabel: "Job price",
  },
];

const INVOICE_STATUSES = [
  { name: "DraftQuote", label: "Quote", isOutstanding: true },
  { name: "InvoiceSent", label: "Sent", isOutstanding: true },
  { name: "Paid", label: "Paid", isOutstanding: false },
];

const TABS = {
  DASHBOARD: "Dashboard",
  ADD_JOB: "AddJob",
  HISTORY: "History",
  PRO: "Pro",
  SETTINGS: "Settings",
};

const FEATURE_BULLETS = [
  "Unlimited jobs, customers, and invoice stages",
  "Four-week revenue trends and outstanding invoice totals",
  "Invoice export, templates, and follow-up reminders",
];

const FALLBACK_OFFERS = [
  {
    productId: "field_ledger_pro_yearly",
    title: "Pro Yearly",
    description: "Best value for solo operators who invoice every week",
    price: "$59.99 / year",
  },
  {
    productId: "field_ledger_pro_monthly",
    title: "Pro Monthly",
    description: "Lower commitment while testing the workflow",
    price: "$6.99 / month",
  },
];

const dateFormatter = new Intl.DateTimeFormat("en-US", {
  month: "short",
  day: "numeric",
});

const moneyFormatters = new Map();

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function createDefaultDraft() {
  return {
    id: 0,
    clientName: "",
    jobName: "",
    siteAddress: "",
    dateText: todayIso(),
    startTimeText: "08:00",
    endTimeText: "12:00",
    pricingMode: "hourly",
    laborRateText: "85",
    fixedPriceText: "",
    materialsBilledText: "0",
    calloutFeeText: "0",
    extraChargeText: "0",
    materialsCostText: "0",
    travelCostText: "0",
    status: "DraftQuote",
    workSummary: "",
    dueDateText: "",
    reminderDateText: "",
    reminderNote: "",
  };
}

function getCurrencyOption(code) {
  return CURRENCY_OPTIONS.find((option) => option.code === code) ?? CURRENCY_OPTIONS[0];
}

function getThemeMode(storageValue) {
  return THEME_MODES.find((mode) => mode.storageValue === storageValue) ?? THEME_MODES[0];
}

function getPricingMode(rawValue) {
  const normalized = String(rawValue ?? "").toLowerCase();
  return (
    PRICING_MODES.find((mode) => mode.storageValue === normalized || mode.name.toLowerCase() === normalized) ??
    PRICING_MODES[0]
  );
}

function getInvoiceStatus(name) {
  return INVOICE_STATUSES.find((status) => status.name === name) ?? INVOICE_STATUSES[0];
}

function displayCurrencyLabel(code) {
  const currency = getCurrencyOption(code);
  return `${currency.code}  ${currencySymbol(code)}  ${currency.label}`;
}

function currencyPickerLabel(code) {
  const currency = getCurrencyOption(code);
  return `${currency.code} • ${currency.label}`;
}

function currencySymbol(code) {
  try {
    return new Intl.NumberFormat(getCurrencyOption(code).locale, {
      style: "currency",
      currency: code,
      currencyDisplay: "narrowSymbol",
      maximumFractionDigits: 0,
    })
      .formatToParts(0)
      .find((part) => part.type === "currency")?.value ?? code;
  } catch (_error) {
    return code;
  }
}

function moneyFormatter(code) {
  const currency = getCurrencyOption(code);
  const cacheKey = `${currency.locale}:${currency.code}`;
  if (!moneyFormatters.has(cacheKey)) {
    moneyFormatters.set(
      cacheKey,
      new Intl.NumberFormat(currency.locale, {
        style: "currency",
        currency: currency.code,
        maximumFractionDigits: 2,
      }),
    );
  }
  return moneyFormatters.get(cacheKey);
}

function formatCurrency(amount, code) {
  return moneyFormatter(code).format(Number(amount ?? 0));
}

function formatHours(hours) {
  return `${Number(hours ?? 0).toFixed(1)} h`;
}

function formatShortDate(isoDate) {
  if (!isoDate) return "";
  const [year, month, day] = isoDate.split("-").map(Number);
  if (!year || !month || !day) return isoDate;
  return dateFormatter.format(new Date(year, month - 1, day));
}

function formatClock(timeText) {
  const minutes = parseTimeToMinutes(timeText);
  if (minutes == null) return timeText ?? "";
  const hours24 = Math.floor(minutes / 60);
  const minutesPart = String(minutes % 60).padStart(2, "0");
  const hours12 = hours24 % 12 || 12;
  const meridiem = hours24 >= 12 ? "PM" : "AM";
  return `${hours12}:${minutesPart} ${meridiem}`;
}

function parseTimeToMinutes(rawValue) {
  const match = /^(\d{2}):(\d{2})$/.exec(String(rawValue ?? "").trim());
  if (!match) return null;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return null;
  return hours * 60 + minutes;
}

function isValidIsoDate(rawValue) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(rawValue ?? "").trim())) {
    return false;
  }
  const [year, month, day] = rawValue.split("-").map(Number);
  const candidate = new Date(Date.UTC(year, month - 1, day));
  return (
    candidate.getUTCFullYear() === year &&
    candidate.getUTCMonth() + 1 === month &&
    candidate.getUTCDate() === day
  );
}

function parseOptionalDate(rawValue, fieldName) {
  const trimmed = String(rawValue ?? "").trim();
  if (!trimmed) return { value: null };
  if (!isValidIsoDate(trimmed)) {
    return { errorMessage: `${fieldName} must use YYYY-MM-DD.` };
  }
  return { value: trimmed };
}

function safeHours(startTimeText, endTimeText) {
  const startMinutes = parseTimeToMinutes(startTimeText);
  const endMinutes = parseTimeToMinutes(endTimeText);
  if (startMinutes == null || endMinutes == null || endMinutes <= startMinutes) {
    return 0;
  }
  return (endMinutes - startMinutes) / 60;
}

function normalizeMoneyInput(value) {
  if (value == null) return 0;
  const parsed = Number.parseFloat(String(value).trim());
  return Number.isFinite(parsed) ? parsed : 0;
}

function toDraftAmount(value) {
  const number = Number(value ?? 0);
  if (number === 0) return "0";
  return String(number);
}

function hasMoneyValue(rawValue) {
  const normalized = String(rawValue ?? "").trim();
  return !["", "0", "0.0", "0.00"].includes(normalized);
}

function isEditingDraft(draft) {
  return Number(draft?.id ?? 0) !== 0;
}

function previewDraft(draft) {
  const pricingMode = getPricingMode(draft.pricingMode);
  const laborRate = normalizeMoneyInput(draft.laborRateText);
  const fixedPrice = normalizeMoneyInput(draft.fixedPriceText);
  const materialsBilled = normalizeMoneyInput(draft.materialsBilledText);
  const calloutFee = normalizeMoneyInput(draft.calloutFeeText);
  const extraCharge = normalizeMoneyInput(draft.extraChargeText);
  const materialsCost = normalizeMoneyInput(draft.materialsCostText);
  const travelCost = normalizeMoneyInput(draft.travelCostText);
  const hours = safeHours(draft.startTimeText, draft.endTimeText);
  const laborTotal = pricingMode.storageValue === "fixed" ? fixedPrice : hours * laborRate;
  const invoiceTotal = laborTotal + materialsBilled + calloutFee + extraCharge;
  const totalCosts = materialsCost + travelCost;

  return {
    hours,
    laborTotal,
    invoiceTotal,
    totalCosts,
    estimatedProfit: invoiceTotal - totalCosts,
    dueDateText: String(draft.dueDateText ?? "").trim(),
    reminderDateText: String(draft.reminderDateText ?? "").trim(),
  };
}

function validateDraft(draft) {
  const clientName = String(draft.clientName ?? "").trim();
  if (!clientName) {
    return { errorMessage: "Add the customer name." };
  }

  const jobName = String(draft.jobName ?? "").trim();
  if (!jobName) {
    return { errorMessage: "Add the job name or service." };
  }

  const dateText = String(draft.dateText ?? "").trim();
  if (!isValidIsoDate(dateText)) {
    return { errorMessage: "Date must use YYYY-MM-DD." };
  }

  const startTimeText = String(draft.startTimeText ?? "").trim();
  if (parseTimeToMinutes(startTimeText) == null) {
    return { errorMessage: "Start time must use HH:MM." };
  }

  const endTimeText = String(draft.endTimeText ?? "").trim();
  if (parseTimeToMinutes(endTimeText) == null) {
    return { errorMessage: "End time must use HH:MM." };
  }

  if (safeHours(startTimeText, endTimeText) <= 0) {
    return { errorMessage: "End time must be after start time." };
  }

  const pricingMode = getPricingMode(draft.pricingMode);
  const laborRate = normalizeMoneyInput(draft.laborRateText);
  if (pricingMode.storageValue === "hourly" && laborRate <= 0) {
    return { errorMessage: "Hourly rate must be greater than zero." };
  }

  const fixedPrice = normalizeMoneyInput(draft.fixedPriceText);
  if (pricingMode.storageValue === "fixed" && fixedPrice <= 0) {
    return { errorMessage: "Job price must be greater than zero." };
  }

  const dueDate = parseOptionalDate(draft.dueDateText, "Due date");
  if (dueDate.errorMessage) {
    return { errorMessage: dueDate.errorMessage };
  }

  const reminderDate = parseOptionalDate(draft.reminderDateText, "Reminder date");
  if (reminderDate.errorMessage) {
    return { errorMessage: reminderDate.errorMessage };
  }

  const status = getInvoiceStatus(draft.status);
  const materialsBilled = normalizeMoneyInput(draft.materialsBilledText);
  const calloutFee = normalizeMoneyInput(draft.calloutFeeText);
  const extraCharge = normalizeMoneyInput(draft.extraChargeText);
  const materialsCost = normalizeMoneyInput(draft.materialsCostText);
  const travelCost = normalizeMoneyInput(draft.travelCostText);

  return {
    job: enrichJob({
      id: Number(draft.id ?? 0),
      clientName,
      jobName,
      siteAddress: String(draft.siteAddress ?? "").trim(),
      date: dateText,
      startTime: startTimeText,
      endTime: endTimeText,
      laborRate,
      pricingMode: pricingMode.storageValue,
      fixedPrice: pricingMode.storageValue === "fixed" ? fixedPrice : 0,
      materialsBilled,
      calloutFee,
      extraCharge,
      materialsCost,
      travelCost,
      invoiceStatus: status.name,
      workSummary: String(draft.workSummary ?? "").trim(),
      paymentDueDate: dueDate.value,
      reminderDate: status.isOutstanding ? reminderDate.value : null,
      reminderNote: String(draft.reminderNote ?? "").trim(),
    }),
  };
}

function jobToDraft(job) {
  const enriched = enrichJob(job);
  return {
    id: enriched.id,
    clientName: enriched.clientName,
    jobName: enriched.jobName,
    siteAddress: enriched.siteAddress,
    dateText: enriched.date,
    startTimeText: enriched.startTime,
    endTimeText: enriched.endTime,
    pricingMode: enriched.pricingMode,
    laborRateText: toDraftAmount(enriched.laborRate),
    fixedPriceText: toDraftAmount(enriched.fixedPrice),
    materialsBilledText: toDraftAmount(enriched.materialsBilled),
    calloutFeeText: toDraftAmount(enriched.calloutFee),
    extraChargeText: toDraftAmount(enriched.extraCharge),
    materialsCostText: toDraftAmount(enriched.materialsCost),
    travelCostText: toDraftAmount(enriched.travelCost),
    status: enriched.invoiceStatus,
    workSummary: enriched.workSummary,
    dueDateText: enriched.paymentDueDate ?? "",
    reminderDateText: enriched.reminderDate ?? "",
    reminderNote: enriched.reminderNote,
  };
}

function enrichJob(job) {
  const pricingMode = getPricingMode(job.pricingMode);
  const invoiceStatus = getInvoiceStatus(job.invoiceStatus);
  const durationMinutes = Math.max(
    0,
    (parseTimeToMinutes(job.endTime) ?? 0) - (parseTimeToMinutes(job.startTime) ?? 0),
  );
  const durationHours = durationMinutes / 60;
  const laborTotal =
    pricingMode.storageValue === "fixed" ? Number(job.fixedPrice ?? 0) : durationHours * Number(job.laborRate ?? 0);
  const invoiceTotal =
    laborTotal +
    Number(job.materialsBilled ?? 0) +
    Number(job.calloutFee ?? 0) +
    Number(job.extraCharge ?? 0);
  const totalCosts = Number(job.materialsCost ?? 0) + Number(job.travelCost ?? 0);
  const estimatedProfit = invoiceTotal - totalCosts;
  const averageHourlyProfit = durationHours === 0 ? 0 : estimatedProfit / durationHours;
  const timeWindowLabel = `${formatClock(job.startTime)} - ${formatClock(job.endTime)}`;
  const scheduleSummary = pricingMode.storageValue === "fixed" ? "Fixed price" : timeWindowLabel;
  const reminderNote = String(job.reminderNote ?? "");

  return {
    id: Number(job.id ?? 0),
    clientName: String(job.clientName ?? ""),
    jobName: String(job.jobName ?? ""),
    siteAddress: String(job.siteAddress ?? ""),
    date: String(job.date ?? todayIso()),
    startTime: String(job.startTime ?? "08:00"),
    endTime: String(job.endTime ?? "12:00"),
    laborRate: Number(job.laborRate ?? 0),
    pricingMode: pricingMode.storageValue,
    fixedPrice: Number(job.fixedPrice ?? 0),
    materialsBilled: Number(job.materialsBilled ?? 0),
    calloutFee: Number(job.calloutFee ?? 0),
    extraCharge: Number(job.extraCharge ?? 0),
    materialsCost: Number(job.materialsCost ?? 0),
    travelCost: Number(job.travelCost ?? 0),
    invoiceStatus: invoiceStatus.name,
    workSummary: String(job.workSummary ?? ""),
    paymentDueDate: job.paymentDueDate ? String(job.paymentDueDate) : null,
    reminderDate: job.reminderDate ? String(job.reminderDate) : null,
    reminderNote,
    durationMinutes,
    durationHours,
    laborTotal,
    baseChargeLabel: pricingMode.summaryLabel,
    invoiceTotal,
    totalCosts,
    estimatedProfit,
    averageHourlyProfit,
    timeWindowLabel,
    scheduleSummary,
    headline: `${job.jobName} for ${job.clientName}`,
    hasReminder: invoiceStatus.isOutstanding && Boolean(job.reminderDate),
    reminderMessage: reminderNote || `Invoice follow-up for ${job.clientName} on ${job.jobName}.`,
    isOutstanding: invoiceStatus.isOutstanding,
    statusLabel: invoiceStatus.label,
  };
}

function sortJobsForStorage(jobs) {
  return [...jobs].map(enrichJob).sort(compareJobsByDateDescending);
}

function sortJobsForHistory(jobs) {
  return [...jobs]
    .map(enrichJob)
    .sort((left, right) => {
      if (left.isOutstanding !== right.isOutstanding) {
        return left.isOutstanding ? -1 : 1;
      }
      return compareJobsByDateDescending(left, right);
    });
}

function compareJobsByDateDescending(left, right) {
  if (left.date !== right.date) return right.date.localeCompare(left.date);
  if (left.startTime !== right.startTime) return right.startTime.localeCompare(left.startTime);
  return Number(right.id) - Number(left.id);
}

function buildTrend(today, jobs) {
  const currentWeekStart = previousOrSameMonday(today);
  const enrichedJobs = jobs.map(enrichJob);
  const trend = [];
  for (let weekOffset = 3; weekOffset >= 0; weekOffset -= 1) {
    const start = addDays(currentWeekStart, -(weekOffset * 7));
    const end = addDays(start, 6);
    const value = enrichedJobs
      .filter((job) => job.date >= start && job.date <= end)
      .reduce((sum, job) => sum + job.invoiceTotal, 0);
    trend.push({
      label: weekOffset === 0 ? "Now" : `${weekOffset}w`,
      value,
    });
  }
  return trend;
}

function createDashboardSnapshot(jobs, today = todayIso()) {
  const enrichedJobs = jobs.map(enrichJob);
  if (enrichedJobs.length === 0) {
    return {
      weekRevenue: 0,
      weekProfit: 0,
      weekCosts: 0,
      weekHours: 0,
      averageJobValue: 0,
      monthRevenue: 0,
      unpaidAmount: 0,
      followUpCount: 0,
      topClient: "No jobs yet",
      trend: buildTrend(today, []),
    };
  }

  const startOfWeek = previousOrSameMonday(today);
  const startOfMonthWindow = addDays(today, -29);
  const weekJobs = enrichedJobs.filter((job) => job.date >= startOfWeek);
  const monthJobs = enrichedJobs.filter((job) => job.date >= startOfMonthWindow);

  const revenueByClient = new Map();
  for (const job of enrichedJobs) {
    revenueByClient.set(job.clientName, (revenueByClient.get(job.clientName) ?? 0) + job.invoiceTotal);
  }

  const topClient =
    [...revenueByClient.entries()].sort((left, right) => right[1] - left[1])[0]?.[0] ?? "No jobs yet";
  const weekHours = weekJobs.reduce((sum, job) => sum + job.durationHours, 0);
  const monthRevenue = monthJobs.reduce((sum, job) => sum + job.invoiceTotal, 0);

  return {
    weekRevenue: weekJobs.reduce((sum, job) => sum + job.invoiceTotal, 0),
    weekProfit: weekJobs.reduce((sum, job) => sum + job.estimatedProfit, 0),
    weekCosts: weekJobs.reduce((sum, job) => sum + job.totalCosts, 0),
    weekHours,
    averageJobValue: monthJobs.length === 0 ? 0 : monthRevenue / monthJobs.length,
    monthRevenue,
    unpaidAmount: enrichedJobs.filter((job) => job.isOutstanding).reduce((sum, job) => sum + job.invoiceTotal, 0),
    followUpCount: enrichedJobs.filter((job) => job.isOutstanding).length,
    topClient,
    trend: buildTrend(today, enrichedJobs),
  };
}

function previousOrSameMonday(isoDate) {
  const [year, month, day] = isoDate.split("-").map(Number);
  const date = new Date(year, month - 1, day);
  const dayOfWeek = date.getDay();
  const offset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
  date.setDate(date.getDate() + offset);
  return localDateToIso(date);
}

function addDays(isoDate, amount) {
  const [year, month, day] = isoDate.split("-").map(Number);
  const date = new Date(year, month - 1, day);
  date.setDate(date.getDate() + amount);
  return localDateToIso(date);
}

function localDateToIso(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getRemainingFreeEntries(jobCount) {
  return Math.max(FREE_JOB_LIMIT - Number(jobCount ?? 0), 0);
}

function createBillingState(config = {}) {
  const forcePro = Boolean(config.forcePro);
  const checkoutEnabled = Boolean(config.checkoutEnabled);
  const portalEnabled = Boolean(config.portalEnabled);
  return {
    isLoading: false,
    isConnected: checkoutEnabled || forcePro,
    isPro: forcePro,
    offers: config.offers?.length ? config.offers : FALLBACK_OFFERS,
    statusMessage: forcePro
      ? "Web preview is running with Pro unlocked."
      : checkoutEnabled
        ? "Choose a plan to start Stripe Checkout."
        : "Stripe checkout is not configured yet on this deploy.",
    verificationSource: forcePro ? "web-preview-config" : null,
    verifiedExpiryTime: null,
    isVerificationConfigured: checkoutEnabled || forcePro,
    checkoutEnabled,
    portalEnabled,
  };
}

function createTabMeta(selectedTab, isEditing) {
  switch (selectedTab) {
    case TABS.DASHBOARD:
      return {
        title: "Home",
        subtitle: "This week, unpaid jobs, and recent work.",
      };
    case TABS.ADD_JOB:
      return {
        title: isEditing ? "Edit job" : "New job",
        subtitle: isEditing
          ? "Update the saved job and keep the same invoice record."
          : "Add a job and save it in a minute.",
      };
    case TABS.HISTORY:
      return {
        title: "Jobs",
        subtitle: "Unpaid, paid, shared, and reminded jobs.",
      };
    case TABS.PRO:
      return {
        title: "Pro",
        subtitle: "Plans and billing status.",
      };
    case TABS.SETTINGS:
      return {
        title: "Settings",
        subtitle: "Currency and display mode.",
      };
    default:
      return {
        title: APP_NAME,
        subtitle: "Offline-first job tracking.",
      };
  }
}

function nextJobId(jobs) {
  const maxId = jobs.reduce((highest, job) => Math.max(highest, Number(job.id ?? 0)), 0);
  return maxId + 1;
}

export {
  APP_NAME,
  CURRENCY_OPTIONS,
  FALLBACK_OFFERS,
  FEATURE_BULLETS,
  FREE_JOB_LIMIT,
  INVOICE_STATUSES,
  PRICING_MODES,
  TABS,
  THEME_MODES,
  addDays,
  createBillingState,
  createDashboardSnapshot,
  createDefaultDraft,
  createTabMeta,
  currencyPickerLabel,
  currencySymbol,
  displayCurrencyLabel,
  enrichJob,
  formatClock,
  formatCurrency,
  formatHours,
  formatShortDate,
  getCurrencyOption,
  getInvoiceStatus,
  getPricingMode,
  getRemainingFreeEntries,
  getThemeMode,
  hasMoneyValue,
  isEditingDraft,
  nextJobId,
  parseTimeToMinutes,
  previewDraft,
  safeHours,
  sortJobsForHistory,
  sortJobsForStorage,
  todayIso,
  validateDraft,
  jobToDraft,
};
