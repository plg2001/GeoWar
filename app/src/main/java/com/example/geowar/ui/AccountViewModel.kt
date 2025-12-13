package com.example.geowar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.repository.UserRepository
import kotlinx.coroutines.launch

class AccountViewModel(private val userRepository: UserRepository) : ViewModel() {

    var username by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var avatarSeed by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadUserProfile() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = userRepository.getCurrentUserDetails()
            result.onSuccess { userDetails ->
                username = userDetails.username
                email = userDetails.email
                avatarSeed = userDetails.avatar_seed
            }.onFailure { error ->
                errorMessage = error.message ?: "Errore sconosciuto"
            }
            isLoading = false
        }
    }

    fun updateUsername(newUsername: String) {
        username = newUsername
    }

    fun updateEmail(newEmail: String) {
        email = newEmail
    }

    fun generateNewAvatarSeed() {
        avatarSeed = java.util.UUID.randomUUID().toString()
    }

    fun saveChanges(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = userRepository.updateUserDetails(username, email, avatarSeed)
            result.onSuccess {
                onSuccess()
            }.onFailure { error ->
                errorMessage = error.message ?: "Impossibile salvare le modifiche"
            }
            isLoading = false
        }
    }
}
