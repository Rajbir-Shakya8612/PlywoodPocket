package com.plywoodpocket.crm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.models.UserProfile
import com.plywoodpocket.crm.models.UpdateUserRequest
import com.plywoodpocket.crm.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = ApiClient(tokenManager).apiService

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val response = api.getUserProfile()
                if (response.isSuccessful) {
                    response.body()?.let {
                        _uiState.value = ProfileUiState.Success(it)
                    } ?: run {
                        _uiState.value = ProfileUiState.Error("Failed to load profile")
                    }
                } else {
                    _uiState.value = ProfileUiState.Error(response.message())
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun updateProfile(updateRequest: UpdateUserRequest) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                // Get current user profile to get the user id
                val profileResponse = api.getUserProfile()
                if (profileResponse.isSuccessful) {
                    val user = profileResponse.body()
                    if (user != null) {
                        val response = api.updateUser(user.id, updateRequest)
                        if (response.isSuccessful) {
                            response.body()?.let {
                                _uiState.value = ProfileUiState.Success(it.user)
                            } ?: run {
                                _uiState.value = ProfileUiState.Error("Failed to update profile")
                            }
                        } else {
                            _uiState.value = ProfileUiState.Error(response.message())
                        }
                    } else {
                        _uiState.value = ProfileUiState.Error("User not found")
                    }
                } else {
                    _uiState.value = ProfileUiState.Error(profileResponse.message())
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.logout()
                if (response.isSuccessful) {
                    tokenManager.clearAuthData()
                    onSuccess()
                } else {
                    _uiState.value = ProfileUiState.Error("Logout failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Logout failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
}