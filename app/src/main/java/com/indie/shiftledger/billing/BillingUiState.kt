package com.indie.shiftledger.billing

import com.indie.shiftledger.model.DisplayOffer

data class BillingUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val isPro: Boolean = true,
    val offers: List<DisplayOffer> = emptyList(),
    val statusMessage: String? = "All features are currently free.",
    val verificationSource: String? = null,
    val verifiedExpiryTime: String? = null,
    val isVerificationConfigured: Boolean = false,
)
