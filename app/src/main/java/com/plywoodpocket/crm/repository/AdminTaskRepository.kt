package com.plywoodpocket.crm.repository

import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.models.TaskListResponse
import com.plywoodpocket.crm.models.TaskResponse
import retrofit2.Response

class AdminTaskRepository(private val apiService: ApiService) {
    suspend fun getTasks(status: String? = null, type: String? = null, assignee: Int? = null, search: String? = null, page: Int? = null): Response<TaskListResponse> {
        return apiService.getAdminTasks(status, type, assignee, search, page)
    }
    suspend fun createTask(task: Map<String, Any?>): Response<TaskResponse> {
        return apiService.createAdminTask(task)
    }
    suspend fun getTask(taskId: Int): Response<TaskResponse> {
        return apiService.getAdminTask(taskId)
    }
    suspend fun updateTaskStatus(taskId: Int, status: String): Response<TaskResponse> {
        return apiService.updateAdminTaskStatus(taskId, mapOf("status" to status))
    }
    suspend fun updateTask(taskId: Int, task: Map<String, Any?>): Response<TaskResponse> {
        return apiService.updateAdminTask(taskId, task)
    }
    suspend fun deleteTask(taskId: Int): Response<TaskResponse> {
        return apiService.deleteAdminTask(taskId)
    }
} 