package com.skillbridge.sa.data.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skillbridge.sa.MainActivity
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.AppNotification

object SystemNotificationHelper {

    const val CHANNEL_ID = "skillbridge_sa_notifications"
    private const val CHANNEL_NAME = "SkillBridge SA Alerts"
    private const val CHANNEL_DESC = "Real-time alerts for jobs, mentorship, applications, and messages."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, notification: AppNotification) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = R.mipmap.ic_launcher

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(iconRes)
        builder.setContentTitle(notification.title)
        builder.setContentText(notification.body)
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setAutoCancel(true)
        builder.setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notification.notificationId.hashCode(), builder.build())
        } catch (_: Exception) {
            // Permission missing or channel unavailable
        }
    }
}
