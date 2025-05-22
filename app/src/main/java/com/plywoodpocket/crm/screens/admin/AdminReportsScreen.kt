package com.plywoodpocket.crm.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plywoodpocket.crm.viewmodel.ReportsViewModel
import com.plywoodpocket.crm.viewmodel.ReportsUiState

@Composable
fun AdminReportsScreen(
    reportsViewModel: ReportsViewModel
) {
    val uiState by reportsViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        reportsViewModel.loadAllReports()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is ReportsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ReportsUiState.Error -> {
                val message = (uiState as ReportsUiState.Error).message
                Text(
                    text = "Error: $message",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ReportsUiState.Success -> {
                val data = (uiState as ReportsUiState.Success)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text("Admin Reports", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    data.dashboard?.let { DashboardStatsSection(it) }
                    Spacer(modifier = Modifier.height(24.dp))
                    data.attendance?.let { AttendanceChartSection(it) }
                    Spacer(modifier = Modifier.height(24.dp))
                    data.performance?.let { PerformanceChartSection(it) }
                }
            }
        }
    }
}

@Composable
fun DashboardStatsSection(dashboard: com.plywoodpocket.crm.models.AdminDashboardReportResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Salespersons", dashboard.totalSalespersons?.toString() ?: "-")
                StatItem("Leads", dashboard.totalLeads?.toString() ?: "-")
                StatItem("Sales", dashboard.totalSales?.toString() ?: "-")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Attendance %", dashboard.todayAttendance?.toString() ?: "-")
                StatItem("Lead %", dashboard.leadChange?.let { "${"%.1f".format(it)}%" } ?: "-")
                StatItem("Sales %", dashboard.salesChange?.let { "${"%.1f".format(it)}%" } ?: "-")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun AttendanceChartSection(attendance: com.plywoodpocket.crm.models.AttendanceOverviewResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Attendance (Last 30 Days)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // Placeholder for chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("[Attendance Chart Here]", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Present", attendance.presentCount?.toString() ?: "-")
                StatItem("Absent", attendance.absentCount?.toString() ?: "-")
                StatItem("Late", attendance.lateCount?.toString() ?: "-")
            }
        }
    }
}

@Composable
fun PerformanceChartSection(performance: com.plywoodpocket.crm.models.PerformanceOverviewResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Performance (This Month)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // Placeholder for chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("[Performance Chart Here]", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Present", performance.totalPresent?.toString() ?: "-")
                StatItem("Absent", performance.totalAbsent?.toString() ?: "-")
                StatItem("Late", performance.totalLate?.toString() ?: "-")
            }
        }
    }
} 