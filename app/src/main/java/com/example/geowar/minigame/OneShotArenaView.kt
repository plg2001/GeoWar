package com.example.geowar.minigame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.geowar.minigame.data.GameState
import com.example.geowar.minigame.data.Player
import com.example.geowar.minigame.data.Projectile
import kotlin.math.pow
import kotlin.math.sqrt

class OneShotArenaView(context: Context, attrs: AttributeSet?) : View(context, attrs), SensorEventListener {

    // --- Stato del Gioco ---
    private var gameState = GameState.WAITING
    private var player: Player? = null
    private var opponent: Player? = null
    private var projectile: Projectile? = null

    // --- Rendering ---
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3498db") }
    private val opponentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#e74c3c") }
    private val projectilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 120f
        textAlign = Paint.Align.CENTER
    }
    private val backgroundColor = Color.parseColor("#2c3e50")

    // --- Controlli ---
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // --- Game Loop ---
    private val gameLoop = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) {
                return
            }

            update()
            invalidate()

            postDelayed(this, 16) // Schedula il prossimo frame (~60 FPS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerSensorListener()
        // Avvia il ciclo di gioco.
        post(gameLoop)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterSensorListener()
        // Interrompe il ciclo di gioco in modo pulito.
        removeCallbacks(gameLoop)
    }

    /**
     * Inizializza o reimposta lo stato del gioco in modo sicuro.
     */
    private fun resetGame() {
        // Assicura che le dimensioni siano valide prima di posizionare gli oggetti.
        if (width == 0 || height == 0) return

        player = Player(id = 0, x = width / 2f, y = height - 200f)
        opponent = Player(id = 1, x = width / 2f, y = 200f)
        projectile = null
        gameState = GameState.PLAYING
        registerSensorListener() // Assicura che i sensori siano attivi per il nuovo round.
    }

    /**
     * Aggiorna la logica di gioco frame by frame.
     */
    private fun update() {
        if (gameState != GameState.PLAYING) return

        projectile?.let { proj ->
            proj.y += proj.velocityY

            val localOpponent = opponent ?: return@let

            // Controlla la collisione (VITTORIA)
            val distance = sqrt((proj.x - localOpponent.x).toDouble().pow(2) + (proj.y - localOpponent.y).toDouble().pow(2)).toFloat()
            if (distance < proj.radius + localOpponent.radius) {
                endGame(didPlayerWin = true)
                return
            }

            // Controlla se hai mancato (SCONFITTA)
            // Se il proiettile ha superato la posizione y dell'avversario senza colpirlo.
            if (proj.y < localOpponent.y) {
                endGame(didPlayerWin = false)
                return
            }
        }
    }

    /**
     * Disegna la scena corrente sul Canvas.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Pattern di inizializzazione robusto: se il gioco non è pronto, lo si inizializza qui.
        // Questo garantisce che 'width' e 'height' siano disponibili.
        if (player == null) {
            resetGame()
        }

        canvas.drawColor(backgroundColor)

        // Evita NullPointerException se resetGame non è ancora terminato
        val localPlayer = player ?: return
        val localOpponent = opponent ?: return

        canvas.drawCircle(localPlayer.x, localPlayer.y, localPlayer.radius, playerPaint)
        canvas.drawCircle(localOpponent.x, localOpponent.y, localOpponent.radius, opponentPaint)
        projectile?.let { canvas.drawCircle(it.x, it.y, it.radius, projectilePaint) }

        if (gameState == GameState.FINISHED) {
            val message = if (localPlayer.isAlive) "VITTORIA!" else "SCONFITTA!"
            canvas.drawText(message, width / 2f, height / 2f, textPaint)
        }
    }

    /**
     * Gestisce l'input del tocco per sparare.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameState == GameState.PLAYING && projectile == null && event.action == MotionEvent.ACTION_DOWN) {
            fire()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun fire() {
        player?.let { p ->
            projectile = Projectile(x = p.x, y = p.y - p.radius, velocityY = -60f, ownerId = p.id)
        }
    }

    private fun endGame(didPlayerWin: Boolean) {
        if (gameState == GameState.FINISHED) return

        gameState = GameState.FINISHED
        player?.isAlive = didPlayerWin

        unregisterSensorListener()

        postDelayed({ resetGame() }, 3000)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (gameState != GameState.PLAYING) return

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            player?.let {
                it.x -= event.values[0] * 2.5f
                // Limita il movimento del giocatore all'interno dello schermo.
                it.x = it.x.coerceIn(it.radius, width - it.radius)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Non necessario */ }

    private fun registerSensorListener() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(this)
    }
}
