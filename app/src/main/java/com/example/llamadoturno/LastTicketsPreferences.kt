package com.example.llamadoturno

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object LastTicketsPreferences {

    private const val PREF_NAME = "last_tickets"
    private const val KEY_LIST = "tickets"

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveTicket(
        context: Context,
        codigo: String,
        descripcion: String,
        nombre: String,
        dui: String,
        tiempo: String
    ) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val jsonString = prefs.getString(KEY_LIST, "[]") ?: "[]"
        val oldArray = JSONArray(jsonString)

        val newArray = JSONArray()

        // NUEVO TICKET
        val newTicket = JSONObject().apply {
            put("codigo", codigo)
            put("descripcion", descripcion)
            put("nombre", nombre)
            put("dui", dui)
            put("tiempo", tiempo)

            // 🔥🔥🔥 FECHA OBLIGATORIA
            put("fecha", LocalDate.now().toString())
        }

        newArray.put(newTicket)

        // COPIAR ANTERIORES (máximo 5)
        for (i in 0 until oldArray.length()) {
            if (newArray.length() >= 5) break
            newArray.put(oldArray.getJSONObject(i))
        }

        prefs.edit().putString(KEY_LIST, newArray.toString()).apply()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun getTickets(context: Context): List<TicketData> {

        val prefs = context.getSharedPreferences("last_tickets", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("tickets", "[]") ?: "[]"

        val jsonArray = try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            JSONArray()
        }

        val hoy = LocalDate.now().toString()
        val nuevaLista = JSONArray()   // aquí guardaremos SOLO los tickets válidos
        val lista = mutableListOf<TicketData>()

        for (i in 0 until jsonArray.length()) {

            val obj = try {
                jsonArray.getJSONObject(i)
            } catch (e: Exception) {
                continue
            }

            val fechaGuardada = obj.optString("fecha", "")  // fecha real del ticket

            // ----------- ELIMINA tickets de días anteriores -----------
            if (fechaGuardada != hoy) {
                continue
            }

            // ----------- CARGA SOLO LOS DEL DÍA -----------------------
            val codigo = obj.optString("codigo", "—")
            val descripcion = obj.optString("descripcion", "—")
            val nombre = obj.optString("nombre", "Desconocido")
            val dui = obj.optString("dui", "—")
            val tiempo = obj.optString("tiempo", "—")

            lista.add(
                TicketData(
                    codigo = codigo,
                    descripcion = descripcion,
                    nombre = nombre,
                    dui = dui,
                    tiempo = tiempo,
                    fecha = fechaGuardada
                )
            )

            // Mantener SOLO los actuales en memoria
            nuevaLista.put(obj)
        }

        // 🔥 Guardamos solo los tickets de hoy (los demás quedan eliminados)
        prefs.edit().putString("tickets", nuevaLista.toString()).apply()

        return lista
    }


}

data class TicketData(
    val codigo: String,
    val descripcion: String,
    val nombre: String,
    val dui: String,
    val tiempo: String,
    val fecha: String // 🔥 NUEVO CAMPO
)
