Implementa un minigioco PVP chiamato "One-Shot Arena", pensato per durare massimo 5–10 secondi


REQUISITI GENERALI
- Rendering: Canvas / View personalizzata (NO engine esterni tipo Unity, LibGDX)
- Architettura semplice, leggibile, facilmente estendibile
- Nessuna persistenza, nessun menu complesso
- Codice commentato

GAMEPLAY
- Arena 2D vista top-down, dimensione fissa
- Due giocatori rappresentati con i loro avatar che si trovano uno davanti l'atro (io sotto lo schermo lui sopra)
- Ogni giocatore ha:
    - una mira fissa
    - un proiettile che si muove in linea retta
    - Colpi infinito
- Se un giocatore spara e colpisce l’altro → vince immediatamente
- Se un giocatore spara e sbaglia → perde immediatamente
- Nessun respawn, nessun punteggio, fine partita istantanea

CONTROLLI
- I giocatori si muovono da destra a sinistra dello schermo (orizzontalmente) con l'accellerometro dell telefono
- Tap singolo per sparare
- Dopo lo sparo l’input viene disabilitato

DURATA
- Il match deve poter finire in meno di 20 secondi
- Appena c’è un vincitore, mostra un semplice testo:
  "VITTORIA" / "SCONFITTA"

STRUTTURA CODICE RICHIESTA
- OneShotArenaView : View personalizzata che gestisce rendering e input
- Player data class (posizione, direzione, vivo)
- GameState enum (WAITING, PLAYING, FINISHED)
- Update loop minimale (invalidate + postDelayed oppure Choreographer)

GRAFICA
- Minimal:
    - background neutro
    - per i giocatori prendi i loro avatar
    - linea o freccia che indica la direzione di mira
    - proiettile come piccolo cerchio che viaggia in linea retta

COLLISIONI
- Hitbox circolare semplice
- Collisione proiettile-giocatore con distanza euclidea

NETWORKING
- NON implementare networking
- Scrivi il codice in modo che sia facile in futuro sincronizzare:
    - posizione iniziale
    - angolo di tiro
    - evento "shot fired"

CONSEGNA
- Fornisci:
    1) Il codice completo della View
    2) Le data class
    3) Un esempio di Activity che carica il minigioco
- NON usare librerie esterne
- NON aggiungere feature non richieste
