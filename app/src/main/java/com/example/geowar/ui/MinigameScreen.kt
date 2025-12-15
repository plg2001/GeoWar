package com.example.geowar.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
    
    // Constants
    val ballRadius = 20.dp
    val targetRadius = 60.dp
    val friction = 0.95f
    val sensitivity = 1.5f 

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && isRunning) {
                    // event.values[0] = x force (sideways)
                    // event.values[1] = y force (longitudinal)
                    
                    // Screen coordinates: +x right, +y down.
                    // Tilt phone right (right side down) -> x sensor < 0? 
                    // Standard: +x is right. Gravity on x when tilted right is negative (gravity points left relative to phone).
                    // Actually, if I tilt right side down, gravity vector has a component pointing to +x of the phone.
                    // So sensor x becomes negative (acceleration = forces - gravity, if stationary, acc = -g). 
                    // Let's rely on testing or standard behavior:
                    // usually -x moves ball left, +x moves ball right.
                    // sensor x is positive when device left side is down. (Gravity pulls right).
                    
                    val ax = -event.values[0] 
                    val ay = event.values[1] 

                    // Update velocity
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
            
            // Bounds check (soft bounce or clamp) - let's just clamp for now to 300px roughly
            // Ideally we need canvas size. For now assume reasonable bounds.
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
                 onWin()
             }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("DISINNESCA LA BOMBA!", color = Color.Red, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Tieni la pallina nel cerchio blu", color = Color.White)
        
        Spacer(modifier = Modifier.height(20.dp))
        
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
}
