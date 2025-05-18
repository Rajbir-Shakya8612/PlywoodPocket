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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.applyCanvas
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlin.math.round
import kotlin.math.pow
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.content.pm.PackageManager
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Job
import androidx.compose.material.icons.filled.History

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
    val state by viewModel.state
    val salespersons by viewModel.salespersons
    val selectedSalesperson by viewModel.selectedSalesperson
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var startDate by remember { mutableStateOf(sdf.format(Date())) }
    var endDate by remember { mutableStateOf(sdf.format(Date())) }
    var showTimelineDialog by remember { mutableStateOf(false) }
    var focusedTrackForDialog by remember { mutableStateOf<AdminLocationTrack?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    var showDialog by remember { mutableStateOf(false) }
    var last10Locations by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var selectedClusterIndex by remember { mutableStateOf<Int?>(null) }

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
                    // List Icon above Load button
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { showTimelineDialog = true }) {
                            Icon(Icons.Default.History, contentDescription = "Show Timeline List")
                        }
                    }

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
                                viewModel.startAutoRefresh()
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("Load")
                        }
                    }

                    // Timeline Dialog
                    if (showTimelineDialog && state.detailedTracks != null) {
                        val processedTracks = processTracksWithStayDuration(state.detailedTracks ?: emptyList())
                        val sortedTracks = processedTracks.sortedByDescending {
                            parseDateTimeToMillis(it.date, it.time?.takeLast(8)) ?: 0L
                        }
                        Dialog(onDismissRequest = { showTimelineDialog = false }) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(min = 220.dp, max = 500.dp)
                            ) {
                                Column(
                                    Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Timeline", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    sortedTracks.forEach { track ->
                                        Card(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .widthIn(min = 180.dp, max = 480.dp)
                                        ) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text("User: ${track.user ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                                Text("Date: ${track.date} ${track.time}", style = MaterialTheme.typography.bodySmall)
                                                Text("Type: ${track.type ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                                if (!track.stay_duration.isNullOrEmpty() && track.stay_duration != "0") {
                                                    Text("Stay Duration: ${track.stay_duration} min", style = MaterialTheme.typography.bodySmall)
                                                }
                                                track.exit_timestamp?.let { Text("Exit: $it", style = MaterialTheme.typography.bodySmall) }
                                                Row(
                                                    Modifier
                                                        .padding(top = 4.dp)
                                                        .fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            focusedTrackForDialog = track
                                                            showTimelineDialog = false
                                                        },
                                                        modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp)
                                                    ) {
                                                        Text("View on Map", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            // Open in Google Maps logic
                                                            val lat = track.latitude.toDoubleOrNull()
                                                            val lng = track.longitude.toDoubleOrNull()
                                                            if (lat != null && lng != null) {
                                                                val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                                context.startActivity(mapIntent)
                                                            }
                                                            showTimelineDialog = false
                                                        },
                                                        modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp)
                                                    ) {
                                                        Text("Open in Google Maps", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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
                        } else if ((state.detailedTracks ?: emptyList()).isNotEmpty()) {
                            val processedTracks = processTracksWithStayDuration(state.detailedTracks ?: emptyList())
                            if (processedTracks.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No location data available for this range.")
                                }
                            } else {
                                // Use the reusable map section
                                LocationMapSection(
                                    tracks = processedTracks,
                                    focusedTrack = focusedTrackForDialog,
                                    showFullTrack = focusedTrackForDialog == null,
                                    cameraPositionState = cameraPositionState
                                )
                                // Reset focus after showing
                                if (focusedTrackForDialog != null) {
                                    LaunchedEffect(focusedTrackForDialog) {
                                        kotlinx.coroutines.delay(300)
                                        focusedTrackForDialog = null
                                    }
                                }
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

suspend fun fetchDirectionsPolyline(points: List<LatLng>, apiKey: String): List<LatLng>? {
    if (points.size < 2) return null
    val origin = "${points.first().latitude},${points.first().longitude}"
    val destination = "${points.last().latitude},${points.last().longitude}"
    val waypoints = points.drop(1).dropLast(1).joinToString("|") { "${it.latitude},${it.longitude}" }
    val url = buildString {
        append("https://maps.googleapis.com/maps/api/directions/json?")
        append("origin=$origin&destination=$destination")
        if (waypoints.isNotEmpty()) append("&waypoints=$waypoints")
        append("&mode=driving&key=$apiKey")
    }
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@withContext null
            val overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
            decodePolyline(overviewPolyline)
        } catch (e: Exception) {
            null
        }
    }
}

fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0
    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lng += dlng
        poly.add(LatLng(lat / 1E5, lng / 1E5))
    }
    return poly
}

fun createNumberedMarkerIcon(context: android.content.Context, number: Int): Bitmap {
    val size = 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    // Draw circle
    canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
    // Draw number
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 36f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    val y = (size / 2f) - ((paint.descent() + paint.ascent()) / 2)
    canvas.drawText(number.toString(), size / 2f, y, paint)
    return bitmap
}

data class ClusteredPoint(
    val latLng: LatLng,
    val count: Int,
    val times: List<String?>,
    val totalStay: Int // in minutes
)

fun clusterPoints(tracks: List<AdminLocationTrack>, precision: Int = 5): List<ClusteredPoint> {
    val map = mutableMapOf<Pair<Double, Double>, MutableList<AdminLocationTrack>>()
    for (track in tracks) {
        val lat = track.latitude.toDoubleOrNull() ?: continue
        val lng = track.longitude.toDoubleOrNull() ?: continue
        val factor = 10.0.pow(precision)
        val key = Pair(round(lat * factor) / factor, round(lng * factor) / factor)
        map.getOrPut(key) { mutableListOf<AdminLocationTrack>() }.add(track)
    }
    return map.map { (key: Pair<Double, Double>, group: MutableList<AdminLocationTrack>) ->
        val totalStay = group.sumOf {
            it.stay_duration?.toIntOrNull() ?: 0
        }
        ClusteredPoint(
            latLng = LatLng(key.first, key.second),
            count = group.size,
            times = group.map { it.time },
            totalStay = totalStay
        )
    }
}

// Helper to parse datetime string to milliseconds (handles both 'date time' and 'date'+'time')
fun parseDateTimeToMillis(date: String?, time: String?): Long? {
    return try {
        val dt = if (time != null && time.contains(":")) "$date $time" else date
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dt?.let { sdf.parse(it)?.time }
    } catch (e: Exception) { null }
}

// Process tracks to auto-calculate stay_duration if missing
fun processTracksWithStayDuration(tracks: List<AdminLocationTrack>): List<AdminLocationTrack> {
    if (tracks.isEmpty()) return tracks
    // Sort tracks by datetime
    val sortedTracks = tracks.sortedBy {
        parseDateTimeToMillis(it.date, it.time?.takeLast(8)) ?: 0L
    }
    val processed = mutableListOf<AdminLocationTrack>()
    var i = 0
    while (i < sortedTracks.size) {
        val current = sortedTracks[i]
        val group = mutableListOf(current)
        var lastLat = current.latitude.toDoubleOrNull() ?: 0.0
        var lastLng = current.longitude.toDoubleOrNull() ?: 0.0
        var j = i + 1
        while (j < sortedTracks.size) {
            val next = sortedTracks[j]
            val lat2 = next.latitude.toDoubleOrNull() ?: 0.0
            val lng2 = next.longitude.toDoubleOrNull() ?: 0.0
            val results = FloatArray(1)
            Location.distanceBetween(lastLat, lastLng, lat2, lng2, results)
            if (results[0] <= 30) {
                group.add(next)
                lastLat = lat2
                lastLng = lng2
                j++
            } else break
        }
        // Calculate stay_duration for the group
        val startMillis = parseDateTimeToMillis(group.first().date, group.first().time?.takeLast(8))
        val endMillis = parseDateTimeToMillis(group.last().date, group.last().time?.takeLast(8))
        val stayDuration = if (startMillis != null && endMillis != null) ((endMillis - startMillis) / 60000).toString() else null
        group.forEach { track ->
            processed.add(track.copy(stay_duration = stayDuration ?: track.stay_duration))
        }
        i += group.size
    }
    return processed
}

@SuppressLint("MissingPermission")
@Composable
fun LocationMapSection(
    tracks: List<AdminLocationTrack>,
    focusedTrack: AdminLocationTrack?,
    showFullTrack: Boolean,
    cameraPositionState: CameraPositionState
) {
    // Process tracks to auto-calculate stay_duration
    val processedTracks = remember(tracks) { processTracksWithStayDuration(tracks) }
    if (processedTracks.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No location data available")
        }
        return
    }

    val context = LocalContext.current
    val clustered = remember(processedTracks) { clusterPoints(processedTracks) }
    val points = clustered.map { it.latLng }
    val apiKey = "AIzaSyCMSCNeXnT5y0CJdTAczN0y9uJe51mytRk" // TODO: Replace with your actual key
    var directionsPolyline by remember { mutableStateOf<List<LatLng>?>(null) }
    var selectedPoint by remember { mutableStateOf<ClusteredPoint?>(null) }
    var last10Locations by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isSatelliteMode by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedClusterIndex by remember { mutableStateOf<Int?>(null) }

    // Function to calculate distance between two points
    fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }

    // Function to get last 10 locations for a point
    fun getLast10Locations(point: ClusteredPoint) {
        val pointTracks = processedTracks.filter { track ->
            val lat = track.latitude.toDoubleOrNull() ?: return@filter false
            val lng = track.longitude.toDoubleOrNull() ?: return@filter false
            val trackLatLng = LatLng(lat, lng)
            calculateDistance(trackLatLng, point.latLng) < 30 // Within 30 meters
        }.sortedByDescending { it.time }
        last10Locations = pointTracks.take(10).map {
            LatLng(it.latitude.toDoubleOrNull() ?: 0.0, it.longitude.toDoubleOrNull() ?: 0.0)
        }
    }

    // Function to open route in Google Maps (robust)
    fun openInGoogleMaps(context: android.content.Context, points: List<LatLng>) {
        if (points.size < 2) return
        val origin = "${points.first().latitude},${points.first().longitude}"
        val destination = "${points.last().latitude},${points.last().longitude}"
        val waypoints = points.drop(1).dropLast(1).joinToString("|") { "${it.latitude},${it.longitude}" }
        val url = "https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$destination&waypoints=$waypoints"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        // Check if Google Maps is installed
        val pm = context.packageManager
        val mapsInstalled = try {
            pm.getPackageInfo("com.google.android.apps.maps", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        if (mapsInstalled) {
            intent.setPackage("com.google.android.apps.maps")
        }
        context.startActivity(intent)
    }

    // Function to open last 10 locations in Google Maps
    fun openLast10InGoogleMaps() {
        if (last10Locations.size < 2) return
        openInGoogleMaps(context, last10Locations)
    }

    LaunchedEffect(points) {
        if (points.size >= 2) {
            // Only fetch directions for points more than 30m apart
            val filteredPoints = mutableListOf<LatLng>()
            var lastPoint = points.first()
            filteredPoints.add(lastPoint)

            for (i in 1 until points.size) {
                val currentPoint = points[i]
                if (calculateDistance(lastPoint, currentPoint) > 30) {
                    filteredPoints.add(currentPoint)
                    lastPoint = currentPoint
                }
            }

            if (filteredPoints.size >= 2) {
                directionsPolyline = fetchDirectionsPolyline(filteredPoints, apiKey)
            }
        }
    }

    LaunchedEffect(focusedTrack) {
        focusedTrack?.latLng()?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    Column {
        // Satellite/Normal Map and Open in Google Maps buttons (responsive)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { isSatelliteMode = !isSatelliteMode },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
            ) {
                Text(
                    if (isSatelliteMode) "Normal Map" else "Satellite Map",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
            Button(
                onClick = { openInGoogleMaps(context, points) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
            ) {
                Text(
                    "Open in Google Maps",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
        }

        // Show dialog for last 10 locations if marker selected
        if (showDialog && selectedClusterIndex != null && last10Locations.size >= 2) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Open last ${last10Locations.size} locations in Google Maps?", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { showDialog = false }) {
                                Text("Cancel")
                            }
                            Button(onClick = {
                                showDialog = false
                                openInGoogleMaps(context, last10Locations)
                            }) {
                                Text("Open in Google Maps")
                            }
                        }
                    }
                }
            }
        }

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = if (isSatelliteMode) MapType.SATELLITE else MapType.NORMAL
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            // Draw main route
            val polylineToDraw = directionsPolyline ?: points
            Polyline(
                points = polylineToDraw,
                color = Color.Blue,
                width = 8f
            )

            // Draw last 10 locations if a point is selected
            if (selectedPoint != null && last10Locations.isNotEmpty()) {
                Polyline(
                    points = last10Locations,
                    color = Color.Red,
                    width = 4f
                )
            }

            clustered.forEachIndexed { idx, cluster ->
                val isFocused = focusedTrack?.latLng() == cluster.latLng
                val iconBitmap = try {
                    if (isFocused) {
                        // Focused marker: use a different color or icon
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    } else {
                        BitmapDescriptorFactory.fromBitmap(createNumberedMarkerIcon(context, idx + 1))
                    }
                } catch (e: Exception) {
                    BitmapDescriptorFactory.defaultMarker(
                        when {
                            isFocused -> BitmapDescriptorFactory.HUE_ORANGE
                            idx == 0 -> BitmapDescriptorFactory.HUE_GREEN
                            idx == clustered.lastIndex -> BitmapDescriptorFactory.HUE_RED
                            else -> BitmapDescriptorFactory.HUE_AZURE
                        }
                    )
                }
                val stayText = if (cluster.totalStay > 0) "Stayed ${cluster.totalStay} min" else ""
                Marker(
                    state = MarkerState(position = cluster.latLng),
                    title = "${idx + 1}. Point${if (stayText.isNotEmpty()) " ($stayText)" else ""}",
                    snippet = "Visited ${cluster.count} times.",
                    icon = iconBitmap,
                    onClick = {
                        // Get last 10 locations up to and including this marker
                        val upToIndex = idx
                        val last10 = clustered.take(upToIndex + 1).takeLast(10).map { it.latLng }
                        last10Locations = last10
                        selectedClusterIndex = idx
                        showDialog = true
                        true
                    }
                )
            }
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
                    if (!track.stay_duration.isNullOrEmpty() && track.stay_duration != "0") {
                        Text("Stay Duration: ${track.stay_duration} min")
                    }
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

    // For auto-refresh job
    private var autoRefreshJob: Job? = null
    private var lastUserId: Int? = null
    private var lastStartDate: String? = null
    private var lastEndDate: String? = null

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
        // Save last params for auto-refresh
        if (userId != null) lastUserId = userId
        if (startDate != null) lastStartDate = startDate
        if (endDate != null) lastEndDate = endDate
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

    fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5 * 60 * 1000) // 5 minutes
                if (lastUserId != null && lastStartDate != null && lastEndDate != null) {
                    fetchDetailedTracks(lastUserId, lastStartDate, lastEndDate)
                }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
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