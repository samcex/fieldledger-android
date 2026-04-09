package com.indie.shiftledger.billing

import com.android.billingclient.api.Purchase
import com.indie.shiftledger.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BillingVerificationResult(
    val active: Boolean,
    val verificationSource: String? = null,
    val latestExpiryTime: String? = null,
    val statusMessage: String? = null,
)

class BillingVerificationClient {
    private val backendUrl = BuildConfig.BILLING_BACKEND_URL.trim().trimEnd('/')

    val isConfigured: Boolean
        get() = backendUrl.isNotBlank()

    fun verifySubscription(
        packageName: String,
        purchase: Purchase,
    ): BillingVerificationResult {
        if (!isConfigured) {
            return BillingVerificationResult(
                active = false,
                statusMessage = "Billing backend URL is not configured.",
            )
        }

        var connection: HttpURLConnection? = null
        return runCatching {
            connection = URL("$backendUrl/verify/google-play-subscription").openConnection() as HttpURLConnection
            val activeConnection = requireNotNull(connection)
            activeConnection.requestMethod = "POST"
            activeConnection.connectTimeout = 10_000
            activeConnection.readTimeout = 10_000
            activeConnection.doOutput = true
            activeConnection.setRequestProperty("Content-Type", "application/json")

            val payload = JSONObject()
                .put("packageName", packageName)
                .put("purchaseToken", purchase.purchaseToken)
                .put("orderId", purchase.orderId ?: JSONObject.NULL)
                .put(
                    "productIds",
                    JSONArray().apply {
                        purchase.products.forEach(::put)
                    },
                )

            activeConnection.outputStream.bufferedWriter().use { writer ->
                writer.write(payload.toString())
            }

            val body = (if (activeConnection.responseCode in 200..299) {
                activeConnection.inputStream
            } else {
                activeConnection.errorStream
            })?.bufferedReader()?.use { it.readText() }.orEmpty()

            val json = body.takeIf(String::isNotBlank)?.let(::JSONObject) ?: JSONObject()
            BillingVerificationResult(
                active = json.optBoolean("active", false),
                verificationSource = json.optString("source").takeIf(String::isNotBlank),
                latestExpiryTime = json.optString("latestExpiryTime").takeIf(String::isNotBlank),
                statusMessage = json.optString("message").takeIf(String::isNotBlank)
                    ?: json.optString("subscriptionState").takeIf(String::isNotBlank)
                    ?: if (activeConnection.responseCode !in 200..299) {
                        "Verification failed with HTTP ${activeConnection.responseCode}."
                    } else {
                        null
                    },
            )
        }.getOrElse { error ->
            BillingVerificationResult(
                active = false,
                statusMessage = error.message ?: "Could not verify the purchase with the backend.",
            )
        }.also {
            connection?.disconnect()
        }
    }
}
