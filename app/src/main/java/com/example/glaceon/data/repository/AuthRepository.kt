package com.example.glaceon.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.glaceon.data.model.User
import kotlinx.coroutines.delay

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences =
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    // Mock user database for development
    private val mockUsers = mutableMapOf<String, MockUser>()
    
    data class MockUser(
        val username: String,
        val email: String,
        val password: String,
        val givenName: String?,
        val familyName: String?,
        val isConfirmed: Boolean = false
    )

    suspend fun signUp(
            username: String,
            password: String,
            email: String,
            givenName: String? = null,
            familyName: String? = null
    ): Result<String> {
        return try {
            // Simulate network delay
            delay(1000)
            
            // Check if user already exists
            if (mockUsers.containsKey(username)) {
                Result.failure(Exception("User already exists"))
            } else {
                // Create mock user
                val mockUser = MockUser(
                    username = username,
                    email = email,
                    password = password,
                    givenName = givenName,
                    familyName = familyName,
                    isConfirmed = false
                )
                mockUsers[username] = mockUser
                
                // Save pending confirmation
                prefs.edit().putString("pending_confirmation", username).apply()
                
                Result.success("Confirmation required")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmSignUp(username: String, confirmationCode: String): Result<String> {
        return try {
            // Simulate network delay
            delay(500)
            
            val pendingUsername = prefs.getString("pending_confirmation", null)
            
            if (pendingUsername != username) {
                Result.failure(Exception("No pending confirmation for this user"))
            } else if (confirmationCode != "123456") { // Mock confirmation code
                Result.failure(Exception("Invalid confirmation code"))
            } else {
                // Confirm the user
                mockUsers[username]?.let { user ->
                    mockUsers[username] = user.copy(isConfirmed = true)
                    prefs.edit().remove("pending_confirmation").apply()
                    Result.success("Account confirmed")
                } ?: Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendConfirmationCode(username: String): Result<String> {
        return try {
            // Simulate network delay
            delay(500)
            
            val pendingUsername = prefs.getString("pending_confirmation", null)
            val user = mockUsers[username]
            
            when {
                user == null -> Result.failure(Exception("User not found"))
                user.isConfirmed -> Result.failure(Exception("User is already confirmed"))
                pendingUsername != username -> Result.failure(Exception("No pending confirmation for this user"))
                else -> {
                    // In real implementation, this would call the API
                    // For mock, we just return success message
                    Result.success("Verification code resent. Please check your email.")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(username: String): Result<String> {
        return try {
            // Simulate network delay
            delay(1000)
            
            val user = mockUsers[username]
            
            when {
                user == null -> Result.failure(Exception("User not found"))
                !user.isConfirmed -> Result.failure(Exception("User account is not confirmed"))
                else -> {
                    // Save pending password reset
                    prefs.edit().putString("pending_password_reset", username).apply()
                    Result.success("Password reset code sent to your email. Please check your inbox.")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(username: String, confirmationCode: String, newPassword: String): Result<String> {
        return try {
            // Simulate network delay
            delay(1000)
            
            val pendingResetUsername = prefs.getString("pending_password_reset", null)
            val user = mockUsers[username]
            
            when {
                user == null -> Result.failure(Exception("User not found"))
                pendingResetUsername != username -> Result.failure(Exception("No pending password reset for this user"))
                confirmationCode != "123456" -> Result.failure(Exception("Invalid verification code")) // Mock code
                newPassword.length < 8 -> Result.failure(Exception("Password must be at least 8 characters long"))
                !isValidPassword(newPassword) -> Result.failure(Exception("Password must contain at least one uppercase letter, one lowercase letter, and one number"))
                else -> {
                    // Update password
                    mockUsers[username] = user.copy(password = newPassword)
                    prefs.edit().remove("pending_password_reset").apply()
                    Result.success("Password reset successfully. You can now sign in with your new password.")
                }
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
            // Simulate network delay
            delay(1000)
            
            val user = mockUsers[username]
            
            when {
                user == null -> Result.failure(Exception("User not found"))
                !user.isConfirmed -> Result.failure(Exception("Account not confirmed"))
                user.password != password -> Result.failure(Exception("Invalid password"))
                else -> {
                    // Generate mock token
                    val mockToken = "mock-jwt-token-${System.currentTimeMillis()}"
                    saveAccessToken(mockToken)
                    
                    // Save current user info
                    prefs.edit().apply {
                        putString("current_user_id", user.username)
                        putString("current_username", user.username)
                        putString("current_email", user.email)
                        apply()
                    }
                    
                    Result.success(mockToken)
                }
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
