package com.plywoodpocket.crm.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocationHelper {

    private fun getFusedLocationProvider(context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

//    @SuppressLint("MissingPermission")
//    suspend fun getCurrentLocation(context: Context): Location? {
//        val fusedClient = getFusedLocationProvider(context)
//
//        return suspendCancellableCoroutine { cont ->
//            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
//                .addOnSuccessListener { location ->
//                    cont.resume(location)
//                }
//                .addOnFailureListener { e ->
//                    cont.resumeWithException(e)
//                }
//        }
//    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        val request = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000 // interval 1 second
        )
            .setMaxUpdates(1)
            .build()

        return suspendCancellableCoroutine { cont ->
            fusedClient.requestLocationUpdates(
                request,
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        fusedClient.removeLocationUpdates(this)
                        val location = result.lastLocation
                        cont.resume(location)
                    }

                    override fun onLocationAvailability(p0: com.google.android.gms.location.LocationAvailability) {
                        if (!p0.isLocationAvailable) {
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
