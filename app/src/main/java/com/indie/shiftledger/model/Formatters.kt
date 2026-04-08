package com.indie.shiftledger.model

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

fun formatCurrency(
    amount: Double,
    currency: CurrencyOption,
): String {
    return NumberFormat.getCurrencyInstance(currency.locale)
        .apply {
            this.currency = currency.currency
            maximumFractionDigits = 2
        }
        .format(amount)
}

fun formatHours(hours: Double): String = String.format(Locale.US, "%.1f h", hours)

fun formatShortDate(date: LocalDate): String = dateFormatter.format(date)

fun formatClock(time: LocalTime): String = timeFormatter.format(time)

fun formatStatus(status: InvoiceStatus): String = status.label
