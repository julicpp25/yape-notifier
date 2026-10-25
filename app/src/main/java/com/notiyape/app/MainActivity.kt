package com.notiyape.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    // El nombre de tu bot oficial de Telegram
    private val BOT_USERNAME = "alertasaudioyape_pe_bot"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 80, 50, 50)
        }

        val titulo = TextView(this).apply {
            text = "NotiCobros Perú 🇵🇪"
            textSize = 22f
            setPadding(0, 0, 0, 20)
        }

        val subtitulo = TextView(this).apply {
            text = "Ingresa el Código de tu Tienda:"
            textSize = 14f
            setPadding(0, 0, 0, 10)
        }

        val inputCodigo = EditText(this).apply {
            hint = "Ej: TEST-01"
            setText(prefs.getString("codigo_tienda", "TEST-01"))
        }

        val btnGuardar = Button(this).apply {
            text = "Guardar Código"
            setOnClickListener {
                val codigo = inputCodigo.text.toString().trim()
                if (codigo.isNotEmpty()) {
                    prefs.edit().putString("codigo_tienda", codigo).apply()
                    Toast.makeText(this@MainActivity, "Código guardado con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Ingresa un código válido", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnVincularTelegram = Button(this).apply {
            text = "1. Vincular mi Telegram (1 toque)"
            setOnClickListener {
                val codigo = prefs.getString("codigo_tienda", "") ?: ""
                if (codigo.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Primero guarda tu código de tienda", Toast.LENGTH_SHORT).show()
                } else {
                    // Abre Telegram directamente en el bot con el código listo
                    val urlTelegram = "https://t.me/$BOT_USERNAME?start=$codigo"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlTelegram))
                    startActivity(intent)
                }
            }
        }

        val btnPermiso = Button(this).apply {
            text = "2. Activar Notificaciones de Yape"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        layout.addView(titulo)
        layout.addView(subtitulo)
        layout.addView(inputCodigo)
        layout.addView(btnGuardar)
        layout.addView(btnVincularTelegram)
        layout.addView(btnPermiso)

        setContentView(layout)
    }
}
