# GeoWar Project Pipeline

## 🟢 FASE 1: Il Backend (Il Cervello)
Obiettivo: Avere un server remoto che gestisce utenti, zone e combattimenti. Tecnologia: Python (Flask) su PythonAnywhere + Database SQL.

### Setup Database (Req #10):
- **Tabella Users**: username, password, team (RED/BLUE), score, lat (nuovo!), lon (nuovo!), last_active (nuovo!).
- **Tabella Targets**: id, lat, lon, name, owner_team (NEUTRAL/RED/BLUE).

### API Endpoints (Req #9):
- **POST /login**: Verifica credenziali.
- **GET /targets**: Scarica la lista dei bersagli per la mappa.
- **POST /hack**: Logica di conquista (Se neutro -> conquista; Se nemico -> ruba).
- **POST /heartbeat** (Cruciale per PvP): Riceve la posizione dell'utente, aggiorna il DB, cerca nemici vicini e risponde con "SAFE" o "DANGER".

## 🔵 FASE 2: L'App Mobile - Scheletro & Mappa
Obiettivo: Vedere il mondo di gioco. Tecnologia: Android (Kotlin).

### Login Screen (Req #2):
- Due bottoni giganti: "Join RED Team" / "Join BLUE Team".
- Salva il token utente nelle preferenze locali.

### Mappa (Req #5, #8):
- Implementa Google Maps SDK.
- Chiedi permessi GPS.
- Disegna i Marker dei target scaricati dal server: Colorali di Grigio/Rosso/Blu in base al JSON ricevuto.

## 🟠 FASE 3: Il PvE (Conquista delle Zone)
Obiettivo: Andare sul posto e catturare obiettivi.

### Geofencing (Req #5):
- Calcola costantemente distanza(Tu, Target).
- Se distanza < 20m -> Mostra bottone "HACK SYSTEM".

### Minigame "Disinnesco" (Req #3, #4):
- Si apre una Canvas custom (Grafica 2D).
- Disegna un cerchio e una pallina.
- Usa l'Accelerometro (Sensore): inclina il telefono per tenere la pallina al centro mentre una barra di caricamento avanza.

### La Prova (Req #6):
- Vinto il minigioco, apri la Fotocamera.
- Scatta foto -> Prendi la Bitmap -> Disegna sopra la scritta "HACKED" o il logo del team (Image Processing) -> Invia al server (o salva finto invio se vuoi risparmiare dati).

### Reward (Req #1):
- Dopo l'hack, chiama una Public API (es. Chuck Norris Jokes API o Techy Quotes) per mostrare una "frase segreta decifrata" come premio.

## 🔴 FASE 4: Il PvP (Sfida tra Giocatori)
Obiettivo: Gestire l'incontro ravvicinato con il nemico.

### Background Service (Req #7 - Concurrency):
- Crea un processo in background (Coroutine/WorkManager) che ogni 5 secondi chiama POST /heartbeat.
- Questo processo non deve fermarsi mai finché l'app è aperta.

### Gestione "DANGER":
- Se il server risponde "DANGER", l'app lancia un Overlay/Dialog a tutto schermo.
- Mostra: "NEMICO RILEVATO: [Nome Nemico]!".

### Minigame PvP "Quick Draw":
- Schermo nero. Scritta "PRONTI...".
- Dopo X secondi (random) -> Schermo VERDE ("SPARA!").
- Il giocatore deve tappare il più veloce possibile.
- Chi perde viene "stordito" (non può hackerare per 2 minuti).