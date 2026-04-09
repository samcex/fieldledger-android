package com.indie.shiftledger.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.indie.shiftledger.data.SettingsRepository
import com.indie.shiftledger.model.DisplayOffer
import com.indie.shiftledger.model.MonetizationPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingRepository(
    context: Context,
    private val settingsRepository: SettingsRepository,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val verificationClient = BillingVerificationClient()
    private val productCache = mutableMapOf<String, ProductDetails>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var hasStarted = false

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    private val _uiState = MutableStateFlow(
        BillingUiState(
            isVerificationConfigured = verificationClient.isConfigured,
        ),
    )
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    fun start() {
        if (hasStarted) return
        hasStarted = true
        connect()
    }

    fun refreshEntitlements() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _uiState.update {
                    it.copy(statusMessage = billingResult.debugMessage.takeIf(String::isNotBlank))
                }
                return@queryPurchasesAsync
            }

            scope.launch {
                verifyPurchases(purchases)
            }
        }
    }

    fun launchPurchase(activity: Activity, offer: DisplayOffer) {
        val productDetails = productCache[offer.productId] ?: run {
            _uiState.update {
                it.copy(statusMessage = "Play products are not loaded yet.")
            }
            return
        }
        val offerToken = offer.offerToken ?: run {
            _uiState.update {
                it.copy(statusMessage = "Live Play subscription details are not available yet.")
            }
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingParams = BillingFlowParams.newBuilder()
            .setObfuscatedAccountId(settingsRepository.installationId)
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val launchResult = billingClient.launchBillingFlow(activity, billingParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _uiState.update {
                it.copy(statusMessage = launchResult.debugMessage.takeIf(String::isNotBlank))
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    verifyPurchases(purchases.orEmpty())
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiState.update {
                    it.copy(statusMessage = "Purchase canceled.")
                }
            }

            else -> {
                _uiState.update {
                    it.copy(statusMessage = billingResult.debugMessage.takeIf(String::isNotBlank))
                }
            }
        }
    }

    fun close() {
        billingClient.endConnection()
        scope.cancel()
    }

    private fun connect() {
        _uiState.update {
            it.copy(
                isLoading = true,
                isVerificationConfigured = verificationClient.isConfigured,
            )
        }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _uiState.update { state ->
                            state.copy(
                                isConnected = true,
                                isLoading = false,
                                statusMessage = if (!verificationClient.isConfigured) {
                                    "Set FIELDLEDGER_BILLING_BACKEND_URL before granting Pro."
                                } else {
                                    state.statusMessage
                                },
                            )
                        }
                        loadOffers()
                        refreshEntitlements()
                    } else {
                        _uiState.update {
                            it.copy(
                                isConnected = false,
                                isLoading = false,
                                statusMessage = billingResult.debugMessage.takeIf(String::isNotBlank),
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _uiState.update { it.copy(isConnected = false) }
                }
            },
        )
    }

    private fun loadOffers() {
        val productList = supportedProductIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _uiState.update {
                    it.copy(statusMessage = billingResult.debugMessage.takeIf(String::isNotBlank))
                }
                return@queryProductDetailsAsync
            }

            productCache.clear()
            val liveOffers = result.productDetailsList
                .mapNotNull(::toDisplayOffer)
                .sortedByDescending { it.productId == MonetizationPlan.yearlyProductId }

            _uiState.update {
                it.copy(
                    offers = liveOffers,
                    statusMessage = if (liveOffers.isEmpty()) {
                        "Connect Play test products to enable purchases."
                    } else {
                        it.statusMessage
                    },
                )
            }
        }
    }

    private suspend fun verifyPurchases(purchases: List<Purchase>) {
        val supportedPurchases = purchases.filter { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it in supportedProductIds }
        }

        if (supportedPurchases.isEmpty()) {
            _uiState.update {
                it.copy(
                    isPro = false,
                    verificationSource = null,
                    verifiedExpiryTime = null,
                    statusMessage = if (!verificationClient.isConfigured) {
                        "Set FIELDLEDGER_BILLING_BACKEND_URL before granting Pro."
                    } else {
                        "No verified active subscription found."
                    },
                )
            }
            return
        }

        if (!verificationClient.isConfigured) {
            _uiState.update {
                it.copy(
                    isPro = false,
                    verificationSource = null,
                    verifiedExpiryTime = null,
                    statusMessage = "Billing backend URL is not configured.",
                )
            }
            return
        }

        var activeResult: BillingVerificationResult? = null
        var lastResult: BillingVerificationResult? = null

        supportedPurchases.forEach { purchase ->
            val result = verificationClient.verifySubscription(
                packageName = appContext.packageName,
                purchase = purchase,
            )
            lastResult = result

            if (result.active) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                activeResult = result
                return@forEach
            }
        }

        val winningResult = activeResult ?: lastResult
        _uiState.update {
            it.copy(
                isPro = winningResult?.active == true,
                verificationSource = winningResult?.verificationSource,
                verifiedExpiryTime = winningResult?.latestExpiryTime,
                statusMessage = when {
                    winningResult?.active == true -> "Pro verified by backend."
                    winningResult != null && !winningResult.statusMessage.isNullOrBlank() -> winningResult.statusMessage
                    else -> "No verified active subscription found."
                },
            )
        }
    }

    private fun toDisplayOffer(product: ProductDetails): DisplayOffer? {
        val subscriptionOffer = product.subscriptionOfferDetails
            ?.firstOrNull()
            ?: return null

        productCache[product.productId] = product

        val billingPhase = subscriptionOffer.pricingPhases.pricingPhaseList.lastOrNull()
            ?: return null

        return when (product.productId) {
            MonetizationPlan.yearlyProductId -> DisplayOffer(
                productId = product.productId,
                title = "Pro Yearly",
                description = "Best value for solo operators who invoice every week",
                price = "${billingPhase.formattedPrice} / year",
                offerToken = subscriptionOffer.offerToken,
            )

            MonetizationPlan.monthlyProductId -> DisplayOffer(
                productId = product.productId,
                title = "Pro Monthly",
                description = "Lower commitment while testing the workflow",
                price = "${billingPhase.formattedPrice} / month",
                offerToken = subscriptionOffer.offerToken,
            )

            else -> null
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _uiState.update {
                    it.copy(statusMessage = result.debugMessage.takeIf(String::isNotBlank))
                }
            }
        }
    }

    private val supportedProductIds: List<String>
        get() = listOf(
            MonetizationPlan.monthlyProductId,
            MonetizationPlan.yearlyProductId,
        )
}
