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

    @Headers("Accept: application/json")
    @GET("api/users/profile")
    suspend fun getUserProfile(): Response<UserProfile>

    @Headers("Accept: application/json")
    @GET("api/users/{user}")
    suspend fun getUser(@Path("user") userId: Int): Response<UserProfileResponse>

    @Headers("Accept: application/json")
    @PUT("api/users/{user}")
    suspend fun updateUser(
        @Path("user") userId: Int,
        @Body request: UpdateUserRequest
    ): Response<UserProfileResponse>

    // Leads API
    @Headers("Accept: application/json")
    @GET("api/salesperson/leads")
    suspend fun getLeads(): Response<LeadsResponse>

    @Headers("Accept: application/json")
    @POST("api/salesperson/leads")
    suspend fun createLead(@Body request: LeadRequest): Response<LeadResponse>


    @Headers("Accept: application/json")
    @GET("api/salesperson/leads/follow-ups")
    suspend fun getFollowUps(): Response<FollowUpsResponse>

    @Headers("Accept: application/json")
    @GET("api/salesperson/leads/{lead}")
    suspend fun getLead(@Path("lead") leadId: Int): Response<LeadResponse>

    @Headers("Accept: application/json")
    @PUT("api/salesperson/leads/{lead}")
    suspend fun updateLead(@Path("lead") leadId: Int, @Body request: LeadRequest): Response<LeadResponse>

    @Headers("Accept: application/json")
    @DELETE("api/salesperson/leads/{lead}")
    suspend fun deleteLead(@Path("lead") leadId: Int): Response<Any>

    @Headers("Accept: application/json")
    @PUT("api/salesperson/leads/{lead}/status")
    suspend fun updateLeadStatus(@Path("lead") leadId: Int, @Body request: UpdateStatusRequest): Response<LeadResponse>

    @Headers("Accept: application/json")
    @GET("api/salesperson/leads/status/{status}")
    suspend fun getLeadsByStatus(@Path("status") status: Int): Response<LeadsResponse>

    @Headers("Accept: application/json")
    @POST("api/salesperson/leads/{lead}/follow-up")
    suspend fun scheduleFollowUp(@Path("lead") leadId: Int, @Body request: FollowUpRequest): Response<Any>

    @Headers("Accept: application/json")
    @GET("api/salesperson/leads/stats")
    suspend fun getLeadStats(): Response<LeadStatsResponse>

    @Headers("Accept: application/json")
    @GET("api/salesperson/leads/{lead}/follow-up-history")
    suspend fun getFollowUpHistory(@Path("lead") leadId: Int): Response<FollowUpHistoryResponse>

    @Headers("Accept: application/json")
    @POST("api/salesperson/leads/{lead}/complete-follow-up")
    suspend fun completeFollowUp(@Path("lead") leadId: Int, @Body request: CompleteFollowUpRequest): Response<Any>


} 