package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.billing.BillingUiState
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.DashboardSnapshot
import com.indie.shiftledger.model.JobRecord
import com.indie.shiftledger.model.WeeklyRevenue
import com.indie.shiftledger.model.formatCurrency
import com.indie.shiftledger.model.formatHours
import com.indie.shiftledger.model.formatShortDate
import com.indie.shiftledger.ui.theme.LedgerEmptyCard
import com.indie.shiftledger.ui.theme.LedgerHeroPanel
import com.indie.shiftledger.ui.theme.LedgerMetricTile
import com.indie.shiftledger.ui.theme.LedgerPanel
import com.indie.shiftledger.ui.theme.LedgerPill
import com.indie.shiftledger.ui.theme.LedgerSectionHeader

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    snapshot: DashboardSnapshot,
    recentJobs: List<JobRecord>,
    billing: BillingUiState,
    currency: CurrencyOption,
    jobCount: Int,
    remainingFreeEntries: Int,
    onOpenPro: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LedgerHeroPanel {
                LedgerPill(
                    label = "Week to date",
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                )
                Text(
                    text = formatCurrency(snapshot.weekRevenue, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    text = "Your live command view for billed work, profit, and open invoice pressure.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroPill(label = "${snapshot.followUpCount} open follow-ups")
                    HeroPill(label = "Top client ${snapshot.topClient}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroMetric(label = "Profit", value = formatCurrency(snapshot.weekProfit, currency))
                    HeroMetric(label = "Hours", value = formatHours(snapshot.weekHours))
                    HeroMetric(label = "Outstanding", value = formatCurrency(snapshot.unpaidAmount, currency))
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Classic scorecard",
                    body = "The four numbers that matter when you open the app between jobs.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerMetricTile(
                        label = "30-day billed",
                        value = formatCurrency(snapshot.monthRevenue, currency),
                        supporting = "Rolling month revenue",
                    )
                    LedgerMetricTile(
                        label = "Average job",
                        value = formatCurrency(snapshot.averageJobValue, currency),
                        supporting = "Useful when quoting similar work",
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerMetricTile(
                        label = "Week costs",
                        value = formatCurrency(snapshot.weekCosts, currency),
                        supporting = "Materials and travel booked this week",
                    )
                    LedgerMetricTile(
                        label = "Open pipeline",
                        value = "${snapshot.followUpCount} jobs",
                        supporting = "Quotes and invoices still to clear",
                    )
                }
            }
        }

        if (!billing.isPro) {
            item {
                LedgerPanel(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f),
                ) {
                    LedgerSectionHeader(
                        title = "Starter plan status",
                        body = "You are using the free ledger. Pro removes the 15-job cap and unlocks the full workflow.",
                        trailing = {
                            LedgerPill(
                                label = "$remainingFreeEntries left",
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            )
                        },
                    )
                    Text(
                        text = "$jobCount jobs logged so far. Upgrade before the cap interrupts capture on site.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onOpenPro, modifier = Modifier.fillMaxWidth()) {
                        Text("Review Pro plans")
                    }
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Four-week revenue line",
                    body = if (billing.isPro) {
                        "Compact revenue bars so you can see whether the book is tightening or building."
                    } else {
                        "Starter shows the framework. Pro unlocks the live four-week revenue view."
                    },
                )
                if (billing.isPro) {
                    TrendBoard(
                        trend = snapshot.trend,
                        currency = currency,
                    )
                } else {
                    LedgerEmptyCard(
                        title = "Trend view locked",
                        body = "Upgrade to Pro to track week-over-week movement and spot slowdowns before invoices stall.",
                    )
                }
            }
        }

        item {
            LedgerSectionHeader(
                title = "Latest work",
                body = if (recentJobs.isEmpty()) {
                    "Save your first job and this becomes a live ledger of recent activity."
                } else {
                    "Recent jobs with status, invoice amount, and time window in a cleaner ledger view."
                },
            )
        }

        if (recentJobs.isEmpty()) {
            item {
                LedgerEmptyCard(
                    title = "No work logged yet",
                    body = "Once a job is saved, ShiftLedger starts surfacing billed totals, margins, and due pressure automatically.",
                )
            }
        } else {
            items(recentJobs, key = { it.id }) { job ->
                RecentWorkCard(
                    job = job,
                    currency = currency,
                )
            }
        }
    }
}

@Composable
private fun HeroPill(
    label: String,
) {
    LedgerPill(
        label = label,
        containerColor = Color.White.copy(alpha = 0.16f),
        contentColor = Color.White,
    )
}

@Composable
private fun RowScope.HeroMetric(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun TrendBoard(
    trend: List<WeeklyRevenue>,
    currency: CurrencyOption,
) {
    val maxValue = trend.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        trend.forEach { week ->
            val ratio = (week.value / maxValue).toFloat().coerceIn(0f, 1f)
            val barHeight = (ratio * 120f).dp.coerceAtLeast(18.dp)
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .height(130.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }
                    Text(
                        text = week.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatCurrency(week.value, currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentWorkCard(
    job: JobRecord,
    currency: CurrencyOption,
) {
    LedgerPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = job.jobName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = job.clientName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            InvoiceStatusBadge(status = job.invoiceStatus)
        }

        Text(
            text = formatCurrency(job.invoiceTotal, currency),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Profit ${formatCurrency(job.estimatedProfit, currency)}  •  ${job.timeWindowLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatShortDate(job.date),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (job.workSummary.isNotBlank()) {
            Text(
                text = job.workSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
