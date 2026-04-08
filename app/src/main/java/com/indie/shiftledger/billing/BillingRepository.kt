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
import com.indie.shiftledger.model.DisplayOffer
import com.indie.shiftledger.model.MonetizationPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BillingRepository(
    context: Context,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val productCache = mutableMapOf<String, ProductDetails>()
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

    private val _uiState = MutableStateFlow(BillingUiState())
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

            val hasActivePro = purchases.any { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.any { it in supportedProductIds }
            }

            purchases
                .filter { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        !purchase.isAcknowledged &&
                        purchase.products.any { it in supportedProductIds }
                }
                .forEach(::acknowledgePurchase)

            _uiState.update { state ->
                state.copy(
                    isPro = hasActivePro,
                    statusMessage = if (hasActivePro) "Pro subscription active." else state.statusMessage,
                )
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
                purchases.orEmpty().forEach(::handlePurchase)
                refreshEntitlements()
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
    }

    private fun connect() {
        _uiState.update { it.copy(isLoading = true) }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _uiState.update { it.copy(isConnected = true, isLoading = false) }
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

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.products.none { it in supportedProductIds }) return

        if (!purchase.isAcknowledged) {
            acknowledgePurchase(purchase)
        }

        _uiState.update {
            it.copy(isPro = true, statusMessage = "Pro unlocked on this device.")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { _ ->
            // MVP note: production apps should verify purchases on a secure backend.
        }
    }

    private val supportedProductIds: List<String>
        get() = listOf(
            MonetizationPlan.monthlyProductId,
            MonetizationPlan.yearlyProductId,
        )
}
