package com.marquitobro.clashai

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val capture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            startForegroundService(Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("code", result.resultCode)
                putExtra("data", data)
            })
            moveTaskToBack(true)
        }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) requestCapture()
    }
    private fun requestCapture() {
        capture.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
            addView(TextView(context).apply {
                text = "Clash Tracker Lite"
                textSize = 28f
            })
            addView(TextView(context).apply {
                text = "Elixir estimado y ciclo observado. Detector visual experimental de 12 tropas. Inicia la ventana, comparte toda la pantalla y pulsa Inicio al comenzar una repetición estándar. No detecta todos los hechizos."
                textSize = 16f
            })
            addView(Button(context).apply {
                text = "Abrir ventana flotante"
                setOnClickListener {
                    if (Settings.canDrawOverlays(this@MainActivity)) requestCapture()
                    else permission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + packageName)))
                }
            })
            addView(Button(context).apply {
                text = "Cerrar ventana"
                setOnClickListener { stopService(Intent(this@MainActivity, ScreenCaptureService::class.java)) }
            })
        })
    }
}
