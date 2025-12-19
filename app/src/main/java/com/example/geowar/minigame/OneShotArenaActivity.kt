package com.example.geowar.minigame

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Activity che ospita il minigioco "One-Shot Arena".
 *
 * Crea e imposta la `OneShotArenaView` come content view.
 */
class OneShotArenaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Istanzia la View del gioco e la imposta come contenuto dell'Activity.
        val arenaView = OneShotArenaView(this, null)
        setContentView(arenaView)
    }
}
