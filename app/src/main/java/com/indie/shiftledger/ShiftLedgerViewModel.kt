package com.indie.shiftledger

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.indie.shiftledger.billing.BillingRepository
import com.indie.shiftledger.billing.BillingUiState
import com.indie.shiftledger.data.JobDatabase
import com.indie.shiftledger.data.JobRepository
import com.indie.shiftledger.model.DashboardSnapshot
import com.indie.shiftledger.model.DisplayOffer
import com.indie.shiftledger.model.JobDraft
import com.indie.shiftledger.model.JobRecord
import com.indie.shiftledger.model.MonetizationPlan
import com.indie.shiftledger.model.validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FieldLedgerViewModel(
    private val jobRepository: JobRepository,
    private val billingRepository: BillingRepository,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(FieldLedgerTab.Dashboard)
    private val draft = MutableStateFlow(JobDraft())
    private val snackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FieldLedgerUiState> = combine(
        jobRepository.jobs,
        billingRepository.uiState,
        selectedTab,
        draft,
        snackMessage,
    ) { jobs, billing, tab, currentDraft, message ->
        FieldLedgerUiState(
            jobs = jobs,
            dashboard = DashboardSnapshot.fromJobs(jobs),
            billing = billing,
            selectedTab = tab,
            draft = currentDraft,
            snackbarMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FieldLedgerUiState(),
    )

    init {
        billingRepository.start()
    }

    fun selectTab(tab: FieldLedgerTab) {
        selectedTab.value = tab
    }

    fun updateDraft(transform: (JobDraft) -> JobDraft) {
        draft.update(transform)
    }

    fun saveDraft() {
        val state = uiState.value
        if (!state.billing.isPro && state.jobs.size >= MonetizationPlan.freeJobLimit) {
            selectedTab.value = FieldLedgerTab.Pro
            snackMessage.value = "Free plan limit reached. Upgrade to keep adding jobs."
            return
        }

        val validation = state.draft.validate()
        val job = validation.job
        if (job == null) {
            snackMessage.value = validation.errorMessage
            return
        }

        viewModelScope.launch {
            jobRepository.save(job)
            draft.value = JobDraft()
            selectedTab.value = FieldLedgerTab.Dashboard
            snackMessage.value = "Job saved."
        }
    }

    fun deleteJob(id: Long) {
        viewModelScope.launch {
            jobRepository.deleteById(id)
            snackMessage.value = "Job deleted."
        }
    }

    fun refreshBilling() {
        billingRepository.refreshEntitlements()
    }

    fun dismissMessage() {
        snackMessage.value = null
    }

    fun proOfferByProductId(productId: String): DisplayOffer? {
        return uiState.value.billing.offers.firstOrNull { it.productId == productId }
    }

    fun launchPurchase(activity: Activity, offer: DisplayOffer) {
        billingRepository.launchPurchase(activity, offer)
    }

    override fun onCleared() {
        billingRepository.close()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = JobDatabase.build(appContext)
                    val repository = JobRepository(database.jobDao())
                    val billing = BillingRepository(appContext)
                    return FieldLedgerViewModel(repository, billing) as T
                }
            }
        }
    }
}

data class FieldLedgerUiState(
    val jobs: List<JobRecord> = emptyList(),
    val dashboard: DashboardSnapshot = DashboardSnapshot(),
    val billing: BillingUiState = BillingUiState(),
    val selectedTab: FieldLedgerTab = FieldLedgerTab.Dashboard,
    val draft: JobDraft = JobDraft(),
    val snackbarMessage: String? = null,
) {
    val remainingFreeEntries: Int
        get() = (MonetizationPlan.freeJobLimit - jobs.size).coerceAtLeast(0)
}

enum class FieldLedgerTab {
    Dashboard,
    AddShift,
    History,
    Pro,
}
