package com.plywoodpocket.crm.models

import com.google.gson.annotations.SerializedName

// Sales Plan Models

data class Plan(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("month") val month: Int,
    @SerializedName("year") val year: Int,
    @SerializedName("type") val type: String,
    @SerializedName("lead_target") val leadTarget: Int,
    @SerializedName("sales_target") val salesTarget: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: String,
    @SerializedName("achievements") val achievements: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("progress_percentage") val progressPercentage: Int
)

data class PlanRequest(
    @SerializedName("type") val type: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("lead_target") val leadTarget: Int,
    @SerializedName("sales_target") val salesTarget: Double,
    @SerializedName("description") val description: String,
    @SerializedName("notes") val notes: String?
)

data class PlanResponse(
    val success: Boolean,
    val data: PlanData? = null,
    val plan: Plan? = null,
    val message: String? = null
)

data class PlanData(
    val plans: List<Plan> = emptyList()
    // Add other fields if you want to use them (monthlyTarget, chartData, etc)
) 