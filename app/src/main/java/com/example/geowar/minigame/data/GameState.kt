package com.example.geowar.minigame.data

/**
 * Enum per rappresentare i possibili stati del minigioco.
 */
enum class GameState {
    /**
     * Stato iniziale, in attesa che il gioco inizi.
     * I giocatori possono vedere l'arena ma non possono ancora interagire.
     */
    WAITING,

    /**
     * Il gioco è in corso. I giocatori possono muoversi e sparare.
     */
    PLAYING,

    /**
     * Il gioco è terminato. Viene mostrato un messaggio di vittoria o sconfitta.
     * L'input è disabilitato.
     */
    FINISHED
}
