package com.example.glaceon.data.model

import com.google.gson.annotations.SerializedName

// Auth Request models
data class AuthRequest(
    val action: String,
    val username: String? = null,
    val password: String? = null,
    val email: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val confirmationCode: String? = null,
    val newPassword: String? = null
)

// Auth Response models
data class AuthResponse(
    val message: String? = null,
    val error: String? = null,
    val token: String? = null,
    val user: UserData? = null,
    // Registration specific fields
    val userId: String? = null,
    val username: String? = null,
    val email: String? = null,
    val confirmationRequired: Boolean? = null,
    val confirmed: Boolean? = null,
    val passwordReset: Boolean? = null,
    val nextStep: String? = null
) {
    // Helper property to determine if the response is successful
    val success: Boolean
        get() = error == null
}

data class UserData(
    val userId: String,
    val username: String,
    val email: String,
    val emailVerified: Boolean = false
)

// User model for UI layer
data class User(
    val userId: String,
    val username: String,
    val email: String,
    val emailVerified: Boolean = false,
    val givenName: String? = null,
    val familyName: String? = null
)