package com.example.geowar.data.auth

import com.example.geowar.models.UserDetails
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("/auth/google_login") // Assumiamo che il path sia questo
    suspend fun googleLogin(@Body request: GoogleLoginRequest): LoginResponse

    @POST("/set_team")
    suspend fun setTeam(@Body request: SetTeamRequest): GenericResponse

    @GET("/user/{user_id}")
    suspend fun getUserDetails(@Path("user_id") userId: Int): Response<UserDetails>

    @PUT("/user/{user_id}")
    suspend fun updateUserDetails(@Path("user_id") userId: Int, @Body userDetails: UserDetails): Response<Unit>

    // --- ADMIN API ---
    @GET("/admin/users")
    suspend fun getAllUsers(): List<UserResponse>

    @POST("/admin/ban_user")
    suspend fun banUser(@Body request: BanUserRequest): GenericResponse

    @GET("/targets")
    suspend fun getTargets(@Query("user_id") userId: Int? = null): List<TargetResponse>

    @POST("/admin/create_target")
    suspend fun createTarget(@Body request: CreateTargetRequest): GenericResponse

    @DELETE("/admin/delete_target/{id}")
    suspend fun deleteTarget(@Path("id") id: Int): GenericResponse

    // NUOVO: Aggiornamento posizione
    @POST("/update_position")
    suspend fun updatePosition(@Body request: UpdatePositionRequest): GenericResponse

    // NUOVO: Ottenere posizioni giocatori attivi
    @GET("/users_positions")
    suspend fun getUsersPositions(): List<UserResponse>

    // NUOVO: Hacking
    @POST("/hack")
    suspend fun hackTarget(@Body request: HackRequest): GenericResponse

    @POST("/generate_random_targets")
    suspend fun generateRandomTargets(@Body request: GenerateRandomTargetsRequest): GenericResponse

    // LOBBY
    @POST("/lobby/join")
    suspend fun joinLobby(@Body request: JoinLobbyRequest): JoinLobbyResponse

    @POST("/lobby/leave")
    suspend fun leaveLobby(@Body request: JoinLobbyRequest): GenericResponse
}
