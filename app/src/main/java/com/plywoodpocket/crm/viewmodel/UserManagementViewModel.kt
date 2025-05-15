package com.plywoodpocket.crm.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.*
import com.plywoodpocket.crm.models.Role
import com.plywoodpocket.crm.repository.UserManagementRepository
import com.plywoodpocket.crm.utils.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class UserManagementViewModel(context: Context) : ViewModel() {
    
    private val tokenManager = TokenManager(context)
    private val apiService = ApiClient(tokenManager).apiService
    private val repository = UserManagementRepository(apiService)
    
    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()
    
    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users: StateFlow<List<UserProfile>> = _users.asStateFlow()
    
    private val _roles = MutableStateFlow<List<Role>>(emptyList())
    val roles: StateFlow<List<Role>> = _roles.asStateFlow()
    
    private val _selectedUser = MutableStateFlow<UserProfile?>(null)
    val selectedUser: StateFlow<UserProfile?> = _selectedUser.asStateFlow()
    
    private var currentJob: Job? = null
    
    init {
        Log.d("UserManagementVM", "Initializing UserManagementViewModel")
        val token = tokenManager.getToken()
        if (token == null) {
            Log.e("UserManagementVM", "No valid authentication token found")
            _uiState.value = _uiState.value.copy(error = "Authentication error. Please log in again.")
        } else {
            Log.d("UserManagementVM", "Valid token found, loading data")
            loadUsers()
            loadRoles()
        }
    }
    
    fun loadUsers() {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            Log.d("UserManagementVM", "Loading users...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAllUsers().fold(
                onSuccess = { usersList ->
                    Log.d("UserManagementVM", "Users loaded successfully: ${usersList.size} users")
                    _users.value = usersList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    Log.e("UserManagementVM", "Error loading users: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
            )
        }
    }
    
    fun loadRoles() {
        viewModelScope.launch {
            Log.d("UserManagementVM", "Loading roles...")
            try {
                repository.getAllRoles().fold(
                    onSuccess = { rolesList ->
                        Log.d("UserManagementVM", "Roles loaded successfully: ${rolesList.size} roles")
                        _roles.value = rolesList
                    },
                    onFailure = { error ->
                        Log.e("UserManagementVM", "Error loading roles: ${error.message}", error)
                        // Don't set UI error for roles failure, just log it
                        // We'll use default roles from the user objects instead
                    }
                )
            } catch (e: Exception) {
                Log.e("UserManagementVM", "Exception loading roles", e)
                // Don't set UI error for roles failure, just log it
            }
        }
    }
    
    fun createUser(request: CreateUserRequest) {
        if (!isValidUserRequest(request)) {
            _uiState.value = _uiState.value.copy(error = "Please fill all required fields correctly")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.createUser(request).fold(
                onSuccess = { user ->
                    _users.value = _users.value + user
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "User created successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create user"
                    )
                }
            )
        }
    }
    
    fun selectUser(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getUserById(userId).fold(
                onSuccess = { user ->
                    _selectedUser.value = user
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load user details"
                    )
                }
            )
        }
    }
    
    fun updateUser(id: Int, request: UpdateUserPasswordRequest) {
        if (!isValidUpdateRequest(request)) {
            _uiState.value = _uiState.value.copy(error = "Please fill all required fields correctly")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.updateUser(id, request).fold(
                onSuccess = { updatedUser ->
                    _users.value = _users.value.map { 
                        if (it.id == id) updatedUser else it 
                    }
                    _selectedUser.value = updatedUser
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "User updated successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update user"
                    )
                }
            )
        }
    }
    
    fun deleteUser(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.deleteUser(id).fold(
                onSuccess = { message ->
                    _users.value = _users.value.filter { it.id != id }
                    _selectedUser.value = null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete user"
                    )
                }
            )
        }
    }
    
    fun toggleUserStatus(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.toggleUserStatus(id, isActive).fold(
                onSuccess = { updatedUser ->
                    _users.value = _users.value.map { 
                        if (it.id == id) updatedUser else it 
                    }
                    _selectedUser.value = if (_selectedUser.value?.id == id) updatedUser else _selectedUser.value
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "User status updated successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update user status"
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    fun clearSelectedUser() {
        _selectedUser.value = null
    }
    
    private fun isValidUserRequest(request: CreateUserRequest): Boolean {
        return request.name.isNotBlank() && 
               isValidEmail(request.email) && 
               request.password.length >= 6 &&
               request.role_id > 0
    }
    
    private fun isValidUpdateRequest(request: UpdateUserPasswordRequest): Boolean {
        return request.name.isNotBlank() && 
               isValidEmail(request.email) && 
               request.role_id > 0 &&
               (request.password == null || request.password.length >= 6)
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }
}

class UserManagementViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserManagementViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 