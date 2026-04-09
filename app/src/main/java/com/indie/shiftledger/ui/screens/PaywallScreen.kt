package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.billing.BillingUiState
import com.indie.shiftledger.model.DisplayOffer
import com.indie.shiftledger.model.MonetizationPlan
import com.indie.shiftledger.ui.theme.LedgerHeroPanel
import com.indie.shiftledger.ui.theme.LedgerPanel
import com.indie.shiftledger.ui.theme.LedgerPill
import com.indie.shiftledger.ui.theme.LedgerSectionHeader

@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    contentPadding: PaddingValues,
    billing: BillingUiState,
    resolveOffer: (String) -> DisplayOffer?,
    onRefresh: () -> Unit,
    onPurchase: (DisplayOffer) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LedgerHeroPanel {
                LedgerPill(
                    label = if (billing.isPro) "Pro active" else "Subscription",
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                )
                Text(
                    text = if (billing.isPro) {
                        "The paid workflow is unlocked on this device."
                    } else {
                        "Upgrade when the workflow saves enough admin time to earn its keep."
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = if (billing.isPro) {
                        "Unlimited capture, cleaner follow-up, and a tool that can stay open all week."
                    } else {
                        "Unlimited jobs, recurring follow-up tools, and a fuller revenue view are what this upgrade is for."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
                billing.statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.84f),
                    )
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "What Pro adds",
                    body = "Three reasons the subscription exists in the product at all.",
                )
                MonetizationPlan.featureBullets.forEach { feature ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ) {
                        Text(
                            text = feature,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
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
                highlighted = offer.productId == MonetizationPlan.yearlyProductId,
                onPurchase = { offer.offerToken?.let { onPurchase(offer) } },
            )
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Launch checklist",
                    body = if (billing.isVerificationConfigured) {
                        "Verification is configured. Play products still need to resolve before purchase buttons can go live."
                    } else {
                        "Set FIELDLEDGER_BILLING_BACKEND_URL before expecting verified Pro unlocks to work."
                    },
                )
                billing.verificationSource?.let { source ->
                    Text(
                        text = "Verification source: $source",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh Play products")
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
    LedgerPanel(
        containerColor = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        borderColor = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (highlighted) {
                LedgerPill(
                    label = "Best value",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Text(text = offer.title, style = MaterialTheme.typography.titleMedium)
        Text(text = offer.price, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = when (offer.productId) {
                MonetizationPlan.yearlyProductId -> "Best for operators who invoice every week and want the habit to stick."
                MonetizationPlan.monthlyProductId -> "Lower commitment while you prove the workflow saves enough time to justify itself."
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
