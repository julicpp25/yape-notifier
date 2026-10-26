package com.notiyape.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
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

        // Genera código único automático (Ej: T-5482)
        var codTienda = prefs.getString("codigo_tienda", "") ?: ""
        if (codTienda.isEmpty()) {
            codTienda = "T-" + (1000..9999).random()
            prefs.edit().putString("codigo_tienda", codTienda).apply()
        }

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 60, 50, 50)
        }
        scroll.addView(layout)

        val titulo = TextView(this).apply {
            text = "NotiCobros Perú 🇵🇪"
            textSize = 24f
            setTextColor(Color.parseColor("#107C41"))
            setPadding(0, 0, 0, 15)
        }

        val txtCodigo = TextView(this).apply {
            text = "Tu Código de Tienda: $codTienda\n"
            textSize = 16f
            setTextColor(Color.BLACK)
        }

        txtEstadoPermiso = TextView(this).apply {
            textSize = 15f
            setPadding(0, 0, 0, 25)
        }

        // ================= PASO 1: TELEGRAM =================
        val btnTelegram = Button(this).apply {
            text = "PASO 1: Vincular mi Telegram (1 Toque)"
            setBackgroundColor(Color.parseColor("#0088CC"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val url = "https://t.me/$BOT_USERNAME?start=$codTienda"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        // ================= PASO 2: PERMISO LECTOR =================
        val btnPermiso = Button(this).apply {
            text = "PASO 2: Activar Lector de Yape"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        // ================= PROBAR =================
        val btnTest = Button(this).apply {
            text = "🚀 PROBAR COBRO (Simulación)"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = """{"codigoTienda": "$codTienda", "nombre": "COBRO DE PRUEBA", "monto": "1.00"}"""
                        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                        val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                        client.newCall(req).execute().close()
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "¡Enviado! Revisa tu Telegram", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {}
                }
            }
        }

        layout.addView(titulo)
        layout.addView(txtCodigo)
        layout.addView(txtEstadoPermiso)
        layout.addView(btnTelegram)
        layout.addView(btnPermiso)
        layout.addView(btnTest)

        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        if (listeners.contains(packageName)) {
            txtEstadoPermiso.text = "🟢 LECTOR DE YAPE: ACTIVO"
            txtEstadoPermiso.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            txtEstadoPermiso.text = "🔴 LECTOR DE YAPE: APAGADO"
            txtEstadoPermiso.setTextColor(Color.RED)
        }
    }
}
