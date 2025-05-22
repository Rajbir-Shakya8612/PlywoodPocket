package com.plywoodpocket.crm.models

import com.google.gson.annotations.SerializedName

// Admin Dashboard Report Response
// This model is based on the backend response structure you provided.
data class AdminDashboardReportResponse(
    @SerializedName("totalSalespersons") val totalSalespersons: Int?,
    @SerializedName("newSalespersons") val newSalespersons: Int?,
    @SerializedName("todayAttendance") val todayAttendance: Int?,
    @SerializedName("attendanceChange") val attendanceChange: Float?,
    @SerializedName("totalLeads") val totalLeads: Int?,
    @SerializedName("leadChange") val leadChange: Float?,
    @SerializedName("totalSales") val totalSales: Float?,
    @SerializedName("salesChange") val salesChange: Float?,
    @SerializedName("todoTasks") val todoTasks: List<TaskSimple>?,
    @SerializedName("inProgressTasks") val inProgressTasks: List<TaskSimple>?,
    @SerializedName("doneTasks") val doneTasks: List<TaskSimple>?,
    @SerializedName("recentActivities") val recentActivities: List<ActivitySimple>?,
    @SerializedName("attendanceData") val attendanceData: AttendanceChartData?,
    @SerializedName("performanceData") val performanceData: PerformanceChartData?,
    @SerializedName("salespersons") val salespersons: List<UserSimple>?,
    @SerializedName("leadStatuses") val leadStatuses: List<LeadStatusSimple>?,
    @SerializedName("leads") val leads: List<LeadSimple>?,
    @SerializedName("leadChartData") val leadChartData: List<LeadChartDataItem>?
)

data class TaskSimple(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("assignee") val assignee: UserSimple?
)

data class ActivitySimple(
    @SerializedName("id") val id: Int?,
    @SerializedName("type") val type: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("user") val user: UserSimple?
)

data class AttendanceChartData(
    @SerializedName("labels") val labels: List<String>?,
    @SerializedName("present") val present: List<Int>?,
    @SerializedName("absent") val absent: List<Int>?,
    @SerializedName("late") val late: List<Int>?
)

data class PerformanceChartData(
    @SerializedName("labels") val labels: List<String>?,
    @SerializedName("data") val data: List<Float>?
)

data class UserSimple(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?
)

data class LeadStatusSimple(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("color") val color: String?
)

data class LeadSimple(
    @SerializedName("id") val id: Int?,
    @SerializedName("status_id") val statusId: Int?,
    @SerializedName("created_at") val createdAt: String?
)

data class LeadChartDataItem(
    @SerializedName("name") val name: String?,
    @SerializedName("count") val count: Int?,
    @SerializedName("percentage") val percentage: Float?,
    @SerializedName("color") val color: String?
)

// Attendance Overview Response
// For /api/admin/attendance/overview

data class AttendanceOverviewResponse(
    @SerializedName("labels") val labels: List<String>?,
    @SerializedName("present") val present: List<Int>?,
    @SerializedName("absent") val absent: List<Int>?,
    @SerializedName("late") val late: List<Int>?,
    @SerializedName("todayAttendance") val todayAttendance: Int?,
    @SerializedName("presentCount") val presentCount: Int?,
    @SerializedName("absentCount") val absentCount: Int?,
    @SerializedName("lateCount") val lateCount: Int?,
    @SerializedName("success") val success: Boolean? = true
)

// Performance Overview Response
// For /api/admin/performance/overview

data class PerformanceOverviewResponse(
    @SerializedName("labels") val labels: List<String>?,
    @SerializedName("present") val present: List<Int>?,
    @SerializedName("absent") val absent: List<Int>?,
    @SerializedName("late") val late: List<Int>?,
    @SerializedName("totalPresent") val totalPresent: Int?,
    @SerializedName("totalAbsent") val totalAbsent: Int?,
    @SerializedName("totalLate") val totalLate: Int?,
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
) 