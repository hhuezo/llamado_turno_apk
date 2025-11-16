package com.example.llamadoturno

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.llamadoturno.ui.theme.LlamadoTurnoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LlamadoTurnoTheme {
                // Pedimos permisos Bluetooth antes de mostrar la app
                SolicitarPermisosBluetooth {
                    PantallaBienvenida()
                }
            }
        }
    }
}


// -----------------------------------------------------
// VALIDAR PERMISOS BLUETOOTH (Android 12+ requiere esto)
// -----------------------------------------------------
fun tienePermisoBluetooth(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
}


// -----------------------------------------------------
// PANTALLA PRINCIPAL (Conexión a API + Impresión BT)
// -----------------------------------------------------
@Composable
fun PantallaBienvenida() {

    val context = LocalContext.current

    var dialogoVisible by remember { mutableStateOf(false) }
    var seleccionImpresora by remember { mutableStateOf(false) }
    var mensajeTurno by remember { mutableStateOf("") }
    var turnoCodigo by remember { mutableStateOf("") }

    fun enviar(departamentoId: Int) {
        Log.d("API", "Enviando turno para departamento $departamentoId")

        TurnoApi.enviarTurno(departamentoId) { success, result ->
            Log.d("API", "Respuesta API -> success=$success result=$result")

            mensajeTurno = result

            if (success) {
                turnoCodigo = result.replace("Turno generado:", "").trim()
                Log.d("API", "Código de turno extraído: $turnoCodigo")
            }

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

        // HEADER
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Domain,
                contentDescription = null,
                tint = Color(0xFF005A9C),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Company Logo", fontSize = 20.sp, color = Color.Black)
        }

        // CONTENIDO
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Bienvenido", fontSize = 36.sp, color = Color.Black)
            Spacer(Modifier.height(10.dp))
            Text("Seleccione el servicio que necesita", fontSize = 18.sp, color = Color.Black)
            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CardServicioVertical(
                    "Colecturia",
                    "Pagos y trámites",
                    Icons.Default.ReceiptLong
                ) { enviar(1) }

                CardServicioVertical(
                    "Atención al cliente",
                    "Consultas e información",
                    Icons.Default.SupportAgent
                ) { enviar(2) }
            }
        }

        Text("Acerque su ticket al lector para ser atendido", fontSize = 14.sp)
    }

    // --------- DIALOGO DE RESULTADO ---------
    if (dialogoVisible) {
        AlertDialog(
            onDismissRequest = { dialogoVisible = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogoVisible = false
                    seleccionImpresora = true
                }) { Text("Imprimir") }
            },
            dismissButton = {
                TextButton(onClick = { dialogoVisible = false }) { Text("Cerrar") }
            },
            title = { Text("Turno generado") },
            text = { Text(mensajeTurno) }
        )
    }

    // --------- SELECCIÓN DE IMPRESORA ---------
    if (seleccionImpresora) {

        SeleccionarImpresora(
            onSelect = { device ->

                Log.d("BT_UI", "User selected printer: ${device.name} (${device.address})")

                seleccionImpresora = false

                // Validar permisos
                if (!tienePermisoBluetooth(context)) {
                    Log.e("BT_UI", "No Bluetooth permission")
                    mensajeTurno = "No se tienen permisos Bluetooth"
                    dialogoVisible = true
                    return@SeleccionarImpresora
                }

                // Intentar conectar
                if (BluetoothPrinter.connect(device)) {

                    Log.d("BT_UI", "Connected successfully. Sending ticket...")

                    BluetoothPrinter.printTicket(turnoCodigo)
                    BluetoothPrinter.close()

                } else {

                    Log.e("BT_UI", "Connection FAILED")
                    mensajeTurno = "No se pudo conectar a la impresora"
                    dialogoVisible = true
                }
            },

            onCancel = {
                seleccionImpresora = false
            }
        )
    }
}


// -----------------------------------------------------
// CARD SERVICIOS
// -----------------------------------------------------
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
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0x1A005A9C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = Color(0xFF005A9C), modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(titulo, fontSize = 22.sp)
                Text(subtitulo, fontSize = 14.sp, color = Color(0xFF617589))
            }
        }
    }
}
