package com.plywoodpocket.crm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun ModernCalendar(
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
    attendanceData: Map<LocalDate, String> = emptyMap()
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = currentMonth.plusMonths(12),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = java.time.DayOfWeek.MONDAY
    )

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.padding(8.dp)) {
        // Header: Month-Year + Arrows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                val previousMonth = calendarState.firstVisibleMonth.yearMonth.minusMonths(1)
                coroutineScope.launch {
                    calendarState.animateScrollToMonth(previousMonth)
                }
            }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous")
            }

            Text(
                text = calendarState.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                        " " + calendarState.firstVisibleMonth.yearMonth.year,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )

            IconButton(onClick = {
                val nextMonth = calendarState.firstVisibleMonth.yearMonth.plusMonths(1)
                coroutineScope.launch {
                    calendarState.animateScrollToMonth(nextMonth)
                }
            }) {
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next")
            }
        }

        // Weekday labels
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            java.time.DayOfWeek.values().forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar view
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day: CalendarDay ->
                val date = day.date
                val attendanceStatus = attendanceData[date]

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clickable(enabled = day.position == DayPosition.MonthDate) {
                            onDayClick(date)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (day.position == DayPosition.MonthDate) {
                        when {
                            date == today -> {
                                // Today - Blue circle with tick
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF2196F3), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Today",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            attendanceStatus != null -> {
                                val backgroundColor = when (attendanceStatus.lowercase()) {
                                    "present" -> Color(0xFF4CAF50)
                                    "late" -> Color(0xFFFFC107)
                                    "absent" -> Color(0xFFF44336)
                                    else -> Color(0xFFE0E0E0)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(backgroundColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            else -> {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            },
            monthHeader = {}, // Skip extra header
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Legend at bottom
        AttendanceLegend()
        AttendanceSummary(attendanceData)
    }
}

@Composable
fun AttendanceLegend() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("Present", Color(0xFF4CAF50))
            LegendItem("Late", Color(0xFFFFC107))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("Absent", Color(0xFFF44336))
            LegendItem("Today", Color(0xFF2196F3))
        }
    }
}


@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Black
        )
    }
}

@Composable
fun AttendanceSummary(attendanceData: Map<LocalDate, String>) {
    val presentCount = attendanceData.count { it.value.equals("present", ignoreCase = true) }
    val absentCount = attendanceData.count { it.value.equals("absent", ignoreCase = true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(text = "Total Present: $presentCount", fontSize = 14.sp, color = Color.Black)
        Text(text = "Total Absent: $absentCount", fontSize = 14.sp, color = Color.Black)
    }
}


