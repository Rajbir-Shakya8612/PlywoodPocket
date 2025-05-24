package com.plywoodpocket.crm.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.data.Entry
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Info

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
                val data = uiState as ReportsUiState.Success
                val dashboard = data.dashboard
                val attendance = data.attendance
                val performance = data.performance
                val salespersons = dashboard?.salespersons
                val selectedSalespersonIdState = remember { mutableStateOf<Int?>(null) }
                val selectedSalespersonId = selectedSalespersonIdState.value
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* TODO: Back navigation logic yahan likho */ }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Admin Reports", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    dashboard?.let { DashboardStatsSection(it) }
                    Spacer(modifier = Modifier.height(24.dp))
//                    if (!salespersons.isNullOrEmpty()) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Text("Filter by Salesperson:", fontWeight = FontWeight.Medium)
//                            Spacer(modifier = Modifier.width(8.dp))
//                            SalespersonFilter(
//                                salespersons = salespersons,
//                                selectedId = selectedSalespersonId,
//                                onSelected = { selectedSalespersonIdState.value = it }
//                            )
//                        }
//                        Spacer(modifier = Modifier.height(16.dp))
//                    }
                    dashboard?.attendanceData?.let { attData ->
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
                                    StatItem("Present", attendance?.presentCount?.toString() ?: "-")
                                    StatItem("Absent", attendance?.absentCount?.toString() ?: "-")
                                    StatItem("Late", attendance?.lateCount?.toString() ?: "-")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    dashboard?.performanceData?.let { perfData ->
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
                                    StatItem("Present", performance?.totalPresent?.toString() ?: "-")
                                    StatItem("Absent", performance?.totalAbsent?.toString() ?: "-")
                                    StatItem("Late", performance?.totalLate?.toString() ?: "-")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    dashboard?.leadChartData?.let { leadChart ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Lead Status Distribution", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    leadChart.forEach { item ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .background(
                                                        color = try {
                                                            Color(android.graphics.Color.parseColor(item.color ?: "#3B82F6"))
                                                        } catch (e: Exception) {
                                                            Color(0xFF3B82F6)
                                                        },
                                                        shape = androidx.compose.foundation.shape.CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(item.name ?: "-", fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                                PieChartView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(420.dp),
                                    data = leadChart
                                )
                            }
                        }
                    }
                }
            }
        }
        // Overlay: Status bar black background (always on top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Color.Black)
                .align(Alignment.TopStart)
        )
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
            var showLeadInfoDialog = remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Attendance (Today)", dashboard.todayAttendance?.toString() ?: "-")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentLeads = dashboard.totalLeads ?: 0
                    val leadChange = dashboard.leadChange ?: 0f
                    val previousLeads = if (leadChange != -100f) {
                        (currentLeads * 100f / (100f + leadChange)).toInt()
                    } else {
                        0
                    }
                    val leadChangeText = "Lead % shows the percentage change in leads compared to the previous period.\nPrevious period: $previousLeads leads\nCurrent period: $currentLeads leads"
                    StatItem("Lead %", dashboard.leadChange?.let { "${"%.1f".format(it)}%" } ?: "-")
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { showLeadInfoDialog.value = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    InfoTooltipDialog(
                        showDialog = showLeadInfoDialog.value,
                        onDismiss = { showLeadInfoDialog.value = false },
                        title = "Lead % Info",
                        message = leadChangeText
                    )
                }
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
    val selectedPercentage = remember { mutableStateOf<String?>(null) }
    Box(modifier = modifier.height(420.dp).width(420.dp)) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx ->
                PieChart(ctx).apply {
                    val entries = data.mapNotNull { item ->
                        if (item.count != null) PieEntry(item.count.toFloat()) else null
                    }
                    val colors = data.map { item ->
                        try { AndroidColor.parseColor(item.color ?: "#3B82F6") } catch (_: Exception) { ColorTemplate.COLORFUL_COLORS.random() }
                    }
                    val dataSet = PieDataSet(entries, "")
                    dataSet.colors = colors
                    dataSet.valueTextSize = 14f
                    dataSet.setDrawValues(true)
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return "%.1f%%".format(value)
                        }
                    }
                    dataSet.selectionShift = 30f // More visible highlight
                    val pieData = PieData(dataSet)
                    this.data = pieData
                    description = Description().apply { text = "" }
                    legend.isEnabled = false
                    setUsePercentValues(true)
                    setHighlightPerTapEnabled(true)
                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            val index = h?.x?.toInt() ?: 0
                            val percent = data.getOrNull(index)?.percentage ?: 0.0
                            selectedPercentage.value = "%.1f%%".format(percent)
                        }
                        override fun onNothingSelected() {
                            selectedPercentage.value = null
                        }
                    })
                    invalidate()
                }
            },
            update = { chart ->
                val entries = data.mapNotNull { item ->
                    if (item.count != null) PieEntry(item.count.toFloat()) else null
                }
                val colors = data.map { item ->
                    try { AndroidColor.parseColor(item.color ?: "#3B82F6") } catch (_: Exception) { ColorTemplate.COLORFUL_COLORS.random() }
                }
                val dataSet = PieDataSet(entries, "")
                dataSet.colors = colors
                dataSet.valueTextSize = 14f
                dataSet.setDrawValues(true)
                dataSet.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "%.1f%%".format(value)
                    }
                }
                dataSet.selectionShift = 30f
                val pieData = PieData(dataSet)
                chart.data = pieData
                chart.legend.isEnabled = false
                chart.setHighlightPerTapEnabled(true)
                chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        val index = h?.x?.toInt() ?: 0
                        val percent = data.getOrNull(index)?.percentage ?: 0.0
                        selectedPercentage.value = "%.1f%%".format(percent)
                    }
                    override fun onNothingSelected() {
                        selectedPercentage.value = null
                    }
                })
                chart.invalidate()
            }
        )
        // Overlay for selected percentage
        if (selectedPercentage.value != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = selectedPercentage.value!!,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoTooltipDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    title: String = "Info",
    message: String
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            },
            title = { Text(title) },
            text = { Text(message) }
        )
    }
}