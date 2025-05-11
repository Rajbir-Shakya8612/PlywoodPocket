package com.plywoodpocket.crm.models

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Double,
    val type: String,
    val tracked_at: String,
    val address: String? = null,
    val exit_timestamp: String? = null,
    val stay_duration: Int? = null
)


