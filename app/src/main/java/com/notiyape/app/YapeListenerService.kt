package com.notiyape.app

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class YapeListenerService : NotificationListenerService() {

    private val client = OkHttpClient()
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbyOiEdKFVc08ti51lu4Q4UT2qAadmiR3D6jIjUxul8coZHtTQNHgUvO-0LzUuu11ewOFQ/exec"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        
        // Escucha Yape
        if (pkg == "com.bcp.innovacxion.yapeapp") {
            val extras = sbn.notification.extras
            val text = extras.getCharSequence("android.text")?.toString() 
                       ?: extras.getCharSequence("android.bigText")?.toString() 
                       ?: extras.getString("android.title") ?: ""

            if (text.isNotEmpty()) {
                procesarYape(text)
            }
        }
    }

    private fun procesarYape(texto: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        // Si está vacío, usa TEST-01 por defecto para no fallar jamás
        val codigoTienda = prefs.getString("codigo_tienda", "TEST-01")?.ifEmpty { "TEST-01" } ?: "TEST-01"

        // 1. Extraer Monto
        val regexMonto = Regex("""S/\.?\s*([\d]+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)
        val matchMonto = regexMonto.find(texto)
        val monto = matchMonto?.groups?.get(1)?.value?.replace(",", ".") ?: "0.00"

        // 2. Extraer Remitente
        var remitente = "Cliente"
        if (texto.contains("te envi", ignoreCase = true)) {
            val antesDeEnvio = texto.split(Regex("te envi[oó]", RegexOption.IGNORE_CASE))[0]
            remitente = antesDeEnvio
                .replace("¡", "")
                .replace("!", "")
                .replace("Te llegó un Yape", "", ignoreCase = true)
                .replace("Yape", "", ignoreCase = true)
                .trim()
        } else {
            remitente = texto
        }

        // 3. Enviar al Servidor
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = """{"codigoTienda": "$codigoTienda", "nombre": "$remitente", "monto": "$monto"}"""
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(SERVIDOR_URL).post(body).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
