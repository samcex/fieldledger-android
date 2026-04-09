package com.indie.shiftledger.model

enum class ThemeMode(
    val storageValue: String,
    val label: String,
    val isDark: Boolean,
) {
    Light(
        storageValue = "light",
        label = "Light",
        isDark = false,
    ),
    AmoledDark(
        storageValue = "amoled_dark",
        label = "Dark",
        isDark = true,
    ),
    ;

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: Light
        }
    }
}
