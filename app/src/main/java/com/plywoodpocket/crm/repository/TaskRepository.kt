package com.plywoodpocket.crm.repository

import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.models.TaskListResponse
import com.plywoodpocket.crm.models.TaskResponse
import retrofit2.Response

class TaskRepository(private val apiService: ApiService) {
    suspend fun getTasks(status: String? = null, type: String? = null, search: String? = null, page: Int? = null): Response<TaskListResponse> {
        return apiService.getTasks(status, type, search, page)
    }

    suspend fun createTask(task: Map<String, Any?>): Response<TaskResponse> {
        return apiService.createTask(task)
    }

    suspend fun getTask(taskId: Int): Response<TaskResponse> {
        return apiService.getTask(taskId)
    }

    suspend fun updateTaskStatus(taskId: Int, status: String): Response<TaskResponse> {
        return apiService.updateTaskStatus(taskId, mapOf("status" to status))
    }

    suspend fun updateTask(taskId: Int, task: Map<String, Any?>): Response<TaskResponse> {
        return apiService.updateTask(taskId, task)
    }
} 