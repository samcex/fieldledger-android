package com.indie.shiftledger.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.indie.shiftledger.model.JobRecord
import java.time.LocalDate
import java.time.ZoneId

class ReminderScheduler(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun sync(job: JobRecord) {
        cancel(job.id)

        if (!job.hasReminder) return

        val triggerAt = reminderTriggerMillis(job.reminderDate ?: return)
        val intent = reminderIntent(job)
        alarmManager?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            intent,
        )
    }

    fun cancel(jobId: Long) {
        alarmManager?.cancel(reminderIntent(jobId))
    }

    private fun reminderTriggerMillis(reminderDate: LocalDate): Long {
        val targetMillis = reminderDate
            .atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return targetMillis.coerceAtLeast(System.currentTimeMillis() + 60_000L)
    }

    private fun reminderIntent(job: JobRecord): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.extraJobId, job.id)
            putExtra(ReminderReceiver.extraTitle, "Follow up with ${job.clientName}")
            putExtra(ReminderReceiver.extraBody, job.reminderMessage)
        }
        return PendingIntent.getBroadcast(
            appContext,
            job.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun reminderIntent(jobId: Long): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            appContext,
            jobId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
