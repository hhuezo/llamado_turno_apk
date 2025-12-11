package com.example.llamadoturno

import android.content.Context
import android.content.SharedPreferences

object PrinterPreferences {

    private const val PREF_NAME = "printer_prefs"
    private const val KEY_PRINTER_NAME = "printer_name"
    private const val KEY_PRINTER_ADDRESS = "printer_address"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun savePrinter(context: Context, name: String, address: String) {
        prefs(context).edit()
            .putString(KEY_PRINTER_NAME, name)
            .putString(KEY_PRINTER_ADDRESS, address)
            .apply()
    }

    fun getPrinterName(context: Context): String? =
        prefs(context).getString(KEY_PRINTER_NAME, null)

    fun getPrinterAddress(context: Context): String? =
        prefs(context).getString(KEY_PRINTER_ADDRESS, null)

    fun clearPrinter(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
