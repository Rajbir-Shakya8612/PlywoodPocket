package com.plywoodpocket.crm.models

data class BannerResponse(
    val banners: List<Banner>
)

data class Banner(
    val id: Int,
    val title: String?,
    val image: String?,
    val link: String?,
    val is_active: Boolean?,
    val order: Int?,
    val created_at: String?,
    val updated_at: String?,
    val image_url: String
) 