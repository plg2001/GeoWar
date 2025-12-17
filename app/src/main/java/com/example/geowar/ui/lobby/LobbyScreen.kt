package com.example.geowar.ui.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LobbyScreen(
    onPublicMatchClick: () -> Unit,
    onPrivateMatchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onPublicMatchClick) {
            Text("PUBLIC MATCH")
        }
        Button(
            onClick = onPrivateMatchClick,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("PRIVATE MATCH")
        }
    }
}