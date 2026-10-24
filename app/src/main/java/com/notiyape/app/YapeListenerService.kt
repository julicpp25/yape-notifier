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
        // Detecta Yape oficial
        if (pkg == "com.bcp.innovacxion.yapeapp") {
            val extras = sbn.notification.extras
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            procesarYape(text)
        }
    }

    private fun procesarYape(texto: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val webhookUrl = prefs.getString("sheet_url", "") ?: ""

        if (webhookUrl.isEmpty()) return

        val patron = Pattern.compile("(.+?)\\s+te\\s+envi[oó]\\s+S/\\s*([0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = patron.matcher(texto)

        var remitente = "Desconocido"
        var monto = "0.00"

        if (matcher.find()) {
            remitente = matcher.group(1)?.trim() ?: remitente
            monto = matcher.group(2)?.trim() ?: monto
        } else {
            remitente = texto
        }

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
