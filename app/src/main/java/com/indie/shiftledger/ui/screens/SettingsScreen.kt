package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
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
import com.indie.shiftledger.ui.theme.LedgerHeroPanel
import com.indie.shiftledger.ui.theme.LedgerPanel
import com.indie.shiftledger.ui.theme.LedgerPill
import com.indie.shiftledger.ui.theme.LedgerSectionHeader

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    contentPadding: PaddingValues,
    currency: CurrencyOption,
    themeMode: ThemeMode,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LedgerHeroPanel {
                LedgerPill(
                    label = "Settings",
                    containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                    contentColor = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.displaySmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = "Choose your currency and whether the app uses light or dark mode.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PreferenceHeroPill(label = currency.displayLabel)
                    PreferenceHeroPill(label = themeMode.label)
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Currency",
                    body = "This changes how money is shown across the app.",
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                CurrencyPicker(
                    label = "Currency",
                    selected = currency,
                    onSelect = onCurrencySelected,
                )
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Appearance",
                    body = "Use light mode or dark mode.",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = "Color mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (themeMode == ThemeMode.AmoledDark) {
                                "Dark mode uses black backgrounds and brighter text."
                            } else {
                                "Light mode uses white backgrounds and black text."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Current mode: ${themeMode.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
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
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Preview",
                    body = "A quick preview of how amounts will look.",
                )
                Text(text = "Revenue ${formatCurrency(3210.40, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Costs ${formatCurrency(685.10, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Profit ${formatCurrency(2525.30, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "These settings are saved on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PreferenceHeroPill(
    label: String,
) {
    LedgerPill(
        label = label,
        containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
        contentColor = androidx.compose.ui.graphics.Color.White,
    )
}
