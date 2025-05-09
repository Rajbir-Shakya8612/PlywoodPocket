package com.plywoodpocket.crm.models

import com.plywoodpocket.crm.models.LocationData

data class AttendanceCheckInRequest(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val checkInTime: String
)

data class AttendanceCheckOutRequest(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val checkOutTime: String
)

data class AttendanceResponse(
    val status: String,
    val message: String,
    val data: AttendanceHistoryItem?
)

data class AttendanceHistoryItem(
    val date: String,
    val checkInTime: String?,
    val checkOutTime: String?,
    val workingHours: String?,
    val checkInLocation: String?,
    val checkOutLocation: String?
)

data class AttendanceStatusResponse(
    val success: Boolean,
    val message: String,
    val status: String?,
    val check_in_time: String? = null,
    val check_out_time: String? = null
)

data class CheckInRequest(
    val check_in_latitude: Double,
    val check_in_longitude: Double,
    val check_in_accuracy: Double,
    val check_in_address: String
)

data class CheckOutRequest(
    val check_out_latitude: Double,
    val check_out_longitude: Double,
    val check_out_accuracy: Double,
    val check_out_address: String
)
