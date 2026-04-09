package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.JobRecord
import com.indie.shiftledger.model.formatCurrency
import com.indie.shiftledger.model.formatShortDate
import com.indie.shiftledger.ui.theme.LedgerEmptyCard
import com.indie.shiftledger.ui.theme.LedgerHeroPanel
import com.indie.shiftledger.ui.theme.LedgerMetricTile
import com.indie.shiftledger.ui.theme.LedgerPanel
import com.indie.shiftledger.ui.theme.LedgerPill
import com.indie.shiftledger.ui.theme.LedgerSectionHeader

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    contentPadding: PaddingValues,
    jobs: List<JobRecord>,
    currency: CurrencyOption,
    onExport: (JobRecord) -> Unit,
    onScheduleReminder: (Long) -> Unit,
    onClearReminder: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val openJobs = jobs.count { it.invoiceStatus.isOutstanding }
    val paidJobs = jobs.size - openJobs
    val outstandingValue = jobs
        .filter { it.invoiceStatus.isOutstanding }
        .sumOf { it.invoiceTotal }
    val displayJobs = jobs.sortedWith(
        compareBy<JobRecord> { !it.invoiceStatus.isOutstanding }
            .thenByDescending { it.date }
            .thenByDescending { it.startTime },
    )

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LedgerHeroPanel {
                LedgerPill(
                    label = "Ledger pipeline",
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                )
                Text(
                    text = formatCurrency(outstandingValue, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    text = "Outstanding work stays ahead of fully paid jobs so the next follow-up is obvious.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HistoryHeroMetric(label = "Total", value = "${jobs.size}")
                    HistoryHeroMetric(label = "Open", value = "$openJobs")
                    HistoryHeroMetric(label = "Paid", value = "$paidJobs")
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Pipeline summary",
                    body = "Use this view like a paper invoice tray: open items first, settled work after that.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerMetricTile(
                        label = "Outstanding",
                        value = formatCurrency(outstandingValue, currency),
                        supporting = "Still open across quotes and invoices",
                    )
                    LedgerMetricTile(
                        label = "Reminder load",
                        value = "${jobs.count { it.hasReminder }} active",
                        supporting = "Jobs already scheduled for follow-up",
                    )
                }
            }
        }

        item {
            LedgerSectionHeader(
                title = "All jobs",
                body = if (displayJobs.isEmpty()) {
                    "No work has been saved yet."
                } else {
                    "Open jobs stay at the top. Export, remind, clear, or delete directly from each card."
                },
            )
        }

        if (displayJobs.isEmpty()) {
            item {
                LedgerEmptyCard(
                    title = "No jobs yet",
                    body = "As soon as you save work, this page becomes your invoice queue with reminders and export actions.",
                )
            }
        } else {
            items(displayJobs, key = { it.id }) { job ->
                JobPipelineCard(
                    job = job,
                    currency = currency,
                    onExport = onExport,
                    onScheduleReminder = onScheduleReminder,
                    onClearReminder = onClearReminder,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun RowScope.HistoryHeroMetric(
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
private fun JobPipelineCard(
    job: JobRecord,
    currency: CurrencyOption,
    onExport: (JobRecord) -> Unit,
    onScheduleReminder: (Long) -> Unit,
    onClearReminder: (Long) -> Unit,
    onDelete: (Long) -> Unit,
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
            text = "Profit ${formatCurrency(job.estimatedProfit, currency)}  •  Costs ${formatCurrency(job.totalCosts, currency)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${formatShortDate(job.date)}  •  ${job.timeWindowLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (job.paymentDueDate != null) {
                LedgerPill(
                    label = "Due ${formatShortDate(job.paymentDueDate)}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (job.hasReminder && job.reminderDate != null) {
                LedgerPill(
                    label = "Reminder ${formatShortDate(job.reminderDate)}",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        if (job.siteAddress.isNotBlank()) {
            Text(text = job.siteAddress, style = MaterialTheme.typography.bodySmall)
        }
        if (job.workSummary.isNotBlank()) {
            Text(
                text = job.workSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill(
                icon = Icons.Rounded.Share,
                label = "Export",
                onClick = { onExport(job) },
            )
            if (job.invoiceStatus.isOutstanding) {
                ActionPill(
                    icon = if (job.hasReminder) Icons.Rounded.AlarmOff else Icons.Rounded.Alarm,
                    label = if (job.hasReminder) "Clear" else "Remind",
                    onClick = {
                        if (job.hasReminder) {
                            onClearReminder(job.id)
                        } else {
                            onScheduleReminder(job.id)
                        }
                    },
                )
            }
            ActionPill(
                icon = Icons.Rounded.Delete,
                label = "Delete",
                onClick = { onDelete(job.id) },
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                )
            }
            Text(
                text = label,
                modifier = Modifier.padding(end = 12.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
