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

    val currency: StateFlow<CurrencyOption> = _currency.asStateFlow()

    fun updateCurrency(currency: CurrencyOption) {
        if (_currency.value == currency) return

        prefs.edit()
            .putString(KEY_CURRENCY_CODE, currency.code)
            .apply()

        _currency.value = currency
    }

    private companion object {
        const val PREFS_NAME = "field-ledger-settings"
        const val KEY_CURRENCY_CODE = "currency_code"
    }
}
