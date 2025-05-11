package com.plywoodpocket.crm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.plywoodpocket.crm.utils.TokenManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

sealed class LeadsUiState {
    object Loading : LeadsUiState()
    data class Success(val leads: List<Lead>, val statuses: List<LeadStatus>) : LeadsUiState()
    data class Error(val message: String) : LeadsUiState()
}

class LeadsViewModel(app: Application, tokenManager: TokenManager) : AndroidViewModel(app) {
    private val api: ApiService = ApiClient(tokenManager).apiService
    private val _uiState = MutableStateFlow<LeadsUiState>(LeadsUiState.Loading)
    val uiState: StateFlow<LeadsUiState> = _uiState

    var selectedStatusId: Int? = null
        private set

    fun loadLeads(statusId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = LeadsUiState.Loading
            try {
                val response = if (statusId == null) api.getLeads() else api.getLeadsByStatus(statusId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _uiState.value = LeadsUiState.Success(
                            leads = body.leads,
                            statuses = body.lead_statuses
                        )
                    } else {
                        _uiState.value = LeadsUiState.Error("No data found")
                    }
                } else {
                    _uiState.value = LeadsUiState.Error(response.message())
                }
            } catch (e: Exception) {
                _uiState.value = LeadsUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun createLead(request: LeadRequest, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.createLead(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, response.body()?.message ?: "Lead created")
                    loadLeads(selectedStatusId)
                } else {
                    onResult(false, response.body()?.message ?: response.message())
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun updateLead(leadId: Int, request: LeadRequest, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.updateLead(leadId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, response.body()?.message ?: "Lead updated")
                    loadLeads(selectedStatusId)
                } else {
                    onResult(false, response.body()?.message ?: response.message())
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun deleteLead(leadId: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteLead(leadId)
                if (response.isSuccessful) {
                    onResult(true, "Lead deleted")
                    loadLeads(selectedStatusId)
                } else {
                    onResult(false, response.message())
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun selectStatus(statusId: Int?) {
        selectedStatusId = statusId
        loadLeads(statusId)
    }
}

class LeadsViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeadsViewModel::class.java)) {
            val tokenManager = TokenManager(app.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return LeadsViewModel(app, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 