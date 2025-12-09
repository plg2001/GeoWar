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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geowar.ui.auth.AuthScreen
import com.example.geowar.ui.auth.AuthViewModel
import com.example.geowar.ui.LandingScreen
import com.example.geowar.ui.MapScreen
import com.example.geowar.ui.TeamSelectionScreen
import com.example.geowar.ui.theme.GeoWarTheme

class MainActivity : ComponentActivity() {
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
                    
                    // Stato per tracciare l'ID utente dopo il login/registrazione
                    var currentUserId by remember { mutableStateOf<Int?>(null) }
                    
                    NavHost(navController = navController, startDestination = "landing") {
                        
                        // 1. Landing Screen
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

                        // 2. Auth Screen
                        composable("auth") {
                            var isLoading by remember { mutableStateOf(false) }

                            AuthScreen(
                                onLoginClick = { user, pass ->
                                    isLoading = true
                                    viewModel.login(user, pass) { userResponse, msg ->
                                        isLoading = false
                                        if (userResponse != null) {
                                            currentUserId = userResponse.id
                                            // Salviamo ID nelle preferenze
                                            sharedPref.edit().putInt("USER_ID", userResponse.id).apply()
                                            
                                            Toast.makeText(this@MainActivity, "Benvenuto ${userResponse.username}", Toast.LENGTH_SHORT).show()
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
                                            // Salviamo ID nelle preferenze
                                            sharedPref.edit().putInt("USER_ID", userResponse.id).apply()
                                            
                                            Toast.makeText(this@MainActivity, "Account creato!", Toast.LENGTH_SHORT).show()
                                            navController.navigate("team_selection")
                                        } else {
                                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                isLoading = isLoading
                            )
                        }

                        // 3. Team Selection
                        composable("team_selection") {
                            TeamSelectionScreen(
                                onTeamSelected = { team ->
                                    // 1. Salva localmente
                                    with(sharedPref.edit()) {
                                        putString("TEAM", team)
                                        apply()
                                    }
                                    
                                    // 2. Salva sul Server (Se abbiamo un ID utente)
                                    if (currentUserId != null) {
                                        viewModel.setTeam(currentUserId!!, team) { success, msg ->
                                            if (!success) {
                                                // Log o Toast errore (opzionale, per ora procediamo comunque)
                                                // Toast.makeText(this@MainActivity, "Warning: Team not synced", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }

                                    // 3. Naviga
                                    navController.navigate("map/$team") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 4. Game Map
                        composable("map/{team}") { backStackEntry ->
                            val team = backStackEntry.arguments?.getString("team") ?: "UNKNOWN"
                            MapScreen(team = team)
                        }
                    }
                }
            }
        }
    }
}