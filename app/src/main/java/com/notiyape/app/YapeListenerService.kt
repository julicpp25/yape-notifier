package com.notiyape.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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
    
    private var motorVoz: TextToSpeech? = null
    private var vozLista = false

    // Colección de Frases Motivacionales para el Negocio
    private val frasesMotivacionales = listOf(
        "¡Excelente! El esfuerzo da frutos, ¡vamos por más ventas!",
        "¡Plata en mano! Sigue así, el negocio está imparable.",
        "¡Cobro confirmado! Con fe y trabajo, hoy rompemos récord.",
        "¡Golazo! Cada sol cuenta, ¡que sigan lloviendo los cobros!",
        "¡Buen trabajo! Clientes felices, caja llena. ¡A seguir creciendo!",
        "¡Qué buena venta! Sigue dándole con todo, el éxito es tuyo.",
        "¡Caja sumando! La constancia es la clave, ¡vamos por el siguiente!",
        "¡Día productivo! Tu trabajo vale oro, ¡a seguir facturando!"
    )

    override fun onCreate() {
        super.onCreate()
        motorVoz = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locSpanish = Locale("es", "PE")
            motorVoz?.language = locSpanish
            
            // Ajuste fino para voz más natural y enérgica
            motorVoz?.setPitch(1.02f)
            motorVoz?.setSpeechRate(0.98f)

            // Configurar canal de audio multimedia con máxima prioridad
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            motorVoz?.setAudioAttributes(audioAttributes)

            // Seleccionar la voz de mayor fidelidad del sistema
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

        // ================= 3. HABLAR CON ALTO VOLUMEN Y FRASE MOTIVADORA =================
        hablarEnVozAltaConMotivacion(appOrigen, monto)

        // 4. Enviar a Servidor Central (Telegram + Google Sheets)
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

    private fun hablarEnVozAltaConMotivacion(appOrigen: String, monto: String) {
        if (vozLista && motorVoz != null) {
            try {
                // Subir automáticamente el volumen multimedia al 90% para que suene fuerte
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val volumenMaximo = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val volumenDeseado = (volumenMaximo * 0.90).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumenDeseado, 0)
            } catch (e: Exception) {}

            // Construir la frase dinámica
            val fraseCobro = if (monto == "1" || monto == "1.00") {
                "Te llegó un $appOrigen de un sol."
            } else {
                "Te llegó un $appOrigen de $monto soles."
            }

            // Elegir una frase motivadora al azar
            val motivacion = frasesMotivacionales.random()
            val discursoCompleto = "$fraseCobro $motivacion"

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // Volumen máximo del motor
            }

            motorVoz?.speak(discursoCompleto, TextToSpeech.QUEUE_FLUSH, params, "ID_COBRO_VOZ")
        }
    }

    override fun onDestroy() {
        motorVoz?.stop()
        motorVoz?.shutdown()
        super.onDestroy()
    }
}
