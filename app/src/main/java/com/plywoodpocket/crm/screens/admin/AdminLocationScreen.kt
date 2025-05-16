package com.plywoodpocket.crm.screens.admin

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.models.*
import com.plywoodpocket.crm.utils.TokenManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import androidx.compose.material3.CenterAlignedTopAppBar
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun AdminLocationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AdminLocationViewModel = viewModel(
        factory = AdminLocationViewModelFactory(ApiClient(TokenManager(context)).apiService)
    )
    val state = viewModel.state.value
    val salespersons by viewModel.salespersons
    val selectedSalesperson by viewModel.selectedSalesperson
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var startDate by remember { mutableStateOf(sdf.format(Date())) }
    var endDate by remember { mutableStateOf(sdf.format(Date())) }

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Location Timeline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (permissionState.status) {
                is PermissionStatus.Granted -> {
                    // Salesperson Dropdown
                    SalespersonDropdown(
                        salespersons = salespersons,
                        selected = selectedSalesperson,
                        onSelect = { viewModel.selectSalesperson(it) }
                    )
                    Spacer(Modifier.height(8.dp))

                    // Responsive Date Pickers and Load Button
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DatePickerField(
                            label = "Start Date",
                            date = startDate,
                            onDateSelected = { selected -> startDate = selected }
                        )
                        DatePickerField(
                            label = "End Date",
                            date = endDate,
                            onDateSelected = { selected -> endDate = selected }
                        )
                        Button(
                            onClick = {
                                viewModel.fetchDetailedTracks(
                                    userId = selectedSalesperson?.id,
                                    startDate = startDate,
                                    endDate = endDate
                                )
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("Load")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        if (state.isLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (state.error != null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error: ${state.error}", color = Color.Red)
                            }
                        } else if (state.detailedTracks != null) {
                            val tracks = state.detailedTracks
                            if (tracks.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No location data available for this range.")
                                }
                            } else {
                                // Use the reusable map section
                                LocationMapSection(
                                    tracks = tracks,
                                    focusedTrack = null,
                                    showFullTrack = true
                                )
                            }
                        }
                    }
                }
                is PermissionStatus.Denied -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Location permission is required to display the map.")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { permissionState.launchPermissionRequest() }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerField(label: String, date: String, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val (year, month, day) = date.split("-").map { it.toInt() }
    calendar.set(year, month - 1, day)
    val datePickerDialog = remember {
        DatePickerDialog(context, { _, y, m, d ->
            val selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
            onDateSelected(selectedDate)
        }, year, month - 1, day)
    }
    OutlinedButton(onClick = { datePickerDialog.show() }) {
        Text("$label: $date")
    }
}

@Composable
fun LocationStatsSection(stats: AdminLocationStats) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Total Locations: ${stats.total_locations ?: 0}")
            Text("Total Distance: ${stats.total_distance ?: 0.0} km")
            Text("Average Speed: ${stats.average_speed ?: 0.0} km/h")
            Text("Total Time: ${stats.total_time ?: "-"}")
            stats.check_ins?.let { Text("Check-ins: $it") }
            stats.check_outs?.let { Text("Check-outs: $it") }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun LocationMapSection(
    tracks: List<AdminLocationTrack>,
    focusedTrack: AdminLocationTrack?,
    showFullTrack: Boolean
) {
    if (tracks.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No location data available")
        }
        return
    }
    val points = tracks.mapNotNull { it.latLng() }
    val cameraPositionState = rememberCameraPositionState {
        position = when {
            focusedTrack?.latLng() != null -> CameraPosition.fromLatLngZoom(focusedTrack.latLng()!!, 16f)
            points.isNotEmpty() -> CameraPosition.fromLatLngZoom(points.first(), 12f)
            else -> CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 1f)
        }
    }
    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        cameraPositionState = cameraPositionState
    ) {
        if (showFullTrack) {
            // All markers and polyline
            tracks.forEachIndexed { idx, track ->
                val isCurrent = idx == tracks.lastIndex
                track.latLng()?.let { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = track.user ?: "User",
                        snippet = buildString {
                            append("${track.date} ${track.time}")
                            track.stay_duration?.let { append("\nStay: $it") }
                            track.exit_timestamp?.let { append("\nExit: $it") }
                        },
                        icon = if (isCurrent) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN) else null
                    )
                }
            }
            Polyline(
                points = points,
                color = Color(0xFF4CAF50), // Green
                width = 6f
            )
        } else if (focusedTrack?.latLng() != null) {
            // Only focused marker
            Marker(
                state = MarkerState(position = focusedTrack.latLng()!!),
                title = focusedTrack.user ?: "User",
                snippet = buildString {
                    append("${focusedTrack.date} ${focusedTrack.time}")
                    focusedTrack.stay_duration?.let { append("\nStay: ${focusedTrack.stay_duration}") }
                    focusedTrack.exit_timestamp?.let { append("\nExit: ${focusedTrack.exit_timestamp}") }
                },
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }
    }
}

@Composable
fun LocationTrackListSection(
    tracks: List<AdminLocationTrack>,
    onViewOnMap: (AdminLocationTrack) -> Unit,
    onOpenInGoogleMaps: (AdminLocationTrack) -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Text("Timeline", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        tracks.forEach { track ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Text("User: ${track.user ?: "-"}")
                    Text("Date: ${track.date} ${track.time}")
                    Text("Type: ${track.type ?: "-"}")
                    track.stay_duration?.let { Text("Stay Duration: $it") }
                    track.exit_timestamp?.let { Text("Exit: $it") }
                    Row(Modifier.padding(top = 4.dp)) {
                        Button(onClick = { onViewOnMap(track) }) {
                            Text("View on Map")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onOpenInGoogleMaps(track) }) {
                            Text("Open in Google Maps")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalespersonDropdown(
    salespersons: List<UserProfile>,
    selected: UserProfile?,
    onSelect: (UserProfile?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.name ?: "Select Salesperson")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            salespersons.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.name ?: "-") },
                    onClick = {
                        expanded = false
                        onSelect(user)
                    }
                )
            }
        }
    }
}

@Composable
fun MonthDropdown(
    months: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = { Text(month) },
                    onClick = {
                        expanded = false
                        onSelect(month)
                    }
                )
            }
        }
    }
}

fun getLast12Months(): List<String> {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val calendar = Calendar.getInstance()
    return List(12) {
        val month = sdf.format(calendar.time)
        calendar.add(Calendar.MONTH, -1)
        month
    }
}

class AdminLocationViewModel(private val apiService: ApiService) : ViewModel() {
    private val _state = mutableStateOf(AdminLocationState())
    val state: State<AdminLocationState> = _state

    private val _salespersons = mutableStateOf<List<UserProfile>>(emptyList())
    val salespersons: State<List<UserProfile>> = _salespersons

    private val _selectedSalesperson = mutableStateOf<UserProfile?>(null)
    val selectedSalesperson: State<UserProfile?> = _selectedSalesperson

    // New states for map focus and full track
    private val _focusedTrack = mutableStateOf<AdminLocationTrack?>(null)
    val focusedTrack: State<AdminLocationTrack?> = _focusedTrack

    private val _showFullTrack = mutableStateOf(true)
    val showFullTrack: State<Boolean> = _showFullTrack

    fun focusTrack(track: AdminLocationTrack?) {
        _focusedTrack.value = track
        _showFullTrack.value = false
    }
    fun showFullTrack() {
        _focusedTrack.value = null
        _showFullTrack.value = true
    }

    init {
        fetchSalespersons()
    }

    fun fetchSalespersons() {
        viewModelScope.launch {
            try {
                val response = apiService.getAdminUsers()
                if (response.isSuccessful) {
                    _salespersons.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun selectSalesperson(user: UserProfile?) {
        _selectedSalesperson.value = user
    }

    fun fetchDetailedTracks(userId: Int? = null, startDate: String? = null, endDate: String? = null) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = apiService.getAdminDetailedTracks(userId, startDate, endDate)
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        detailedTracks = response.body()?.data,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = response.message())
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }
}

class AdminLocationViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminLocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminLocationViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class AdminLocationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val timeline: AdminLocationTimelineResponse? = null,
    val detailedTracks: List<AdminLocationTrack>? = null
)