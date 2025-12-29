
package com.example.geowar.ui.lobby

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateMatchScreen(
    navController: NavController,
    onJoinLobby: (String) -> Unit,
    onCreateLobby: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var lobbyCode by remember { mutableStateOf("") }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Private Lobby") },
            text = {
                OutlinedTextField(
                    value = lobbyCode,
                    onValueChange = { lobbyCode = it },
                    label = { Text("Lobby Code") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onJoinLobby(lobbyCode)
                        showJoinDialog = false
                    }
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onCreateLobby() },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Create Private Lobby")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showJoinDialog = true },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Join Private Lobby")
        }
    }
}
