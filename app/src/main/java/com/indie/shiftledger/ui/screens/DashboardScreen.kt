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
import androidx.compose.material3.ButtonDefaults
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
            HeroCard(snapshot = snapshot, currency = currency)
        }

        item {
            MetricsGrid(snapshot = snapshot, currency = currency)
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
                title = "Recent jobs",
                subtitle = if (recentJobs.isEmpty()) {
                    "Your latest jobs will show here as soon as you save one."
                } else {
                    "The last ${recentJobs.size} jobs with invoice status and profit at a glance."
                },
            )
        }

        if (recentJobs.isEmpty()) {
            item {
                InfoCard(
                    title = "Nothing logged yet",
                    body = "Add the first job and the app will start showing weekly billed totals, profit, and open invoice follow-ups.",
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
private fun HeroCard(
    snapshot: DashboardSnapshot,
    currency: CurrencyOption,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
                shape = RoundedCornerShape(30.dp),
            )
            .padding(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "This week billed",
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = formatCurrency(snapshot.weekRevenue, currency),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "Invoice-ready total across saved jobs, shown in ${currency.code}.",
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroPill(label = "Profit", value = formatCurrency(snapshot.weekProfit, currency))
                HeroPill(label = "Follow-ups", value = "${snapshot.followUpCount} open")
            }
        }
    }
}

@Composable
private fun HeroPill(
    label: String,
    value: String,
) {
    Surface(
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.74f),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(text = value, color = Color.White, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun MetricsGrid(
    snapshot: DashboardSnapshot,
    currency: CurrencyOption,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label = "30-day billed",
                value = formatCurrency(snapshot.monthRevenue, currency),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
            MetricTile(
                label = "Average job",
                value = formatCurrency(snapshot.averageJobValue, currency),
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label = "Outstanding",
                value = formatCurrency(snapshot.unpaidAmount, currency),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            )
            MetricTile(
                label = "Hours this week",
                value = formatHours(snapshot.weekHours),
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Top customer", style = MaterialTheme.typography.labelLarge)
                    Text(text = snapshot.topClient, style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = "Costs ${formatCurrency(snapshot.weekCosts, currency)}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.MetricTile(
    label: String,
    value: String,
    containerColor: Color,
) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(24.dp),
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Free plan status", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "You have logged $jobCount jobs. $remainingFreeEntries free jobs remain before Pro is required.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onOpenPro,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) {
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "4-week billed trend", style = MaterialTheme.typography.titleMedium)
            if (locked) {
                Text(
                    text = "Upgrade to Pro to unlock multi-week revenue trends and outstanding invoice analytics.",
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
    val maxValue = snapshot.trend.maxOfOrNull { it.value.coerceAtLeast(0.0) }?.takeIf { it > 0 } ?: 1.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        snapshot.trend.forEach { week ->
            val ratio = (week.value / maxValue).toFloat().coerceIn(0.12f, 1f)
            val barColor = MaterialTheme.colorScheme.secondary
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
                        color = barColor,
                        cornerRadius = CornerRadius(22f, 22f),
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = job.jobName, style = MaterialTheme.typography.titleSmall)
                    Text(text = job.clientName, style = MaterialTheme.typography.bodySmall)
                }
                InvoiceStatusBadge(status = job.invoiceStatus)
            }
            Text(
                text = "${formatShortDate(job.date)}  •  ${job.timeWindowLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Invoice ${formatCurrency(job.invoiceTotal, currency)}  •  Profit ${formatCurrency(job.estimatedProfit, currency)}",
                style = MaterialTheme.typography.bodySmall,
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
        shape = RoundedCornerShape(22.dp),
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
