package com.example.llamadoturno

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun SeleccionarImpresora(
    onSelect: (BluetoothDevice) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // ✔ Verificación de permiso requerida por Android
    val tienePermiso = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED

    if (!tienePermiso) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("Permiso requerido") },
            text = { Text("La aplicación no tiene permiso para acceder a dispositivos Bluetooth.") },
            confirmButton = {
                TextButton(onClick = onCancel) { Text("Cerrar") }
            }
        )
        return
    }

    // ✔ AQUÍ ya tenemos el permiso garantizado
    val dispositivos = BluetoothPrinter.getPairedPrinters()?.toList() ?: emptyList()

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Seleccione impresora") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (dispositivos.isEmpty()) {
                    Text("No hay impresoras emparejadas.")
                } else {
                    dispositivos.forEach { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { onSelect(device) },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(device.name ?: "Impresora desconocida")
                                Text(device.address)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    )
}
