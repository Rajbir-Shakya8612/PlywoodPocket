package com.plywoodpocket.crm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReportsUiState {
    object Loading : ReportsUiState()
    data class Success(
        val dashboard: AdminDashboardReportResponse?,
        val attendance: AttendanceOverviewResponse?,
        val performance: PerformanceOverviewResponse?
    ) : ReportsUiState()
    data class Error(val message: String) : ReportsUiState()
}

class ReportsViewModel(private val apiClient: ApiClient) : ViewModel() {
    private val _uiState = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun loadAllReports(date: String? = null, userId: Int? = null, status: String? = null, month: String? = null) {
        _uiState.value = ReportsUiState.Loading
        viewModelScope.launch {
            try {
                val dashboardResp = apiClient.apiService.getAdminDashboardReport()
                val attendanceResp = apiClient.apiService.getAdminAttendanceOverview(date, userId, status)
                val performanceResp = apiClient.apiService.getAdminPerformanceOverview(month)

                val dashboardData = dashboardResp.body()?.data

                if (dashboardResp.isSuccessful && attendanceResp.isSuccessful && performanceResp.isSuccessful) {
                    _uiState.value = ReportsUiState.Success(
                        dashboard = dashboardData,
                        attendance = attendanceResp.body(),
                        performance = performanceResp.body()
                    )
                } else {
                    _uiState.value = ReportsUiState.Error(
                        dashboardResp.message() + "\n" + attendanceResp.message() + "\n" + performanceResp.message()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ReportsUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
} 