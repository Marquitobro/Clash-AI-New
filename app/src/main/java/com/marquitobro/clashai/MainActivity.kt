package com.marquitobro.clashai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClashAiScreen() }
    }
}

@Composable
private fun ClashAiScreen() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF10151F)) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CLASH AI", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("MAZO DEL RIVAL", color = Color(0xFF91A4C2), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2636)), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Estado", color = Color(0xFF91A4C2), fontSize = 12.sp)
                        Text("Preparado para una partida", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("La detección automática de cartas todavía está en desarrollo.", color = Color(0xFFB7C2D4), fontSize = 12.sp)
                    }
                }
                Text("Últimas 8 cartas detectadas", color = Color.White, fontWeight = FontWeight.Bold)
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items((1..8).toList()) { _ ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202B3D)), shape = RoundedCornerShape(12.dp)) {
                            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(vertical = 25.dp), contentAlignment = Alignment.Center) {
                                Text("?", color = Color(0xFF91A4C2), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text("Las cartas aparecerán aquí cuando el detector visual esté integrado y validado.", color = Color(0xFF91A4C2), fontSize = 11.sp)
            }
        }
    }
}
