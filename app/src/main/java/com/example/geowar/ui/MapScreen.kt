package com.example.geowar.ui

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import coil.request.ImageRequest
import com.example.geowar.R
import com.example.geowar.ui.composables.Joystick
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    username: String,
    team: String,
    onLogout: () -> Unit,
    onAccountClick: () -> Unit
) {
    val context = LocalContext.current
    val mapViewModel: MapViewModel = viewModel()
    val customMapStyle = remember {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val playerPosition = mapViewModel.playerPosition
    val avatarSeed by remember { derivedStateOf { mapViewModel.avatarSeed } }
    val targets by remember { derivedStateOf { mapViewModel.targets } }
    val nearbyTarget by remember { derivedStateOf { mapViewModel.nearbyTarget } }
    val otherPlayers by remember { derivedStateOf { mapViewModel.otherPlayers } }

    var hasPermission by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    
    // Stato per il Minigioco
    var showMinigame by remember { mutableStateOf(false) }
    var currentMinigameTarget by remember { mutableStateOf<String?>(null) }
    
    // Variabile per capire se è il primo posizionamento della camera
    var isFirstCameraMove by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    // Camera inizializzata a zoom alto (20f), anche se la posizione è provvisoria
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.4642, 9.1900), 20f)
    }

    val coroutineScope = rememberCoroutineScope()

    // Lifecycle Observer per ricaricare l'avatar quando si torna nella schermata
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mapViewModel.reloadAvatar()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        // mapViewModel.reloadAvatar() // Ora gestito da ON_RESUME
    }

    if (hasPermission) {
        val locationRequest = remember {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).apply {
                setMinUpdateIntervalMillis(2000L)
            }.build()
        }
        val locationCallback = remember {
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        mapViewModel.initializePlayerPosition(LatLng(it.latitude, it.longitude))
                    }
                }
            }
        }

        DisposableEffect(fusedLocationClient) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                mapViewModel.stopMoving()
            }
        }
    }

    // Logica di aggiornamento camera
    LaunchedEffect(playerPosition) {
        playerPosition?.let {
            if (isFirstCameraMove) {
                // Primo caricamento: SCATTO immediato allo zoom massimo (20f)
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 20f))
                isFirstCameraMove = false
            } else {
                // Movimenti successivi (Joystick): Animazione rapida (100ms) mantenendo lo zoom
                cameraPositionState.animate(CameraUpdateFactory.newLatLng(it), 100)
            }
        }
    }

    val teamColor = if (team == "BLUE") Color(0xFF00E5FF) else Color(0xFFFF4081)
    val teamHue = if (team == "BLUE") BitmapDescriptorFactory.HUE_CYAN else BitmapDescriptorFactory.HUE_ROSE

    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Mappa per salvare le bitmap degli avatar degli altri giocatori
    // Chiave: username, Valore: Pair(avatarSeed, Bitmap)
    // Usiamo una Pair per controllare se il seed è cambiato
    val otherPlayersBitmaps = remember { mutableStateMapOf<String, Pair<String, Bitmap>>() }

    LaunchedEffect(avatarSeed) {
        avatarSeed?.let {
            val request = ImageRequest.Builder(context)
                .data("https://api.dicebear.com/7.x/pixel-art/png?seed=$it")
                .target {
                    avatarBitmap = (it as android.graphics.drawable.BitmapDrawable).bitmap
                }.build()
            context.imageLoader.enqueue(request)
        }
    }
    
    // Scarica gli avatar per gli altri giocatori
    LaunchedEffect(otherPlayers) {
        otherPlayers.forEach { player ->
            if (player.avatar_seed != null) {
                val cached = otherPlayersBitmaps[player.username]
                
                // Scarica se non c'è in cache O se il seed è diverso
                if (cached == null || cached.first != player.avatar_seed) {
                    val request = ImageRequest.Builder(context)
                        .data("https://api.dicebear.com/7.x/pixel-art/png?seed=${player.avatar_seed}")
                        .target {
                             val bitmap = (it as android.graphics.drawable.BitmapDrawable).bitmap
                             otherPlayersBitmaps[player.username] = Pair(player.avatar_seed, bitmap)
                        }.build()
                    context.imageLoader.enqueue(request)
                }
            }
        }
    }

    // SE IL MINIGIOCO E' ATTIVO, MOSTRA SOLO QUELLO A SCHERMO INTERO
    if (showMinigame) {
        MinigameScreen(
            targetName = currentMinigameTarget ?: "TARGET",
            onWin = {
                // Se vinco il minigioco, chiamo l'API per conquistare il target
                nearbyTarget?.let { target ->
                    mapViewModel.conquerTarget(target.id)
                }
                
                showMinigame = false
                currentMinigameTarget = null
                
                // IMPORTANTE: Chiude il popup "TARGET RILEVATO" per evitare che riappaia subito
                mapViewModel.clearNearbyTarget()
            },
            onLose = {
                showMinigame = false
                currentMinigameTarget = null
            }
        )
        return // Esce dalla funzione per non renderizzare la mappa sotto
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (playerPosition != null) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapStyleOptions = customMapStyle
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
            ) {
                // Marker Giocatore
                if (avatarBitmap != null) {
                    Marker(
                        state = MarkerState(position = playerPosition),
                        title = "$username",
                        icon = BitmapDescriptorFactory.fromBitmap(avatarBitmap!!)
                    )
                } else {
                    Marker(
                        state = MarkerState(position = playerPosition),
                        title = "$username",
                        icon = BitmapDescriptorFactory.defaultMarker(teamHue)
                    )
                }
                
                // Marker Altri Giocatori
                otherPlayers.forEach { player ->
                    val playerPos = LatLng(player.lat ?: 0.0, player.lon ?: 0.0)
                    val playerColor = if (player.team == "BLUE") BitmapDescriptorFactory.HUE_CYAN else if (player.team == "RED") BitmapDescriptorFactory.HUE_ROSE else BitmapDescriptorFactory.HUE_VIOLET
                    
                    val playerCache = otherPlayersBitmaps[player.username]
                    
                    if (playerCache != null) {
                        Marker(
                            state = MarkerState(position = playerPos),
                            title = player.username,
                            snippet = "Team: ${player.team}",
                            icon = BitmapDescriptorFactory.fromBitmap(playerCache.second)
                        )
                    } else {
                        Marker(
                            state = MarkerState(position = playerPos),
                            title = player.username,
                            snippet = "Team: ${player.team}",
                            icon = BitmapDescriptorFactory.defaultMarker(playerColor)
                        )
                    }
                }

                // Marker Target
                targets.forEach { target ->
                    val targetColor = when (target.owner) {
                        "BLUE" -> BitmapDescriptorFactory.HUE_AZURE
                        "RED" -> BitmapDescriptorFactory.HUE_RED
                        else -> BitmapDescriptorFactory.HUE_YELLOW // NEUTRAL
                    }
                    
                    Marker(
                        state = MarkerState(position = LatLng(target.lat, target.lon)),
                        title = target.name,
                        snippet = "Owner: ${target.owner}",
                        icon = BitmapDescriptorFactory.defaultMarker(targetColor)
                    )
                }
            }
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("In attesa del segnale GPS...", color = Color.White)
                }
            }
        }

        // --- UI OVERLAY ---

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))) {
                Text(
                    text = "OPERATORE: TEAM $team",
                    color = teamColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FloatingActionButton(
                onClick = { showLogoutDialog = true },
                containerColor = Color.Red,
                contentColor = Color.White,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
            }
        }
        
        // --- POPUP INTERAZIONE TARGET ---
        AnimatedVisibility(
            visible = nearbyTarget != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            nearbyTarget?.let { target ->
                val isEnemy = target.owner != team && target.owner != "NEUTRAL"
                val buttonText = if (isEnemy) "HACK SYSTEM (AVVIA)" else "CONQUISTA (DIRETTA)"
                val buttonColor = if (isEnemy) Color.Red else teamColor

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header con pulsante chiusura
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TARGET RILEVATO",
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            IconButton(
                                onClick = { mapViewModel.clearNearbyTarget() },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Sei entrato nello spazio conteso di:",
                            color = Color.White
                        )
                        Text(
                            text = target.name,
                            color = teamColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        if (target.owner != "NEUTRAL" && target.owner != team) {
                            Text(
                                text = "PROPRIETA': ${target.owner}",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else if (target.owner == "NEUTRAL") {
                            Text(
                                text = "PROPRIETA': NEUTRALE",
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Text(
                                text = "PROPRIETA': TUO TEAM",
                                color = Color.Green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Logica pulsante: Se già mio -> niente o rinforza. Se nemico -> Minigame. Se Neutro -> Conquista diretta (o minigame facile, per ora diretta)
                        if (target.owner == team) {
                            Text("Zona già sotto controllo.", color = Color.Green)
                        } else {
                            Button(
                                onClick = { 
                                    if (isEnemy) {
                                        // Avvia minigioco solo se nemico
                                        currentMinigameTarget = target.name
                                        showMinigame = true
                                    } else {
                                        // Conquista diretta se neutrale (o logica placeholder)
                                        Toast.makeText(context, "Target Neutrale Conquistato!", Toast.LENGTH_SHORT).show()
                                        mapViewModel.conquerTarget(target.id) 
                                        mapViewModel.clearNearbyTarget()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(buttonText, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (playerPosition != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
            ) {
                Joystick(onMove = { dx, dy ->
                    mapViewModel.startMoving(dx, dy)
                })
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 32.dp)
        ) {
            val rotation by animateFloatAsState(if (isFabMenuExpanded) 45f else 0f, label = "fab_rotation")
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Bottom)
            ) {
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        FabAction(icon = Icons.Default.AccountCircle, label = "Account", onClick = onAccountClick)
                        FabAction(icon = Icons.Default.MyLocation, label = "Centra") {
                            playerPosition?.let {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 20f), 500)
                                }
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, "Apri menu azioni", modifier = Modifier.rotate(rotation))
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Abbandonare la partita?") },
                text = { Text("Sei sicuro di voler uscire e tornare al menu principale?") },
                confirmButton = {
                    Button(onClick = { showLogoutDialog = false; onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Abbandona") }
                },
                dismissButton = { TextButton({ showLogoutDialog = false }) { Text("Annulla") } }
            )
        }
    }
}

@Composable
private fun FabAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) { Icon(imageVector = icon, contentDescription = label) }
        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
