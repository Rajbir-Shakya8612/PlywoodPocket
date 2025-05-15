package com.plywoodpocket.crm.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.*

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_TOKEN_EXPIRATION = "token_expiration"
    }

    // Save the auth data along with token expiration timestamp
    fun saveAuthData(token: String, userId: Int, userName: String, userEmail: String, userRole: String, tokenExpiration: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_ROLE, userRole)
            putLong(KEY_TOKEN_EXPIRATION, tokenExpiration)  // Save expiration time
            apply()
        }
    }

    // Retrieve the token from shared preferences
    fun getToken(): String? {
        val token = sharedPreferences.getString(KEY_TOKEN, null)
        val expiration = sharedPreferences.getLong(KEY_TOKEN_EXPIRATION, 0)
        
        Log.d("TokenManager", "Getting token - exists: ${token != null}")
        
        // If token is expired, clear it and return null
        if (token != null && System.currentTimeMillis() > expiration) {
            Log.d("TokenManager", "Token expired, clearing auth data")
            clearAuthData()
            return null
        }
        
        if (token != null) {
            Log.d("TokenManager", "Valid token retrieved")
        } else {
            Log.d("TokenManager", "No token found")
        }
        
        return token
    }

    // Retrieve user data from shared preferences
    fun getUserData(): UserData? {
        val userId = sharedPreferences.getInt(KEY_USER_ID, -1)
        val userName = sharedPreferences.getString(KEY_USER_NAME, null)
        val userEmail = sharedPreferences.getString(KEY_USER_EMAIL, null)
        val userRole = sharedPreferences.getString(KEY_USER_ROLE, null)
        val expiration = sharedPreferences.getLong(KEY_TOKEN_EXPIRATION, 0)

        // If data is expired, clear it and return null
        if (System.currentTimeMillis() > expiration) {
            clearAuthData()
            return null
        }

        return if (userId != -1 && userName != null && userEmail != null && userRole != null) {
            UserData(userId, userName, userEmail, userRole)
        } else null
    }

    // Clear all auth data from shared preferences
    fun clearAuthData() {
        sharedPreferences.edit().clear().apply()
    }

    // Check if the user is logged in and if the token is still valid
    fun isLoggedIn(): Boolean {
        val token = getToken()
        val expiration = sharedPreferences.getLong(KEY_TOKEN_EXPIRATION, 0)
        val currentTime = System.currentTimeMillis()

        // If token exists and is not expired
        return token != null && expiration > currentTime
    }

    // Add this function to get the current user id
    fun getUserId(): Int? {
        val userId = sharedPreferences.getInt(KEY_USER_ID, -1)
        val expiration = sharedPreferences.getLong(KEY_TOKEN_EXPIRATION, 0)
        if (System.currentTimeMillis() > expiration) {
            clearAuthData()
            return null
        }
        return if (userId != -1) userId else null
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun getUserRole(): String? {
        return sharedPreferences.getString(KEY_USER_ROLE, null)
    }
}

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val role: String
)