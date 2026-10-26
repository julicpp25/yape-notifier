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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

class MainActivity : Activity() {

    private val BOT_USERNAME = "alertasaudioyape_pe_bot"
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbyOiEdKFVc08ti51lu4Q4UT2qAadmiR3D6jIjUxul8coZHtTQNHgUvO-0LzUuu11ewOFQ/exec"
    private val client = OkHttpClient()

    private lateinit var layoutPaso1: LinearLayout
    private lateinit var layoutPaso2: LinearLayout
    private lateinit var layoutPaso3: LinearLayout
    private lateinit var txtEstadoPermiso: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        // Genera un código único automático si es nuevo (Ej: T-7821)
        var codTienda = prefs.getString("codigo_tienda", "") ?: ""
        if (codTienda.isEmpty()) {
            codTienda = "T-" + (1000..9999).random()
            prefs.edit().putString("codigo_tienda", codTienda).apply()
        }

        val scroll = ScrollView(this)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 60, 50, 50)
        }
        scroll.addView(mainLayout)

        val titulo = TextView(this).apply {
            text = "NotiCobros Perú 🇵🇪"
            textSize = 24f
            setTextColor(Color.parseColor("#107C41"))
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(titulo)

        // ================= PASO 1: VINCULAR TELEGRAM PRIMERO =================
        layoutPaso1 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        val t1 = TextView(this).apply {
            text = "PASO 1 DE 3: ¿A qué celular avisamos?"
            textSize = 17f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 10)
        }
        val desc1 = TextView(this).apply {
            text = "Conecta el teléfono personal donde quieres recibir las alertas de cobro cuando no estés en tu negocio:\n(Tu código de tienda: $codTienda)"
            textSize = 13f
            setPadding(0, 0, 0, 15)
        }
        val btnTelegram = Button(this).apply {
            text = "📲 1. VINCULAR CON TELEGRAM (1 Toque)"
            setBackgroundColor(Color.parseColor("#0088CC"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val url = "https://t.me/$BOT_USERNAME?start=$codTienda"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
        val btnIrPaso2 = Button(this).apply {
            text = "Ya lo vinculé, Continuar al Paso 2 ➡️"
            setOnClickListener { mostrarPaso(2) }
        }

        layoutPaso1.addView(t1)
        layoutPaso1.addView(desc1)
        layoutPaso1.addView(btnTelegram)
        layoutPaso1.addView(btnIrPaso2)

        // ================= PASO 2: CREAR EXCEL =================
        layoutPaso2 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        val t2 = TextView(this).apply {
            text = "PASO 2 DE 3: Tu Negocio y tu Excel"
            textSize = 17f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 10)
        }
        val desc2 = TextView(this).apply {
            text = "Ingresa los datos para generar tu hoja de ventas contable:"
            textSize = 13f
            setPadding(0, 0, 0, 15)
        }
        val inputNombre = EditText(this).apply { hint = "Nombre del Negocio (Ej: Bodega Central)" }
        val inputCorreo = EditText(this).apply { hint = "Tu correo Gmail (para darte el Excel)" }
        
        val btnCrearExcel = Button(this).apply {
            text = "✨ Crear mi Excel y Pasar al Paso 3 ➡️"
            setBackgroundColor(Color.parseColor("#107C41"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val nom = inputNombre.text.toString().trim()
                val cor = inputCorreo.text.toString().trim()

                if (nom.isEmpty() || cor.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                Toast.makeText(this@MainActivity, "Creando tu Excel...", Toast.LENGTH_SHORT).show()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val nomEnc = URLEncoder.encode(nom, "UTF-8")
                        val corEnc = URLEncoder.encode(cor, "UTF-8")
                        val urlGet = "$SERVIDOR_URL?accion=crear_tienda&codigoTienda=$codTienda&nombreNegocio=$nomEnc&correo=$corEnc"
                        
                        val req = Request.Builder().url(urlGet).get().build()
                        val res = client.newCall(req).execute()
                        val resStr = res.body?.string() ?: ""
                        res.close()

                        val resJson = JSONObject(resStr)
                        if (resJson.optString("status") == "exito") {
                            prefs.edit().putString("nombre_tienda", nom).putBoolean("excel_listo", true).apply()
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "¡Excel creado! Te llegó el link a Telegram", Toast.LENGTH_LONG).show()
                                mostrarPaso(3)
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Error: ${resJson.optString("mensaje")}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        layoutPaso2.addView(t2)
        layoutPaso2.addView(desc2)
        layoutPaso2.addView(inputNombre)
        layoutPaso2.addView(inputCorreo)
        layoutPaso2.addView(btnCrearExcel)

        // ================= PASO 3: ACTIVAR EN CAJA =================
        layoutPaso3 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        val t3 = TextView(this).apply {
            text = "PASO 3 DE 3: Activar Lector en Caja"
            textSize = 17f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 10)
        }
        txtEstadoPermiso = TextView(this).apply {
            textSize = 15f
            setPadding(0, 0, 0, 15)
        }
        val btnPermiso = Button(this).apply {
            text = "🛡️ Activar Permiso de Lectura"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        val btnTest = Button(this).apply {
            text = "🚀 Probar Cobro de Prueba"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val json = """{"codigoTienda": "$codTienda", "nombre": "COBRO DE PRUEBA", "monto": "1.00"}"""
                    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    client.newCall(req).execute().close()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "¡Enviado! Revisa Telegram y tu Excel", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        layoutPaso3.addView(t3)
        layoutPaso3.addView(txtEstadoPermiso)
        layoutPaso3.addView(btnPermiso)
        layoutPaso3.addView(btnTest)

        mainLayout.addView(layoutPaso1)
        mainLayout.addView(layoutPaso2)
        mainLayout.addView(layoutPaso3)

        setContentView(scroll)

        val excelListo = prefs.getBoolean("excel_listo", false)
        if (excelListo) {
            mostrarPaso(3)
        } else {
            mostrarPaso(1)
        }
    }

    private fun mostrarPaso(paso: Int) {
        layoutPaso1.visibility = if (paso == 1) View.VISIBLE else View.GONE
        layoutPaso2.visibility = if (paso == 2) View.VISIBLE else View.GONE
        layoutPaso3.visibility = if (paso == 3) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        if (listeners.contains(packageName)) {
            txtEstadoPermiso.text = "🟢 LECTOR DE COBROS: ACTIVO"
            txtEstadoPermiso.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            txtEstadoPermiso.text = "🔴 LECTOR DE COBROS: APAGADO"
            txtEstadoPermiso.setTextColor(Color.RED)
        }
    }
}
