package com.example.geowar.ui

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(team: String) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf(LatLng(41.8902, 12.4922)) } // Default Roma
    var hasPermission by remember { mutableStateOf(false) }

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

    // 1. Chiede il permesso all'avvio
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    // 2. 🟢 AGGIORNAMENTO AUTOMATICO: Inizia a ricevere la posizione in tempo reale se il permesso è concesso
    if (hasPermission) {
        val locationRequest = remember {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // Ogni 5 secondi
                .build()
        }

        val locationCallback = remember {
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        // Aggiorna la posizione del giocatore sulla mappa
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            }
        }

        // 3. Gestisce il ciclo di vita: avvia/ferma gli aggiornamenti quando la mappa è visibile/nascosta
        DisposableEffect(fusedLocationClient) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    // 4. Anima la camera per seguire il giocatore
    LaunchedEffect(userLocation) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLng(userLocation))
    }

    // Colori Team
    val teamColor = if (team == "BLUE") Color(0xFF00E5FF) else Color(0xFFFF4081)
    val markerHue = if (team == "BLUE") BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_ROSE

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true), // Ora usiamo il pallino blu nativo e preciso
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
        ) {
            // Potremmo anche non usare un Marker custom e affidarci a quello di `isMyLocationEnabled`,
            // ma lo teniamo per coerenza di stile se volessimo personalizzarlo.
            Marker(
                state = MarkerState(position = userLocation),
                title = "Tu ($team)",
                snippet = "Posizione attuale",
                icon = BitmapDescriptorFactory.defaultMarker(markerHue)
            )
        }
        
        // Header info
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp), // Spazio per status bar
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
        ) {
            Text(
                text = "OPERATORE: TEAM $team",
                color = teamColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
