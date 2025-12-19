package com.example.geowar.minigame.data

/**
 * Rappresenta un giocatore nell'arena.
 *
 * @param id Identificativo univoco del giocatore (es. 0 per il giocatore locale, 1 per l'avversario).
 * @param x Posizione orizzontale del centro del giocatore.
 * @param y Posizione verticale del centro del giocatore.
 * @param isAlive Stato del giocatore (vivo o morto).
 * @param radius Raggio del cerchio che rappresenta il giocatore (usato per rendering e collisioni).
 */
data class Player(
    val id: Int,
    var x: Float,
    var y: Float,
    var isAlive: Boolean = true,
    val radius: Float = 50f
)
