package com.example.geowar.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.jwt.JWT
import com.example.geowar.data.auth.ApiClient
import com.example.geowar.data.auth.GoogleLoginRequest
import com.example.geowar.data.auth.LoginRequest
import com.example.geowar.data.auth.RegisterRequest
import com.example.geowar.data.auth.SetTeamRequest
import com.example.geowar.data.auth.UserResponse
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AuthViewModel : ViewModel() {

    fun login(
        username: String,
        password: String,
        onResult: (UserResponse?, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.login(LoginRequest(username, password))
                onResult(response.user, "Login ok: ${response.user.username}")
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: "Errore sconosciuto"
                onResult(null, "Errore Login: $errorBody")
            } catch (e: Exception) {
                onResult(null, "Errore login: ${e.message}")
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
                onResult(response.user, "Registrazione ok: ${response.user.username}")
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: "Errore sconosciuto"
                onResult(null, "Errore Registrazione: $errorBody")
            } catch (e: Exception) {
                onResult(null, "Errore registrazione: ${e.message}")
            }
        }
    }
    
    // NUOVO: Login con Google
    fun signInWithGoogleToken(
        token: String,
        onResult: (UserResponse?, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Decodifichiamo il token JWT per estrarre email e nome
                var email = "google_user@example.com"
                var name = "Google User"

                try {
                    val jwt = JWT(token)
                    email = jwt.getClaim("email").asString() ?: email
                    name = jwt.getClaim("name").asString() ?: name
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Inviamo token, email e username (che ora corrisponde a 'name' nel backend)
                val response = ApiClient.authApi.googleLogin(GoogleLoginRequest(token, email, name))
                onResult(response.user, "Google Login successo: ${response.user.username}")
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                onResult(null, "Errore Google Login (HTTP ${e.code()}): $errorBody")
            } catch (e: Exception) {
                onResult(null, "Errore Google Login: ${e.message}")
            }
        }
    }
    
    // Alias per compatibilità
    fun loginWithGoogle(token: String, onResult: (UserResponse?, String) -> Unit) {
        signInWithGoogleToken(token, onResult)
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
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: "Errore sconosciuto"
                onResult(false, "Errore salvataggio team: $errorBody")
            } catch (e: Exception) {
                onResult(false, "Errore salvataggio team: ${e.message}")
            }
        }
    }
}
