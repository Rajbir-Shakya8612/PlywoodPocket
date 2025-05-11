package com.plywoodpocket.crm.utils

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationTrackingWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val apiClient = ApiClient(TokenManager(appContext)).apiService

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // First check if user is still checked in
            val statusResponse = apiClient.getAttendanceStatus()
            if (!statusResponse.isSuccessful || !statusResponse.body()?.canCheckOut!!) {
                // User is not checked in or already checked out
                stopLocationService()
                return@withContext Result.success()
            }

            // Start the foreground service if not already running
            startLocationService()

            val location = LocationHelper.getCurrentLocation(applicationContext)
            if (location != null) {
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed.toDouble(),
                    type = "tracking",
                    tracked_at = System.currentTimeMillis().toString(),
                    address = LocationHelper.reverseGeocode(applicationContext, location.latitude, location.longitude)
                )

                val response = apiClient.trackLocation(locationData)
                return@withContext if (response.isSuccessful) Result.success() else Result.retry()
            } else {
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            return@withContext Result.retry()
        }
    }

    private fun startLocationService() {
        val intent = Intent(applicationContext, LocationTrackingService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    private fun stopLocationService() {
        val intent = Intent(applicationContext, LocationTrackingService::class.java)
        applicationContext.stopService(intent)
    }
}
