import { addDays, enrichJob, todayIso } from "./logic.js";

const DELIVERY_PREFIX = "fieldledger.web.reminder.delivered.";
const ONE_MINUTE = 60 * 1000;

function deliveryKey(jobId, reminderDate) {
  return `${DELIVERY_PREFIX}${jobId}.${reminderDate}`;
}

function getStorage() {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage;
  } catch (_error) {
    return null;
  }
}

function wasDelivered(jobId, reminderDate) {
  return getStorage()?.getItem(deliveryKey(jobId, reminderDate)) === "true";
}

function markDelivered(jobId, reminderDate) {
  getStorage()?.setItem(deliveryKey(jobId, reminderDate), "true");
}

function clearDelivered(jobId, reminderDate) {
  if (!reminderDate) return;
  getStorage()?.removeItem(deliveryKey(jobId, reminderDate));
}

function reminderTriggerTime(reminderDate) {
  const [year, month, day] = reminderDate.split("-").map(Number);
  const triggerAt = new Date(year, month - 1, day, 9, 0, 0, 0).getTime();
  return Math.max(triggerAt, Date.now() + ONE_MINUTE);
}

async function showBrowserNotification(registration, title, body) {
  if (registration?.showNotification) {
    await registration.showNotification(title, {
      body,
      icon: "/assets/icon.svg",
      badge: "/assets/icon.svg",
      tag: `fieldledger-${title}`,
    });
    return;
  }

  if (typeof Notification !== "undefined" && Notification.permission === "granted") {
    const notification = new Notification(title, {
      body,
      icon: "/assets/icon.svg",
      tag: `fieldledger-${title}`,
    });
    notification.onclick = () => {
      window.focus();
      notification.close();
    };
  }
}

function createReminderEngine({ onToast } = {}) {
  const timers = new Map();
  let serviceWorkerRegistration = null;

  async function ensureServiceWorker() {
    if (!("serviceWorker" in navigator)) return null;
    if (serviceWorkerRegistration) return serviceWorkerRegistration;
    try {
      serviceWorkerRegistration = await navigator.serviceWorker.ready;
    } catch (_error) {
      serviceWorkerRegistration = null;
    }
    return serviceWorkerRegistration;
  }

  function cancel(jobId) {
    const timer = timers.get(jobId);
    if (timer) {
      clearTimeout(timer);
      timers.delete(jobId);
    }
  }

  async function notify(job) {
    if (!job.reminderDate || wasDelivered(job.id, job.reminderDate)) {
      return;
    }

    const title = `Follow up with ${job.clientName}`;
    const body = job.reminderMessage;

    if (typeof Notification !== "undefined" && Notification.permission === "granted") {
      const registration = await ensureServiceWorker();
      await showBrowserNotification(registration, title, body);
    } else if (onToast) {
      onToast("Browser notifications are off. The reminder is due now.");
    }

    markDelivered(job.id, job.reminderDate);
  }

  function sync(jobs) {
    timers.forEach((timerId) => clearTimeout(timerId));
    timers.clear();

    for (const rawJob of jobs) {
      const job = enrichJob(rawJob);
      if (!job.hasReminder || !job.reminderDate) {
        clearDelivered(job.id, rawJob.reminderDate);
        continue;
      }

      const triggerAt = reminderTriggerTime(job.reminderDate);
      const delay = Math.max(triggerAt - Date.now(), 1000);
      timers.set(
        job.id,
        setTimeout(() => {
          notify(job).finally(() => timers.delete(job.id));
        }, delay),
      );

      if (job.reminderDate <= todayIso() && !wasDelivered(job.id, job.reminderDate)) {
        setTimeout(() => {
          notify(job);
        }, 1000);
      }
    }
  }

  async function requestPermission() {
    if (typeof Notification === "undefined") {
      return "unsupported";
    }
    if (Notification.permission === "granted") {
      return "granted";
    }
    if (Notification.permission === "denied") {
      return "denied";
    }
    return Notification.requestPermission();
  }

  function permission() {
    if (typeof Notification === "undefined") {
      return "unsupported";
    }
    return Notification.permission;
  }

  function scheduleTomorrowMessage(job) {
    const enriched = enrichJob(job);
    return {
      reminderDate: addDays(todayIso(), 1),
      reminderNote:
        enriched.reminderNote ||
        `Follow up with ${enriched.clientName} about ${enriched.jobName}.`,
    };
  }

  return {
    cancel,
    permission,
    requestPermission,
    scheduleTomorrowMessage,
    sync,
  };
}

export { createReminderEngine, reminderTriggerTime };
