package com.plywoodpocket.crm

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plywoodpocket.crm.components.InfiniteCardView
import com.plywoodpocket.crm.ui.theme.*
import com.plywoodpocket.crm.utils.PermissionHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var showPermissionDialog by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            showPermissionDialog = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                if (!PermissionHandler.hasRequiredPermissions(this@MainActivity)) {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS
                    ))
                }
            }

            PlywoodPocketTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }

            if (showPermissionDialog) {
                PermissionHandler.PermissionRequestDialog(
                    onDismiss = { showPermissionDialog = false },
                    onSettingsClick = {
                        PermissionHandler.openAppSettings(this@MainActivity)
                        showPermissionDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen() {
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
        Spacer(modifier = Modifier.height(40.dp))
        TabRowSection(selectedTab) { selectedTab = it }
        Spacer(modifier = Modifier.height(32.dp))
        GridMenu(searchQuery)
        Spacer(modifier = Modifier.weight(1f))
        BottomNavBar(
            selectedIndex = selectedIndex,
            setSelectedIndex = { selectedIndex = it },
            selectedTab = selectedTab,
            setSelectedTab = { selectedTab = it }
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
fun GridMenu(searchQuery: String) {
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
                homeItems.forEach { MenuItem(it.first, it.second, it.third) }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                analyticsItems.forEach { MenuItem(it.first, it.second, it.third) }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                performanceItems.forEach { MenuItem(it.first, it.second, it.third) }
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
                filteredItems.forEach { MenuItem(it.first, it.second, it.third) }
            }
        }
    }
}

@Composable
fun MenuItem(title: String, iconRes: Int, textColor: Color) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .padding(vertical = 15.dp)
            .clickable { },
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
    setSelectedTab: (Int) -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = White
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
            BottomNavItem(Icons.Filled.ShoppingCart, "Offers", selectedIndex == 3) {
                setSelectedIndex(3)
            }
            BottomNavItem(Icons.Filled.Person, "Profile", selectedIndex == 4) {
                setSelectedIndex(4)
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