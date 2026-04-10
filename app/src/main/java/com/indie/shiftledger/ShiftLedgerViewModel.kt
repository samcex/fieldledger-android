package com.indie.shiftledger

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.indie.shiftledger.billing.BillingRepository
import com.indie.shiftledger.billing.BillingUiState
import com.indie.shiftledger.data.JobDatabase
import com.indie.shiftledger.data.JobRepository
import com.indie.shiftledger.data.SettingsRepository
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.DashboardSnapshot
import com.indie.shiftledger.model.InvoiceStatus
import com.indie.shiftledger.model.JobDraft
import com.indie.shiftledger.model.JobRecord
import com.indie.shiftledger.model.ThemeMode
import com.indie.shiftledger.model.asDraft
import com.indie.shiftledger.model.isEditing
import com.indie.shiftledger.model.validate
import com.indie.shiftledger.notifications.ReminderScheduler
import java.time.LocalDate
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
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(FieldLedgerTab.Dashboard)
    private val draft = MutableStateFlow(JobDraft())
    private val snackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FieldLedgerUiState> = combine(
        jobRepository.jobs,
        billingRepository.uiState,
        settingsRepository.currency,
        settingsRepository.onboardingComplete,
        settingsRepository.themeMode,
        settingsRepository.companyName,
        settingsRepository.logoUri,
        selectedTab,
        draft,
        snackMessage,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val jobs = values[0] as List<JobRecord>
        val billing = values[1] as BillingUiState
        val currency = values[2] as CurrencyOption
        val onboardingComplete = values[3] as Boolean
        val themeMode = values[4] as ThemeMode
        val companyName = values[5] as String
        val logoUri = values[6] as String?
        val tab = values[7] as FieldLedgerTab
        val currentDraft = values[8] as JobDraft
        val message = values[9] as String?

        FieldLedgerUiState(
            jobs = jobs,
            dashboard = DashboardSnapshot.fromJobs(jobs),
            billing = billing,
            currency = currency,
            themeMode = themeMode,
            companyName = companyName,
            logoUri = logoUri,
            showOnboarding = !onboardingComplete,
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
        viewModelScope.launch {
            syncRemindersInternal()
        }
    }

    fun selectTab(tab: FieldLedgerTab) {
        selectedTab.value = tab
    }

    fun updateDraft(transform: (JobDraft) -> JobDraft) {
        draft.update(transform)
    }

    fun editJob(id: Long) {
        val existingJob = uiState.value.jobs.firstOrNull { it.id == id } ?: run {
            snackMessage.value = "Could not open that job."
            return
        }

        draft.value = existingJob.asDraft()
        selectedTab.value = FieldLedgerTab.AddJob
        snackMessage.value = "Editing ${existingJob.jobName}."
    }

    fun cancelDraftEdit() {
        if (!draft.value.isEditing) return
        draft.value = JobDraft()
        selectedTab.value = FieldLedgerTab.History
        snackMessage.value = "Edit cancelled."
    }

    fun updateCurrency(currency: CurrencyOption) {
        settingsRepository.updateCurrency(currency)
        snackMessage.value = "Currency changed to ${currency.code}."
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        settingsRepository.updateThemeMode(themeMode)
        snackMessage.value = "${themeMode.label} enabled."
    }

    fun updateCompanyName(companyName: String) {
        settingsRepository.updateCompanyName(companyName)
    }

    fun updateLogoUri(logoUri: String?) {
        settingsRepository.updateLogoUri(logoUri)
    }

    fun showMessage(message: String) {
        snackMessage.value = message
    }

    fun completeOnboarding() {
        settingsRepository.completeOnboarding()
        selectedTab.value = FieldLedgerTab.Dashboard
        snackMessage.value = "Ready to log the first job."
    }

    fun saveDraft() {
        val state = uiState.value
        val validation = state.draft.validate()
        val job = validation.job
        if (job == null) {
            snackMessage.value = validation.errorMessage
            return
        }

        viewModelScope.launch {
            val savedJob = jobRepository.save(job)
            reminderScheduler.sync(savedJob)
            val wasEditing = state.draft.isEditing
            draft.value = JobDraft()
            selectedTab.value = if (wasEditing) FieldLedgerTab.History else FieldLedgerTab.Dashboard
            snackMessage.value = if (wasEditing) "Job updated." else "Job saved."
        }
    }

    fun deleteJob(id: Long) {
        viewModelScope.launch {
            jobRepository.deleteById(id)
            reminderScheduler.cancel(id)
            snackMessage.value = "Job deleted."
        }
    }

    fun scheduleReminderTomorrow(id: Long) {
        val existingJob = uiState.value.jobs.firstOrNull { it.id == id } ?: return
        if (!existingJob.invoiceStatus.isOutstanding) {
            snackMessage.value = "Paid jobs do not need reminders."
            return
        }

        viewModelScope.launch {
            val savedJob = jobRepository.save(
                existingJob.copy(
                    reminderDate = LocalDate.now().plusDays(1),
                    reminderNote = existingJob.reminderNote.ifBlank {
                        "Follow up with ${existingJob.clientName} about ${existingJob.jobName}."
                    },
                ),
            )
            reminderScheduler.sync(savedJob)
            snackMessage.value = "Reminder set for tomorrow morning."
        }
    }

    fun clearReminder(id: Long) {
        val existingJob = uiState.value.jobs.firstOrNull { it.id == id } ?: return

        viewModelScope.launch {
            val savedJob = jobRepository.save(
                existingJob.copy(
                    reminderDate = null,
                    reminderNote = "",
                ),
            )
            reminderScheduler.sync(savedJob)
            snackMessage.value = "Reminder cleared."
        }
    }

    fun markJobPaid(id: Long) {
        val existingJob = uiState.value.jobs.firstOrNull { it.id == id } ?: return
        if (existingJob.invoiceStatus == InvoiceStatus.Paid) {
            snackMessage.value = "Job is already marked paid."
            return
        }

        viewModelScope.launch {
            val savedJob = jobRepository.save(
                existingJob.copy(
                    invoiceStatus = InvoiceStatus.Paid,
                    reminderDate = null,
                    reminderNote = "",
                ),
            )
            reminderScheduler.sync(savedJob)
            snackMessage.value = "Job marked paid."
        }
    }

    fun markInvoiceSent(id: Long, notify: Boolean = true) {
        val existingJob = uiState.value.jobs.firstOrNull { it.id == id } ?: return
        if (existingJob.invoiceStatus == InvoiceStatus.Paid) return
        if (existingJob.invoiceStatus == InvoiceStatus.InvoiceSent) {
            if (notify) {
                snackMessage.value = "Invoice is already marked sent."
            }
            return
        }

        viewModelScope.launch {
            jobRepository.save(
                existingJob.copy(
                    invoiceStatus = InvoiceStatus.InvoiceSent,
                ),
            )
            if (notify) {
                snackMessage.value = "Invoice marked sent."
            }
        }
    }

    fun syncReminders() {
        viewModelScope.launch {
            syncRemindersInternal()
        }
    }

    fun dismissMessage() {
        snackMessage.value = null
    }

    override fun onCleared() {
        billingRepository.close()
    }

    private suspend fun syncRemindersInternal() {
        jobRepository.allJobs().forEach(reminderScheduler::sync)
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = JobDatabase.build(appContext)
                    val repository = JobRepository(database.jobDao())
                    val settings = SettingsRepository(appContext)
                    val billing = BillingRepository(appContext, settings)
                    val reminders = ReminderScheduler(appContext)
                    return FieldLedgerViewModel(repository, billing, settings, reminders) as T
                }
            }
        }
    }
}

data class FieldLedgerUiState(
    val jobs: List<JobRecord> = emptyList(),
    val dashboard: DashboardSnapshot = DashboardSnapshot(),
    val billing: BillingUiState = BillingUiState(),
    val currency: CurrencyOption = CurrencyOption.USD,
    val themeMode: ThemeMode = ThemeMode.Light,
    val companyName: String = "",
    val logoUri: String? = null,
    val showOnboarding: Boolean = true,
    val selectedTab: FieldLedgerTab = FieldLedgerTab.Dashboard,
    val draft: JobDraft = JobDraft(),
    val snackbarMessage: String? = null,
) {
    val isEditingDraft: Boolean
        get() = draft.isEditing
}

enum class FieldLedgerTab {
    Dashboard,
    AddJob,
    History,
    Settings,
}
