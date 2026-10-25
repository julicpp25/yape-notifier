package com.notiyape.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : Activity() {

    private val BOT_USERNAME = "alertasaudioyape_pe_bot"
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbyOiEdKFVc08ti51lu4Q4UT2qAadmiR3D6jIjUxul8coZHtTQNHgUvO-0LzUuu11ewOFQ/exec"
    private val client = OkHttpClient()

    private lateinit var txtEstadoPermiso: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        if (!prefs.contains("codigo_tienda") || prefs.getString("codigo_tienda", "")!!.isEmpty()) {
            prefs.edit().putString("codigo_tienda", "TEST-01").apply()
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 60, 50, 50)
        }

        val titulo = TextView(this).apply {
            text = "NotiCobros Perú 🇵🇪"
            textSize = 22f
            setPadding(0, 0, 0, 20)
        }

        txtEstadoPermiso = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 30)
        }

        val subtitulo = TextView(this).apply {
            text = "Código de tu Tienda:"
            textSize = 14f
        }

        val inputCodigo = EditText(this).apply {
            setText(prefs.getString("codigo_tienda", "TEST-01"))
        }

        val btnGuardar = Button(this).apply {
            text = "Guardar Código"
            setOnClickListener {
                val cod = inputCodigo.text.toString().trim()
                val finalCod = if (cod.isEmpty()) "TEST-01" else cod
                prefs.edit().putString("codigo_tienda", finalCod).apply()
                Toast.makeText(this@MainActivity, "Guardado: $finalCod", Toast.LENGTH_SHORT).show()
            }
        }

        val btnPermiso = Button(this).apply {
            text = "1. Activar Permisos de Yape"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val btnTestDirecto = Button(this).apply {
            text = "🚀 PROBAR CONEXIÓN (Simular Cobro)"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val cod = prefs.getString("codigo_tienda", "TEST-01") ?: "TEST-01"
                Toast.makeText(this@MainActivity, "Enviando prueba al servidor...", Toast.LENGTH_SHORT).show()
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = """{"codigoTienda": "$cod", "nombre": "PRUEBA DESDE CELULAR", "monto": "5.00"}"""
                        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                        val request = Request.Builder().url(SERVIDOR_URL).post(body).build()
                        val response = client.newCall(request).execute()
                        val exito = response.isSuccessful
                        response.close()

                        runOnUiThread {
                            if (exito) {
                                Toast.makeText(this@MainActivity, "✅ ¡Enviado con éxito! Revisa Telegram", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@MainActivity, "❌ Error del servidor: ${response.code}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "❌ Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        layout.addView(titulo)
        layout.addView(txtEstadoPermiso)
        layout.addView(subtitulo)
        layout.addView(inputCodigo)
        layout.addView(btnGuardar)
        layout.addView(btnPermiso)
        layout.addView(btnTestDirecto)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoPermiso()
    }

    private fun actualizarEstadoPermiso() {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val tienePermiso = listeners.contains(packageName)

        if (tienePermiso) {
            txtEstadoPermiso.text = "🟢 PERMISO DE YAPE: ACTIVO"
            txtEstadoPermiso.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            txtEstadoPermiso.text = "🔴 PERMISO DE YAPE: APAGADO"
            txtEstadoPermiso.setTextColor(Color.RED)
        }
    }
}
