package com.plywoodpocket.crm.utils

import android.content.Context
import androidx.work.*
//import com.plywoodpocket.crm.workers.LocationTrackingWorker
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    fun scheduleLocationTracking(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<LocationTrackingWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "location_tracking",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
