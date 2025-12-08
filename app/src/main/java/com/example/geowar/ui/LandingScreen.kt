package com.example.geowar.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview // Import necessario
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LandingScreen(onStartClick: () -> Unit) {
    // Animazione pulsing per il testo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Colori delle squadre
    val redColor = Color(0xFFD32F2F)
    val blueColor = Color(0xFF1976D2)
    val darkBg = Color(0xFF0A0E17)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        darkBg,
                        Color(0xFF16213E), // Deep Blueish transition
                        Color.Black
                    )
                )
            )
            .clickable { onStartClick() },
        contentAlignment = Alignment.Center
    ) {
        // Overlay sfumato con i colori delle squadre per atmosfera
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            blueColor.copy(alpha = 0.1f),
                            Color.Transparent,
                            redColor.copy(alpha = 0.1f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Logo del gioco (Uso Icona Vettoriale sicura anti-crash)
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "GeoWar Logo",
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GEO WAR",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "TAP TO START",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = alpha),
                    letterSpacing = 2.sp
                )
            )
        }
    }
}

// -----------------------------------------------------------
// AGGIUNGI QUESTO PER LA PREVIEW
// -----------------------------------------------------------

@Preview(
    showBackground = true,
    name = "Landing Screen Preview",
    device = "id:pixel_5" // Mostra la preview con le dimensioni di un Pixel 5
)
@Composable
fun LandingScreenPreview() {
    LandingScreen(
        onStartClick = {
            // Simuliamo il click in preview (stampa nei log se usi Interactive Mode)
            println("Start clicked in Preview")
        }
    )
}