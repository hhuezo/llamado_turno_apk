package com.example.llamadoturno

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object UsuarioApi {

    private val client = OkHttpClient()

    fun obtenerUsuario(dui: String, callback: (Boolean, String) -> Unit) {
        val url = "http://192.162.20.95/llamado_turno/public/api/get_data_usuario/$dui"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(false, "Error de conexión con el servidor")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    callback(false, "El DUI no fue encontrado")
                    return
                }

                try {
                    val json = JSONObject(body)
                    val nombre = json.getJSONObject("data").getString("nombre")

                    callback(true, nombre)

                } catch (e: Exception) {
                    callback(false, "Formato inválido en la respuesta")
                }
            }
        })
    }
}
