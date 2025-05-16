package com.plywoodpocket.crm.screens.admin

import android.annotation.SuppressLint
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
//import com.google.maps.android.graphics.BitmapDescriptorFactory

@OptIn(ExperimentalMaterial3Api::class)
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
    val months = remember { getLast12Months() }
    var selectedMonth by remember { mutableStateOf(months.first()) }
    val focusedTrack by viewModel.focusedTrack
    val showFullTrack by viewModel.showFullTrack

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
            // Salesperson Dropdown
            SalespersonDropdown(
                salespersons = salespersons,
                selected = selectedSalesperson,
                onSelect = { viewModel.selectSalesperson(it) }
            )
            Spacer(Modifier.height(8.dp))
            // Month Picker
            MonthDropdown(
                months = months,
                selected = selectedMonth,
                onSelect = {
                    selectedMonth = it
                    viewModel.fetchTimeline(selectedSalesperson?.id, it)
                }
            )
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.error != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.error}", color = Color.Red)
                    }
                } else if (state.timeline != null) {
                    val timeline = state.timeline
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        LocationStatsSection(timeline.stats)
                        Spacer(Modifier.height(16.dp))
                        // Buttons for full track and open in Google Maps
                        Row(Modifier.padding(horizontal = 16.dp)) {
                            Button(onClick = { viewModel.showFullTrack() }) {
                                Text("View Full Track")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                // Open last 10 points in Google Maps
                                val points = timeline.tracks.takeLast(10).map { it.latitude to it.longitude }
                                if (points.size >= 2) {
                                    val uri = "http://maps.google.com/maps?saddr=${points.first().first},${points.first().second}&daddr=" +
                                        points.drop(1).joinToString("+to:") { "${it.first},${it.second}" }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                                    context.startActivity(intent)
                                }
                            }) {
                                Text("Open Last 10 in Google Maps")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LocationMapSection(
                            tracks = timeline.tracks,
                            focusedTrack = focusedTrack,
                            showFullTrack = showFullTrack
                        )
                        Spacer(Modifier.height(16.dp))
                        LocationTrackListSection(
                            tracks = timeline.tracks,
                            onViewOnMap = { viewModel.focusTrack(it) },
                            onOpenInGoogleMaps = { track ->
                                val uri = "geo:${track.latitude},${track.longitude}?q=${track.latitude},${track.longitude}(Location)"
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
    // Fetch data on first launch
    LaunchedEffect(Unit) {
        viewModel.fetchTimeline()
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
    val points = tracks.map { LatLng(it.latitude, it.longitude) }
    val cameraPositionState = rememberCameraPositionState {
        position = when {
            focusedTrack != null -> CameraPosition.fromLatLngZoom(LatLng(focusedTrack.latitude, focusedTrack.longitude), 16f)
            points.isNotEmpty() -> CameraPosition.fromLatLngZoom(points.first(), 12f)
            else -> CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 1f)
        }
    }
    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        cameraPositionState = cameraPositionState
    ) {
        if (showFullTrack) {
            // All markers and polyline
            tracks.forEachIndexed { idx, track ->
                val isCurrent = idx == tracks.lastIndex
                Marker(
                    state = MarkerState(position = LatLng(track.latitude, track.longitude)),
                    title = track.user ?: "User",
                    snippet = buildString {
                        append("${track.date} ${track.time}")
                        track.stay_duration?.let { append("\nStay: $it") }
                        track.exit_timestamp?.let { append("\nExit: $it") }
                    },
                    icon = if (isCurrent) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN) else null
                )
            }
            Polyline(
                points = points,
                color = Color(0xFF4CAF50), // Green
                width = 6f
            )
        } else if (focusedTrack != null) {
            // Only focused marker
            Marker(
                state = MarkerState(position = LatLng(focusedTrack.latitude, focusedTrack.longitude)),
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
        fetchTimeline(userId = user?.id)
    }

    fun fetchTimeline(userId: Int? = null, month: String? = null) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = apiService.getAdminLocationTimeline(userId, month)
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        timeline = response.body(),
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
    val timeline: AdminLocationTimelineResponse? = null
) 