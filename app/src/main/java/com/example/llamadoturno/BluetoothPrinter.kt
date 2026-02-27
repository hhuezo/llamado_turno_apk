package com.example.llamadoturno

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.io.OutputStream
import java.lang.reflect.Method
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object BluetoothPrinter {

    private const val TAG = "BT_PRINTER"

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getPairedPrinters(): Set<BluetoothDevice>? {
        return bluetoothAdapter?.bondedDevices
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket!!.connect()
            } catch (e: Exception) {
                val method: Method = device.javaClass.getMethod(
                    "createRfcommSocket", Int::class.javaPrimitiveType
                )
                socket = method.invoke(device, 1) as BluetoothSocket
                socket!!.connect()
            }

            outputStream = socket!!.outputStream
            // Dar tiempo a la impresora para estar lista tras la conexión
            Thread.sleep(500)
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error connecting: ${e.message}")
            false
        }
    }


    // Comandos ESC/POS: tamaño 0x1D 0x21 n → n = (ancho-1)<<4 | (alto-1)
    // 0x11 = 2x2, 0x10 = 2x1 (doble ancho), 0x01 = 1x2
    private fun size2x2() = byteArrayOf(0x1D.toByte(), 0x21, 0x11)
    private fun size2x1() = byteArrayOf(0x1D.toByte(), 0x21, 0x10) // un poco más grande que normal
    private fun sizeNormal() = byteArrayOf(0x1D.toByte(), 0x21, 0x00)
    private fun boldOn() = byteArrayOf(0x1B.toByte(), 0x45, 0x01)
    private fun boldOff() = byteArrayOf(0x1B.toByte(), 0x45, 0x00)

    // -------------------------------------------------------------
    // TICKET REDISEÑADO — LETRA GRANDE Y MEJOR VISIBILIDAD
    // -------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun printTicket(turno: String, dui: String, nombre: String, servicio: String) {
        try {

            val ESC = 0x1B.toByte()
            val center = byteArrayOf(ESC, 0x61, 0x01)
            val left = byteArrayOf(ESC, 0x61, 0x00)

            val now = LocalDateTime.now()
            val fechaHora = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

            outputStream?.apply {

                write(center)
                printLogo()
                write(byteArrayOf(0x1B, 0x40)) // ESC @ — reset impresora tras modo gráfico
                write(center)
                write("\n".toByteArray()) // Poco margen entre logo y texto

                // ----- TÍTULO "TURNO" — UN POCO MÁS PEQUEÑO (2x2) -----
                write(center)
                write(size2x2())
                write(boldOn())
                write("TURNO\n".toByteArray())
                write(boldOff())
                write(sizeNormal())

                write(center)
                write("========================\n".toByteArray())

                // ----- NÚMERO DE TURNO — DESTACADO (2x2 + BOLD) -----
                write(center)
                write(size2x2())
                write(boldOn())
                write("$turno\n".toByteArray())
                write(boldOff())
                write(sizeNormal())

                write(center)
                write("========================\n".toByteArray())
                write("\n".toByteArray())

                // ----- DUI, CLIENTE, SERVICIO — un poco más grande (2x1) -----
                write(left)
                write(size2x1())
                write(boldOn())
                write("DUI: ".toByteArray())
                write(boldOff())
                write("$dui\n".toByteArray())

                write(boldOn())
                write("Cliente:\n".toByteArray())
                write(boldOff())
                write("$nombre\n\n".toByteArray())

                write(boldOn())
                write("Servicio:\n".toByteArray())
                write(boldOff())
                write(clean("$servicio\n\n").toByteArray())

                write(sizeNormal())
                write(boldOn())
                write("Fecha y hora:\n".toByteArray())
                write(boldOff())
                write("$fechaHora\n".toByteArray())

                write("------------------------\n".toByteArray())

                // ----- PIE — TEXTO PEQUEÑO Y CENTRADO -----
                write(center)
                write(sizeNormal())
                write("Gracias por esperar\n".toByteArray())
                write("Su turno sera llamado\n".toByteArray())

                write("\n\n\n\n\n".toByteArray())

                flush()
                // Esperar a que la impresora termine de procesar todo el ticket
                // antes de cerrar la conexión (evita que reimpresión quede a medias)
                Thread.sleep(2000)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Print error: ${e.message}")
        }
    }

    fun clean(text: String): String {
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return normalized.replace("[^\\p{ASCII}]".toRegex(), "")
    }

    // -------------------------------------------------------------
    // 🔥🔥🔥 LOGO 100% FUNCIONAL — COPIADO DE TU OTRA APLICACIÓN 🔥🔥🔥
    // -------------------------------------------------------------
    private fun printLogo() {
        try {
            Log.e("LOGO", "==== INICIANDO printLogo ====")

            val ctx = MyApp.instance

            val bmpRaw = BitmapFactory.decodeResource(ctx.resources, R.drawable.escudo)
            Log.e("LOGO", "Imagen cargada: ${bmpRaw.width}x${bmpRaw.height}")

            val sizePx = 100
            Log.e("LOGO", "Escalando a: ${sizePx}px")

            val bmpScaled = Bitmap.createScaledBitmap(bmpRaw, sizePx, sizePx, true)
            Log.e("LOGO", "bmpScaled listo: ${bmpScaled.width}x${bmpScaled.height}")

            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            Log.e("LOGO", "Bitmap ARGB creado")

            for (y in 0 until sizePx) {
                for (x in 0 until sizePx) {
                    val pixel = bmpScaled.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
                    val bw = if (gray < 128) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    bmp.setPixel(x, y, bw)
                }
            }
            Log.e("LOGO", "Conversión B/W terminada")

            val bytesPerRow = sizePx / 8
            Log.e("LOGO", "bytesPerRow=$bytesPerRow")

            val imageData = ByteArray(bytesPerRow * sizePx)
            Log.e("LOGO", "imageData size=${imageData.size}")

            for (y in 0 until sizePx) {
                for (xByte in 0 until bytesPerRow) {
                    var value = 0
                    for (bit in 0 until 8) {
                        val x = xByte * 8 + bit
                        val pixel = bmp.getPixel(x, y)
                        val bitValue = if (pixel == 0xFF000000.toInt()) 1 else 0
                        value = value or (bitValue shl (7 - bit))
                    }
                    imageData[y * bytesPerRow + xByte] = value.toByte()
                }
            }

            Log.e("LOGO", "DATA GENERADA correctamente.")

            val header = byteArrayOf(
                0x1D, 0x76, 0x30, 0x00,
                (bytesPerRow and 0xFF).toByte(),
                0x00.toByte(),
                (sizePx and 0xFF).toByte(),
                0x00.toByte()
            )

            Log.e("LOGO", "HEADER: ${header.joinToString()}")

            Log.e("LOGO", "Escribiendo HEADER al socket…")
            outputStream?.write(header) ?: Log.e("LOGO", "outputStream es NULL!")

            Log.e("LOGO", "Escribiendo DATA al socket…")
            outputStream?.write(imageData)

            Log.e("LOGO", "ENVIADO AL SOCKET — SI AÚN NO IMPRIME, LA IMPRESORA LO ESTÁ RECHAZANDO")

            outputStream?.write("\n".toByteArray())

            Log.e("LOGO", "==== FIN printLogo ====")

        } catch (e: Exception) {
            Log.e("LOGO_ERROR", "ERROR AL IMPRIMIR LOGO: ${e.message}")
            e.printStackTrace()
        }
    }



    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.O)
    fun reimprimirTicket(ticket: TicketData): Boolean {

        try {

            val address = PrinterPreferences.getPrinterAddress(MyApp.instance)
                ?: return false

            val device = BluetoothAdapter.getDefaultAdapter()?.bondedDevices
                ?.firstOrNull { it.address == address }
                ?: return false

            // Conectar igual que siempre
            if (!connect(device)) return false

            // Usar exactamente el mismo formato de impresión
            printTicket(
                turno = ticket.codigo,
                dui = ticket.dui,
                nombre = ticket.nombre,
                servicio = ticket.descripcion
            )

            close()

            return true

        } catch (e: Exception) {
            Log.e("REIMPRIMIR", "Error al reimprimir: ${e.message}")
            return false
        }
    }



    fun close() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
    }
}
