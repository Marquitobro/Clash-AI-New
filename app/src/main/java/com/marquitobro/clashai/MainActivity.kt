package com.marquitobro.clashai

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val projectionRequest by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClashAiScreen(
                onStartCapture = { beginCapture() },
                onStopCapture = {
                    startService(Intent(this, ScreenCaptureService::class.java).apply {
                        action = ScreenCaptureService.ACTION_STOP
                    })
                }
            )
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        launchCapturePermission()
    }

    private val overlayPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) beginCapture()
        else Toast.makeText(this, "Permite aparecer encima de otras apps.", Toast.LENGTH_LONG).show()
    }

    private fun beginCapture() {
        if (!Settings.canDrawOverlays(this)) {
            overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchCapturePermission()
        }
    }

    private fun launchCapturePermission() {
        capturePermission.launch(projectionRequest.createScreenCaptureIntent())
    }

    private val capturePermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            moveTaskToBack(true)
        }
    }
}

@Composable
private fun ClashAiScreen(onStartCapture: () -> Unit, onStopCapture: () -> Unit) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF10151F)) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CLASH AI", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("PRUEBA DE VENTANA FLOTANTE", color = Color(0xFF91A4C2), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2636)), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Captura de pantalla", color = Color(0xFF91A4C2), fontSize = 12.sp)
                        Text("Activa el botón flotante y permite la captura de toda la pantalla.", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Button(onClick = onStartCapture, modifier = Modifier.fillMaxWidth()) {
                            Text("Activar botón flotante")
                        }
                        Button(onClick = onStopCapture, modifier = Modifier.fillMaxWidth()) {
                            Text("Detener captura")
                        }
                        Text("Abre el juego o una repetición. Activa AI, pulsa Nueva partida y confirma los nombres de carta que el OCR detecte. El cálculo es aproximado; no juega automáticamente.", color = Color(0xFFB7C2D4), fontSize = 12.sp)
                    }
                }
                Text("Seguimiento disponible en la ventana flotante", color = Color.White, fontWeight = FontWeight.Bold)
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items((1..8).toList()) { _ ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202B3D)), shape = RoundedCornerShape(12.dp)) {
                            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(vertical = 25.dp), contentAlignment = Alignment.Center) {
                                Text("?", color = Color(0xFF91A4C2), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text("OCR experimental: no identifica dibujos de tropas. Usa preferentemente grabaciones o repeticiones.", color = Color(0xFF91A4C2), fontSize = 11.sp)
            }
        }
    }
}