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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDelete: (Long) -> Unit,
) {
    val openJobs = jobs.count { it.invoiceStatus.isOutstanding }
    val paidJobs = jobs.size - openJobs
    val outstandingValue = jobs
        .filter { it.invoiceStatus.isOutstanding }
        .sumOf { it.invoiceTotal }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "Job pipeline", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Stay on top of quotes, sent invoices, and paid work without leaving the timeline.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryPill(label = "${jobs.size} total")
                        SummaryPill(label = "$openJobs open")
                        SummaryPill(label = "$paidJobs paid")
                    }
                    Text(
                        text = "Outstanding value: ${formatCurrency(outstandingValue, currency)}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        if (jobs.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "No jobs yet", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Saved jobs land here with invoice stage, billed total, and profit so you can spot follow-ups fast.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        } else {
            items(jobs, key = { it.id }) { job ->
                Card(
                    shape = RoundedCornerShape(22.dp),
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                InvoiceStatusBadge(status = job.invoiceStatus)
                                IconButton(onClick = { onDelete(job.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete job")
                                }
                            }
                        }
                        Text(
                            text = "${formatShortDate(job.date)}  •  ${job.timeWindowLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Invoice ${formatCurrency(job.invoiceTotal, currency)}  •  Profit ${formatCurrency(job.estimatedProfit, currency)}  •  Costs ${formatCurrency(job.totalCosts, currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (job.siteAddress.isNotBlank()) {
                            Text(text = job.siteAddress, style = MaterialTheme.typography.bodySmall)
                        }
                        if (job.workSummary.isNotBlank()) {
                            Text(text = job.workSummary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
