package com.example.geowar

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geowar.data.auth.UserResponse
import com.example.geowar.ui.LandingScreen
import com.example.geowar.ui.MapScreen
import com.example.geowar.ui.TeamSelectionScreen
import com.example.geowar.ui.admin.AdminScreen
import com.example.geowar.ui.auth.AuthScreen
import com.example.geowar.ui.auth.AuthViewModel
import com.example.geowar.ui.theme.GeoWarTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import androidx.core.content.edit
import java.util.UUID


class MainActivity : ComponentActivity() {

    // =====================================
    // GOOGLE SIGN-IN MANAGER
    // =====================================
    private val credentialManager by lazy {
        CredentialManager.create(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPref = getPreferences(Context.MODE_PRIVATE)

        setContent {
            GeoWarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: AuthViewModel = viewModel()

                    var currentUserId by remember { mutableStateOf<Int?>(null) }

                    NavHost(navController = navController, startDestination = "landing") {

                        // -------------------------
                        // 1. Landing Screen
                        // -------------------------
                        composable("landing") {
                            LandingScreen(
                                onStartClick = {
                                    val savedTeam = sharedPref.getString("TEAM", null)
                                    val savedUserId = sharedPref.getInt("USER_ID", -1)
                                    
                                    if (savedTeam != null && savedUserId != -1) {
                                        currentUserId = savedUserId
                                        navController.navigate("map/$savedTeam") {
                                            popUpTo("landing") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("auth")
                                    }
                                }
                            )
                        }

                        // -------------------------
                        // 2. Auth Screen
                        // -------------------------
                        composable("auth") {
                            var isLoading by remember { mutableStateOf(false) }

                            // Funzione helper per gestire il post-login
                            fun handleLoginSuccess(userResponse: UserResponse) {
                                currentUserId = userResponse.id
                                sharedPref.edit { 
                                    putInt("USER_ID", userResponse.id) 
                                }

                                Toast.makeText(
                                    this@MainActivity,
                                    "Benvenuto ${userResponse.username}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                if (userResponse.admin) {
                                    // Se è ADMIN, vai alla dashboard
                                    navController.navigate("admin_dashboard") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                } else {
                                    // Se è utente normale, vai alla selezione team
                                    navController.navigate("team_selection")
                                }
                            }

                            AuthScreen(
                                onLoginClick = { user, pass ->
                                    isLoading = true
                                    viewModel.login(user, pass) { userResponse, msg ->
                                        isLoading = false
                                        if (userResponse != null) {
                                            handleLoginSuccess(userResponse)
                                        } else {
                                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onRegisterClick = { user, pass, email ->
                                    isLoading = true
                                    viewModel.register(user, pass, email) { userResponse, msg ->
                                        isLoading = false
                                        if (userResponse != null) {
                                            handleLoginSuccess(userResponse)
                                        } else {
                                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onGoogleClick = {
                                    signInWithGoogle { googleIdToken ->
                                        isLoading = true
                                        viewModel.signInWithGoogleToken(googleIdToken) { userResponse, msg ->
                                            isLoading = false
                                            if (userResponse != null) {
                                                handleLoginSuccess(userResponse)
                                            } else {
                                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                isLoading = isLoading
                            )
                        }

                        // -------------------------
                        // 3. Team Selection
                        // -------------------------
                        composable("team_selection") {
                            TeamSelectionScreen(
                                onTeamSelected = { team ->
                                    sharedPref.edit { putString("TEAM", team) }

                                    if (currentUserId != null) {
                                        viewModel.setTeam(currentUserId!!, team) { _, _ -> }
                                    }

                                    navController.navigate("map/$team") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // -------------------------
                        // 4. Map Screen
                        // -------------------------
                        composable("map/{team}") { backStackEntry ->
                            val team = backStackEntry.arguments?.getString("team") ?: "UNKNOWN"
                            MapScreen(
                                team = team,
                                onLogout = {
                                    sharedPref.edit { clear() }
                                    navController.navigate("auth") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // -------------------------
                        // 5. Admin Dashboard
                        // -------------------------
                        composable("admin_dashboard") {
                            AdminScreen(
                                onLogout = {
                                    // Logout semplice: pulisci le preferenze e torna all'auth
                                    sharedPref.edit { clear() }
                                    navController.navigate("auth") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    // =====================================================
    //                GOOGLE SIGN-IN FUNCTIONS
    // =====================================================

    private fun buildSignInRequest(): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("143510152058-65kf5bucon42l77e7qk1bsgl70qki9so.apps.googleusercontent.com")
            .setAutoSelectEnabled(false)
            .setNonce(UUID.randomUUID().toString())
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    private fun signInWithGoogle(onSuccess: (String) -> Unit) {
        val request = buildSignInRequest()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )

                handleGoogleCredential(result.credential, onSuccess)

            } catch (e: NoCredentialException) {
                Log.e("GOOGLE_SIGN_IN", "No credentials available", e)
                Toast.makeText(this@MainActivity, "Errore config: controlla SHA-1 o aggiungi account Google", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialException) {
                Log.e("GOOGLE_SIGN_IN", "Sign-in cancelled or failed", e)
                Toast.makeText(this@MainActivity, "Accesso annullato", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("GOOGLE_SIGN_IN", "Generic Exception: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Errore generico Sign-In", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleCredential(credential: Credential, onSuccess: (String) -> Unit) {
        when (credential) {
            is GoogleIdTokenCredential -> {
                val googleIdToken = credential.idToken
                Log.d("GOOGLE_SIGN_IN", "Got Google ID token: $googleIdToken")
                onSuccess(googleIdToken)
            }
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val googleIdToken = googleIdTokenCredential.idToken
                        Log.d("GOOGLE_SIGN_IN", "Got Google ID token from CustomCredential: $googleIdToken")
                        onSuccess(googleIdToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("GOOGLE_SIGN_IN", "Received an invalid Google ID token response", e)
                    }
                } else {
                    Log.e("GOOGLE_SIGN_IN", "Unexpected CustomCredential type: ${credential.type}")
                }
            }
            else -> {
                Log.e("GOOGLE_SIGN_IN", "Unexpected credential type: ${credential::class.java.name}")
            }
        }
    }
}
