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
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    @Volatile
    private var vozLista = false
    @Volatile
    private var frasePendienteEnCola: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
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
        inicializarMotorVoz()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarEscudoForeground24x7()
        if (!vozLista || motorVoz == null) {
            inicializarMotorVoz()
        }
        revisarNotificacionesActivasRecientes()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // ESCUDO ANTI-CIERRE PARA HONOR MAGIC 8 LITE, XIAOMI, SAMSUNG, OPPO Y VIVO:
        // Cuando el usuario desliza y cierra la app de "Recientes", mantenemos vivo el servicio y el motor de voz
        adquirirWakeLockTemporal(6000L)
        iniciarEscudoForeground24x7()
        if (!vozLista || motorVoz == null) {
            inicializarMotorVoz()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, YapeListenerService::class.java))
            } catch (e: Exception) {}
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        iniciarEscudoForeground24x7()
        if (!vozLista || motorVoz == null) {
            inicializarMotorVoz()
        }
        revisarNotificacionesActivasRecientes()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // ESCUDO AUTO-RECONEXIÓN: Si Honor MagicOS / MIUI / OneUI intenta desconectar el lector, se reconecta al instante
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, YapeListenerService::class.java))
            } catch (e: Exception) {}
        }
    }

    private fun inicializarMotorVoz() {
        mainHandler.post {
            try {
                motorVoz?.shutdown()
            } catch (e: Exception) {}
            vozLista = false
            try {
                motorVoz = TextToSpeech(applicationContext, this, "com.google.android.tts")
            } catch (e: Exception) {
                try {
                    motorVoz = TextToSpeech(applicationContext, this)
                } catch (ex: Exception) {}
            }
        }
    }

    private fun revisarNotificacionesActivasRecientes() {
        try {
            val activas = activeNotifications ?: return
            val ahora = System.currentTimeMillis()
            for (sbn in activas) {
                // Solo revisar notificaciones publicadas en los últimos 45 segundos
                if ((ahora - sbn.postTime) < 45_000L) {
                    onNotificationPosted(sbn)
                }
            }
        } catch (e: Exception) {}
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
                    description = "Mantiene el lector de voz de Yape y Plin activo con app cerrada o pantalla apagada"
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
                    .setContentTitle("🟢 YapeAlDía Activo 24/7 (Yape + Plin)")
                    .setContentText("Altavoz de cobros activo con app cerrada o pantalla apagada")
                    .setOngoing(true)
                    .setContentIntent(pi)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                    .setContentTitle("🟢 YapeAlDía Activo 24/7 (Yape + Plin)")
                    .setContentText("Altavoz de cobros activo con app cerrada o pantalla apagada")
                    .setOngoing(true)
                    .setContentIntent(pi)
                    .build()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        777,
                        notif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (e: Exception) {
                    startForeground(777, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                }
            } else {
                startForeground(777, notif)
            }
        } catch (e: Exception) {}
    }

    private fun emitirNotificacionCobroInstantanea(metodo: String, monto: String, remitente: String) {
        // En Honor Magic 8 Lite, Xiaomi, OPPO, Vivo e Infinix, emitir una notificación IMPORTANCE_HIGH
        // rompe de inmediato el congelador de caché (Cached Process Freezer) cuando la app está cerrada.
        try {
            val channelId = "yapealdia_cobros_alerta_alta"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    channelId,
                    "Alertas Instantáneas de Cobros",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Despierta el altavoz de inmediato cuando llega un Yape o Plin con la app cerrada"
                    enableVibration(true)
                    setSound(null, null) // Sin sonido propio para no tapar la voz TTS
                }
                nm.createNotificationChannel(canal)
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pi = PendingIntent.getActivity(
                this,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, channelId)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this).setPriority(Notification.PRIORITY_MAX)
            }

            val notif = builder
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("💰 ¡$metodo recibido: S/ $monto!")
                .setContentText("De: $remitente • Registrado en YapeAlDía")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            nm.notify(888, notif)
        } catch (e: Exception) {}
    }

    private fun adquirirWakeLockTemporal(duracionMs: Long = 12000L) {
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

            // Si había un cobro esperando porque Honor/Xiaomi había desconectado el motor de voz al cerrar la app,
            // lo hablamos en este mismo instante sin esperar a que el usuario abra la app.
            val pendiente = frasePendienteEnCola
            if (!pendiente.isNullOrEmpty()) {
                frasePendienteEnCola = null
                hablarAhoraConFocoDeAudio(pendiente)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Ignorar notificaciones de resumen de grupo de Android (evita el segundo disparo automático de Yape/Plin)
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }

        val pkg = sbn.packageName ?: return
        // Ignorar nuestra propia app y apps de mensajería/redes sociales para evitar falsas alarmas
        if (pkg == packageName ||
            pkg.contains("whatsapp", ignoreCase = true) ||
            pkg.contains("telegram", ignoreCase = true) ||
            pkg.contains("facebook", ignoreCase = true) ||
            pkg.contains("instagram", ignoreCase = true) ||
            pkg.contains("messaging", ignoreCase = true) ||
            pkg.contains("mms", ignoreCase = true) ||
            pkg.contains("gmail", ignoreCase = true)
        ) {
            return
        }

        val esYape = (pkg == "com.bcp.innovacxion.yapeapp")
        val esInterbank = (pkg == "pe.com.interbank.mobilebanking")
        val esBBVA = (pkg == "com.bbva.bbvacontigo" || pkg == "com.bbva.nxt_peru")
        val esScotiabank = (pkg == "com.scotiabank.peru.pe" || pkg == "pe.com.scotiabank.blpm.android.client")
        val esBanbif = (pkg == "com.banbif.bifmovil")
        val esOtraEntidadPlin = (
            pkg == "pe.com.cajaarequipa.movil" ||
            pkg == "pe.com.izipay.izipayya" ||
            pkg == "pe.com.cajasullana.movil" ||
            pkg.contains("plin", ignoreCase = true)
        )

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim() ?: ""
        val text = extras.getCharSequence("android.text")?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString()?.trim() ?: ""

        // Unificamos título y texto para que tanto Yape como Plin (Interbank, BBVA, Scotiabank, etc.) se reconozcan siempre
        val cuerpoPrincipal = if (bigText.isNotEmpty() && bigText.length >= text.length) bigText else text
        val textoCompleto = if (title.isNotEmpty() && !cuerpoPrincipal.contains(title, ignoreCase = true)) {
            "$title. $cuerpoPrincipal"
        } else {
            cuerpoPrincipal
        }.trim()

        if (textoCompleto.isEmpty()) return

        if (esYape || esInterbank || esBBVA || esScotiabank || esBanbif || esOtraEntidadPlin) {
            val metodo = when {
                esYape -> "Yape"
                esInterbank -> "Plin Interbank"
                esBBVA -> "Plin BBVA"
                esScotiabank -> "Plin Scotiabank"
                esBanbif -> "Plin BanBif"
                else -> "Plin"
            }
            procesarPago(textoCompleto, metodo)
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
        // ESCUDO 0: BLOQUEO DE YAPE/PLIN PERSONAL DEL CAJERO
        // Solo el celular del Dueño (Receptor Oficial) debe leer notificaciones de Yape/Plin.
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
            return
        }

        // =========================================================================
        // ESCUDO 2: VERBO DE PAGO OBLIGATORIO (YAPE Y PLIN EN SIMULTÁNEO)
        // =========================================================================
        val tieneVerboPago = texto.contains(
            Regex("(te envi|te mand|transfiri|recibiste un pago|pago de|te yape|yapeó|yapeo|te pline|plineó|plineo|recibiste un yape|recibiste un plin|envi[oó] un plin|envi[oó] por plin|abono plin|recibiste)", RegexOption.IGNORE_CASE)
        )
        if (!tieneVerboPago) {
            return
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
        // 4. EXTRAER REMITENTE LIMPIO (Soporta formatos Yape y Plin de todos los bancos)
        // =========================================================================
        var remitente = "Cliente"
        val regexVerbos = Regex("(te envi[oó]|transfiri[oó]|te mand[oó]|recibiste un pago de|pago de|te yape[oó]|yape[oó]|te pline[oó]|pline[oó])", RegexOption.IGNORE_CASE)

        if (texto.contains(regexVerbos)) {
            val antes = texto.split(regexVerbos)[0]
            val posible = antes.replace(
                Regex("(¡|!|Confirmación de Pago\\.?|Confirmacion de Pago\\.?|Te llegó un Yape|Recibiste un Plin|Yape|Plin|Interbank|BBVA|Scotiabank|BanBif|Pago|Notificación)", RegexOption.IGNORE_CASE),
                ""
            ).trim()
            if (posible.isNotEmpty() && posible.length >= 3) {
                remitente = posible
            }
        }

        // Si el nombre aparece después ("de Juan Pérez")
        if (remitente == "Cliente" || remitente.length < 3) {
            val matchDe = Regex("""(?:de|por)\s+([A-Za-zÀ-ÿ\s]{3,35})""", RegexOption.IGNORE_CASE).find(texto)
            if (matchDe != null) {
                val candidata = matchDe.groups[1]?.value?.trim() ?: ""
                if (candidata.isNotEmpty() &&
                    !candidata.contains("Yape", ignoreCase = true) &&
                    !candidata.contains("Plin", ignoreCase = true)
                ) {
                    remitente = candidata
                }
            }
        }

        // =========================================================================
        // ESCUDO 5: ANTI-DUPLICADOS DE 60 SEGUNDOS (MEMORIA + PREFS)
        // =========================================================================
        val nowMs = System.currentTimeMillis()
        val montoNormalizado = String.format(Locale.US, "%.2f", montoDouble)
        val huellaActual = "${remitente.lowercase(Locale.ROOT).trim()}|$montoNormalizado|$metodo"
        synchronized(lockDedup) {
            val ultimaPref = prefs.getString("ultima_huella_cobro", "") ?: ""
            val ultimoTsPref = prefs.getLong("ultimo_ts_cobro", 0L)
            if ((huellaActual == ultimaHuellaMemoria && (nowMs - ultimoTsMemoria) < 60_000L) ||
                (huellaActual == ultimaPref && (nowMs - ultimoTsPref) < 60_000L)
            ) {
                return
            }
            ultimaHuellaMemoria = huellaActual
            ultimoTsMemoria = nowMs
            prefs.edit()
                .putString("ultima_huella_cobro", huellaActual)
                .putLong("ultimo_ts_cobro", nowMs)
                .putLong("ultimo_cobro_local_ms", nowMs)
                .putString("ultimo_monto_local", montoNormalizado)
                .apply()
        }

        // 1. Despertar CPU por 12 segundos y emitir notificación de alta prioridad para romper
        // el congelamiento en segundo plano de Honor Magic 8 Lite, Xiaomi, Samsung, OPPO y Vivo.
        adquirirWakeLockTemporal(12000L)
        emitirNotificacionCobroInstantanea(metodo, montoNormalizado, remitente)

        // 2. Anuncio en voz alta inmediato con foco de audio y auto-reparación del motor TTS
        val decirNombre = prefs.getBoolean("decir_nombre_voz", true)
        if (prefs.getBoolean("voz_activa", true)) {
            reproducirTimbreYVoz(metodo, monto, remitente, decirNombre)
        }

        val cajeroActual = "Caja Principal"
        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(nowMs))
        val idUnicoOperacion = "${codigoTienda}_${remitente.replace(" ", "")}_${montoNormalizado}_$horaActual"

        prefs.edit()
            .putString("ultimo_cobro_resumen", "$remitente|$monto|$metodo|$horaActual")
            .apply()

        val dbHelper = OfflineDatabaseHelper(this)
        val idFilaSQLite = dbHelper.insertarCobroLocal(
            codigoTienda = codigoTienda,
            nombre = remitente,
            monto = montoNormalizado,
            metodo = metodo,
            cajero = cajeroActual,
            fecha = fechaHoy,
            hora = horaActual,
            timestamp = nowMs,
            sincronizado = true
        )

        guardarCobroLocal(remitente, montoNormalizado, metodo)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("codigoTienda", codigoTienda)
                    put("idUnico", idUnicoOperacion)
                    put("nombre", "$remitente ($metodo)")
                    put("monto", montoNormalizado)
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

                if (!exito && idFilaSQLite != -1L) {
                    dbHelper.marcarComoPendiente(idFilaSQLite)
                } else if (exito) {
                    sincronizarColaPendienteSQLite(dbHelper)
                }
            } catch (e: Exception) {
                if (idFilaSQLite != -1L) {
                    dbHelper.marcarComoPendiente(idFilaSQLite)
                }
            }
        }
    }

    private fun sincronizarColaPendienteSQLite(dbHelper: OfflineDatabaseHelper) {
        val pendientes = dbHelper.obtenerCobrosPendientes()
        for (item in pendientes) {
            try {
                dbHelper.marcarComoSincronizado(item.idLocal)
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
                if (!res.isSuccessful) {
                    dbHelper.marcarComoPendiente(item.idLocal)
                }
                res.close()
            } catch (e: Exception) {
                dbHelper.marcarComoPendiente(item.idLocal)
                break
            }
        }
    }

    private fun reproducirTimbreYVoz(
        metodo: String,
        monto: String,
        remitente: String = "Cliente",
        decirNombre: Boolean = true
    ) {
        val metodoVoz = if (metodo.startsWith("Plin", ignoreCase = true)) "Plin" else metodo
        val fraseMonto = if (monto == "1" || monto == "1.00") "un sol" else "$monto soles"
        val fraseCliente = if (decirNombre && remitente.isNotBlank() && !remitente.equals("Cliente", ignoreCase = true)) {
            " de $remitente"
        } else {
            ""
        }
        val animo = frasesMotivadoras.random()
        val discurso = "¡Te llegó un $metodoVoz de $fraseMonto$fraseCliente! $animo"

        hablarAhoraConFocoDeAudio(discurso)
    }

    private fun hablarAhoraConFocoDeAudio(discurso: String) {
        adquirirWakeLockTemporal(12000L)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                // Solicitar foco de audio prioritario para despertar el canal multimedia en Honor / Xiaomi / Samsung
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val reqFocus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .build()
                    audioManager.requestAudioFocus(reqFocus)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    )
                }

                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                delay(200)
                toneG.release()
            } catch (e: Exception) {}

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            if (vozLista && motorVoz != null) {
                val resultado = motorVoz?.speak(discurso, TextToSpeech.QUEUE_FLUSH, params, "ID_TTS_${System.currentTimeMillis()}")
                // Si Honor MagicOS o Xiaomi desconectó el motor TTS al cerrar la app, speak() devuelve ERROR (-1).
                // En ese caso guardamos la frase en cola y reconectamos el motor de voz en milisegundos.
                if (resultado == TextToSpeech.ERROR) {
                    frasePendienteEnCola = discurso
                    inicializarMotorVoz()
                }
            } else {
                frasePendienteEnCola = discurso
                inicializarMotorVoz()
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
