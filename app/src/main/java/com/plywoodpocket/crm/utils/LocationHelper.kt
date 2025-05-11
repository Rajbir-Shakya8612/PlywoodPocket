package com.plywoodpocket.crm.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocationHelper {

    private fun getFusedLocationProvider(context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val fusedClient = getFusedLocationProvider(context)

        try {
            // First try to get last known location immediately
            val lastLocation = fusedClient.lastLocation.await()
            if (lastLocation != null) {
                return lastLocation
            }
        } catch (e: Exception) {
            // If last location fails, continue with requesting new location
        }

        // If last location is not available, request new location
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 500 // interval 0.5 second
        )
            .setMaxUpdates(1)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(1000)
            .build()

        var isResumed = false

        return suspendCancellableCoroutine { cont ->
            fusedClient.requestLocationUpdates(
                request,
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        if (!isResumed) {
                            isResumed = true
                            fusedClient.removeLocationUpdates(this)
                            val location = result.lastLocation
                            cont.resume(location)
                        }
                    }

                    override fun onLocationAvailability(p0: com.google.android.gms.location.LocationAvailability) {
                        if (!p0.isLocationAvailable && !isResumed) {
                            isResumed = true
                            cont.resume(null)
                        }
                    }
                },
                null
            )
        }
    }

    fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.get(0)?.getAddressLine(0) ?: "Unknown Location"
        } catch (e: Exception) {
            "Unknown Location"
        }
    }
}
