package com.example.geowar

import android.content.Context
import android.os.Bundle
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
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geowar.ui.LandingScreen
import com.example.geowar.ui.MapScreen
import com.example.geowar.ui.TeamSelectionScreen
import com.example.geowar.ui.auth.AuthScreen
import com.example.geowar.ui.auth.AuthViewModel
import com.example.geowar.ui.theme.GeoWarTheme
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch



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

                            AuthScreen(
                                onLoginClick = { user, pass ->
                                    isLoading = true
                                    viewModel.login(user, pass) { userResponse, msg ->
                                        isLoading = false
                                        if (userResponse != null) {
                                            currentUserId = userResponse.id
                                            sharedPref.edit().putInt("USER_ID", userResponse.id).apply()

                                            Toast.makeText(
                                                this@MainActivity,
                                                "Benvenuto ${userResponse.username}",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            navController.navigate("team_selection")
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
                                            currentUserId = userResponse.id
                                            sharedPref.edit().putInt("USER_ID", userResponse.id).apply()

                                            Toast.makeText(
                                                this@MainActivity,
                                                "Account creato!",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            navController.navigate("team_selection")
                                        } else {
                                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onGoogleClick = {        // <<<<<< AGGIUNTO: login con Google
                                    signInWithGoogle()
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
                                    sharedPref.edit().putString("TEAM", team).apply()

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
                            MapScreen(team = team)
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
        val googleIdOption = GetSignInWithGoogleOption.Builder(
            serverClientId = "143510152058-sq83ea4aebkcogptq8qnv6ea3jbn23rk.apps.googleusercontent.com"
        ).build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    fun signInWithGoogle() {
        val request = buildSignInRequest()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )

                handleGoogleCredential(result.credential)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Errore login Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleCredential(credential: Credential) {
        when (credential) {
            is GoogleIdTokenCredential -> {
                val name = credential.displayName
                val email = credential.id
                val idToken = credential.idToken

                Toast.makeText(this, "Ciao $name!", Toast.LENGTH_SHORT).show()

                // Qui puoi collegare il login Google al tuo backend se vuoi
            }
        }
    }
}
