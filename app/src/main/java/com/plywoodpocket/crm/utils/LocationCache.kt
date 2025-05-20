package com.plywoodpocket.crm.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.plywoodpocket.crm.models.LocationData
import android.util.Log

data class SimpleLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

object LocationCache {
    private const val TAG = "LocationCache"
    private const val PREF_NAME = "location_cache"
    private const val KEY_LAST_LOCATION = "last_location"
    private const val KEY_OFFLINE_LOCATIONS = "offline_locations"
    private val gson = Gson()

    fun saveLastLocation(context: Context, location: SimpleLocation) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("lat", java.lang.Double.doubleToRawLongBits(location.latitude))
                .putLong("lng", java.lang.Double.doubleToRawLongBits(location.longitude))
                .putLong("timestamp", location.timestamp)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving last location: ${e.message}")
        }
    }

    fun getLastLocation(context: Context): SimpleLocation? {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val lat = java.lang.Double.longBitsToDouble(prefs.getLong("lat", Long.MIN_VALUE))
            val lng = java.lang.Double.longBitsToDouble(prefs.getLong("lng", Long.MIN_VALUE))
            val ts = prefs.getLong("timestamp", -1)
            return if (lat != Long.MIN_VALUE.toDouble() && lng != Long.MIN_VALUE.toDouble()) {
                SimpleLocation(lat, lng, ts)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last location: ${e.message}")
            return null
        }
    }

    fun saveOfflineLocation(context: Context, locationData: LocationData) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val offlineLocations = getOfflineLocations(context).toMutableList()
            offlineLocations.add(locationData)
            
            // Keep only last 100 offline locations to prevent memory issues
            if (offlineLocations.size > 100) {
                offlineLocations.removeAt(0)
            }
            
            prefs.edit()
                .putString(KEY_OFFLINE_LOCATIONS, gson.toJson(offlineLocations))
                .apply()
            
            Log.d(TAG, "Saved offline location, total offline locations: ${offlineLocations.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving offline location: ${e.message}")
        }
    }

    fun getOfflineLocations(context: Context): List<LocationData> {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_OFFLINE_LOCATIONS, null) ?: return emptyList()
            val type = object : TypeToken<List<LocationData>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting offline locations: ${e.message}")
            return emptyList()
        }
    }

    fun removeOfflineLocation(context: Context, locationData: LocationData) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val offlineLocations = getOfflineLocations(context).toMutableList()
            offlineLocations.remove(locationData)
            
            prefs.edit()
                .putString(KEY_OFFLINE_LOCATIONS, gson.toJson(offlineLocations))
                .apply()
            
            Log.d(TAG, "Removed offline location, remaining offline locations: ${offlineLocations.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing offline location: ${e.message}")
        }
    }

    fun clearOfflineLocations(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_OFFLINE_LOCATIONS)
                .apply()
            Log.d(TAG, "Cleared all offline locations")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing offline locations: ${e.message}")
        }
    }
}
