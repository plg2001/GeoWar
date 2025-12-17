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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import kotlin.random.Random

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
    val currentLobbyId by remember { derivedStateOf { mapViewModel.currentLobbyId } }
    val gameCancelled by remember { derivedStateOf { mapViewModel.gameCancelled } }
    
    var hasPermission by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    
    var showMinigame by remember { mutableStateOf(false) }
    var currentMinigameTargetName by remember { mutableStateOf<String?>(null) }
    var currentMinigameColor by remember { mutableStateOf<String>("RED") }
    
    var isFirstCameraMove by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.4642, 9.1900), 20f)
    }

    val coroutineScope = rememberCoroutineScope()

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
    }
    
    if (gameCancelled) {
        CyberpunkDialog(
            title = "TRANSMISSION LOST",
            message = "Not enough operators to sustain connection. Returning to lobby selection.",
            confirmText = "ROGER",
            onConfirm = {
                mapViewModel.resetGameCancelled()
                mapViewModel.leaveLobby()
                onLogout()
            },
            onDismiss = { /* Non dismissable */ },
            icon = Icons.Default.WarningAmber,
            titleColor = MaterialTheme.colorScheme.error
        )
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

    LaunchedEffect(playerPosition) {
        playerPosition?.let {
            if (isFirstCameraMove) {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 20f))
                isFirstCameraMove = false
            } else {
                cameraPositionState.animate(CameraUpdateFactory.newLatLng(it), 100)
            }
        }
    }

    val teamColor = if (team == "BLUE") Color(0xFF00E5FF) else Color(0xFFFF4081)
    val teamHue = if (team == "BLUE") BitmapDescriptorFactory.HUE_CYAN else BitmapDescriptorFactory.HUE_ROSE

    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
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
    
    LaunchedEffect(otherPlayers) {
        otherPlayers.forEach { player ->
            if (player.avatar_seed != null) {
                val cached = otherPlayersBitmaps[player.username]
                
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

    if (showMinigame) {
        val isNeutralTarget = nearbyTarget?.owner == "NEUTRAL"
        
        if (isNeutralTarget) {
            ColorMinigameScreen(
                targetColorName = currentMinigameColor,
                onWin = {
                    nearbyTarget?.let { target -> mapViewModel.conquerTarget(target.id) }
                    showMinigame = false
                    currentMinigameTargetName = null
                    mapViewModel.clearNearbyTarget()
                    Toast.makeText(context, "NEUTRAL TARGET CONQUERED!", Toast.LENGTH_SHORT).show()
                },
                onLose = {
                    showMinigame = false
                    currentMinigameTargetName = null
                }
            )
        } else {
             MinigameScreen(
                targetName = currentMinigameTargetName ?: "TARGET",
                onWin = {
                    nearbyTarget?.let { target -> mapViewModel.conquerTarget(target.id) }
                    showMinigame = false
                    currentMinigameTargetName = null
                    mapViewModel.clearNearbyTarget()
                    Toast.makeText(context, "HACK COMPLETED!", Toast.LENGTH_SHORT).show()
                },
                onLose = { shouldCooldown ->
                    if (shouldCooldown) {
                        nearbyTarget?.let { target ->
                            mapViewModel.setTargetCooldown(target.id, 2 * 60 * 1000) // 2 minuti
                        }
                        Toast.makeText(context, "System Locked! Cooldown: 2m", Toast.LENGTH_SHORT).show()
                    }
                    showMinigame = false
                    currentMinigameTargetName = null
                }
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (playerPosition != null) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false, mapStyleOptions = customMapStyle),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
            ) {
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for GPS signal...", color = Color.White)
                }
            }
        }

        // --- UI OVERLAY ---
        
        if (targets.isEmpty() && playerPosition != null && !gameCancelled) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "> WAITING FOR OPPONENTS...",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Need at least 1 RED and 1 BLUE operator to begin.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))) {
                Text(
                    text = "OPERATOR: TEAM $team",
                    color = teamColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FloatingActionButton(
                onClick = { showLogoutDialog = true },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 120.dp, start = 16.dp)
        ) {
            if (currentLobbyId != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))) {
                    Text(
                        text = "LOBBY #$currentLobbyId",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = nearbyTarget != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            nearbyTarget?.let { target ->
                val isNeutral = target.owner == "NEUTRAL"
                val isEnemy = target.owner != team && !isNeutral
                val isOwned = target.owner == team
                val isCooldown = mapViewModel.isTargetOnCooldown(target.id)

                val buttonText = when {
                    isNeutral -> "START COLOR SCAN"
                    isOwned -> "SECURE ZONE"
                    isCooldown -> "SYSTEM LOCKED (COOLDOWN)"
                    else -> "HACK SYSTEM (START)"
                }
                
                val buttonColor = when {
                    isNeutral -> Color.Yellow
                    isOwned -> Color.Green
                    isCooldown -> Color.Gray
                    else -> Color.Red
                }

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
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TARGET DETECTED",
                                color = if (isNeutral) Color.Yellow else if (isEnemy) Color.Red else Color.Green,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            IconButton(
                                onClick = { mapViewModel.clearNearbyTarget() },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(text = "You entered the contested zone of:", color = Color.White)
                        Text(
                            text = target.name,
                            color = teamColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        if (isEnemy) {
                            Text(
                                text = "OWNER: ${target.owner}",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else if (isNeutral) {
                            Text(
                                text = "OWNER: NEUTRAL",
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Find the requested color to conquer!",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                text = "OWNER: YOUR TEAM",
                                color = Color.Green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (isOwned) {
                            Text("Zone already under control.", color = Color.Green)
                        } else {
                            Button(
                                onClick = { 
                                    if (isNeutral) {
                                        val colors = listOf("RED", "GREEN", "BLUE")
                                        currentMinigameColor = colors[Random.nextInt(colors.size)]
                                        currentMinigameTargetName = target.name
                                        showMinigame = true
                                    } else {
                                        if (isCooldown) {
                                            Toast.makeText(context, "Access Denied: Security Cooldown Active", Toast.LENGTH_SHORT).show()
                                        } else {
                                            currentMinigameTargetName = target.name
                                            showMinigame = true
                                        }
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

        if (playerPosition != null && targets.isNotEmpty()) {
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
                        FabAction(icon = Icons.Default.MyLocation, label = "Center") {
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
                    Icon(Icons.Default.Add, "Open actions menu", modifier = Modifier.rotate(rotation))
                }
            }
        }

        if (showLogoutDialog) {
             CyberpunkDialog(
                title = "DISCONNECT?",
                message = "Are you sure you want to disconnect and return to lobby selection?",
                confirmText = "LEAVE MATCH",
                dismissText = "STAY",
                onConfirm = {
                    showLogoutDialog = false
                    mapViewModel.leaveLobby()
                    onLogout()
                },
                onDismiss = { showLogoutDialog = false },
                titleColor = MaterialTheme.colorScheme.error
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

@Composable
fun CyberpunkDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    titleColor: Color = MaterialTheme.colorScheme.primary
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, titleColor),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(text = title, style = MaterialTheme.typography.headlineSmall, color = titleColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                     if (dismissText != null) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(dismissText)
                        }
                    }
                    Button(
                        onClick = onConfirm, 
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = titleColor)
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}