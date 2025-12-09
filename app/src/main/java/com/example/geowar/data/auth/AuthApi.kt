package com.example.geowar.data.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/login")
    suspend fun login(@Body request: AuthRequest): LoginResponse

    @POST("/register")
    suspend fun register(@Body request: AuthRequest): RegisterResponse
}
