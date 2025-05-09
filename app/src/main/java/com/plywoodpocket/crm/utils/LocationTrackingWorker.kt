package com.plywoodpocket.crm.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

import com.plywoodpocket.crm.utils.LocationCache
import com.plywoodpocket.crm.utils.SimpleLocation


class LocationTrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForeground(getForegroundInfo())

        try {
            val context = applicationContext
            val location = LocationHelper.getCurrentLocation(context)

            if (location != null) {
                // Get the current time in milliseconds as the timestamp
                val timestamp = System.currentTimeMillis() // Get the current timestamp

                val currentLocation = SimpleLocation(location.latitude, location.longitude, timestamp)
                val lastLocation = LocationCache.getLastLocation(context)

                // ✅ Compare distance (in meters)
                if (lastLocation != null) {
                    val distance = calculateDistance(lastLocation, currentLocation)

                    // If user moved less than 30 meters, maybe skip sending
                    if (distance < 30) {
                        return@withContext Result.success() // OR log the idle
                    }
                }

                // ✅ Save current location as last
                LocationCache.saveLastLocation(context, currentLocation)

                val tokenManager = TokenManager(context)
                val apiClient = ApiClient(tokenManager)

                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed.toDouble(),
                    type = "tracking",
                    tracked_at = timestamp.toString() // Optionally, you can send timestamp as String
                )

                val response = apiClient.apiService.trackLocation(locationData)



                return@withContext if (response.isSuccessful) Result.success() else Result.retry()
            } else {
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            return@withContext Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationHelper.buildTrackingNotification(applicationContext)
        return ForegroundInfo(1001, notification)
    }

    // ✅ Distance Calculation (in meters)
    private fun calculateDistance(loc1: SimpleLocation, loc2: SimpleLocation): Float {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(
            loc1.latitude, loc1.longitude,
            loc2.latitude, loc2.longitude,
            result
        )
        return result[0]
    }
}

