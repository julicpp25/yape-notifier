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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        
        // Detecta notificación de Yape
        if (pkg == "com.bcp.innovacxion.yapeapp") {
            val extras = sbn.notification.extras
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            if (text.isNotEmpty()) {
                procesarYape(text)
            }
        }
    }

    private fun procesarYape(texto: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val webhookUrl = prefs.getString("sheet_url", "") ?: ""

        if (webhookUrl.isEmpty()) return

        // 1. EXTRAER EL MONTO: Busca "S/" seguido de cualquier número (tenga o no decimales)
        val regexMonto = Regex("""S/\.?\s*([\d]+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)
        val matchMonto = regexMonto.find(texto)
        val monto = matchMonto?.groups?.get(1)?.value?.replace(",", ".") ?: "0.00"

        // 2. EXTRAER EL NOMBRE: Corta antes de "te envió" y elimina la palabra "Yape!"
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

        // 3. ENVIAR A GOOGLE SHEETS
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = """{"nombre": "$remitente", "monto": "$monto"}"""
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(webhookUrl).post(body).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
