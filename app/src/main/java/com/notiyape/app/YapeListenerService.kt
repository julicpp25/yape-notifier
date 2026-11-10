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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    private var intentoMotorGenerico = false
    @Volatile
    private var frasePendienteEnCola: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient()
    private var jobLatidoCajeroBg: Job? = null
    private var ultimoIdCajeroBg = -1
    private var ultimoAvisoBloqueoMs = 0L

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

    // =========================================================================
    // 120 FRASES CORTAS Y GRACIOSAS "MODO BARRIO" DIVIDIDAS SEGÚN EL MONTO
    // =========================================================================

    // Rango 1: De S/ 0.10 a S/ 5.00 (30 frases - "Modo Sencillo / Misión Pan")
    private val frasesBarrioSencillo = listOf(
        "¡Todo suma, causa!",
        "¡Salió para el pancito, habla bien!",
        "¡Poquito a poquito se llena el chanchito!",
        "¡Ya salió para el pasaje en combi!",
        "¡Un sencillo más pa' la alcancía!",
        "¡Así sea un solcito, suena bonito!",
        "¡De sol en sol se arma el bolsón!",
        "¡Chaufa es chaufa, batería!",
        "¡Grano a grano llena la gallina el buche!",
        "¡Salió para el marciano de fruta!",
        "¡Algo es algo, peor es fiado!",
        "¡Sencillito pero seguro, casero!",
        "¡Ya tenemos para la emoliente con quinua!",
        "¡Aunque sea chiquito, cayó al toque!",
        "¡Paga chiquito pero paga al contado!",
        "¡Para el chicle aunque sea, sobrino!",
        "¡Suma, suma, que la caja no perdona!",
        "¡Cualquier sol es bendición, causa!",
        "¡Ya salió para el huevito de codorniz!",
        "¡Misión pan con torreja completada!",
        "¡Chiquito pero cumplidor ese cliente!",
        "¡Al menos no pidió rebaja, habla bien!",
        "¡Un sol más cerca de ser millonarios!",
        "¡Gota a gota se llena la bota!",
        "¡Salió pa' la canchita salada!",
        "¡Plata es plata, así sea sencillo!",
        "¡Cero fiado, puro yapeo al toque!",
        "¡Buena voz, caserito cumplidor!",
        "¡Directo al chanchito sin escala!",
        "¡Un pasito más pa' la quincena!"
    )

    // Rango 2: De S/ 5.01 a S/ 30.00 (30 frases - "Modo Caserito Fino / Menú")
    private val frasesBarrioMediano = listOf(
        "¡Habla bien, batería! ¡Salió para el menú!",
        "¡Esa gente fina, pagando sin pedir fiado!",
        "¡Qué elegancia la de Francia, casero!",
        "¡Hoy no se fía, mañana tampoco!",
        "¡Ya salió para el chaufa con Inca Kola!",
        "¡Así me gusta, pagando al toque!",
        "¡Cliente que yapea rápido vale por dos!",
        "¡Asu mare, estamos activos hoy!",
        "¡Bien jugado, caserito, vuelva pronto!",
        "¡Salió para el ceviche carretillero!",
        "¡Pura gente positiva en este negocio!",
        "¡Sonó la caja, sonríe el dueño!",
        "¡Eso sí es un cliente de categoría!",
        "¡Cayó redondito, sin llorar!",
        "¡Habla, causa, sigue la buena racha!",
        "¡Con este pago ya aseguramos el almuerzo!",
        "¡Fino, fino, como ceviche de corvina!",
        "¡Sin regatear, así da gusto atender!",
        "¡Ese Yape entró más rápido que combi vacía!",
        "¡Puro efectivo digital, cero billete falso!",
        "¡Atiende bonito que el casero es puntual!",
        "¡Ya salió para el lomito saltado!",
        "¡Suelta la música que estamos vendiendo!",
        "¡Calientito como pan recién salido!",
        "¡Qué buena mano tiene ese cliente!",
        "¡Paga y se va feliz, qué tal servicio!",
        "¡Se mueve la caja, se mueve el barrio!",
        "¡Bendito sea el Yape que no pide vuelto!",
        "¡Otro cliente que no pide fiado, milagro!",
        "¡Sigue sumando la máquina, sobrino!"
    )

    // Rango 3: De S/ 30.01 a S/ 99.99 (30 frases - "Modo Pollito a la Brasa / VIP")
    private val frasesBarrioPollo = listOf(
        "¡Uyuyuy! ¡Salió para el pollo a la brasa!",
        "¡Qué abuso! ¡Ese cliente gasta como los dioses!",
        "¡Ponle su yapita a ese casero VIP!",
        "¡Billetón en la casa, habla bien!",
        "¡Asu qué nivel, pura gente pudiente!",
        "¡Fuego en la caja, hoy rompemos récord!",
        "¡Con todas sus cremas, sobrino!",
        "¡Ese Yape vino con fuerza de león!",
        "¡Atención de primera para el jefe!",
        "¡Hoy se come rico en la casa!",
        "¡Qué tal jugada, directo a la ganancia!",
        "¡Ese cliente vino con la billetera recargada!",
        "¡Salió para la parrilla del fin de semana!",
        "¡Cero miseria, puro gasto fino!",
        "¡Aplaude caja que entró un buen billete!",
        "¡Ese casero merece bolsa doble gratis!",
        "¡Estamos en las grandes ligas, batería!",
        "¡Suena fuerte ese altavoz que hay billete!",
        "¡Como jugando ya subió la cuenta!",
        "¡Póngale su refresco de cortesía al toque!",
        "¡Así facturan los campeones del barrio!",
        "¡Qué lindo suena cuando el monto es grande!",
        "¡Ese cliente sí mueve la economía peruana!",
        "¡Sin miedo al éxito, qué buena compra!",
        "¡Agarra esa flor, venta de peso!",
        "¡Entró fresquito como helado en verano!",
        "¡Con clientes así trabajamos hasta domingo!",
        "¡Se prendió la racha ganadora, causa!",
        "¡Peso pesado en el mostrador!",
        "¡Ya salió para pagar la luz con alegría!"
    )

    // Rango 4: De S/ 100.00 a más (30 frases - "Modo Patrón / Tío Rico")
    private val frasesBarrioPatron = listOf(
        "¡Aguanta el coche! ¡Llegó el patrón del barrio!",
        "¡Alfombra roja para este cliente millonario!",
        "¡Asu mare, qué tal billetón! ¡Cierra la tienda!",
        "¡Alerta de magnate en el mostrador!",
        "¡Habla, Tío Rico! ¡Invítale su gaseosa heladita!",
        "¡Ese Yape pesó como lingote de oro!",
        "¡Con esta venta ya hicimos el día, causa!",
        "¡Silencio todos que acaba de pagar el jefe!",
        "¡Bendecido y afortunado, qué tal ventaza!",
        "¡Traigan seguridad que cayó un billetón!",
        "¡Ese cliente juega en la Champions League!",
        "¡Me desmayo! ¡Qué hermoso suena ese monto!",
        "¡Fácil es el dueño del banco, atiéndelo rey!",
        "¡Salió para el tono con orquesta completa!",
        "¡Puro nivel gerencial en esta tienda!",
        "¡Rompió el chanchito y nos dejó la gloria!",
        "¡Un aplauso cerrado para este clientazo!",
        "¡Hoy cenamos langosta en el barrio!",
        "¡Qué tal golazo de media cancha, batería!",
        "¡Pónganle aire acondicionado a ese casero!",
        "¡Nivel Dubai en pleno mostrador!",
        "¡Casi revienta el parlante con tanto billete!",
        "¡Eso no es un Yape, es un aguinaldo!",
        "¡Que vuelva todos los días ese patrón!",
        "¡De frente a la bóveda fuerte, sobrino!",
        "¡Con razón brillaba la puerta cuando entró!",
        "¡Puro Santa Rosa digital en la cuenta!",
        "¡Hicimos la América con este cobro!",
        "¡Anoten su nombre en el cuadro de honor!",
        "¡Qué espectáculo de venta, somos imparables!"
    )

    private fun obtenerFraseSegunModoYMonto(montoDouble: Double): String {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val modo = prefs.getString("modo_frases_voz", "MOTIVADOR") ?: "MOTIVADOR"
        if (modo != "BARRIO") {
            return frasesMotivadoras.random()
        }
        return when {
            montoDouble <= 5.0 -> frasesBarrioSencillo.random()
            montoDouble <= 30.0 -> frasesBarrioMediano.random()
            montoDouble < 100.0 -> frasesBarrioPollo.random()
            else -> frasesBarrioPatron.random()
        }
    }

    override fun onCreate() {
        super.onCreate()
        iniciarEscudoForeground24x7()
        inicializarMotorVoz()
        iniciarLatidoCajeroSegundoPlano()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarEscudoForeground24x7()
        if (!vozLista || motorVoz == null) {
            inicializarMotorVoz()
        }
        iniciarLatidoCajeroSegundoPlano()
        revisarNotificacionesActivasRecientes()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        adquirirWakeLockTemporal(6000L)
        iniciarEscudoForeground24x7()
        if (!vozLista || motorVoz == null) {
            inicializarMotorVoz()
        }
        iniciarLatidoCajeroSegundoPlano()
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
        iniciarLatidoCajeroSegundoPlano()
        revisarNotificacionesActivasRecientes()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, YapeListenerService::class.java))
            } catch (e: Exception) {}
        }
    }

    // =========================================================================
    // PROTECCIÓN #5: SINCRONIZADOR 24/7 EN SEGUNDO PLANO PARA EL CAJERO
    // Permite que el celular del Cajero cante los cobros aunque cierre la app o apague pantalla
    // =========================================================================
    private fun iniciarLatidoCajeroSegundoPlano() {
        if (jobLatidoCajeroBg?.isActive == true) return
        jobLatidoCajeroBg = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
                    val esModoCajero = prefs.getBoolean("cajero_logueado", false)
                    val esDueno = prefs.getBoolean("es_celular_dueño_receptor", false)
                    val cod = prefs.getString("codigo_tienda", "") ?: ""

                    if (esModoCajero && !esDueno && cod.isNotEmpty()) {
                        val url = "$SERVIDOR_URL?accion=consultar_cobros&codigoTienda=$cod"
                        val res = client.newCall(Request.Builder().url(url).get().build()).execute()
                        val jsonStr = res.body?.string() ?: "[]"
                        res.close()

                        if (jsonStr.trim().startsWith("[")) {
                            val arr = JSONArray(jsonStr)
                            if (arr.length() > 0) {
                                val ult = arr.getJSONObject(0)
                                val idCobro = ult.optInt("id", -1)
                                val monto = ult.optString("monto", "0.00")
                                val nombre = ult.optString("nombre", "Cliente")
                                val hora = ult.optString("hora", "")

                                if (ultimoIdCajeroBg == -1) {
                                    ultimoIdCajeroBg = idCobro
                                } else if (idCobro > ultimoIdCajeroBg) {
                                    ultimoIdCajeroBg = idCobro
                                    val nowMs = System.currentTimeMillis()
                                    val ultimoCobroLocalMs = prefs.getLong("ultimo_cobro_local_ms", 0L)
                                    val montoDouble = monto.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    val huellaSinc = "${nombre.lowercase(Locale.ROOT).trim()}|${String.format(Locale.US, "%.2f", montoDouble)}|$hora"
                                    val ultimaHuellaSinc = prefs.getString("ultima_huella_sinc_vista", "") ?: ""
                                    val ultimoTsSinc = prefs.getLong("ultimo_ts_sinc_visto", 0L)

                                    val yaFueCantado = ((nowMs - ultimoCobroLocalMs) < 55_000L) ||
                                        (huellaSinc == ultimaHuellaSinc && (nowMs - ultimoTsSinc) < 90_000L)

                                    if (!yaFueCantado && prefs.getBoolean("voz_activa", true)) {
                                        prefs.edit()
                                            .putString("ultima_huella_sinc_vista", huellaSinc)
                                            .putLong("ultimo_ts_sinc_visto", nowMs)
                                            .putLong("ultimo_cobro_local_ms", nowMs)
                                            .putString("ultimo_cobro_resumen", "$nombre|$monto|Cobro|$hora")
                                            .apply()

                                        val metodoDet = if (nombre.contains("Plin", ignoreCase = true)) "Plin" else "Yape"
                                        val nombreLimpio = nombre.replace(Regex("""\s*\(.*?\)\s*"""), "").trim()
                                        val decirNombre = prefs.getBoolean("decir_nombre_voz", true)
                                        emitirNotificacionCobroInstantanea(metodoDet, monto, nombreLimpio)
                                        reproducirTimbreYVoz(metodoDet, monto, nombreLimpio, decirNombre, montoDouble)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {}
                delay(5000L)
            }
        }
    }

    private fun inicializarMotorVoz(usarMotorPorDefecto: Boolean = false) {
        mainHandler.post {
            try {
                motorVoz?.shutdown()
            } catch (e: Exception) {}
            vozLista = false
            try {
                motorVoz = if (!usarMotorPorDefecto) {
                    TextToSpeech(applicationContext, this, "com.google.android.tts")
                } else {
                    TextToSpeech(applicationContext, this)
                }
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
                    "Guardia 24/7 NotiAlDía",
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
                    .setContentTitle("🟢 NotiAlDía Activo 24/7 (Yape + Plin)")
                    .setContentText("Altavoz de cobros activo con app cerrada o pantalla apagada")
                    .setOngoing(true)
                    .setContentIntent(pi)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                    .setContentTitle("🟢 NotiAlDía Activo 24/7 (Yape + Plin)")
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
                    setSound(null, null)
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

    // =========================================================================
    // PROTECCIÓN #4: CASCADA DE IDIOMAS ESPAÑOL + RESPALDO MOTOR SAMSUNG/HONOR TTS
    // =========================================================================
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localesEspanol = listOf(
                Locale("es", "PE"),
                Locale("es", "419"),
                Locale("es", "US"),
                Locale("es", "MX"),
                Locale("es", "ES"),
                Locale("es")
            )
            for (loc in localesEspanol) {
                try {
                    val res = motorVoz?.setLanguage(loc)
                    if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                        break
                    }
                } catch (e: Exception) {}
            }

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

            val pendiente = frasePendienteEnCola
            if (!pendiente.isNullOrEmpty()) {
                frasePendienteEnCola = null
                hablarAhoraConFocoDeAudio(pendiente)
            }
        } else if (!intentoMotorGenerico) {
            // Si el celular (ej. Samsung / Honor) no tiene activo Google TTS, usamos el motor nativo del teléfono
            intentoMotorGenerico = true
            inicializarMotorVoz(usarMotorPorDefecto = true)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }

        val pkg = sbn.packageName ?: return
        if (pkg == packageName ||
            pkg.contains("whatsapp", ignoreCase = true) ||
            pkg.contains("telegram", ignoreCase = true) ||
            pkg.contains("facebook", ignoreCase = true) ||
            pkg.contains("instagram", ignoreCase = true) ||
            pkg.contains("tiktok", ignoreCase = true) ||
            pkg.contains("messaging", ignoreCase = true) ||
            pkg.contains("mms", ignoreCase = true) ||
            pkg.contains("gmail", ignoreCase = true) ||
            pkg.contains("chrome", ignoreCase = true)
        ) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim() ?: ""
        val text = extras.getCharSequence("android.text")?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString()?.trim() ?: ""

        val cuerpoPrincipal = if (bigText.isNotEmpty() && bigText.length >= text.length) bigText else text
        val baseTexto = if (title.isNotEmpty() && !cuerpoPrincipal.contains(title, ignoreCase = true)) {
            "$title $cuerpoPrincipal"
        } else {
            cuerpoPrincipal
        }.trim()
        val textoCompleto = if (subText.isNotEmpty() && !baseTexto.contains(subText, ignoreCase = true)) {
            "$subText $baseTexto"
        } else {
            baseTexto
        }.trim()

        if (textoCompleto.isEmpty()) return

        val esYape = (pkg == "com.bcp.innovacxion.yapeapp" || pkg.contains("yape", ignoreCase = true))
        val esInterbank = (
            pkg == "pe.com.interbank.mobilebanking" ||
            pkg.contains("interbank", ignoreCase = true) ||
            textoCompleto.contains("Interbank", ignoreCase = true)
        )
        val esBBVA = (
            pkg == "com.bbva.bbvacontigo" ||
            pkg == "com.bbva.nxt_peru" ||
            pkg.contains("bbva", ignoreCase = true) ||
            textoCompleto.contains("BBVA", ignoreCase = true)
        )
        val esScotiabank = (
            pkg == "com.scotiabank.peru.pe" ||
            pkg == "pe.com.scotiabank.blpm.android.client" ||
            pkg.contains("scotiabank", ignoreCase = true) ||
            textoCompleto.contains("Scotiabank", ignoreCase = true)
        )
        val esBanbif = (
            pkg == "com.banbif.bifmovil" ||
            pkg.contains("banbif", ignoreCase = true) ||
            textoCompleto.contains("BanBif", ignoreCase = true)
        )
        val esOtraEntidadPlin = (
            pkg == "pe.com.cajaarequipa.movil" ||
            pkg == "pe.com.izipay.izipayya" ||
            pkg == "pe.com.cajasullana.movil" ||
            pkg.contains("plin", ignoreCase = true) ||
            textoCompleto.contains(Regex("(plinead|pinead|pline[oó]|recibiste un plin|has recibido un plin|transferencia plin|abono plin|te hicieron un plin)", RegexOption.IGNORE_CASE))
        )

        if (esYape || esInterbank || esBBVA || esScotiabank || esBanbif || esOtraEntidadPlin) {
            val metodo = when {
                esYape -> "Yape"
                esInterbank -> "Plin Interbank"
                esBBVA -> "Plin BBVA"
                esScotiabank -> "Plin Scotiabank"
                esBanbif -> "Plin BanBif"
                else -> "Plin"
            }
            val sbnKeyUnica = "${pkg}|${sbn.id}|${sbn.postTime}"
            procesarPago(textoCompleto, metodo, sbnKeyUnica)
        }
    }

    companion object {
        @Volatile
        private var ultimaSbnKeyMemoria: String = ""
        @Volatile
        private var ultimaHuellaMemoria: String = ""
        @Volatile
        private var ultimoTsMemoria: Long = 0L
        private val lockDedup = Any()
    }

    // =========================================================================
    // PROTECCIÓN #1: EXTRACTOR SEGURO DE MONTOS GRANDES (S/ 1,250.00 Y PEN)
    // =========================================================================
    private fun extraerMontoSeguroEnSoles(texto: String): Double? {
        val regex = Regex("""(?:S/\.?|PEN)\s*([\d]{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|[\d]+(?:[.,]\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(texto) ?: return null
        val crudo = match.groups[1]?.value?.trim() ?: return null

        val normalizado = when {
            // Caso miles con coma y decimal con punto: "1,250.50" -> "1250.50"
            crudo.contains(",") && crudo.contains(".") -> {
                if (crudo.lastIndexOf(".") > crudo.lastIndexOf(",")) {
                    crudo.replace(",", "")
                } else {
                    crudo.replace(".", "").replace(",", ".")
                }
            }
            // Caso coma de miles exacta sin decimales: "1,200" -> "1200"
            crudo.contains(",") && crudo.substringAfterLast(",").length == 3 -> {
                crudo.replace(",", "")
            }
            // Caso coma decimal: "15,50" -> "15.50"
            crudo.contains(",") -> {
                crudo.replace(",", ".")
            }
            else -> crudo
        }
        return normalizado.toDoubleOrNull()
    }

    private fun procesarPago(texto: String, metodo: String, sbnKeyUnica: String = "") {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val codigoTienda = prefs.getString("codigo_tienda", "") ?: return

        // =========================================================================
        // ESCUDO 0: BLOQUEO DE YAPE/PLIN PERSONAL DEL CAJERO
        // =========================================================================
        val esCelularDuenoReceptor = prefs.getBoolean("es_celular_dueño_receptor", false)
        val esModoCajero = prefs.getBoolean("cajero_logueado", false)
        if (!esCelularDuenoReceptor || esModoCajero) {
            return
        }

        // =========================================================================
        // PROTECCIÓN #3: DETECTAR SI EL CELULAR OCULTA NOTIFICACIONES EN BLOQUEO
        // =========================================================================
        val esNotificacionOcultaPorBloqueo = texto.contains(
            Regex("(contenido oculto|notificación oculta|ocultó el contenido|tienes \\d+ notificaci|nuevas notificaciones)", RegexOption.IGNORE_CASE)
        ) && !texto.contains(Regex("""(?:S/\.?|PEN)\s*\d""", RegexOption.IGNORE_CASE))

        if (esNotificacionOcultaPorBloqueo) {
            val ahora = System.currentTimeMillis()
            if (ahora - ultimoAvisoBloqueoMs > 180_000L && prefs.getBoolean("voz_activa", true)) {
                ultimoAvisoBloqueoMs = ahora
                hablarAhoraConFocoDeAudio("¡Atención! Llegó una notificación de $metodo, pero tu celular oculta los datos en pantalla bloqueada. Activa mostrar contenido en pantalla de bloqueo.")
            }
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
        // ESCUDO 2: VERBO DE PAGO OBLIGATORIO (YAPE Y TODOS LOS BANCOS PLIN)
        // =========================================================================
        val tieneVerboPago = texto.contains(
            Regex("(te ha plinead|te ha pinead|ha plinead|ha pinead|plinead[oa]|pinead[oa]|te pline|pline[oó]|plineo|te hicieron un plin|has recibido un plin|transferencia plin|operaci[oó]n plin|te ha yapead|ha yapead|yapead[oa]|te yape|yape[oó]|yapeo|te ha enviad|ha enviad|te envi|te ha mandad|te mand|te ha transferid|transfiri|recibiste un pago|pago de|recibiste un yape|recibiste un plin|envi[oó] un plin|envi[oó] por plin|abono plin|confirmaci[oó]n de pago|recibiste)", RegexOption.IGNORE_CASE)
        )
        if (!tieneVerboPago) {
            return
        }

        // =========================================================================
        // 3. EXTRAER MONTO EN SOLES SEGURO (Soporta S/ 1,250.00, S/.1.00, PEN 20.00)
        // =========================================================================
        val montoDouble = extraerMontoSeguroEnSoles(texto) ?: return
        if (montoDouble <= 0.0) return
        val montoNormalizado = String.format(Locale.US, "%.2f", montoDouble)
        val montoVozTexto = if (montoDouble % 1.0 == 0.0) montoDouble.toInt().toString() else montoNormalizado

        // =========================================================================
        // 4. EXTRAER REMITENTE LIMPIO
        // =========================================================================
        var remitente = "Cliente"
        val regexVerbos = Regex("(te ha plinead[oa]|te ha pinead[oa]|ha plinead[oa]|ha pinead[oa]|te pline[oó]|plinead[oa]|pinead[oa]|pline[oó]|te hicieron un plin|has recibido un plin de|transferencia plin de|te ha yapead[oa]|ha yapead[oa]|te yape[oó]|yapead[oa]|yape[oó]|te ha enviad[oa]|ha enviad[oa]|te envi[oó]|te ha transferid[oa]|transfiri[oó]|te ha mandad[oa]|te mand[oó]|recibiste un pago de|recibiste un plin de|recibiste un yape de|pago de)", RegexOption.IGNORE_CASE)

        if (texto.contains(regexVerbos)) {
            val antes = texto.split(regexVerbos)[0]
            val posible = antes.replace(
                Regex("(¡|!|Confirmación de Pago\\.?|Confirmacion de Pago\\.?|Te llegó un Yape|Recibiste un Plin|Yape\\.?|Plin\\.?|Interbank\\.?|BBVA\\.?|Scotiabank\\.?|BanBif\\.?|Pago\\.?|Notificación\\.?|:|-)", RegexOption.IGNORE_CASE),
                ""
            ).trim()
            if (posible.isNotEmpty() && posible.length >= 3) {
                remitente = posible
            }
        }

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
        // PROTECCIÓN #2: ESCUDO ANTI-REBOTE INTELIGENTE
        // Bloquea el doble disparo de la MISMA notificación (sbnKeyUnica o <5 seg),
        // pero permite que 2 clientes distintos paguen el mismo monto seguido.
        // =========================================================================
        val nowMs = System.currentTimeMillis()
        val huellaActual = "${remitente.lowercase(Locale.ROOT).trim()}|$montoNormalizado|$metodo"
        val ventanaDedupMs = if (remitente.equals("Cliente", ignoreCase = true)) 4_000L else 6_500L

        synchronized(lockDedup) {
            if (sbnKeyUnica.isNotEmpty() && sbnKeyUnica == ultimaSbnKeyMemoria) {
                return // Es exactamente la misma notificación de Android re-evaluada
            }
            val ultimaPref = prefs.getString("ultima_huella_cobro", "") ?: ""
            val ultimoTsPref = prefs.getLong("ultimo_ts_cobro", 0L)
            if ((huellaActual == ultimaHuellaMemoria && (nowMs - ultimoTsMemoria) < ventanaDedupMs) ||
                (huellaActual == ultimaPref && (nowMs - ultimoTsPref) < ventanaDedupMs)
            ) {
                return
            }
            if (sbnKeyUnica.isNotEmpty()) {
                ultimaSbnKeyMemoria = sbnKeyUnica
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

        adquirirWakeLockTemporal(12000L)
        emitirNotificacionCobroInstantanea(metodo, montoNormalizado, remitente)

        val decirNombre = prefs.getBoolean("decir_nombre_voz", true)
        if (prefs.getBoolean("voz_activa", true)) {
            reproducirTimbreYVoz(metodo, montoVozTexto, remitente, decirNombre, montoDouble)
        }

        val cajeroActual = "Caja Principal"
        val horaActual = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(nowMs))
        val horaCorta = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(nowMs))
        val idUnicoOperacion = "${codigoTienda}_${remitente.replace(" ", "")}_${montoNormalizado}_$horaActual"

        prefs.edit()
            .putString("ultimo_cobro_resumen", "$remitente|$montoNormalizado|$metodo|$horaCorta")
            .apply()

        val dbHelper = OfflineDatabaseHelper(this)
        val idFilaSQLite = dbHelper.insertarCobroLocal(
            codigoTienda = codigoTienda,
            nombre = remitente,
            monto = montoNormalizado,
            metodo = metodo,
            cajero = cajeroActual,
            fecha = fechaHoy,
            hora = horaCorta,
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
                    put("hora", horaCorta)
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
        decirNombre: Boolean = true,
        montoDouble: Double = 0.0
    ) {
        val metodoVoz = if (metodo.startsWith("Plin", ignoreCase = true)) "Plin" else metodo
        val fraseMonto = if (monto == "1" || monto == "1.00") "un sol" else "$monto soles"
        val fraseCliente = if (decirNombre && remitente.isNotBlank() && !remitente.equals("Cliente", ignoreCase = true)) {
            " de $remitente"
        } else {
            ""
        }
        val mNum = if (montoDouble > 0.0) montoDouble else (monto.replace(",", ".").toDoubleOrNull() ?: 1.0)
        val animo = obtenerFraseSegunModoYMonto(mNum)
        val discurso = "¡Te llegó un $metodoVoz de $fraseMonto$fraseCliente! $animo"

        hablarAhoraConFocoDeAudio(discurso)
    }

    private fun hablarAhoraConFocoDeAudio(discurso: String) {
        adquirirWakeLockTemporal(12000L)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
                // Doble timbre despertador (350ms) para despertar parlantes Bluetooth en reposo antes de hablar
                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 240)
                delay(320)
                toneG.release()
            } catch (e: Exception) {}

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            if (vozLista && motorVoz != null) {
                val resultado = motorVoz?.speak(discurso, TextToSpeech.QUEUE_FLUSH, params, "ID_TTS_${System.currentTimeMillis()}")
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
        jobLatidoCajeroBg?.cancel()
        motorVoz?.stop()
        motorVoz?.shutdown()
        super.onDestroy()
    }
}
