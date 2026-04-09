package com.indie.shiftledger

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indie.shiftledger.export.InvoicePdfExporter
import com.indie.shiftledger.export.InvoiceShareLauncher
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.ui.screens.DashboardScreen
import com.indie.shiftledger.ui.screens.HistoryScreen
import com.indie.shiftledger.ui.screens.JobFormScreen
import com.indie.shiftledger.ui.screens.OnboardingScreen
import com.indie.shiftledger.ui.screens.PaywallScreen
import com.indie.shiftledger.ui.screens.SettingsScreen
import com.indie.shiftledger.ui.theme.FieldLedgerTheme
import com.indie.shiftledger.ui.theme.LedgerPill

@Composable
fun FieldLedgerApp(
    viewModel: FieldLedgerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val invoiceExporter = remember(context) { InvoicePdfExporter(context) }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissMessage()
    }

    FieldLedgerTheme(darkTheme = uiState.themeMode.isDark) {
        AppBackdrop {
            if (uiState.showOnboarding) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                ) { innerPadding ->
                    OnboardingScreen(
                        modifier = Modifier.padding(bottom = 8.dp),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            top = innerPadding.calculateTopPadding() + 26.dp,
                            end = 20.dp,
                            bottom = innerPadding.calculateBottomPadding() + 28.dp,
                        ),
                        currency = uiState.currency,
                        onCurrencySelected = viewModel::updateCurrency,
                        onContinue = viewModel::completeOnboarding,
                    )
                }
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0),
                    topBar = {
                        AppChrome(
                            selectedTab = uiState.selectedTab,
                            currency = uiState.currency,
                            isPro = uiState.billing.isPro,
                        )
                    },
                    bottomBar = {
                        FieldLedgerBottomBar(
                            selectedTab = uiState.selectedTab,
                            isPro = uiState.billing.isPro,
                            onSelect = viewModel::selectTab,
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                ) { innerPadding ->
                    val contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 10.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 12.dp,
                    )

                    when (uiState.selectedTab) {
                        FieldLedgerTab.Dashboard -> DashboardScreen(
                            modifier = Modifier.padding(bottom = 8.dp),
                            contentPadding = contentPadding,
                            snapshot = uiState.dashboard,
                            recentJobs = uiState.jobs.take(4),
                            billing = uiState.billing,
                            currency = uiState.currency,
                            jobCount = uiState.jobs.size,
                            remainingFreeEntries = uiState.remainingFreeEntries,
                            onOpenPro = { viewModel.selectTab(FieldLedgerTab.Pro) },
                        )

                        FieldLedgerTab.AddJob -> JobFormScreen(
                            modifier = Modifier.padding(bottom = 8.dp),
                            contentPadding = contentPadding,
                            draft = uiState.draft,
                            billing = uiState.billing,
                            currency = uiState.currency,
                            jobCount = uiState.jobs.size,
                            remainingFreeEntries = uiState.remainingFreeEntries,
                            onDraftChange = { updater -> viewModel.updateDraft(updater) },
                            onSave = viewModel::saveDraft,
                            onOpenPro = { viewModel.selectTab(FieldLedgerTab.Pro) },
                        )

                        FieldLedgerTab.History -> HistoryScreen(
                            modifier = Modifier.padding(bottom = 8.dp),
                            contentPadding = contentPadding,
                            jobs = uiState.jobs,
                            currency = uiState.currency,
                            onExport = { job ->
                                runCatching {
                                    val pdfFile = invoiceExporter.export(job, uiState.currency)
                                    InvoiceShareLauncher.share(context, pdfFile)
                                }.onFailure {
                                    viewModel.showMessage("Could not export invoice PDF.")
                                }
                            },
                            onScheduleReminder = viewModel::scheduleReminderTomorrow,
                            onClearReminder = viewModel::clearReminder,
                            onDelete = viewModel::deleteJob,
                        )

                        FieldLedgerTab.Pro -> PaywallScreen(
                            modifier = Modifier.padding(bottom = 8.dp),
                            contentPadding = contentPadding,
                            billing = uiState.billing,
                            resolveOffer = viewModel::proOfferByProductId,
                            onRefresh = viewModel::refreshBilling,
                            onPurchase = { offer ->
                                context.findActivity()?.let { activity ->
                                    viewModel.launchPurchase(activity, offer)
                                }
                            },
                        )

                        FieldLedgerTab.Settings -> SettingsScreen(
                            modifier = Modifier.padding(bottom = 8.dp),
                            contentPadding = contentPadding,
                            currency = uiState.currency,
                            themeMode = uiState.themeMode,
                            onCurrencySelected = viewModel::updateCurrency,
                            onThemeModeChanged = viewModel::updateThemeMode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBackdrop(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                        center = Offset(180f, 80f),
                        radius = 760f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        center = Offset(960f, 260f),
                        radius = 960f,
                    ),
                ),
        )
        content()
    }
}

@Composable
private fun AppChrome(
    selectedTab: FieldLedgerTab,
    currency: CurrencyOption,
    isPro: Boolean,
) {
    val meta = tabMeta(selectedTab)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "SHIFTLEDGER",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = meta.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = meta.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LedgerPill(
                    label = currency.code,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                LedgerPill(
                    label = if (isPro) "Pro active" else "Starter",
                    containerColor = if (isPro) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (isPro) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                )
            }
        }
    }
}

@Composable
private fun FieldLedgerBottomBar(
    selectedTab: FieldLedgerTab,
    isPro: Boolean,
    onSelect: (FieldLedgerTab) -> Unit,
) {
    val items = remember {
        listOf(
            BottomBarItem(FieldLedgerTab.Dashboard, "Home", Icons.Rounded.Dashboard),
            BottomBarItem(FieldLedgerTab.AddJob, "Capture", Icons.Rounded.PostAdd),
            BottomBarItem(FieldLedgerTab.History, "Ledger", Icons.Rounded.History),
            BottomBarItem(FieldLedgerTab.Settings, "Settings", Icons.Rounded.Settings),
            BottomBarItem(FieldLedgerTab.Pro, "Pro", Icons.Rounded.Lock),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 640.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            shape = RoundedCornerShape(34.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
            shadowElevation = 14.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item ->
                    val selected = item.tab == selectedTab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(item.tab) },
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (item.tab == FieldLedgerTab.Pro && !isPro) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class BottomBarItem(
    val tab: FieldLedgerTab,
    val label: String,
    val icon: ImageVector,
)

private data class TabMeta(
    val title: String,
    val subtitle: String,
)

private fun tabMeta(selectedTab: FieldLedgerTab): TabMeta = when (selectedTab) {
    FieldLedgerTab.Dashboard -> TabMeta(
        title = "Command overview",
        subtitle = "Weekly totals, outstanding value, and your latest work.",
    )

    FieldLedgerTab.AddJob -> TabMeta(
        title = "Capture a job",
        subtitle = "Log the work while the details are still fresh.",
    )

    FieldLedgerTab.History -> TabMeta(
        title = "Ledger and pipeline",
        subtitle = "Invoices, reminders, export, and payment status.",
    )

    FieldLedgerTab.Pro -> TabMeta(
        title = "ShiftLedger Pro",
        subtitle = "Plans, purchase state, and launch readiness.",
    )

    FieldLedgerTab.Settings -> TabMeta(
        title = "Preferences",
        subtitle = "Currency and display mode for this device.",
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
