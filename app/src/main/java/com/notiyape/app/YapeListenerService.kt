package com.notiyape.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbw0_0UBYuP2yVjPHOmMDyLlehzvT41nuJPPCOnRw2Zd1V1Y57xI2nHUyVI1nSBhR7F_3w/exec"
    private var motorVoz: TextToSpeech? = null
    private var vozLista = false
    private val client = OkHttpClient()

    private val frasesMotivadoras = listOf(
        "¡A seguir vendiendo!",
        "¡Excelente venta!",
        "¡Negocio en marcha!",
        "¡Sumando ganancias!",
        "¡Cliente satisfecho!",
        "¡El negocio crece!",
        "¡Buena racha!",
        "¡Caja llena!",
        "¡Éxito total!",
        "¡Seguimos facturando!",
        "¡Otro cliente feliz!",
        "¡Meta del día más cerca!",
        "¡Qué buena venta!",
        "¡Golpe de suerte!",
        "¡Imparables hoy!",
        "¡Venta confirmada!",
        "¡Dinero en cuenta!",
        "¡Vamos por más!",
        "¡Todo un éxito!",
        "¡Gran atención!",
        "¡Caja activa!",
        "¡Produciendo al cien!",
        "¡Negocio imparable!",
        "¡Así da gusto vender!",
        "¡Qué buen ritmo!",
        "¡Excelente servicio!",
        "¡Cobro recibido!",
        "¡Directo a la ganancia!",
        "¡Suma y sigue!",
        "¡Día productivo!",
        "¡Victoria comercial!",
        "¡Creciendo día a día!",
        "¡A paso firme!",
        "¡Buen trabajo!",
        "¡Gran trabajo en caja!",
        "¡Facturando con fuerza!",
        "¡Siempre adelante!",
        "¡Cliente fiel!",
        "¡Pago verificado!",
        "¡Negocio próspero!",
        "¡Gran vibra hoy!",
        "¡Récord de ventas!",
        "¡El esfuerzo paga!",
        "¡Buen comercio!",
        "¡Avanzando seguro!",
        "¡Éxito garantizado!",
        "¡Caja en positivo!",
        "¡Sonríe, estamos vendiendo!",
        "¡Plata en mano!",
        "¡Todo marcha bien!",
        "¡Ventas arriba!",
        "¡Día ganador!",
        "¡Así se hace!",
        "¡Excelente!",
        "¡Maravilloso!",
        "¡Genial!",
        "¡Fantástico!",
        "¡Fenomenal!",
        "¡Sensacional!",
        "¡Magnífico!",
        "¡Estupendo!",
        "¡Extraordinario!",
        "¡Formidable!",
        "¡Brillante!",
        "¡Impresionante!",
        "¡Bárbaro!",
        "¡De lujo!",
        "¡Calidad total!",
        "¡Venta redonda!",
        "¡Buen ritmo comercial!",
        "¡Sigue la buena racha!",
        "¡Caja sonriente!",
        "¡Comercio activo!",
        "¡Clientes contentos!",
        "¡Ganancia asegurada!",
        "¡Venta exitosa!",
        "¡Meta lograda!",
        "¡Paso a paso al éxito!",
        "¡Creciendo juntos!",
        "¡El negocio rinde!",
        "¡Jornada brillante!",
        "¡Fuerza comercial!",
        "¡Qué alegría vender!",
        "¡Motivación pura!",
        "¡Buen dinerito!",
        "¡Premio al esfuerzo!",
        "¡Dedicación y éxito!",
        "¡Seguimos sumando!",
        "¡Negocio ganador!",
        "¡Caja triunfante!",
        "¡La tienda prospera!",
        "¡Venta impecable!",
        "¡Atención de primera!",
        "¡Un éxito más!",
        "¡Ganancias al tope!",
        "¡La caja no descansa!",
        "¡Cero deudas, pura venta!",
        "¡Clientes de primera!",
        "¡Día redondo!",
        "¡Cosechando frutos!",
        "¡El trabajo rinde!",
        "¡Firme en las ventas!",
        "¡Poder comercial!",
        "¡Adelante siempre!",
        "¡Buena estrella hoy!",
        "¡Seguridad y confianza!",
        "¡Venta rápida!",
        "¡Dinero seguro!",
        "¡Caja en movimiento!",
        "¡Qué buen día!",
        "¡Un cliente más!",
        "¡Venta asegurada!",
        "¡Negocio en ascenso!",
        "¡Punto para la caja!",
        "¡Todo un campeón!",
        "¡Gran momento!",
        "¡Así se trabaja!",
        "¡Venta anotada!",
        "¡Éxito continuo!",
        "¡Ganancias reales!",
        "¡Orgullo de negocio!",
        "¡Caja productiva!",
        "¡Venta concretada!",
        "¡Excelente balance!",
        "¡Día provechoso!",
        "¡Ventas sin parar!",
        "¡Flujo constante!",
        "¡El comercio no para!",
        "¡Venta registrada!",
        "¡A toda máquina!",
        "¡Buena faena!",
        "¡Todo en orden!",
        "¡Caja veloz!",
        "¡Cobro exitoso!",
        "¡Vamos por el récord!",
        "¡Plata segura!",
        "¡Venta limpia!",
        "¡Operación lista!",
        "¡Enviado y recibido!",
        "¡Caja actualizada!"
    )

    override fun onCreate() {
        super.onCreate()
        iniciarEscudoForeground24x7()
        try {
            motorVoz = TextToSpeech(this, this, "com.google.android.tts")
        } catch (e: Exception) {
            motorVoz = TextToSpeech(this, this)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        iniciarEscudoForeground24x7()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // ESCUDO AUTO-RECONEXIÓN: Si el sistema intenta desconectar el lector, se reconecta solo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, YapeListenerService::class.java))
            } catch (e: Exception) {}
        }
    }

    private fun iniciarEscudoForeground24x7() {
        try {
            val channelId = "yapealdia_guardia_24x7"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    channelId,
                    "Guardia 24/7 YapeAlDía",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Mantiene el lector de cobros activo con pantalla apagada o minimizada"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(canal)
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, channelId)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                    .setContentTitle("🟢 YapeAlDía Activo 24/7")
                    .setContentText("Escuchando y sincronizando cobros en segundo plano")
                    .setOngoing(true)
                    .setContentIntent(pi)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                    .setContentTitle("🟢 YapeAlDía Activo 24/7")
                    .setContentText("Escuchando y sincronizando cobros en segundo plano")
                    .setOngoing(true)
                    .setContentIntent(pi)
                    .build()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(777, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(777, notif)
            }
        } catch (e: Exception) {}
    }

    private fun adquirirWakeLockTemporal(duracionMs: Long = 6500L) {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "YapeAlDia::CobroWakeLock"
            )
            wl.acquire(duracionMs)
        } catch (e: Exception) {}
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

    companion object {
        @Volatile
        private var ultimaHuellaMemoria: String = ""
        @Volatile
        private var ultimoTsMemoria: Long = 0L
        private val lockDedup = Any()
    }

    private fun procesarPago(texto: String, metodo: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val codigoTienda = prefs.getString("codigo_tienda", "") ?: return

        // =========================================================================
        // ESCUDO 0: BLOQUEO DE YAPE PERSONAL DEL CAJERO
        // Solo el celular del Dueño (Receptor Oficial) debe leer notificaciones de Yape/Plin.
        // Si este celular está en modo Cajero, se ignoran todas las notificaciones locales
        // para que los Yapes personales del empleado JAMÁS se registren en la tienda.
        // =========================================================================
        val esCelularDuenoReceptor = prefs.getBoolean("es_celular_dueño_receptor", false)
        val esModoCajero = prefs.getBoolean("cajero_logueado", false)
        if (!esCelularDuenoReceptor || esModoCajero) {
            return
        }

        // =========================================================================
        // ESCUDO 1: DESCARTAR PUBLICIDAD, CRÉDITOS Y PRÉSTAMOS
        // =========================================================================
        val esPublicidad = texto.contains(
            Regex("(crédito|credito|préstamo|prestamo|pre-aprobado|aprobado al instante|pide tu|promoción|promo|descuento|sorteo|gana|premios|seguro|paga tus servicios|solicita)", RegexOption.IGNORE_CASE)
        )
        if (esPublicidad) {
            return // Se descarta en silencio absoluto, no es un cobro
        }

        // =========================================================================
        // ESCUDO 2: VERBO DE PAGO OBLIGATORIO (SOPORTA NUEVAS VARIANTES YAPE & PLIN)
        // =========================================================================
        val tieneVerboPago = texto.contains(
            Regex("(te envi|te mand|transfiri|recibiste un pago|pago de|te yape|yapeó|yapeo|te pline|plineó|plineo|recibiste un yape|recibiste un plin|recibiste)", RegexOption.IGNORE_CASE)
        )
        if (!tieneVerboPago) {
            return // Si no tiene frase de transferencia, se ignora
        }

        // =========================================================================
        // 3. EXTRAER MONTO EN SOLES (Ej: S/ 15.00, S/. 15, S/15,50)
        // =========================================================================
        val regexMonto = Regex("""S/\.?\s*([\d]+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)
        val matchMonto = regexMonto.find(texto) ?: return
        val monto = matchMonto.groups[1]?.value?.replace(",", ".") ?: return
        val montoDouble = monto.toDoubleOrNull() ?: 0.0
        if (montoDouble <= 0.0) return

        // =========================================================================
        // 4. EXTRAER REMITENTE LIMPIO (Soporta nombres antes o después del verbo)
        // =========================================================================
        var remitente = "Cliente"
        val regexVerbos = Regex("(te envi[oó]|transfiri[oó]|te mand[oó]|recibiste un pago de|pago de|te yape[oó]|yape[oó]|te pline[oó]|pline[oó])", RegexOption.IGNORE_CASE)

        if (texto.contains(regexVerbos)) {
            val antes = texto.split(regexVerbos)[0]
            val posible = antes.replace(Regex("(¡|!|Te llegó un Yape|Yape|Plin|Pago|Notificación)", RegexOption.IGNORE_CASE), "").trim()
            if (posible.isNotEmpty() && posible.length >= 3) {
                remitente = posible
            }
        }

        // Si el nombre aparece después ("de Juan Pérez")
        if (remitente == "Cliente" || remitente.length < 3) {
            val matchDe = Regex("""(?:de|por)\s+([A-Za-zÀ-ÿ\s]{3,35})""", RegexOption.IGNORE_CASE).find(texto)
            if (matchDe != null) {
                val candidata = matchDe.groups[1]?.value?.trim() ?: ""
                if (candidata.isNotEmpty() && !candidata.contains("Yape", ignoreCase = true) && !candidata.contains("Plin", ignoreCase = true)) {
                    remitente = candidata
                }
            }
        }

        // =========================================================================
        // ESCUDO 5: ANTI-DUPLICADOS DE 45 SEGUNDOS (MEMORIA + PREFS)
        // Evita que un mismo Yape se registre o cante 2 veces cuando Android lanza
        // doble evento onNotificationPosted o cuando se prueba Dueño + Cajero en 1 celular.
        // =========================================================================
        val nowMs = System.currentTimeMillis()
        val huellaActual = "${remitente.lowercase(Locale.ROOT)}|${String.format(Locale.US, "%.2f", montoDouble)}|$metodo"
        synchronized(lockDedup) {
            val ultimaPref = prefs.getString("ultima_huella_cobro", "") ?: ""
            val ultimoTsPref = prefs.getLong("ultimo_ts_cobro", 0L)
            if ((huellaActual == ultimaHuellaMemoria && (nowMs - ultimoTsMemoria) < 45_000L) ||
                (huellaActual == ultimaPref && (nowMs - ultimoTsPref) < 45_000L)
            ) {
                return // Duplicado detectado dentro de los 45 segundos: se ignora por completo
            }
            ultimaHuellaMemoria = huellaActual
            ultimoTsMemoria = nowMs
            prefs.edit()
                .putString("ultima_huella_cobro", huellaActual)
                .putLong("ultimo_ts_cobro", nowMs)
                .putLong("ultimo_cobro_local_ms", nowMs)
                .putString("ultimo_monto_local", monto)
                .apply()
        }

        // Despertar procesador durante 6.5 segundos aunque la pantalla esté apagada o bloqueada
        adquirirWakeLockTemporal(6500L)

        // =========================================================================
        // VERIFICADOR DE MONTO ESPERADO EN CAJA (ALERTA DE PAGO INCOMPLETO)
        // =========================================================================
        val montoEsperadoStr = prefs.getString("monto_esperado_pos", "") ?: ""
        val montoEsperado = montoEsperadoStr.toDoubleOrNull() ?: 0.0
        val cajeroOrdenEsperada = prefs.getString("cajero_orden_pos", "") ?: ""

        // Anuncio en voz alta con timbre previo y verificación de monto completo
        if (prefs.getBoolean("voz_activa", true)) {
            reproducirTimbreYVoz(metodo, monto, montoDouble, montoEsperado)
        }

        // Si se cubrió el monto esperado en el POS, limpiamos la orden pendiente
        if (montoEsperado > 0.0 && montoDouble >= (montoEsperado - 0.01)) {
            prefs.edit().remove("monto_esperado_pos").remove("cajero_orden_pos").apply()
        }

        val cajeroActual = when {
            cajeroOrdenEsperada.isNotEmpty() -> cajeroOrdenEsperada
            prefs.getBoolean("cajero_logueado", false) -> prefs.getString("cajero_activo", "Caja") ?: "Caja"
            else -> prefs.getString("cajero_activo", "Dueño") ?: "Dueño"
        }

        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(nowMs))

        // 1. RESPALDO OFFLINE INMEDIATO EN SQLITE LOCAL (Nunca se pierde aunque no haya señal)
        val dbHelper = OfflineDatabaseHelper(this)
        val idFilaSQLite = dbHelper.insertarCobroLocal(
            codigoTienda = codigoTienda,
            nombre = remitente,
            monto = monto,
            metodo = metodo,
            cajero = cajeroActual,
            fecha = fechaHoy,
            hora = horaActual,
            timestamp = nowMs,
            sincronizado = false
        )

        guardarCobroLocal(remitente, monto, metodo)

        // 2. Intentar enviar a la nube y sincronizar cola offline pendiente si hay conexión
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("codigoTienda", codigoTienda)
                    put("nombre", "$remitente ($metodo)")
                    put("monto", monto)
                    put("cajero", cajeroActual)
                    put("fecha", fechaHoy)
                    put("hora", horaActual)
                    put("timestamp", nowMs)
                }
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(SERVIDOR_URL).post(body).build()
                val response = client.newCall(request).execute()
                val exito = response.isSuccessful
                response.close()

                if (exito && idFilaSQLite != -1L) {
                    dbHelper.marcarComoSincronizado(idFilaSQLite)
                    sincronizarColaPendienteSQLite(dbHelper)
                }
            } catch (e: Exception) {
                // Sin conexión a internet: El cobro ya quedó resguardado en SQLite local (sincronizado = 0)
                // Se enviará automáticamente cuando retorne la señal.
            }
        }
    }

    private fun sincronizarColaPendienteSQLite(dbHelper: OfflineDatabaseHelper) {
        val pendientes = dbHelper.obtenerCobrosPendientes()
        for (item in pendientes) {
            try {
                val json = JSONObject().apply {
                    put("codigoTienda", item.codigoTienda)
                    put("nombre", "${item.nombre} (${item.metodo})")
                    put("monto", item.monto)
                    put("cajero", item.cajero)
                    put("fecha", item.fecha)
                    put("hora", item.hora)
                    put("timestamp", item.timestamp)
                }
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    dbHelper.marcarComoSincronizado(item.idLocal)
                }
                res.close()
            } catch (e: Exception) {
                break
            }
        }
    }

    private fun reproducirTimbreYVoz(
        metodo: String,
        monto: String,
        montoRecibido: Double = 0.0,
        montoEsperado: Double = 0.0
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                delay(220)
                toneG.release()
            } catch (e: Exception) {}

            if (vozLista && motorVoz != null) {
                val fraseMonto = if (monto == "1" || monto == "1.00") "un sol" else "$monto soles"
                val discurso = if (montoEsperado > 0.0 && montoRecibido > 0.0 && montoRecibido < (montoEsperado - 0.01)) {
                    val faltante = String.format(Locale.US, "%.2f", montoEsperado - montoRecibido).removeSuffix(".00")
                    val esperadoTxt = String.format(Locale.US, "%.2f", montoEsperado).removeSuffix(".00")
                    "¡Atención caja! Se recibió un $metodo de $fraseMonto, pero el cobro en pantalla era de $esperadoTxt soles. ¡Faltan $faltante soles!"
                } else {
                    val animo = frasesMotivadoras.random()
                    "¡Te llegó un $metodo de $fraseMonto! $animo"
                }
                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                motorVoz?.speak(discurso, TextToSpeech.QUEUE_FLUSH, params, "ID_TTS")
            }
        }
    }

    private fun guardarCobroLocal(nombre: String, monto: String, metodo: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val historialStr = prefs.getString("historial_cobros", "[]") ?: "[]"
        val array = JSONArray(historialStr)
        val nowMs = System.currentTimeMillis()
        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(nowMs))

        val nuevoCobro = JSONObject().apply {
            put("hora", horaActual)
            put("fecha", fechaHoy)
            put("timestamp", nowMs)
            put("nombre", nombre)
            put("monto", monto)
            put("metodo", metodo)
        }

        val nuevoArray = JSONArray().apply {
            put(nuevoCobro)
            for (i in 0 until minOf(array.length(), 50)) {
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
