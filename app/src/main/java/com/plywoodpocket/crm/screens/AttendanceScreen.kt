package com.plywoodpocket.crm.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plywoodpocket.crm.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.plywoodpocket.crm.utils.LocationServiceHelper
import com.plywoodpocket.crm.ui.ModernCalendar
import java.time.LocalDate
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke

@SuppressLint("MissingPermission")
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, onBack: () -> Unit = {}) {
    val status = viewModel.attendanceStatus
    val context = LocalContext.current
    val currentDate =
        remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    LaunchedEffect(Unit) { viewModel.fetchAttendanceStatus() }

    if (viewModel.showLocationDialog) {
        LocationServiceHelper.LocationServiceDialog(
            onDismiss = { viewModel.showLocationDialog = false },
            onSettingsClick = {
                LocationServiceHelper.openLocationSettings(context)
                viewModel.showLocationDialog = false
            }
        )
    }
    if (viewModel.showNetworkDialog) {
        LocationServiceHelper.NetworkErrorDialog(
            onDismiss = { viewModel.showNetworkDialog = false },
            onSettingsClick = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                viewModel.showNetworkDialog = false
            }
        )
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 12.dp, end = 12.dp, top = 32.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onBack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TitleText("Attendance Dashboard", 24, Color.Black)
        }
        Card(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 4.dp)
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(50),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA726))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Date",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentDate,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(12.dp),
            border = BorderStroke(1.dp, Color(0x1A000000)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA726))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                viewModel.errorMessage?.let {
                    ErrorCard(it)
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    StatusCard(status, viewModel, context)
                }
            }
        }
        CalendarCard(viewModel)
    }
}

@Composable
fun TitleText(text: String, size: Int, color: Color = Color(0xFFFFA726)) {
    Text(
        text = text,
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Text(
            text = message,
            color = Color(0xFFD32F2F),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun StatusCard(status: String, viewModel: AttendanceViewModel, context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (status) {
                "loading" -> CircularProgressIndicator(color = Color(0xFFFFA726))

                "none" -> {
                    TitleText("Not Checked In", 18, Color.Gray)
                    LoadingButton(
                        text = "Check In",
                        loading = viewModel.loading,
                        onClick = { viewModel.performCheckIn(context) },
                        backgroundColor = Color(0xFFFFA726)
                    )
                }

                "checked_in" -> {
                    TitleText("Checked In", 18, Color(0xFF4CAF50))
                    InfoText("Time: ${formatIsoToTime(viewModel.checkInTime)}", color = Color.Gray)
                    LoadingButton(
                        text = "Check Out",
                        loading = viewModel.loading,
                        onClick = { viewModel.performCheckOut(context) },
                        backgroundColor = Color(0xFFFFC107)
                    )
                }

                "checked_out" -> {
                    TitleText("Attendance Complete ✅", 18, Color(0xFF4CAF50))
                    InfoText(
                        "Check In: ${formatIsoToDate(viewModel.checkInTime)} ${
                            formatIsoToTime(
                                viewModel.checkInTime
                            )
                        }",
                        color = Color.Gray
                    )
                    InfoText(
                        "Check Out: ${formatIsoToDate(viewModel.checkOutTime)} ${
                            formatIsoToTime(
                                viewModel.checkOutTime
                            )
                        }",
                        color = Color.Gray
                    )
                    viewModel.workingHours?.let { InfoText("Working Hours: $it", color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
fun LoadingButton(text: String, loading: Boolean, onClick: () -> Unit, backgroundColor: Color) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        enabled = !loading
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White
            )
        } else {
            Text(text, color = Color.White)
        }
    }
}

@Composable
fun InfoText(text: String, color: Color = Color.Gray) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = color
    )
}

@Composable
fun CalendarCard(viewModel: AttendanceViewModel) {
    val today = LocalDate.now()
    val currentMonth = today.monthValue
    val currentYear = today.year
    val rawAttendanceData = mapOf(
        today.minusDays(2) to "present",
        today.minusDays(1) to "late",
        today to "present",
    )
    val attendanceData = rawAttendanceData.filterKeys { it.isBefore(today.plusDays(1)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA726))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TitleText("Attendance Calendar", 18, Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            ModernCalendar(
                modifier = Modifier.fillMaxWidth(),
                attendanceData = attendanceData,
                onDayClick = { /* Optionally show details */ },
                summaryUpToDate = today
            )
        }
    }
}

fun formatIsoToDate(iso: String?): String? {
    return iso?.let {
        try {
            val parser =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            val date = parser.parse(it)
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(date ?: return null)
        } catch (e: Exception) {
            null
        }
    }
}

fun formatIsoToTime(iso: String?): String? {
    return iso?.let {
        try {
            val parser =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            val date = parser.parse(it)
            SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(date ?: return null)
        } catch (e: Exception) {
            null
        }
    }
}

