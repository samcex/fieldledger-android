package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.indie.shiftledger.ui.theme.LedgerHeroPanel
import com.indie.shiftledger.ui.theme.LedgerMetricTile
import com.indie.shiftledger.ui.theme.LedgerPanel
import com.indie.shiftledger.ui.theme.LedgerPill
import com.indie.shiftledger.ui.theme.LedgerSectionHeader
import com.indie.shiftledger.ui.theme.ledgerTextFieldColors

@Composable
fun JobFormScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
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
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LedgerHeroPanel {
                LedgerPill(
                    label = "Job draft",
                    containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                    contentColor = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = formatCurrency(preview.invoiceTotal, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = "Capture the work like a paper ledger: customer, time, billables, and follow-up all on one clean form.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroDraftPill(label = "Currency ${currency.code}")
                    HeroDraftPill(label = if (billing.isPro) "Pro active" else "$remainingFreeEntries free left")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DraftHeroMetric(label = "Profit", value = formatCurrency(preview.estimatedProfit, currency))
                    DraftHeroMetric(label = "Hours", value = formatHours(preview.hours))
                    DraftHeroMetric(label = "Stage", value = draft.status.label)
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Capture status",
                    body = "Keep the financial picture visible while you fill the form.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerMetricTile(
                        label = "Invoice total",
                        value = formatCurrency(preview.invoiceTotal, currency),
                        supporting = "Labor, materials, callout, and extras",
                    )
                    LedgerMetricTile(
                        label = "Total costs",
                        value = formatCurrency(preview.totalCosts, currency),
                        supporting = "Materials cost plus travel",
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

        item {
            FormSection(
                title = "1. Work details",
                subtitle = "The customer, the job, and the notes you will care about when the invoice is due.",
            ) {
                FormField(
                    label = "Customer",
                    value = draft.clientName,
                    onValueChange = { value -> onDraftChange { it.copy(clientName = value) } },
                )
                FormField(
                    label = "Job or service",
                    value = draft.jobName,
                    onValueChange = { value -> onDraftChange { it.copy(jobName = value) } },
                )
                FormField(
                    label = "Site address",
                    value = draft.siteAddress,
                    onValueChange = { value -> onDraftChange { it.copy(siteAddress = value) } },
                )
                FormField(
                    label = "Work summary",
                    value = draft.workSummary,
                    onValueChange = { value -> onDraftChange { it.copy(workSummary = value) } },
                    minLines = 4,
                )
            }
        }

        item {
            FormSection(
                title = "2. Time and labor",
                subtitle = "Set the work window and the labor rate that drives the draft total.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Date",
                        value = draft.dateText,
                        onValueChange = { value -> onDraftChange { it.copy(dateText = value) } },
                    )
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Labor rate",
                        value = draft.laborRateText,
                        onValueChange = { value -> onDraftChange { it.copy(laborRateText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Start time",
                        value = draft.startTimeText,
                        onValueChange = { value -> onDraftChange { it.copy(startTimeText = value) } },
                    )
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "End time",
                        value = draft.endTimeText,
                        onValueChange = { value -> onDraftChange { it.copy(endTimeText = value) } },
                    )
                }
            }
        }

        item {
            FormSection(
                title = "3. Billable items",
                subtitle = "Separate what you charge from what the job actually costs you.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Materials billed",
                        value = draft.materialsBilledText,
                        onValueChange = { value -> onDraftChange { it.copy(materialsBilledText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Callout fee",
                        value = draft.calloutFeeText,
                        onValueChange = { value -> onDraftChange { it.copy(calloutFeeText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Extra charge",
                        value = draft.extraChargeText,
                        onValueChange = { value -> onDraftChange { it.copy(extraChargeText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Materials cost",
                        value = draft.materialsCostText,
                        onValueChange = { value -> onDraftChange { it.copy(materialsCostText = value) } },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                FormField(
                    label = "Travel cost",
                    value = draft.travelCostText,
                    onValueChange = { value -> onDraftChange { it.copy(travelCostText = value) } },
                    keyboardType = KeyboardType.Decimal,
                )
            }
        }

        item {
            FormSection(
                title = "4. Follow-up and status",
                subtitle = "Choose the invoice stage and set due or reminder dates before the job goes cold.",
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            shape = RoundedCornerShape(18.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Due date",
                        value = draft.dueDateText,
                        onValueChange = { value -> onDraftChange { it.copy(dueDateText = value) } },
                    )
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Reminder date",
                        value = draft.reminderDateText,
                        onValueChange = { value -> onDraftChange { it.copy(reminderDateText = value) } },
                    )
                }
                FormField(
                    label = "Reminder note",
                    value = draft.reminderNote,
                    onValueChange = { value -> onDraftChange { it.copy(reminderNote = value) } },
                    minLines = 3,
                )
            }
        }

        item {
            LedgerPanel(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            ) {
                LedgerSectionHeader(
                    title = "Ready to save",
                    body = "Review the money side one last time before this job becomes part of the ledger.",
                )
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

@Composable
private fun HeroDraftPill(
    label: String,
) {
    LedgerPill(
        label = label,
        containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
        contentColor = androidx.compose.ui.graphics.Color.White,
    )
}

@Composable
private fun RowScope.DraftHeroMetric(
    label: String,
    value: String,
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(24.dp),
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    LedgerPanel {
        LedgerSectionHeader(
            title = title,
            body = subtitle,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun FormField(
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
        shape = RoundedCornerShape(20.dp),
        colors = ledgerTextFieldColors(),
    )
}
