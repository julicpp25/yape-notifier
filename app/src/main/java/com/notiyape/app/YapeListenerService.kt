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
import java.util.regex.Pattern

class YapeListenerService : NotificationListenerService() {

    private val client = OkHttpClient()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        
        // Escucha notificaciones de Yape
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

        var remitente = "Cliente"
        var monto = "0.00"

        // 1. Extraer Monto: Busca "S/" seguido del número con decimales
        val patronMonto = Pattern.compile("S/\\.?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcherMonto = patronMonto.matcher(texto)
        if (matcherMonto.find()) {
            monto = matcherMonto.group(1)?.replace(",", ".") ?: "0.00"
        }

        // 2. Extraer Remitente: Extrae el nombre antes de "te envió" y quita prefijos como "Yape!"
        val patronNombre = Pattern.compile("(?:.*(?:Yape!|Yape)\\s*)?(.+?)\\s+te\\s+(?:envi[oó]|mand[oó]|transfiri[oó])", Pattern.CASE_INSENSITIVE)
        val matcherNombre = patronNombre.matcher(texto)
        if (matcherNombre.find()) {
            remitente = matcherNombre.group(1)
                ?.replace("¡", "")
                ?.replace("!", "")
                ?.trim() ?: "Cliente"
        } else {
            remitente = texto
        }

        // 3. Enviar a Google Sheets
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
