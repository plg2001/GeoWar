package com.example.geowar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview // Importante per la preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TeamSelectionScreen(onTeamSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CHOOSE YOUR ALLEGIANCE",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Light
            )
        }

        // Teams Container
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RED TEAM CARD
            TeamCard(
                name = "IMPERIUM",
                color = Color(0xFFD32F2F),
                secondaryColor = Color(0xFFFF5252),
                description = "Strength. Order. Conquest.\nDominating the map through aggressive expansion.",
                icon = Icons.Default.Warning,
                alignment = Alignment.Start,
                onClick = { onTeamSelected("RED") },
                modifier = Modifier.weight(1f)
            )

            // BLUE TEAM CARD
            TeamCard(
                name = "REBELLION",
                color = Color(0xFF1976D2),
                secondaryColor = Color(0xFF448AFF),
                description = "Liberty. Strategy. Defense.\nProtecting the zones from total control.",
                icon = Icons.Default.Star,
                alignment = Alignment.End,
                onClick = { onTeamSelected("BLUE") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TeamCard(
    name: String,
    color: Color,
    secondaryColor: Color,
    description: String,
    icon: ImageVector,
    alignment: Alignment.Horizontal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CutCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.4f))
                )
            )
            .border(2.dp, secondaryColor, CutCornerShape(32.dp))
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        // Background Icon faded
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(150.dp)
                .alpha(0.1f),
            tint = Color.White
        )

        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "TAP TO JOIN >>>",
                style = MaterialTheme.typography.labelLarge,
                color = secondaryColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ----------------------------------------------------------------
// AGGIUNGI QUESTO ALLA FINE DEL FILE PER VEDERE LA PREVIEW
// ----------------------------------------------------------------

@Preview(
    showBackground = true,
    name = "Team Selection Preview",
    device = "spec:width=411dp,height=891dp" // Simula un telefono grande
)
@Composable
fun TeamSelectionScreenPreview() {
    // Passiamo una lambda vuota {} perché in preview non ci serve navigare davvero
    TeamSelectionScreen(
        onTeamSelected = { teamName ->
            // Questo print appare nei log se usi la Interactive Mode
            println("Preview selection: $teamName")
        }
    )
}