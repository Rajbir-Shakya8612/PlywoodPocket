package com.plywoodpocket.crm.models

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: Role
)

data class Role(
    val id: Int,
    val name: String,
    val description: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val role_id: Int,
    val brand_passkey: String
) 