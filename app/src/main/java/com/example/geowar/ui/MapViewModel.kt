package com.example.geowar.ui

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.auth.ApiClient
import com.example.geowar.data.auth.TargetResponse
import com.example.geowar.data.auth.UpdatePositionRequest
import com.example.geowar.repository.UserRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    var playerPosition by mutableStateOf<LatLng?>(null)
        private set

    var avatarSeed by mutableStateOf<String?>(null)
        private set

    var targets by mutableStateOf<List<TargetResponse>>(emptyList())
        private set
    
    // Variabile per il target vicino (entro 20 metri)
    var nearbyTarget by mutableStateOf<TargetResponse?>(null)
        private set

    private var movementJob: Job? = null
    private var heartbeatJob: Job? = null
    
    private val moveSpeed = 0.00001 // Circa 1m per tick (50ms)

    private val userRepository = UserRepository(ApiClient.authApi, application)

    init {
        loadAvatar()
        loadTargets()
        startHeartbeat()
    }

    private fun loadAvatar() {
        viewModelScope.launch {
            userRepository.getCurrentUserDetails().onSuccess {
                avatarSeed = it.avatar_seed
            }
        }
    }

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
        movementJob?.cancel()
        if (dx == 0f && dy == 0f) return

        movementJob = viewModelScope.launch {
            while (true) {
                playerPosition?.let { currentPos ->
                    val newLat = currentPos.latitude + (-dy * moveSpeed)
                    val newLng = currentPos.longitude + (dx * moveSpeed)
                    val newPos = LatLng(newLat, newLng)
                    playerPosition = newPos
                    
                    // Controllo prossimità ogni volta che ci muoviamo
                    checkProximityToTargets(newPos)
                }
                delay(50)
            }
        }
    }

    fun stopMoving() {
        movementJob?.cancel()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                playerPosition?.let { pos ->
                    val userId = userRepository.getUserId()
                    if (userId != -1) {
                        try {
                            ApiClient.authApi.updatePosition(
                                UpdatePositionRequest(userId, pos.latitude, pos.longitude)
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                delay(3000) // 3 secondi
            }
        }
    }

    private fun checkProximityToTargets(currentPos: LatLng) {
        // Se c'è già un target attivo (popup aperto), non cerchiamo altro per evitare spam/flash
        if (nearbyTarget != null) return

        var foundTarget: TargetResponse? = null
        val distanceResults = FloatArray(1)

        for (target in targets) {
            Location.distanceBetween(
                currentPos.latitude, currentPos.longitude,
                target.lat, target.lon,
                distanceResults
            )
            // Se distanza < 20 metri
            if (distanceResults[0] < 20) {
                foundTarget = target
                break // Trovato uno, usciamo
            }
        }
        nearbyTarget = foundTarget
    }
    
    fun clearNearbyTarget() {
        nearbyTarget = null
    }

    override fun onCleared() {
        super.onCleared()
        movementJob?.cancel()
        heartbeatJob?.cancel()
    }
}
