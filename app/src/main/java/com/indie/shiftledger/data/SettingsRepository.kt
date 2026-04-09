package com.indie.shiftledger.data

import android.content.Context
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SettingsRepository(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val installationId: String = prefs.getString(KEY_INSTALLATION_ID, null)
        ?: UUID.randomUUID().toString().also { generatedId ->
            prefs.edit()
                .putString(KEY_INSTALLATION_ID, generatedId)
                .apply()
        }

    private val _currency = MutableStateFlow(
        CurrencyOption.fromCode(
            prefs.getString(KEY_CURRENCY_CODE, CurrencyOption.USD.code),
        ),
    )
    private val _onboardingComplete = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
    )
    private val _themeMode = MutableStateFlow(
        ThemeMode.fromStorageValue(
            prefs.getString(KEY_THEME_MODE, ThemeMode.Light.storageValue),
        ),
    )

    val currency: StateFlow<CurrencyOption> = _currency.asStateFlow()
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun updateCurrency(currency: CurrencyOption) {
        if (_currency.value == currency) return

        prefs.edit()
            .putString(KEY_CURRENCY_CODE, currency.code)
            .apply()

        _currency.value = currency
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        if (_themeMode.value == themeMode) return

        prefs.edit()
            .putString(KEY_THEME_MODE, themeMode.storageValue)
            .apply()

        _themeMode.value = themeMode
    }

    fun completeOnboarding() {
        if (_onboardingComplete.value) return

        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .apply()

        _onboardingComplete.value = true
    }

    private companion object {
        const val PREFS_NAME = "field-ledger-settings"
        const val KEY_CURRENCY_CODE = "currency_code"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_INSTALLATION_ID = "installation_id"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
