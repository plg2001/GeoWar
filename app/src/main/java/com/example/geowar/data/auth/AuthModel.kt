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

data class GoogleLoginRequest(
    val token: String,
    val email: String,
    val name: String
)

data class SetTeamRequest(
    val user_id: Int,
    val team: String
)

data class UserResponse(
    val id: Int,
    val username: String,
    val admin: Boolean = false,
    val team: String? = null,
    val score: Int = 0,
    val avatar_seed: String? = null
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

// NUOVI MODELLI PER L'ADMIN
data class AdminUserListResponse(
    val users: List<UserResponse>
)

data class BanUserRequest(
    val user_id: Int
)

data class CreateTargetRequest(
    val name: String,
    val lat: Double,
    val lon: Double,
    val owner_team: String = "NEUTRAL"
)

data class TargetResponse(
    val id: Int,
    val name: String,
    val lat: Double,
    val lon: Double,
    val owner: String
)

// NUOVO: Richiesta aggiornamento posizione
data class UpdatePositionRequest(
    val user_id: Int,
    val lat: Double,
    val lon: Double
)
