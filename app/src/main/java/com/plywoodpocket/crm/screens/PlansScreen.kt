@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.plywoodpocket.crm.screens

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.Plan
import com.plywoodpocket.crm.models.PlanRequest
import com.plywoodpocket.crm.utils.TokenManager
import com.plywoodpocket.crm.viewmodel.PlanViewModel
import com.plywoodpocket.crm.viewmodel.PlanViewModelFactory
import com.plywoodpocket.crm.utils.DateUtils.formatIsoToDate
import java.util.*
import android.widget.Toast

@Composable
fun PlansScreen(
    viewModel: PlanViewModel = viewModel(
        factory = PlanViewModelFactory(
            apiService = ApiClient(TokenManager(LocalContext.current)).apiService,
            tokenManager = TokenManager(LocalContext.current)
        )
    ),
    onBack: (() -> Unit)? = null,
    onCreatePlan: (() -> Unit)? = null
) {
    val plans by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedType by remember { mutableStateOf("monthly") }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Details dialog state
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf<Plan?>(null) }

    val context = LocalContext.current
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(selectedType) {
        viewModel.loadPlans(selectedType)
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Plan Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { onBack?.invoke() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Plan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Type Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlanTypeChip(
                    text = "Monthly",
                    selected = selectedType == "monthly",
                    onClick = { selectedType = "monthly" }
                )
                PlanTypeChip(
                    text = "Quarterly",
                    selected = selectedType == "quarterly",
                    onClick = { selectedType = "quarterly" }
                )
                PlanTypeChip(
                    text = "Yearly",
                    selected = selectedType == "yearly",
                    onClick = { selectedType = "yearly" }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                plans.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Data Found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    // Filter plans by selectedType if needed
                    val filteredPlans = plans.filter { it.type.equals(selectedType, ignoreCase = true) }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredPlans) { plan ->
                            PlanCard(plan = plan, onDetailsClick = {
                                selectedPlan = plan
                                showDetailsDialog = true
                            })
                        }
                    }
                }
            }
        }
        if (showCreateDialog) {
            CreatePlanDialog(
                onDismiss = { showCreateDialog = false },
                onSubmit = { planRequest ->
                    viewModel.createPlan(planRequest, selectedType)
                    showCreateDialog = false
                }
            )
        }
        if (showDetailsDialog && selectedPlan != null) {
            PlanDetailsDialog(plan = selectedPlan!!, onDismiss = { showDetailsDialog = false })
        }
    }
}

@Composable
fun PlanCard(plan: Plan, onDetailsClick: () -> Unit) {
    val startDate = formatIsoToDate(plan.startDate)
    val endDate = formatIsoToDate(plan.endDate)
    val salesTarget = plan.salesTarget.toDoubleOrNull() ?: 0.0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${plan.type.capitalize()} Plan (${startDate ?: "-"} - ${endDate ?: "-"})",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Leads: 0/${plan.leadTarget}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Sales: ₹0/₹$salesTarget",
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = plan.progressPercentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            Text(
                text = "Status: ${plan.status.capitalize()}",
                style = MaterialTheme.typography.bodyMedium
            )
            plan.notes?.let {
                Text(
                    text = "Notes: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDetailsClick) {
                Text("Details")
            }
        }
    }
}

@Composable
fun PlanTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CreatePlanDialog(
    onDismiss: () -> Unit,
    onSubmit: (PlanRequest) -> Unit
) {
    var type by remember { mutableStateOf("monthly") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var leadTarget by remember { mutableStateOf("") }
    var salesTarget by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // DatePicker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    if (showStartDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    state.selectedDateMillis?.let {
                        startDate = dateFormatter.format(Date(it))
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
    if (showEndDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    state.selectedDateMillis?.let {
                        endDate = dateFormatter.format(Date(it))
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Plan") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PlanTypeChip("Monthly", type == "monthly") { type = "monthly" }
                    PlanTypeChip("Quarterly", type == "quarterly") { type = "quarterly" }
                    PlanTypeChip("Yearly", type == "yearly") { type = "yearly" }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (yyyy-MM-dd)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Start Date")
                        }
                    }
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (yyyy-MM-dd)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick End Date")
                        }
                    }
                )
                OutlinedTextField(
                    value = leadTarget,
                    onValueChange = { leadTarget = it.filter { c -> c.isDigit() } },
                    label = { Text("Lead Target") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = salesTarget,
                    onValueChange = { salesTarget = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Sales Target") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = false
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Basic validation
                if (startDate.isBlank() || endDate.isBlank() || leadTarget.isBlank() || salesTarget.isBlank() || description.isBlank()) {
                    error = "All fields except notes are required."
                    return@Button
                }
                onSubmit(
                    PlanRequest(
                        type = type,
                        startDate = startDate,
                        endDate = endDate,
                        leadTarget = leadTarget.toIntOrNull() ?: 0,
                        salesTarget = salesTarget.toDoubleOrNull() ?: 0.0,
                        description = description,
                        notes = if (notes.isBlank()) null else notes
                    )
                )
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PlanDetailsDialog(plan: Plan, onDismiss: () -> Unit) {
    val startDate = formatIsoToDate(plan.startDate)
    val endDate = formatIsoToDate(plan.endDate)
    val salesTarget = plan.salesTarget.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "${plan.type.capitalize()} Plan",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${startDate ?: "-"} to ${endDate ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                )
            }
        },
        text = {
            Column {
                Text("Start Date: ${startDate ?: "-"}")
                Text("End Date: ${endDate ?: "-"}")
                Text("Achieved Leads: 0")
                Text("Achieved Sales: ₹0")
                Text("Lead Target: ${plan.leadTarget}")
                Text("Sales Target: ₹$salesTarget")
                Text("Status: ${plan.status.capitalize()}")
                LinearProgressIndicator(
                    progress = plan.progressPercentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Text("Description: ${plan.description}")
                Text("Notes: ${plan.notes ?: "-"}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
} 