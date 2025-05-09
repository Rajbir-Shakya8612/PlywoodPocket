package com.plywoodpocket.crm.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.plywoodpocket.crm.api.ApiClient
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.utils.TokenManager


class AttendanceViewModelFactory(
    private val app: Application,
    private val tokenManager: TokenManager,
    private val api: ApiService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AttendanceViewModel::class.java) -> {
                AttendanceViewModel(app, tokenManager, api) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
