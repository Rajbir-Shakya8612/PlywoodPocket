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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Schedule follow-up reminders
        WorkManagerScheduler.scheduleFollowUpReminders(this)
        setContent {
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

@Composable
fun MainScreen(activity: MainActivity) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = com.plywoodpocket.crm.viewmodel.AuthViewModelFactory(context))
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    var showRegister by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAttendanceScreen by remember { mutableStateOf(false) }
    var showLeadsScreen by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    // Observe AttendanceViewModel error for session expiration
    val attendanceViewModel: com.plywoodpocket.crm.viewmodel.AttendanceViewModel = viewModel()
    val attendanceError = attendanceViewModel.errorMessage

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
                        // Navigation will be handled in AppNavHost based on role
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
    LaunchedEffect(attendanceError) {
        if (attendanceError == "Session expired. Please login again.") {
            authViewModel.logout()
        }
    }

    // Check location services on app start
    LaunchedEffect(Unit) {
        if (!LocationServiceHelper.isLocationEnabled(context)) {
            showLocationDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            showPermissionDialog = true
        }
    }

    LaunchedEffect(Unit) {
        if (!PermissionHandler.hasRequiredPermissions(context as android.app.Activity)) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
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
                    onLeadsClick = { showLeadsScreen = true }
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
    onLeadsClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedIndex by remember { mutableStateOf(2) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )
        Spacer(modifier = Modifier.height(24.dp))
        BankCard()
        Spacer(modifier = Modifier.height(24.dp))
        InfiniteCardView()
        Spacer(modifier = Modifier.height(16.dp))
        TabRowSection(selectedTab) { selectedTab = it }
        Spacer(modifier = Modifier.height(16.dp))
        GridMenu(searchQuery, onAttendanceClick, onLeadsClick)
        Spacer(modifier = Modifier.weight(1f))
        BottomNavBar(
            selectedIndex = selectedIndex,
            setSelectedIndex = { selectedIndex = it },
            selectedTab = selectedTab,
            setSelectedTab = { selectedTab = it },
            onProfileClick = { navController.navigate("profile") },
            onLogout = { onLogout() },
            modifier = Modifier.navigationBarsPadding()
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
fun BankCard() {
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(listOf(Orange, OrangeLight))
            )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("ICICI Bank", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("2300 1130 5224", color = White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Account No.", color = White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrangeLight.copy(alpha = 0.7f))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text("Show Balance", color = White, fontSize = 14.sp)
                }
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
fun GridMenu(searchQuery: String, onAttendanceClick: () -> Unit, onLeadsClick: () -> Unit) {
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
                homeItems.drop(1).forEach { MenuItem(it.first, it.second, it.third, onClick = {}) }
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