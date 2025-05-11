package com.plywoodpocket.crm.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plywoodpocket.crm.models.*
import com.plywoodpocket.crm.viewmodel.LeadsViewModel
import com.plywoodpocket.crm.viewmodel.LeadsUiState
import com.plywoodpocket.crm.ui.theme.Orange
import com.plywoodpocket.crm.ui.theme.White
import com.plywoodpocket.crm.utils.LocationHelper
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.plywoodpocket.crm.viewmodel.LeadsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LeadsViewModel = viewModel(factory = LeadsViewModelFactory(context.applicationContext as android.app.Application))
    val uiState by viewModel.uiState.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingLead by remember { mutableStateOf<Lead?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadLeads()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leads Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingLead = null
                showForm = true
            }, containerColor = Orange) {
                Icon(Icons.Default.Add, contentDescription = "Add Lead", tint = White)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(White)) {
            when (uiState) {
                is LeadsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LeadsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((uiState as LeadsUiState.Error).message, color = Color.Red)
                    }
                }
                is LeadsUiState.Success -> {
                    val statuses = (uiState as LeadsUiState.Success).statuses
                    val leads = (uiState as LeadsUiState.Success).leads
                    StatusRow(
                        statuses = statuses,
                        selectedStatusId = viewModel.selectedStatusId,
                        onStatusSelected = { viewModel.selectStatus(it) },
                        onAddClick = {
                            editingLead = null
                            showForm = true
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    if (leads.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No leads found", color = Color.Gray)
                        }
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            leads.forEach { lead ->
                                LeadCard(
                                    lead = lead,
                                    onEdit = {
                                        editingLead = lead
                                        showForm = true
                                    },
                                    onCall = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                                        context.startActivity(intent)
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        if (showForm) {
            LeadFormDialog(
                lead = editingLead,
                statuses = (uiState as? LeadsUiState.Success)?.statuses ?: emptyList(),
                onDismiss = { showForm = false },
                onSubmit = { req, id ->
                    if (id == null) {
                        viewModel.createLead(req) { success, msg ->
                            showForm = false
                            if (!success) scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    } else {
                        viewModel.updateLead(id, req) { success, msg ->
                            showForm = false
                            if (!success) scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun StatusRow(
    statuses: List<LeadStatus>,
    selectedStatusId: Int?,
    onStatusSelected: (Int?) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        statuses.forEach { status ->
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(android.graphics.Color.parseColor(status.color)))
                    .clickable { onStatusSelected(status.id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(status.name, color = White, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = "Add Lead", tint = Orange)
        }
    }
}

@Composable
fun LeadCard(
    lead: Lead,
    onEdit: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCall) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF43A047))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Orange)
                }
            }
            Text("Phone: ${lead.phone}", fontSize = 14.sp)
            Text("Email: ${lead.email}", fontSize = 14.sp)
            Text("Status: ${lead.status?.name ?: "-"}", fontSize = 14.sp)
            Text("Follow-up: ${lead.follow_up_date ?: "-"}", fontSize = 14.sp)
            if (!lead.address.isNullOrBlank()) Text("Address: ${lead.address}", fontSize = 14.sp)
            if (!lead.notes.isNullOrBlank()) Text("Notes: ${lead.notes}", fontSize = 14.sp)
        }
    }
} 