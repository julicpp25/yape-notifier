package com.notiyape.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private val BOT_USERNAME = "alertasaudioyape_pe_bot"
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbyOiEdKFVc08ti51lu4Q4UT2qAadmiR3D6jIjUxul8coZHtTQNHgUvO-0LzUuu11ewOFQ/exec"
    private val client = OkHttpClient()

    private lateinit var layoutVentana1: LinearLayout
    private lateinit var layoutVentana2: LinearLayout
    private lateinit var layoutVentana3: LinearLayout
    private lateinit var layoutDashboard: LinearLayout

    private lateinit var txtEstadoPermiso: TextView
    private lateinit var txtTotalHoy: TextView
    private lateinit var layoutListaCobros: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this)
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 50, 40, 40)
        }
        scroll.addView(mainContainer)

        // ========================================================
        // VENTANA 1: REGISTRO + TIENDA (100% GRATIS)
        // ========================================================
        layoutVentana1 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val badgeGratis = TextView(this).apply {
            text = "🎁 SERVICIO 100% GRATIS Y COMUNITARIO"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setPadding(20, 15, 20, 15)
            gravity = Gravity.CENTER
        }

        val tit1 = TextView(this).apply {
            text = "Bienvenido a NotiCobros 🇵🇪"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#107C41"))
            setPadding(0, 30, 0, 10)
        }

        val desc1 = TextView(this).apply {
            text = "Esta aplicación avisa tus cobros en voz alta y te permite ausentarte de tu negocio con total seguridad.\n\nPara comenzar, ingresa los datos de tu tienda:"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 25)
        }

        val inputNombre = EditText(this).apply {
            hint = "Nombre de tu Tienda (Ej: Bodega San Martín)"
        }

        val inputCorreo = EditText(this).apply {
            hint = "Tu Correo Electrónico (Para tu cuenta)"
        }

        val btnContinuar1 = Button(this).apply {
            text = "Siguiente: Conectar Alertas ➡️"
            setBackgroundColor(Color.parseColor("#107C41"))
            setTextColor(Color.WHITE)
            setPadding(0, 25, 0, 25)
            setOnClickListener {
                val nom = inputNombre.text.toString().trim()
                val cor = inputCorreo.text.toString().trim()

                if (nom.isEmpty() || cor.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Por favor completa ambos campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Genera código único basado en el nombre
                val codigoGenerado = nom.replace(" ", "").take(6).uppercase() + "-" + (100..999).random()
                prefs.edit()
                    .putString("nombre_tienda", nom)
                    .putString("correo_tienda", cor)
                    .putString("codigo_tienda", codigoGenerado)
                    .apply()

                mostrarVentana(2)
            }
        }

        layoutVentana1.addView(badgeGratis)
        layoutVentana1.addView(tit1)
        layoutVentana1.addView(desc1)
        layoutVentana1.addView(inputNombre)
        layoutVentana1.addView(inputCorreo)
        layoutVentana1.addView(btnContinuar1)

        // ========================================================
        // VENTANA 2: TELEGRAM OBLIGATORIO
        // ========================================================
        layoutVentana2 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val tit2 = TextView(this).apply {
            text = "Paso 2: Conectar Alertas Personales"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0088CC"))
            setPadding(0, 0, 0, 15)
        }

        val avisoTelegram = TextView(this).apply {
            text = "⚠️ REQUISITO IMPORTANTE:\nDebes tener descargada la app Telegram en tu celular personal (iPhone o Android) para que te suenen los cobros cuando no estés en la tienda."
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#B71C1C"))
            setBackgroundColor(Color.parseColor("#FFEBEE"))
            setPadding(25, 20, 25, 20)
        }

        val descTelegram = TextView(this).apply {
            text = "\nToca el botón azul para abrir Telegram. Al abrirse, solo debes presionar 'INICIAR' para vincular este negocio."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 20)
        }

        val btnAbrirTelegram = Button(this).apply {
            text = "📲 Vincular con Telegram (1 Toque)"
            setBackgroundColor(Color.parseColor("#0088CC"))
            setTextColor(Color.WHITE)
            setPadding(0, 25, 0, 25)
            setOnClickListener {
                val cod = prefs.getString("codigo_tienda", "TEST-01")
                val url = "https://t.me/$BOT_USERNAME?start=$cod"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        val btnContinuar2 = Button(this).apply {
            text = "Ya lo vinculé en Telegram ➡️"
            setPadding(0, 25, 0, 25)
            setOnClickListener {
                mostrarVentana(3)
            }
        }

        layoutVentana2.addView(tit2)
        layoutVentana2.addView(avisoTelegram)
        layoutVentana2.addView(descTelegram)
        layoutVentana2.addView(btnAbrirTelegram)
        layoutVentana2.addView(btnContinuar2)

        // ========================================================
        // VENTANA 3: PERMISOS + EXPLICACIÓN + PRUEBA ÚNICA
        // ========================================================
        layoutVentana3 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val tit3 = TextView(this).apply {
            text = "Paso 3: Activar Lector de Yape"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 15)
        }

        val explicacionPermiso = TextView(this).apply {
            text = "¿Por qué activar este permiso?\n\nEsta aplicación necesita escuchar las notificaciones de Yape y Plin para:\n\n1. Hablar en voz alta: '¡Te llegó un Yape de 10 soles!'.\n2. Guardar automáticamente tu reporte contable.\n3. Enviarte la alerta a tu Telegram cuando estés ausente."
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            setPadding(25, 20, 25, 20)
        }

        txtEstadoPermiso = TextView(this).apply {
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 20, 0, 20)
        }

        val btnActivarPermiso = Button(this).apply {
            text = "🛡️ Activar Lector de Yape"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val descPruebaUnica = TextView(this).apply {
            text = "\nHaz clic abajo para simular un cobro de prueba (habilitado 1 sola vez para verificar que todo se vinculó):"
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 15)
        }

        val btnPruebaUnica = Button(this).apply {
            text = "🚀 Probar Cobro de Verificación"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setPadding(0, 25, 0, 25)
            setOnClickListener {
                isEnabled = false // Se deshabilita para que solo se use 1 vez
                val cod = prefs.getString("codigo_tienda", "TEST-01") ?: "TEST-01"
                
                Toast.makeText(this@MainActivity, "Enviando prueba...", Toast.LENGTH_SHORT).show()

                CoroutineScope(Dispatchers.IO).launch {
                    val json = """{"codigoTienda": "$cod", "nombre": "COBRO VERIFICADO", "monto": "1.00"}"""
                    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    try {
                        client.newCall(req).execute().close()
                    } catch (e: Exception) {}

                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "¡Configuración exitosa! Bienvenido.", Toast.LENGTH_LONG).show()
                        prefs.edit().putBoolean("onboarding_listo", true).apply()
                        mostrarVentana(4)
                    }
                }
            }
        }

        layoutVentana3.addView(tit3)
        layoutVentana3.addView(explicacionPermiso)
        layoutVentana3.addView(txtEstadoPermiso)
        layoutVentana3.addView(btnActivarPermiso)
        layoutVentana3.addView(descPruebaUnica)
        layoutVentana3.addView(btnPruebaUnica)

        // ========================================================
        // VENTANA 4: PANEL DE CONTROL PRINCIPAL (DASHBOARD EN VIVO)
        // ========================================================
        layoutDashboard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val bannerTienda = TextView(this).apply {
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#107C41"))
            setPadding(0, 0, 0, 5)
        }

        val bannerCodigo = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, 20)
        }

        // Tarjeta de total del día
        val cardTotal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setPadding(30, 25, 30, 25)
        }
        val lblTotal = TextView(this).apply {
            text = "VENTAS TOTALES DE HOY:"
            textSize = 12f
            setTextColor(Color.parseColor("#2E7D32"))
            setTypeface(null, Typeface.BOLD)
        }
        txtTotalHoy = TextView(this).apply {
            text = "S/ 0.00"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1B5E20"))
        }
        cardTotal.addView(lblTotal)
        cardTotal.addView(txtTotalHoy)

        val titHistorial = TextView(this).apply {
            text = "\n📋 Últimos Cobros Recibidos en Vivo:"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(0, 15, 0, 15)
        }

        layoutListaCobros = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        layoutDashboard.addView(bannerTienda)
        layoutDashboard.addView(bannerCodigo)
        layoutDashboard.addView(cardTotal)
        layoutDashboard.addView(titHistorial)
        layoutDashboard.addView(layoutListaCobros)

        mainContainer.addView(layoutVentana1)
        mainContainer.addView(layoutVentana2)
        mainContainer.addView(layoutVentana3)
        mainContainer.addView(layoutDashboard)

        setContentView(scroll)

        // Si ya completó los pasos en el pasado, entra directo al Dashboard
        val yaCompleto = prefs.getBoolean("onboarding_listo", false)
        if (yaCompleto) {
            val nom = prefs.getString("nombre_tienda", "Mi Tienda")
            val cod = prefs.getString("codigo_tienda", "")
            bannerTienda.text = "🏪 $nom"
            bannerCodigo.text = "Código de Tienda: $cod"
            mostrarVentana(4)
        } else {
            mostrarVentana(1)
        }
    }

    private fun mostrarVentana(paso: Int) {
        layoutVentana1.visibility = if (paso == 1) View.VISIBLE else View.GONE
        layoutVentana2.visibility = if (paso == 2) View.VISIBLE else View.GONE
        layoutVentana3.visibility = if (paso == 3) View.VISIBLE else View.GONE
        layoutDashboard.visibility = if (paso == 4) View.VISIBLE else View.GONE

        if (paso == 4) {
            cargarHistorialLocal()
        }
    }

    private fun cargarHistorialLocal() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val historialStr = prefs.getString("historial_cobros", "[]") ?: "[]"
        val array = JSONArray(historialStr)

        layoutListaCobros.removeAllViews()

        var sumaTotal = 0.0
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        if (array.length() == 0) {
            val txtVacio = TextView(this).apply {
                text = "Aún no se han recibido pagos hoy.\nLos cobros aparecerán aquí automáticamente."
                setTextColor(Color.GRAY)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            layoutListaCobros.addView(txtVacio)
        } else {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val nombre = item.optString("nombre", "Cliente")
                val montoStr = item.optString("monto", "0.00")
                val hora = item.optString("hora", "")
                val fecha = item.optString("fecha", "")
                val metodo = item.optString("metodo", "Yape")

                // Sumar al total del día
                if (fecha == fechaHoy) {
                    sumaTotal += montoStr.toDoubleOrNull() ?: 0.0
                }

                // Tarjeta de cobro individual
                val cardItem = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(20, 20, 20, 20)
                    setBackgroundColor(if (i % 2 == 0) Color.WHITE else Color.parseColor("#FAFAFA"))
                }

                val colTexto = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val txtNombre = TextView(this).apply {
                    text = nombre
                    setTypeface(null, Typeface.BOLD)
                    textSize = 14f
                    setTextColor(Color.BLACK)
                }

                val txtDetalle = TextView(this).apply {
                    text = "$metodo • $hora"
                    textSize = 12f
                    setTextColor(Color.GRAY)
                }

                colTexto.addView(txtNombre)
                colTexto.addView(txtDetalle)

                val txtMonto = TextView(this).apply {
                    text = "+ S/ $montoStr"
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#107C41"))
                    gravity = Gravity.END
                }

                cardItem.addView(colTexto)
                cardItem.addView(txtMonto)

                layoutListaCobros.addView(cardItem)
            }
        }

        txtTotalHoy.text = String.format(Locale.US, "S/ %.2f", sumaTotal)
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

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_listo", false)) {
            cargarHistorialLocal()
        }
    }
}
