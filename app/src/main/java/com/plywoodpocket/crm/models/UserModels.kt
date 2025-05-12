package com.plywoodpocket.crm.models

data class UserProfile(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val whatsapp_number: String?,
    val pincode: String?,
    val address: String?,
    val location: String?,
    val designation: String?,
    val date_of_joining: String?,
    val target_amount: Double?,
    val target_leads: Int?,
    val role: Role,
    val is_active: Boolean,
    val photo: String? = null,
    val status: String? = null,
    val brand_id: Int? = null
)

data class UserProfileResponse(
    val success: Boolean,
    val user: UserProfile
)

data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val whatsapp_number: String? = null,
    val pincode: String? = null,
    val address: String? = null,
    val location: String? = null,
    val designation: String? = null,
    val date_of_joining: String? = null,
    val target_amount: Double? = null,
    val target_leads: Int? = null,
    val status: String? = null,
    val role_id: Int? = null
) {
    companion object {
        fun fromUserProfile(profile: UserProfile): UpdateUserRequest {
            return UpdateUserRequest(
                name = profile.name,
                email = profile.email,
                phone = profile.phone,
                whatsapp_number = profile.whatsapp_number,
                pincode = profile.pincode,
                address = profile.address,
                location = profile.location,
                designation = profile.designation,
                date_of_joining = profile.date_of_joining,
                target_amount = profile.target_amount,
                target_leads = profile.target_leads,
                status = profile.status,
                role_id = profile.role.id
            )
        }
    }
} 