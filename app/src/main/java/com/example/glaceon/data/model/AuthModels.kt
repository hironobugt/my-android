package com.example.glaceon.data.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val givenName: String? = null,
    val familyName: String? = null
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val idToken: String,
    val expiresIn: Long
)

data class User(
    val userId: String,
    val username: String,
    val email: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val emailVerified: Boolean = false
)