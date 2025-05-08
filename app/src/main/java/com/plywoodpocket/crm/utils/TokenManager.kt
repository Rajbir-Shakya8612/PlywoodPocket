package com.plywoodpocket.crm.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
    }

    fun saveAuthData(token: String, userId: Int, userName: String, userEmail: String, userRole: String) {
        sharedPreferences.edit().apply {
            putString(KEY_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_ROLE, userRole)
            apply()
        }
    }

    fun getToken(): String? = sharedPreferences.getString(KEY_TOKEN, null)

    fun getUserData(): UserData? {
        val userId = sharedPreferences.getInt(KEY_USER_ID, -1)
        val userName = sharedPreferences.getString(KEY_USER_NAME, null)
        val userEmail = sharedPreferences.getString(KEY_USER_EMAIL, null)
        val userRole = sharedPreferences.getString(KEY_USER_ROLE, null)

        return if (userId != -1 && userName != null && userEmail != null && userRole != null) {
            UserData(userId, userName, userEmail, userRole)
        } else null
    }

    fun clearAuthData() {
        sharedPreferences.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null
}

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val role: String
) 