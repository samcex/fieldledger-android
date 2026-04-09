package com.indie.shiftledger.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.indie.shiftledger.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        ReminderNotifications.ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            intent.getLongExtra(extraJobId, 0L).toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ReminderNotifications.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(intent.getStringExtra(extraTitle) ?: "Payment reminder")
            .setContentText(intent.getStringExtra(extraBody) ?: "A saved invoice still needs follow-up.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    intent.getStringExtra(extraBody) ?: "A saved invoice still needs follow-up.",
                ),
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            intent.getLongExtra(extraJobId, 0L).toInt(),
            notification,
        )
    }

    companion object {
        const val extraJobId = "extra_job_id"
        const val extraTitle = "extra_title"
        const val extraBody = "extra_body"
    }
}
