package com.example.geowar.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geowar.data.LobbyInfo

@Composable
fun LobbyListScreen(
    lobbyViewModel: LobbyViewModel,
    onLobbyClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val lobbies by lobbyViewModel.lobbies.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AVAILABLE LOBBIES",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lobbies) { lobby ->
                LobbyItem(lobby = lobby, onClick = { onLobbyClick(lobby.id) })
            }
        }
    }
}

@Composable
fun LobbyItem(lobby: LobbyInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lobby #${lobby.id}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lobby.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lobby.status == "WAITING") Color(0xFF4CAF50) else Color(0xFFFFC107)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Counters
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Players
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Players",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = lobby.playerCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.width(12.dp))

                // Red Targets
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Red Targets",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = lobby.targetsRed.toString(), fontSize = 16.sp, color = Color.Red, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.width(8.dp))

                // Blue Targets
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Blue Targets",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Blue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = lobby.targetsBlue.toString(), fontSize = 16.sp, color = Color.Blue, fontWeight = FontWeight.Medium)
            }
        }
    }
}
