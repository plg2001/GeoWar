package com.example.geowar.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.auth.ApiClient
import com.example.geowar.data.auth.LoginRequest
import com.example.geowar.data.auth.RegisterRequest
import com.example.geowar.data.auth.SetTeamRequest
import com.example.geowar.data.auth.UserResponse
import kotlinx.coroutines.launch

// DTO per la richiesta di login con Google
data class GoogleLoginRequest(val token: String)

class AuthViewModel : ViewModel() {

    // Modificato callback: restituisce UserResponse? (null se errore) e String (messaggio)
    fun login(
        username: String,
        password: String,
        onResult: (UserResponse?, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.login(LoginRequest(username, password))
                onResult(response.user, "Login ok: ${'$'}{response.user.username}")
            } catch (e: Exception) {
                onResult(null, "Errore login: ${'$'}{e.message}")
            }
        }
    }

    fun register(
        username: String,
        password: String,
        email: String,
        onResult: (UserResponse?, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.register(RegisterRequest(username, password, email))
                onResult(response.user, "Registrazione ok: ${'$'}{response.user.username}")
            } catch (e: Exception) {
                onResult(null, "Errore registrazione: ${'$'}{e.message}")
            }
        }
    }

    /**
     * Invia il token ID di Google al backend per la verifica.
     */
    fun loginWithGoogle(
        idToken: String,
        onResult: (UserResponse?, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Qui dovrai chiamare il tuo endpoint del backend
                val response = ApiClient.authApi.loginWithGoogle(GoogleLoginRequest(idToken))
                onResult(response.user, "Login con Google ok: ${'$'}{response.user.username}")
            } catch (e: Exception) {
                Log.e("GOOGLE_LOGIN_ERROR", "Chiamata API per il login con Google fallita", e)
                onResult(null, "Errore login con Google: ${'$'}{e.message}")
            }
        }
    }

    fun setTeam(
        userId: Int,
        team: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.setTeam(SetTeamRequest(userId, team))
                onResult(true, "Team aggiornato: $team")
            } catch (e: Exception) {
                onResult(false, "Errore salvataggio team: ${'$'}{e.message}")
            }
        }
    }
}
