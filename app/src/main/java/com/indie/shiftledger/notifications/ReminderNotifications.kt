package com.indie.shiftledger.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ReminderNotifications {
    const val channelId = "fieldledger_payment_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existingChannel = manager.getNotificationChannel(channelId)
        if (existingChannel != null) return

        val channel = NotificationChannel(
            channelId,
            "Payment reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Follow-up reminders for unpaid FieldLedger jobs."
        }

        manager.createNotificationChannel(channel)
    }
}
