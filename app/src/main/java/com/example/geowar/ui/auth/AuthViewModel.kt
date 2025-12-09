package com.example.geowar.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.auth.ApiClient
import com.example.geowar.data.auth.AuthRequest
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    fun login(
        username: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.login(AuthRequest(username, password))
                onResult(true, "Login ok: ${response.user.username}")
            } catch (e: Exception) {
                onResult(false, "Errore login: ${e.message}")
            }
        }
    }

    fun register(
        username: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.register(AuthRequest(username, password))
                onResult(true, "Registrazione ok: ${response.user.username}")
            } catch (e: Exception) {
                onResult(false, "Errore registrazione: ${e.message}")
            }
        }
    }
}
