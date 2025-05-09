package com.plywoodpocket.crm.models

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val role_id: Int,
    val passkey: String,
    val is_active: Boolean = true,
    val phone: String? = null,
    val whatsapp_number: String? = null,
    val pincode: String? = null,
    val address: String? = null,
    val location: String? = null,
    val designation: String? = null,
    val date_of_joining: String? = null,
    val target_amount: Double? = null,
    val target_leads: Int? = null
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: Role
)

data class RegisteredUser(
    val id: Int,
    val name: String,
    val email: String,
    val role_id: Int,
    val brand_id: Int,
    val updated_at: String,
    val created_at: String
)


data class RegisterResponse(
    val message: String,
    val user: RegisteredUser,
    val token: String
)

data class Role(
    val id: Int,
    val name: String,
    val role: String,
    val description: String,
    val slug: String
)
