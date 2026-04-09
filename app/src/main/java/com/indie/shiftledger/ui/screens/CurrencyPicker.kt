package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.ui.theme.ledgerTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPicker(
    modifier: Modifier = Modifier,
    label: String,
    selected: CurrencyOption,
    onSelect: (CurrencyOption) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = selected.pickerLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ledgerTextFieldColors(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CurrencyOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = option.pickerLabel,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Symbol ${option.symbol}",
                                modifier = Modifier.padding(top = 2.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val CurrencyOption.pickerLabel: String
    get() = "$code • $label"
