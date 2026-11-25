package com.marquitobro.clashai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.app.PendingIntent
import android.widget.Toast
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var frameCount = 0
    private var lastFrameCount = 0
    private var analyzing = false
    private val tracker = BattleTracker()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var recognitionBusy = false
    private var lastRecognition = 0L
    private var session = 0
    private var candidate: BattleTracker.Card? = null
    private var candidateAt = 0L
    private var speed = 1
    private var battleStatus: TextView? = null
    private var candidateButton: Button? = null
    private fun now() = SystemClock.elapsedRealtime()
    private fun updateBattle() {
        battleStatus?.text = "Elixir rival: " + tracker.estimate(now()) +
            "\nCiclo observado: " + tracker.cycle() +
            "\nMazo observado: " + tracker.observed() +
            "\n" + tracker.advice()
        val current = candidate?.takeIf { now() - candidateAt < 5000 }
        candidateButton?.isEnabled = current != null && analyzing
        candidateButton?.text = current?.let { "Confirmar " + it.name } ?: "Sin nombre detectado"
    }
    private fun analyzeImage(image: android.media.Image) {
        if (!analyzing || recognitionBusy || now() - lastRecognition < 650) return
        lastRecognition = now()
        val plane = image.planes.firstOrNull() ?: return
        val stride = plane.rowStride / plane.pixelStride
        val bitmap = Bitmap.createBitmap(stride, image.height, Bitmap.Config.ARGB_8888)
        try { plane.buffer.rewind(); bitmap.copyPixelsFromBuffer(plane.buffer) }
        catch (_: Exception) { bitmap.recycle(); return }
        val left = (image.width * 0.06).toInt()
        val top = (image.height * 0.12).toInt()
        val width = (image.width * 0.88).toInt()
        val height = (image.height * 0.55).toInt()
        if (width <= 0 || height <= 0) { bitmap.recycle(); return }
        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        if (cropped !== bitmap) bitmap.recycle()
        recognitionBusy = true
        val token = session
        try {
            recognizer.process(InputImage.fromBitmap(cropped, 0))
                .addOnSuccessListener { result ->
                    if (analyzing && token == session) {
                        val found = result.textBlocks.flatMap { it.lines }
                            .mapNotNull { BattleTracker.recognize(it.text) }.distinct()
                        if (found.size == 1) { candidate = found[0]; candidateAt = now() }
                        updateBattle()
                    }
                }
                .addOnCompleteListener { recognitionBusy = false; cropped.recycle() }
        } catch (_: Exception) { recognitionBusy = false; cropped.recycle() }
    }
    private var overlay: View? = null
    private var status: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            val delta = frameCount - lastFrameCount
            lastFrameCount = frameCount
            status?.text = if (!analyzing) "En pausa" else if (delta > 0)
                "Captura: $delta fps" else "Sin fotogramas"
            updateBattle()
            if (overlay != null) mainHandler.postDelayed(this, 1000)
        }
    }

    private fun showOverlay() {
        if (overlay != null) return
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Falta permiso de ventana flotante", Toast.LENGTH_LONG).show()
            return
        }
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 4, 8, 4)
            setBackgroundColor(Color.rgb(28, 38, 54))
        }
        val toggle = Button(this).apply {
            text = "AI · Analizar"
            setOnClickListener {
                analyzing = !analyzing
                text = if (analyzing) "Pausar" else "AI · Analizar"
                session++
                candidate = null
                updateBattle()
                status?.text = if (analyzing) "Comprobando..." else "En pausa"
            }
        }
        val indicator = TextView(this).apply {
            text = "En pausa"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
        }
        status = indicator
        val close = Button(this).apply {
            text = "Cerrar"
            setOnClickListener { stopCapture(); stopSelf() }
        }
        panel.addView(toggle)
        panel.addView(indicator)
        val reset = Button(this).apply {
            text = "Nueva partida"
            setOnClickListener { tracker.reset(now()); speed = 1; session++; candidate = null; updateBattle() }
        }
        val rateButton = Button(this).apply {
            text = "Cambiar x1/x2/x3"
            setOnClickListener { speed = speed % 3 + 1; tracker.speed(now(), speed); text = "Elixir x" + speed }
        }
        val battle = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
            maxWidth = (resources.displayMetrics.density * 220).toInt()
            maxLines = 8
        }
        battleStatus = battle
        val confirm = Button(this).apply {
            isEnabled = false
            text = "Sin nombre detectado"
            setOnClickListener {
                val card = candidate
                if (card != null && now() - candidateAt < 5000 && tracker.started) {
                    tracker.record(now(), card)
                    candidate = null
                    candidateAt = 0
                    updateBattle()
                }
            }
        }
        candidateButton = confirm
        panel.addView(reset)
        panel.addView(rateButton)
        panel.addView(battle)
        panel.addView(confirm)
        panel.addView(close)
        updateBattle()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 8
            y = (resources.displayMetrics.density * 100).toInt()
        }
        wm.addView(panel, params)
        overlay = panel
        mainHandler.post(refresh)
    }

    private fun removeOverlay() {
        mainHandler.removeCallbacks(refresh)
        val view = overlay
        overlay = null
        status = null
        battleStatus = null
        candidateButton = null
        session++
        candidate = null
        if (view != null) {
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view) }
            catch (_: IllegalArgumentException) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode == 0 || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (projection != null) return START_NOT_STICKY
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                    stopSelf()
                }
            }, null)
            startVirtualDisplay()
            if (virtualDisplay == null) throw IllegalStateException("No se inició la captura")
            showOverlay()
        } catch (error: Exception) {
            Toast.makeText(this, "No se pudo iniciar la captura: " + error.javaClass.simpleName, Toast.LENGTH_LONG).show()
            stopCapture()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startVirtualDisplay() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                try {
                    if (analyzing) { frameCount++; analyzeImage(image) }
                } finally { image.close() }
            }
        }, null)

        virtualDisplay = projection?.createVirtualDisplay(
            "ClashAI-ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun stopCapture() {
        removeOverlay()
        analyzing = false
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        val activeProjection = projection
        projection = null
        activeProjection?.stop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Captura de pantalla", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder
            .setContentTitle("Clash AI activo")
            .setContentText("Análisis local de texto y seguimiento experimental")
            .addAction(Notification.Action.Builder(null, "Detener", PendingIntent.getService(this, 1, Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build())
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopCapture()
        recognizer.close()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.marquitobro.clashai.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "capture_result_code"
        const val EXTRA_RESULT_DATA = "capture_result_data"
        private const val CHANNEL_ID = "clash_ai_capture"
        private const val NOTIFICATION_ID = 7101
    }
}