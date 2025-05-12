package com.plywoodpocket.crm.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import android.os.Build

object WorkManagerScheduler {
    private const val LOCATION_TRACKING_WORK = "location_tracking"
    private const val FOLLOW_UP_REMINDER_WORK = "follow_up_reminder"

    fun scheduleLocationTracking(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<LocationTrackingWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "LocationTracking",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun stopLocationTracking(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LOCATION_TRACKING_WORK)
    }

    fun scheduleFollowUpReminders(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Schedule initial work request
        val initialWorkRequest = OneTimeWorkRequestBuilder<FollowUpReminderWorker>()
            .setConstraints(constraints)
            .build()

        // Schedule periodic work request
        val periodicWorkRequest = PeriodicWorkRequestBuilder<FollowUpReminderWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .build()

        // Enqueue both initial and periodic work
        WorkManager.getInstance(context).apply {
            // Start with immediate check
            enqueue(initialWorkRequest)
            
            // Then schedule periodic checks
            enqueueUniquePeriodicWork(
                FOLLOW_UP_REMINDER_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        }
    }

    fun stopFollowUpReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FOLLOW_UP_REMINDER_WORK)
    }
}
