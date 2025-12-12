package com.example.geowar.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import com.example.geowar.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    team: String,
    onLogout: () -> Unit, // Callback per il logout
    onAccountClick: () -> Unit // Callback per la schermata account
) {
    val context = LocalContext.current
    val customMapStyle = remember {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf(LatLng(41.8902, 12.4922)) }
    var isFirstLocationReceived by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    val moveStep = 0.00015

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 17f)
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (hasPermission) {
        val locationRequest = remember {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).apply {
                setMinUpdateIntervalMillis(1000L)
            }.build()
        }
        val locationCallback = remember {
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (!isFirstLocationReceived) {
                        locationResult.lastLocation?.let {
                            userLocation = LatLng(it.latitude, it.longitude)
                            isFirstLocationReceived = true
                        }
                    }
                }
            }
        }
        DisposableEffect(fusedLocationClient) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        }
    }

    LaunchedEffect(userLocation, isFirstLocationReceived) {
        if (isFirstLocationReceived) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(userLocation))
        }
    }

    val markerHue = if (team == "BLUE") BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_ROSE
    val teamColor = if (team == "BLUE") Color(0xFF00E5FF) else Color(0xFFFF4081)

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapStyleOptions = customMapStyle
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
        ) {
            Marker(
                state = MarkerState(position = userLocation),
                title = "Tu ($team)",
                icon = BitmapDescriptorFactory.defaultMarker(markerHue)
            )
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(180.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RepeatingDpadButton(Icons.Default.KeyboardArrowUp) { userLocation = LatLng(userLocation.latitude + moveStep, userLocation.longitude) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RepeatingDpadButton(Icons.Default.KeyboardArrowLeft) { userLocation = LatLng(userLocation.latitude, userLocation.longitude - moveStep) }
                    Spacer(Modifier.size(50.dp))
                    RepeatingDpadButton(Icons.Default.KeyboardArrowRight) { userLocation = LatLng(userLocation.latitude, userLocation.longitude + moveStep) }
                }
                RepeatingDpadButton(Icons.Default.KeyboardArrowDown) { userLocation = LatLng(userLocation.latitude - moveStep, userLocation.longitude) }
            }
        }

        // --- ELEGANT FLOATING ACTION BUTTON MENU ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 32.dp)
        ) {
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
                        FabAction(
                            icon = Icons.Default.AccountCircle,
                            label = "Account",
                            onClick = {
                                onAccountClick()
                                isFabMenuExpanded = false
                            }
                        )
                        FabAction(
                            icon = Icons.Default.MyLocation,
                            label = "Centra",
                            onClick = {
                                coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation, 17f)) }
                                isFabMenuExpanded = false
                            }
                        )
                    }
                }

                val rotation by animateFloatAsState(if (isFabMenuExpanded) 45f else 0f, label = "fab_rotation")
                FloatingActionButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Default.Add,
                        "Apri menu azioni",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Abbandonare la partita?") },
                text = { Text("Sei sicuro di voler uscire e tornare al menu principale?") },
                confirmButton = {
                    Button(
                        onClick = { showLogoutDialog = false; onLogout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Abbandona") }
                },
                dismissButton = { TextButton({ showLogoutDialog = false }) { Text("Annulla") } }
            )
        }
    }
}

@Composable
private fun FabAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
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
fun RepeatingDpadButton(icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            while (true) {
                onClick()
                delay(50)
            }
        }
    }

    Surface(
        shape = CircleShape,
        color = Color.DarkGray.copy(alpha = 0.8f),
        contentColor = Color.White,
        modifier = Modifier
            .size(50.dp)
            .clickable(interactionSource = interactionSource, indication = null) {}
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(32.dp))
        }
    }
}
