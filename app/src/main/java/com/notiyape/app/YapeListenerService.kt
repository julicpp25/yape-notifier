package com.notiyape.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class YapeListenerService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private val client = OkHttpClient()
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbyOiEdKFVc08ti51lu4Q4UT2qAadmiR3D6jIjUxul8coZHtTQNHgUvO-0LzUuu11ewOFQ/exec"
    
    private var motorVoz: TextToSpeech? = null
    private var vozLista = false

    // Frases cortas, humanas y naturales (sin monotonía)
    private val frasesMotivadoras = listOf(
        "¡A seguir sumando!",
        "¡Excelente venta!",
        "¡Vamos por más!",
        "¡El negocio está imparable!",
        "¡Buen trabajo!",
        "¡Caja sumando!",
        "¡A seguir facturando!",
        "¡Qué buena racha!"
    )

    override fun onCreate() {
        super.onCreate()
        // Forzar el motor neuronal de Google para voz humana de alta calidad
        try {
            motorVoz = TextToSpeech(this, this, "com.google.android.tts")
        } catch (e: Exception) {
            motorVoz = TextToSpeech(this, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locSpanish = Locale("es", "PE")
            motorVoz?.language = locSpanish
            
            // Tono y velocidad optimizados para sonar como persona real
            motorVoz?.setPitch(1.04f)
            motorVoz?.setSpeechRate(1.02f)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            motorVoz?.setAudioAttributes(audioAttributes)

            // Buscar la voz de más alta calidad instalada
            try {
                val mejorVoz = motorVoz?.voices?.filter {
                    it.locale.language == "es" && !it.name.contains("low")
                }?.maxByOrNull { it.quality }

                if (mejorVoz != null) {
                    motorVoz?.voice = mejorVoz
                }
            } catch (e: Exception) {}

            vozLista = true
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
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
                val metodo = if (esYape) "Yape" else "Plin"
                procesarPago(text, metodo)
            }
        }
    }

    private fun procesarPago(texto: String, metodo: String) {
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

        // 3. Hablar en voz alta humana
        hablarEnVozAlta(metodo, monto)

        // 4. Guardar en el historial local del celular para verlo en la app
        guardarCobroLocal(remitente, monto, metodo)

        // 5. Enviar a Google Sheets y Telegram
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = """{"codigoTienda": "$codigoTienda", "nombre": "$remitente ($metodo)", "monto": "$monto"}"""
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(SERVIDOR_URL).post(body).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hablarEnVozAlta(metodo: String, monto: String) {
        if (vozLista && motorVoz != null) {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
            } catch (e: Exception) {}

            val fraseMonto = if (monto == "1" || monto == "1.00") "un sol" else "$monto soles"
            val animo = frasesMotivadoras.random()
            val discurso = "¡Te llegó un $metodo de $fraseMonto! $animo"

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            motorVoz?.speak(discurso, TextToSpeech.QUEUE_FLUSH, params, "ID_TTS")
        }
    }

    private fun guardarCobroLocal(nombre: String, monto: String, metodo: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val historialStr = prefs.getString("historial_cobros", "[]") ?: "[]"
        val array = JSONArray(historialStr)

        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val nuevoCobro = JSONObject().apply {
            put("hora", horaActual)
            put("fecha", fechaHoy)
            put("nombre", nombre)
            put("monto", monto)
            put("metodo", metodo)
        }

        // Agregar el nuevo al inicio de la lista
        val nuevoArray = JSONArray()
        nuevoArray.put(nuevoCobro)
        for (i in 0 until minOf(array.length(), 40)) {
            nuevoArray.put(array.getJSONObject(i))
        }

        prefs.edit().putString("historial_cobros", nuevoArray.toString()).apply()
    }

    override fun onDestroy() {
        motorVoz?.stop()
        motorVoz?.shutdown()
        super.onDestroy()
    }
}
