package com.example.geowar

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geowar.data.ApiClient
import com.example.geowar.data.auth.UserResponse
import com.example.geowar.ui.LandingScreen
import com.example.geowar.ui.MapScreen
import com.example.geowar.ui.MinigameScreen
import com.example.geowar.ui.ColorMinigameScreen
import com.example.geowar.ui.TeamSelectionScreen
import com.example.geowar.ui.admin.AdminScreen
import com.example.geowar.ui.auth.AuthScreen
import com.example.geowar.ui.auth.AuthViewModel
import com.example.geowar.ui.lobby.LobbyListScreen
import com.example.geowar.ui.lobby.LobbyScreen
import com.example.geowar.ui.lobby.LobbyViewModel
import com.example.geowar.ui.theme.GeoWarTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.core.content.edit
import com.example.geowar.ui.AccountScreen
import java.util.UUID
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch
import kotlin.random.Random

const val PREFS_NAME = "geowar_prefs"
const val PREF_USER_ID = "USER_ID"
const val PREF_TEAM = "TEAM"
const val PREF_USERNAME = "USERNAME"

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

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)


        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            GeoWarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: AuthViewModel = viewModel()
                    val lobbyViewModel: LobbyViewModel = viewModel()

                    var currentUserId by remember { mutableStateOf<Int?>(null) }

                    NavHost(navController = navController, startDestination = "landing") {

                        // -------------------------
                        // 1. Landing Screen
                        // -------------------------
                        composable("landing") {
                            val context = LocalContext.current
                            LandingScreen(
                                onStartClick = {
                                    val savedTeam = sharedPref.getString(PREF_TEAM, null)
                                    val savedUserId = sharedPref.getInt(PREF_USER_ID, -1)
                                    
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
                                    putInt(PREF_USER_ID, userResponse.id)
                                    putString(PREF_USERNAME, userResponse.username) 
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
                                    // Se è utente normale, vai alla selezione lobby
                                    navController.navigate("lobby_selection")
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
                        // 3. Lobby Selection
                        // -------------------------
                        composable("lobby_selection") {
                            val username = sharedPref.getString(PREF_USERNAME, "Player") ?: "Player"
                            LobbyScreen(
                                username = username,
                                onPublicMatchClick = {
                                    lobbyViewModel.getLobbies()
                                    navController.navigate("lobby_list")
                                },
                                onPrivateMatchClick = {
                                    Toast.makeText(this@MainActivity, "In arrivo...", Toast.LENGTH_SHORT).show()
                                },
                                onLogoutClick = {
                                    sharedPref.edit { clear() }
                                    navController.navigate("auth") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                },
                                onAccountClick = {
                                    navController.navigate("account")
                                }
                            )
                        }
                        
                        // -------------------------
                        // 3.5. Lobby List
                        // -------------------------
                        composable("lobby_list") {
                            LobbyListScreen(
                                lobbyViewModel = lobbyViewModel,
                                onLobbyClick = { lobbyId ->
                                    if (currentUserId != null) {
                                        lobbyViewModel.joinLobby(currentUserId!!, lobbyId) { response, msg ->
                                            if (response != null) {
                                                Toast.makeText(this@MainActivity, "Lobby: ${response.lobby_id}", Toast.LENGTH_SHORT).show()
                                                navController.navigate("team_selection")
                                            } else {
                                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(this@MainActivity, "Errore: Utente non identificato", Toast.LENGTH_SHORT).show()
                                        navController.navigate("auth")
                                    }
                                },
                                onBackClick = { 
                                    navController.popBackStack()
                                }
                            )
                        }

                        // -------------------------
                        // 4. Team Selection
                        // -------------------------
                        composable("team_selection") {
                            TeamSelectionScreen(
                                onTeamSelected = { team ->
                                    sharedPref.edit { putString(PREF_TEAM, team) }

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
                        // 5. Map Screen
                        // -------------------------
                        composable("map/{team}") { backStackEntry ->
                            val team = backStackEntry.arguments?.getString("team") ?: "UNKNOWN"
                            val username = sharedPref.getString(PREF_USERNAME, "") ?: ""
                            MapScreen(
                                username = username,
                                team = team,
                                onLogout = {
                                    // Rimuovi solo il team, mantieni login
                                    sharedPref.edit { remove(PREF_TEAM) }
                                    // Torna alla selezione lobby
                                    navController.navigate("lobby_selection") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                },
                                onAccountClick = {
                                    navController.navigate("account")
                                }
                            )
                        }

                        // -------------------------
                        // 6. Admin Dashboard
                        // -------------------------
                        composable("admin_dashboard") {
                            AdminScreen(
                                onLogout = {
                                    // Logout semplice: pulisci le preferenze e torna all\'auth
                                    sharedPref.edit { clear() }
                                    navController.navigate("auth") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // -------------------------
                        // 7. Account Screen
                        // -------------------------
                        composable("account") {
                            AccountScreen(
                                authApi = ApiClient.authApi,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        
                        // -------------------------
                        // 8. Accelerometer Minigame Screen (Old)
                        // -------------------------
                        composable("minigame") {
                            MinigameScreen(
                                onWin = {
                                    Toast.makeText(this@MainActivity, "HAI VINTO!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onLose = {
                                    Toast.makeText(this@MainActivity, "HAI PERSO/USCITO!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // -------------------------
                        // 9. Color Minigame Screen (New)
                        // -------------------------
                        composable("color_minigame") {
                            // Seleziona un colore casuale tra ROSSO, VERDE, BLU
                            val randomColor = remember {
                                val colors = listOf("ROSSO", "VERDE", "BLU")
                                colors[Random.nextInt(colors.size)]
                            }

                            ColorMinigameScreen(
                                targetColorName = randomColor, 
                                onWin = {
                                    Toast.makeText(this@MainActivity, "COLORE TROVATO!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onLose = {
                                    navController.popBackStack()
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

    private fun signInWithGoogle(onSuccess: (String) -> Unit) {
         val request = buildSignInRequest()

         lifecycleScope.launch {
             try {
                 val result = credentialManager.getCredential(
                     request = request,
                     context = this@MainActivity
                 )
                 val credential = result.credential
                 
                 // Google ID Token Credential
                 if (credential is CustomCredential && 
                     credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                     
                     val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                     onSuccess(googleIdTokenCredential.idToken)
                 } else {
                     Log.e("Auth", "Unexpected credential type")
                 }

             } catch (e: GetCredentialException) {
                 Log.e("Auth", "GetCredentialException", e)
             } catch (e: Exception) {
                 Log.e("Auth", "Login failed", e)
             }
         }
    }

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
}
