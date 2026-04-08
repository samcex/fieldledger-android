package com.indie.shiftledger.data

import android.content.Context
import com.indie.shiftledger.model.CurrencyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _currency = MutableStateFlow(
        CurrencyOption.fromCode(
            prefs.getString(KEY_CURRENCY_CODE, CurrencyOption.USD.code),
        ),
    )
    private val _onboardingComplete = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
    )

    val currency: StateFlow<CurrencyOption> = _currency.asStateFlow()
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    fun updateCurrency(currency: CurrencyOption) {
        if (_currency.value == currency) return

        prefs.edit()
            .putString(KEY_CURRENCY_CODE, currency.code)
            .apply()

        _currency.value = currency
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
    }
}
