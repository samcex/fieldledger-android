package com.indie.shiftledger.billing

import android.app.Activity
import android.content.Context
import com.indie.shiftledger.data.SettingsRepository
import com.indie.shiftledger.model.DisplayOffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingRepository(
    context: Context,
    settingsRepository: SettingsRepository,
) {
    @Suppress("unused")
    private val appContext = context.applicationContext

    @Suppress("unused")
    private val settings = settingsRepository

    private val _uiState = MutableStateFlow(
        BillingUiState(
            isLoading = false,
            isConnected = false,
            isPro = true,
            statusMessage = "All features are currently free.",
            isVerificationConfigured = false,
        ),
    )
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    fun start() = Unit

    @Suppress("unused")
    fun refreshEntitlements() = Unit

    @Suppress("unused")
    fun launchPurchase(activity: Activity, offer: DisplayOffer) = Unit

    fun close() = Unit
}
