package com.plywoodpocket.crm

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.utils.TokenManager
import com.plywoodpocket.crm.components.InfiniteCardView
import com.plywoodpocket.crm.screens.LoginScreen
import com.plywoodpocket.crm.screens.RegisterScreen
import com.plywoodpocket.crm.ui.theme.*
import com.plywoodpocket.crm.utils.LocationServiceHelper
import com.plywoodpocket.crm.utils.PermissionHandler
import com.plywoodpocket.crm.viewmodel.AuthViewModel
import com.plywoodpocket.crm.viewmodel.AuthState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.plywoodpocket.crm.utils.WorkManagerScheduler
import com.plywoodpocket.crm.screens.AppNavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Download
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.plywoodpocket.crm.models.Banner
import com.plywoodpocket.crm.models.BannerResponse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import android.content.Intent
import android.os.Build
import com.plywoodpocket.crm.utils.LocationTrackingService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Schedule follow-up reminders
        WorkManagerScheduler.scheduleFollowUpReminders(this)
        setContent {
            val context = this
            var permissionsGranted by remember { mutableStateOf(false) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions: Map<String, Boolean> ->
                permissionsGranted = permissions.values.all { it }
            }
            // Check permissions on start and after login
            LaunchedEffect(Unit) {
                if (!PermissionHandler.hasRequiredPermissions(context)) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    )
                } else {
                    permissionsGranted = true
                }
            }
            if (!permissionsGranted) {
                PermissionHandler.PermissionRequestDialog(
                    onDismiss = {},
                    onSettingsClick = {
                        PermissionHandler.openAppSettings(context)
                    }
                )
            } else {
                // Handle notification intent
                val intent = intent
                val shouldNavigateToFollowUp = intent.getBooleanExtra("navigate_to_followup_detail", false)
                val leadId = intent.getIntExtra("lead_id", -1)
                AppNavHost(
                    activity = this,
                    initialLeadId = if (shouldNavigateToFollowUp && leadId != -1) leadId else null
                )
                // Clear the intent extras after handling
                intent.removeExtra("navigate_to_followup_detail")
                intent.removeExtra("lead_id")
            }
        }
    }
}

@Composable
fun MainScreen(activity: MainActivity) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = com.plywoodpocket.crm.viewmodel.AuthViewModelFactory(context))
    var showRegister by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAttendanceScreen by remember { mutableStateOf(false) }
    var showLeadsScreen by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    // Add AttendanceViewModel for check-in status using factory
    val attendanceViewModel: com.plywoodpocket.crm.viewmodel.AttendanceViewModel = viewModel(
        factory = com.plywoodpocket.crm.viewmodel.AttendanceViewModelFactory(
            context.applicationContext as android.app.Application,
            com.plywoodpocket.crm.utils.TokenManager(context),
            com.plywoodpocket.crm.api.ApiClient(com.plywoodpocket.crm.utils.TokenManager(context)).apiService
        )
    )
    val isCheckedIn = attendanceViewModel.attendanceStatus == "checked_in"

    // Permission launcher defined in correct scope
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            showPermissionDialog = true
        } else {
            // Check location services after permissions are granted
            if (!LocationServiceHelper.isLocationEnabled(context)) {
                showLocationDialog = true
            }
        }
    }

    // Check authentication state on app start and when auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                val msg = (authState as AuthState.Success).message
                when {
                    msg.contains("Registration successful", ignoreCase = true) -> {
                        showRegister = false
                    }
                    msg.contains("Login successful", ignoreCase = true) -> {
                        // Check permissions after successful login
                        if (!PermissionHandler.hasRequiredPermissions(context)) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            )
                        } else if (!LocationServiceHelper.isLocationEnabled(context)) {
                            showLocationDialog = true
                        }
                    }
                }
            }
            is AuthState.Error -> {
                val errorMsg = (authState as AuthState.Error).message
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    // Check session expiration from attendance error
    LaunchedEffect(attendanceViewModel.errorMessage) {
        if (attendanceViewModel.errorMessage == "Session expired. Please login again.") {
            authViewModel.logout()
        }
    }

    // Check location services on app start
    LaunchedEffect(Unit) {
        if (!LocationServiceHelper.isLocationEnabled(context)) {
            showLocationDialog = true
        }
    }

    // Check permissions on app start
    LaunchedEffect(Unit) {
        if (!PermissionHandler.hasRequiredPermissions(context)) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
    }

    // Handle check-in status changes
    LaunchedEffect(isCheckedIn) {
        if (isCheckedIn) {
            // Start location tracking service when checked in
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            // Stop location tracking service when checked out
            context.stopService(Intent(context, LocationTrackingService::class.java))
            WorkManagerScheduler.stopLocationTracking(context)
        }
    }

    PlywoodPocketTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            if (!isLoggedIn || showRegister) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(Color.Black)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFFFA726))
                    ) {
                        when {
                            !isLoggedIn && !showRegister -> {
                                LoginScreen(
                                    onLoginSuccess = { email, password ->
                                        authViewModel.login(email, password)
                                    },
                                    onNavigateToRegister = {
                                        showRegister = true
                                    }
                                )
                            }
                            showRegister -> {
                                RegisterScreen(
                                    onNavigateToLogin = {
                                        showRegister = false
                                    },
                                    onRegisterSuccess = {
                                        showRegister = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(Color.Black)
                    )
                }
            } else {
                DashboardScreen(
                    navController = navController,
                    onLogout = {
                        authViewModel.logout()
                    },
                    onAttendanceClick = { showAttendanceScreen = true },
                    onLeadsClick = { showLeadsScreen = true },
                    onPlansClick = { navController.navigate("plans") }
                )
            }

            if (showPermissionDialog) {
                PermissionHandler.PermissionRequestDialog(
                    onDismiss = { showPermissionDialog = false },
                    onSettingsClick = {
                        PermissionHandler.openAppSettings(context)
                        showPermissionDialog = false
                    }
                )
            }
            if (showLocationDialog) {
                LocationServiceHelper.LocationServiceDialog(
                    onDismiss = { showLocationDialog = false },
                    onSettingsClick = {
                        LocationServiceHelper.openLocationSettings(context)
                        showLocationDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onAttendanceClick: () -> Unit,
    onLeadsClick: () -> Unit,
    onPlansClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedIndex by remember { mutableStateOf(2) }
    var searchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current
    val attendanceViewModel: com.plywoodpocket.crm.viewmodel.AttendanceViewModel = viewModel(
        factory = com.plywoodpocket.crm.viewmodel.AttendanceViewModelFactory(
            context.applicationContext as android.app.Application,
            com.plywoodpocket.crm.utils.TokenManager(context),
            com.plywoodpocket.crm.api.ApiClient(com.plywoodpocket.crm.utils.TokenManager(context)).apiService
        )
    )
    val isCheckedIn = attendanceViewModel.attendanceStatus == "checked_in"

    // BannerViewModel instance
    val bannerViewModel: BannerViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val apiService = com.plywoodpocket.crm.api.ApiClient(com.plywoodpocket.crm.utils.TokenManager(context)).apiService
                @Suppress("UNCHECKED_CAST")
                return BannerViewModel(apiService) as T
            }
        }
    )
    val banners = bannerViewModel.banners
    val isLoading = bannerViewModel.isLoading

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Only status bar area black
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(24.dp))
                    .background(Color.Black)
            )
            Spacer(modifier = Modifier.height(12.dp))
            SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (banners.isNotEmpty()) {
                BannerCarousel(banners, context)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // SCROLLABLE SECTION
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 72.dp)
                    .navigationBarsPadding()
            ) {
                TabRowSection(selectedTab) { selectedTab = it }
                Spacer(modifier = Modifier.height(8.dp))
                GridMenu(
                    searchQuery = searchQuery,
                    onAttendanceClick = { navController.navigate("attendance") },
                    onLeadsClick = { navController.navigate("leads") },
                    onPlansClick = { navController.navigate("plans") }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        BottomNavBar(
            selectedIndex = selectedIndex,
            setSelectedIndex = { selectedIndex = it },
            selectedTab = selectedTab,
            setSelectedTab = { selectedTab = it },
            onProfileClick = { navController.navigate("profile") },
            onLogout = {
                if (isCheckedIn) {
                    Toast.makeText(context, "Please check out before logout", Toast.LENGTH_SHORT).show()
                } else {
                    onLogout()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Gray.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .height(56.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = null,
                tint = Gray,
                modifier = Modifier.padding(start = 16.dp)
            )
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("What would you like to do today?") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun BannerCarousel(banners: List<Banner>, context: Context) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val cardWidth = 360.dp
    val cardHeight = 200.dp
    val cardSpacing = 20.dp
    val indicatorBackground = Orange
    val indicatorDotColor = Color.White
    val indicatorDotSize = 7.dp
    val indicatorDotSelectedSize = 11.dp
    val indicatorHeight = 24.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight + 20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(cardSpacing)
        ) {
            items(banners.size) { index ->
                BannerCard(
                    imageUrl = banners[index].image_url,
                    onDownload = { downloadImage(context, banners[index].image_url) },
                    width = cardWidth,
                    height = cardHeight
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (banners.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .height(indicatorHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(indicatorBackground)
                    .padding(horizontal = 12.dp, vertical = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.height(indicatorHeight)
                ) {
                    val currentIndex = listState.firstVisibleItemIndex
                    for (i in banners.indices) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == currentIndex) indicatorDotSelectedSize else indicatorDotSize)
                                .clip(CircleShape)
                                .background(indicatorDotColor)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BannerCard(imageUrl: String, onDownload: () -> Unit, width: Dp = 360.dp, height: Dp = 200.dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Banner Image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
        ) {
            IconButton(
                onClick = onDownload,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Orange)
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
            }
        }
    }
}

@Composable
fun TabRowSection(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TabItem("Home", selectedTab == 0, Modifier.width(100.dp)) { onTabSelected(0) }
        Spacer(modifier = Modifier.width(24.dp))
        TabItem("Analytics", selectedTab == 1, Modifier.width(100.dp)) { onTabSelected(1) }
        Spacer(modifier = Modifier.width(24.dp))
        TabItem("Performance", selectedTab == 2, Modifier.width(100.dp)) { onTabSelected(2) }
    }
}

@Composable
fun TabItem(title: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Orange.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Orange else Gray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun GridMenu(searchQuery: String, onAttendanceClick: () -> Unit, onLeadsClick: () -> Unit, onPlansClick: () -> Unit) {
    val homeItems = listOf(
        Triple("Attendance", android.R.drawable.ic_menu_my_calendar, Color(0xFF1976D2)),
        Triple("Plans", android.R.drawable.ic_menu_compass, Color(0xFF1976D2)),
        Triple("Point Wallet", android.R.drawable.ic_menu_view, Color(0xFF1976D2))
    )
    val analyticsItems = listOf(
        Triple("Leads", android.R.drawable.ic_menu_agenda, Color(0xFF1976D2)),
        Triple("Meetings", android.R.drawable.ic_menu_today, Color(0xFF1976D2)),
        Triple("Primary Sales", android.R.drawable.ic_menu_sort_alphabetically, Color(0xFF1976D2))
    )
    val performanceItems = listOf(
        Triple("Sales", android.R.drawable.ic_menu_sort_by_size, Color(0xFF1976D2)),
        Triple("Tasks", android.R.drawable.ic_menu_manage, Color(0xFF1976D2)),
        Triple("Secondary Sales", android.R.drawable.ic_menu_send, Color(0xFF1976D2))
    )
    val allItems = homeItems + analyticsItems + performanceItems
    val filteredItems = if (searchQuery.isBlank()) null else allItems.filter { it.first.contains(searchQuery, ignoreCase = true) }

    if (filteredItems == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MenuItem("Attendance", android.R.drawable.ic_menu_my_calendar, Color(0xFF1976D2), onClick = onAttendanceClick)
                MenuItem("Plans", android.R.drawable.ic_menu_compass, Color(0xFF1976D2), onClick = onPlansClick)
                homeItems.drop(2).forEach { MenuItem(it.first, it.second, it.third, onClick = {}) }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MenuItem("Leads", android.R.drawable.ic_menu_agenda, Color(0xFF1976D2), onClick = onLeadsClick)
                analyticsItems.drop(1).forEach { MenuItem(it.first, it.second, it.third, onClick = {}) }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                performanceItems.forEach { MenuItem(it.first, it.second, it.third, onClick = {}) }
            }
        }
    } else {
        if (filteredItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No results found", color = Gray)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                filteredItems.forEach { MenuItem(it.first, it.second, it.third, onClick = {}) }
            }
        }
    }
}

@Composable
fun MenuItem(title: String, iconRes: Int, textColor: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .padding(vertical = 15.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(White)
                .border(1.dp, Orange, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Orange,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 11.sp,
            maxLines = 2,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun BottomNavBar(
    selectedIndex: Int,
    setSelectedIndex: (Int) -> Unit,
    selectedTab: Int,
    setSelectedTab: (Int) -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shadowElevation = 8.dp,
        color = White,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Filled.Home, "Home", selectedIndex == 0) {
                setSelectedIndex(0)
                setSelectedTab(0)
            }
            BottomNavItem(Icons.Filled.Star, "Favorites", selectedIndex == 1) {
                setSelectedIndex(1)
            }
            BottomNavItem(Icons.Filled.Menu, "Dashboard", selectedIndex == 2) {
                setSelectedIndex(2)
                setSelectedTab(0)
            }
            BottomNavItem(Icons.Filled.Logout, "Logout", selectedIndex == 3) {
                setSelectedIndex(3)
                onLogout()
            }
            BottomNavItem(Icons.Filled.Person, "Profile", selectedIndex == 4) {
                setSelectedIndex(4)
                onProfileClick()
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) Orange else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (selected) White else Gray)
        }
        Text(
            text = label,
            color = if (selected) Orange else Gray,
            fontSize = 14.sp
        )
    }
}

// Date utility for dd-MM-yyyy format
fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    return sdf.format(Date())
}

@Composable
fun AppNavHost(
    activity: MainActivity,
    initialLeadId: Int? = null
) {
    val navController = rememberNavController()
    var showLogin by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = remember { AuthViewModel(activity) }
    val context = activity.applicationContext

    LaunchedEffect(Unit) {
        showLogin = !authViewModel.isLoggedIn()
    }

    // Handle initial navigation to follow-up detail if needed
    LaunchedEffect(initialLeadId) {
        if (initialLeadId != null && !showLogin) {
            navController.navigate("followup_detail/$initialLeadId")
        }
    }


}

// Banner data model
data class Banner(
    val id: Int,
    val image_url: String
)

// BannerViewModel for API fetching
class BannerViewModel(private val apiService: com.plywoodpocket.crm.api.ApiService) : ViewModel() {
    var banners by mutableStateOf<List<Banner>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set

    init {
        fetchBanners()
    }

    private fun fetchBanners() {
        viewModelScope.launch {
            try {
                val response = apiService.getBanners()
                if (response.isSuccessful) {
                    banners = response.body()?.banners ?: emptyList()
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }
}

// Download utility function
fun downloadImage(context: Context, imageUrl: String) {
    val request = DownloadManager.Request(Uri.parse(imageUrl))
        .setTitle("BannerImage")
        .setDescription("Downloading image...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "banner_${System.currentTimeMillis()}.jpg")
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
}