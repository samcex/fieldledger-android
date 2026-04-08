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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
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
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
            ) { innerPadding ->
                val contentPadding = PaddingValues(
                    start = 20.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
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
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        content()
    }
}

@Composable
private fun AppChrome(
    selectedTab: FieldLedgerTab,
    currency: CurrencyOption,
    isPro: Boolean,
) {
    val header = when (selectedTab) {
        FieldLedgerTab.Dashboard -> "Run the business, not the paperwork"
        FieldLedgerTab.AddJob -> "Capture today while the details are fresh"
        FieldLedgerTab.History -> "Track the pipeline and chase open money"
        FieldLedgerTab.Pro -> "Monetize the workflow with recurring value"
        FieldLedgerTab.Settings -> "Tune how money and formatting show up"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "FieldLedger", style = MaterialTheme.typography.titleLarge)
            Text(
                text = header,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChromeChip(
                    label = if (isPro) "Pro active" else "Free plan",
                    containerColor = if (isPro) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    },
                )
                ChromeChip(
                    label = "Currency ${currency.code}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ChromeChip(
    label: String,
    containerColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
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
                        colors = NavigationBarItemDefaults.colors(
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
