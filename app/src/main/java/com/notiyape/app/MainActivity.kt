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

    private lateinit var layoutPaso1: LinearLayout
    private lateinit var layoutPaso2: LinearLayout
    private lateinit var layoutPaso3: LinearLayout
    private lateinit var txtEstadoPermiso: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

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

        // ================= PASO 1: DATOS DEL NEGOCIO =================
        layoutPaso1 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        val t1 = TextView(this).apply {
            text = "PASO 1 DE 3: Tu Negocio y Excel"
            textSize = 17f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 10)
        }
        val desc1 = TextView(this).apply {
            text = "Ingresa tu negocio para crear tu hoja contable automática:"
            textSize = 13f
            setPadding(0, 0, 0, 15)
        }
        val inputNombre = EditText(this).apply { hint = "Nombre del Negocio (Ej: Bodega Central)" }
        val inputCorreo = EditText(this).apply { hint = "Tu correo Gmail (para darte el Excel)" }
        
        val btnPaso1 = Button(this).apply {
            text = "Crear mi Excel y Pasar al Paso 2 ➡️"
            setBackgroundColor(Color.parseColor("#107C41"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val nom = inputNombre.text.toString().trim()
                val cor = inputCorreo.text.toString().trim()

                if (nom.isEmpty() || cor.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                Toast.makeText(this@MainActivity, "Creando tu Excel en Google...", Toast.LENGTH_SHORT).show()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = JSONObject().apply {
                            put("accion", "crear_tienda")
                            put("nombreNegocio", nom)
                            put("correo", cor)
                        }
                        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                        val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                        val res = client.newCall(req).execute()
                        val resStr = res.body?.string() ?: ""
                        res.close()

                        val resJson = JSONObject(resStr)
                        if (resJson.optString("status") == "exito") {
                            val cod = resJson.getString("codigoTienda")
                            prefs.edit().putString("codigo_tienda", cod).putString("nombre_tienda", nom).apply()
                            
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "¡Excel creado con éxito!", Toast.LENGTH_SHORT).show()
                                mostrarPaso(2)
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

        layoutPaso1.addView(t1)
        layoutPaso1.addView(desc1)
        layoutPaso1.addView(inputNombre)
        layoutPaso1.addView(inputCorreo)
        layoutPaso1.addView(btnPaso1)

        // ================= PASO 2: VINCULAR TELEGRAM =================
        layoutPaso2 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        val t2 = TextView(this).apply {
            text = "PASO 2 DE 3: ¿A qué celular avisamos?"
            textSize = 17f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 10)
        }
        val desc2 = TextView(this).apply {
            text = "Conecta el teléfono personal donde quieres que suenen las alertas cuando estés ausente:"
            textSize = 13f
            setPadding(0, 0, 0, 15)
        }
        val btnTelegram = Button(this).apply {
            text = "📲 1. Conectar con Telegram (1 toque)"
            setBackgroundColor(Color.parseColor("#0088CC"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val cod = prefs.getString("codigo_tienda", "") ?: ""
                val url = "https://t.me/$BOT_USERNAME?start=$cod"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
        val btnPaso2 = Button(this).apply {
            text = "Ya lo vinculé, ir al Paso Final ➡️"
            setOnClickListener { mostrarPaso(3) }
        }

        layoutPaso2.addView(t2)
        layoutPaso2.addView(desc2)
        layoutPaso2.addView(btnTelegram)
        layoutPaso2.addView(btnPaso2)

        // ================= PASO 3: ACTIVAR LECTOR EN CAJA =================
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
                val cod = prefs.getString("codigo_tienda", "") ?: ""
                CoroutineScope(Dispatchers.IO).launch {
                    val json = """{"codigoTienda": "$cod", "nombre": "COBRO DE PRUEBA", "monto": "1.00"}"""
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

        // Si ya tenía tienda guardada va directo al Paso 3, si es nuevo arranca en Paso 1
        val codGuardado = prefs.getString("codigo_tienda", "") ?: ""
        if (codGuardado.isNotEmpty()) {
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
