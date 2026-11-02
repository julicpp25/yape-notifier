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
   
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbw0_0UBYuP2yVjPHOmMDyLlehzvT41nuJPPCOnRw2Zd1V1Y57xI2nHUyVI1nSBhR7F_3w/exec"
    private var motorVoz: TextToSpeech? = null
    private var vozLista = false

    private val frasesMotivadoras = listOf(
       "¡El éxito no se detiene!",
        "¡Suma y sigue!",
        "¡Negocio en marcha!",
        "¡Cada yapeo cuenta!",
        "¡A toda máquina!",
        "¡Un paso más cerca de la meta!",
        "¡El esfuerzo rinde frutos!",
        "¡Rumbo a la cima!",
        "¡Fuerza y constante crecimiento!",
        "¡Imparables hoy!",
        "¡Dale que se puede!",
        "¡Hoy se rompen récords!",
        "¡Constancia y éxito!",
        "¡Seguimos avanzando!",
        "¡La meta está más cerca!",
        "¡A paso firme!",
        "¡El trabajo duro se nota!",
        "¡El negocio no descansa!",
        "¡Camino a un día increíble!",
        "¡Paso a paso, llegando lejos!",
        "¡Caja llena, corazón contento!",
        "¡Sonando la caja!",
        "¡Más saldo a favor!",
        "¡La caja registradora no para!",
        "¡Suma que suma!",
        "¡Ingreso confirmado!",
        "¡Dinero al bolsillo!",
        "¡Cuenta recargada!",
        "¡Facturando a lo grande!",
        "¡Yape directo al éxito!",
        "¡Multiplicando ingresos!",
        "¡Caja verde!",
        "¡Sumando números verdes!",
        "¡La cuenta sigue creciendo!",
        "¡Otro yape para la lista!",
        "¡Balance positivo!",
        "¡Entrada confirmada!",
        "¡Sumando con fuerza!",
        "¡Un yape más a la cuenta!",
        "¡Caja en movimiento constante!",
        "¡No paramos de vender!",
        "¡Racha ganadora!",
        "¡Ritmo imbatible!",
        "¡Cliente satisfecho, negocio contento!",
        "¡A toda velocidad!",
        "¡Con la bendición del día!",
        "¡El día está encendido!",
        "¡Fluidez total en caja!",
        "¡Moviendo la mercadería!",
        "¡El día promete bastante!",
        "¡Sigue la buena racha!",
        "¡Nada nos detiene!",
        "¡Negocio activo al 100%!",
        "¡Qué gran jornada!",
        "¡Con todo el entusiasmo!",
        "¡Ventana de oportunidades abierta!",
        "¡Energía positiva en cada venta!",
        "¡Esto recién empieza!",
        "¡Modo imparable activado!",
        "¡Ventas en serie!",
        "¡Orgullo emprendedor!",
        "¡Fruto de tu dedicación!",
        "¡Premio al trabajo bien hecho!",
        "¡Haciendo historia hoy!",
        "¡Excelencia en cada atención!",
        "¡Tu esfuerzo vale oro!",
        "¡Creciendo como los grandes!",
        "¡Construyendo el futuro!",
        "¡Especialistas en vender!",
        "¡Bien jugado!",
        "¡Un triunfo más!",
        "¡Premio a la constancia!",
        "¡Visión de éxito!",
        "¡Avanzando a pasos gigantes!",
        "¡Cosechando resultados!",
        "¡Determinación total!",
        "¡Marca registrada de éxito!",
        "¡Superando expectativas!",
        "¡Imposible frenar este crecimiento!",
        "¡Triunfo asegurado!",
        "¡Otro más!",
        "¡Yape recibido!",
        "¡Venta lograda!",
        "¡Aprobado!",
        "¡Confirmado!",
        "¡Suma exitosa!",
        "¡Directo a la caja!",
        "¡Punto para el negocio!",
        "¡Suma registrada!",
        "¡Pago al instante!",
        "¡Venta al hilo!",
        "¡Check en la lista!",
        "¡Concretado!",
        "¡Cobro exitoso!",
        "¡Yapeo en línea!",
        "¡Otro cliente feliz!",
        "¡Suma instantánea!",
        "¡Operación lista!",
        "¡Enviado y recibido!",
        "¡Caja actualizada!",
    )

    override fun onCreate() {
        super.onCreate()
        try {
            motorVoz = TextToSpeech(this, this, "com.google.android.tts")
        } catch (e: Exception) {
            motorVoz = TextToSpeech(this, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            motorVoz?.language = Locale("es", "PE")
            motorVoz?.setPitch(1.04f)
            motorVoz?.setSpeechRate(1.02f)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            motorVoz?.setAudioAttributes(audioAttributes)

            try {
                val mejorVoz = motorVoz?.voices?.filter {
                    it.locale.language == "es" && !it.name.contains("low")
                }?.maxByOrNull { it.quality }
                if (mejorVoz != null) motorVoz?.voice = mejorVoz
            } catch (e: Exception) {}

            vozLista = true
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName

        // DETECCIÓN MULTIBANCO (YAPE + TODOS LOS PLIN DE PERÚ)
        val esYape = (pkg == "com.bcp.innovacxion.yapeapp")
        val esInterbank = (pkg == "pe.com.interbank.mobilebanking")
        val esBBVA = (pkg == "com.bbva.bbvacontigo")
        val esScotiabank = (pkg == "com.scotiabank.peru.pe")
        val esBanbif = (pkg == "com.banbif.bifmovil")

        if (esYape || esInterbank || esBBVA || esScotiabank || esBanbif) {
            val extras = sbn.notification.extras
            val text = extras.getCharSequence("android.text")?.toString()
                ?: extras.getCharSequence("android.bigText")?.toString()
                ?: extras.getString("android.title") ?: ""

            if (text.isNotEmpty()) {
                val metodo = when {
                    esYape -> "Yape"
                    esInterbank -> "Plin Interbank"
                    esBBVA -> "Plin BBVA"
                    esScotiabank -> "Plin Scotiabank"
                    else -> "Plin / Transferencia"
                }
                procesarPago(text, metodo)
            }
        }
    }

    private fun procesarPago(texto: String, metodo: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val codigoTienda = prefs.getString("codigo_tienda", "") ?: return

        // Extraer Monto
        val regexMonto = Regex("""S/\.?\s*([\d]+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)
        val matchMonto = regexMonto.find(texto)
        val monto = matchMonto?.groups?.get(1)?.value?.replace(",", ".") ?: "0.00"

        // Extraer Remitente (Corregido con comillas vacías)
        var remitente = "Cliente"
        if (texto.contains("te envi", ignoreCase = true) || texto.contains("transfirió", ignoreCase = true) || texto.contains("pago de", ignoreCase = true)) {
            val antes = texto.split(Regex("(te envi[oó]|transfiri[oó]|pago de)", RegexOption.IGNORE_CASE))[0]
            remitente = antes.replace(Regex("(¡|!|Te llegó un Yape|Yape|Plin)", RegexOption.IGNORE_CASE), "").trim()
        } else {
            remitente = texto
        }
        if (remitente.isEmpty()) remitente = "Cliente"

        if (prefs.getBoolean("voz_activa", true)) {
            hablarEnVozAlta(metodo, monto)
        }

        guardarCobroLocal(remitente, monto, metodo)

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

        val nuevoArray = JSONArray().apply {
            put(nuevoCobro)
            for (i in 0 until minOf(array.length(), 40)) {
                put(array.getJSONObject(i))
            }
        }

        prefs.edit().putString("historial_cobros", nuevoArray.toString()).apply()
    }

    override fun onDestroy() {
        motorVoz?.stop()
        motorVoz?.shutdown()
        super.onDestroy()
    }
}
