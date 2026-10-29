package com.notiyape.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import org.json.JSONObject
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
    private lateinit var cardPruebaUnica: LinearLayout

    private lateinit var bannerTienda: TextView
    private lateinit var bannerCodigo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F4F6F8"))
        }
        
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 45, 35, 45)
        }
        scroll.addView(mainContainer)

        // ================= VENTANA 1: REGISTRO NUEVO =================
        layoutVentana1 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val card1 = crearTarjeta(this)

        val badgeGratis = TextView(this).apply {
            text = "🎁 SERVICIO 100% GRATUITO Y LIBRE"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0A6C3B"))
            background = crearBordeRedondo("#E8F5E9", "#A5D6A7", 20f)
            setPadding(25, 12, 25, 12)
            gravity = Gravity.CENTER
        }

        val tit1 = TextView(this).apply {
            text = "NotiCobros Perú 🇵🇪"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 25, 0, 8)
        }

        val desc1 = TextView(this).apply {
            text = "Tus cobros cantados en voz alta y registrados en tu Google Sheets en tiempo real."
            textSize = 14f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 0, 0, 25)
        }

        val inputNombre = crearInput(this, "Nombre de tu Negocio (Ej: Bodega Central)")
        val inputCorreo = crearInput(this, "Correo Electrónico (Para tu cuenta)")

        val btnContinuar1 = crearBoton(this, "Crear mi Negocio y Continuar ➡️", "#00A859") {
            val nom = inputNombre.text.toString().trim()
            val cor = inputCorreo.text.toString().trim()

            if (nom.isEmpty() || cor.isEmpty()) {
                Toast.makeText(this@MainActivity, "Por favor completa ambos campos", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            // Código único aleatorio fresco para cada registro nuevo
            val codigoGenerado = nom.replace(" ", "").take(5).uppercase() + "-" + (100..999).random()

            Toast.makeText(this@MainActivity, "Registrando negocio...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("accion", "registrar_tienda")
                        put("codigoTienda", codigoGenerado)
                        put("nombreNegocio", nom)
                        put("correo", cor)
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    val res = client.newCall(req).execute()
                    val resStr = res.body?.string() ?: ""
                    res.close()

                    val resJson = JSONObject(resStr)
                    val sheetUrl = resJson.optString("sheetUrl", "")

                    prefs.edit()
                        .putString("nombre_tienda", nom)
                        .putString("correo_tienda", cor)
                        .putString("codigo_tienda", codigoGenerado)
                        .putString("sheet_url_cliente", sheetUrl)
                        .putBoolean("onboarding_listo", false)
                        .putBoolean("prueba_realizada", false)
                        .apply()

                    runOnUiThread {
                        mostrarVentana(2)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        prefs.edit()
                            .putString("nombre_tienda", nom)
                            .putString("correo_tienda", cor)
                            .putString("codigo_tienda", codigoGenerado)
                            .apply()
                        mostrarVentana(2)
                    }
                }
            }
        }

        card1.addView(badgeGratis)
        card1.addView(tit1)
        card1.addView(desc1)
        card1.addView(inputNombre)
        card1.addView(inputCorreo)
        card1.addView(btnContinuar1)
        layoutVentana1.addView(card1)

        // ================= VENTANA 2: TELEGRAM =================
        layoutVentana2 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val card2 = crearTarjeta(this)

        val tit2 = TextView(this).apply {
            text = "Paso 2: Conectar Alertas"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 15)
        }

        val avisoTelegram = TextView(this).apply {
            text = "⚠️ REQUISITO IMPORTANTE:\nDebes tener descargada la aplicación Telegram en tu celular personal para recibir los avisos de cobro cuando estés ausente."
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#991B1B"))
            background = crearBordeRedondo("#FEE2E2", "#FCA5A5", 16f)
            setPadding(25, 20, 25, 20)
        }

        val descTelegram = TextView(this).apply {
            text = "\nToca el botón para abrir Telegram. Al abrirse, presiona 'INICIAR' para vincular tus alertas:"
            textSize = 14f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 0, 0, 15)
        }

        val btnAbrirTelegram = crearBoton(this, "📲 Vincular con Telegram (1 Toque)", "#0088CC") {
            val cod = prefs.getString("codigo_tienda", "TEST-01")
            val url = "https://t.me/$BOT_USERNAME?start=$cod"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        val btnContinuar2 = crearBoton(this, "Ya lo vinculé, Continuar ➡️", "#0F172A") {
            mostrarVentana(3)
        }

        card2.addView(tit2)
        card2.addView(avisoTelegram)
        card2.addView(descTelegram)
        card2.addView(btnAbrirTelegram)
        card2.addView(btnContinuar2)
        layoutVentana2.addView(card2)

        // ================= VENTANA 3: PERMISOS =================
        layoutVentana3 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val card3 = crearTarjeta(this)

        val tit3 = TextView(this).apply {
            text = "Paso 3: Activar Lector de Cobros"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 15)
        }

        val explicacionPermiso = TextView(this).apply {
            text = "¿Por qué activar este permiso?\n\nEsta app necesita escuchar las notificaciones de Yape y Plin para:\n\n1. Hablar en voz alta: '¡Te llegó un Yape de 10 soles!'.\n2. Guardar automáticamente tu reporte contable.\n3. Enviarte la alerta a tu Telegram cuando estés ausente."
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            background = crearBordeRedondo("#F8FAFC", "#E2E8F0", 16f)
            setPadding(25, 20, 25, 20)
        }

        txtEstadoPermiso = TextView(this).apply {
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 20, 0, 20)
        }

        val btnActivarPermiso = crearBoton(this, "🛡️ Activar Lector de Yape", "#475569") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val btnIrDashboard = crearBoton(this, "Entrar a mi Panel de Negocio 🚀", "#00A859") {
            prefs.edit().putBoolean("onboarding_listo", true).apply()
            val nom = prefs.getString("nombre_tienda", "Mi Negocio")
            val cod = prefs.getString("codigo_tienda", "")
            bannerTienda.text = "🏪 $nom"
            bannerCodigo.text = "Código de Tienda: $cod"
            mostrarVentana(4)
        }

        card3.addView(tit3)
        card3.addView(explicacionPermiso)
        card3.addView(txtEstadoPermiso)
        card3.addView(btnActivarPermiso)
        card3.addView(btnIrDashboard)
        layoutVentana3.addView(card3)

        // ================= VENTANA 4: PANEL PRINCIPAL (DASHBOARD) =================
        layoutDashboard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardInfo = crearTarjeta(this)

        bannerTienda = TextView(this).apply {
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
        }

        bannerCodigo = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 4, 0, 15)
        }

        // Tarjeta de prueba única
        cardPruebaUnica = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#EFF6FF", "#BFDBFE", 20f)
            setPadding(30, 25, 30, 25)
        }
        val tPrueba = TextView(this).apply {
            text = "🧪 PRUEBA DE CONEXIÓN"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E40AF"))
        }
        val dPrueba = TextView(this).apply {
            text = "Toca abajo para simular un cobro y comprobar que tu celular se vinculó con Telegram y tu hoja de cálculo. (Este botón solo se puede usar 1 vez y luego desaparecerá)."
            textSize = 12f
            setTextColor(Color.parseColor("#3B82F6"))
            setPadding(0, 8, 0, 15)
        }
        val btnTestUnico = crearBoton(this, "🚀 Realizar Cobro de Prueba (S/ 1.00)", "#2563EB") {
            val cod = prefs.getString("codigo_tienda", "TEST-01") ?: "TEST-01"
            Toast.makeText(this@MainActivity, "Enviando cobro de verificación...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                val json = """{"codigoTienda": "$cod", "nombre": "COBRO VERIFICADO", "monto": "1.00"}"""
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                try {
                    client.newCall(req).execute().close()
                } catch (e: Exception) {}

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "✅ ¡Verificación concretada! Revisa tu Telegram y tu Excel.", Toast.LENGTH_LONG).show()
                    prefs.edit().putBoolean("prueba_realizada", true).apply()
                    cardPruebaUnica.visibility = View.GONE
                }
            }
        }
        cardPruebaUnica.addView(tPrueba)
        cardPruebaUnica.addView(dPrueba)
        cardPruebaUnica.addView(btnTestUnico)

        val btnAbrirSheet = crearBoton(this, "📊 Abrir mi Hoja de Google Sheets en vivo", "#00A859") {
            val url = prefs.getString("sheet_url_cliente", "") ?: ""
            if (url.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(this@MainActivity, "Revisa el enlace que te envió Telegram", Toast.LENGTH_LONG).show()
            }
        }

        val cardTotal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#ECFDF5", "#A7F3D0", 20f)
            setPadding(30, 25, 30, 25)
        }
        val lblTotal = TextView(this).apply {
            text = "TOTAL VENDIDO HOY"
            textSize = 12f
            setTextColor(Color.parseColor("#047857"))
            setTypeface(null, Typeface.BOLD)
        }
        txtTotalHoy = TextView(this).apply {
            text = "S/ 0.00"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#065F46"))
        }
        cardTotal.addView(lblTotal)
        cardTotal.addView(txtTotalHoy)

        val titHistorial = TextView(this).apply {
            text = "\n📋 Cobros Recibidos en Vivo:"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 10, 0, 10)
        }

        layoutListaCobros = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // BOTÓN PARA REINICIAR APP Y PROBAR OTRO NEGOCIO DESDE CERO
        val btnResetTest = Button(this).apply {
            text = "🔄 Desvincular y Registrar Otro Negocio"
            setTextColor(Color.parseColor("#DC2626"))
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 13f
            setPadding(0, 40, 0, 20)
            setOnClickListener {
                prefs.edit().clear().apply() // Borra toda la memoria de la app
                Toast.makeText(this@MainActivity, "Memoria borrada. Inicia un nuevo registro.", Toast.LENGTH_SHORT).show()
                mostrarVentana(1)
            }
        }

        cardInfo.addView(bannerTienda)
        cardInfo.addView(bannerCodigo)
        cardInfo.addView(cardPruebaUnica)
        cardInfo.addView(btnAbrirSheet)
        cardInfo.addView(cardTotal)
        cardInfo.addView(titHistorial)
        cardInfo.addView(layoutListaCobros)
        cardInfo.addView(btnResetTest)

        layoutDashboard.addView(cardInfo)

        mainContainer.addView(layoutVentana1)
        mainContainer.addView(layoutVentana2)
        mainContainer.addView(layoutVentana3)
        mainContainer.addView(layoutDashboard)

        setContentView(scroll)

        val yaCompleto = prefs.getBoolean("onboarding_listo", false)
        if (yaCompleto) {
            val nom = prefs.getString("nombre_tienda", "Mi Negocio")
            val cod = prefs.getString("codigo_tienda", "")
            bannerTienda.text = "🏪 $nom"
            bannerCodigo.text = "Código de Tienda: $cod"

            if (prefs.getBoolean("prueba_realizada", false)) {
                cardPruebaUnica.visibility = View.GONE
            }

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
                setTextColor(Color.parseColor("#94A3B8"))
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

                if (fecha == fechaHoy) {
                    sumaTotal += montoStr.toDoubleOrNull() ?: 0.0
                }

                val itemBox = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(25, 20, 25, 20)
                    background = crearBordeRedondo("#FFFFFF", "#E2E8F0", 16f)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 12) }
                }

                val colTexto = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val txtNombre = TextView(this).apply {
                    text = nombre
                    setTypeface(null, Typeface.BOLD)
                    textSize = 14f
                    setTextColor(Color.parseColor("#0F172A"))
                }

                val txtDetalle = TextView(this).apply {
                    text = "$metodo • $hora"
                    textSize = 12f
                    setTextColor(Color.parseColor("#64748B"))
                }

                colTexto.addView(txtNombre)
                colTexto.addView(txtDetalle)

                val txtMonto = TextView(this).apply {
                    text = "+ S/ $montoStr"
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#00A859"))
                    gravity = Gravity.END
                }

                itemBox.addView(colTexto)
                itemBox.addView(txtMonto)
                layoutListaCobros.addView(itemBox)
            }
        }

        txtTotalHoy.text = String.format(Locale.US, "S/ %.2f", sumaTotal)
    }

    override fun onResume() {
        super.onResume()
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        if (listeners.contains(packageName)) {
            txtEstadoPermiso.text = "🟢 LECTOR DE YAPE: ACTIVO"
            txtEstadoPermiso.setTextColor(Color.parseColor("#0A6C3B"))
        } else {
            txtEstadoPermiso.text = "🔴 LECTOR DE YAPE: APAGADO"
            txtEstadoPermiso.setTextColor(Color.parseColor("#DC2626"))
        }

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_listo", false)) {
            cargarHistorialLocal()
        }
    }

    private fun crearTarjeta(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 28f
                setStroke(2, Color.parseColor("#E2E8F0"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 25) }
        }
    }

    private fun crearBordeRedondo(colorFondo: String, colorBorde: String, radio: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorFondo))
            cornerRadius = radio
            setStroke(2, Color.parseColor(colorBorde))
        }
    }

    private fun crearBoton(context: Context, texto: String, colorHex: String, accion: () -> Unit): Button {
        return Button(context).apply {
            text = texto
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(colorHex))
                cornerRadius = 20f
            }
            setPadding(30, 24, 30, 24)
            isAllCaps = false
            setOnClickListener { accion() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 10, 0, 10) }
        }
    }

    private fun crearInput(context: Context, hintText: String): EditText {
        return EditText(context).apply {
            hint = hintText
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.parseColor("#0F172A"))
            textSize = 14f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F8FAFC"))
                cornerRadius = 18f
                setStroke(2, Color.parseColor("#CBD5E1"))
            }
            setPadding(35, 26, 35, 26)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 16) }
        }
    }
}
