package com.plywoodpocket.crm.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.plywoodpocket.crm.viewmodel.AdminLeadsViewModel
import com.plywoodpocket.crm.viewmodel.AdminLeadsViewModelFactory
import com.plywoodpocket.crm.viewmodel.LeadsUiState
import com.plywoodpocket.crm.screens.LeadFormDialog
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeadsScreen(
    onBack: () -> Unit,
    navController: NavController? = null
) {
    val context = LocalContext.current
    val viewModel: AdminLeadsViewModel =
        viewModel(factory = AdminLeadsViewModelFactory(context.applicationContext as android.app.Application))
    val uiState by viewModel.uiState.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingLead by remember { mutableStateOf<Lead?>(null) }
    var defaultStatusForNewLead by remember { mutableStateOf<LeadStatus?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var showDetailsForLead by remember { mutableStateOf<Lead?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLeads()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Leads", fontWeight = FontWeight.Bold) },
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
            }, containerColor = Color.Red) {
                Icon(Icons.Default.Add, contentDescription = "Add Lead", tint = Color.White)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
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
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
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
                                                    com.plywoodpocket.crm.screens.LeadCardWhite(
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
                                                            val phoneNumber =
                                                                if (lead.phone.startsWith("+91")) {
                                                                    lead.phone
                                                                } else {
                                                                    "+91${lead.phone}"
                                                                }
                                                            val uri =
                                                                Uri.parse("https://wa.me/${phoneNumber}")
                                                            val intent =
                                                                Intent(Intent.ACTION_VIEW, uri)
                                                            context.startActivity(intent)
                                                        },
                                                        onDelete = {
                                                            viewModel.deleteLead(
                                                                lead.id ?: return@LeadCardWhite
                                                            ) { success, msg ->
                                                                scope.launch {
                                                                    snackbarHostState.showSnackbar(msg)
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
                            Text("Follow-up: ${lead.follow_up_date ?: "-"}")
                            if (!lead.address.isNullOrBlank()) Text("Address: ${lead.address}")
                            if (!lead.notes.isNullOrBlank()) Text("Notes: ${lead.notes}")
                            if (!lead.description.isNullOrBlank()) Text("Description: ${lead.description}")
                            if (!lead.company.isNullOrBlank()) Text("Company: ${lead.company}")
                            if (lead.additional_info != null) {
                                val info = lead.additional_info
                                val infoText = when (info) {
                                    is String -> info
                                    is List<*> -> info.joinToString(", ") { it.toString() }
                                    else -> info.toString()
                                }
                                if (infoText.isNotBlank() && infoText != "null") Text("Additional Info: $infoText")
                            }
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
} 