package com.plywoodpocket.crm.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.plywoodpocket.crm.models.Task
import com.plywoodpocket.crm.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    onCreateTask: () -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val tasks by viewModel.tasks.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Fetch tasks on first load
    LaunchedEffect(Unit) { viewModel.fetchTasks() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 12.dp, end = 12.dp) // Top padding as per green box
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBack?.invoke() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Tasks", style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(44.dp)
                    .padding(end = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        } else {
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks found", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(tasks) { task ->
                        TaskItem(task, onClick = { onTaskClick(task) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { taskMap ->
                viewModel.createTask(taskMap, onSuccess = { showAddDialog = false }, onError = {})
            }
        )
    }
}

@Composable
fun TaskItem(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium)
            Text("Due: ${task.due_date}", style = MaterialTheme.typography.bodySmall)
            Text("Status: ${task.status}", style = MaterialTheme.typography.bodySmall)
            if (task.assignee?.name != null) {
                Text("Assigned by: ${task.assignee.name}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onSubmit: (Map<String, Any?>) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("lead") }
    var status by remember { mutableStateOf("pending") }
    var priority by remember { mutableStateOf("medium") }
    var dueDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && dueDate.isNotBlank()) {
                        onSubmit(
                            mapOf(
                                "title" to title,
                                "description" to description,
                                "type" to type,
                                "status" to status,
                                "priority" to priority,
                                "due_date" to dueDate
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && dueDate.isNotBlank()
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Add Task") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type:", modifier = Modifier.width(60.dp))
                    DropdownMenuBox(
                        options = listOf("lead", "sale", "meeting"),
                        selected = type,
                        onSelected = { type = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status:", modifier = Modifier.width(60.dp))
                    DropdownMenuBox(
                        options = listOf("pending", "in_progress", "completed"),
                        selected = status,
                        onSelected = { status = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority:", modifier = Modifier.width(60.dp))
                    DropdownMenuBox(
                        options = listOf("low", "medium", "high"),
                        selected = priority,
                        onSelected = { priority = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {},
                    label = { Text("Due Date*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                        }
                    }
                )
                if (showDatePicker) {
                    DatePickerDialog(
                        onDateSelected = {
                            dueDate = it
                            showDatePicker = false
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
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
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val datePicker = android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val formatted = String.format("%04d-%02d-%02d", y, m + 1, d)
                onDateSelected(formatted)
            },
            year, month, day
        )
        datePicker.setOnCancelListener { onDismiss() }
        datePicker.show()
    }
} 