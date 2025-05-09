package com.plywoodpocket.crm.screens

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plywoodpocket.crm.models.AttendanceHistoryItem
import com.plywoodpocket.crm.viewmodel.AttendanceViewModel
import com.plywoodpocket.crm.utils.PermissionHandler
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.plywoodpocket.crm.ui.theme.Orange
import com.plywoodpocket.crm.ui.theme.OrangeLight
import com.plywoodpocket.crm.ui.theme.White
import com.plywoodpocket.crm.ui.theme.Gray
import androidx.compose.foundation.border
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.plywoodpocket.crm.utils.LocationTrackingWorker

@SuppressLint("MissingPermission")
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, onBack: () -> Unit = {}) {
    val status = viewModel.attendanceStatus
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchAttendanceStatus()
    }

    when (status) {
        "loading" -> CircularProgressIndicator()
        "none" -> Button(onClick = { viewModel.performCheckIn(context) }) {
            Text("Check In")
        }
        "checked_in" -> Button(onClick = { viewModel.performCheckOut(context) }) {
            Text("Check Out")
        }
        "checked_out" -> Text("Attendance Complete ✅")
    }

    // 📅 Calendar Code – Keep as-is (mark current date)
}


@Composable
fun CustomAttendanceCalendar(
    month: Int,
    year: Int,
    today: Triple<Int, Int, Int>,
    history: List<AttendanceHistoryItem>
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, month)
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = Color(0xFFB0B0B0))
            Text(monthName, color = Color(0xFFB0B0B0), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFB0B0B0))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1976D2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        val totalCells = daysInMonth + firstDayOfWeek
        val rows = (totalCells / 7) + if (totalCells % 7 != 0) 1 else 0
        var day = 1
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    if (cellIndex < firstDayOfWeek || day > daysInMonth) {
                        Box(Modifier.weight(1f).height(36.dp)) {}
                    } else {
                        val statusColor = getAttendanceColorForDay(day, month + 1, year, history)
                        val isToday = today.first == day && today.second == (month + 1) && today.third == year
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor)
                                .border(
                                    BorderStroke(2.dp, if (isToday) Color(0xFF1976D2) else Color.Transparent),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        day++
                    }
                }
            }
        }
    }
}

fun getAttendanceColorForDay(day: Int, month: Int, year: Int, history: List<AttendanceHistoryItem>): Color {
    val dateStr = String.format("%02d-%02d-%04d", day, month, year)
    val item = history.find { it.date == dateStr }
    return when {
        item == null -> Color(0xFFE0E0E0) // gray
        item.checkInTime == null -> Color(0xFFFFCDD2) // red
        item.checkInTime != null && item.checkOutTime == null -> Color(0xFFFFF9C4) // yellow
        item.checkInTime != null && item.checkOutTime != null -> Color(0xFFC8E6C9) // green
        else -> Color(0xFFE0E0E0)
    }
}

fun getDaysInMonth(calendar: Calendar): Int {
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

@Composable
fun AttendanceHistoryCard(item: AttendanceHistoryItem) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF1976D2))
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.date, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.CheckCircle else Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (expanded) Color.Green else Color.Gray
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Check-in: ${item.checkInTime ?: "--"}")
                Text("Check-out: ${item.checkOutTime ?: "--"}")
                Text("Hours: ${item.workingHours ?: "--"}")
                Text("Check-in Location: ${item.checkInLocation ?: "--"}")
                Text("Check-out Location: ${item.checkOutLocation ?: "--"}")
            }
        }
    }
}

fun scheduleLocationTracking(context: android.content.Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
        
    val workRequest = PeriodicWorkRequestBuilder<LocationTrackingWorker>(
        5, TimeUnit.MINUTES
    ).setConstraints(constraints).build()
    
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "LocationTracking",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
} 