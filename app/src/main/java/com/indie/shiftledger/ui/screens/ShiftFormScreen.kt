package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.billing.BillingUiState
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.InvoiceStatus
import com.indie.shiftledger.model.JobDraft
import com.indie.shiftledger.model.PricingMode
import com.indie.shiftledger.model.formatCurrency
import com.indie.shiftledger.model.formatHours
import com.indie.shiftledger.model.preview
import com.indie.shiftledger.ui.theme.LedgerHeroPanel
import com.indie.shiftledger.ui.theme.LedgerMetricTile
import com.indie.shiftledger.ui.theme.LedgerPanel
import com.indie.shiftledger.ui.theme.LedgerPill
import com.indie.shiftledger.ui.theme.LedgerSectionHeader
import com.indie.shiftledger.ui.theme.ledgerTextFieldColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

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
    var showJobDetails by rememberSaveable {
        mutableStateOf(draft.siteAddress.isNotBlank() || draft.workSummary.isNotBlank())
    }
    var showMoreAmounts by rememberSaveable {
        mutableStateOf(
            hasMoneyValue(draft.extraChargeText) ||
                hasMoneyValue(draft.materialsCostText) ||
                hasMoneyValue(draft.travelCostText),
        )
    }
    var showDueDate by rememberSaveable {
        mutableStateOf(draft.dueDateText.isNotBlank())
    }
    var showReminder by rememberSaveable {
        mutableStateOf(draft.reminderDateText.isNotBlank() || draft.reminderNote.isNotBlank())
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LedgerHeroPanel {
                LedgerPill(
                    label = "New job",
                    containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                    contentColor = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = formatCurrency(preview.invoiceTotal, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = "Add the customer, amount, and reminder in a few simple steps.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroDraftPill(label = "Currency ${currency.code}")
                    HeroDraftPill(label = if (billing.isPro) "Pro active" else "$remainingFreeEntries free left")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DraftHeroMetric(label = "Profit", value = formatCurrency(preview.estimatedProfit, currency))
                    DraftHeroMetric(
                        label = if (draft.pricingMode == PricingMode.Fixed) "Base" else "Hours",
                        value = if (draft.pricingMode == PricingMode.Fixed) {
                            formatCurrency(preview.laborTotal, currency)
                        } else {
                            formatHours(preview.hours)
                        },
                    )
                    DraftHeroMetric(label = "Stage", value = draft.status.label)
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Summary",
                    body = "Check the total while you fill in the job.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LedgerMetricTile(
                        label = "Invoice total",
                        value = formatCurrency(preview.invoiceTotal, currency),
                        supporting = if (draft.pricingMode == PricingMode.Fixed) {
                            "Job price, materials, callout, and extras"
                        } else {
                            "Labor, materials, callout, and extras"
                        },
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
                title = "1. Basic details",
                subtitle = "Start with the fields most jobs need.",
            ) {
                Text(
                    text = "How are you charging for this job?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PricingMode.entries.forEach { pricingMode ->
                        FilterChip(
                            selected = draft.pricingMode == pricingMode,
                            onClick = {
                                onDraftChange {
                                    it.copy(
                                        pricingMode = pricingMode,
                                        fixedPriceText = if (
                                            pricingMode == PricingMode.Fixed &&
                                            it.fixedPriceText.isBlank()
                                        ) {
                                            preview.laborTotal.toMoneyInput()
                                        } else {
                                            it.fixedPriceText
                                        },
                                    )
                                }
                            },
                            label = { Text(pricingMode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            shape = RoundedCornerShape(18.dp),
                        )
                    }
                }
                Text(
                    text = if (draft.pricingMode == PricingMode.Fixed) {
                        "Use one total amount when the customer is paying a single agreed price."
                    } else {
                        "Use an hourly rate and the start and end time to calculate the invoice."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FormField(
                    label = "Customer name",
                    value = draft.clientName,
                    onValueChange = { value -> onDraftChange { it.copy(clientName = value) } },
                )
                FormField(
                    label = "Job name",
                    value = draft.jobName,
                    onValueChange = { value -> onDraftChange { it.copy(jobName = value) } },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DatePickerField(
                        modifier = Modifier.weight(1f),
                        label = "Work date",
                        value = draft.dateText,
                        onValueChange = { value -> onDraftChange { it.copy(dateText = value) } },
                    )
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = draft.pricingMode.inputLabel,
                        value = if (draft.pricingMode == PricingMode.Fixed) {
                            draft.fixedPriceText
                        } else {
                            draft.laborRateText
                        },
                        onValueChange = { value ->
                            onDraftChange {
                                if (draft.pricingMode == PricingMode.Fixed) {
                                    it.copy(fixedPriceText = value)
                                } else {
                                    it.copy(laborRateText = value)
                                }
                            }
                        },
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
                if (draft.pricingMode == PricingMode.Fixed) {
                    Text(
                        text = "Time stays on the job record, but the invoice uses the fixed job price.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OptionalToggleChip(
                    label = if (showJobDetails) "Hide address and notes" else "Add address and notes",
                    selected = showJobDetails,
                    onClick = { showJobDetails = !showJobDetails },
                )
                if (showJobDetails) {
                    FormField(
                        label = "Address",
                        value = draft.siteAddress,
                        onValueChange = { value -> onDraftChange { it.copy(siteAddress = value) } },
                    )
                    FormField(
                        label = "Notes",
                        value = draft.workSummary,
                        onValueChange = { value -> onDraftChange { it.copy(workSummary = value) } },
                        minLines = 4,
                    )
                }
            }
        }

        item {
            FormSection(
                title = "2. Amounts",
                subtitle = "Enter what you will charge for this job.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "Materials to charge",
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
                OptionalToggleChip(
                    label = if (showMoreAmounts) "Hide extra amounts" else "Add extra amounts",
                    selected = showMoreAmounts,
                    onClick = { showMoreAmounts = !showMoreAmounts },
                )
                if (showMoreAmounts) {
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
        }

        item {
            FormSection(
                title = "3. Invoice",
                subtitle = "Set the job status and add dates only if you need them.",
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OptionalToggleChip(
                        label = if (showDueDate) "Hide due date" else "Add due date",
                        selected = showDueDate,
                        onClick = { showDueDate = !showDueDate },
                    )
                    if (draft.status.isOutstanding) {
                        OptionalToggleChip(
                            label = if (showReminder) "Hide reminder" else "Add reminder",
                            selected = showReminder,
                            onClick = { showReminder = !showReminder },
                        )
                    }
                }
                if (showDueDate) {
                    DatePickerField(
                        label = "Due date",
                        value = draft.dueDateText,
                        onValueChange = { value -> onDraftChange { it.copy(dueDateText = value) } },
                    )
                }
                if (draft.status.isOutstanding && showReminder) {
                    DatePickerField(
                        label = "Reminder date",
                        value = draft.reminderDateText,
                        onValueChange = { value -> onDraftChange { it.copy(reminderDateText = value) } },
                    )
                    FormField(
                        label = "Reminder note",
                        value = draft.reminderNote,
                        onValueChange = { value -> onDraftChange { it.copy(reminderNote = value) } },
                        minLines = 3,
                    )
                }
            }
        }

        item {
            LedgerPanel(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            ) {
                LedgerSectionHeader(
                    title = "Save job",
                    body = "Check the main numbers before you save.",
                )
                Text(
                    text = "Total ${formatCurrency(preview.invoiceTotal, currency)}  •  Costs ${formatCurrency(preview.totalCosts, currency)}  •  Profit ${formatCurrency(preview.estimatedProfit, currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Due ${preview.dueDateText.ifBlank { "not set" }}  •  Reminder ${preview.reminderDateText.ifBlank { "not set" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (limitReached) {
                    Text(
                        text = "The free plan limit is reached. Move to Pro to keep saving jobs.",
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
private fun DatePickerField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = value.toPickerDateMillis(),
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        onValueChange(datePickerState.selectedDateMillis.toIsoDateText())
                        showDatePicker = false
                    },
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (value.isNotBlank()) {
                        Text(
                            text = "Clear",
                            modifier = Modifier.clickable {
                                onValueChange("")
                                showDatePicker = false
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = "Cancel",
                        modifier = Modifier.clickable { showDatePicker = false },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
            )
        }
    }

    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                showDatePicker = true
            },
        value = value,
        onValueChange = {},
        readOnly = true,
        interactionSource = interactionSource,
        label = { Text(label) },
        placeholder = { Text("Select date") },
        trailingIcon = {
            if (value.isBlank()) {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = "Choose $label",
                    )
                }
            } else {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear $label",
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = ledgerTextFieldColors(),
    )
}

private fun String.toPickerDateMillis(): Long? = runCatching {
    LocalDate.parse(trim())
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()
}.getOrNull()

private fun Long?.toIsoDateText(): String {
    if (this == null) return ""
    return Instant.ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()
}

private fun hasMoneyValue(rawValue: String): Boolean {
    return when (rawValue.trim()) {
        "", "0", "0.0", "0.00" -> false
        else -> true
    }
}

private fun Double.toMoneyInput(): String {
    if (this <= 0.0) return ""
    val formatted = String.format(Locale.US, "%.2f", this)
    return formatted.trimEnd('0').trimEnd('.')
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
private fun OptionalToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = RoundedCornerShape(18.dp),
    )
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
