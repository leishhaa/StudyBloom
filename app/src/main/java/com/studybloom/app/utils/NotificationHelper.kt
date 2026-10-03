package com.studybloom.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.studybloom.app.R
import com.studybloom.app.activities.MainActivity
import java.util.concurrent.TimeUnit

/**
 * Handles creation of Notification Channels, showing local notifications,
 * and scheduling background reminders via Android WorkManager.
 */
object NotificationHelper {

    const val CHANNEL_ID = "studybloom_daily_reminders"
    private const val WORK_TAG_DAILY_REMINDER = "studybloom_daily_reminder_work"
    private const val NOTIFICATION_ID = 1001

    /**
     * Initializes the Notification Channel required on Android 8.0 (API 26) and above.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Displays the cute reminder notification to the user.
     */
    fun showRevisionNotification(context: Context) {
        createNotificationChannel(context)

        // Intent to launch MainActivity when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_studybloom_logo)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Notification permission might not be granted on Android 13+
            e.printStackTrace()
        }
    }

    /**
     * Schedules a recurring 24-hour background reminder using WorkManager.
     */
    fun scheduleDailyReminder(context: Context) {
        val reminderRequest = PeriodicWorkRequestBuilder<DailyRevisionWorker>(
            24, TimeUnit.HOURS
        ).addTag(WORK_TAG_DAILY_REMINDER).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    /**
     * Cancels the scheduled WorkManager reminder.
     */
    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG_DAILY_REMINDER)
    }
}
