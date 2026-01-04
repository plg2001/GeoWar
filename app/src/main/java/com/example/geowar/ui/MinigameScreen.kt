package com.example.geowar.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Colori Cyberpunk
val NeonCyan = Color(0xFF00FBFF)
val NeonPink = Color(0xFFFF00AE)
val DarkBackground = Color(0xFF0A0E14)
val GridColor = Color(0xFF1A2633)

enum class MinigameState {
    HACKING_SEQUENCE,
    DEFUSE_MINIGAME
}

@Composable
fun HackingProgress(
    targetName: String,
    onHackingComplete: () -> Unit,
    onCancel: () -> Unit
) {
    // Simulate hacking progress
    LaunchedEffect(Unit) {
        delay(2000) // Simulate a 2-second hack
        onHackingComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HACKING IN PROGRESS...",
                color = NeonCyan,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = NeonPink)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TARGET: $targetName",
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
            ) {
                Text("CANCEL HACK")
            }
        }
    }
}


@Composable
fun MinigameScreen(
    targetName: String = "UNKNOWN TARGET",
    onWin: () -> Unit,
    onLose: (Boolean) -> Unit
) {
    var minigameState by remember { mutableStateOf(MinigameState.HACKING_SEQUENCE) }

    if (minigameState == MinigameState.HACKING_SEQUENCE) {
        HackingProgress(
            targetName = targetName,
            onHackingComplete = { minigameState = MinigameState.DEFUSE_MINIGAME },
            onCancel = { onLose(false) }
        )
        return
    }

    // --- LOGICA ORIGINALE PRESERVATA ---
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var ballPosition by remember { mutableStateOf(Offset.Zero) }
    var velocity by remember { mutableStateOf(Offset.Zero) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isRunning by remember { mutableStateOf(true) }
    var showWinDialog by remember { mutableStateOf(false) }
    var showLoseDialog by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableLongStateOf(30L) }

    val sensitivity = 1.8f
    val friction = 0.94f
    val ballRadius = 25.dp
    val targetRadius = 70.dp

    // --- ANIMAZIONI ESTETICHE ---
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "radar_rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000), repeatMode = RepeatMode.Reverse), label = "glow_pulse"
    )

    // Logica sensori e loop (invariata)
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && isRunning) {
                    velocity += Offset(-event.values[0] * sensitivity, event.values[1] * sensitivity)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis()
            while (timeLeft > 0 && isRunning) {
                delay(100)
                timeLeft = (30 - (System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(0)
                if (timeLeft == 0L) { isRunning = false; showLoseDialog = true }
            }
        }
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(16)
            ballPosition += velocity
            velocity *= friction
            val maxDist = 450f
            if (ballPosition.x.coerceIn(-maxDist, maxDist) != ballPosition.x) velocity = velocity.copy(x = -velocity.x * 0.5f)
            if (ballPosition.y.coerceIn(-maxDist, maxDist) != ballPosition.y) velocity = velocity.copy(y = -velocity.y * 0.5f)
            ballPosition = Offset(ballPosition.x.coerceIn(-maxDist, maxDist), ballPosition.y.coerceIn(-maxDist, maxDist))
        }
    }

    val density = LocalDensity.current
    val targetRadiusPx = with(density) { targetRadius.toPx() }

    LaunchedEffect(ballPosition, isRunning) {
        if (isRunning) {
            if (ballPosition.getDistance() < targetRadiusPx) progress += 0.005f else progress -= 0.002f
            progress = progress.coerceIn(0f, 1f)
            if (progress >= 1f) { isRunning = false; showWinDialog = true }
        }
    }

    if (showWinDialog) {
        AlertDialog(
            onDismissRequest = {
                showWinDialog = false
                onWin()
            },
            title = { Text("WIN", color = NeonCyan) },
            text = { Text("Hack completed perfectly") },
            confirmButton = {
                Button(onClick = {
                    showWinDialog = false
                    onWin()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showLoseDialog) {
        AlertDialog(
            onDismissRequest = {
                showLoseDialog = false
                onLose(true)
            },
            title = { Text("LOSE", color = NeonPink) },
            text = { Text("Hai perso il minigioco.") },
            confirmButton = {
                Button(onClick = {
                    showLoseDialog = false
                    onLose(true)
                }) {
                    Text("OK")
                }
            }
        )
    }

    // --- UI DESIGN ---
    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // 1. Griglia di Sfondo
        DigitalGrid()

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Estetico
            Text(
                "SYSTEM OVERRIDE",
                color = NeonCyan,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 4.sp
            )

            Text(
                text = if (timeLeft < 10) "00:0${timeLeft}" else "00:${timeLeft}",
                color = if (timeLeft < 10) NeonPink else Color.White,
                fontSize = 48.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )

            // Barra di caricamento Cyber
            CyberProgressBar(progress)

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Area di gioco Canvas
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)

                    // Disegna Radar/Target
                    drawRadar(center, targetRadiusPx, rotation, pulse)

                    // Disegna la "Bomba" (Nucleo Energetico)
                    val ballCenter = center + ballPosition
                    drawBombCore(ballCenter, with(density) { ballRadius.toPx() }, pulse)

                    // Linea di aggancio se fuori target
                    if (ballPosition.getDistance() > targetRadiusPx) {
                        drawLine(
                            brush = Brush.linearGradient(listOf(NeonCyan, Color.Transparent)),
                            start = center,
                            end = ballCenter,
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }
                }
            }

            Text(
                "STABILIZE THE CORE",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = { onLose(false) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
            ) {
                Text("ABORT MISSION", color = Color.Gray)
            }
        }
    }
}

@Composable
fun DigitalGrid() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        for (x in 0..size.width.toInt() step step.toInt()) {
            drawLine(GridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
        }
        for (y in 0..size.height.toInt() step step.toInt()) {
            drawLine(GridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
        }
    }
}

@Composable
fun CyberProgressBar(progress: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("STABILITY", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
            Text("${(progress * 100).toInt()}%", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(12.dp).background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(NeonCyan.copy(0.5f), NeonCyan)))
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadar(
    center: Offset,
    radius: Float,
    rotation: Float,
    pulse: Float
) {
    // Cerchio esterno pulsante
    drawCircle(
        color = NeonCyan.copy(alpha = 0.1f * pulse),
        radius = radius + (20f * pulse),
        center = center
    )

    // Anello principale
    drawCircle(
        color = NeonCyan,
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // Linee rotanti del radar
    rotate(rotation, center) {
        drawArc(
            brush = Brush.sweepGradient(listOf(Color.Transparent, NeonCyan.copy(alpha = 0.4f))),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        // Mirini rotanti
        for (i in 0..3) {
            rotate(i * 90f, center) {
                drawLine(
                    color = NeonCyan,
                    start = Offset(center.x + radius - 20f, center.y),
                    end = Offset(center.x + radius + 10f, center.y),
                    strokeWidth = 4f
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBombCore(
    center: Offset,
    radius: Float,
    pulse: Float
) {
    // Glow esterno
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(NeonPink.copy(alpha = 0.6f * pulse), Color.Transparent),
            center = center,
            radius = radius * 3f
        ),
        radius = radius * 3f,
        center = center
    )

    // Core centrale
    drawCircle(
        color = Color.White,
        radius = radius * 0.4f,
        center = center
    )

    drawCircle(
        color = NeonPink,
        radius = radius * 0.8f,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
}