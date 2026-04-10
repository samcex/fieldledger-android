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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    listState: LazyListState,
    contentPadding: PaddingValues,
    snapshot: DashboardSnapshot,
    recentJobs: List<JobRecord>,
    currency: CurrencyOption,
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
                    label = "This week",
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                )
                Text(
                    text = formatCurrency(snapshot.weekRevenue, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    text = "See what you billed this week, what is unpaid, and your latest jobs.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroPill(label = "${snapshot.followUpCount} unpaid")
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
                    title = "At a glance",
                    body = "The main numbers most people check first.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerMetricTile(
                        label = "This month",
                        value = formatCurrency(snapshot.monthRevenue, currency),
                        supporting = "Total billed in the last 30 days",
                    )
                    LedgerMetricTile(
                        label = "Average job",
                        value = formatCurrency(snapshot.averageJobValue, currency),
                        supporting = "Helpful when pricing similar work",
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerMetricTile(
                        label = "Costs",
                        value = formatCurrency(snapshot.weekCosts, currency),
                        supporting = "Materials and travel this week",
                    )
                    LedgerMetricTile(
                        label = "Unpaid jobs",
                        value = "${snapshot.followUpCount}",
                        supporting = "Jobs that still need follow-up",
                    )
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Last 4 weeks",
                    body = "A simple view of how your weekly totals are moving.",
                )
                TrendBoard(
                    trend = snapshot.trend,
                    currency = currency,
                )
            }
        }

        item {
            LedgerSectionHeader(
                title = "Recent jobs",
                body = if (recentJobs.isEmpty()) {
                    "Save your first job and it will show up here."
                } else {
                    "Your latest saved jobs."
                },
            )
        }

        if (recentJobs.isEmpty()) {
            item {
                LedgerEmptyCard(
                    title = "No jobs yet",
                    body = "Save your first job to see totals and unpaid jobs here.",
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
            text = "Profit ${formatCurrency(job.estimatedProfit, currency)}  •  ${job.scheduleSummary}",
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
