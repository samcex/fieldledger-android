package com.indie.shiftledger.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

data class JobDraft(
    val clientName: String = "",
    val jobName: String = "",
    val siteAddress: String = "",
    val dateText: String = LocalDate.now().toString(),
    val startTimeText: String = "08:00",
    val endTimeText: String = "12:00",
    val laborRateText: String = "85",
    val materialsBilledText: String = "0",
    val calloutFeeText: String = "0",
    val extraChargeText: String = "0",
    val materialsCostText: String = "0",
    val travelCostText: String = "0",
    val status: InvoiceStatus = InvoiceStatus.DraftQuote,
    val workSummary: String = "",
)

data class DraftValidation(
    val job: JobRecord? = null,
    val errorMessage: String? = null,
)

data class JobDraftPreview(
    val hours: Double,
    val laborTotal: Double,
    val invoiceTotal: Double,
    val totalCosts: Double,
    val estimatedProfit: Double,
)

fun JobDraft.preview(): JobDraftPreview {
    val laborRate = laborRateText.toDoubleOrNull() ?: 0.0
    val materialsBilled = materialsBilledText.toDoubleOrNull() ?: 0.0
    val calloutFee = calloutFeeText.toDoubleOrNull() ?: 0.0
    val extraCharge = extraChargeText.toDoubleOrNull() ?: 0.0
    val materialsCost = materialsCostText.toDoubleOrNull() ?: 0.0
    val travelCost = travelCostText.toDoubleOrNull() ?: 0.0
    val hours = safeHours(startTimeText, endTimeText)
    val laborTotal = hours * laborRate
    val invoiceTotal = laborTotal + materialsBilled + calloutFee + extraCharge
    val totalCosts = materialsCost + travelCost

    return JobDraftPreview(
        hours = hours,
        laborTotal = laborTotal,
        invoiceTotal = invoiceTotal,
        totalCosts = totalCosts,
        estimatedProfit = invoiceTotal - totalCosts,
    )
}

fun JobDraft.validate(): DraftValidation {
    val client = clientName.trim()
    if (client.isEmpty()) {
        return DraftValidation(errorMessage = "Add the customer name.")
    }

    val jobName = jobName.trim()
    if (jobName.isEmpty()) {
        return DraftValidation(errorMessage = "Add the job name or service.")
    }

    val date = try {
        LocalDate.parse(dateText.trim())
    } catch (_: DateTimeParseException) {
        return DraftValidation(errorMessage = "Date must use YYYY-MM-DD.")
    }

    val start = try {
        LocalTime.parse(startTimeText.trim())
    } catch (_: DateTimeParseException) {
        return DraftValidation(errorMessage = "Start time must use HH:MM.")
    }

    val end = try {
        LocalTime.parse(endTimeText.trim())
    } catch (_: DateTimeParseException) {
        return DraftValidation(errorMessage = "End time must use HH:MM.")
    }

    if (!end.isAfter(start)) {
        return DraftValidation(errorMessage = "End time must be after start time.")
    }

    val laborRate = laborRateText.toDoubleOrNull()
    if (laborRate == null || laborRate <= 0) {
        return DraftValidation(errorMessage = "Labor rate must be greater than zero.")
    }

    return DraftValidation(
        job = JobRecord(
            clientName = client,
            jobName = jobName,
            siteAddress = siteAddress.trim(),
            date = date,
            startTime = start,
            endTime = end,
            laborRate = laborRate,
            materialsBilled = materialsBilledText.toDoubleOrNull() ?: 0.0,
            calloutFee = calloutFeeText.toDoubleOrNull() ?: 0.0,
            extraCharge = extraChargeText.toDoubleOrNull() ?: 0.0,
            materialsCost = materialsCostText.toDoubleOrNull() ?: 0.0,
            travelCost = travelCostText.toDoubleOrNull() ?: 0.0,
            invoiceStatus = status,
            workSummary = workSummary.trim(),
        ),
    )
}

private fun safeHours(startTime: String, endTime: String): Double {
    val start = runCatching { LocalTime.parse(startTime.trim()) }.getOrNull() ?: return 0.0
    val end = runCatching { LocalTime.parse(endTime.trim()) }.getOrNull() ?: return 0.0
    if (!end.isAfter(start)) return 0.0
    return java.time.Duration.between(start, end).toMinutes() / 60.0
}
