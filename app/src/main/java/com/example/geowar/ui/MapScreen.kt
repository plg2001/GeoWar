package com.example.geowar.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable // <--- AGGIUNTO IMPORT MANCANTE
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    team: String,
    onLogout: () -> Unit // Callback per il logout
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Posizione iniziale
    var userLocation by remember { mutableStateOf(LatLng(41.8902, 12.4922)) }
    var isFirstLocationReceived by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    
    // Stato per il Dialog di Logout
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Step di movimento
    val moveStep = 0.00015 

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
        }
    )

    // Camera State
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 17f)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    // Logica GPS (Solo avvio)
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
            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    // Camera Follow
    LaunchedEffect(userLocation) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLng(userLocation))
    }

    // Configurazione visuale
    val markerHue = if (team == "BLUE") BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_ROSE
    val teamColor = if (team == "BLUE") Color(0xFF00E5FF) else Color(0xFFFF4081)

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false), 
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            )
        ) {
            Marker(
                state = MarkerState(position = userLocation),
                title = "Tu ($team)",
                icon = BitmapDescriptorFactory.defaultMarker(markerHue)
            )
        }
        
        // --- HEADER INFO & LOGOUT ---
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card Team
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Text(
                    text = "OPERATORE: TEAM $team",
                    color = teamColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))

            // Pulsante Logout
            FloatingActionButton(
                onClick = { showLogoutDialog = true },
                containerColor = Color.Red,
                contentColor = Color.White,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
            }
        }

        // --- CONTROLLER GAMEBOY ---
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
                // SU
                RepeatingDpadButton(Icons.Default.KeyboardArrowUp) {
                    userLocation = LatLng(userLocation.latitude + moveStep, userLocation.longitude)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // SX
                    RepeatingDpadButton(Icons.Default.KeyboardArrowLeft) {
                        userLocation = LatLng(userLocation.latitude, userLocation.longitude - moveStep)
                    }
                    Spacer(modifier = Modifier.size(50.dp))
                    // DX
                    RepeatingDpadButton(Icons.Default.KeyboardArrowRight) {
                        userLocation = LatLng(userLocation.latitude, userLocation.longitude + moveStep)
                    }
                }
                
                // GIU
                RepeatingDpadButton(Icons.Default.KeyboardArrowDown) {
                    userLocation = LatLng(userLocation.latitude - moveStep, userLocation.longitude)
                }
            }
        }
        
        // --- DIALOG DI CONFERMA LOGOUT ---
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Abbandonare la partita?") },
                text = { Text("Sei sicuro di voler uscire e tornare al menu principale?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Abbandona")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}

// Composable personalizzato che ripete l'azione mentre il tasto è premuto
@Composable
fun RepeatingDpadButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Logica di ripetizione
    LaunchedEffect(isPressed) {
        if (isPressed) {
            while (true) {
                onClick()
                delay(50) // Velocità di ripetizione (50ms = molto fluido)
            }
        }
    }

    Surface(
        shape = CircleShape,
        color = Color.DarkGray.copy(alpha = 0.8f),
        contentColor = Color.White,
        modifier = Modifier
            .size(50.dp)
            // Colleghiamo l'interazione per rilevare il "press"
            .clickable(
                interactionSource = interactionSource,
                indication = null // Rimuove ripple di default se fastidioso, o usa LocalIndication.current
            ) {}
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// NOTA: Ho rimosso la funzione 'fun Modifier.clickable(...)' duplicata che causava l'errore.
// Ora usiamo quella standard importata da androidx.compose.foundation.clickable
