package com.indie.shiftledger.model

enum class PricingMode(
    val label: String,
    val storageValue: String,
    val inputLabel: String,
    val summaryLabel: String,
) {
    Hourly(
        label = "Hourly",
        storageValue = "hourly",
        inputLabel = "Hourly rate",
        summaryLabel = "Labor",
    ),
    Fixed(
        label = "Fixed price",
        storageValue = "fixed",
        inputLabel = "Job price",
        summaryLabel = "Job price",
    ),
    ;

    companion object {
        fun fromStorageValue(rawValue: String?): PricingMode {
            return entries.firstOrNull { mode ->
                mode.storageValue.equals(rawValue, ignoreCase = true) ||
                    mode.name.equals(rawValue, ignoreCase = true)
            } ?: Hourly
        }
    }
}
