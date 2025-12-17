package com.example.geowar.models

data class UserDetails(
    val username: String,
    val email: String,
    val avatar_seed: String?,
    val lobby_id: Int? = null
)
