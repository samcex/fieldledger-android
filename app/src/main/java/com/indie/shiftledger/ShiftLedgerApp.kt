package com.indie.shiftledger

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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

    AppBackdrop {
        if (uiState.showOnboarding) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            ) { innerPadding ->
                OnboardingScreen(
                    modifier = Modifier.padding(bottom = 8.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 20.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 20.dp,
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
                    top = innerPadding.calculateTopPadding() + 6.dp,
                    end = 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + 8.dp,
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
                        onCurrencySelected = viewModel::updateCurrency,
                    )
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
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
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
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            Color.Transparent,
                        ),
                        center = Offset(120f, 120f),
                        radius = 680f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                        center = Offset(900f, 0f),
                        radius = 920f,
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "FIELDLEDGER",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = meta.title, style = MaterialTheme.typography.titleLarge)
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = meta.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = meta.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChromePill(
                    label = if (isPro) "Pro unlocked" else "Starter plan",
                    containerColor = if (isPro) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    },
                )
                ChromePill(
                    label = currency.code,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ChromePill(
    label: String,
    containerColor: Color,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
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
            BottomBarItem(FieldLedgerTab.Dashboard, "Overview", Icons.Rounded.Dashboard),
            BottomBarItem(FieldLedgerTab.AddJob, "Capture", Icons.Rounded.PostAdd),
            BottomBarItem(FieldLedgerTab.History, "Jobs", Icons.Rounded.History),
            BottomBarItem(FieldLedgerTab.Settings, "Settings", Icons.Rounded.Settings),
            BottomBarItem(FieldLedgerTab.Pro, "Pro", Icons.Rounded.Lock),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 10.dp,
            shadowElevation = 14.dp,
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = item.tab == selectedTab,
                        onClick = { onSelect(item.tab) },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        icon = {
                            if (item.tab == FieldLedgerTab.Pro && !isPro) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        },
                        label = { Text(item.label) },
                    )
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
    val label: String,
    val title: String,
    val subtitle: String,
)

private fun tabMeta(selectedTab: FieldLedgerTab): TabMeta = when (selectedTab) {
    FieldLedgerTab.Dashboard -> TabMeta(
        label = "Overview",
        title = "Money, pipeline, and pressure points",
        subtitle = "A tighter operating view for what needs attention next.",
    )

    FieldLedgerTab.AddJob -> TabMeta(
        label = "Capture",
        title = "Turn field notes into invoice-ready work",
        subtitle = "Keep the important numbers visible while you log the job.",
    )

    FieldLedgerTab.History -> TabMeta(
        label = "Jobs",
        title = "See what is paid, open, or going cold",
        subtitle = "Outstanding work stays at the top so follow-ups take less effort.",
    )

    FieldLedgerTab.Pro -> TabMeta(
        label = "Pro",
        title = "Charge for admin relief, not vague features",
        subtitle = "Subscriptions only make sense if the workflow keeps saving real time.",
    )

    FieldLedgerTab.Settings -> TabMeta(
        label = "Settings",
        title = "Format the app for the way you bill",
        subtitle = "Currency and money presentation should feel local, not generic.",
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
