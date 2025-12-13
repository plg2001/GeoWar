package com.example.geowar.models

data class TargetResponse(
    val id: Int,
    val name: String,
    val lat: Double,
    val lon: Double,
    val owner: String
)
