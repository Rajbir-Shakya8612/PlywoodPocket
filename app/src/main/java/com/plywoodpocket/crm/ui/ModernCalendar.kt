package com.plywoodpocket.crm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.atStartOfMonth
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ModernCalendar(
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
    attendanceData: Map<LocalDate, String> = emptyMap() // Map of date to attendance status
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = currentMonth.plusMonths(12),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = java.time.DayOfWeek.MONDAY
    )

    Column(modifier = modifier) {
        // Month and year header
        Text(
            text = calendarState.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                    " " + calendarState.firstVisibleMonth.yearMonth.year,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        // Weekdays row
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
        // Calendar
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
                                // Today: blue circle with check
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF1976D2), CircleShape),
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
                                // Day with attendance
                                val backgroundColor = when (attendanceStatus.lowercase()) {
                                    "present" -> Color(0xFF28a745) // Green
                                    "late" -> Color(0xFFffc107) // Yellow
                                    "absent" -> Color(0xFFdc3545) // Red
                                    else -> Color(0xFFE0E0E0) // Gray
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
                                // Normal day
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
            monthHeader = {}, // Optional: add month header if needed
            modifier = Modifier.fillMaxWidth()
        )
    }
} 