package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.ThemeMode
import com.indie.shiftledger.model.formatCurrency

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    currency: CurrencyOption,
    themeMode: ThemeMode,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "Money formatting", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Choose the currency and color mode you want to work in. Both update instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Current format",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(text = currency.displayLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Sample invoice ${formatCurrency(1285.50, currency)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = "Appearance", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (themeMode == ThemeMode.AmoledDark) {
                                "AMOLED dark uses true black surfaces for OLED screens."
                            } else {
                                "Light mode keeps the warmer paper-style palette."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Current mode: ${themeMode.label}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Switch(
                        checked = themeMode == ThemeMode.AmoledDark,
                        onCheckedChange = { enabled ->
                            onThemeModeChanged(
                                if (enabled) ThemeMode.AmoledDark else ThemeMode.Light,
                            )
                        },
                    )
                }
            }
        }

        item {
            Text(text = "Available currencies", style = MaterialTheme.typography.titleMedium)
        }

        items(CurrencyOption.entries, key = { it.code }) { option ->
            CurrencyOptionCard(
                option = option,
                selected = option == currency,
                onSelect = { onCurrencySelected(option) },
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "Preview", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Revenue ${formatCurrency(3210.40, currency)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Costs ${formatCurrency(685.10, currency)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Profit ${formatCurrency(2525.30, currency)}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Currency formatting is saved locally on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyOptionCard(
    option: CurrencyOption,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = option.displayLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Symbol ${option.symbol}  •  Code ${option.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
