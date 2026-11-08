package com.notiyape.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MyFirebaseMessagingService : FirebaseMessagingService(), TextToSpeech.OnInitListener {

    private var motorVozPush: TextToSpeech? = null
    private var vozLista = false
    private var discursoPendiente: String? = null

    override fun onCreate() {
        super.onCreate()
        try {
            motorVozPush = TextToSpeech(this, this, "com.google.android.tts")
        } catch (e: Exception) {
            motorVozPush = TextToSpeech(this, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            motorVozPush?.language = Locale("es", "PE")
            motorVozPush?.setPitch(1.04f)
            motorVozPush?.setSpeechRate(1.02f)
            vozLista = true
            discursoPendiente?.let { texto ->
                hablarEnSegundoPlano(texto)
                discursoPendiente = null
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    companion object {
        @Volatile
        private var ultimaHuellaPush: String = ""
        @Volatile
        private var ultimoTsPush: Long = 0L
        private val lockPush = Any()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        // 1. Si este celular es el del Dueño (Receptor Principal), IGNORAR SIEMPRE el Push de Firebase,
        // porque el propio celular del Dueño ya detectó el Yape directamente por YapeListenerService.
        val esCelularDueno = prefs.getBoolean("es_celular_dueño_receptor", false)
        val esModoCajero = prefs.getBoolean("cajero_logueado", false)
        if (esCelularDueno && !esModoCajero) {
            return
        }

        val ultimoCobroLocalMs = prefs.getLong("ultimo_cobro_local_ms", 0L)
        if (System.currentTimeMillis() - ultimoCobroLocalMs < 45_000L) {
            return
        }

        val titulo = remoteMessage.notification?.title ?: remoteMessage.data["titulo"] ?: "🔔 YapeAlDía"
        val mensaje = remoteMessage.notification?.body ?: remoteMessage.data["mensaje"] ?: "¡Nuevo cobro recibido!"
        val monto = remoteMessage.data["monto"] ?: ""

        // 2. Escudo Anti-Duplicados Push de 60 segundos en el celular del Cajero
        val nowMs = System.currentTimeMillis()
        val huellaPushActual = "${mensaje.trim().lowercase(Locale.ROOT)}|${monto.trim()}"
        synchronized(lockPush) {
            val ultimaPref = prefs.getString("ultima_huella_push_cajero", "") ?: ""
            val ultimoTsPref = prefs.getLong("ultimo_ts_push_cajero", 0L)
            if ((huellaPushActual == ultimaHuellaPush && (nowMs - ultimoTsPush) < 60_000L) ||
                (huellaPushActual == ultimaPref && (nowMs - ultimoTsPref) < 60_000L)
            ) {
                return
            }
            ultimaHuellaPush = huellaPushActual
            ultimoTsPush = nowMs
            // Marcar inmediatamente ultimo_cobro_local_ms para que si el Cajero tiene la pantalla MainActivity
            // abierta, iniciarSincronizador() NO vuelva a cantar el mismo cobro una segunda vez.
            prefs.edit()
                .putString("ultima_huella_push_cajero", huellaPushActual)
                .putLong("ultimo_ts_push_cajero", nowMs)
                .putLong("ultimo_cobro_local_ms", nowMs)
                .apply()
        }

        // Despertar CPU del celular del Cajero aunque esté con pantalla apagada o minimizado
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YapeAlDia::PushCajeroWakeLock")
            wl.acquire(6000L)
        } catch (e: Exception) {}

        mostrarNotificacionPush(titulo, mensaje)

        if (prefs.getBoolean("voz_activa", true)) {
            val fraseHablar = if (monto.isNotEmpty()) {
                val mTexto = if (monto == "1" || monto == "1.00") "un sol" else "$monto soles"
                "¡Atención caja! Llegó un cobro de $mTexto. $mensaje"
            } else {
                "¡Atención caja! $mensaje"
            }
            if (vozLista) {
                hablarEnSegundoPlano(fraseHablar)
            } else {
                discursoPendiente = fraseHablar
            }
        }
    }

    private fun hablarEnSegundoPlano(texto: String) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                delay(220)
                tg.release()
            } catch (e: Exception) {}

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            motorVozPush?.speak(texto, TextToSpeech.QUEUE_FLUSH, params, "TTS_PUSH_CAJERO")
        }
    }

    private fun mostrarNotificacionPush(titulo: String, mensaje: String) {
        val channelId = "yapealdia_alertas_channel"
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Cobro YapeAlDía",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones instantáneas de pagos"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)

            val notification = Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } else {
            @Suppress("DEPRECATION")
            val notification = Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setSound(defaultSoundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    override fun onDestroy() {
        motorVozPush?.stop()
        motorVozPush?.shutdown()
        super.onDestroy()
    }
}
