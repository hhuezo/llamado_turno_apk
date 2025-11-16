package com.example.llamadoturno

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

object TurnoApi {

    private val client = OkHttpClient()

    fun enviarTurno(departamentoId: Int, callback: (Boolean, String) -> Unit) {
        val url = "http://192.168.1.206/llamado_turno/public/api/turnos"

        val json = JSONObject().apply {
            put("departamento_id", departamentoId)
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object: Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(false, "No se pudo conectar con el servidor")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    callback(false, "No se pudo generar el turno. Intente de nuevo.")
                    return
                }

                try {
                    val jsonResp = JSONObject(bodyString)
                    val turno = jsonResp.getJSONObject("turno")
                    val codigo = turno.getString("codigo_turno")

                    callback(true, "Turno generado: $codigo")

                } catch (e: Exception) {
                    callback(true, "Turno generado correctamente")
                }
            }
        })
    }
}
