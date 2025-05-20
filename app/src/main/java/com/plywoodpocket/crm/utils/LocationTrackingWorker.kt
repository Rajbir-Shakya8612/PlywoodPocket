package com.plywoodpocket.crm.utils

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class LocationTrackingWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val apiClient = ApiClient(TokenManager(appContext)).apiService
    private val TAG = "LocationTrackingWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting location tracking work")
            
            // First check if user is still checked in
            val statusResponse = apiClient.getAttendanceStatus()
            if (!statusResponse.isSuccessful) {
                Log.e(TAG, "Failed to get attendance status: ${statusResponse.code()}")
                return@withContext Result.retry()
            }

            val canCheckOut = statusResponse.body()?.canCheckOut ?: false
            if (!canCheckOut) {
                Log.d(TAG, "User is not checked in, stopping location service")
                stopLocationService()
                return@withContext Result.success()
            }

            // Start the foreground service if not already running
            startLocationService()

            // Get current location with retry logic
            var location = LocationHelper.getCurrentLocation(applicationContext)
            var retryCount = 0
            while (location == null && retryCount < 3) {
                Log.d(TAG, "Retrying location fetch attempt ${retryCount + 1}")
                kotlinx.coroutines.delay(2000) // Wait 2 seconds between retries
                location = LocationHelper.getCurrentLocation(applicationContext)
                retryCount++
            }

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

                // Cache the location
                LocationCache.saveLastLocation(applicationContext, 
                    SimpleLocation(location.latitude, location.longitude, System.currentTimeMillis()))

                val response = apiClient.trackLocation(locationData)
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully tracked location")
                    return@withContext Result.success()
                } else {
                    Log.e(TAG, "Failed to track location: ${response.code()}")
                    return@withContext Result.retry()
                }
            } else {
                Log.e(TAG, "Failed to get location after retries")
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in location tracking: ${e.message}", e)
            return@withContext Result.retry()
        }
    }

    private fun startLocationService() {
        try {
            val intent = Intent(applicationContext, LocationTrackingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            Log.d(TAG, "Location service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location service: ${e.message}", e)
        }
    }

    private fun stopLocationService() {
        try {
            val intent = Intent(applicationContext, LocationTrackingService::class.java)
            applicationContext.stopService(intent)
            Log.d(TAG, "Location service stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location service: ${e.message}", e)
        }
    }
}
