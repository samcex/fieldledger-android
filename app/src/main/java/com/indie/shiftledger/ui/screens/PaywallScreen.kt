package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            HeroCard(billing = billing)
        }

        item {
            ValueCard()
        }

        items(MonetizationPlan.fallbackOffers, key = { it.productId }) { fallback ->
            val liveOffer = resolveOffer(fallback.productId)
            val offer = liveOffer ?: fallback
            OfferCard(
                offer = offer,
                enabled = !billing.isPro && offer.offerToken != null,
                highlighted = offer.productId == MonetizationPlan.yearlyProductId,
                onPurchase = { offer.offerToken?.let { onPurchase(offer) } },
            )
        }

        item {
            LaunchChecklistCard(
                billing = billing,
                onRefresh = onRefresh,
            )
        }
    }
}

@Composable
private fun HeroCard(
    billing: BillingUiState,
) {
    Card(
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                    shape = RoundedCornerShape(34.dp),
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.14f),
            ) {
                Text(
                    text = if (billing.isPro) "PRO ACTIVE" else "FIELDLEDGER PRO",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
            Text(
                text = if (billing.isPro) {
                    "The paid workflow is unlocked on this device."
                } else {
                    "Charge for time saved, not for vague analytics."
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                text = if (billing.isPro) {
                    "Keep shipping retention features that directly reduce admin drag."
                } else {
                    "Unlimited jobs, recurring follow-up tools, and invoice workflow are the reasons this subscription can justify itself."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f),
            )
            billing.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun ValueCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Why this can sell", style = MaterialTheme.typography.titleMedium)
            MonetizationPlan.featureBullets.forEach { feature ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                ) {
                    Text(
                        text = feature,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    offer: DisplayOffer,
    enabled: Boolean,
    highlighted: Boolean,
    onPurchase: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (highlighted) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "BEST VALUE",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            Text(text = offer.title, style = MaterialTheme.typography.titleMedium)
            Text(text = offer.price, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = when (offer.productId) {
                    MonetizationPlan.yearlyProductId -> "Best for operators who invoice every week and want the habit to stick."
                    MonetizationPlan.monthlyProductId -> "Lower commitment while you prove the workflow saves enough time to earn its keep."
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

@Composable
private fun LaunchChecklistCard(
    billing: BillingUiState,
    onRefresh: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Launch checklist", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (billing.isVerificationConfigured) {
                    "The backend verifier URL is configured. Play products still need to exist before purchase buttons can resolve to live offers."
                } else {
                    "Set FIELDLEDGER_BILLING_BACKEND_URL before you expect verified Pro unlocks to work."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            billing.verificationSource?.let { source ->
                Text(
                    text = "Verification source: $source",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onRefresh) {
                Text("Refresh Play products")
            }
        }
    }
}
