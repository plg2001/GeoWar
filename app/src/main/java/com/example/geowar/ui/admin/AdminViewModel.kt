package com.example.geowar.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.auth.ApiClient
import com.example.geowar.data.auth.BanUserRequest
import com.example.geowar.data.auth.CreateTargetRequest
import com.example.geowar.data.auth.TargetResponse
import com.example.geowar.data.auth.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<UserResponse>>(emptyList())
    val users: StateFlow<List<UserResponse>> = _users.asStateFlow()

    private val _targets = MutableStateFlow<List<TargetResponse>>(emptyList())
    val targets: StateFlow<List<TargetResponse>> = _targets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Carica utenti
                val userList = ApiClient.authApi.getAllUsers()
                _users.value = userList

                // Carica target
                val targetList = ApiClient.authApi.getTargets()
                _targets.value = targetList
            } catch (e: Exception) {
                _message.value = "Errore caricamento dati: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun banUser(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = ApiClient.authApi.banUser(BanUserRequest(userId))
                if (response.success) {
                    _message.value = "Utente bannato/rimosso con successo"
                    loadData() // Ricarica la lista
                } else {
                    _message.value = "Errore: ${response.message}"
                }
            } catch (e: Exception) {
                _message.value = "Errore ban: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTarget(name: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = ApiClient.authApi.createTarget(CreateTargetRequest(name, lat, lon))
                if (response.success) {
                    _message.value = "Target creato con successo"
                    loadData()
                } else {
                    _message.value = "Errore creazione: ${response.message}"
                }
            } catch (e: Exception) {
                _message.value = "Errore creazione: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTarget(targetId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = ApiClient.authApi.deleteTarget(targetId)
                if (response.success) {
                    _message.value = "Target eliminato"
                    loadData()
                } else {
                    _message.value = "Errore eliminazione: ${response.message}"
                }
            } catch (e: Exception) {
                _message.value = "Errore eliminazione: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
