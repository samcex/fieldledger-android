package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.billing.BillingUiState
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.DashboardSnapshot
import com.indie.shiftledger.model.JobRecord
import com.indie.shiftledger.model.formatCurrency
import com.indie.shiftledger.model.formatHours
import com.indie.shiftledger.model.formatShortDate

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
            RevenueHero(snapshot = snapshot, currency = currency)
        }

        item {
            PulseBoard(snapshot = snapshot, currency = currency)
        }

        if (!billing.isPro) {
            item {
                UpgradePromptCard(
                    jobCount = jobCount,
                    remainingFreeEntries = remainingFreeEntries,
                    onOpenPro = onOpenPro,
                )
            }
        }

        item {
            TrendCard(
                snapshot = snapshot,
                currency = currency,
                locked = !billing.isPro,
                onOpenPro = onOpenPro,
            )
        }

        item {
            SectionHeader(
                title = "Latest work",
                subtitle = if (recentJobs.isEmpty()) {
                    "Save the first job and this screen turns into a working ledger."
                } else {
                    "The newest jobs with invoice value, status, and due pressure in one glance."
                },
            )
        }

        if (recentJobs.isEmpty()) {
            item {
                InfoCard(
                    title = "Nothing logged yet",
                    body = "Once you save a job, this screen starts surfacing billed totals, open follow-ups, and weekly momentum.",
                )
            }
        } else {
            items(recentJobs, key = { it.id }) { job ->
                RecentJobRow(job = job, currency = currency)
            }
        }
    }
}

@Composable
private fun RevenueHero(
    snapshot: DashboardSnapshot,
    currency: CurrencyOption,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
                shape = RoundedCornerShape(32.dp),
            )
            .padding(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "THIS WEEK",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.78f),
            )
            Text(
                text = formatCurrency(snapshot.weekRevenue, currency),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Text(
                text = "Week-to-date billed work, with unpaid pressure and margin visible below.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTag(label = "Profit ${formatCurrency(snapshot.weekProfit, currency)}")
                HeroTag(label = "${snapshot.followUpCount} follow-ups")
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.14f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HeroMetric(label = "Outstanding", value = formatCurrency(snapshot.unpaidAmount, currency))
                    HeroMetric(label = "Top client", value = snapshot.topClient)
                    HeroMetric(label = "Hours", value = formatHours(snapshot.weekHours))
                }
            }
        }
    }
}

@Composable
private fun HeroTag(
    label: String,
) {
    Surface(
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.width(92.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun PulseBoard(
    snapshot: DashboardSnapshot,
    currency: CurrencyOption,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PulseTile(
                label = "30-day billed",
                value = formatCurrency(snapshot.monthRevenue, currency),
                detail = "Recent volume across every saved job.",
                containerColor = MaterialTheme.colorScheme.surface,
            )
            PulseTile(
                label = "Average job",
                value = formatCurrency(snapshot.averageJobValue, currency),
                detail = "Useful when you quote similar work.",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PulseTile(
                label = "Week costs",
                value = formatCurrency(snapshot.weekCosts, currency),
                detail = "Materials and travel eating into margin.",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            )
            PulseTile(
                label = "Open pressure",
                value = "${snapshot.followUpCount} jobs",
                detail = "Outstanding invoices and quotes still in play.",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            )
        }
    }
}

@Composable
private fun RowScope.PulseTile(
    label: String,
    value: String,
    detail: String,
    containerColor: Color,
) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UpgradePromptCard(
    jobCount: Int,
    remainingFreeEntries: Int,
    onOpenPro: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Starter plan runway", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "You have logged $jobCount jobs. $remainingFreeEntries free jobs remain before unlimited logging moves behind Pro.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onOpenPro) {
                Text("See Pro pricing")
            }
        }
    }
}

@Composable
private fun TrendCard(
    snapshot: DashboardSnapshot,
    currency: CurrencyOption,
    locked: Boolean,
    onOpenPro: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Four-week momentum", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "A compact revenue view so you can tell whether the pipeline is tightening or climbing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (locked) {
                Text(
                    text = "Unlock trend history and deeper outstanding analytics with Pro.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onOpenPro) {
                    Text("Unlock Pro")
                }
            } else {
                TrendChart(snapshot = snapshot, currency = currency)
            }
        }
    }
}

@Composable
private fun TrendChart(
    snapshot: DashboardSnapshot,
    currency: CurrencyOption,
) {
    val maxValue = snapshot.trend.maxOfOrNull { it.value.coerceAtLeast(0.0) }?.takeIf { it > 0.0 } ?: 1.0
    val barColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        snapshot.trend.forEach { week ->
            val ratio = (week.value / maxValue).toFloat().coerceIn(0.12f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = formatCurrency(week.value, currency),
                    style = MaterialTheme.typography.labelSmall,
                )
                Canvas(
                    modifier = Modifier
                        .width(48.dp)
                        .height((120 * ratio).dp),
                ) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = barColors,
                        ),
                        cornerRadius = CornerRadius(26f, 26f),
                    )
                }
                Text(text = week.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RecentJobRow(
    job: JobRecord,
    currency: CurrencyOption,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                text = "${formatShortDate(job.date)}  •  ${job.timeWindowLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Estimated profit ${formatCurrency(job.estimatedProfit, currency)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
