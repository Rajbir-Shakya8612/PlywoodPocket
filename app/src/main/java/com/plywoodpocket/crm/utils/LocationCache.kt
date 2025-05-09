package com.plywoodpocket.crm.utils

import android.content.Context

data class SimpleLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

object LocationCache {
    fun saveLastLocation(context: Context, location: SimpleLocation) {
        val prefs = context.getSharedPreferences("location_cache", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("lat", java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong("lng", java.lang.Double.doubleToRawLongBits(location.longitude))
            .putLong("timestamp", location.timestamp)
            .apply()
    }

    fun getLastLocation(context: Context): SimpleLocation? {
        val prefs = context.getSharedPreferences("location_cache", Context.MODE_PRIVATE)
        val lat = java.lang.Double.longBitsToDouble(prefs.getLong("lat", Long.MIN_VALUE))
        val lng = java.lang.Double.longBitsToDouble(prefs.getLong("lng", Long.MIN_VALUE))
        val ts = prefs.getLong("timestamp", -1)
        return if (lat != Long.MIN_VALUE.toDouble() && lng != Long.MIN_VALUE.toDouble()) {
            SimpleLocation(lat, lng, ts)
        } else null
    }
}
