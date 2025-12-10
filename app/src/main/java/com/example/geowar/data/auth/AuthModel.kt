package com.example.geowar.data.auth

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String
)

data class SetTeamRequest(
    val user_id: Int,
    val team: String
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

data class GenericResponse(
    val message: String,
    val success: Boolean
)



