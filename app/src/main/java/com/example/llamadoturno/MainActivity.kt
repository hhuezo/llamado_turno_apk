package com.example.llamadoturno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.llamadoturno.ui.theme.LlamadoTurnoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LlamadoTurnoTheme {
                PantallaBienvenida()
            }
        }
    }
}

@Composable
fun PantallaBienvenida() {

    var dialogoVisible by remember { mutableStateOf(false) }
    var mensajeTurno by remember { mutableStateOf("") }

    fun enviar(departamentoId: Int) {
        TurnoApi.enviarTurno(departamentoId) { success, result ->
            mensajeTurno = if (success) result else "Error: $result"
            dialogoVisible = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color(0xFFF0F2F5)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // -------- HEADER ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Domain,
                contentDescription = null,
                tint = Color(0xFF005A9C),
                modifier = Modifier.size(40.dp)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                "Company Logo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        // -------- CONTENIDO CENTRAL ----------
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Bienvenido",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "Seleccione el servicio que necesita",
                fontSize = 18.sp,
                color = Color(0xFF333333)
            )

            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CardServicioVertical(
                    titulo = "Colecturia",
                    subtitulo = "Pagos y trámites",
                    icono = Icons.Default.ReceiptLong,
                    onClick = { enviar(1) }
                )

                CardServicioVertical(
                    titulo = "Atención al cliente",
                    subtitulo = "Consultas e información",
                    icono = Icons.Default.SupportAgent,
                    onClick = { enviar(2) }
                )
            }
        }

        Text(
            "Acerque su ticket al lector para ser atendido",
            fontSize = 14.sp,
            color = Color(0xFF333333)
        )
    }

    // ---------- DIALOGO PARA MOSTRAR RESPUESTA ----------
    if (dialogoVisible) {
        AlertDialog(
            onDismissRequest = { dialogoVisible = false },
            confirmButton = {
                TextButton(onClick = { dialogoVisible = false }) {
                    Text("OK")
                }
            },
            title = { Text("Turno generado") },
            text = { Text(mensajeTurno) }
        )
    }
}


// ------------------------------------------------------------------------
//  CARD VERTICAL CON CLICK
// ------------------------------------------------------------------------
@Composable
fun CardServicioVertical(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0x1A005A9C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = Color(0xFF005A9C),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(subtitulo, fontSize = 14.sp, color = Color(0xFF617589))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPantalla() {
    LlamadoTurnoTheme {
        PantallaBienvenida()
    }
}
