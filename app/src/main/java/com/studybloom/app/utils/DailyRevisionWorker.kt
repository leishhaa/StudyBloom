package com.studybloom.app.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Background WorkManager Worker executed periodically to remind students to revise.
 * Beginner-friendly demonstration of Android WorkManager background jobs.
 */
class DailyRevisionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            // Check if user still has notifications enabled
            val prefs = SharedPrefsHelper(context)
            if (prefs.isDailyReminderEnabled) {
                NotificationHelper.showRevisionNotification(context)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
