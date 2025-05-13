package com.plywoodpocket.crm.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plywoodpocket.crm.MainActivity
import com.plywoodpocket.crm.viewmodel.AuthViewModel
import com.plywoodpocket.crm.viewmodel.AttendanceViewModel
import com.plywoodpocket.crm.viewmodel.AttendanceViewModelFactory
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.utils.TokenManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plywoodpocket.crm.DashboardScreen

@Composable
fun AppNavHost(activity: MainActivity) {
    val navController = rememberNavController()
    var showLogin by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = remember { AuthViewModel(activity) }
    val context = activity.applicationContext

    LaunchedEffect(Unit) {
        showLogin = !authViewModel.isLoggedIn()
    }

    NavHost(navController, startDestination = if (showLogin) "login" else "dashboard") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { email, password ->
                    authViewModel.login(email, password)
                    showLogin = false
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
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
                    showLogin = false
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    showLogin = true
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

        composable("profile") {
            ProfileScreen(
                onLogout = {
                    authViewModel.logout()
                    showLogin = true
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