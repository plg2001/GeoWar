package com.example.geowar.ui.lobby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geowar.ui.theme.TerminalShape

@Composable
fun LobbyScreen(
    username: String,
    onPublicMatchClick: () -> Unit,
    onPrivateMatchClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ACCESS TERMINAL",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "OPERATOR: $username",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // PUBLIC MATCH BUTTON
            Button(
                onClick = onPublicMatchClick,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp),
                shape = TerminalShape(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = "Public Match", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("PUBLIC MATCH", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PRIVATE MATCH BUTTON
            OutlinedButton(
                onClick = onPrivateMatchClick,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp),
                shape = TerminalShape(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Private Match", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "PRIVATE MATCH",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // LOGOUT BUTTON
        TextButton(
            onClick = onLogoutClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOGOUT", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}