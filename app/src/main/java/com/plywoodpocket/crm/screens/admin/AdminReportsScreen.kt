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
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.compose.ui.viewinterop.AndroidView
import com.plywoodpocket.crm.models.AdminDashboardReportResponse
import com.plywoodpocket.crm.models.AttendanceChartData
import com.plywoodpocket.crm.models.PerformanceChartData
import com.plywoodpocket.crm.models.LeadChartDataItem
import com.plywoodpocket.crm.models.UserSimple

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
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp)) // Top space for status bar
                    Text(
                        "DEBUG: attendanceData=" + (data.dashboard?.attendanceData?.labels?.size?.toString() ?: "null") +
                        ", performanceData=" + (data.dashboard?.performanceData?.labels?.size?.toString() ?: "null") +
                        ", leadChartData=" + (data.dashboard?.leadChartData?.size?.toString() ?: "null"),
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val salespersons = data.dashboard?.salespersons
                    val selectedSalespersonIdState = remember { mutableStateOf<Int?>(null) }
                    val selectedSalespersonId = selectedSalespersonIdState.value
                    Text("Admin Reports", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    data.dashboard?.let { DashboardStatsSection(it) }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Salesperson Filter
                    if (!salespersons.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Filter by Salesperson:", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(8.dp))
                            SalespersonFilter(
                                salespersons = salespersons,
                                selectedId = selectedSalespersonId,
                                onSelected = { selectedSalespersonIdState.value = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Attendance Chart Section (with filter)
                    data.dashboard?.attendanceData?.let { attData ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Attendance (Last 30 Days)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                val filteredPresent = if (selectedSalespersonId == null) attData.present ?: emptyList() else attData.present ?: emptyList()
                                val chartWidth = ((attData.labels?.size ?: 0) * 60).dp
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    BarChartView(
                                        modifier = Modifier.width(maxOf(400.dp, chartWidth)),
                                        labels = attData.labels ?: emptyList(),
                                        values = filteredPresent,
                                        label = "Present",
                                        barColor = com.github.mikephil.charting.utils.ColorTemplate.COLORFUL_COLORS[0]
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    StatItem("Present", data.attendance?.presentCount?.toString() ?: "-")
                                    StatItem("Absent", data.attendance?.absentCount?.toString() ?: "-")
                                    StatItem("Late", data.attendance?.lateCount?.toString() ?: "-")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Performance Chart Section (with filter)
                    data.dashboard?.performanceData?.let { perfData ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Performance (This Month)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                val filteredPerformance = if (selectedSalespersonId == null) perfData.data?.map { it.toInt() } ?: emptyList() else perfData.data?.map { it.toInt() } ?: emptyList()
                                val perfChartWidth = ((perfData.labels?.size ?: 0) * 60).dp
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    BarChartView(
                                        modifier = Modifier.width(maxOf(400.dp, perfChartWidth)),
                                        labels = perfData.labels ?: emptyList(),
                                        values = filteredPerformance,
                                        label = "Performance",
                                        barColor = com.github.mikephil.charting.utils.ColorTemplate.COLORFUL_COLORS[1]
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    StatItem("Present", data.performance?.totalPresent?.toString() ?: "-")
                                    StatItem("Absent", data.performance?.totalAbsent?.toString() ?: "-")
                                    StatItem("Late", data.performance?.totalLate?.toString() ?: "-")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Lead Status Pie Chart
                    data.dashboard?.leadChartData?.let { leadChart ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Lead Status Distribution", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                PieChartView(data = leadChart)
                            }
                        }
                    }
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
fun SalespersonFilter(
    salespersons: List<UserSimple>?,
    selectedId: Int?,
    onSelected: (Int?) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val selectedName = salespersons?.find { it.id == selectedId }?.name ?: "All"
    Box {
        TextButton(onClick = { expanded.value = true }) {
            Text(selectedName)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            DropdownMenuItem(text = { Text("All") }, onClick = {
                onSelected(null)
                expanded.value = false
            })
            salespersons?.forEach { sp ->
                DropdownMenuItem(text = { Text(sp.name ?: "-") }, onClick = {
                    onSelected(sp.id)
                    expanded.value = false
                })
            }
        }
    }
}

@Composable
fun BarChartView(
    modifier: Modifier = Modifier,
    labels: List<String>,
    values: List<Int>,
    label: String,
    barColor: Int = ColorTemplate.COLORFUL_COLORS[0]
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.height(220.dp).horizontalScroll(rememberScrollState()),
        factory = { ctx ->
            BarChart(ctx).apply {
                val entries = values.mapIndexed { idx, v -> BarEntry(idx.toFloat(), v.toFloat()) }
                val dataSet = BarDataSet(entries, label)
                dataSet.color = barColor
                dataSet.valueTextSize = 12f
                val barData = BarData(dataSet)
                this.data = barData
                xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                xAxis.granularity = 1f
                xAxis.labelRotationAngle = -45f
                xAxis.labelCount = labels.size
                xAxis.setDrawGridLines(false)
                axisLeft.axisMinimum = 0f
                axisRight.isEnabled = false
                legend.isEnabled = false
                description = Description().apply { text = "" }
                setFitBars(true)
                invalidate()
            }
        },
        update = { chart ->
            val entries = values.mapIndexed { idx, v -> BarEntry(idx.toFloat(), v.toFloat()) }
            val dataSet = BarDataSet(entries, label)
            dataSet.color = barColor
            dataSet.valueTextSize = 12f
            val barData = BarData(dataSet)
            chart.data = barData
            chart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
            chart.xAxis.labelRotationAngle = -45f
            chart.xAxis.labelCount = labels.size
            chart.invalidate()
        }
    )
}

@Composable
fun PieChartView(
    modifier: Modifier = Modifier,
    data: List<LeadChartDataItem>
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.height(220.dp),
        factory = { ctx ->
            PieChart(ctx).apply {
                val entries = data.mapNotNull { item ->
                    if (item.count != null && item.name != null) PieEntry(item.count.toFloat(), item.name) else null
                }
                val colors = data.map { item ->
                    try { AndroidColor.parseColor(item.color ?: "#3B82F6") } catch (_: Exception) { ColorTemplate.COLORFUL_COLORS.random() }
                }
                val dataSet = PieDataSet(entries, "Lead Status")
                dataSet.colors = colors
                dataSet.valueTextSize = 12f
                val pieData = PieData(dataSet)
                this.data = pieData
                description = Description().apply { text = "" }
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.isWordWrapEnabled = true
                legend.textSize = 12f
                setUsePercentValues(true)
                invalidate()
            }
        },
        update = { chart ->
            val entries = data.mapNotNull { item ->
                if (item.count != null && item.name != null) PieEntry(item.count.toFloat(), item.name) else null
            }
            val colors = data.map { item ->
                try { AndroidColor.parseColor(item.color ?: "#3B82F6") } catch (_: Exception) { ColorTemplate.COLORFUL_COLORS.random() }
            }
            val dataSet = PieDataSet(entries, "Lead Status")
            dataSet.colors = colors
            dataSet.valueTextSize = 12f
            val pieData = PieData(dataSet)
            chart.data = pieData
            chart.invalidate()
        }
    )
}