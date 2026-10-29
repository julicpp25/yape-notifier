package com.notiyape.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
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

    // VISTAS DEL ASISTENTE INICIAL (ONBOARDING)
    private lateinit var layoutWizard: LinearLayout
    private lateinit var paso1Layout: LinearLayout
    private lateinit var paso2Layout: LinearLayout
    private lateinit var paso3Layout: LinearLayout

    // VISTAS DEL SISTEMA PRINCIPAL (DESPUÉS DEL REGISTRO)
    private lateinit var layoutPrincipal: LinearLayout
    private lateinit var layoutCajero: LinearLayout
    private lateinit var layoutAdmin: LinearLayout

    // COMPONENTES CAJERO
    private lateinit var txtTurnoCajero: TextView
    private lateinit var imgQrGenerado: ImageView
    private lateinit var txtMontoCobroQr: TextView
    private lateinit var layoutListaCobrosCajero: LinearLayout
    private lateinit var switchVoz: Switch

    // COMPONENTES ADMIN
    private lateinit var txtTotalHoy: TextView
    private lateinit var cardPruebaUnica: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F4F6F8"))
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 40, 30, 40)
        }
        scroll.addView(mainContainer)

        // =====================================================================
        // FASE A: ASISTENTE DE REGISTRO INICIAL (SI LA TIENDA NO ESTÁ REGISTRADA)
        // =====================================================================
        layoutWizard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // --- PASO 1: DATOS + PIN DEL DUEÑO ---
        paso1Layout = crearTarjeta(this)
        val badge = TextView(this).apply {
            text = "🎁 SERVICIO 100% GRATUITO Y COMUNITARIO"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0A6C3B"))
            background = crearBordeRedondo("#E8F5E9", "#A5D6A7", 18f)
            setPadding(20, 10, 20, 10)
            gravity = Gravity.CENTER
        }
        val t1 = TextView(this).apply {
            text = "Registro de Tienda Nueva 🇵🇪"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 20, 0, 8)
        }
        val d1 = TextView(this).apply {
            text = "Configura tu negocio para empezar a cantar cobros y controlar tu caja:"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 0, 0, 15)
        }
        val inNombre = crearInput(this, "Nombre del Negocio (Ej: Bodega Central)")
        val inCorreo = crearInput(this, "Correo del Dueño (Para tu Excel)")
        val inPin = crearInput(this, "Crea tu PIN de Dueño (4 dígitos)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val btnPaso1 = crearBoton(this, "Continuar a Conectar Alertas ➡️", "#00A859") {
            val nom = inNombre.text.toString().trim()
            val cor = inCorreo.text.toString().trim()
            val pin = inPin.text.toString().trim()

            if (nom.isEmpty() || cor.isEmpty() || pin.length < 4) {
                Toast.makeText(this@MainActivity, "Completa todos los campos (PIN mínimo 4 dígitos)", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            val codGenerado = nom.replace(" ", "").take(5).uppercase() + "-" + (100..999).random()

            Toast.makeText(this@MainActivity, "Registrando en la nube...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("accion", "registrar_tienda")
                        put("codigoTienda", codGenerado)
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
                        .putString("codigo_tienda", codGenerado)
                        .putString("pin_admin", pin)
                        .putString("sheet_url_cliente", sheetUrl)
                        .putString("cajero_actual", "Cajero Principal")
                        .apply()

                    runOnUiThread {
                        paso1Layout.visibility = View.GONE
                        paso2Layout.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        prefs.edit()
                            .putString("nombre_tienda", nom)
                            .putString("correo_tienda", cor)
                            .putString("codigo_tienda", codGenerado)
                            .putString("pin_admin", pin)
                            .putString("cajero_actual", "Cajero Principal")
                            .apply()
                        paso1Layout.visibility = View.GONE
                        paso2Layout.visibility = View.VISIBLE
                    }
                }
            }
        }
        paso1Layout.addView(badge)
        paso1Layout.addView(t1)
        paso1Layout.addView(d1)
        paso1Layout.addView(inNombre)
        paso1Layout.addView(inCorreo)
        paso1Layout.addView(inPin)
        paso1Layout.addView(btnPaso1)

        // --- PASO 2: VINCULAR TELEGRAM ---
        paso2Layout = crearTarjeta(this).apply { visibility = View.GONE }
        val t2 = TextView(this).apply {
            text = "Paso 2: Alertas al Celular del Dueño 📲"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        val avisoTele = TextView(this).apply {
            text = "⚠️ REQUISITO:\nEl celular personal del dueño debe tener descargado Telegram para recibir las alertas cuando esté fuera."
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#991B1B"))
            background = crearBordeRedondo("#FEE2E2", "#FCA5A5", 14f)
            setPadding(20, 15, 20, 15)
        }
        val btnTele = crearBoton(this, "Vincular Telegram del Dueño (1 Toque)", "#0088CC") {
            val cod = prefs.getString("codigo_tienda", "TEST-01")
            val url = "https://t.me/$BOT_USERNAME?start=$cod"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        val btnPaso2 = crearBoton(this, "Ya lo vinculé, Siguiente ➡️", "#0F172A") {
            paso2Layout.visibility = View.GONE
            paso3Layout.visibility = View.VISIBLE
        }
        paso2Layout.addView(t2)
        paso2Layout.addView(avisoTele)
        paso2Layout.addView(btnTele)
        paso2Layout.addView(btnPaso2)

        // --- PASO 3: PERMISO DE YAPE Y FINALIZAR ---
        paso3Layout = crearTarjeta(this).apply { visibility = View.GONE }
        val t3 = TextView(this).apply {
            text = "Paso 3: Activar Lector en Caja 🛡️"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        val d3 = TextView(this).apply {
            text = "Es necesario para escuchar los pagos de Yape/Plin, cantarlos por voz y guardarlos en tu Excel:"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 0, 0, 15)
        }
        val btnPerm = crearBoton(this, "Activar Permiso de Notificaciones", "#475569") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        val btnFinalizar = crearBoton(this, "Completar Registro y Abrir Caja 🚀", "#00A859") {
            prefs.edit().putBoolean("onboarding_listo", true).apply()
            layoutWizard.visibility = View.GONE
            layoutPrincipal.visibility = View.VISIBLE
            cargarHistorialCajero()
        }
        paso3Layout.addView(t3)
        paso3Layout.addView(d3)
        paso3Layout.addView(btnPerm)
        paso3Layout.addView(btnFinalizar)

        layoutWizard.addView(paso1Layout)
        layoutWizard.addView(paso2Layout)
        layoutWizard.addView(paso3Layout)
        mainContainer.addView(layoutWizard)

        // =====================================================================
        // FASE B: SISTEMA OPERATIVO DIARIO (PUNTO DE VENTA + ADMIN DUEÑO)
        // =====================================================================
        layoutPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        // BARRA SUPERIOR
        val barraSup = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 15)
        }
        txtTurnoCajero = TextView(this).apply {
            text = "👤 Turno: " + prefs.getString("cajero_actual", "Cajero 1")
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchVoz = Switch(this).apply {
            text = "🔊 Voz "
            isChecked = prefs.getBoolean("voz_activa", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("voz_activa", isChecked).apply()
                Toast.makeText(this@MainActivity, if (isChecked) "Voz activada" else "Voz silenciada", Toast.LENGTH_SHORT).show()
            }
        }
        val btnAdmin = Button(this).apply {
            text = "⚙️ Admin"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = crearBordeRedondo("#0F172A", "#0F172A", 14f)
            setPadding(18, 10, 18, 10)
            setOnClickListener {
                if (layoutAdmin.visibility == View.VISIBLE) {
                    layoutAdmin.visibility = View.GONE
                    layoutCajero.visibility = View.VISIBLE
                } else {
                    pedirPinDueño(prefs) {
                        layoutCajero.visibility = View.GONE
                        layoutAdmin.visibility = View.VISIBLE
                        actualizarDatosAdmin(prefs)
                    }
                }
            }
        }
        barraSup.addView(txtTurnoCajero)
        barraSup.addView(switchVoz)
        barraSup.addView(btnAdmin)
        layoutPrincipal.addView(barraSup)

        // =================== MODO CAJERO (PUNTO DE VENTA) ===================
        layoutCajero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardPos = crearTarjeta(this)

        val titPos = TextView(this).apply {
            text = "Punto de Venta - Cobro Rápido"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        val inMonto = EditText(this).apply {
            hint = "S/ 0.00"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = crearBordeRedondo("#F8FAFC", "#CBD5E1", 16f)
            setPadding(20, 20, 20, 20)
        }
        imgQrGenerado = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(450, 450).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 15, 0, 10)
            }
            visibility = View.GONE
        }
        txtMontoCobroQr = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00A859"))
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val btnCobrarQr = crearBoton(this, "⚡ Generar QR para el Cliente", "#00A859") {
            val monto = inMonto.text.toString().trim()
            if (monto.isEmpty()) {
                Toast.makeText(this@MainActivity, "Digita el monto de la compra", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }
            val cod = prefs.getString("codigo_tienda", "TEST-01") ?: "TEST-01"
            val payload = "YAPE://PAY?MERCHANT=$cod&AMOUNT=$monto&CURRENCY=PEN"
            val qr = generarCodigoQr(payload, 450, 450)
            if (qr != null) {
                imgQrGenerado.setImageBitmap(qr)
                imgQrGenerado.visibility = View.VISIBLE
                txtMontoCobroQr.text = "Muestra al cliente: S/ $monto"
                txtMontoCobroQr.visibility = View.VISIBLE
            }
        }
        cardPos.addView(titPos)
        cardPos.addView(inMonto)
        cardPos.addView(btnCobrarQr)
        cardPos.addView(imgQrGenerado)
        cardPos.addView(txtMontoCobroQr)

        val cardFeed = crearTarjeta(this)
        val titFeed = TextView(this).apply {
            text = "📋 Cobros Recibidos en este Turno:"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        layoutListaCobrosCajero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cardFeed.addView(titFeed)
        cardFeed.addView(layoutListaCobrosCajero)

        layoutCajero.addView(cardPos)
        layoutCajero.addView(cardFeed)
        layoutPrincipal.addView(layoutCajero)

        // =================== MODO ADMIN (SOLO DUEÑO CON PIN) ===================
        layoutAdmin = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val cardAdmin = crearTarjeta(this)

        val titAdm = TextView(this).apply {
            text = "Panel de Administración (Dueño) 👑"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 12)
        }

        // TARJETA DE PRUEBA ÚNICA (DESAPARECE TRAS USARSE)
        cardPruebaUnica = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#EFF6FF", "#BFDBFE", 18f)
            setPadding(20, 18, 20, 18)
        }
        val tp = TextView(this).apply {
            text = "🧪 PRUEBA DE CONEXIÓN (1 SOLA VEZ)"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E40AF"))
        }
        val dp = TextView(this).apply {
            text = "Sirve para comprobar que tu caja se vinculó con Telegram y tu hoja de cálculo. Tras probar, desaparecerá:"
            textSize = 11f
            setTextColor(Color.parseColor("#3B82F6"))
            setPadding(0, 4, 0, 10)
        }
        val btnTestUnico = crearBoton(this, "🚀 Realizar Cobro de Prueba (S/ 1.00)", "#2563EB") {
            val cod = prefs.getString("codigo_tienda", "TEST-01") ?: "TEST-01"
            CoroutineScope(Dispatchers.IO).launch {
                val json = """{"codigoTienda": "$cod", "nombre": "COBRO VERIFICADO", "monto": "1.00"}"""
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                try { client.newCall(req).execute().close() } catch (e: Exception) {}
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "✅ ¡Conexión verificada con éxito!", Toast.LENGTH_LONG).show()
                    prefs.edit().putBoolean("prueba_realizada", true).apply()
                    cardPruebaUnica.visibility = View.GONE
                }
            }
        }
        cardPruebaUnica.addView(tp)
        cardPruebaUnica.addView(dp)
        cardPruebaUnica.addView(btnTestUnico)

        // TOTAL DEL DÍA
        val cardTot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#ECFDF5", "#A7F3D0", 16f)
            setPadding(20, 18, 20, 18)
        }
        val lblT = TextView(this).apply {
            text = "VENTAS TOTALES DE HOY"
            textSize = 11f
            setTextColor(Color.parseColor("#047857"))
            setTypeface(null, Typeface.BOLD)
        }
        txtTotalHoy = TextView(this).apply {
            text = "S/ 0.00"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#065F46"))
        }
        cardTot.addView(lblT)
        cardTot.addView(txtTotalHoy)

        val btnVerSheet = crearBoton(this, "📊 Abrir mi Hoja de Google Sheets", "#00A859") {
            val url = prefs.getString("sheet_url_cliente", "") ?: ""
            if (url.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(this@MainActivity, "Revisa el enlace enviado a tu Telegram", Toast.LENGTH_LONG).show()
            }
        }

        // ADMINISTRAR TURNOS Y CAJEROS
        val btnCambiarTurno = crearBoton(this, "👥 Cambiar Cajero / Turno de Trabajo", "#0F172A") {
            dialogCambiarCajero(prefs)
        }

        // COMPARTIR CON CAJERO (SOLO LECTURA)
        val inCorreoEmpleado = crearInput(this, "Correo Gmail del cajero (Acceso Solo Lectura)")
        val btnCompartirEmp = crearBoton(this, "🔒 Dar Acceso Solo Lectura al Cajero", "#15803D") {
            val cor = inCorreoEmpleado.text.toString().trim()
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            if (cor.isEmpty() || !cor.contains("@")) {
                Toast.makeText(this@MainActivity, "Ingresa un Gmail válido", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("accion", "compartir_sheet")
                        put("codigoTienda", cod)
                        put("correoCompartir", cor)
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    val res = client.newCall(req).execute()
                    val resJson = JSONObject(res.body?.string() ?: "")
                    res.close()
                    runOnUiThread {
                        if (resJson.optString("status") == "exito") {
                            Toast.makeText(this@MainActivity, "✅ Acceso concedido a $cor", Toast.LENGTH_LONG).show()
                            inCorreoEmpleado.setText("")
                        } else {
                            Toast.makeText(this@MainActivity, "Error: ${resJson.optString("mensaje")}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        val btnResetTienda = Button(this).apply {
            text = "⚠️ Desvincular y Registrar Nueva Tienda"
            setTextColor(Color.RED)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 12f
            setPadding(0, 30, 0, 10)
            setOnClickListener {
                prefs.edit().clear().apply()
                Toast.makeText(this@MainActivity, "Datos borrados. Iniciando registro nuevo...", Toast.LENGTH_SHORT).show()
                layoutPrincipal.visibility = View.GONE
                paso1Layout.visibility = View.VISIBLE
                paso2Layout.visibility = View.GONE
                paso3Layout.visibility = View.GONE
                layoutWizard.visibility = View.VISIBLE
            }
        }

        cardAdmin.addView(titAdm)
        cardAdmin.addView(cardPruebaUnica)
        cardAdmin.addView(cardTot)
        cardAdmin.addView(btnVerSheet)
        cardAdmin.addView(btnCambiarTurno)
        cardAdmin.addView(inCorreoEmpleado)
        cardAdmin.addView(btnCompartirEmp)
        cardAdmin.addView(btnResetTienda)
        layoutAdmin.addView(cardAdmin)
        layoutPrincipal.addView(layoutAdmin)

        mainContainer.addView(layoutPrincipal)

        setContentView(scroll)

        // REVISAR SI YA ESTÁ REGISTRADA
        val yaRegistrada = prefs.getBoolean("onboarding_listo", false)
        if (yaRegistrada) {
            layoutWizard.visibility = View.GONE
            layoutPrincipal.visibility = View.VISIBLE
            if (prefs.getBoolean("prueba_realizada", false)) {
                cardPruebaUnica.visibility = View.GONE
            }
            cargarHistorialCajero()
        } else {
            layoutWizard.visibility = View.VISIBLE
            layoutPrincipal.visibility = View.GONE
        }
    }

    // ================= FUNCIONES AUXILIARES =================
    private fun pedirPinDueño(prefs: android.content.SharedPreferences, onCorrecto: () -> Unit) {
        val inPin = EditText(this).apply {
            hint = "Ingresa PIN de 4 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }
        AlertDialog.Builder(this)
            .setTitle("Acceso de Administración")
            .setMessage("Solo el dueño del negocio puede acceder:")
            .setView(inPin)
            .setPositiveButton("Entrar") { _, _ ->
                val ingresado = inPin.text.toString().trim()
                val guardado = prefs.getString("pin_admin", "1234")
                if (ingresado == guardado) {
                    onCorrecto()
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogCambiarCajero(prefs: android.content.SharedPreferences) {
        val inTurno = EditText(this).apply {
            hint = "Ej: Turno Tarde - Ana"
            setText(prefs.getString("cajero_actual", ""))
        }
        AlertDialog.Builder(this)
            .setTitle("Asignar Cajero / Turno")
            .setView(inTurno)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoTurno = inTurno.text.toString().trim()
                if (nuevoTurno.isNotEmpty()) {
                    prefs.edit().putString("cajero_actual", nuevoTurno).apply()
                    txtTurnoCajero.text = "👤 Turno: $nuevoTurno"
                    Toast.makeText(this, "Turno actualizado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarDatosAdmin(prefs: android.content.SharedPreferences) {
        if (prefs.getBoolean("prueba_realizada", false)) {
            cardPruebaUnica.visibility = View.GONE
        }
    }

    private fun generarCodigoQr(contenido: String, ancho: Int, alto: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, ancho, alto)
            val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.RGB_565)
            for (x in 0 until ancho) {
                for (y in 0 until alto) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    private fun cargarHistorialCajero() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val historialStr = prefs.getString("historial_cobros", "[]") ?: "[]"
        val array = JSONArray(historialStr)

        layoutListaCobrosCajero.removeAllViews()
        var sumaTotal = 0.0
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        if (array.length() == 0) {
            val txtVacio = TextView(this).apply {
                text = "Esperando el primer cobro del turno..."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 30)
            }
            layoutListaCobrosCajero.addView(txtVacio)
        } else {
            for (i in 0 until minOf(array.length(), 15)) {
                val item = array.getJSONObject(i)
                val nombre = item.optString("nombre", "Cliente")
                val montoStr = item.optString("monto", "0.00")
                val hora = item.optString("hora", "")
                val fecha = item.optString("fecha", "")

                if (fecha == fechaHoy) {
                    sumaTotal += montoStr.toDoubleOrNull() ?: 0.0
                }

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(20, 16, 20, 16)
                    background = crearBordeRedondo("#FFFFFF", "#E2E8F0", 14f)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 10) }
                }

                val colTexto = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                colTexto.addView(TextView(this).apply {
                    text = nombre
                    setTypeface(null, Typeface.BOLD)
                    textSize = 13f
                    setTextColor(Color.parseColor("#0F172A"))
                })
                colTexto.addView(TextView(this).apply {
                    text = hora
                    textSize = 11f
                    setTextColor(Color.GRAY)
                })

                val txtMonto = TextView(this).apply {
                    text = "+ S/ $montoStr"
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#00A859"))
                }

                row.addView(colTexto)
                row.addView(txtMonto)
                layoutListaCobrosCajero.addView(row)
            }
        }

        txtTotalHoy.text = String.format(Locale.US, "S/ %.2f", sumaTotal)
    }

    override fun onResume() {
        super.onResume()
        if (getSharedPreferences("YapePrefs", Context.MODE_PRIVATE).getBoolean("onboarding_listo", false)) {
            cargarHistorialCajero()
        }
    }

    private fun crearTarjeta(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 35, 35, 35)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#E2E8F0"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
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
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(colorHex))
                cornerRadius = 16f
            }
            setPadding(20, 18, 20, 18)
            isAllCaps = false
            setOnClickListener { accion() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
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
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#CBD5E1"))
            }
            setPadding(30, 22, 30, 22)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 6, 0, 14) }
        }
    }
}
