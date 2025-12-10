package com.example.geowar.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: (String, String, String) -> Unit,
    onGoogleClick: () -> Unit,
    isLoading: Boolean = false
) {

    var isLoginMode by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val darkBg = Color(0xFF0A0E17)
    val neonBlue = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF4081)

    val activeColor = if (isLoginMode) neonBlue else neonPink

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ACCESS TERMINAL",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Text(
                text = if (isLoginMode) "IDENTITY VERIFICATION" else "NEW AGENT ENLISTMENT",
                style = MaterialTheme.typography.bodySmall,
                color = activeColor,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ───────────────────────────────────────────────
            // TOGGLE LOGIN / REGISTER
            // ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CutCornerShape(10.dp))
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize()
                        .background(if (isLoginMode) activeColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            isLoginMode = true
                            errorMessage = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("LOGIN", color = if (isLoginMode) activeColor else Color.Gray)
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxSize()
                        .background(if (!isLoginMode) activeColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            isLoginMode = false
                            errorMessage = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("REGISTER", color = if (!isLoginMode) activeColor else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ───────────────────────────────────────────────
            // USERNAME
            // ───────────────────────────────────────────────
            CyberTextField(
                value = username,
                onValueChange = { username = it },
                label = "USERNAME",
                icon = Icons.Default.Person,
                activeColor = activeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ───────────────────────────────────────────────
            // EMAIL (REGISTER)
            // ───────────────────────────────────────────────
            if (!isLoginMode) {
                CyberTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "SECURE EMAIL",
                    icon = Icons.Default.Email,
                    activeColor = activeColor,
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ───────────────────────────────────────────────
            // PASSWORD
            // ───────────────────────────────────────────────
            CyberTextField(
                value = password,
                onValueChange = { password = it },
                label = "PASSWORD",
                icon = Icons.Default.Lock,
                isPassword = true,
                activeColor = activeColor
            )

            // CONFIRM PASSWORD
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(16.dp))
                CyberTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "CONFIRM PASSWORD",
                    icon = Icons.Default.Check,
                    isPassword = true,
                    activeColor = activeColor
                )
            }

            // ERROR MESSAGE
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage!!, color = Color.Red)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ───────────────────────────────────────────────
            // MAIN BUTTON LOGIN / REGISTER
            // ───────────────────────────────────────────────
            if (isLoading) {
                CircularProgressIndicator(color = activeColor)
            } else {

                Button(
                    onClick = {
                        errorMessage = null
                        if (isLoginMode) {
                            if (username.isNotEmpty() && password.isNotEmpty()) {
                                onLoginClick(username, password)
                            } else errorMessage = "Fill all fields"
                        } else {
                            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || confirmPassword.isEmpty()) {
                                errorMessage = "All fields required"
                            } else if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                            } else {
                                onRegisterClick(username, password, email)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text(
                        text = if (isLoginMode) "INITIALIZE LINK" else "CREATE PROFILE",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ───────────────────────────────────────────────
                // GOOGLE SIGN-IN BUTTON
                // ───────────────────────────────────────────────
                Button(
                    onClick = { onGoogleClick() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Continue with Google",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}



// ===================================================================
// SUPPORT TEXT FIELD
// ===================================================================
@Composable
fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    activeColor: Color,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = activeColor) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
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

