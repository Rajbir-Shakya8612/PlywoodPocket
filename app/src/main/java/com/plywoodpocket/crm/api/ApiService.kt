package com.plywoodpocket.crm.api

import com.plywoodpocket.crm.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @Headers("Accept: application/json")
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Headers("Accept: application/json")
    @POST("api/logout")
    suspend fun logout(): Response<Unit>

    @Headers("Accept: application/json")
    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @Headers("Accept: application/json")
    @GET("api/roles")
    suspend fun getRoles(): Response<List<Role>>
} 