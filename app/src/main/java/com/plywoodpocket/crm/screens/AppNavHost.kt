package com.plywoodpocket.crm.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plywoodpocket.crm.MainActivity
import com.plywoodpocket.crm.viewmodel.AuthViewModel
import com.plywoodpocket.crm.viewmodel.AuthState
import com.plywoodpocket.crm.viewmodel.AttendanceViewModel
import com.plywoodpocket.crm.viewmodel.AttendanceViewModelFactory
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.utils.TokenManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plywoodpocket.crm.DashboardScreen

@Composable
fun AppNavHost(
    activity: MainActivity,
    initialLeadId: Int? = null
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = remember { AuthViewModel(activity) }
    val context = activity.applicationContext
    
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // Handle initial navigation to follow-up detail if needed
    LaunchedEffect(initialLeadId) {
        if (initialLeadId != null && isLoggedIn) {
            navController.navigate("followup_detail/$initialLeadId")
        }
    }

    // Handle navigation after successful login
    LaunchedEffect(authState) {
        val currentState = authState // Store in local variable for smart casting
        when (currentState) {
            is AuthState.Success -> {
                val msg = currentState.message
                if (msg.contains("Login successful", ignoreCase = true)) {
                    // Navigate based on role after login
                    if (authViewModel.isAdmin()) {
                        navController.navigate("admin_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                } else if (msg.contains("Registration successful", ignoreCase = true)) {
                    // Navigate based on role after registration
                    if (authViewModel.isAdmin()) {
                        navController.navigate("admin_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            }
            is AuthState.Error -> {
                val errorMsg = (authState as AuthState.Error).message
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show() // Show error toast
            }
            else -> {}
        }
    }

    // Determine initial destination
    val startDestination = when {
        !isLoggedIn -> "login"
        authViewModel.isAdmin() -> "admin_dashboard"
        else -> "dashboard"
    }

    NavHost(navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { email, password ->
                    authViewModel.login(email, password)
                    // Navigation will be handled by LaunchedEffect above
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onAttendanceClick = {
                    navController.navigate("attendance")
                },
                onLeadsClick = {
                    navController.navigate("leads")
                }
            )
        }

        composable("admin_dashboard") {
            AdminDashboardScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                },
                onProfileClick = {
                    navController.navigate("profile")
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("attendance") {
            val apiClient = ApiClient(TokenManager(context))
            val attendanceViewModel: AttendanceViewModel = viewModel(
                factory = AttendanceViewModelFactory(
                    context.applicationContext as android.app.Application,
                    TokenManager(context),
                    apiClient.apiService
                )
            )
            AttendanceScreen(
                viewModel = attendanceViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("leads") {
            LeadsScreen(
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }

        composable("followup_detail/{leadId}") { backStackEntry ->
            val leadId = backStackEntry.arguments?.getString("leadId")?.toIntOrNull()
            if (leadId != null) {
                FollowUpDetailScreen(
                    leadId = leadId,
                    navController = navController
                )
            }
        }
    }
} 