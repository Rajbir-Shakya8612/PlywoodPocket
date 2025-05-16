package com.plywoodpocket.crm.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.plywoodpocket.crm.BottomNavBar
import com.plywoodpocket.crm.TabRowSection
import com.plywoodpocket.crm.ui.theme.*

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit
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
        TabRowSection(selectedTab) { selectedTab = it }
        Spacer(modifier = Modifier.height(16.dp))
        AdminGridMenu(
            searchQuery = searchQuery,
            onUsersClick = { navController.navigate("users_screen") },
            onReportsClick = { navController.navigate("reports_screen") },
            onSettingsClick = { navController.navigate("settings_screen") },
            onApprovalsClick = { navController.navigate("approvals_screen") },
            onNotificationsClick = { navController.navigate("notifications_screen") },
            onAuditLogClick = { navController.navigate("audit_log_screen") },
            onAnalyticsClick = { navController.navigate("analytics_screen") },
            onLocationClick = { navController.navigate("admin_location_screen") },
            onSecurityClick = { navController.navigate("security_screen") }
        )
        Spacer(modifier = Modifier.weight(1f))
        BottomNavBar(
            selectedIndex = selectedIndex,
            setSelectedIndex = { selectedIndex = it },
            selectedTab = selectedTab,
            setSelectedTab = { selectedTab = it },
            onProfileClick = onProfileClick,
            onLogout = onLogout,
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
            .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
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
                tint = Color.Gray,
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
fun AdminGridMenu(
    searchQuery: String,
    onUsersClick: () -> Unit,
    onReportsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onApprovalsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAuditLogClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onLocationClick: () -> Unit,
    onSecurityClick: () -> Unit
) {
    val adminItems = listOf(
        Triple("Users", android.R.drawable.ic_menu_manage, Color(0xFF1976D2)) to onUsersClick,
        Triple("Reports", android.R.drawable.ic_menu_agenda, Color(0xFF1976D2)) to onReportsClick,
        Triple("Settings", android.R.drawable.ic_menu_preferences, Color(0xFF1976D2)) to onSettingsClick,
        Triple("Approvals", android.R.drawable.ic_menu_send, Color(0xFF1976D2)) to onApprovalsClick,
        Triple("Notifications", android.R.drawable.ic_menu_info_details, Color(0xFF1976D2)) to onNotificationsClick,
        Triple("Audit Log", android.R.drawable.ic_menu_recent_history, Color(0xFF1976D2)) to onAuditLogClick,
        Triple("Analytics", android.R.drawable.ic_menu_sort_by_size, Color(0xFF1976D2)) to onAnalyticsClick,
        Triple("Location", android.R.drawable.ic_menu_save, Color(0xFF1976D2)) to onLocationClick,
        Triple("Security", android.R.drawable.ic_menu_camera, Color(0xFF1976D2)) to onSecurityClick,
    )
    val filteredItems = if (searchQuery.isBlank()) null else adminItems.filter { it.first.first.contains(searchQuery, ignoreCase = true) }

    if (filteredItems == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                adminItems.take(3).forEach { (item, onClick) ->
                    MenuItem(item.first, item.second, item.third, onClick = onClick)
                }
            }
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                adminItems.drop(3).take(3).forEach { (item, onClick) ->
                    MenuItem(item.first, item.second, item.third, onClick = onClick)
                }
            }
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                adminItems.drop(6).forEach { (item, onClick) ->
                    MenuItem(item.first, item.second, item.third, onClick = onClick)
                }
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
                filteredItems.forEach { (item, onClick) ->
                    MenuItem(item.first, item.second, item.third, onClick = onClick)
                }
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