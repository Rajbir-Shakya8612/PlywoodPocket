package com.plywoodpocket.crm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.repository.TaskRepository

class TaskViewModelFactory(
    private val apiService: ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(TaskRepository(apiService)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 