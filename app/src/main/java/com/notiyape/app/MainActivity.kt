package com.notiyape.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : Activity() {

    private val BOT_USERNAME = "alertasaudioyape_pe_bot"
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbyOiEdKFVc08ti51lu4Q4UT2qAadmiR3D6jIjUxul8coZHtTQNHgUvO-0LzUuu11ewOFQ/exec"
    private val client = OkHttpClient()

    private lateinit var txtEstadoPermiso: TextView
    private lateinit var layoutRegistro: LinearLayout
    private lateinit var layoutActivo: LinearLayout
    private lateinit var txtInfoTienda: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        scroll.addView(mainLayout)

        val titulo = TextView(this).apply {
            text = "NotiCobros Perú 🇵🇪"
            textSize = 24f
            setTextColor(Color.parseColor("#107C41"))
            setPadding(0, 0, 0, 10)
        }

        txtEstadoPermiso = TextView(this).apply {
            textSize = 15f
            setPadding(0, 0, 0, 25)
        }

        // ================= VISTA 1: REGISTRO NUEVO =================
        layoutRegistro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val subtitulo = TextView(this).apply {
            text = "Configura tu negocio en 1 paso:"
            textSize = 16f
            setPadding(0, 0, 0, 15)
        }

        val inputNombre = EditText(this).apply {
            hint = "Nombre del Negocio (Ej: Bodega Don Pepe)"
        }

        val inputCorreo = EditText(this).apply {
            hint = "Tu correo Gmail (para darte tu Excel)"
        }

        val btnCrear = Button(this).apply {
            text = "✨ CREAR MI EXCEL Y ACTIVAR"
            setBackgroundColor(Color.parseColor("#107C41"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val nombre = inputNombre.text.toString().trim()
                val correo = inputCorreo.text.toString().trim()

                if (nombre.isEmpty() || correo.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                Toast.makeText(this@MainActivity, "Creando tu Excel en Google...", Toast.LENGTH_LONG).show()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = JSONObject().apply {
                            put("accion", "crear_tienda")
                            put("nombreNegocio", nombre)
                            put("correo", correo)
                        }
                        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                        val request = Request.Builder().url(SERVIDOR_URL).post(body).build()
                        val res = client.newCall(request).execute()
                        val resBody = res.body?.string() ?: ""
                        res.close()

                        val resJson = JSONObject(resBody)
                        if (resJson.optString("status") == "exito") {
                            val cod = resJson.getString("codigoTienda")
                            prefs.edit().putString("codigo_tienda", cod).putString("nombre_tienda", nombre).apply()

                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "¡Listo! Excel creado y compartido", Toast.LENGTH_LONG).show()
                                actualizarVistas(prefs)
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Error: ${resJson.optString("mensaje")}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        layoutRegistro.addView(subtitulo)
        layoutRegistro.addView(inputNombre)
        layoutRegistro.addView(inputCorreo)
        layoutRegistro.addView(btnCrear)

        // ================= VISTA 2: NEGOCIO YA ACTIVADO =================
        layoutActivo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        txtInfoTienda = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }

        val btnTelegram = Button(this).apply {
            text = "1. Vincular mi Celular a Telegram"
            setBackgroundColor(Color.parseColor("#0088CC"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val cod = prefs.getString("codigo_tienda", "") ?: ""
                val urlTelegram = "https://t.me/$BOT_USERNAME?start=$cod"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlTelegram)))
            }
        }

        val btnPermiso = Button(this).apply {
            text = "2. Activar Permiso de Lectura"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val btnTestDirecto = Button(this).apply {
            text = "🚀 PROBAR COBRO (Simulación)"
            setOnClickListener {
                val cod = prefs.getString("codigo_tienda", "") ?: ""
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = """{"codigoTienda": "$cod", "nombre": "PRUEBA DEL CLIENTE", "monto": "1.00"}"""
                        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                        val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                        client.newCall(req).execute().close()
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "¡Enviado! Revisa Telegram y tu Excel", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {}
                }
            }
        }

        layoutActivo.addView(txtInfoTienda)
        layoutActivo.addView(btnTelegram)
        layoutActivo.addView(btnPermiso)
        layoutActivo.addView(btnTestDirecto)

        mainLayout.addView(titulo)
        mainLayout.addView(txtEstadoPermiso)
        mainLayout.addView(layoutRegistro)
        mainLayout.addView(layoutActivo)

        setContentView(scroll)
        actualizarVistas(prefs)
    }

    private fun actualizarVistas(prefs: android.content.SharedPreferences) {
        val cod = prefs.getString("codigo_tienda", "") ?: ""
        val nom = prefs.getString("nombre_tienda", "") ?: ""

        if (cod.isNotEmpty()) {
            layoutRegistro.visibility = View.GONE
            layoutActivo.visibility = View.VISIBLE
            txtInfoTienda.text = "🏪 Negocio: $nom\n🔑 Código: $cod\n\nTu Excel está creado y recibiendo cobros."
        } else {
            layoutRegistro.visibility = View.VISIBLE
            layoutActivo.visibility = View.GONE
        }
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
