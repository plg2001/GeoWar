package com.example.geowar.ui

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

    var hasPermission by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    
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

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        mapViewModel.reloadAvatar()
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

    // MODIFICA: Logica di aggiornamento camera
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

    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
                        icon = BitmapDescriptorFactory.defaultMarker()
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
                                    // Anche il tasto "Centra" riporta allo zoom massimo
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
