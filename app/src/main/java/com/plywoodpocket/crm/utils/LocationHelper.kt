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
        val maxWaitMillis = 5000L // 5 seconds
        val minAccuracy = 50f // meters

        // Try last location, but only if it's very recent and accurate
        try {
            val lastLocation = fusedClient.lastLocation.await()
            if (lastLocation != null) {
                val now = System.currentTimeMillis()
                val isRecent = (now - lastLocation.time) < 10_000 // 10 seconds
                val isAccurate = lastLocation.accuracy < minAccuracy
                if (isRecent && isAccurate) {
                    return lastLocation
                }
            }
        } catch (_: Exception) {}

        // Request a new location update and wait for a good fix
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        )
            .setMaxUpdates(5)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(maxWaitMillis)
            .build()

        return suspendCancellableCoroutine { cont ->
            val startTime = System.currentTimeMillis()
            val callback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val location = result.lastLocation
                    val isAccurate = location != null && location.accuracy < minAccuracy
                    val isTimeout = System.currentTimeMillis() - startTime > maxWaitMillis
                    if (isAccurate || isTimeout) {
                        fusedClient.removeLocationUpdates(this)
                        cont.resume(location)
                    }
                }
            }
            fusedClient.requestLocationUpdates(request, callback, null)
            cont.invokeOnCancellation { fusedClient.removeLocationUpdates(callback) }
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
