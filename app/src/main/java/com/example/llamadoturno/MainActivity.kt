package com.example.llamadoturno

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
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
                SolicitarPermisosBluetooth {
                    PantallaBienvenida()
                }
            }
        }
    }
}

// --------------------------------------------------
// PERMISOS BLUETOOTH
// --------------------------------------------------
fun tienePermisoBluetooth(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
}



// ======================================================
//                      PANTALLA PRINCIPAL
// ======================================================
@Composable
fun PantallaBienvenida() {

    val context = LocalContext.current

    // Cargar lista guardada en memoria
    var listaUltimosTickets by remember {
        mutableStateOf(LastTicketsPreferences.getTickets(context))
    }

    val savedPrinterAddress = PrinterPreferences.getPrinterAddress(context)
    var seleccionImpresora by remember { mutableStateOf(savedPrinterAddress == null) }

    var dialogoVisible by remember { mutableStateOf(false) }
    var mensajeTurno by remember { mutableStateOf("") }

    // Modal DUI
    var mostrarModalDui by remember { mutableStateOf(false) }
    var duiIngresado by remember { mutableStateOf("") }
    var nombreManual by remember { mutableStateOf("") }
    var cargandoNombre by remember { mutableStateOf(false) }
    var intentoBusqueda by remember { mutableStateOf(false) }
    var errorNombre by remember { mutableStateOf("") }
    var errorDui by remember { mutableStateOf("") }

    var imprimiendo by remember { mutableStateOf(false) }

    var departamentoSeleccionado by remember { mutableStateOf(0) }
    var nombreServicio by remember { mutableStateOf("") }

    // Reimpresión (estado global solo para loading)
    var reimprimiendo by remember { mutableStateOf(false) }
    var mensajeReimpresion by remember { mutableStateOf("") }

    var esPreferencial by remember { mutableStateOf(false) }


    fun abrirModalDui() {
        duiIngresado = ""
        nombreManual = ""
        cargandoNombre = false
        intentoBusqueda = false
        errorNombre = ""
        errorDui = ""
        mostrarModalDui = true
        esPreferencial = false
    }

    // -------------------------------------------------------------
    // ENVIAR + GUARDAR EN MEMORIA + IMPRIMIR
    // -------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun enviar(departamentoId: Int, dui: String, nombre: String, preferencial: Boolean) {

        TurnoApi.enviarTurno(departamentoId, dui, nombre,preferencial) { success, result ->

            mensajeTurno = result

            if (success) {

                val turnoCodigo = result.replace("Turno generado:", "").trim()

                // Guardar en memoria
                LastTicketsPreferences.saveTicket(
                    context = context,
                    codigo = turnoCodigo,
                    descripcion = nombreServicio,
                    nombre = nombre,
                    dui = dui,
                    tiempo = "Hace un momento"
                )

                listaUltimosTickets = LastTicketsPreferences.getTickets(context)

                // Imprimir
                val address = PrinterPreferences.getPrinterAddress(context)
                if (address != null) {

                    val device = try {
                        BluetoothAdapter.getDefaultAdapter()?.bondedDevices
                            ?.firstOrNull { it.address == address }
                    } catch (e: SecurityException) { null }

                    if (device != null) {

                        imprimiendo = true

                        if (BluetoothPrinter.connect(device)) {

                            BluetoothPrinter.printTicket(
                                turno = turnoCodigo,
                                dui = dui,
                                nombre = nombre,
                                servicio = nombreServicio
                            )

                            BluetoothPrinter.close()

                        } else {
                            mensajeTurno = "No se pudo conectar a la impresora."
                        }

                        imprimiendo = false
                    }
                }
            }

            dialogoVisible = true
        }
    }

    // -------------------------------------------------------------
    // REIMPRIMIR TICKET (LO LÓGICO ESTÁ AQUÍ, NO EN EL ITEM)
    // -------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun reimprimir(ticket: TicketData, onResult: (String) -> Unit) {

        reimprimiendo = true
        mensajeReimpresion = ""

        val address = PrinterPreferences.getPrinterAddress(context)

        val mensaje = if (address == null) {

            "No hay impresora configurada"

        } else {

            val device = BluetoothAdapter.getDefaultAdapter()
                ?.bondedDevices
                ?.firstOrNull { it.address == address }

            if (device == null) {
                "Impresora no encontrada"
            } else if (!BluetoothPrinter.connect(device)) {
                "No se pudo conectar a la impresora"
            } else {

                BluetoothPrinter.printTicket(
                    turno = ticket.codigo,
                    dui = ticket.dui,
                    nombre = ticket.nombre,
                    servicio = ticket.descripcion
                )

                BluetoothPrinter.close()
                "Ticket reimpreso correctamente"
            }
        }

        mensajeReimpresion = mensaje
        reimprimiendo = false
        onResult(mensaje)
    }


    // ======================================================
    // UI
    // ======================================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(180.dp)
            )

            Text("Bienvenido", fontSize = 32.sp, color = Color(0xFF111827))
            Text("Seleccione el servicio que necesita", fontSize = 16.sp, color = Color(0xFF6B7280))

            Spacer(Modifier.height(40.dp))

            ServicioCardModern("Colecturía", "Pagos y trámites", Icons.Default.ReceiptLong) {
                departamentoSeleccionado = 1
                nombreServicio = "COLECTURÍA"
                abrirModalDui()
            }

            Spacer(Modifier.height(20.dp))

            ServicioCardModern("Atención al cliente", "Consultas e información", Icons.Default.SupportAgent) {
                departamentoSeleccionado = 2
                nombreServicio = "ATENCIÓN AL CLIENTE"
                abrirModalDui()
            }

            Spacer(Modifier.height(40.dp))

            // 🔹 Lista de últimos tickets, ahora con callback de reimpresión
            UltimosTicketsList(
                tickets = listaUltimosTickets,
                onReimprimir = { ticket, onResult ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        reimprimir(ticket, onResult)
                    } else {
                        onResult("La reimpresión requiere Android O o superior")
                    }
                }
            )

            Spacer(Modifier.height(50.dp))

            // Botón configuración impresora
            Button(
                onClick = {
                    PrinterPreferences.clearPrinter(context)
                    seleccionImpresora = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F1FB)),
                shape = RoundedCornerShape(50),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Settings, null, tint = Color(0xFF005696))
                Spacer(Modifier.width(12.dp))
                Text("Configuración de Impresora", color = Color(0xFF005696), fontSize = 18.sp)
            }
        }


        // ======================================================
        // MODAL DUI (IGUAL QUE LO TENÍAS)
        // ======================================================
        if (mostrarModalDui) {

            AlertDialog(
                onDismissRequest = { mostrarModalDui = false },
                title = { Text("Identificación", fontSize = 22.sp) },
                text = {

                    Column(Modifier.fillMaxWidth()) {

                        Text(
                            text = "Servicio: $nombreServicio",
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A),
                        )

                        Spacer(Modifier.height(12.dp))


                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Preferencial",
                                fontSize = 16.sp,
                                color = Color(0xFF111827)
                            )

                            Switch(
                                checked = esPreferencial,
                                onCheckedChange = { esPreferencial = it }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = duiIngresado,
                            onValueChange = {
                                duiIngresado = it.filter { d -> d.isDigit() }.take(9)
                                errorDui = ""
                            },
                            label = { Text("Ingrese DUI (9 dígitos)") },
                            isError = errorDui.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        if (errorDui.isNotEmpty())
                            Text(errorDui, color = Color.Red, fontSize = 14.sp)

                        Spacer(Modifier.height(20.dp))

                        if (cargandoNombre)
                            CircularProgressIndicator()

                        if (intentoBusqueda && !cargandoNombre) {

                            OutlinedTextField(
                                value = nombreManual,
                                onValueChange = {
                                    val limpio = it.replace(Regex("[^A-Za-zÁÉÍÓÚÑáéíóúñ ]"), "")
                                    nombreManual = limpio.uppercase()
                                    errorNombre = ""
                                },
                                label = { Text("Nombre completo") },
                                isError = errorNombre.isNotEmpty(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (errorNombre.isNotEmpty())
                                Text(errorNombre, color = Color.Red, fontSize = 14.sp)
                        }
                    }
                },

                confirmButton = {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        OutlinedButton(onClick = { mostrarModalDui = false }) {
                            Text("Cancelar", fontSize = 18.sp)
                        }

                        Spacer(Modifier.weight(1f))

                        if (!intentoBusqueda) {

                            Button(
                                onClick = {

                                    if (duiIngresado.length != 9) {
                                        errorDui = "El DUI debe tener 9 dígitos"
                                        return@Button
                                    }

                                    cargandoNombre = true
                                    intentoBusqueda = true
                                    nombreManual = ""

                                    UsuarioApi.obtenerUsuario(duiIngresado) { success, result ->

                                        cargandoNombre = false
                                        nombreManual = if (success) result.uppercase() else ""
                                    }
                                }
                            ) {
                                Text("Buscar")
                            }

                        } else {

                            Button(
                                onClick = {

                                    if (nombreManual.isBlank()) {
                                        errorNombre = "Ingrese un nombre válido"
                                        return@Button
                                    }

                                    mostrarModalDui = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        enviar(departamentoSeleccionado, duiIngresado, nombreManual, esPreferencial)
                                    }

                                }
                            ) {
                                Text("Imprimir")
                            }
                        }
                    }
                }
            )
        }


        // Selección de impresora
        if (seleccionImpresora) {
            SeleccionarImpresora(
                onSelect = {
                    PrinterPreferences.savePrinter(context, it.name, it.address)
                    seleccionImpresora = false
                },
                onCancel = { seleccionImpresora = false }
            )
        }

        // 🔥 Loading imprimir o reimprimir (A PANTALLA COMPLETA)
        if (imprimiendo || reimprimiendo) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (reimprimiendo) "Reimprimiendo..." else "Imprimiendo...",
                        color = Color.White
                    )
                }
            }
        }

        // Resultado Turno generado (no toqué esto)
        if (dialogoVisible) {
            AlertDialog(
                onDismissRequest = { dialogoVisible = false },
                confirmButton = {
                    TextButton(onClick = { dialogoVisible = false }) { Text("OK") }
                },
                title = { Text("Turno generado") },
                text = { Text(mensajeTurno) }
            )
        }
    }
}



// ======================================================
//                CARD SERVICIO
// ======================================================
@Composable
fun ServicioCardModern(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
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
                    .background(Color(0xFFE8F1FB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, null, tint = Color(0xFF005696), modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(titulo, fontSize = 20.sp, color = Color(0xFF111827))
                Text(subtitulo, fontSize = 14.sp, color = Color(0xFF6B7280))
            }

            Spacer(Modifier.weight(1f))

            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF9CA3AF))
        }
    }
}



// ======================================================
//          LISTA ÚLTIMOS TICKETS (CON REIMPRIMIR)
// ======================================================
@Composable
fun UltimosTicketsList(
    tickets: List<TicketData>,
    onReimprimir: (TicketData, (String) -> Unit) -> Unit
) {

    Column(modifier = Modifier.fillMaxWidth()) {

        Text("ÚLTIMOS TICKETS GENERADOS", fontSize = 14.sp, color = Color(0xFF6B7280))

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .background(Color.White, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                tickets.forEachIndexed { index, ticket ->

                    UltimoTicketItem(ticket, onReimprimir)

                    if (index < tickets.size - 1) {
                        Divider(color = Color(0xFFE5E7EB))
                    }
                }
            }
        }
    }
}



// ======================================================
//               ITEM TICKET (SIN LOADING ADENTRO)
// ======================================================
@Composable
fun UltimoTicketItem(
    ticket: TicketData,
    onReimprimir: (TicketData, (String) -> Unit) -> Unit
) {

    var mostrarDialogo by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .background(Color(0xFFF3F4F6), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(ticket.codigo, fontSize = 15.sp, color = Color(0xFF374151))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(ticket.descripcion, fontSize = 16.sp, color = Color(0xFF111827))
            Text("Nombre: ${ticket.nombre}", fontSize = 13.sp, color = Color(0xFF6B7280))
            Text("DUI: ${ticket.dui}", fontSize = 13.sp, color = Color(0xFF6B7280))
            Text(ticket.tiempo, fontSize = 12.sp, color = Color(0xFF9CA3AF))
        }

        IconButton(
            onClick = {
                onReimprimir(ticket) { resultado ->
                    mensaje = resultado
                    mostrarDialogo = true
                }
            }
        ) {
            Icon(
                Icons.Default.Print,
                contentDescription = "Reimprimir",
                tint = Color(0xFF005696),
                modifier = Modifier.size(40.dp)
            )
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("OK") }
            },
            title = { Text("Reimpresión de Ticket") },
            text = { Text(mensaje) }
        )
    }
}
