package com.plywoodpocket.crm.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.LoginRequest
import com.plywoodpocket.crm.models.RegisterRequest
import com.plywoodpocket.crm.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(context: Context) : ViewModel() {
    private val tokenManager = TokenManager(context)
    private val apiClient = ApiClient(tokenManager)
    private val apiService = apiClient.apiService

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _roles = MutableStateFlow<List<com.plywoodpocket.crm.models.Role>>(emptyList())
    val roles: StateFlow<List<com.plywoodpocket.crm.models.Role>> = _roles

    private val _selectedRole = MutableStateFlow<com.plywoodpocket.crm.models.Role?>(null)
    val selectedRole: StateFlow<com.plywoodpocket.crm.models.Role?> = _selectedRole

    private val _isLoadingRoles = MutableStateFlow(false)
    val isLoadingRoles: StateFlow<Boolean> = _isLoadingRoles

    private val _rolesError = MutableStateFlow<String?>(null)
    val rolesError: StateFlow<String?> = _rolesError

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        tokenManager.saveAuthData(
                            loginResponse.token,
                            loginResponse.user.id,
                            loginResponse.user.name,
                            loginResponse.user.email,
                            loginResponse.user.role.name
                        )
                        _authState.value = AuthState.Success("Login successful")
                    }
                } else {
                    _authState.value = AuthState.Error("Login failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login failed: ${e.message}")
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
                val response = apiService.register(
                    RegisterRequest(
                        name = name,
                        email = email,
                        password = password,
                        password_confirmation = confirmPassword,
                        role_id = selectedRole,
                        brand_passkey = brandPasskey
                    )
                )
                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        tokenManager.saveAuthData(
                            loginResponse.token,
                            loginResponse.user.id,
                            loginResponse.user.name,
                            loginResponse.user.email,
                            loginResponse.user.role.name
                        )
                        _authState.value = AuthState.Success("Registration successful")
                    }
                } else {
                    _authState.value = AuthState.Error("Registration failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Registration failed: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.logout()
                if (response.isSuccessful) {
                    tokenManager.clearAuthData()
                    _authState.value = AuthState.Success("Logout successful")
                } else {
                    _authState.value = AuthState.Error("Logout failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Logout failed: ${e.message}")
            }
        }
    }

    fun fetchRoles() {
        viewModelScope.launch {
            _isLoadingRoles.value = true
            _rolesError.value = null
            try {
                val response = apiService.getRoles()
                if (response.isSuccessful) {
                    response.body()?.let { rolesList ->
                        _roles.value = rolesList
                    }
                } else {
                    _rolesError.value = "Failed to fetch roles: ${response.message()}"
                }
            } catch (e: Exception) {
                _rolesError.value = "Failed to fetch roles: ${e.message}"
            } finally {
                _isLoadingRoles.value = false
            }
        }
    }

    fun setSelectedRole(roleId: Int) {
        _selectedRole.value = _roles.value.find { it.id == roleId }
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
} 