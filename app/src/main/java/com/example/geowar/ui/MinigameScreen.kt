package com.example.geowar.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MinigameScreen(
    targetName: String = "TARGET SCONOSCIUTO", // Nome del target (default per test)
    onWin: () -> Unit,
    onLose: () -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    // Game State
    var ballPosition by remember { mutableStateOf(Offset.Zero) } // Relative to center
    var velocity by remember { mutableStateOf(Offset.Zero) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isRunning by remember { mutableStateOf(true) }
    var showWinDialog by remember { mutableStateOf(false) } // Controllo del popup di vittoria
    
    // Difficulty Settings
    var sensitivity by remember { mutableFloatStateOf(1.5f) } 
    val friction = 0.95f
    val ballRadius = 20.dp
    val targetRadius = 60.dp

    DisposableEffect(sensitivity) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && isRunning) {
                    val ax = -event.values[0] 
                    val ay = event.values[1] 

                    // Update velocity using dynamic sensitivity
                    velocity += Offset(ax * sensitivity, ay * sensitivity)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    // Physics Loop
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(16) // ~60 FPS
            
            // Apply Velocity
            ballPosition += velocity
            
            // Apply Friction
            velocity *= friction
            
            // Bounds check (soft bounce or clamp)
            val maxDist = 400f
            if (ballPosition.x < -maxDist) { ballPosition = ballPosition.copy(x = -maxDist); velocity = velocity.copy(x = -velocity.x * 0.5f) }
            if (ballPosition.x > maxDist) { ballPosition = ballPosition.copy(x = maxDist); velocity = velocity.copy(x = -velocity.x * 0.5f) }
            if (ballPosition.y < -maxDist) { ballPosition = ballPosition.copy(y = -maxDist); velocity = velocity.copy(y = -velocity.y * 0.5f) }
            if (ballPosition.y > maxDist) { ballPosition = ballPosition.copy(y = maxDist); velocity = velocity.copy(y = -velocity.y * 0.5f) }
        }
    }

    val density = LocalDensity.current
    val targetRadiusPx = with(density) { targetRadius.toPx() }
    
    // Progress Logic
    LaunchedEffect(ballPosition) {
        if (isRunning) {
             val distance = ballPosition.getDistance()
             if (distance < targetRadiusPx) {
                 progress += 0.003f 
             } else {
                 progress -= 0.001f 
             }
             progress = progress.coerceIn(0f, 1f)
             
             if (progress >= 1f) {
                 isRunning = false
                 showWinDialog = true // Mostra il popup invece di uscire subito
             }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DISINNESCA LA BOMBA!", color = Color.Red, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Target: $targetName", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
            Text("Tieni la pallina nel cerchio blu", color = Color.White)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Difficulty Slider
            Text("Sensibilità Accelerometro: ${String.format("%.1f", sensitivity)}", color = Color.Yellow)
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0.5f..5.0f,
                steps = 9,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(20.dp),
                color = Color.Green,
                trackColor = Color.DarkGray,
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // Draw Target Zone
                    drawCircle(
                        color = Color.Blue.copy(alpha = 0.3f),
                        radius = targetRadiusPx,
                        center = center
                    )
                    drawCircle(
                        color = Color.Blue,
                        radius = targetRadiusPx,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    
                    // Draw Ball
                    val ballCenter = center + ballPosition
                    val color = if (ballPosition.getDistance() < targetRadiusPx) Color.Green else Color.Red
                    
                    drawCircle(
                        color = color,
                        radius = ballRadius.toPx(),
                        center = ballCenter
                    )
                    
                    // Draw connecting line if far
                    if (ballPosition.getDistance() > targetRadiusPx) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = center,
                            end = ballCenter,
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
            
            Button(onClick = onLose) { 
                Text("Arrenditi (Esci)")
            }
        }

        // --- POPUP VITTORIA (STILE GEO-WAR) ---
        AnimatedVisibility(
            visible = showWinDialog,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
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
                    // Header
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Green,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "HACK COMPLETATO!",
                                color = Color.Green,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Hai conquistato il target:",
                        color = Color.White
                    )
                    Text(
                        text = targetName,
                        color = Color.Cyan,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Ottimo lavoro agente. La zona è ora sotto il controllo del tuo team.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onWin,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("RITORNA ALLA MAPPA", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
