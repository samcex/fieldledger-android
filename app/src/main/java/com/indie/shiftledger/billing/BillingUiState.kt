package com.indie.shiftledger.billing

import com.indie.shiftledger.model.DisplayOffer

data class BillingUiState(
    val isLoading: Boolean = true,
    val isConnected: Boolean = false,
    val isPro: Boolean = false,
    val offers: List<DisplayOffer> = emptyList(),
    val statusMessage: String? = null,
    val verificationSource: String? = null,
    val verifiedExpiryTime: String? = null,
    val isVerificationConfigured: Boolean = false,
)
