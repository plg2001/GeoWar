package com.example.geowar.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.ApiClient
import com.example.geowar.data.auth.JoinLobbyRequest
import com.example.geowar.data.auth.JoinLobbyResponse
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LobbyViewModel : ViewModel() {

    fun joinPublicLobby(
        userId: Int,
        onResult: (JoinLobbyResponse?, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.joinLobby(JoinLobbyRequest(userId))
                onResult(response, "Entrato nella lobby: ${response.lobby_id}")
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: "Errore sconosciuto"
                onResult(null, "Errore Join Lobby: $errorBody")
            } catch (e: Exception) {
                onResult(null, "Errore Join Lobby: ${e.message}")
            }
        }
    }
}
