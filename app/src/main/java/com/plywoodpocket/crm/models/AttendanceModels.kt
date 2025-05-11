package com.plywoodpocket.crm.models

data class AttendanceStatusResponse(
    val attendance: Attendance?,
    val canCheckIn: Boolean,
    val canCheckOut: Boolean
)

data class Attendance(
    val check_in_time: String? = null,
    val check_out_time: String? = null,
    val working_hours: String? = null,
    val status: String? = null
)

data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val check_in_time: String? = null,
    val check_out_time: String? = null,
    val status: String? = null,
    val working_hours: String? = null
)

data class CheckInRequest(
    val check_in_location: String // This will be a JSON string containing location data
)

data class CheckOutRequest(
    val check_out_location: String // This will be a JSON string containing location data
)
