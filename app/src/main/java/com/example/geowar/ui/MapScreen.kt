package com.example.geowar.ui

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Looper
import android.text.TextPaint
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

// --------------------------
// Bitmap helper + cache model
// --------------------------

private data class AvatarCache(
    val withName: Bitmap,
    val withoutName: Bitmap
)


fun getTargetWithBorder(
    source: Bitmap,
    teamColor: Int
): Bitmap {

    // --------------------------
    // 🎛️ PARAMETRI
    // --------------------------
    val circleSize = 100f      // dimensione cerchio (come sizeDp = 200)
    val fillFactor = 1f     // quanto la torre riempie il cerchio
    val borderWidth = 8f
    val shadowRadius = 18f

    // --------------------------
    // 📐 OUTPUT
    // --------------------------
    val outputSize = (circleSize + shadowRadius * 2).toInt()
    val center = outputSize / 2f

    val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    // --------------------------
    // 🖼️ SCALA TORRE (MANTIENE PROPORZIONI)
    // --------------------------
    val targetHeight = circleSize * fillFactor
    val aspectRatio = source.width.toFloat() / source.height.toFloat()
    val targetWidth = targetHeight * aspectRatio

    val scaledTower = Bitmap.createScaledBitmap(
        source,
        targetWidth.toInt(),
        targetHeight.toInt(),
        true
    )

    val left = center - targetWidth / 2f
    val top = center - targetHeight / 2f

    // --------------------------
    // ✨ GLOW
    // --------------------------
    val glowPaint = Paint().apply {
        color = teamColor
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        setShadowLayer(shadowRadius, 0f, 0f, teamColor)
    }
    canvas.drawCircle(center, center, circleSize / 2f, glowPaint)

    // --------------------------
    // 🔵 BORDO
    // --------------------------
    val borderPaint = Paint().apply {
        color = teamColor
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }
    canvas.drawCircle(center, center, circleSize / 2f, borderPaint)

    // --------------------------
    // 🏰 TORRE
    // --------------------------
    canvas.drawBitmap(scaledTower, left, top, null)

    return output
}



fun resizedBitmapDescriptor(
    context: android.content.Context,
    resId: Int,
    sizeDp: Int
): com.google.android.gms.maps.model.BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()

    val bitmap = BitmapFactory.decodeResource(context.resources, resId)
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()

    val targetWidth: Int
    val targetHeight: Int

    if (bitmap.width > bitmap.height) {
        targetWidth = sizePx
        targetHeight = (sizePx / ratio).toInt()
    } else {
        targetHeight = sizePx
        targetWidth = (sizePx * ratio).toInt()
    }

    val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)


    return BitmapDescriptorFactory.fromBitmap(scaled)
}
fun getAvatarWithBorderAndName(
    source: Bitmap,
    teamColor: Int,
    username: String,
    showName: Boolean
): Bitmap {
    val avatarSize = 110f
    val borderWidth = 8f
    val shadowRadius = 15f
    val fontSize = 28f
    val paddingBetween = 12f

    val textPaint = TextPaint().apply {
        color = android.graphics.Color.WHITE
        textSize = fontSize
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
    }

    val name = username.uppercase()
    val textWidth = if (showName) textPaint.measureText(name) else 0f
    val contentWidth = max(avatarSize + (shadowRadius * 2), textWidth + 20f).toInt()

    val contentHeight = if (showName) {
        (fontSize + paddingBetween + avatarSize + (shadowRadius * 2)).toInt()
    } else {
        (avatarSize + (shadowRadius * 2)).toInt()
    }

    val output = Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val centerX = contentWidth / 2f

    if (showName) {
        canvas.drawText(name, centerX, fontSize, textPaint)
    }

    val avatarTop = if (showName) fontSize + paddingBetween + shadowRadius else shadowRadius
    val avatarCenterY = avatarTop + (avatarSize / 2f)

    val scaledSource = Bitmap.createScaledBitmap(source, avatarSize.toInt(), avatarSize.toInt(), true)
    val circularBitmap = Bitmap.createBitmap(avatarSize.toInt(), avatarSize.toInt(), Bitmap.Config.ARGB_8888)
    val canvasCircle = Canvas(circularBitmap)
    val path = Path().apply {
        addCircle(avatarSize / 2f, avatarSize / 2f, avatarSize / 2f, Path.Direction.CCW)
    }
    canvasCircle.clipPath(path)
    canvasCircle.drawBitmap(scaledSource, 0f, 0f, null)

    val paintGlow = Paint().apply {
        color = teamColor
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        setShadowLayer(shadowRadius, 0f, 0f, teamColor)
    }
    canvas.drawCircle(centerX, avatarCenterY, avatarSize / 2f, paintGlow)

    val paintBorder = Paint().apply {
        color = teamColor
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }
    canvas.drawCircle(centerX, avatarCenterY, avatarSize / 2f, paintBorder)

    canvas.drawBitmap(
        circularBitmap,
        centerX - (avatarSize / 2f),
        avatarCenterY - (avatarSize / 2f),
        null
    )

    return output
}

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

    // --- ViewModel state ---
    val playerPosition = mapViewModel.playerPosition
    val avatarSeed by remember { derivedStateOf { mapViewModel.avatarSeed } }
    val targets by remember { derivedStateOf { mapViewModel.targets } }
    val targetsRed by remember { derivedStateOf { mapViewModel.targetsRed } }
    val targetsBlue by remember { derivedStateOf { mapViewModel.targetsBlue } }
    val nearbyTarget by remember { derivedStateOf { mapViewModel.nearbyTarget } }
    val otherPlayers by remember { derivedStateOf { mapViewModel.otherPlayers } }
    val currentLobbyId by remember { derivedStateOf { mapViewModel.currentLobbyId } }
    val gameCancelled by remember { derivedStateOf { mapViewModel.gameCancelled } }
    val bombDifficulty by remember { derivedStateOf { mapViewModel.bombDifficulty } }

    // --- UI state ---
    var hasPermission by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var winnerTeam by remember { mutableStateOf<String?>(null) }

    // --- MATCH TIMER (5 MINUTES) ---
    val matchDurationMs = 30 * 1000L
    var remainingTimeMs by remember { mutableStateOf(matchDurationMs) }
    var timerRunning by remember { mutableStateOf(false) }


    var showMinigame by remember { mutableStateOf(false) }
    var currentMinigameTargetName by remember { mutableStateOf<String?>(null) }
    var currentMinigameColor by remember { mutableStateOf("RED") }

    var isFirstCameraMove by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.4642, 9.1900), 20f)
    }

    val coroutineScope = rememberCoroutineScope()

    // --- Zoom derived state ---
    val currentZoom by remember { derivedStateOf { cameraPositionState.position.zoom } }
    val shouldShowNames by remember { derivedStateOf { currentZoom > 15f } }
    val markerAlpha by remember { derivedStateOf { if (currentZoom < 13f) 0.5f else 1.0f } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mapViewModel.reloadAvatar()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Mostra il dialog quando la difficoltà viene caricata
    LaunchedEffect(bombDifficulty) {
        if (bombDifficulty != null) {
            showDifficultyDialog = true
        }
    }

    // --- START MATCH TIMER WHEN GAME BEGINS ---
    LaunchedEffect(targets.isNotEmpty()) {
        if (targets.isNotEmpty() && !timerRunning) {
            timerRunning = true
            remainingTimeMs = matchDurationMs

            while (remainingTimeMs > 0 && timerRunning) {
                delay(1000L)
                remainingTimeMs -= 1000L
            }

            if (remainingTimeMs <= 0) {
                timerRunning = false
                winnerTeam = when {
                    targetsRed > targetsBlue -> "RED"
                    targetsBlue > targetsRed -> "BLUE"
                    else -> "DRAW"
                }
            }
        }
    }


    // --- Game cancelled dialog ---
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
            onDismiss = { /* non dismissable */ },
            icon = Icons.Default.WarningAmber,
            titleColor = MaterialTheme.colorScheme.error
        )
    }

    // --- Location updates ---
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
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                mapViewModel.stopMoving()
            }
        }
    }

    // --- Camera follow player (leggero) ---
    LaunchedEffect(playerPosition) {
        playerPosition?.let {
            if (isFirstCameraMove) {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 20f))
                isFirstCameraMove = false
            } else {
                // Non seguire automaticamente la camera se non è la prima mossa
                // cameraPositionState.animate(CameraUpdateFactory.newLatLng(it), 100)
            }
        }
    }

    val teamColor = if (team == "BLUE") Color(0xFF00E5FF) else Color(0xFFFF4081)
    val teamHue = if (team == "BLUE") BitmapDescriptorFactory.HUE_CYAN else BitmapDescriptorFactory.HUE_ROSE
    val myColorInt = remember(team) { if (team == "BLUE") 0xFF00E5FF.toInt() else 0xFFFF4081.toInt() }

    // --------------------------
    // ✅ AVATAR CACHE (NO LAG)
    // --------------------------

    var myAvatarCache by remember { mutableStateOf<AvatarCache?>(null) }
    val targetIconCache = remember { mutableStateMapOf<String, BitmapDescriptor>() }
    val baseTowerBitmap by remember {
        mutableStateOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.torre_red)
        )
    }

    LaunchedEffect(avatarSeed, username, myColorInt) {
        myAvatarCache = null
        avatarSeed?.let { seed ->
            val request = ImageRequest.Builder(context)
                .data("https://api.dicebear.com/7.x/pixel-art/png?seed=$seed")
                .allowHardware(false)
                .target { result ->
                    val original = (result as BitmapDrawable).bitmap
                    myAvatarCache = AvatarCache(
                        withName = getAvatarWithBorderAndName(original, myColorInt, username, true),
                        withoutName = getAvatarWithBorderAndName(original, myColorInt, username, false)
                    )
                }
                .build()
            context.imageLoader.enqueue(request)
        }
    }

    // Altri giocatori: cache per username -> (seed, AvatarCache)
    val otherPlayersCache = remember { mutableStateMapOf<String, Pair<String, AvatarCache>>() }

    LaunchedEffect(otherPlayers) {
        otherPlayers.forEach { p ->
            val seed = p.avatar_seed ?: return@forEach
            val key = p.username

            val existing = otherPlayersCache[key]
            if (existing != null && existing.first == seed) return@forEach // già ok

            val colorInt = if (p.team == "BLUE") 0xFF00E5FF.toInt() else 0xFFFF4081.toInt()

            val request = ImageRequest.Builder(context)
                .data("https://api.dicebear.com/7.x/pixel-art/png?seed=$seed")
                .allowHardware(false)
                .target { result ->
                    val original = (result as BitmapDrawable).bitmap
                    val cache = AvatarCache(
                        withName = getAvatarWithBorderAndName(original, colorInt, p.username, true),
                        withoutName = getAvatarWithBorderAndName(original, colorInt, p.username, false)
                    )
                    otherPlayersCache[key] = seed to cache
                }
                .build()

            context.imageLoader.enqueue(request)
        }
    }

    // Bitmap scelta in base allo zoom: istantaneo (non crea bitmap)
    val myMarkerBitmap: Bitmap? = remember(myAvatarCache, shouldShowNames) {
        myAvatarCache?.let { if (shouldShowNames) it.withName else it.withoutName }
    }

    // --------------------------
    // Minigame overlay (come tuo)
    // --------------------------
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
                            mapViewModel.setTargetCooldown(target.id, 2 * 60 * 1000)
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

    // --- DIALOG CONFERMA DIFFICOLTÀ ---
    if (showDifficultyDialog && bombDifficulty != null) {
        CyberpunkDialog(
            title = "INCOMING HACK",
            message = "Accelerometer sensitivity for this hack will be set to ${String.format(Locale.US, "%.2f", bombDifficulty!!.difficulty_multiplier)}x based on your team's ${bombDifficulty!!.team_targets} controlled targets. Proceed?",
            confirmText = "START HACK",
            dismissText = "ABORT",
            onConfirm = {
                showDifficultyDialog = false
                showMinigame = true // Avvia il minigioco
                mapViewModel.clearBombDifficulty() // Pulisci lo stato
            },
            onDismiss = {
                showDifficultyDialog = false
                mapViewModel.clearBombDifficulty() // Pulisci lo stato
            },
            icon = Icons.Default.Sensors
        )
    }

    // --------------------------
    // UI + MAP
    // --------------------------

    Box(modifier = Modifier.fillMaxSize()) {
        if (playerPosition != null) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapStyleOptions = customMapStyle
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                // --- ME ---
                if (myMarkerBitmap != null) {
                    Marker(
                        state = MarkerState(position = playerPosition),
                        icon = BitmapDescriptorFactory.fromBitmap(myMarkerBitmap!!),
                        alpha = markerAlpha,
                        anchor = if (shouldShowNames)
                            androidx.compose.ui.geometry.Offset(0.5f, 0.8f)
                        else
                            androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                    )
                } else {
                    Marker(
                        state = MarkerState(position = playerPosition),
                        title = username,
                        icon = BitmapDescriptorFactory.defaultMarker(teamHue)
                    )
                }

                // --- OTHER PLAYERS ---
                otherPlayers.forEach { p ->
                    val pos = LatLng(p.lat ?: 0.0, p.lon ?: 0.0)
                    val cachePair = otherPlayersCache[p.username]
                    val cache = cachePair?.second
                    val bmp = if (shouldShowNames) cache?.withName else cache?.withoutName

                    if (bmp != null) {
                        Marker(
                            state = MarkerState(position = pos),
                            icon = BitmapDescriptorFactory.fromBitmap(bmp),
                            alpha = markerAlpha,
                            anchor = if (shouldShowNames)
                                androidx.compose.ui.geometry.Offset(0.5f, 0.65f)
                            else
                                androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                        )
                    }
                }

                // --- TARGETS ---
                val torreRedIcon = remember {
                    resizedBitmapDescriptor(context, R.drawable.torre_red, sizeDp = 200)
                }




                targets.forEach { target ->
                    val colorInt = when (target.owner) {
                        "BLUE" -> 0xFF00E5FF.toInt()
                        "RED" -> 0xFFFF4081.toInt()
                        else -> 0xFFFFD600.toInt() // NEUTRAL
                    }

                    val cacheKey = "${target.id}_${target.owner}"

                    val icon = targetIconCache.getOrPut(cacheKey) {
                        BitmapDescriptorFactory.fromBitmap(
                            getTargetWithBorder(baseTowerBitmap, colorInt)
                        )
                    }

                    Marker(
                        state = MarkerState(position = LatLng(target.lat, target.lon)),
                        title = target.name,
                        icon = icon,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.75f)
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

        // --- WAITING OVERLAY ---
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
                        text = "WAITING FOR OPPONENTS...",
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

        // --- TOP HUD ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))) {
                    Text(
                        text = "OPERATOR: TEAM $team",
                        color = teamColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                FloatingActionButton(
                    onClick = { showLogoutDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (timerRunning) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                    border = BorderStroke(1.dp, teamColor)
                ) {
                    Text(
                        text = "⏱ ${formatTime(remainingTimeMs)}",
                        color = if (remainingTimeMs <= 30_000) Color.Red else Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))



            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Blue.copy(alpha = 0.8f)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "BLUE: $targetsBlue",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                    Text(
                        text = "RED: $targetsRed",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // --- LOBBY BADGE & MY LOCATION BUTTON ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 48.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    playerPosition?.let {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(it, cameraPositionState.position.zoom),
                                1000
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on me")
            }
        }


        // --- NEARBY TARGET CARD ---
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
                                    currentMinigameTargetName = target.name
                                    if (isNeutral) {
                                        val colors = listOf("RED", "GREEN", "BLUE")
                                        currentMinigameColor = colors[Random.nextInt(colors.size)]
                                        showMinigame = true
                                    } else {
                                        if (isCooldown) {
                                            Toast.makeText(context, "Access Denied: Security Cooldown Active", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Avvia la logica per la difficoltà
                                            mapViewModel.fetchBombDifficulty()
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

        // --- JOYSTICK ---
        if (playerPosition != null && targets.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
            ) {
                Joystick(onMove = { dx, dy -> mapViewModel.startMoving(dx, dy) })
            }
        }

        // --- LOGOUT DIALOG ---
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

        // --- GAME OVER SCREEN ---
        if (winnerTeam != null) {
            GameOverScreen(
                winnerTeam = winnerTeam!!,
                onDismiss = { /* Non-dismissable */ }
            )

            LaunchedEffect(winnerTeam) {
                delay(5000L) // Aspetta 5 secondi
                mapViewModel.leaveLobby()
                onLogout()
            }
        }

    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


@Composable
private fun FabAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) { Icon(imageVector = icon, contentDescription = label) }

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
        ) {
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
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = titleColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

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
