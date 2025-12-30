package com.example.geowar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun GameOverScreen(
    winnerTeam: String,
    onDismiss: () -> Unit
) {
    val title: String
    val titleColor: Color

    when (winnerTeam) {
        "RED" -> {
            title = "RED TEAM WINS"
            titleColor = Color(0xFFFF4081)
        }
        "BLUE" -> {
            title = "BLUE TEAM WINS"
            titleColor = Color(0xFF00E5FF)
        }
        else -> {
            title = "DRAW"
            titleColor = Color.Yellow
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(2.dp, titleColor),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MATCH FINISHED",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Returning to lobby...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}