package com.plywoodpocket.crm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.repository.AdminTaskRepository

class AdminTaskViewModelFactory(
    private val apiService: ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminTaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminTaskViewModel(AdminTaskRepository(apiService)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 