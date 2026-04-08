package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "Quick job capture", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Log the work now, finish the invoice later. Date uses YYYY-MM-DD and time uses HH:MM.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "All money fields use ${currency.displayLabel}. Dictation works well in the work summary field when your hands are busy.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    ) {
                        Text(
                            text = "Current currency ${currency.code}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
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
            SectionCard(
                title = "Client and site",
                subtitle = "Capture who the work was for and where it happened.",
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
                title = "Schedule",
                subtitle = "Keep the date and time format tight so export stays clean.",
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
                title = "Pricing and costs",
                subtitle = "Separate what you bill from what you spend to keep profit visible.",
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
                title = "Invoice stage",
                subtitle = "Use status to keep the pipeline honest.",
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
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "Live preview", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Hours: ${formatHours(preview.hours)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Labor total: ${formatCurrency(preview.laborTotal, currency)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Invoice total: ${formatCurrency(preview.invoiceTotal, currency)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Estimated costs: ${formatCurrency(preview.totalCosts, currency)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Estimated profit: ${formatCurrency(preview.estimatedProfit, currency)}", style = MaterialTheme.typography.bodyMedium)
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
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
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
        shape = RoundedCornerShape(20.dp),
    )
}
