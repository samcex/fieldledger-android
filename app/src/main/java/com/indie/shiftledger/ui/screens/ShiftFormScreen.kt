package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.billing.BillingUiState
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.InvoiceStatus
import com.indie.shiftledger.model.JobDraft
import com.indie.shiftledger.model.formatCurrency
import com.indie.shiftledger.model.formatHours
import com.indie.shiftledger.model.preview

@Composable
fun JobFormScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    draft: JobDraft,
    billing: BillingUiState,
    currency: CurrencyOption,
    jobCount: Int,
    remainingFreeEntries: Int,
    onDraftChange: ((JobDraft) -> JobDraft) -> Unit,
    onSave: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val preview = draft.preview()
    val limitReached = !billing.isPro && remainingFreeEntries == 0

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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(text = "Draft snapshot", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Keep the numbers visible while you capture the job. Date format is YYYY-MM-DD and time format is HH:MM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryPill(label = "Currency ${currency.code}")
                        SummaryPill(label = if (billing.isPro) "Pro active" else "$remainingFreeEntries free left")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DraftMetric(
                            label = "Invoice",
                            value = formatCurrency(preview.invoiceTotal, currency),
                        )
                        DraftMetric(
                            label = "Profit",
                            value = formatCurrency(preview.estimatedProfit, currency),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DraftMetric(
                            label = "Hours",
                            value = formatHours(preview.hours),
                        )
                        DraftMetric(
                            label = "Stage",
                            value = draft.status.label,
                        )
                    }
                    if (!billing.isPro) {
                        Text(
                            text = "$jobCount of 15 starter jobs used.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "1. Work details",
                subtitle = "The customer, the job, and the notes you will care about later.",
            ) {
                Field(
                    label = "Customer",
                    value = draft.clientName,
                    onValueChange = { value -> onDraftChange { it.copy(clientName = value) } },
                )
                Field(
                    label = "Job or service",
                    value = draft.jobName,
                    onValueChange = { value -> onDraftChange { it.copy(jobName = value) } },
                )
                Field(
                    label = "Site address",
                    value = draft.siteAddress,
                    onValueChange = { value -> onDraftChange { it.copy(siteAddress = value) } },
                )
                Field(
                    label = "Work summary",
                    value = draft.workSummary,
                    onValueChange = { value -> onDraftChange { it.copy(workSummary = value) } },
                    minLines = 3,
                )
            }
        }

        item {
            SectionCard(
                title = "2. Time and rate",
                subtitle = "Set the service window and the labor rate that drives the draft total.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Date",
                        value = draft.dateText,
                        onValueChange = { value -> onDraftChange { it.copy(dateText = value) } },
                    )
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Labor rate",
                        value = draft.laborRateText,
                        onValueChange = { value -> onDraftChange { it.copy(laborRateText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Start time",
                        value = draft.startTimeText,
                        onValueChange = { value -> onDraftChange { it.copy(startTimeText = value) } },
                    )
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "End time",
                        value = draft.endTimeText,
                        onValueChange = { value -> onDraftChange { it.copy(endTimeText = value) } },
                    )
                }
            }
        }

        item {
            SectionCard(
                title = "3. Billable items",
                subtitle = "Separate what the customer sees from what the job actually costs you.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Materials billed",
                        value = draft.materialsBilledText,
                        onValueChange = { value -> onDraftChange { it.copy(materialsBilledText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Callout fee",
                        value = draft.calloutFeeText,
                        onValueChange = { value -> onDraftChange { it.copy(calloutFeeText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Extra charge",
                        value = draft.extraChargeText,
                        onValueChange = { value -> onDraftChange { it.copy(extraChargeText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Materials cost",
                        value = draft.materialsCostText,
                        onValueChange = { value -> onDraftChange { it.copy(materialsCostText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                Field(
                    label = "Travel cost",
                    value = draft.travelCostText,
                    onValueChange = { value -> onDraftChange { it.copy(travelCostText = value) } },
                    keyboardType = KeyboardType.Decimal,
                )
            }
        }

        item {
            SectionCard(
                title = "4. Follow-up",
                subtitle = "Keep the invoice moving with a due date, reminder date, and pipeline status.",
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InvoiceStatus.values().forEach { status ->
                        FilterChip(
                            selected = draft.status == status,
                            onClick = { onDraftChange { it.copy(status = status) } },
                            label = { Text(status.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Due date",
                        value = draft.dueDateText,
                        onValueChange = { value -> onDraftChange { it.copy(dueDateText = value) } },
                    )
                    Field(
                        modifier = Modifier.weight(1f),
                        label = "Reminder date",
                        value = draft.reminderDateText,
                        onValueChange = { value -> onDraftChange { it.copy(reminderDateText = value) } },
                    )
                }
                Field(
                    label = "Reminder note",
                    value = draft.reminderNote,
                    onValueChange = { value -> onDraftChange { it.copy(reminderNote = value) } },
                    minLines = 2,
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "Ready to save", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Invoice ${formatCurrency(preview.invoiceTotal, currency)}  •  Costs ${formatCurrency(preview.totalCosts, currency)}  •  Profit ${formatCurrency(preview.estimatedProfit, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Due ${preview.dueDateText.ifBlank { "not set" }}  •  Reminder ${preview.reminderDateText.ifBlank { "not set" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (limitReached) {
                        Text(
                            text = "The starter plan limit is reached. Move to Pro to keep logging jobs.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = onOpenPro, modifier = Modifier.fillMaxWidth()) {
                            Text("Unlock Pro")
                        }
                    } else {
                        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                            Text("Save job")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun RowScope.DraftMetric(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun Field(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        singleLine = minLines == 1,
        shape = RoundedCornerShape(18.dp),
    )
}
