package com.plywoodpocket.crm.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun PriorityChip(priority: String?) {
    val color = when (priority) {
        "high" -> Color.Red
        "medium" -> Color(0xFFFFA726)
        "low" -> Color(0xFF66BB6A)
        else -> Color.Gray
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(priority?.replaceFirstChar { it.uppercase() } ?: "-", color = color, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
fun DropdownMenuBox(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun DatePickerDialog(onDateSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    LaunchedEffect(Unit) {
        val datePicker = android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val pickedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                pickedCal.set(Calendar.YEAR, y)
                pickedCal.set(Calendar.MONTH, m)
                pickedCal.set(Calendar.DAY_OF_MONTH, d)
                pickedCal.set(Calendar.HOUR_OF_DAY, 0)
                pickedCal.set(Calendar.MINUTE, 0)
                pickedCal.set(Calendar.SECOND, 0)
                pickedCal.set(Calendar.MILLISECOND, 0)
                val formatted = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                formatted.timeZone = TimeZone.getTimeZone("UTC")
                onDateSelected(formatted.format(pickedCal.time))
            },
            year, month, day
        )
        datePicker.setOnCancelListener { onDismiss() }
        datePicker.show()
    }
} 