package com.notiyape.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 80, 50, 50)
        }

        val titulo = TextView(this).apply {
            text = "Yape & Plin Notifier"
            textSize = 22f
            setPadding(0, 0, 0, 30)
        }

        val subtitulo = TextView(this).apply {
            text = "URL de tu Google Sheet (Apps Script):"
            textSize = 14f
        }

        val inputUrl = EditText(this).apply {
            hint = "Pega aquí la URL web..."
            setText(prefs.getString("sheet_url", ""))
        }

        val btnGuardar = Button(this).apply {
            text = "Guardar URL"
            setOnClickListener {
                val url = inputUrl.text.toString().trim()
                prefs.edit().putString("sheet_url", url).apply()
                Toast.makeText(this@MainActivity, "URL guardada con éxito", Toast.LENGTH_SHORT).show()
            }
        }

        val btnPermiso = Button(this).apply {
            text = "1. Activar Permiso de Lectura"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        layout.addView(titulo)
        layout.addView(subtitulo)
        layout.addView(inputUrl)
        layout.addView(btnGuardar)
        layout.addView(btnPermiso)

        setContentView(layout)
    }
}
