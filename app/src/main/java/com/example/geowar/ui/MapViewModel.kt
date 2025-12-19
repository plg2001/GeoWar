package com.example.geowar.ui

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geowar.data.ApiClient
import com.example.geowar.data.auth.GenerateRandomTargetsRequest
import com.example.geowar.data.auth.HackRequest
import com.example.geowar.data.auth.JoinLobbyRequest
import com.example.geowar.data.auth.TargetResponse
import com.example.geowar.data.auth.UpdatePositionRequest
import com.example.geowar.data.auth.UserResponse
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

    var currentLobbyId by mutableStateOf<Int?>(null)
        private set

    var targets by mutableStateOf<List<TargetResponse>>(emptyList())
        private set

    var targetsRed by mutableStateOf(0)
        private set

    var targetsBlue by mutableStateOf(0)
        private set

    // Variabile per il target vicino (entro 20 metri)
    var nearbyTarget by mutableStateOf<TargetResponse?>(null)
        private set

    // Lista degli altri giocatori
    var otherPlayers by mutableStateOf<List<UserResponse>>(emptyList())
        private set

    // Stato partita annullata
    var gameCancelled by mutableStateOf(false)
        private set

    // Cooldown dei target: TargetID -> Timestamp scandenza
    private val _targetCooldowns = mutableStateMapOf<Int, Long>()
    val targetCooldowns: Map<Int, Long> = _targetCooldowns

    private var movementJob: Job? = null
    private var heartbeatJob: Job? = null
    private var playersFetcherJob: Job? = null
    private var targetsPollingJob: Job? = null // Nuovo Job per aggiornare i target
    private var lobbyInfoJob: Job? = null

    private val moveSpeed = 0.0001 // Circa 1m per tick (50ms)

    private val userRepository = UserRepository(ApiClient.authApi, application)

    init {
        loadUserDetails()
        loadTargets() // Caricamento iniziale
        startPollingTargets() // Avvio polling periodico
        startHeartbeat()
        startFetchingPlayers()
        startPollingLobbyInfo()
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            userRepository.getCurrentUserDetails().onSuccess {
                avatarSeed = it.avatar_seed
                currentLobbyId = it.lobby_id
            }
        }
    }

    fun reloadAvatar() {
        loadUserDetails()
    }

    fun loadTargets() {
        viewModelScope.launch {
            try {
                val userId = userRepository.getUserId()
                if (userId != -1) {
                    targets = ApiClient.authApi.getTargets(userId)
                } else {
                    targets = ApiClient.authApi.getTargets(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPollingTargets() {
        targetsPollingJob?.cancel()
        targetsPollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    // Ricarica la lista dei target dal server ogni 5 secondi
                    // per aggiornare i colori se qualcuno li ha conquistati
                    val userId = userRepository.getUserId()
                    if (userId != -1) {
                        val newTargets = ApiClient.authApi.getTargets(userId)

                        // Logica per rilevare partita annullata (targets spariti)
                        if (targets.isNotEmpty() && newTargets.isEmpty()) {
                            gameCancelled = true
                        }

                        targets = newTargets
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    private fun startPollingLobbyInfo() {
        lobbyInfoJob?.cancel()
        lobbyInfoJob = viewModelScope.launch {
            while (isActive) {
                try {
                    currentLobbyId?.let {
                        val lobbyInfo = ApiClient.authApi.getLobbies().find { lobby -> lobby.id == it }
                        lobbyInfo?.let {
                            targetsRed = lobbyInfo.targetsRed
                            targetsBlue = lobbyInfo.targetsBlue
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    fun initializePlayerPosition(initialLocation: LatLng) {
        if (playerPosition == null) {
            playerPosition = initialLocation
            // generateRandomTargets(initialLocation) // DECOMMETARE IN FUTURO
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

    fun leaveLobby() {
        viewModelScope.launch {
            val userId = userRepository.getUserId()
            if (userId != -1) {
                try {
                    ApiClient.authApi.leaveLobby(JoinLobbyRequest(userId, currentLobbyId ?: -1))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun resetGameCancelled() {
        gameCancelled = false
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

    private fun startFetchingPlayers() {
        playersFetcherJob?.cancel()
        playersFetcherJob = viewModelScope.launch {
            while (isActive) {
                try {
                    currentLobbyId?.let { lobbyId ->
                        val lobbyUsers = ApiClient.authApi.getLobbyUsers(lobbyId)
                        val currentUserId = userRepository.getUserId()
                        otherPlayers = lobbyUsers.filter { it.id != currentUserId }
                    } ?: run {
                        otherPlayers = emptyList() // Svuota la lista se non c'è lobby
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3000) // Aggiorna ogni 3 secondi
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

        if (foundTarget != null) {
            nearbyTarget = foundTarget
        }
    }

    fun clearNearbyTarget() {
        nearbyTarget = null
    }

    // Questa funzione ora viene chiamata dalla MapScreen solo dopo la vittoria del minigioco (o conquista diretta)
    fun conquerTarget(targetId: Int) {
        viewModelScope.launch {
            val userId = userRepository.getUserId()
            if (userId != -1) {
                try {
                    ApiClient.authApi.hackTarget(HackRequest(userId, targetId))
                    // Ricarica immediatamente i target per aggiornare il colore sulla mappa
                    loadTargets()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Gestione Cooldown
    fun setTargetCooldown(targetId: Int, durationMillis: Long) {
        _targetCooldowns[targetId] = System.currentTimeMillis() + durationMillis
    }

    fun isTargetOnCooldown(targetId: Int): Boolean {
        val expiration = _targetCooldowns[targetId] ?: return false
        return System.currentTimeMillis() < expiration
    }

    private fun generateRandomTargets(currentLocation: LatLng) {
        viewModelScope.launch {
            try {
                ApiClient.authApi.generateRandomTargets(
                    GenerateRandomTargetsRequest(
                        lat = currentLocation.latitude,
                        lon = currentLocation.longitude
                    )
                )
                // Dopo aver generato i target, ricaricali per visualizzarli
                loadTargets()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        movementJob?.cancel()
        heartbeatJob?.cancel()
        playersFetcherJob?.cancel()
        targetsPollingJob?.cancel()
        lobbyInfoJob?.cancel()
    }
}
