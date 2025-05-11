package com.plywoodpocket.crm.models

import com.google.gson.annotations.SerializedName

// Main lead data class
data class Lead(
    val id: Int? = null,
    val name: String,
    val phone: String,
    val email: String,
    val address: String?,
    val status_id: Int,
    val status: LeadStatus? = null,
    val follow_up_date: String?,
    val notes: String?,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val location: String?,
    val pincode: String?,
    val company: String?,
    val additional_info: String?,
    val source: String?,
    val expected_amount: Double?,
    val lost_reason: String? = null,
    val closed_won_reason: String? = null
)

data class LeadStatus(
    val id: Int,
    val name: String,
    val color: String
)

data class LeadsResponse(
    val lead_statuses: List<LeadStatus>,
    val leads: List<Lead>,
    val chart_data: List<LeadChartData>?
)

data class LeadChartData(
    val name: String,
    val count: Int,
    val percentage: Double,
    val color: String
)

data class LeadResponse(
    val success: Boolean,
    val message: String?,
    val lead: Lead?
)

data class LeadRequest(
    val name: String,
    val phone: String,
    val email: String,
    val address: String?,
    val status_id: Int,
    val follow_up_date: String?,
    val notes: String?,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val location: String?,
    val pincode: String?,
    val company: String?,
    val additional_info: String?,
    val source: String?,
    val expected_amount: Double?,
    val lost_reason: String? = null,
    val closed_won_reason: String? = null
)

data class UpdateStatusRequest(
    val status_id: Int,
    val reason: String? = null
)

data class FollowUpRequest(
    val next_follow_up: String,
    val notes: String? = null
)

data class LeadStatsResponse(
    val success: Boolean,
    val stats: LeadStats
)

data class LeadStats(
    val total_leads: Int,
    val leads_by_status: Map<Int, Int>,
    val total_value: Double,
    val conversion_rate: Double
)

data class FollowUpHistoryResponse(
    val success: Boolean,
    val history: List<FollowUpHistory>
)

data class FollowUpHistory(
    val id: Int,
    val notes: String?,
    val date: String?,
    val user: User?
)

data class CompleteFollowUpRequest(
    val notes: String,
    val next_follow_up_date: String?,
    val next_follow_up_notes: String?
)

data class FollowUpsResponse(
    val success: Boolean,
    val followUps: List<LeadFollowUp>
)

data class LeadFollowUp(
    val id: Int,
    val name: String?,
    val follow_up_date: String?,
    val phone: String?,
    val email: String?,
    val company: String?,
    val is_overdue: Boolean?,
    val days_left: Int?,
    val readable_diff: String?
)