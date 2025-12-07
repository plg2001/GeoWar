package com.example.geowar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geowar.ui.AuthScreen
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

                    NavHost(navController = navController, startDestination = "landing") {
                        
                        // 1. Landing Screen
                        composable("landing") {
                            LandingScreen(
                                onStartClick = {
                                    val savedTeam = sharedPref.getString("TEAM", null)
                                    if (savedTeam != null) {
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
                            AuthScreen(
                                onLoginClick = { navController.navigate("team_selection") },
                                onRegisterClick = { navController.navigate("team_selection") }
                            )
                        }

                        // 3. Team Selection
                        composable("team_selection") {
                            TeamSelectionScreen(
                                onTeamSelected = { team ->
                                    with(sharedPref.edit()) {
                                        putString("TEAM", team)
                                        apply()
                                    }
                                    navController.navigate("map/$team") {
                                        popUpTo("landing") { inclusive = true }
                                    }
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