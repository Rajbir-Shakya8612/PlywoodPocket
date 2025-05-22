package com.plywoodpocket.crm.models

// Task data model for CRUD operations

data class Task(
    val id: Int,
    val title: String,
    val description: String?,
    val type: String,
    val status: String,
    val priority: String?,
    val due_date: String,
    val assignee_id: Int,
    val assignee: UserProfile? = null,
    val completed_at: String? = null
)

data class TaskResponse(
    val success: Boolean,
    val message: String?,
    val task: Task?
)

data class TaskListResponse(
    val data: List<Task>,
    val current_page: Int,
    val last_page: Int,
    val total: Int
) 