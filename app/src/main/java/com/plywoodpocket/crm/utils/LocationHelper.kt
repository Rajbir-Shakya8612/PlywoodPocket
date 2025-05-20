package com.plywoodpocket.crm.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocationHelper {
    private const val TAG = "LocationHelper"

    private fun getFusedLocationProvider(context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.Main) {
        try {
            val fusedClient = getFusedLocationProvider(context)
            val maxWaitMillis = 5000L // 5 seconds
            val minAccuracy = 50f // meters

            // Try last location first
            try {
                val lastLocation = fusedClient.lastLocation.await()
                if (lastLocation != null) {
                    val now = System.currentTimeMillis()
                    val isRecent = (now - lastLocation.time) < 10_000 // 10 seconds
                    val isAccurate = lastLocation.accuracy < minAccuracy
                    if (isRecent && isAccurate) {
                        Log.d(TAG, "Using last known location")
                        return@withContext lastLocation
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting last location: ${e.message}")
            }

            // Request a new location update
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
            )
                .setMaxUpdates(5)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(maxWaitMillis)
                .build()

            return@withContext suspendCancellableCoroutine { cont ->
                try {
                    val callback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            val location = result.lastLocation
                            val isAccurate = location != null && location.accuracy < minAccuracy
                            val isTimeout = System.currentTimeMillis() - System.currentTimeMillis() > maxWaitMillis
                            
                            if (isAccurate || isTimeout) {
                                fusedClient.removeLocationUpdates(this)
                                cont.resume(location)
                            }
                        }
                    }

                    // Ensure we're on the main thread for location updates
                    if (Looper.myLooper() == null) {
                        Looper.prepare()
                    }

                    fusedClient.requestLocationUpdates(
                        request,
                        callback,
                        Looper.getMainLooper()
                    )

                    cont.invokeOnCancellation {
                        try {
                            fusedClient.removeLocationUpdates(callback)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing location updates: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting location updates: ${e.message}")
                    cont.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrentLocation: ${e.message}")
            null
        }
    }

    fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.get(0)?.getAddressLine(0) ?: "Unknown Location"
        } catch (e: Exception) {
            Log.e(TAG, "Error in reverseGeocode: ${e.message}")
            "Unknown Location"
        }
    }
}
