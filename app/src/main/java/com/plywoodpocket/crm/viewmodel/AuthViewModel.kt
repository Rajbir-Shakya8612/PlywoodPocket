package com.plywoodpocket.crm.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.LoginRequest
import com.plywoodpocket.crm.models.RegisterRequest
import com.plywoodpocket.crm.models.Role
import com.plywoodpocket.crm.utils.TokenManager
import com.plywoodpocket.crm.utils.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(context: Context) : ViewModel() {

    private val appContext = context
    private val tokenManager = TokenManager(context)
    private val apiService = ApiClient(tokenManager).apiService

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoggedIn = MutableStateFlow(tokenManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    private val _roles = MutableStateFlow<List<Role>>(emptyList())
    val roles: StateFlow<List<Role>> = _roles

    private val _selectedRole = MutableStateFlow<Role?>(null)
    val selectedRole: StateFlow<Role?> = _selectedRole

    private val _isLoadingRoles = MutableStateFlow(false)
    val isLoadingRoles: StateFlow<Boolean> = _isLoadingRoles

    private val _rolesError = MutableStateFlow<String?>(null)
    val rolesError: StateFlow<String?> = _rolesError

    init {
        _userRole.value = tokenManager.getUserRole()
        // Only set error state if not logged in
        if (!tokenManager.isLoggedIn()) {
            _authState.value = AuthState.Error("Please login to continue")
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService?.login(LoginRequest(email, password))
                if (response?.isSuccessful == true) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        val expiration = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24 hrs
                        tokenManager.saveAuthData(
                            loginResponse.token,
                            loginResponse.user.id,
                            loginResponse.user.name,
                            loginResponse.user.email,
                            loginResponse.user.role.slug, // Save role slug instead of name
                            expiration
                        )
                        _isLoggedIn.value = true
                        _userRole.value = loginResponse.user.role.slug
                        _authState.value = AuthState.Success("Login successful")
                        // Fetch live location in background to warm up GPS
                        launch(Dispatchers.IO) {
                            try {
                                LocationHelper.getCurrentLocation(appContext)
                            } catch (_: Exception) {}
                        }
                    } else {
                        _authState.value = AuthState.Error("Login failed: Empty response")
                    }
                } else {
                    _authState.value = AuthState.Error("Login failed: ${response?.message() ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        brandPasskey: String,
        selectedRole: Int
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val request = RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                    password_confirmation = confirmPassword,
                    role_id = selectedRole,
                    passkey = brandPasskey
                )

                val response = apiService?.register(request)
                if (response?.isSuccessful == true) {
                    val registerResponse = response.body()
                    if (registerResponse != null) {
                        val expiration = System.currentTimeMillis() + 24 * 60 * 60 * 1000
                        // Set role based on role_id (1 = admin)
                        val roleSlug = if (registerResponse.user.role_id == 1) "admin" else "employee"
                        tokenManager.saveAuthData(
                            registerResponse.token,
                            registerResponse.user.id,
                            registerResponse.user.name,
                            registerResponse.user.email,
                            roleSlug,
                            expiration
                        )
                        _isLoggedIn.value = true
                        _userRole.value = roleSlug
                        _authState.value = AuthState.Success("Registration successful")
                    } else {
                        _authState.value = AuthState.Error("Registration failed: Empty response")
                    }
                } else {
                    _authState.value = AuthState.Error("Registration failed: ${response?.message() ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Registration failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService?.logout()
                tokenManager.clearAuthData()
                _isLoggedIn.value = false
                _userRole.value = null
                _authState.value = AuthState.Success("Logout successful")
            } catch (e: Exception) {
                // Even if there's an exception, clear local auth data
                tokenManager.clearAuthData()
                _isLoggedIn.value = false
                _userRole.value = null
                _authState.value = AuthState.Success("Logout successful")
            }
        }
    }

    fun fetchRoles() {
        viewModelScope.launch {
            _isLoadingRoles.value = true
            _rolesError.value = null
            try {
                val response = apiService?.getRoles()
                if (response?.isSuccessful == true) {
                    response.body()?.let {
                        _roles.value = it
                    } ?: run {
                        _rolesError.value = "Roles list is empty"
                    }
                } else {
                    _rolesError.value = "Failed to fetch roles: ${response?.message() ?: "Unknown error"}"
                }
            } catch (e: Exception) {
                _rolesError.value = "Failed to fetch roles: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoadingRoles.value = false
            }
        }
    }

    fun setSelectedRole(roleId: Int) {
        _selectedRole.value = _roles.value.find { it.id == roleId }
    }

    fun isLoggedIn(): Boolean {
        return _isLoggedIn.value
    }

    fun isAdmin(): Boolean {
        return _userRole.value?.equals("admin", ignoreCase = true) == true
    }
}
