package com.example.geowar.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.auth.ApiClient
import com.example.geowar.data.auth.BanUserRequest
import com.example.geowar.data.auth.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<UserResponse>>(emptyList())
    val users: StateFlow<List<UserResponse>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userList = ApiClient.authApi.getAllUsers()
                _users.value = userList
            } catch (e: Exception) {
                _message.value = "Errore caricamento dati: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun banUser(userId: Int) {
        val originalUsers = _users.value
        // Aggiornamento ottimistico: rimuove l'utente dalla UI immediatamente
        _users.value = originalUsers.filterNot { it.id == userId }

        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.banUser(BanUserRequest(userId))
                if (response.success) {
                    // Successo: la UI è già corretta, mostra solo il messaggio
                    _message.value = response.message
                } else {
                    // Fallimento: ripristina la lista originale e mostra l'errore
                    _message.value = "Ban fallito: ${response.message}"
                    _users.value = originalUsers
                }
            } catch (e: Exception) {
                // Eccezione: ripristina la lista originale e mostra l'errore
                _message.value = "Errore di rete durante il ban: ${e.message}"
                _users.value = originalUsers
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
