package com.indie.shiftledger.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.indie.shiftledger.data.JobDatabase
import com.indie.shiftledger.data.JobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val scheduler = ReminderScheduler(appContext)
                val repository = JobRepository(JobDatabase.build(appContext).jobDao())
                repository.allJobs().forEach(scheduler::sync)
            }

            pendingResult.finish()
        }
    }
}
