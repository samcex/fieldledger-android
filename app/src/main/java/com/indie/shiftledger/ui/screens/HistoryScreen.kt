package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
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
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "Pipeline snapshot", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Outstanding work stays ahead of fully paid jobs so the next follow-up is obvious.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatCurrency(outstandingValue, currency),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Still open across quotes and invoices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PipelineMetric(label = "Total", value = "${jobs.size}")
                        PipelineMetric(label = "Open", value = "$openJobs")
                        PipelineMetric(label = "Paid", value = "$paidJobs")
                    }
                }
            }
        }

        if (displayJobs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "No jobs yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "As soon as you save work, this view turns into a live pipeline with reminders, PDF export, and payment status.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
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
private fun RowScope.PipelineMetric(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
private fun JobPipelineCard(
    job: JobRecord,
    currency: CurrencyOption,
    onExport: (JobRecord) -> Unit,
    onScheduleReminder: (Long) -> Unit,
    onClearReminder: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    DetailChip(
                        label = "Due ${formatShortDate(job.paymentDueDate)}",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                }
                if (job.hasReminder && job.reminderDate != null) {
                    DetailChip(
                        label = "Reminder ${formatShortDate(job.reminderDate)}",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
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
}

@Composable
private fun DetailChip(
    label: String,
    containerColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
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
