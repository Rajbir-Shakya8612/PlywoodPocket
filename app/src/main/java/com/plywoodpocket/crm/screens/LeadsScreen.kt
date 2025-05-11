package com.plywoodpocket.crm.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Chat
import androidx.compose.ui.text.input.TextFieldValue
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LeadsViewModel =
        viewModel(factory = LeadsViewModelFactory(context.applicationContext as android.app.Application))
    val uiState by viewModel.uiState.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingLead by remember { mutableStateOf<Lead?>(null) }
    var defaultStatusForNewLead by remember { mutableStateOf<LeadStatus?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedStatusId by remember { mutableStateOf<Int?>(null) }
    var showDetailsForLead by remember { mutableStateOf<Lead?>(null) }

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
                defaultStatusForNewLead = null
                showForm = true
            }, containerColor = Orange) {
                Icon(Icons.Default.Add, contentDescription = "Add Lead", tint = White)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(White)
        ) {
            // Search bar sabse upar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search leads by name, phone, email...", color = Color.Black) },
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF2196F3)
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
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
                    val leads = (uiState as LeadsUiState.Success).leads
                    val statuses = (uiState as LeadsUiState.Success).statuses ?: emptyList()

                    // Show each status as a colored card, with its leads inside as white cards
                    LazyRow(Modifier.fillMaxSize()) {
                        items(statuses) { status ->
                            val statusLeads = leads.filter {
                                it.status_id == status.id &&
                                        (it.name.contains(searchQuery.text, true) ||
                                                it.phone.contains(searchQuery.text, true) ||
                                                it.email.contains(searchQuery.text, true))
                            }
                            Card(
                                modifier = Modifier
                                    .width(340.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(android.graphics.Color.parseColor(status.color))
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            status.name,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Badge
                                        Box(
                                            Modifier
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                statusLeads.size.toString(),
                                                color = Color(android.graphics.Color.parseColor(status.color)),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        IconButton(onClick = {
                                            editingLead = null
                                            defaultStatusForNewLead = status
                                            showForm = true
                                        }) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add Lead",
                                                tint = Color(0xFFFFFFFF)
                                            )
                                        }
                                    }
                                    // Scrollable leads list
                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        if (statusLeads.isEmpty()) {
                                            Text(
                                                "No leads",
                                                color = Color.Gray,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        } else {
                                            Column {
                                                statusLeads.forEach { lead ->
                                                    LeadCardWhite(
                                                        lead = lead,
                                                        onEdit = {
                                                            editingLead = lead
                                                            defaultStatusForNewLead = lead.status
                                                            showForm = true
                                                        },
                                                        onCall = {
                                                            val intent = Intent(
                                                                Intent.ACTION_DIAL,
                                                                Uri.parse("tel:${lead.phone}")
                                                            )
                                                            context.startActivity(intent)
                                                        },
                                                        onWhatsApp = {
                                                            val uri =
                                                                Uri.parse("https://wa.me/${lead.phone}")
                                                            val intent =
                                                                Intent(Intent.ACTION_VIEW, uri)
                                                            context.startActivity(intent)
                                                        },
                                                        onDelete = {
                                                            viewModel.deleteLead(
                                                                lead.id ?: return@LeadCardWhite
                                                            ) { success, msg ->
                                                                scope.launch {
                                                                    snackbarHostState.showSnackbar(
                                                                        msg
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onLocation = {
                                                            lead.latitude?.let { lat ->
                                                                lead.longitude?.let { lng ->
                                                                    val uri =
                                                                        Uri.parse("geo:$lat,$lng?q=$lat,$lng(${lead.address ?: "Lead Location"})")
                                                                    val intent = Intent(
                                                                        Intent.ACTION_VIEW,
                                                                        uri
                                                                    )
                                                                    context.startActivity(intent)
                                                                }
                                                            }
                                                        },
                                                        onDetails = {
                                                            showDetailsForLead = lead
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
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
                defaultStatus = defaultStatusForNewLead,
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
        if (showDetailsForLead != null) {
            AlertDialog(
                onDismissRequest = { showDetailsForLead = null },
                title = { Text("Lead Details") },
                text = {
                    val lead = showDetailsForLead!!
                    Column {
                        Text("Name: ${lead.name}")
                        Text("Phone: ${lead.phone}")
                        Text("Email: ${lead.email}")
                        Text("Status: ${lead.status?.name ?: "-"}")
                        Text("Follow-up: ${lead.follow_up_date?.let { formatDate(it) } ?: "-"}")
                        if (!lead.address.isNullOrBlank()) Text("Address: ${lead.address}")
                        if (!lead.notes.isNullOrBlank()) Text("Notes: ${lead.notes}")
                        if (!lead.description.isNullOrBlank()) Text("Description: ${lead.description}")
                        if (!lead.company.isNullOrBlank()) Text("Company: ${lead.company}")
                        if (!lead.additional_info.isNullOrBlank()) Text("Additional Info: ${lead.additional_info}")
                        if (!lead.source.isNullOrBlank()) Text("Source: ${lead.source}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailsForLead = null }) { Text("Close") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

fun formatDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        formatter.format(parser.parse(dateStr) ?: return dateStr)
    } catch (e: Exception) {
        dateStr
    }
}

// LeadCardWhite: White background, black text, colorful icons, info icon at end
@Composable
fun LeadCardWhite(
    lead: Lead,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onDelete: () -> Unit,
    onLocation: () -> Unit,
    onDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(lead.phone, color = Color.Gray, fontSize = 15.sp)
            }
            if (!lead.email.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(lead.email, color = Color.Gray, fontSize = 15.sp)
                }
            }
            if (!lead.company.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(lead.company ?: "", color = Color.Gray, fontSize = 15.sp)
                }
            }
            if (!lead.follow_up_date.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Follow-up: ${formatDate(lead.follow_up_date)}",
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                }
            }
            // Action icons row
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onWhatsApp) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2196F3))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFF44336)
                    )
                }
                IconButton(onClick = onLocation) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFFFF9800)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDetails) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
