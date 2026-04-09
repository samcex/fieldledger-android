package com.indie.shiftledger.model

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max

data class JobRecord(
    val id: Long = 0L,
    val clientName: String,
    val jobName: String,
    val siteAddress: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val laborRate: Double,
    val materialsBilled: Double,
    val calloutFee: Double,
    val extraCharge: Double,
    val materialsCost: Double,
    val travelCost: Double,
    val invoiceStatus: InvoiceStatus,
    val workSummary: String,
    val paymentDueDate: LocalDate? = null,
    val reminderDate: LocalDate? = null,
    val reminderNote: String = "",
) {
    val durationMinutes: Long
        get() = max(0, java.time.Duration.between(startTime, endTime).toMinutes())

    val durationHours: Double
        get() = durationMinutes / 60.0

    val laborTotal: Double
        get() = durationHours * laborRate

    val invoiceTotal: Double
        get() = laborTotal + materialsBilled + calloutFee + extraCharge

    val totalCosts: Double
        get() = materialsCost + travelCost

    val estimatedProfit: Double
        get() = invoiceTotal - totalCosts

    val averageHourlyProfit: Double
        get() = if (durationHours == 0.0) 0.0 else estimatedProfit / durationHours

    val timeWindowLabel: String
        get() = "${formatClock(startTime)} - ${formatClock(endTime)}"

    val headline: String
        get() = "$jobName for $clientName"

    val hasReminder: Boolean
        get() = invoiceStatus.isOutstanding && reminderDate != null

    val reminderMessage: String
        get() = reminderNote.ifBlank {
            "Invoice follow-up for $clientName on $jobName."
        }
}

enum class InvoiceStatus(
    val label: String,
    val isOutstanding: Boolean,
) {
    DraftQuote(label = "Needs quote", isOutstanding = true),
    InvoiceSent(label = "Invoice sent", isOutstanding = true),
    Paid(label = "Paid", isOutstanding = false),
}
