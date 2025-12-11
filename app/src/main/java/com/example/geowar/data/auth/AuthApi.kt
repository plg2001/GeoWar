package com.example.geowar.data.auth

import com.example.geowar.ui.auth.GoogleLoginRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("/auth/google_login")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): LoginResponse

    @POST("/set_team")
    suspend fun setTeam(@Body request: SetTeamRequest): GenericResponse
}
