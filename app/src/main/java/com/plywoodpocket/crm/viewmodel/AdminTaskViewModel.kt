package com.plywoodpocket.crm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.models.Task
import com.plywoodpocket.crm.repository.AdminTaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AdminTaskViewModel(private val repository: AdminTaskRepository) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _taskUpdateSuccess = MutableStateFlow(false)
    val taskUpdateSuccess: StateFlow<Boolean> = _taskUpdateSuccess

    fun fetchTasks(status: String? = null, type: String? = null, assignee: Int? = null, search: String? = null, page: Int? = null) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getTasks(status, type, assignee, search, page)
                if (response.isSuccessful) {
                    _tasks.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = "Failed to fetch tasks"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
            _loading.value = false
        }
    }

    fun createTask(task: Map<String, Any?>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = repository.createTask(task)
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchTasks()
                    _taskUpdateSuccess.value = true
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string()?.let { parseErrorMessage(it) }
                    onError(errorMsg ?: response.body()?.message ?: "Failed to create task")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            }
            _loading.value = false
        }
    }

    fun updateTaskStatus(taskId: Int, status: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = repository.updateTaskStatus(taskId, status)
                if (response.isSuccessful && response.body()?.task != null) {
                    fetchTasks()
                    onSuccess()
                } else {
                    onError(response.body()?.message ?: "Failed to update status")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            }
            _loading.value = false
        }
    }

    fun updateTask(taskId: Int, task: Map<String, Any?>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = repository.updateTask(taskId, task)
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchTasks()
                    _taskUpdateSuccess.value = true
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string()?.let { parseErrorMessage(it) }
                    onError(errorMsg ?: response.body()?.message ?: "Failed to update task")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            }
            _loading.value = false
        }
    }

    fun deleteTask(taskId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = repository.deleteTask(taskId)
                if (response.isSuccessful) {
                    fetchTasks()
                    onSuccess()
                } else {
                    onError(response.body()?.message ?: "Failed to delete task")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            }
            _loading.value = false
        }
    }

    fun resetTaskUpdateSuccess() {
        _taskUpdateSuccess.value = false
    }

    private fun parseErrorMessage(json: String): String? {
        return try {
            val jsonObj = JSONObject(json)
            jsonObj.optString("message")
        } catch (e: Exception) {
            null
        }
    }
} 