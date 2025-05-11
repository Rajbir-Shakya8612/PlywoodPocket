package com.plywoodpocket.crm.api


import com.plywoodpocket.crm.models.LoginResponse
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
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @Headers("Accept: application/json")
    @GET("api/roles")
    suspend fun getRoles(): Response<List<Role>>

    @GET("api/salesperson/attendance/status")
    suspend fun getAttendanceStatus(): Response<AttendanceStatusResponse>

    @Headers("Accept: application/json")
    @POST("api/attendance/checkin")
    suspend fun checkIn(@Body request: CheckInRequest): Response<Any>

    @Headers("Accept: application/json")
    @POST("api/attendance/checkout")
    suspend fun checkOut(@Body request: CheckOutRequest): Response<Any>

    @Headers("Accept: application/json")
    @POST("api/location/tracks")
    suspend fun trackLocation(@Body locationData: LocationData): Response<Any>


} 