package com.example.geowar.data.auth

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path

interface AuthApi {

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("/auth/google_login") // Assumiamo che il path sia questo
    suspend fun googleLogin(@Body request: GoogleLoginRequest): LoginResponse
    
    @POST("/set_team")
    suspend fun setTeam(@Body request: SetTeamRequest): GenericResponse

    // --- ADMIN API ---
    @GET("/admin/users")
    suspend fun getAllUsers(): List<UserResponse>

    @POST("/admin/ban_user")
    suspend fun banUser(@Body request: BanUserRequest): GenericResponse

    @GET("/targets")
    suspend fun getTargets(): List<TargetResponse>

    @POST("/admin/create_target")
    suspend fun createTarget(@Body request: CreateTargetRequest): GenericResponse
    
    @DELETE("/admin/delete_target/{id}")
    suspend fun deleteTarget(@Path("id") id: Int): GenericResponse
}
