package com.plywoodpocket.crm.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import android.os.Build

object WorkManagerScheduler {
    private const val LOCATION_TRACKING_WORK = "location_tracking"

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

        val workRequest = PeriodicWorkRequestBuilder<FollowUpReminderWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "FollowUpReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
