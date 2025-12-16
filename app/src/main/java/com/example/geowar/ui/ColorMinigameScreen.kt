package com.example.geowar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

@Composable
fun ColorMinigameScreen(
    targetColorName: String = "ROSSO", // ROSSO, VERDE, BLU
    onWin: () -> Unit,
    onLose: () -> Unit
) {
    val context = LocalContext.current
    
    // Stato permessi
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        ColorMinigameContent(targetColorName, onWin, onLose)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Permesso fotocamera necessario per questo minigioco.", color = Color.White)
            Button(onClick = onLose) {
                Text("Esci")
            }
        }
    }
}

@Composable
fun ColorMinigameContent(
    targetColorName: String,
    onWin: () -> Unit,
    onLose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var progress by remember { mutableFloatStateOf(0f) }
    var showWinDialog by remember { mutableStateOf(false) }
    var isGameActive by remember { mutableStateOf(true) }

    // Mapping nome -> Hue range (approssimativo)
    val targetHueRange = remember(targetColorName) {
        when (targetColorName.uppercase()) {
            "ROSSO" -> listOf(0f..20f, 330f..360f)
            "VERDE" -> listOf(70f..170f)
            "BLU" -> listOf(190f..270f)
            else -> listOf(0f..360f)
        }
    }
    
    val targetUiColor = remember(targetColorName) {
        when (targetColorName.uppercase()) {
            "ROSSO" -> Color.Red
            "VERDE" -> Color.Green
            "BLU" -> Color.Blue
            else -> Color.White
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Logica vittoria
    LaunchedEffect(progress) {
        if (progress >= 1f && isGameActive) {
            isGameActive = false
            showWinDialog = true
        }
    }

    // Callback per aggiornare lo stato dalla fotocamera (thread-safe)
    val currentProgressUpdater = rememberUpdatedState { newProgress: Float ->
        progress = (progress + newProgress).coerceIn(0f, 1f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. Camera Preview (Background)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analyzer ->
                            analyzer.setAnalyzer(cameraExecutor) { image ->
                                if (!isGameActive) {
                                    image.close()
                                    return@setAnalyzer
                                }
                                
                                val matchPct = calculateColorMatch(image, targetHueRange)
                                image.close()

                                // Update UI
                                val delta = if (matchPct > 0.05) 0.02f else -0.005f
                                
                                // Run on Main Thread
                                previewView.post {
                                    currentProgressUpdater.value(delta)
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("ColorMinigame", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // 2. UI Overlay (Foreground)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TROVA IL COLORE: $targetColorName",
                style = MaterialTheme.typography.headlineMedium,
                color = targetUiColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                color = targetUiColor,
                trackColor = Color.Gray.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onLose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text("Esci")
            }
        }
        
        // 3. Win Dialog (Center) - Inside Box Scope
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
                        text = "Hai identificato il colore:",
                        color = Color.White
                    )
                    Text(
                        text = targetColorName,
                        color = targetUiColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Analisi completata. Il target è stato acquisito.",
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

// Funzione di analisi immagine
private fun calculateColorMatch(image: ImageProxy, targetHueRanges: List<ClosedFloatingPointRange<Float>>): Float {
    val width = image.width
    val height = image.height
    val planes = image.planes
    
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val yRowStride = planes[0].rowStride
    val uvRowStride = planes[1].rowStride
    val uvPixelStride = planes[1].pixelStride

    var matchingPixels = 0
    val step = 10 
    var totalSampled = 0

    for (y in 0 until height step step) {
        for (x in 0 until width step step) {
            // Y index
            val yIndex = y * yRowStride + x
            
            // UV index
            val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride
            
            if (yIndex >= yBuffer.capacity() || uvIndex >= uBuffer.capacity() || uvIndex >= vBuffer.capacity()) continue

            // YUV to RGB
            val Y = (yBuffer.get(yIndex).toInt() and 0xFF)
            val U = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
            val V = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128
            
            val R = (Y + 1.370705 * V).toInt().coerceIn(0, 255)
            val G = (Y - 0.337633 * U - 0.698001 * V).toInt().coerceIn(0, 255)
            val B = (Y + 1.732446 * U).toInt().coerceIn(0, 255)

            // RGB to HSV
            val hsv = FloatArray(3)
            AndroidColor.RGBToHSV(R, G, B, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val valBrightness = hsv[2]

            if (sat > 0.4f && valBrightness > 0.3f) {
                for (range in targetHueRanges) {
                    if (hue in range) {
                        matchingPixels++
                        break
                    }
                }
            }
            totalSampled++
        }
    }
    
    return if (totalSampled > 0) matchingPixels.toFloat() / totalSampled else 0f
}
