package com.plywoodpocket.crm.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plywoodpocket.crm.ui.theme.*
import com.plywoodpocket.crm.viewmodel.ProfileViewModel
import com.plywoodpocket.crm.viewmodel.ProfileUiState
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.plywoodpocket.crm.models.UserProfile
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import android.widget.Toast


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    // AttendanceViewModel for check-in status
    val attendanceViewModel: com.plywoodpocket.crm.viewmodel.AttendanceViewModel = viewModel(
        factory = com.plywoodpocket.crm.viewmodel.AttendanceViewModelFactory(
            context.applicationContext as android.app.Application,
            com.plywoodpocket.crm.utils.TokenManager(context),
            com.plywoodpocket.crm.api.ApiClient(com.plywoodpocket.crm.utils.TokenManager(context)).apiService
        )
    )
    val isCheckedIn = attendanceViewModel.attendanceStatus == "checked_in"

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImageUri = uri
            // TODO: Handle image upload if needed
        }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is ProfileUiState.Success) {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(White)
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Orange)
                    }
                }

                is ProfileUiState.Success -> {
                    val profile = (uiState as ProfileUiState.Success).profile
                    ProfileContent(
                        profile = profile,
                        selectedImageUri = selectedImageUri,
                        onImageClick = { launcher.launch("image/*") },
                        onEditClick = { showEditSheet = true },
                        onLogoutClick = {
                            if (isCheckedIn) {
                                Toast.makeText(context, "Please check out before logout", Toast.LENGTH_SHORT).show()
                            } else {
                                showLogoutDialog = true
                            }
                        }
                    )
                    if (showEditSheet) {
                        EditProfileBottomSheet(
                            profile = profile,
                            onDismiss = { showEditSheet = false },
                            onSave = { updatedProfile ->
                                viewModel.updateProfile(
                                    com.plywoodpocket.crm.models.UpdateUserRequest.fromUserProfile(
                                        updatedProfile
                                    )
                                )
                                showEditSheet = false
                            }
                        )
                    }
                }

                is ProfileUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState as ProfileUiState.Error).message,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onLogout)
                    }
                ) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    profile: UserProfile,
    selectedImageUri: Uri?,
    onImageClick: () -> Unit,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image with edit
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .clickable { onImageClick() },
            contentAlignment = Alignment.BottomEnd
        ) {
            val painter: Painter? = when {
                selectedImageUri != null -> rememberAsyncImagePainter(selectedImageUri)
                !profile.photo.isNullOrEmpty() -> rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(profile.photo)
                        .crossfade(true)
                        .build()
                )

                else -> null
            }
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            } else {
                // Compose placeholder: Circle with Person icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Placeholder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Image",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(4.dp)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = profile.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = profile.role.name,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        // Edit Button
        OutlinedButton(
            onClick = onEditClick,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Edit Profile")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                ProfileDetailItem(Icons.Default.Email, "Email", profile.email)
                ProfileDetailItem(Icons.Default.Phone, "Phone", profile.phone ?: "Not set")
                ProfileDetailItem(
                    Icons.Default.Chat,
                    "WhatsApp",
                    profile.whatsapp_number ?: "Not set"
                )
                ProfileDetailItem(Icons.Default.LocationOn, "Address", profile.address ?: "Not set")
                ProfileDetailItem(Icons.Default.Place, "Location", profile.location ?: "Not set")
                ProfileDetailItem(Icons.Default.Pin, "Pincode", profile.pincode ?: "Not set")
                ProfileDetailItem(
                    Icons.Default.Work,
                    "Designation",
                    profile.designation ?: "Not set"
                )
                ProfileDetailItem(
                    Icons.Default.DateRange,
                    "Date of Joining",
                    profile.date_of_joining?.let {
                        try {
                            if (it.isNotBlank()) {
                                val inputFormat = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ss",
                                    java.util.Locale.getDefault()
                                )
                                val outputFormat = java.text.SimpleDateFormat(
                                    "dd-MM-yyyy",
                                    java.util.Locale.getDefault()
                                )
                                val date = inputFormat.parse(it.substring(0, 19))
                                date?.let { outputFormat.format(it) } ?: it
                            } else "Not set"
                        } catch (e: Exception) {
                            it
                        }
                    } ?: "Not set"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Targets Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Targets", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TargetItem(
                        "Amount",
                        profile.target_amount?.toString() ?: "Not set",
                        Icons.Default.AttachMoney
                    )
                    TargetItem(
                        "Leads",
                        profile.target_leads?.toString() ?: "Not set",
                        Icons.Default.People
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, Modifier.padding(end = 8.dp))
            Text("Logout")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var phone by remember { mutableStateOf(profile.phone ?: "") }
    var whatsapp by remember { mutableStateOf(profile.whatsapp_number ?: "") }
    var address by remember { mutableStateOf(profile.address ?: "") }
    var location by remember { mutableStateOf(profile.location ?: "") }
    var pincode by remember { mutableStateOf(profile.pincode ?: "") }
    var designation by remember { mutableStateOf(profile.designation ?: "") }

    var phoneError by remember { mutableStateOf(false) }
    var whatsappError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = it.isBlank()
                },
                label = { Text("Name") },
                isError = nameError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (nameError) {
                Text("Name is required", color = Color.Red, fontSize = 12.sp)
            }

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                        phone = it
                    }
                },
                label = { Text("Phone") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (phoneError) {
                Text("Enter valid 10-digit phone", color = Color.Red, fontSize = 12.sp)
            }

            // WhatsApp
            OutlinedTextField(
                value = whatsapp,
                onValueChange = {
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                        whatsapp = it
                    }
                },
                label = { Text("WhatsApp Number") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (whatsappError) {
                Text("Enter valid 10-digit WhatsApp number", color = Color.Red, fontSize = 12.sp)
            }

            // Address, Location, etc.
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pincode,
                onValueChange = { pincode = it },
                label = { Text("Pincode") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = designation,
                onValueChange = { designation = it },
                label = { Text("Designation") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Final Validation
                        nameError = name.isBlank()
                        phoneError = !phone.matches(Regex("^\\d{10}$"))
                        whatsappError = !whatsapp.matches(Regex("^\\d{10}$"))

                        if (!nameError && !phoneError && !whatsappError) {
                            val updatedProfile = profile.copy(
                                name = name,
                                phone = phone,
                                whatsapp_number = whatsapp,
                                address = address,
                                location = location,
                                pincode = pincode,
                                designation = designation
                            )
                            onSave(updatedProfile)
                        }
                    }
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Orange,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Gray
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TargetItem(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier // allow external modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Orange.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = Gray
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Orange
            )
        }
    }
}