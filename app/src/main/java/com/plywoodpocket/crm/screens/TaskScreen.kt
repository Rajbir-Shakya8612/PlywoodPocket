package com.plywoodpocket.crm.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plywoodpocket.crm.models.Task
import com.plywoodpocket.crm.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import com.plywoodpocket.crm.screens.PriorityChip
import com.plywoodpocket.crm.screens.DropdownMenuBox
import com.plywoodpocket.crm.screens.DatePickerDialog
import com.plywoodpocket.crm.utils.DateUtils

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
    var showEditDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    // Fetch tasks on first load
    LaunchedEffect(Unit) { viewModel.fetchTasks() }

    val groupedTasks = remember(tasks) {
        tasks.groupBy { it.status }
    }
    val statusOrder = listOf("pending", "in_progress", "completed")
    val statusLabels = mapOf(
        "pending" to "Pending",
        "in_progress" to "In Progress",
        "completed" to "Completed"
    )
    val statusColors = mapOf(
        "pending" to Color(0xFFFFA726),
        "in_progress" to Color(0xFF42A5F5),
        "completed" to Color(0xFF66BB6A)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 12.dp, end = 12.dp)
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
                modifier = Modifier.size(44.dp).padding(end = 4.dp)
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
                LazyColumn(modifier = Modifier.weight(1f)) {
                    statusOrder.forEach { status ->
                        val list = groupedTasks[status] ?: emptyList()
                        if (list.isNotEmpty()) {
                            item {
                                StatusHeader(label = statusLabels[status] ?: status.capitalize(), color = statusColors[status] ?: Color.Gray)
                            }
                            items(list) { task ->
                                TaskCard(
                                    task = task,
                                    onEdit = {
                                        selectedTask = task
                                        showEditDialog = true
                                    },
                                    onDetails = {
                                        selectedTask = task
                                        showDetailsDialog = true
                                    },
                                    statusColor = statusColors[status] ?: Color.Gray,
                                    viewModel = viewModel
                                )
                            }
                        }
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
    if (showEditDialog && selectedTask != null) {
        EditTaskDialog(
            task = selectedTask!!,
            onDismiss = { showEditDialog = false },
            onSubmit = { taskMap ->
                viewModel.updateTask(
                    selectedTask!!.id,
                    taskMap,
                    onSuccess = { showEditDialog = false },
                    onError = { showEditDialog = false }
                )
            }
        )
    }
    if (showDetailsDialog && selectedTask != null) {
        TaskDetailsDialog(
            task = selectedTask!!,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
fun StatusHeader(label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp, 24.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    onEdit: () -> Unit,
    onDetails: () -> Unit,
    statusColor: Color,
    viewModel: TaskViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("pending", "in_progress", "completed")
    val statusLabels = mapOf(
        "pending" to "Pending",
        "in_progress" to "In Progress",
        "completed" to "Completed"
    )
    val dueDateFormatted = DateUtils.formatIsoToDate(task.due_date) ?: task.due_date
    val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    val isDueToday = dueDateFormatted == today
    val isOverdue = try {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val due = sdf.parse(dueDateFormatted)
        due != null && due.before(sdf.parse(today))
    } catch (e: Exception) { false }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    PriorityChip(priority = task.priority)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Due: $dueDateFormatted", style = MaterialTheme.typography.bodySmall)
                    if (isDueToday || isOverdue) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Due notification", tint = if (isOverdue) Color.Red else Color(0xFFFFA726), modifier = Modifier.size(16.dp).padding(start = 4.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, statusColor),
                            modifier = Modifier
                                .clickable { statusMenuExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(statusLabels[task.status] ?: task.status.capitalize(), color = statusColor, fontSize = 12.sp)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            statusOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(statusLabels[option] ?: option.capitalize()) },
                                    onClick = {
                                        statusMenuExpanded = false
                                        if (option != task.status) {
                                            coroutineScope.launch {
                                                viewModel.updateTaskStatus(
                                                    taskId = task.id,
                                                    status = option,
                                                    onSuccess = {},
                                                    onError = {}
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (task.assignee?.name != null) {
                        Text("Assigned by: ${task.assignee.name}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            IconButton(onClick = onDetails) {
                Icon(Icons.Filled.Info, contentDescription = "Details", tint = Color(0xFF1976D2))
            }
//            IconButton(onClick = onEdit) {
//                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color(0xFFFFA726))
//            }
        }
    }
}

@Composable
fun TaskDetailsDialog(task: Task, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Task Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Title: ${task.title}", fontWeight = FontWeight.Bold)
                Text("Description: ${task.description ?: "-"}")
                Text("Type: ${task.type?.capitalize() ?: "-"}")
                Text("Status: ${task.status.capitalize()}")
                Text("Priority: ${task.priority?.capitalize() ?: "-"}")
                val dueDateFormatted = DateUtils.formatIsoToDate(task.due_date) ?: task.due_date
                Text("Due Date: $dueDateFormatted")
                if (task.assignee?.name != null) {
                    Text("Assigned by: ${task.assignee.name}")
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
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
                        onSelected = { selectedType -> type = selectedType }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status:", modifier = Modifier.width(60.dp))
                    DropdownMenuBox(
                        options = listOf("pending", "in_progress", "completed"),
                        selected = status,
                        onSelected = { selectedStatus -> status = selectedStatus }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority:", modifier = Modifier.width(60.dp))
                    DropdownMenuBox(
                        options = listOf("low", "medium", "high"),
                        selected = priority,
                        onSelected = { selectedPriority -> priority = selectedPriority }
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
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onSubmit: (Map<String, Any?>) -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description ?: "") }
    var type by remember { mutableStateOf(task.type ?: "lead") }
    var status by remember { mutableStateOf(task.status) }
    var priority by remember { mutableStateOf(task.priority ?: "medium") }
    var dueDate by remember { mutableStateOf(task.due_date.take(10)) }
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
                Text("Update Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit Task") },
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
                        onSelected = { selectedType -> type = selectedType }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status:", modifier = Modifier.width(60.dp))
                    DropdownMenuBox(
                        options = listOf("pending", "in_progress", "completed"),
                        selected = status,
                        onSelected = { selectedStatus -> status = selectedStatus }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority:", modifier = Modifier.width(60.dp))
                    DropdownMenuBox(
                        options = listOf("low", "medium", "high"),
                        selected = priority,
                        onSelected = { selectedPriority -> priority = selectedPriority }
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