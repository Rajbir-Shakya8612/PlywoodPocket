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

data class AdminLocationTimelineResponse(
    val tracks: List<AdminLocationTrack>,
    val stats: AdminLocationStats
)

data class AdminLocationTrack(
    val id: Int,
    val user: String?,
    val user_id: Int?,
    val latitude: Double,
    val longitude: Double,
    val type: String?,
    val accuracy: Float?,
    val date: String?,
    val time: String?,
    val stay_duration: String? = null,
    val exit_timestamp: String? = null
)

data class AdminLocationStats(
    val total_locations: Int?,
    val total_distance: Double?,
    val average_speed: Double?,
    val total_time: String?,
    val check_ins: Int? = null,
    val check_outs: Int? = null
)

data class AdminLocationTracksResponse(
    val data: List<AdminLocationTrack>
)

data class AdminLocationStatsResponse(
    val total_locations: Int?,
    val total_distance: Double?,
    val average_speed: Double?,
    val total_time: String?,
    val check_ins: Int? = null,
    val check_outs: Int? = null
)

data class AdminLocationDetailedTracksResponse(
    val data: List<AdminLocationTrack>
)


