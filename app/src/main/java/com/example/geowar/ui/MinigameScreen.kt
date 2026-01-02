package com.example.geowar.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.geowar.R
import kotlinx.coroutines.delay

private enum class MinigameState {
    HACKING_SEQUENCE,
    DEFUSE_MINIGAME
}

@Composable
fun HackingProgress(
    targetName: String,
    onHackingComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progressAnimation"
    )

    LaunchedEffect(Unit) {
        val hackingTime = 5000L // 5 seconds
        val steps = 100
        val delayPerStep = hackingTime / steps

        for (i in 1..steps) {
            delay(delayPerStep)
            progress = i / steps.toFloat()
        }
        delay(300) // small delay to show 100%
        onHackingComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Target $targetName found",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Hacking in progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    color = Color.Cyan,
                    trackColor = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ANNULLA")
                }
            }
        }
    }
}


@Composable
fun MinigameScreen(
    targetName: String = "UNKNOWN TARGET",
    onWin: () -> Unit,
    onLose: (Boolean) -> Unit // Boolean indicates if cooldown should be applied
) {

    var minigameState by remember { mutableStateOf(MinigameState.HACKING_SEQUENCE) }

    if (minigameState == MinigameState.HACKING_SEQUENCE) {
        HackingProgress(
            targetName = targetName,
            onHackingComplete = { minigameState = MinigameState.DEFUSE_MINIGAME },
            onCancel = { onLose(false) } // no cooldown
        )
        return // Important to not continue
    }


    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val bombPainter = painterResource(id = R.drawable.video_game_bomb)


    // Game State
    var ballPosition by remember { mutableStateOf(Offset.Zero) } // Relative to center
    var velocity by remember { mutableStateOf(Offset.Zero) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isRunning by remember { mutableStateOf(true) }

    var showWinDialog by remember { mutableStateOf(false) }
    var showLoseDialog by remember { mutableStateOf(false) }

    // Timer State
    var timeLeft by remember { mutableLongStateOf(30L) }

    // Difficulty Settings
    val sensitivity = 1.5f // Hardcoded sensitivity
    val friction = 0.95f
    val ballRadius = 30.dp
    val targetRadius = 60.dp

    DisposableEffect(Unit) { // sensitivity is now a constant, so we can use Unit
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

    // Timer Loop
    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis()
            while (timeLeft > 0 && isRunning) {
                delay(100) // Aggiorna ogni 100ms
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                timeLeft = (30 - elapsed).coerceAtLeast(0)

                if (timeLeft == 0L) {
                    isRunning = false
                    showLoseDialog = true
                }
            }
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
    LaunchedEffect(ballPosition, isRunning) { // Added isRunning to dependencies
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
                showWinDialog = true
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
            Text("DEFUSE THE BOMB!", color = Color.Red, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Target: $targetName", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)

            // Timer Display
            Text(
                text = "Time Left: ${timeLeft}s",
                color = if (timeLeft <= 5) Color.Red else Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text("Keep the bomb inside the blue circle", color = Color.White)

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

                    // Draw Bomb
                    val ballCenter = center + ballPosition
                    val ballRadiusPx = ballRadius.toPx()
                    translate(left = ballCenter.x - ballRadiusPx, top = ballCenter.y - ballRadiusPx) {
                        with(bombPainter) {
                            draw(Size(ballRadiusPx * 2, ballRadiusPx * 2))
                        }
                    }


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

            Button(onClick = { onLose(false) }) {
                Text("Give Up (Exit)")
            }
        }

        // --- POPUP VITTORIA ---
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
                                text = "HACK COMPLETED!",
                                color = Color.Green,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "You conquered the target:",
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
                        text = "Great job agent. The zone is now under your team\'s control.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onWin,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("RETURN TO MAP", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- POPUP SCONFITTA ---
        AnimatedVisibility(
            visible = showLoseDialog,
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
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "HACK FAILED!",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Time expired!",
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "The system detected your intrusion. You must wait before retrying.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )



                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onLose(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("RETURN TO MAP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
