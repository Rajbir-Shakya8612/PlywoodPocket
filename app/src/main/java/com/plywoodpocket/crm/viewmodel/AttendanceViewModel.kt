package com.plywoodpocket.crm.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.plywoodpocket.crm.api.ApiClient
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.models.CheckInRequest
import com.plywoodpocket.crm.models.CheckOutRequest
import com.plywoodpocket.crm.utils.TokenManager
import com.plywoodpocket.crm.utils.LocationHelper
import kotlinx.coroutines.launch


class AttendanceViewModel( application: Application, private val tokenManager: TokenManager, private val api: ApiService ) : ViewModel() {

    var attendanceStatus by mutableStateOf("loading")
    var loading by mutableStateOf(false)

    fun fetchAttendanceStatus() = viewModelScope.launch {
        loading = true
        try {
            val response = api.getAttendanceStatus()
            attendanceStatus = if (response.isSuccessful) {
                response.body()?.status ?: "none"
            } else "none"
        } catch (e: Exception) {
            attendanceStatus = "none"
        }
        loading = false
    }

    fun performCheckIn(context: Context) = viewModelScope.launch {
        loading = true

        val location = LocationHelper.getCurrentLocation(context)
        if (location != null) {
            val request = CheckInRequest(
                check_in_latitude = location.latitude,
                check_in_longitude = location.longitude,
                check_in_accuracy = location.accuracy.toDouble(),
                check_in_address = LocationHelper.reverseGeocode(context, location.latitude, location.longitude)
            )
            try {
                val response = api.checkIn(request)
                if (response.isSuccessful) {
                    attendanceStatus = "checked_in"
                }
            } catch (e: Exception) {
                // Handle network error
            }
        }

        loading = false
    }

    fun performCheckOut(context: Context) = viewModelScope.launch {
        loading = true

        val location = LocationHelper.getCurrentLocation(context)
        if (location != null) {
            val request = CheckOutRequest(
                check_out_latitude = location.latitude,
                check_out_longitude = location.longitude,
                check_out_accuracy = location.accuracy.toDouble(),
                check_out_address = LocationHelper.reverseGeocode(context, location.latitude, location.longitude)
            )
            try {
                val response = api.checkOut(request)
                if (response.isSuccessful) {
                    attendanceStatus = "checked_out"
                }
            } catch (e: Exception) {
                // Handle network error
            }
        }

        loading = false
    }
}