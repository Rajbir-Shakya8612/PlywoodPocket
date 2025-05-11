package com.plywoodpocket.crm.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFA726))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TitleText("Attendance", 24)
        }
        TitleText(currentDate, 16, Color.Gray)

        viewModel.errorMessage?.let {
            ErrorCard(it)
        }

        StatusCard(status, viewModel, context)

        CalendarCard()
    }
}

@Composable
fun TitleText(text: String, size: Int, color: Color = Color.Black) {
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
                "loading" -> CircularProgressIndicator()

                "none" -> {
                    TitleText("Not Checked In", 18, Color.Gray)
                    LoadingButton(
                        text = "Check In",
                        loading = viewModel.loading,
                        onClick = { viewModel.performCheckIn(context) },
                        backgroundColor = Color(0xFF4CAF50)
                    )
                }

                "checked_in" -> {
                    TitleText("Checked In", 18, Color(0xFF4CAF50))
                    InfoText("Time: ${formatIsoToTime(viewModel.checkInTime)}")
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
                        }"
                    )
                    InfoText(
                        "Check Out: ${formatIsoToDate(viewModel.checkOutTime)} ${
                            formatIsoToTime(
                                viewModel.checkOutTime
                            )
                        }"
                    )
                    viewModel.workingHours?.let { InfoText("Working Hours: $it") }
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
fun InfoText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color.Gray
    )
}

@Composable
fun CalendarCard() {
    // Example attendance data: Mark some days with status
    val attendanceData = mapOf(
        LocalDate.now().minusDays(2) to "present",
        LocalDate.now().minusDays(1) to "late",
        LocalDate.now() to "present",
        LocalDate.now().plusDays(1) to "absent"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TitleText("Attendance Calendar", 18)
            Spacer(modifier = Modifier.height(8.dp))

            // Modern calendar view
            ModernCalendar(
                modifier = Modifier
                    .fillMaxWidth(),
                attendanceData = attendanceData,
                onDayClick = { /* Do nothing or show a toast/snackbar if needed */ }
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

