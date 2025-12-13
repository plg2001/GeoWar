package com.example.geowar.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.auth.ApiClient
import com.example.geowar.data.auth.TargetResponse
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

    var targets by mutableStateOf<List<TargetResponse>>(emptyList())
        private set

    private var movementJob: Job? = null
    
    // MODIFICA 1: Riduco la velocità (era 0.00005)
    // 0.00001 è circa 1 metro a tick (molto più lento e controllabile)
    private val moveSpeed = 0.00001 

    private val userRepository = UserRepository(ApiClient.authApi, application)

    init {
        loadAvatar()
        loadTargets()
    }

    private fun loadAvatar() {
        viewModelScope.launch {
            userRepository.getCurrentUserDetails().onSuccess {
                avatarSeed = it.avatar_seed
            }
        }
    }

    // MODIFICA 2: Funzione pubblica per forzare il ricaricamento dell'avatar
    // Da chiamare quando si torna dalla schermata di modifica profilo
    fun reloadAvatar() {
        loadAvatar()
    }

    fun loadTargets() {
        viewModelScope.launch {
            try {
                targets = ApiClient.authApi.getTargets()
            } catch (e: Exception) {
                e.printStackTrace()
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
                delay(50) // Aggiorna ogni 50ms per fluidità (era 100ms)
            }
        }
    }

    fun stopMoving() {
        movementJob?.cancel()
    }
}
