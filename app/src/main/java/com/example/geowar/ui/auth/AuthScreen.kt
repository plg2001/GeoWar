package com.example.geowar.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

// 1. Qui ho rimosso @Preview perché questa funzione richiede parametri
@Composable
fun AuthScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: (String, String) -> Unit
)
 {
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Colori tema Cyber
    val darkBg = Color(0xFF0A0E17)
    val neonBlue = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF4081)

    val activeColor = if(isLoginMode) neonBlue else neonPink

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "ACCESS TERMINAL",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Text(
                text = if(isLoginMode) "IDENTITY VERIFICATION" else "NEW AGENT ENLISTMENT",
                style = MaterialTheme.typography.bodySmall,
                color = activeColor,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Toggle Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CutCornerShape(10.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(if (isLoginMode) activeColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { isLoginMode = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("LOGIN", color = if(isLoginMode) activeColor else Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(if (!isLoginMode) activeColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { isLoginMode = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("REGISTER", color = if(!isLoginMode) activeColor else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Inputs
            CyberTextField(
                value = username,
                onValueChange = { username = it },
                label = "CODENAME / USERNAME",
                icon = Icons.Default.Person,
                activeColor = activeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            CyberTextField(
                value = password,
                onValueChange = { password = it },
                label = "PASSCODE",
                icon = Icons.Default.Lock,
                isPassword = true,
                activeColor = activeColor
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Action Button
            Button(
                onClick = { if(isLoginMode) onLoginClick(username,password) else onRegisterClick(username,password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor
                )
            ) {
                Text(
                    text = if(isLoginMode) "INITIALIZE LINK" else "CREATE PROFILE",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    activeColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = activeColor) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeColor,
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = activeColor
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = CutCornerShape(bottomStart = 16.dp, topEnd = 16.dp)
    )
}

// 2. Questa è la funzione ESCLUSIVA per la preview
@Preview(
    showBackground = true,
    name = "Cyber Interface Preview"
)
@Composable
fun AuthScreenPreview() {
    AuthScreen(
        onLoginClick = { _, _ -> },
        onRegisterClick = { _, _ -> }
    )
}