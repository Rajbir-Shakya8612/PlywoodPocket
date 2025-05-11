package com.plywoodpocket.crm.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@SuppressLint("MissingPermission")
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, onBack: () -> Unit = {}) {
    val status = viewModel.attendanceStatus
    val context = LocalContext.current
    val currentDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    LaunchedEffect(Unit) {
        viewModel.fetchAttendanceStatus()
    }

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Attendance",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Date
        Text(
            text = currentDate,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Error Message
        viewModel.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = error,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (status) {
                    "loading" -> {
                        CircularProgressIndicator()
                    }
                    "none" -> {
                        Text(
                            text = "Not Checked In",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { viewModel.performCheckIn(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            enabled = !viewModel.loading
                        ) {
                            if (viewModel.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Check In", color = Color.White)
                            }
                        }
                    }
                    "checked_in" -> {
                        Text(
                            text = "Checked In",
                            fontSize = 18.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        viewModel.checkInTime?.let {
                            Text(
                                text = "Time: $it",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        Button(
                            onClick = { viewModel.performCheckOut(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                            enabled = !viewModel.loading
                        ) {
                            if (viewModel.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Check Out", color = Color.White)
                            }
                        }
                    }
                    "checked_out" -> {
                        Text(
                            text = "Attendance Complete ✅",
                            fontSize = 18.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        viewModel.checkInTime?.let {
                            Text(
                                text = "Check In: ${formatIsoToDate(it)} ${formatIsoToTime(it)}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        viewModel.checkOutTime?.let {
                            Text(
                                text = "Check Out: ${formatIsoToDate(it)} ${formatIsoToTime(it)}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        viewModel.workingHours?.let {
                            Text(
                                text = "Working Hours: $it",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Calendar Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Calendar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // TODO: Implement calendar view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Calendar View Coming Soon", color = Color.Gray)
                }
            }
        }
    }
}

fun formatToKolkata12Hour(time: String?): String? {
    if (time.isNullOrBlank()) return null
    // Try to parse as HH:mm:ss or HH:mm
    val formats = listOf("HH:mm:ss", "HH:mm")
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(time)
            val outSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            outSdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return date?.let { outSdf.format(it) }
        } catch (_: Exception) {}
    }
    return time // fallback
}

fun formatIsoToDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso)
        val outSdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        outSdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        date?.let { outSdf.format(it) }
    } catch (e: Exception) {
        null
    }
}

fun formatIsoToTime(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso)
        val outSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        outSdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        date?.let { outSdf.format(it) }
    } catch (e: Exception) {
        null
    }
}
