package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.billing.BillingUiState
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
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "Quick job entry", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Log the work now, finish the invoice later. Date uses YYYY-MM-DD and time uses HH:MM.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Dictation works well in the work summary field when your hands are busy.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!billing.isPro) {
                        Text(
                            text = "$jobCount of 15 free jobs used. $remainingFreeEntries remaining.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Field(
                    label = "Work summary",
                    value = draft.workSummary,
                    onValueChange = { value -> onDraftChange { it.copy(workSummary = value) } },
                    minLines = 3,
                )
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "Invoice stage", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InvoiceStatus.values().forEach { status ->
                            FilterChip(
                                selected = draft.status == status,
                                onClick = { onDraftChange { it.copy(status = status) } },
                                label = { Text(status.label) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "Live preview", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Hours: ${formatHours(preview.hours)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Labor total: ${formatCurrency(preview.laborTotal)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Invoice total: ${formatCurrency(preview.invoiceTotal)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Estimated costs: ${formatCurrency(preview.totalCosts)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Estimated profit: ${formatCurrency(preview.estimatedProfit)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Stage: ${draft.status.label}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (limitReached) {
                    Text(
                        text = "Free logging limit reached. Upgrade to Pro to keep adding jobs.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onOpenPro) {
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
    )
}
