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
                    label = "Quick setup",
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                )
                Text(
                    text = "Track jobs and invoices in one place.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = "Add jobs, see what is unpaid, and set reminders without turning your phone into a spreadsheet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OnboardingPill(label = "Jobs")
                    OnboardingPill(label = "Invoices")
                    OnboardingPill(label = currency.code)
                }
            }
        }

        item {
            FeatureCard(
                icon = Icons.Rounded.PostAdd,
                title = "Save a job fast",
                body = "Add the customer, time, and amount in a few taps.",
            )
        }

        item {
            FeatureCard(
                icon = Icons.Rounded.Dashboard,
                title = "See what is unpaid",
                body = "Home shows this week's total and any jobs still waiting on payment.",
            )
        }

        item {
            FeatureCard(
                icon = Icons.Rounded.Notifications,
                title = "Set reminders",
                body = "Choose a due date and reminder when you need one.",
            )
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Choose your currency",
                    body = "You can change this later in Settings.",
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
                Text("Continue")
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
