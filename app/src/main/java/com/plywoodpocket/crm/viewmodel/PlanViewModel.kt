package com.plywoodpocket.crm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.models.Plan
import com.plywoodpocket.crm.models.PlanRequest
import com.plywoodpocket.crm.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlanViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _currentPlan = MutableStateFlow<Plan?>(null)
    val currentPlan: StateFlow<Plan?> = _currentPlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadPlans(type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getPlans(type)
                if (response.isSuccessful) {
                    val plans = response.body()?.data?.plans ?: emptyList()
                    _plans.value = plans
                    _error.value = null
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to load plans"
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                _error.value = if (e.message?.contains("BEGIN_OBJECT") == true) {
                    "Session expired or server error. Please login again."
                } else {
                    e.message
                }
            }
            _isLoading.value = false
        }
    }

    fun createPlan(plan: PlanRequest, type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.createPlan(plan)
                if (response.isSuccessful) {
                    val createdPlan = response.body()?.data?.plans?.firstOrNull()
                        ?: response.body()?.plan
                    if (createdPlan != null) {
                        _currentPlan.value = createdPlan
                        _successMessage.value = "Plan created successfully!"
                        loadPlans(type)
                    } else {
                        _error.value = "Plan created but not returned by server"
                        _successMessage.value = null
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to create plan"
                    _error.value = errorMsg
                    _successMessage.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message
                _successMessage.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}

class PlanViewModelFactory(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlanViewModel(apiService, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 