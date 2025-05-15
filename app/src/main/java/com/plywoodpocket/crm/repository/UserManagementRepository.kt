package com.plywoodpocket.crm.repository

import android.util.Log
import com.plywoodpocket.crm.models.*
import com.plywoodpocket.crm.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserManagementRepository(
    private val apiService: ApiService
) {
    
    suspend fun getAllUsers(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("UserManagementRepo", "Making API call to get all users")
            val response = apiService.getAdminUsers()
            Log.d("UserManagementRepo", "API response received: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d("UserManagementRepo", "Users fetched successfully: ${body.size} users")
                    Result.success(body)
                } else {
                    Log.e("UserManagementRepo", "Response body is null")
                    Result.failure(Exception("Response body is null"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Unauthorized access"
                    403 -> "Forbidden access"
                    404 -> "Users not found"
                    500 -> "Server error"
                    else -> "Failed to fetch users: ${response.code()}"
                }
                Log.e("UserManagementRepo", "Error fetching users: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("UserManagementRepo", "Network error: ${e.localizedMessage}", e)
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
    
    suspend fun createUser(request: CreateUserRequest): Result<UserProfile> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (request.name.isBlank() || request.email.isBlank() || request.password.length < 6) {
                return@withContext Result.failure(Exception("Invalid user data"))
            }
            
            val response = apiService.createAdminUser(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.user != null) {
                    Result.success(body.user)
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to create user"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Invalid user data"
                    409 -> "User already exists"
                    422 -> "Validation failed"
                    else -> "Failed to create user: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
    
    suspend fun getUserById(id: Int): Result<UserProfile> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (id <= 0) {
                return@withContext Result.failure(Exception("Invalid user ID"))
            }
            
            val response = apiService.getAdminUser(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.user != null) {
                    Result.success(body.user)
                } else {
                    Result.failure(Exception("User not found"))
                }
            } else {
                Result.failure(Exception("Failed to fetch user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
    
    suspend fun updateUser(id: Int, request: UpdateUserPasswordRequest): Result<UserProfile> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (id <= 0 || request.name.isBlank() || request.email.isBlank()) {
                return@withContext Result.failure(Exception("Invalid update data"))
            }
            
            val response = apiService.updateAdminUser(id, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.user != null) {
                    Result.success(body.user)
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to update user"))
                }
            } else {
                Result.failure(Exception("Failed to update user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
    
    suspend fun deleteUser(id: Int): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (id <= 0) {
                return@withContext Result.failure(Exception("Invalid user ID"))
            }
            
            val response = apiService.deleteAdminUser(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(body.message)
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to delete user"))
                }
            } else {
                Result.failure(Exception("Failed to delete user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
    
    suspend fun toggleUserStatus(id: Int, isActive: Boolean): Result<UserProfile> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (id <= 0) {
                return@withContext Result.failure(Exception("Invalid user ID"))
            }
            
            val response = apiService.toggleAdminUserStatus(id, ToggleStatusRequest(isActive))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.user != null) {
                    Result.success(body.user)
                } else {
                    Result.failure(Exception(body?.message ?: "Failed to update status"))
                }
            } else {
                Result.failure(Exception("Failed to update status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
    
    suspend fun getAllRoles(): Result<List<Role>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("UserManagementRepo", "Making API call to get all roles")
            val response = apiService.getAdminRoles()
            Log.d("UserManagementRepo", "API response received: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d("UserManagementRepo", "Roles fetched successfully: ${body.size} roles")
                    Result.success(body)
                } else {
                    Log.e("UserManagementRepo", "Failed to fetch roles: Response body is null")
                    Result.failure(Exception("Failed to fetch roles"))
                }
            } else {
                Log.e("UserManagementRepo", "Failed to fetch roles: ${response.code()}")
                Result.failure(Exception("Failed to fetch roles: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("UserManagementRepo", "Network error: ${e.localizedMessage}", e)
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
} 