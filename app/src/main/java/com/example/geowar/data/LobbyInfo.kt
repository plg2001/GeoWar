package com.example.geowar.data

import com.google.gson.annotations.SerializedName

data class LobbyInfo(
    val id: Int,
    val status: String,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("player_count") val playerCount: Int,
    @SerializedName("targets_red") val targetsRed: Int,
    @SerializedName("targets_blue") val targetsBlue: Int
)
