package com.example.llamadoturno

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.OutputStream
import java.lang.reflect.Method
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

    // TRUCO FUNDAMENTAL PARA PT-210 (createRfcommSocket FALLA)
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            Log.d(TAG, "Connecting to: ${device.name} (${device.address})")

            // 1) Intentar conexión normal
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket!!.connect()
            } catch (e: Exception) {
                Log.w(TAG, "Default RFCOMM failed, trying fallback...")

                // 2) FALLBACK: método oculto "createRfcommSocket"
                val method: Method = device.javaClass.getMethod(
                    "createRfcommSocket", Int::class.javaPrimitiveType
                )
                socket = method.invoke(device, 1) as BluetoothSocket
                socket!!.connect()
            }

            outputStream = socket!!.outputStream

            Log.d(TAG, "Bluetooth connection established successfully.")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to PT-210: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // ✨ FORMATO EXACTO QUE FUNCIONA EN PT-210
    fun printTicket(turno: String) {
        try {

            val ESC = 0x1B.toByte()
            val GS = 0x1D.toByte()

            val boldOn = byteArrayOf(ESC, 0x45, 0x01)
            val boldOff = byteArrayOf(ESC, 0x45, 0x00)
            val center = byteArrayOf(ESC, 0x61, 0x01)
            val left = byteArrayOf(ESC, 0x61, 0x00)
            val doubleSize = byteArrayOf(GS, 0x21, 0x11)
            val normalSize = byteArrayOf(GS, 0x21, 0x00)

            outputStream?.apply {

                write(center)
                write(boldOn)
                write(doubleSize)
                write("TURNO\n".toByteArray(Charsets.ISO_8859_1))

                write(normalSize)
                write(boldOff)
                write("------------------------------\n".toByteArray())

                write(boldOn)
                write("NÚMERO: $turno\n".toByteArray(Charsets.ISO_8859_1))
                write(boldOff)

                write("------------------------------\n".toByteArray())

                write(left)
                write("Gracias por esperar\n".toByteArray(Charsets.ISO_8859_1))
                write("Su turno será llamado\n".toByteArray(Charsets.ISO_8859_1))

                write("\n\n\n".toByteArray())

                flush()
                Thread.sleep(150)     // <---- IMPORTANTE
                write(byteArrayOf(0x0A)) // salto extra
                flush()
            }

            Log.d("BT_PRINTER", "Styled ticket sent.")

        } catch (e: Exception) {
            Log.e("BT_PRINTER", "Print error PT-210 styled: ${e.message}")
        }
    }



    fun close() {
        try {
            Thread.sleep(200)  // ← Fix para PT-210
            outputStream?.close()
            socket?.close()
            Log.d("BT_PRINTER", "Bluetooth connection closed.")
        } catch (e: Exception) {
            Log.e("BT_PRINTER", "Error closing Bluetooth connection: ${e.message}")
        }
    }

}
