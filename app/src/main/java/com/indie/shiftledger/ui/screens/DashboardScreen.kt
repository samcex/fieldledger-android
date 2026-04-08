package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            HeroCard(snapshot = snapshot)
        }

        item {
            MetricsCard(snapshot = snapshot)
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
            TrendCard(snapshot = snapshot, locked = !billing.isPro, onOpenPro = onOpenPro)
        }

        item {
            Text(
                text = "Recent jobs",
                style = MaterialTheme.typography.titleMedium,
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
                RecentJobRow(job)
            }
        }
    }
}

@Composable
private fun HeroCard(snapshot: DashboardSnapshot) {
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
                shape = RoundedCornerShape(28.dp),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "This week billed",
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = formatCurrency(snapshot.weekRevenue),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "Invoice-ready total across saved jobs",
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroPill(label = "Profit", value = formatCurrency(snapshot.weekProfit))
                HeroPill(label = "Follow-ups", value = "${snapshot.followUpCount} open")
            }
        }
    }
}

@Composable
private fun HeroPill(label: String, value: String) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
            Text(text = value, color = Color.White, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun MetricsCard(snapshot: DashboardSnapshot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "Core metrics", style = MaterialTheme.typography.titleMedium)
            MetricRow("Average 30-day job value", formatCurrency(snapshot.averageJobValue))
            MetricRow("30-day billed", formatCurrency(snapshot.monthRevenue))
            MetricRow("Outstanding invoices", formatCurrency(snapshot.unpaidAmount))
            MetricRow("Top customer", snapshot.topClient)
            MetricRow("Costs this week", formatCurrency(snapshot.weekCosts))
            MetricRow("Hours logged this week", formatHours(snapshot.weekHours))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun UpgradePromptCard(
    jobCount: Int,
    remainingFreeEntries: Int,
    onOpenPro: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Free plan status", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "You have logged $jobCount jobs. $remainingFreeEntries free jobs remain before Pro is required.",
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
    locked: Boolean,
    onOpenPro: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
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
                TrendChart(snapshot = snapshot)
            }
        }
    }
}

@Composable
private fun TrendChart(snapshot: DashboardSnapshot) {
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
                    text = formatCurrency(week.value),
                    style = MaterialTheme.typography.labelSmall,
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
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
private fun RecentJobRow(job: JobRecord) {
    Card(shape = RoundedCornerShape(22.dp)) {
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
                text = "Invoice ${formatCurrency(job.invoiceTotal)}  •  Profit ${formatCurrency(job.estimatedProfit)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
