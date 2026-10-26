package com.notiyape.app

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

class YapeListenerService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private val client = OkHttpClient()
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbyOiEdKFVc08ti51lu4Q4UT2qAadmiR3D6jIjUxul8coZHtTQNHgUvO-0LzUuu11ewOFQ/exec"
    
    // Motor de Voz en Español
    private var motorVoz: TextToSpeech? = null
    private var vozLista = false

    override fun onCreate() {
        super.onCreate()
        // Inicializa el motor de voz al arrancar el servicio
        motorVoz = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Configurar voz en Español
            val locSpanish = Locale("es", "PE")
            val resultado = motorVoz?.setLanguage(locSpanish)
            if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
                motorVoz?.setLanguage(Locale("es", "ES"))
            }
            vozLista = true
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        
        // Detecta Yape, Plin o Bancos
        val esYape = (pkg == "com.bcp.innovacxion.yapeapp")
        val esInterbank = (pkg == "pe.com.interbank.mobilebanking")
        val esBBVA = (pkg == "com.bbva.bbvacontigo")
        val esScotiabank = (pkg == "com.scotiabank.peru.pe")

        if (esYape || esInterbank || esBBVA || esScotiabank) {
            val extras = sbn.notification.extras
            val text = extras.getCharSequence("android.text")?.toString() 
                       ?: extras.getCharSequence("android.bigText")?.toString() 
                       ?: extras.getString("android.title") ?: ""

            if (text.isNotEmpty()) {
                val appOrigen = if (esYape) "Yape" else "Plin"
                procesarPago(text, appOrigen)
            }
        }
    }

    private fun procesarPago(texto: String, appOrigen: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
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

        // ================= 3. HABLAR EL COBRO EN VOZ ALTA =================
        hablarEnVozAlta(appOrigen, monto)

        // 4. Enviar al Servidor Central (Telegram + Google Sheets)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = """{"codigoTienda": "$codigoTienda", "nombre": "$remitente ($appOrigen)", "monto": "$monto"}"""
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(SERVIDOR_URL).post(body).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hablarEnVozAlta(appOrigen: String, monto: String) {
        if (vozLista && motorVoz != null) {
            // Frase que dirá el celular: "¡Te llegó un Yape de 15 soles!"
            val frase = if (monto == "1" || monto == "1.00") {
                "Te llegó un $appOrigen de un sol"
            } else {
                "Te llegó un $appOrigen de $monto soles"
            }
            motorVoz?.speak(frase, TextToSpeech.QUEUE_FLUSH, null, "ID_COBRO_VOZ")
        }
    }

    override fun onDestroy() {
        motorVoz?.stop()
        motorVoz?.shutdown()
        super.onDestroy()
    }
}
