package com.example.geowar.minigame.data

/**
 * Rappresenta un proiettile sparato da un giocatore.
 *
 * @param x Posizione orizzontale corrente del proiettile.
 * @param y Posizione verticale corrente del proiettile.
 * @param velocityY Velocità verticale del proiettile (costante).
 * @param ownerId ID del giocatore che ha sparato il proiettile.
 * @param radius Raggio del cerchio del proiettile per rendering e collisioni.
 */
data class Projectile(
    var x: Float,
    var y: Float,
    val velocityY: Float,
    val ownerId: Int,
    val radius: Float = 15f
)
