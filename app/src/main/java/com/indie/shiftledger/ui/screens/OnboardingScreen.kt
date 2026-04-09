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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.ui.theme.LedgerHeroPanel
import com.indie.shiftledger.ui.theme.LedgerPanel
import com.indie.shiftledger.ui.theme.LedgerPill
import com.indie.shiftledger.ui.theme.LedgerSectionHeader

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    currency: CurrencyOption,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onContinue: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LedgerHeroPanel {
                LedgerPill(
                    label = "Classic setup",
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                )
                Text(
                    text = "Track the job while you are still on site.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = "ShiftLedger keeps invoicing, follow-up, and profit visible without turning the phone into a spreadsheet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OnboardingPill(label = "Invoices")
                    OnboardingPill(label = "Reminders")
                    OnboardingPill(label = currency.code)
                }
            }
        }

        item {
            FeatureCard(
                icon = Icons.Rounded.PostAdd,
                title = "Capture fast",
                body = "Save the job, hours, costs, and notes before details drift or get lost in messages.",
            )
        }

        item {
            FeatureCard(
                icon = Icons.Rounded.Dashboard,
                title = "Read the numbers",
                body = "Weekly billed totals, profit, and outstanding work stay visible from the first saved job.",
            )
        }

        item {
            FeatureCard(
                icon = Icons.Rounded.Notifications,
                title = "Keep follow-ups moving",
                body = "Due dates, reminders, and invoice export stop open work from quietly going cold.",
            )
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Choose your billing currency",
                    body = "This sets how money is shown across the dashboard, draft preview, and ledger.",
                    trailing = {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CurrencyOption.entries.forEach { option ->
                        FilterChip(
                            selected = option == currency,
                            onClick = { onCurrencySelected(option) },
                            label = { Text(option.code) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                Text(
                    text = "Current selection: ${currency.displayLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start tracking jobs")
            }
        }
    }
}

@Composable
private fun OnboardingPill(
    label: String,
) {
    LedgerPill(
        label = label,
        containerColor = Color.White.copy(alpha = 0.16f),
        contentColor = Color.White,
    )
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
    LedgerPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
