package com.example.geowar.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.geowar.R
import com.example.geowar.data.auth.AuthApi
import com.example.geowar.repository.UserRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBackClick: () -> Unit,
    authApi: AuthApi
) {
    val context = LocalContext.current
    val userRepository = UserRepository(authApi, context)
    val accountViewModel: AccountViewModel = viewModel(factory = AccountViewModelFactory(userRepository))

    val username = accountViewModel.username
    val email = accountViewModel.email
    val avatarSeed = accountViewModel.avatarSeed
    val isLoading = accountViewModel.isLoading
    val errorMessage = accountViewModel.errorMessage

    LaunchedEffect(Unit) {
        accountViewModel.loadUserProfile()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val darkBg = Color(0xFF0A0E17)
    val neonBlue = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF4081)
    val surfaceColor = Color(0xFF151A25)

    Scaffold(
        containerColor = darkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PROFILO UTENTE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro",
                            tint = neonBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkBg
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading && avatarSeed == null) { // Mostra solo al primo caricamento
                CircularProgressIndicator(color = neonBlue)
            } else {
                // --- SEZIONE FOTO PROFILO ---
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, neonBlue, CircleShape)
                ) {
                    AsyncImage(
                        model = "https://api.dicebear.com/7.x/pixel-art/png?seed=${avatarSeed ?: ""}",
                        contentDescription = "Foto Profilo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_launcher_background)
                    )
                    IconButton(
                        onClick = { accountViewModel.generateNewAvatarSeed() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(surfaceColor, CircleShape)
                            .border(1.dp, neonBlue, CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica foto", tint = neonBlue)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SEZIONE DATI UTENTE ---
                Text(
                    "MODIFICA DATI",
                    style = MaterialTheme.typography.titleMedium,
                    color = neonBlue,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { accountViewModel.updateUsername(it) },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getCyberTextFieldColors(neonBlue, surfaceColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { accountViewModel.updateEmail(it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getCyberTextFieldColors(neonBlue, surfaceColor)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- SEZIONE AVATAR DICEBAR ---
                Text(
                    "AVATAR DI GIOCO",
                    style = MaterialTheme.typography.titleMedium,
                    color = neonPink,
                    letterSpacing = 1.sp
                )
                Text(
                    "Questo avatar sarà visibile agli altri giocatori sulla mappa.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(surfaceColor, CutCornerShape(12.dp))
                        .border(1.dp, neonPink.copy(alpha = 0.5f), CutCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://api.dicebear.com/7.x/pixel-art/png?seed=${avatarSeed ?: ""}",
                        contentDescription = "Avatar Preview",
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(id = R.drawable.ic_launcher_background)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { accountViewModel.generateNewAvatarSeed() },
                    shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                    border = BorderStroke(1.dp, neonPink.copy(alpha = 0.7f))
                ) {
                    Text("GENERA NUOVO AVATAR", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- BOTTONE SALVA ---
                Button(
                    onClick = {
                        accountViewModel.saveChanges {
                            Toast.makeText(context, "Profilo aggiornato!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = CutCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = neonBlue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SALVA MODIFICHE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun getCyberTextFieldColors(activeColor: Color, surfaceColor: Color): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = activeColor,
        unfocusedBorderColor = activeColor.copy(alpha = 0.5f),
        focusedLabelColor = activeColor,
        unfocusedLabelColor = Color.Gray,
        cursorColor = activeColor,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = surfaceColor,
        unfocusedContainerColor = surfaceColor,
    )
}

// Factory per il ViewModel
class AccountViewModelFactory(private val userRepository: UserRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
