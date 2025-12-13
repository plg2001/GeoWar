package com.example.geowar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    var playerPosition by mutableStateOf<LatLng?>(null)
        private set

    private var movementJob: Job? = null
    private val moveSpeed = 0.00005 // Velocità di movimento sulla mappa

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
