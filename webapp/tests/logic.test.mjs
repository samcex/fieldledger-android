import test from "node:test";
import assert from "node:assert/strict";

import {
  createDashboardSnapshot,
  createDefaultDraft,
  formatCurrency,
  jobToDraft,
  validateDraft,
} from "../lib/logic.js";

test("calculates weekly revenue outstanding amount and top client", () => {
  const jobs = [
    {
      id: 1,
      clientName: "Acme Bakery",
      jobName: "Grease trap service",
      siteAddress: "12 Market St",
      date: "2026-04-07",
      startTime: "09:00",
      endTime: "13:00",
      laborRate: 85,
      pricingMode: "hourly",
      fixedPrice: 0,
      materialsBilled: 120,
      calloutFee: 40,
      extraCharge: 0,
      materialsCost: 70,
      travelCost: 20,
      invoiceStatus: "InvoiceSent",
      workSummary: "",
      paymentDueDate: null,
      reminderDate: null,
      reminderNote: "",
    },
    {
      id: 2,
      clientName: "Northside Dental",
      jobName: "Drain line inspection",
      siteAddress: "88 Cedar Ave",
      date: "2026-04-06",
      startTime: "10:00",
      endTime: "13:00",
      laborRate: 90,
      pricingMode: "hourly",
      fixedPrice: 0,
      materialsBilled: 0,
      calloutFee: 0,
      extraCharge: 60,
      materialsCost: 25,
      travelCost: 10,
      invoiceStatus: "Paid",
      workSummary: "",
      paymentDueDate: null,
      reminderDate: null,
      reminderNote: "",
    },
  ];

  const snapshot = createDashboardSnapshot(jobs, "2026-04-08");

  assert.equal(snapshot.weekRevenue, 830);
  assert.equal(snapshot.weekProfit, 705);
  assert.equal(snapshot.unpaidAmount, 500);
  assert.equal(snapshot.topClient, "Acme Bakery");
});

test("validates hourly and fixed jobs like the Android draft model", () => {
  const draft = createDefaultDraft();
  const validation = validateDraft({
    ...draft,
    clientName: "Blue River HVAC",
    jobName: "Split unit repair",
    dateText: "2026-04-09",
    startTimeText: "08:30",
    endTimeText: "11:30",
    laborRateText: "95",
    materialsBilledText: "80",
    calloutFeeText: "35",
    extraChargeText: "15",
    materialsCostText: "45",
    travelCostText: "10",
    dueDateText: "2026-04-16",
    reminderDateText: "2026-04-14",
    reminderNote: "Confirm payment before Friday.",
  });

  assert.equal(validation.errorMessage, undefined);
  assert.equal(validation.job.invoiceTotal, 415);
  assert.equal(validation.job.estimatedProfit, 360);
  assert.equal(validation.job.reminderDate, "2026-04-14");

  const fixedValidation = validateDraft({
    ...jobToDraft(validation.job),
    pricingMode: "fixed",
    fixedPriceText: "520",
    laborRateText: "0",
  });

  assert.equal(fixedValidation.errorMessage, undefined);
  assert.equal(fixedValidation.job.laborTotal, 520);
  assert.equal(fixedValidation.job.invoiceTotal, 650);
});

test("formats money with the configured customer currency", () => {
  assert.equal(formatCurrency(1285.5, "USD"), "$1,285.50");
  assert.match(formatCurrency(1285.5, "EUR"), /1.285,50|1.285,50/);
});
