package com.plywoodpocket.crm.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.google.gson.Gson
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.models.*
import com.plywoodpocket.crm.utils.LocationHelper
import com.plywoodpocket.crm.utils.LocationServiceHelper
import com.plywoodpocket.crm.utils.LocationTrackingWorker
import com.plywoodpocket.crm.utils.TokenManager
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AttendanceViewModel(
    application: Application,
    private val tokenManager: TokenManager,
    private val api: ApiService
) : ViewModel() {

    var attendanceStatus by mutableStateOf("loading")
    var loading by mutableStateOf(false)
    var checkInTime by mutableStateOf<String?>(null)
    var checkOutTime by mutableStateOf<String?>(null)
    var workingHours by mutableStateOf<String?>(null)
    var status by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var showLocationDialog by mutableStateOf(false)

    init {
        fetchAttendanceStatus()
    }

    fun fetchAttendanceStatus() = viewModelScope.launch {
        loading = true
        try {
            val response = api.getAttendanceStatus()
            if (response.isSuccessful) {
                response.body()?.let { statusResponse ->
                    attendanceStatus = when {
                        statusResponse.canCheckIn -> "none"
                        statusResponse.canCheckOut -> "checked_in"
                        else -> "checked_out"
                    }
                    statusResponse.attendance?.let { attendance ->
                        checkInTime = attendance.check_in_time
                        checkOutTime = attendance.check_out_time
                        workingHours = attendance.working_hours
                        status = attendance.status
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                if (response.code() == 401 || errorBody?.contains("Unauthenticated", ignoreCase = true) == true) {
                    tokenManager.clearAuthData()
                    errorMessage = "Session expired. Please login again."
                    attendanceStatus = "none"
                } else {
                    attendanceStatus = "none"
                    errorMessage = "Failed to fetch attendance status"
                }
            }
        } catch (e: Exception) {
            attendanceStatus = "none"
            errorMessage = "Error: ${e.message}"
            e.printStackTrace()
        }
        loading = false
    }

    fun performCheckIn(context: Context) = viewModelScope.launch {
        loading = true
        errorMessage = null
        try {
            if (!LocationServiceHelper.isLocationEnabled(context)) {
                showLocationDialog = true
                loading = false
                return@launch
            }

            val location = LocationHelper.getCurrentLocation(context)
            if (location != null) {
                Log.d("AttendanceViewModel", "Location received: ${location.latitude}, ${location.longitude}")
                
                val address = LocationHelper.reverseGeocode(context, location.latitude, location.longitude)
                Log.d("AttendanceViewModel", "Address: $address")

                // Create location JSON object
                val locationData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy
                )
                val locationJson = Gson().toJson(locationData)

                val request = CheckInRequest(check_in_location = locationJson)
                val response = api.checkIn(request)
                Log.d("AttendanceViewModel", "Check-in response: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    attendanceStatus = "checked_in"
                    startLocationTracking(context)
                    fetchAttendanceStatus() // Refresh status after check-in
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (response.code() == 401 || errorBody?.contains("Unauthenticated", ignoreCase = true) == true) {
                        tokenManager.clearAuthData()
                        errorMessage = "Session expired. Please login again."
                        attendanceStatus = "none"
                    } else {
                        errorMessage = "Check-in failed: $errorBody"
                        attendanceStatus = "none"
                    }
                }
            } else {
                errorMessage = "Could not get location. Please ensure location services are enabled."
                attendanceStatus = "none"
            }
        } catch (e: Exception) {
            errorMessage = "Error during check-in: ${e.message}"
            attendanceStatus = "none"
            e.printStackTrace()
        }
        loading = false
    }

    fun performCheckOut(context: Context) = viewModelScope.launch {
        loading = true
        errorMessage = null
        try {
            if (!LocationServiceHelper.isLocationEnabled(context)) {
                showLocationDialog = true
                loading = false
                return@launch
            }

            val location = LocationHelper.getCurrentLocation(context)
            if (location != null) {
                Log.d("AttendanceViewModel", "Location received for check-out: ${location.latitude}, ${location.longitude}")
                
                val address = LocationHelper.reverseGeocode(context, location.latitude, location.longitude)
                Log.d("AttendanceViewModel", "Check-out address: $address")

                // Create location JSON object
                val locationData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy
                )
                val locationJson = Gson().toJson(locationData)

                val request = CheckOutRequest(check_out_location = locationJson)
                val response = api.checkOut(request)
                Log.d("AttendanceViewModel", "Check-out response: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    attendanceStatus = "checked_out"
                    stopLocationTracking(context)
                    fetchAttendanceStatus() // Refresh status after check-out
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (response.code() == 401 || errorBody?.contains("Unauthenticated", ignoreCase = true) == true) {
                        tokenManager.clearAuthData()
                        errorMessage = "Session expired. Please login again."
                        attendanceStatus = "checked_in"
                    } else {
                        errorMessage = "Check-out failed: $errorBody"
                        attendanceStatus = "checked_in"
                    }
                }
            } else {
                errorMessage = "Could not get location. Please ensure location services are enabled."
                attendanceStatus = "checked_in"
            }
        } catch (e: Exception) {
            errorMessage = "Error during check-out: ${e.message}"
            attendanceStatus = "checked_in"
            e.printStackTrace()
        }
        loading = false
    }

    private fun startLocationTracking(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val locationWorkRequest = PeriodicWorkRequestBuilder<LocationTrackingWorker>(
            15, TimeUnit.MINUTES,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "location_tracking",
            ExistingPeriodicWorkPolicy.KEEP,
            locationWorkRequest
        )
    }

    private fun stopLocationTracking(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("location_tracking")
    }
}
