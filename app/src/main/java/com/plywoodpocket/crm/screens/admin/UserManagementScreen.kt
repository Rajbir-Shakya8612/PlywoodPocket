package com.plywoodpocket.crm.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plywoodpocket.crm.models.UserProfile
import com.plywoodpocket.crm.viewmodel.UserManagementViewModel
import com.plywoodpocket.crm.viewmodel.UserManagementViewModelFactory

val LightOrange = Color(0xFFFFF3E0)
val Purple = Color(0xFF7C3AED)
val Orange = Color(0xFFFF9800)
val Green = Color(0xFF43A047)
val Red = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UserManagementViewModel = viewModel(
        factory = UserManagementViewModelFactory(context)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val users by viewModel.users.collectAsState()
    val roles by viewModel.roles.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { _ ->
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { _ ->
            viewModel.clearSuccessMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Purple)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add User", tint = Purple)
                    }
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Orange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightOrange)
            )
        },
        containerColor = LightOrange
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightOrange)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Orange)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onEdit = {
                                viewModel.selectUser(user.id)
                                showEditDialog = true
                            },
                            onDelete = {
                                userToDelete = user
                                showDeleteDialog = true
                            },
                            onToggleStatus = { isActive ->
                                viewModel.toggleUserStatus(user.id, isActive)
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateUserDialog(
            roles = roles,
            onDismiss = { showCreateDialog = false },
            onConfirm = { request ->
                viewModel.createUser(request)
                showCreateDialog = false
            }
        )
    }
    
    if (showEditDialog && selectedUser != null) {
        EditUserDialog(
            user = selectedUser!!,
            roles = roles,
            onDismiss = { 
                showEditDialog = false
                viewModel.clearSelectedUser()
            },
            onConfirm = { request ->
                viewModel.updateUser(selectedUser!!.id, request)
                showEditDialog = false
                viewModel.clearSelectedUser()
            }
        )
    }
    
    if (showDeleteDialog && userToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                userToDelete = null
            },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete ${userToDelete!!.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUser(userToDelete!!.id)
                        showDeleteDialog = false
                        userToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        userToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserCard(
    user: UserProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = user.email,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    user.phone?.let {
                        if (it.isNotBlank()) Text(
                            text = it,
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = "Role: ${user.role.name}",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    user.designation?.let {
                        Text(
                            text = "Designation: $it",
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (user.isActiveDisplay) "Active" else "Inactive",
                            color = if (user.isActiveDisplay) Green else Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = user.isActiveDisplay,
                            onCheckedChange = onToggleStatus,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Purple,
                                uncheckedThumbColor = Purple,
                                checkedTrackColor = Orange,
                                uncheckedTrackColor = Orange
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Purple)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Orange)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { showDetails = true },
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("View Details")
            }
        }
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = {
                Text("User Details", color = Purple, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Name prominent
                        Text(
                            text = user.name,
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Divider(thickness = 1.dp, color = Color(0x20AAAAAA))
                        // Email
                        Text("Email:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                        Text(user.email, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        // Phone
                        user.phone?.let {
                            Text("Phone:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // WhatsApp
                        user.whatsapp_number?.let {
                            Text("WhatsApp:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Pincode
                        user.pincode?.let {
                            Text("Pincode:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Address
                        user.address?.let {
                            Text("Address:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Location
                        user.location?.let {
                            Text("Location:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Designation
                        user.designation?.let {
                            Text("Designation:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Date of Joining
                        user.date_of_joining?.let {
                            Text("Date of Joining:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Target Amount
                        user.target_amount?.let {
                            Text("Target Amount:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it.toString(), color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Target Leads
                        user.target_leads?.let {
                            Text("Target Leads:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it.toString(), color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Role
                        user.role.let {
                            Text("Role:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it.name, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Brand ID
                        user.brand_id?.let {
                            Text("Brand ID:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it.toString(), color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Photo
                        user.photo?.let {
                            Text("Photo URL:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        // Status
                        Text(
                            "Status: ${if (user.isActiveDisplay) "Active" else "Inactive"}",
                            color = if (user.isActiveDisplay) Green else Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        // is_active and status (raw fields)
                        user.is_active?.let {
                            Text("is_active:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it.toString(), color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                        user.status?.let {
                            Text("status:", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                            Text(it, color = Color.Black, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close", color = Purple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
