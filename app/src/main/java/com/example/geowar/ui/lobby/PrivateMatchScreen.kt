
package com.example.geowar.ui.lobby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.geowar.ui.theme.TerminalShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateMatchScreen(
    navController: NavController,
    onJoinLobby: (String) -> Unit,
    onCreateLobby: () -> Unit,
    lobbyCode: String?,
    onLobbyCodeDialogDismiss: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinLobbyCode by remember { mutableStateOf("") }

    if (lobbyCode != null) {
        AlertDialog(
            onDismissRequest = onLobbyCodeDialogDismiss,
            shape = TerminalShape(),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("PRIVATE LOBBY CREATED") },
            text = { Text("CODE: $lobbyCode") },
            confirmButton = {
                Button(
                    onClick = onLobbyCodeDialogDismiss,
                    shape = TerminalShape(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CLOSE")
                }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            shape = TerminalShape(),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("JOIN PRIVATE LOBBY") },
            text = {
                OutlinedTextField(
                    value = joinLobbyCode,
                    onValueChange = { joinLobbyCode = it },
                    label = { Text("LOBBY CODE") },
                    singleLine = true,
                    shape = TerminalShape(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onJoinLobby(joinLobbyCode)
                        showJoinDialog = false
                    },
                    shape = TerminalShape(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("JOIN")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showJoinDialog = false },
                    shape = TerminalShape()
                ) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PRIVATE OPERATIONS",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onCreateLobby() },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(60.dp),
            shape = TerminalShape(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = "Create Private Lobby", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("CREATE PRIVATE LOBBY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showJoinDialog = true },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(60.dp),
            shape = TerminalShape(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = "Join Private Lobby", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "JOIN PRIVATE LOBBY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
