package com.example.geowar.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.ApiClient
import com.example.geowar.repository.UserRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    var playerPosition by mutableStateOf<LatLng?>(null)
        private set

    var avatarSeed by mutableStateOf<String?>(null)
        private set

    private var movementJob: Job? = null
    private val moveSpeed = 0.00005 // Velocità di movimento sulla mappa

    private val userRepository = UserRepository(ApiClient.authApi, application)

    init {
        loadAvatar()
    }

    private fun loadAvatar() {
        viewModelScope.launch {
            userRepository.getCurrentUserDetails().onSuccess {
                avatarSeed = it.avatar_seed
            }
        }
    }

    fun initializePlayerPosition(initialLocation: LatLng) {
        if (playerPosition == null) {
            playerPosition = initialLocation
        }
    }

    fun startMoving(dx: Float, dy: Float) {
        movementJob?.cancel() // Ferma il movimento precedente
        if (dx == 0f && dy == 0f) return // Il joystick è stato rilasciato, ferma tutto

        movementJob = viewModelScope.launch {
            while (true) {
                playerPosition?.let { currentPos ->
                    val newLat = currentPos.latitude + (-dy * moveSpeed)
                    val newLng = currentPos.longitude + (dx * moveSpeed)
                    playerPosition = LatLng(newLat, newLng)
                    // TODO: Inviare la posizione aggiornata al server qui
                }
                delay(100) // Aggiorna la posizione ogni 100ms
            }
        }
    }

    fun stopMoving() {
        movementJob?.cancel()
    }
}
