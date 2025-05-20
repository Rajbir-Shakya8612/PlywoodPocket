package com.plywoodpocket.crm.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

class LocationTrackingWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val apiClient = ApiClient(TokenManager(appContext)).apiService
    private val TAG = "LocationTrackingWorker"
    
    companion object {
        // Queue to store offline locations
        private val offlineLocationQueue = ConcurrentLinkedQueue<LocationData>()
        
        // Flag to track if sync is in progress
        private var isSyncing = false
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting location tracking work")
            
            // First check if user is still checked in
            val statusResponse = if (isNetworkAvailable(applicationContext)) {
                apiClient.getAttendanceStatus()
            } else {
                Log.d(TAG, "Network unavailable, using cached status")
                null
            }

            if (statusResponse != null) {
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

                if (isNetworkAvailable(applicationContext)) {
                    // Try to sync any offline locations first
                    syncOfflineLocations()
                    
                    // Send current location
                    val response = apiClient.trackLocation(locationData)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Successfully tracked location")
                        return@withContext Result.success()
                    } else {
                        Log.e(TAG, "Failed to track location: ${response.code()}")
                        // Add to offline queue if network request fails
                        offlineLocationQueue.add(locationData)
                        LocationCache.saveOfflineLocation(applicationContext, locationData)
                        return@withContext Result.retry()
                    }
                } else {
                    // Network is offline, store location for later sync
                    Log.d(TAG, "Network offline, caching location")
                    offlineLocationQueue.add(locationData)
                    LocationCache.saveOfflineLocation(applicationContext, locationData)
                    return@withContext Result.success()
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

    private suspend fun syncOfflineLocations() {
        if (isSyncing) return
        isSyncing = true
        
        try {
            val offlineLocations = LocationCache.getOfflineLocations(applicationContext)
            Log.d(TAG, "Syncing ${offlineLocations.size} offline locations")
            
            for (locationData in offlineLocations) {
                try {
                    val response = apiClient.trackLocation(locationData)
                    if (response.isSuccessful) {
                        // Remove from queue and cache after successful sync
                        offlineLocationQueue.remove(locationData)
                        LocationCache.removeOfflineLocation(applicationContext, locationData)
                    } else {
                        Log.e(TAG, "Failed to sync offline location: ${response.code()}")
                        break // Stop syncing if we hit an error
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing offline location: ${e.message}")
                    break
                }
            }
        } finally {
            isSyncing = false
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
