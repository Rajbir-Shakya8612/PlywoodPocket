package com.plywoodpocket.crm.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plywoodpocket.crm.models.Task
import com.plywoodpocket.crm.models.UserProfile
import com.plywoodpocket.crm.viewmodel.AdminTaskViewModel
import com.plywoodpocket.crm.viewmodel.AdminTaskViewModelFactory
import com.plywoodpocket.crm.viewmodel.UserManagementViewModel
import com.plywoodpocket.crm.viewmodel.UserManagementViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import com.plywoodpocket.crm.screens.PriorityChip
import com.plywoodpocket.crm.screens.DropdownMenuBox
import com.plywoodpocket.crm.screens.DatePickerDialog
import android.widget.Toast
import com.plywoodpocket.crm.utils.DateUtils
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect
import androidx.compose.material.icons.filled.Search

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "Search by title, due date, assignee, status..."
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        label = { Text(hint) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun AdminTaskScreen(
    onBack: (() -> Unit)? = null
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(Color.Black)
    }
    val context = LocalContext.current
    val adminTaskViewModel: AdminTaskViewModel = viewModel(
        factory = AdminTaskViewModelFactory(
            com.plywoodpocket.crm.api.ApiClient(com.plywoodpocket.crm.utils.TokenManager(context)).apiService
        )
    )
    val userManagementViewModel: UserManagementViewModel = viewModel(
        factory = UserManagementViewModelFactory(context)
    )
    val tasks by adminTaskViewModel.tasks.collectAsState()
    val loading by adminTaskViewModel.loading.collectAsState()
    val error by adminTaskViewModel.error.collectAsState()
    val users by userManagementViewModel.users.collectAsState()
    val taskUpdateSuccess by adminTaskViewModel.taskUpdateSuccess.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        adminTaskViewModel.fetchTasks()
        userManagementViewModel.loadUsers()
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp, start = 12.dp, end = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBack?.invoke() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Admin Tasks",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
            IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(44.dp).padding(end = 4.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = MaterialTheme.colorScheme.primary)
            }
        }
        SearchBar(
            value = searchText,
            onValueChange = { searchText = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        } else {
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks found", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                val filteredTasks = tasks.filter { task ->
                    val search = searchText.trim().lowercase()
                    if (search.isBlank()) return@filter true
                    val inTitle = task.title.lowercase().contains(search)
                    val inDueDate = task.due_date?.lowercase()?.contains(search) == true
                    val inAssignee = task.assignee?.name?.lowercase()?.contains(search) == true
                    val inStatus = task.status.lowercase().contains(search)
                    inTitle || inDueDate || inAssignee || inStatus
                }
                AdminTaskList(
                    tasks = filteredTasks,
                    onEdit = { task -> selectedTask = task; showEditDialog = true },
                    onDetails = { task -> selectedTask = task; showDetailsDialog = true },
                    onDelete = { task -> selectedTask = task; showDeleteDialog = true },
                    onStatusChange = { taskId, status -> adminTaskViewModel.updateTaskStatus(taskId, status, {}, {}) }
                )
            }
        }
    }
    if (showAddDialog) {
        AdminAddEditTaskDialog(
            users = users,
            onDismiss = { showAddDialog = false },
            onSubmit = { taskMap, errorHandler, _ ->
                adminTaskViewModel.createTask(
                    taskMap,
                    onSuccess = {
                        showAddDialog = false
                        adminTaskViewModel.fetchTasks()
                        Toast.makeText(context, "Task created successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onError = errorHandler
                )
            }
        )
    }
    if (showEditDialog && selectedTask != null) {
        AdminAddEditTaskDialog(
            users = users,
            task = selectedTask,
            onDismiss = {
                showEditDialog = false
                selectedTask = null
            },
            onSubmit = { taskMap, errorHandler, _ ->
                adminTaskViewModel.updateTask(
                    selectedTask!!.id,
                    taskMap,
                    onSuccess = {
                        showEditDialog = false
                        selectedTask = null
                        adminTaskViewModel.fetchTasks()
                        Toast.makeText(context, "Task updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onError = errorHandler
                )
            }
        )
    }
    if (showDetailsDialog && selectedTask != null) {
        TaskDetailsDialog(task = selectedTask!!, onDismiss = { showDetailsDialog = false })
    }
    if (showDeleteDialog && selectedTask != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                Button(onClick = {
                    adminTaskViewModel.deleteTask(selectedTask!!.id, onSuccess = { showDeleteDialog = false }, onError = { showDeleteDialog = false })
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") }
        )
    }
}

@Composable
fun AdminTaskList(
    tasks: List<Task>,
    onEdit: (Task) -> Unit,
    onDetails: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onStatusChange: (Int, String) -> Unit
) {
    val groupedTasks = remember(tasks) { tasks.groupBy { it.status } }
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
    ) {
        statusOrder.forEach { status ->
            val list = groupedTasks[status] ?: emptyList()
            if (list.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(10.dp, 24.dp).background(statusColors[status] ?: Color.Gray, RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = statusLabels[status] ?: status.replaceFirstChar { it.uppercase() },
                            color = statusColors[status] ?: Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
                items(list) { task ->
                    AdminTaskCard(
                        task = task,
                        onEdit = { onEdit(task) },
                        onDetails = { onDetails(task) },
                        onDelete = { onDelete(task) },
                        statusColor = statusColors[task.status] ?: Color.Gray,
                        onStatusChange = onStatusChange
                    )
                }
            }
        }
    }
}

@Composable
fun AdminTaskCard(
    task: Task,
    onEdit: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    statusColor: Color,
    onStatusChange: (Int, String) -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("pending", "in_progress", "completed")
    val statusLabels = mapOf(
        "pending" to "Pending",
        "in_progress" to "In Progress",
        "completed" to "Completed"
    )
    val dueDateFormatted = DateUtils.formatIsoToDate(task.due_date) ?: task.due_date
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    PriorityChip(priority = task.priority)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Due: $dueDateFormatted", fontSize = 13.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor),
                            modifier = Modifier.clickable { statusMenuExpanded = true }
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
                                            onStatusChange(task.id, option)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (task.assignee?.name != null) {
                        Text("Assigned: ${task.assignee.name}", fontSize = 13.sp)
                    }
                }
            }
            IconButton(onClick = onDetails) {
                Icon(Icons.Filled.Info, contentDescription = "Details", tint = Color(0xFF1976D2))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color(0xFFFFA726))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddEditTaskDialog(
    users: List<UserProfile>,
    task: Task? = null,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Any?>, (String) -> Unit, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var type by remember { mutableStateOf(task?.type ?: "lead") }
    var status by remember { mutableStateOf(task?.status ?: "pending") }
    var priority by remember { mutableStateOf(task?.priority ?: "medium") }
    var dueDate by remember { mutableStateOf(task?.due_date?.take(10) ?: "") }
    var assigneeId by remember { mutableStateOf(task?.assignee_id ?: users.firstOrNull()?.id ?: 0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (errorMessage != null) {
        LaunchedEffect(errorMessage) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            errorMessage = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && dueDate.isNotBlank() && assigneeId != 0) {
                        onSubmit(
                            mapOf(
                                "title" to title,
                                "description" to description,
                                "type" to type,
                                "status" to status,
                                "priority" to priority,
                                "due_date" to dueDate,
                                "assignee_id" to assigneeId
                            ),
                            { error -> errorMessage = error },
                            onDismiss
                        )
                    }
                },
                enabled = title.isNotBlank() && dueDate.isNotBlank() && assigneeId != 0
            ) {
                Text(if (task == null) "Add Task" else "Update Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(if (task == null) "Add Task" else "Edit Task") },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Assignee:", modifier = Modifier.width(70.dp))
                    DropdownMenuBox(
                        options = users.map { it.name },
                        selected = users.find { it.id == assigneeId }?.name ?: users.firstOrNull()?.name.orEmpty(),
                        onSelected = { name ->
                            assigneeId = users.find { it.name == name }?.id ?: 0
                        }
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