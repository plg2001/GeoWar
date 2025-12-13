package com.example.geowar.models

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val team: String?,
    val score: Int,
    val avatar_seed: String?,
    val admin: Boolean,
    val banned: Boolean
)
