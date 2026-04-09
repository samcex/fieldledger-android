package com.indie.shiftledger.model

import java.util.Currency
import java.util.Locale

enum class CurrencyOption(
    val code: String,
    val label: String,
    val locale: Locale,
) {
    USD(
        code = "USD",
        label = "US Dollar",
        locale = Locale.US,
    ),
    EUR(
        code = "EUR",
        label = "Euro",
        locale = Locale.GERMANY,
    ),
    GBP(
        code = "GBP",
        label = "British Pound",
        locale = Locale.UK,
    ),
    CAD(
        code = "CAD",
        label = "Canadian Dollar",
        locale = Locale.CANADA,
    ),
    AUD(
        code = "AUD",
        label = "Australian Dollar",
        locale = Locale.Builder()
            .setLanguage("en")
            .setRegion("AU")
            .build(),
    ),
    CHF(
        code = "CHF",
        label = "Swiss Franc",
        locale = Locale.Builder()
            .setLanguage("de")
            .setRegion("CH")
            .build(),
    ),
    INR(
        code = "INR",
        label = "Indian Rupee",
        locale = Locale.Builder()
            .setLanguage("en")
            .setRegion("IN")
            .build(),
    ),
    ;

    val currency: Currency
        get() = Currency.getInstance(code)

    val symbol: String
        get() = currency.getSymbol(locale)

    val displayLabel: String
        get() = "$code  $symbol  $label"

    companion object {
        fun fromCode(code: String?): CurrencyOption {
            return entries.firstOrNull { it.code == code } ?: USD
        }
    }
}
