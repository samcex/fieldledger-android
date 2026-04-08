package com.indie.shiftledger

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indie.shiftledger.ui.screens.DashboardScreen
import com.indie.shiftledger.ui.screens.HistoryScreen
import com.indie.shiftledger.ui.screens.JobFormScreen
import com.indie.shiftledger.ui.screens.PaywallScreen

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FieldLedger",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        bottomBar = {
            FieldLedgerBottomBar(
                selectedTab = uiState.selectedTab,
                isPro = uiState.billing.isPro,
                onSelect = viewModel::selectTab,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            start = 16.dp,
            top = innerPadding.calculateTopPadding(),
            end = 16.dp,
            bottom = innerPadding.calculateBottomPadding(),
        )

        when (uiState.selectedTab) {
            FieldLedgerTab.Dashboard -> DashboardScreen(
                modifier = Modifier.padding(bottom = 8.dp),
                contentPadding = contentPadding,
                snapshot = uiState.dashboard,
                recentJobs = uiState.jobs.take(4),
                billing = uiState.billing,
                jobCount = uiState.jobs.size,
                remainingFreeEntries = uiState.remainingFreeEntries,
                onOpenPro = { viewModel.selectTab(FieldLedgerTab.Pro) },
            )

            FieldLedgerTab.AddShift -> JobFormScreen(
                modifier = Modifier.padding(bottom = 8.dp),
                contentPadding = contentPadding,
                draft = uiState.draft,
                billing = uiState.billing,
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
            BottomBarItem(FieldLedgerTab.Dashboard, "Overview", Icons.Rounded.Dashboard),
            BottomBarItem(FieldLedgerTab.AddShift, "New Job", Icons.Rounded.PostAdd),
            BottomBarItem(FieldLedgerTab.History, "Pipeline", Icons.Rounded.History),
            BottomBarItem(FieldLedgerTab.Pro, "Pro", Icons.Rounded.Lock),
        )
    }

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.tab == selectedTab,
                onClick = { onSelect(item.tab) },
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

private data class BottomBarItem(
    val tab: FieldLedgerTab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
