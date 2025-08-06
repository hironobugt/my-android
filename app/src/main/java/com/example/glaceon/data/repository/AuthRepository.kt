package com.example.glaceon.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.glaceon.data.model.User
import com.example.glaceon.data.model.AuthRequest
import com.example.glaceon.data.model.AuthResponse
import com.example.glaceon.data.model.UserData
import com.example.glaceon.data.api.GlaceonApiService
import com.example.glaceon.config.AppConfig
import kotlinx.coroutines.delay

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences =
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val apiService = GlaceonApiService.create()

    suspend fun signUp(
            username: String,
            password: String,
            email: String,
            givenName: String? = null,
            familyName: String? = null
    ): Result<String> {
        return try {
            val request = AuthRequest(
                action = "register",
                username = username,
                password = password,
                email = email,
                givenName = givenName,
                familyName = familyName
            )
            
            val response = apiService.auth(request)
            
            if (response.success) {
                // Save pending confirmation
                prefs.edit().putString("pending_confirmation", username).apply()
                Result.success(response.message ?: "Registration successful. Please check your email for verification code.")
            } else {
                Result.failure(Exception(response.error ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmSignUp(username: String, confirmationCode: String): Result<String> {
        return try {
            val request = AuthRequest(
                action = "confirm",
                username = username,
                confirmationCode = confirmationCode
            )
            
            val response = apiService.auth(request)
            
            if (response.success) {
                // Clear pending confirmation
                prefs.edit().remove("pending_confirmation").apply()
                Result.success(response.message ?: "Account confirmed successfully")
            } else {
                Result.failure(Exception(response.error ?: "Confirmation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendConfirmationCode(username: String): Result<String> {
        return try {
            val request = AuthRequest(
                action = "resend",
                username = username
            )
            
            val response = apiService.auth(request)
            
            if (response.success) {
                Result.success(response.message ?: "Verification code resent. Please check your email.")
            } else {
                Result.failure(Exception(response.error ?: "Failed to resend verification code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(username: String): Result<String> {
        return try {
            val request = AuthRequest(
                action = "forgot-password",
                username = username
            )
            
            val response = apiService.auth(request)
            
            if (response.success) {
                // Save pending password reset
                prefs.edit().putString("pending_password_reset", username).apply()
                Result.success(response.message ?: "Password reset code sent to your email. Please check your inbox.")
            } else {
                Result.failure(Exception(response.error ?: "Failed to send password reset code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(username: String, confirmationCode: String, newPassword: String): Result<String> {
        return try {
            val request = AuthRequest(
                action = "reset-password",
                username = username,
                confirmationCode = confirmationCode,
                newPassword = newPassword
            )
            
            val response = apiService.auth(request)
            
            if (response.success) {
                // Clear pending password reset
                prefs.edit().remove("pending_password_reset").apply()
                Result.success(response.message ?: "Password reset successfully. You can now sign in with your new password.")
            } else {
                Result.failure(Exception(response.error ?: "Password reset failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUpperCase && hasLowerCase && hasDigit
    }

    suspend fun signIn(username: String, password: String): Result<String> {
        return try {
            val request = AuthRequest(
                action = "login",
                username = username,
                password = password
            )
            
            val response = apiService.auth(request)
            
            if (response.success && response.token != null && response.user != null) {
                // Save access token
                saveAccessToken(response.token)
                
                // Save current user info
                prefs.edit().apply {
                    putString("current_user_id", response.user.userId)
                    putString("current_username", response.user.username)
                    putString("current_email", response.user.email)
                    apply()
                }
                
                Result.success(response.token)
            } else {
                Result.failure(Exception(response.error ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            // Simulate network delay
            delay(300)
            
            // Clear all stored data
            clearAccessToken()
            prefs.edit().apply {
                remove("current_user_id")
                remove("current_username")
                remove("current_email")
                apply()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<User?> {
        return try {
            // Simulate network delay
            delay(300)
            
            val token = getAccessToken()
            if (token == null) {
                Result.success(null)
            } else {
                // Get current user info from preferences
                val userId = prefs.getString("current_user_id", null)
                val username = prefs.getString("current_username", null)
                val email = prefs.getString("current_email", null)
                
                if (userId != null && username != null && email != null) {
                    val currentUser = User(
                        userId = userId,
                        username = username,
                        email = email,
                        emailVerified = true
                    )
                    Result.success(currentUser)
                } else {
                    // Token exists but user info is missing, clear token
                    clearAccessToken()
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    private fun saveAccessToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    private fun clearAccessToken() {
        prefs.edit().remove("access_token").apply()
    }

    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }
    
    fun getPendingConfirmation(): String? {
        return prefs.getString("pending_confirmation", null)
    }
    
    fun getPendingPasswordReset(): String? {
        return prefs.getString("pending_password_reset", null)
    }
    
    fun clearPendingConfirmation() {
        prefs.edit().remove("pending_confirmation").apply()
    }
    
    fun clearPendingPasswordReset() {
        prefs.edit().remove("pending_password_reset").apply()
    }
}
