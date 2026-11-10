package com.notiyape.app

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService

/**
 * Auto-arranque al encender o reiniciar el celular:
 * Garantiza que el Escudo 24/7 de NotiAlDía se reconecte automáticamente apenas prenda el teléfono
 * cumpliendo con las políticas de segundo plano de Android 15 y Android 16 (API 36).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = context.getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
            val tiendaActiva = prefs.getBoolean("tienda_registrada", false) || prefs.getBoolean("cajero_logueado", false)
            if (!tiendaActiva) return

            // 1. Solicitar reconexión oficial del NotificationListenerService al sistema Android (Permitido en Android 15/16)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    NotificationListenerService.requestRebind(
                        ComponentName(context, YapeListenerService::class.java)
                    )
                } catch (e: Exception) {}
            }

            // 2. En versiones anteriores a Android 15 (API < 35) también levantamos startForegroundService directamente
            if (Build.VERSION.SDK_INT < 35) {
                try {
                    val srvIntent = Intent(context, YapeListenerService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(srvIntent)
                    } else {
                        context.startService(srvIntent)
                    }
                } catch (e: Exception) {}
            }
        }
    }
}
