package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.billing.BillingUiState
import com.indie.shiftledger.model.DisplayOffer
import com.indie.shiftledger.model.MonetizationPlan

@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    billing: BillingUiState,
    resolveOffer: (String) -> DisplayOffer?,
    onRefresh: () -> Unit,
    onPurchase: (DisplayOffer) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "FieldLedger Pro", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = if (billing.isPro) {
                            "Pro is active on this device. Keep shipping retention features that save real admin time."
                        } else {
                            "Sell recurring admin relief: unlimited jobs, deeper revenue visibility, and invoice workflow tools."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    billing.statusMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "Why users pay", style = MaterialTheme.typography.titleMedium)
                    MonetizationPlan.featureBullets.forEach { feature ->
                        Text(text = "• $feature", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        items(MonetizationPlan.fallbackOffers, key = { it.productId }) { fallback ->
            val liveOffer = resolveOffer(fallback.productId)
            val offer = liveOffer ?: fallback
            OfferCard(
                offer = offer,
                enabled = !billing.isPro && offer.offerToken != null,
                onPurchase = { offer.offerToken?.let { onPurchase(offer) } },
            )
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "Before launch", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Google Play subscription products must exist before purchase buttons will activate. Refresh after creating the matching test products in Play Console.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onRefresh) {
                            Text("Refresh Play products")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    offer: DisplayOffer,
    enabled: Boolean,
    onPurchase: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = offer.title, style = MaterialTheme.typography.titleMedium)
            Text(text = offer.price, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = when (offer.productId) {
                    MonetizationPlan.yearlyProductId -> "Best for operators running weekly invoices and repeat customers."
                    MonetizationPlan.monthlyProductId -> "Lower commitment while proving the job logging workflow."
                    else -> offer.description
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onPurchase,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (enabled) "Upgrade with Google Play" else "Waiting for Play setup")
            }
        }
    }
}
