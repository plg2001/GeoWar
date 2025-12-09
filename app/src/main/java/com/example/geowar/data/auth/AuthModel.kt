package com.example.geowar.data.auth

data class AuthRequest(
    val username: String,
    val password: String
)

data class UserResponse(
    val id: Int,
    val username: String
)

data class LoginResponse(
    val message: String,
    val user: UserResponse,
    val token: String
)

data class RegisterResponse(
    val message: String,
    val user: UserResponse
)
