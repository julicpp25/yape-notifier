package com.notiyape.app

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    // TU NUEVA URL OFICIAL (noticobros.peru@gmail.com)
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbw0_0UBYuP2yVjPHOmMDyLlehzvT41nuJPPCOnRw2Zd1V1Y57xI2nHUyVI1nSBhR7F_3w/exec"
    private val NUMERO_SOPORTE_WSP = "51999999999" // Pon tu número de WhatsApp aquí
    private val REQ_GOOGLE_PICKER = 1001

    private val client = OkHttpClient()
    private var motorVozCajero: TextToSpeech? = null
    private var vozLista = false

    // VISTAS PRINCIPALES
    private lateinit var layoutRolSelector: LinearLayout
    private lateinit var layoutRegistroDueño: LinearLayout
    private lateinit var layoutLoginCajero: LinearLayout
    private lateinit var layoutAppPrincipal: LinearLayout

    // CONTENEDORES DE LAS 3 PESTAÑAS
    private lateinit var tabCajaPOS: LinearLayout
    private lateinit var tabCajeros: LinearLayout
    private lateinit var tabConfiguracion: LinearLayout

    // BOTONES DE NAVEGACIÓN
    private lateinit var btnTabCaja: Button
    private lateinit var btnTabCajeros: Button
    private lateinit var btnTabConfig: Button

    // ELEMENTOS CAJA / POS
    private lateinit var txtInfoCajero: TextView
    private lateinit var inMontoPOS: EditText
    private lateinit var imgQrPOS: ImageView
    private lateinit var txtMontoQrPOS: TextView
    private lateinit var switchVozCaja: Switch
    private lateinit var txtTotalHoyCaja: TextView
    private lateinit var layoutListaCobrosCaja: LinearLayout
    private var ultimoIdCobroConocido = -1
    private var jobSincronizador: Job? = null

    // ELEMENTOS CONFIGURACIÓN
    private lateinit var inCorreoRegistro: EditText
    private lateinit var txtBannerCodigoConfig: TextView
    private lateinit var txtBannerNombreConfig: TextView
    private lateinit var txtSemaforoLector: TextView
    private lateinit var layoutListaCajerosAdmin: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        try {
            motorVozCajero = TextToSpeech(this, this, "com.google.android.tts")
        } catch (e: Exception) {
            motorVozCajero = TextToSpeech(this, this)
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 35, 30, 35)
        }
        scroll.addView(mainContainer)

        // =========================================================================
        // PANTALLA 1: SELECTOR DE ROL
        // =========================================================================
        layoutRolSelector = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardRol = crearTarjeta(this)

        val titRol = TextView(this).apply {
            text = "NotiCobros Perú 🇵🇪"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 6)
        }
        val descRol = TextView(this).apply {
            text = "Cobros por voz, reporte contable y alertas en tiempo real para Yape y Plin."
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 0, 0, 20)
        }

        val btnSoyDueño = crearBoton(this, "👑 Soy el Dueño (Administrar Negocio)", "#0F172A") {
            if (prefs.getBoolean("tienda_registrada", false)) {
                pedirPinDueño(prefs) { mostrarPantallaPrincipal(esDueño = true) }
            } else {
                mostrarVista(2)
            }
        }

        val btnSoyCajero = crearBoton(this, "🛒 Soy Cajero (Entrar al Punto de Venta)", "#00A859") {
            mostrarVista(3)
        }

        cardRol.addView(titRol)
        cardRol.addView(descRol)
        cardRol.addView(btnSoyDueño)
        cardRol.addView(btnSoyCajero)
        layoutRolSelector.addView(cardRol)

        // =========================================================================
        // PANTALLA 2: REGISTRO DEL DUEÑO (Con Selector de Google)
        // =========================================================================
        layoutRegistroDueño = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardReg = crearTarjeta(this)

        val titReg = TextView(this).apply {
            text = "Registro de Tienda Nueva 👑"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 15)
        }

        val inNom = crearInput(this, "Nombre del Negocio (Ej: Bodega Central)")

        inCorreoRegistro = crearInput(this, "Correo Gmail del Dueño")
        val btnGooglePicker = crearBoton(this, "G Seleccionar mi Correo de Google", "#4285F4") {
            try {
                val intent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                startActivityForResult(intent, REQ_GOOGLE_PICKER)
            } catch (e: Exception) {
                Toast.makeText(this, "Escribe tu correo manualmente", Toast.LENGTH_SHORT).show()
            }
        }

        val inPin = crearInput(this, "Crea tu PIN de Dueño (4 dígitos)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val inQrPayload = crearInput(this, "Texto de tu QR de Yape (000201...)")

        val btnCrearTienda = crearBoton(this, "Guardar Tienda y Activar NotiCobros 🚀", "#00A859") {
            val nom = inNom.text.toString().trim()
            val cor = inCorreoRegistro.text.toString().trim()
            val pin = inPin.text.toString().trim()
            val qrRaw = inQrPayload.text.toString().trim()

            if (nom.isEmpty() || cor.isEmpty() || pin.length < 4) {
                Toast.makeText(this, "Completa todos los campos (PIN mín. 4 números)", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            val codGenerado = nom.replace(" ", "").take(5).uppercase() + "-" + (100..999).random()
            Toast.makeText(this, "Registrando en noticobros.peru@gmail.com...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                var sheetUrl = ""
                try {
                    val json = JSONObject().apply {
                        put("accion", "registrar_tienda")
                        put("codigoTienda", codGenerado)
                        put("nombreNegocio", nom)
                        put("correo", cor)
                        put("pinDueño", pin)
                        put("qrBase", qrRaw)
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    val res = client.newCall(req).execute()
                    val resJson = JSONObject(res.body?.string() ?: "")
                    res.close()
                    sheetUrl = resJson.optString("sheetUrl", "")
                } catch (e: Exception) {}

                val topicLimpio = "tienda_" + codGenerado.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
                FirebaseMessaging.getInstance().subscribeToTopic(topicLimpio)

                prefs.edit()
                    .putString("nombre_tienda", nom)
                    .putString("correo_tienda", cor)
                    .putString("codigo_tienda", codGenerado)
                    .putString("pin_dueño", pin)
                    .putString("qr_base_yape", qrRaw)
                    .putString("sheet_url_cliente", sheetUrl)
                    .putBoolean("tienda_registrada", true)
                    .apply()

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "✅ ¡Tienda creada! Correo enviado desde noticobros.peru@gmail.com", Toast.LENGTH_LONG).show()
                    mostrarPantallaPrincipal(esDueño = true)
                }
            }
        }

        cardReg.addView(titReg)
        cardReg.addView(inNom)
        cardReg.addView(btnGooglePicker)
        cardReg.addView(inCorreoRegistro)
        cardReg.addView(inPin)
        cardReg.addView(inQrPayload)
        cardReg.addView(btnCrearTienda)
        layoutRegistroDueño.addView(cardReg)

        // =========================================================================
        // PANTALLA 3: LOGIN DEL CAJERO
        // =========================================================================
        layoutLoginCajero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardLogin = crearTarjeta(this)

        val titLog = TextView(this).apply {
            text = "Acceso para Cajeros 🛒"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 15)
        }

        val inCodTienda = crearInput(this, "Código de Tienda (Ej: BODEG-123)").apply {
            setText(prefs.getString("codigo_tienda", ""))
        }
        val inNomCajero = crearInput(this, "Nombre del Cajero (Ej: Carlos)")
        val inPinCajero = crearInput(this, "PIN de Acceso").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val btnEntrarCaja = crearBoton(this, "Iniciar Turno de Caja 🚀", "#00A859") {
            val cod = inCodTienda.text.toString().trim()
            val caj = inNomCajero.text.toString().trim()
            val pin = inPinCajero.text.toString().trim()

            if (cod.isEmpty() || caj.isEmpty() || pin.isEmpty()) {
                Toast.makeText(this, "Completa todos los datos", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            Toast.makeText(this, "Validando con el negocio...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("accion", "login_cajero")
                        put("codigoTienda", cod)
                        put("nombreCajero", caj)
                        put("pinCajero", pin)
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    val res = client.newCall(req).execute()
                    val resJson = JSONObject(res.body?.string() ?: "")
                    res.close()

                    if (resJson.optString("status") == "exito") {
                        val nomNeg = resJson.optString("nombreNegocio", cod)
                        val qrDescargado = resJson.optString("qrBase", "")

                        val topicLimpio = "tienda_" + cod.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
                        FirebaseMessaging.getInstance().subscribeToTopic(topicLimpio)

                        prefs.edit()
                            .putString("codigo_tienda", cod)
                            .putString("nombre_tienda", nomNeg)
                            .putString("cajero_activo", caj)
                            .putString("qr_base_yape", qrDescargado)
                            .putBoolean("cajero_logueado", true)
                            .apply()

                        runOnUiThread {
                            mostrarPantallaPrincipal(esDueño = false)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error: ${resJson.optString("mensaje")}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val btnVolverLogin = Button(this).apply {
            text = "⬅️ Volver"
            setOnClickListener { mostrarVista(1) }
        }

        cardLogin.addView(titLog)
        cardLogin.addView(inCodTienda)
        cardLogin.addView(inNomCajero)
        cardLogin.addView(inPinCajero)
        cardLogin.addView(btnEntrarCaja)
        cardLogin.addView(btnVolverLogin)
        layoutLoginCajero.addView(cardLogin)

        // =========================================================================
        // PANTALLA 4: APLICACIÓN PRINCIPAL (3 PESTAÑAS)
        // =========================================================================
        layoutAppPrincipal = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val barraPestañas = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 15)
        }

        btnTabCaja = Button(this).apply {
            text = "🛒 Caja / POS"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { seleccionarPestaña(1) }
        }

        btnTabCajeros = Button(this).apply {
            text = "👥 Cajeros"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { seleccionarPestaña(2) }
        }

        btnTabConfig = Button(this).apply {
            text = "⚙️ Config"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { seleccionarPestaña(3) }
        }

        barraPestañas.addView(btnTabCaja)
        barraPestañas.addView(btnTabCajeros)
        barraPestañas.addView(btnTabConfig)
        layoutAppPrincipal.addView(barraPestañas)

        // PESTAÑA 1: CAJA / POS
        tabCajaPOS = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val barraSubCaja = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 10)
        }
        txtInfoCajero = TextView(this).apply {
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchVozCaja = Switch(this).apply {
            text = "🔊 Voz "
            isChecked = prefs.getBoolean("voz_activa", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("voz_activa", isChecked).apply()
            }
        }
        barraSubCaja.addView(txtInfoCajero)
        barraSubCaja.addView(switchVozCaja)

        val cardTotal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#ECFDF5", "#A7F3D0", 18f)
            setPadding(25, 18, 25, 18)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 15)
            }
        }
        val barraTotalHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val lblT = TextView(this).apply {
            text = "VENTAS TOTALES DE HOY"
            textSize = 11f
            setTextColor(Color.parseColor("#047857"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnRefreshCaja = Button(this).apply {
            text = "🔄 Actualizar"
            textSize = 11f
            setTextColor(Color.parseColor("#065F46"))
            background = crearBordeRedondo("#D1FAE5", "#6EE7B7", 12f)
            setPadding(15, 8, 15, 8)
            setOnClickListener {
                Toast.makeText(this@MainActivity, "Actualizando saldo...", Toast.LENGTH_SHORT).show()
                consultarCobrosServidorUnaVez()
            }
        }
        barraTotalHeader.addView(lblT)
        barraTotalHeader.addView(btnRefreshCaja)

        txtTotalHoyCaja = TextView(this).apply {
            text = "S/ 0.00"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#065F46"))
            setPadding(0, 6, 0, 0)
        }
        cardTotal.addView(barraTotalHeader)
        cardTotal.addView(txtTotalHoyCaja)

        val cardCobroPOS = crearTarjeta(this)
        inMontoPOS = EditText(this).apply {
            hint = "S/ 0.00"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = crearBordeRedondo("#F8FAFC", "#CBD5E1", 16f)
            setPadding(20, 20, 20, 20)
        }
        imgQrPOS = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(500, 500).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 15, 0, 10)
            }
            visibility = View.GONE
        }
        txtMontoQrPOS = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00A859"))
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val btnGenerarQr = crearBoton(this, "⚡ Mostrar QR de Cobro", "#00A859") {
            val monto = inMontoPOS.text.toString().trim()
            if (monto.isEmpty()) {
                Toast.makeText(this, "Ingresa un monto primero", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }
            val qrBase = prefs.getString("qr_base_yape", "") ?: ""
            if (qrBase.isEmpty()) {
                Toast.makeText(this, "Configura tu QR de Yape en Configuración", Toast.LENGTH_LONG).show()
                return@crearBoton
            }
            val bmp = generarBitmapQr(qrBase, 500, 500)
            if (bmp != null) {
                imgQrPOS.setImageBitmap(bmp)
                imgQrPOS.visibility = View.VISIBLE
                txtMontoQrPOS.text = "Monto a pagar: S/ $monto"
                txtMontoQrPOS.visibility = View.VISIBLE
            }
        }
        cardCobroPOS.addView(inMontoPOS)
        cardCobroPOS.addView(btnGenerarQr)
        cardCobroPOS.addView(imgQrPOS)
        cardCobroPOS.addView(txtMontoQrPOS)

        val cardHistorialCaja = crearTarjeta(this)
        val titHist = TextView(this).apply {
            text = "📋 Cobros en Vivo:"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        layoutListaCobrosCaja = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cardHistorialCaja.addView(titHist)
        cardHistorialCaja.addView(layoutListaCobrosCaja)

        tabCajaPOS.addView(barraSubCaja)
        tabCajaPOS.addView(cardTotal)
        tabCajaPOS.addView(cardCobroPOS)
        tabCajaPOS.addView(cardHistorialCaja)

        // PESTAÑA 2: CAJEROS
        tabCajeros = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val cardCajeros = crearTarjeta(this)
        val titGestCaj = TextView(this).apply {
            text = "👥 Gestión de Cajeros (Otros Celulares):"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        val inNomCaj = crearInput(this, "Nombre del Cajero (Ej: María López)")
        val inPinCaj = crearInput(this, "PIN para este Cajero (4 dígitos)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val btnGuardarCaj = crearBoton(this, "➕ Crear Cajero Nuevo", "#0F172A") {
            val n = inNomCaj.text.toString().trim()
            val p = inPinCaj.text.toString().trim()
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            if (n.isNotEmpty() && p.length >= 4) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = JSONObject().apply {
                            put("accion", "crear_cajero")
                            put("codigoTienda", cod)
                            put("nombreCajero", n)
                            put("pinCajero", p)
                        }
                        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                        val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                        client.newCall(req).execute().close()
                    } catch (e: Exception) {}
                }
                agregarCajeroLocal(prefs, n, p)
                inNomCaj.setText("")
                inPinCaj.setText("")
                recargarListaCajeros(prefs)
                Toast.makeText(this, "Cajero $n creado con éxito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ingresa nombre y PIN de mín. 4 números", Toast.LENGTH_SHORT).show()
            }
        }
        layoutListaCajerosAdmin = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val inCorreoLectura = crearInput(this, "Correo Gmail del personal para Solo Lectura")
        val btnCompartirLectura = crearBoton(this, "🔒 Compartir Reporte en Solo Lectura", "#15803D") {
            val cor = inCorreoLectura.text.toString().trim()
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            if (cor.isEmpty() || !cor.contains("@")) {
                Toast.makeText(this, "Ingresa un Gmail válido", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@MainActivity, "Acceso concedido a $cor", Toast.LENGTH_LONG).show()
                            inCorreoLectura.setText("")
                        } else {
                            Toast.makeText(this@MainActivity, "Error: ${resJson.optString("mensaje")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        cardCajeros.addView(titGestCaj)
        cardCajeros.addView(inNomCaj)
        cardCajeros.addView(inPinCaj)
        cardCajeros.addView(btnGuardarCaj)
        cardCajeros.addView(layoutListaCajerosAdmin)
        cardCajeros.addView(inCorreoLectura)
        cardCajeros.addView(btnCompartirLectura)
        tabCajeros.addView(cardCajeros)

        // PESTAÑA 3: CONFIGURACIÓN
        tabConfiguracion = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val cardConfig = crearTarjeta(this)

        val cardCodTienda = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#EFF6FF", "#93C5FD", 18f)
            setPadding(25, 20, 25, 20)
        }
        txtBannerNombreConfig = TextView(this).apply {
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E3A8A"))
        }
        txtBannerCodigoConfig = TextView(this).apply {
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1D4ED8"))
            setPadding(0, 4, 0, 10)
        }
        val barraBtnsCod = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnCopiar = crearBoton(this, "📋 Copiar Código", "#1E40AF") {
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Codigo Tienda", cod))
            Toast.makeText(this, "Código copiado: $cod", Toast.LENGTH_SHORT).show()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }

        val btnWspCod = crearBoton(this, "📲 WhatsApp", "#059669") {
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            val nom = prefs.getString("nombre_tienda", "Mi Tienda")
            val mensaje = "Hola! Para entrar a caja de *$nom*, abre la app NotiCobros e ingresa el Código de Tienda: *$cod*"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(mensaje)))
            startActivity(intent)
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }

        barraBtnsCod.addView(btnCopiar)
        barraBtnsCod.addView(btnWspCod)
        cardCodTienda.addView(txtBannerNombreConfig)
        cardCodTienda.addView(txtBannerCodigoConfig)
        cardCodTienda.addView(barraBtnsCod)

        val cardSemaforo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#F1F5F9", "#CBD5E1", 16f)
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 15, 0, 15)
            }
        }
        txtSemaforoLector = TextView(this).apply {
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        cardSemaforo.addView(txtSemaforoLector)

        val btnBateria = crearBoton(this, "🔋 Desactivar Ahorro de Batería (1 Clic)", "#475569") {
            solicitarSinRestriccionesBateria()
        }

        val btnCambiarPin = crearBoton(this, "🔑 Cambiar mi PIN de Dueño", "#0F172A") {
            dialogCambiarPin(prefs)
        }

        val btnAbrirSheet = crearBoton(this, "📊 Abrir mi Google Sheets en vivo", "#00A859") {
            val url = prefs.getString("sheet_url_cliente", "") ?: ""
            if (url.isNotEmpty()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else Toast.makeText(this, "Revisa el enlace enviado a tu correo", Toast.LENGTH_SHORT).show()
        }

        val btnSoporte = crearBoton(this, "💬 Contactar a Soporte por WhatsApp", "#25D366") {
            val cod = prefs.getString("codigo_tienda", "")
            val nom = prefs.getString("nombre_tienda", "")
            val mensaje = "Hola Soporte NotiCobros, necesito ayuda con mi negocio $nom (Código: $cod)."
            val url = "https://api.whatsapp.com/send?phone=$NUMERO_SOPORTE_WSP&text=" + Uri.encode(mensaje)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        val btnCerrarSesion = Button(this).apply {
            text = "Cerrar Sesión / Desvincular"
            setTextColor(Color.parseColor("#DC2626"))
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 12f
            setPadding(0, 30, 0, 10)
            setOnClickListener {
                jobSincronizador?.cancel()
                prefs.edit().putBoolean("cajero_logueado", false).apply()
                mostrarVista(1)
            }
        }

        cardConfig.addView(cardCodTienda)
        cardConfig.addView(cardSemaforo)
        cardConfig.addView(btnBateria)
        cardConfig.addView(btnCambiarPin)
        cardConfig.addView(btnAbrirSheet)
        cardConfig.addView(btnSoporte)
        cardConfig.addView(btnCerrarSesion)
        tabConfiguracion.addView(cardConfig)

        layoutAppPrincipal.addView(tabCajaPOS)
        layoutAppPrincipal.addView(tabCajeros)
        layoutAppPrincipal.addView(tabConfiguracion)

        mainContainer.addView(layoutRolSelector)
        mainContainer.addView(layoutRegistroDueño)
        mainContainer.addView(layoutLoginCajero)
        mainContainer.addView(layoutAppPrincipal)

        setContentView(scroll)

        val codGuardado = prefs.getString("codigo_tienda", "") ?: ""
        if (codGuardado.isNotEmpty()) {
            val topicLimpio = "tienda_" + codGuardado.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
            FirebaseMessaging.getInstance().subscribeToTopic(topicLimpio)
        }

        if (prefs.getBoolean("cajero_logueado", false)) {
            mostrarPantallaPrincipal(esDueño = false)
        } else {
            mostrarVista(1)
        }
    }

    private fun mostrarVista(vista: Int) {
        layoutRolSelector.visibility = if (vista == 1) View.VISIBLE else View.GONE
        layoutRegistroDueño.visibility = if (vista == 2) View.VISIBLE else View.GONE
        layoutLoginCajero.visibility = if (vista == 3) View.VISIBLE else View.GONE
        layoutAppPrincipal.visibility = if (vista == 4) View.VISIBLE else View.GONE

        if (vista == 4) {
            seleccionarPestaña(1)
            iniciarSincronizador()
        } else {
            jobSincronizador?.cancel()
        }
    }

    private fun mostrarPantallaPrincipal(esDueño: Boolean) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val nom = prefs.getString("nombre_tienda", "Mi Tienda")
        val cod = prefs.getString("codigo_tienda", "")
        val caj = prefs.getString("cajero_activo", if (esDueño) "Dueño (Admin)" else "Cajero")

        txtInfoCajero.text = "🏪 $nom | 👤 $caj"
        txtBannerNombreConfig.text = "🏪 $nom"
        txtBannerCodigoConfig.text = "🔑 $cod"

        btnTabCajeros.visibility = if (esDueño) View.VISIBLE else View.GONE
        btnTabConfig.visibility = if (esDueño) View.VISIBLE else View.GONE

        mostrarVista(4)
        recargarListaCajeros(prefs)
    }

    private fun seleccionarPestaña(pestaña: Int) {
        tabCajaPOS.visibility = if (pestaña == 1) View.VISIBLE else View.GONE
        tabCajeros.visibility = if (pestaña == 2) View.VISIBLE else View.GONE
        tabConfiguracion.visibility = if (pestaña == 3) View.VISIBLE else View.GONE

        btnTabCaja.background = crearBordeRedondo(if (pestaña == 1) "#00A859" else "#F1F5F9", "#CBD5E1", 14f)
        btnTabCaja.setTextColor(if (pestaña == 1) Color.WHITE else Color.parseColor("#475569"))

        btnTabCajeros.background = crearBordeRedondo(if (pestaña == 2) "#00A859" else "#F1F5F9", "#CBD5E1", 14f)
        btnTabCajeros.setTextColor(if (pestaña == 2) Color.WHITE else Color.parseColor("#475569"))

        btnTabConfig.background = crearBordeRedondo(if (pestaña == 3) "#00A859" else "#F1F5F9", "#CBD5E1", 14f)
        btnTabConfig.setTextColor(if (pestaña == 3) Color.WHITE else Color.parseColor("#475569"))

        if (pestaña == 3) verificarSemaforoLector()
    }

    private fun verificarSemaforoLector() {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val tienePermiso = listeners.contains(packageName)

        if (tienePermiso) {
            txtSemaforoLector.text = "🟢 LECTOR DE YAPE/PLIN: ACTIVO"
            txtSemaforoLector.setTextColor(Color.parseColor("#065F46"))
        } else {
            txtSemaforoLector.text = "🔴 LECTOR APAGADO: Toca aquí para activarlo"
            txtSemaforoLector.setTextColor(Color.parseColor("#991B1B"))
            txtSemaforoLector.setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }

    private fun solicitarSinRestriccionesBateria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            } else {
                Toast.makeText(this, "✅ NotiCobros ya tiene permiso sin restricciones de batería.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dialogCambiarPin(prefs: android.content.SharedPreferences) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }
        val inActual = EditText(this).apply {
            hint = "PIN Actual (4 dígitos)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val inNuevo = EditText(this).apply {
            hint = "Nuevo PIN (4 dígitos)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        layout.addView(inActual)
        layout.addView(inNuevo)

        AlertDialog.Builder(this)
            .setTitle("Cambiar PIN de Dueño")
            .setView(layout)
            .setPositiveButton("Actualizar") { _, _ ->
                val actual = inActual.text.toString().trim()
                val nuevo = inNuevo.text.toString().trim()
                val pinGuardado = prefs.getString("pin_dueño", "1234")

                if (actual == pinGuardado && nuevo.length >= 4) {
                    prefs.edit().putString("pin_dueño", nuevo).apply()
                    Toast.makeText(this, "✅ PIN actualizado exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN actual incorrecto o nuevo inválido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_GOOGLE_PICKER && resultCode == RESULT_OK && data != null) {
            val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrEmpty()) {
                inCorreoRegistro.setText(accountName)
                Toast.makeText(this, "Correo seleccionado: $accountName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarSincronizador() {
        jobSincronizador?.cancel()
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val cod = prefs.getString("codigo_tienda", "") ?: ""

        jobSincronizador = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val url = "$SERVIDOR_URL?accion=consultar_cobros&codigoTienda=$cod"
                    val req = Request.Builder().url(url).get().build()
                    val res = client.newCall(req).execute()
                    val jsonStr = res.body?.string() ?: "[]"
                    res.close()
                    val array = JSONArray(jsonStr)

                    if (array.length() > 0) {
                        val ultimoCobro = array.getJSONObject(0)
                        val idCobro = ultimoCobro.optInt("id", -1)
                        val monto = ultimoCobro.optString("monto", "0.00")
                        val nombre = ultimoCobro.optString("nombre", "Cliente")

                        if (ultimoIdCobroConocido == -1) {
                            ultimoIdCobroConocido = idCobro
                        } else if (idCobro > ultimoIdCobroConocido) {
                            ultimoIdCobroConocido = idCobro

                            withContext(Dispatchers.Main) {
                                if (prefs.getBoolean("voz_activa", true) && vozLista) {
                                    val frase = if (monto == "1" || monto == "1.00") "un sol" else "$monto soles"
                                    motorVozCajero?.speak("¡Te llegó un cobro de $frase! ¡A seguir sumando!", TextToSpeech.QUEUE_FLUSH, null, "TTS_CAJA")
                                }
                                imgQrPOS.visibility = View.GONE
                                txtMontoQrPOS.visibility = View.GONE
                                Toast.makeText(this@MainActivity, "✅ ¡Cobro confirmado! S/ $monto de $nombre", Toast.LENGTH_LONG).show()
                            }
                        }

                        withContext(Dispatchers.Main) {
                            actualizarListaCaja(array)
                        }
                    }
                } catch (e: Exception) {}
                delay(2500)
            }
        }
    }

    private fun consultarCobrosServidorUnaVez() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val cod = prefs.getString("codigo_tienda", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "$SERVIDOR_URL?accion=consultar_cobros&codigoTienda=$cod"
                val req = Request.Builder().url(url).get().build()
                val res = client.newCall(req).execute()
                val jsonStr = res.body?.string() ?: "[]"
                res.close()
                val array = JSONArray(jsonStr)

                withContext(Dispatchers.Main) {
                    actualizarListaCaja(array)
                    Toast.makeText(this@MainActivity, "Saldo y lista actualizados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {}
        }
    }

    private fun actualizarListaCaja(array: JSONArray) {
        layoutListaCobrosCaja.removeAllViews()
        var sumaTotal = 0.0
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        for (i in 0 until minOf(array.length(), 15)) {
            val item = array.getJSONObject(i)
            val mStr = item.optString("monto", "0.00")
            val fStr = item.optString("fecha", "")

            if (fStr == fechaHoy) {
                sumaTotal += mStr.toDoubleOrNull() ?: 0.0
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(15, 12, 15, 12)
                background = crearBordeRedondo("#FFFFFF", "#E2E8F0", 14f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 8)
                }
            }
            val txtInfo = TextView(this).apply {
                text = "${item.optString("nombre")} (${item.optString("hora")})"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val txtM = TextView(this).apply {
                text = "+ S/ $mStr"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#00A859"))
            }
            row.addView(txtInfo)
            row.addView(txtM)
            layoutListaCobrosCaja.addView(row)
        }
        txtTotalHoyCaja.text = String.format(Locale.US, "S/ %.2f", sumaTotal)
    }

    private fun pedirPinDueño(prefs: android.content.SharedPreferences, onOk: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }
        val inP = EditText(this).apply {
            hint = "Ingresa tu PIN de 4 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }
        val btnOlvido = Button(this).apply {
            text = "¿Olvidaste tu PIN? Recupéralo por correo"
            textSize = 12f
            setTextColor(Color.parseColor("#2563EB"))
            setBackgroundColor(Color.TRANSPARENT)
        }
        layout.addView(inP)
        layout.addView(btnOlvido)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Acceso Administrador 👑")
            .setView(layout)
            .setPositiveButton("Ingresar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (inP.text.toString().trim() == prefs.getString("pin_dueño", "1234")) {
                    dialog.dismiss()
                    onOk()
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnOlvido.setOnClickListener {
            dialog.dismiss()
            solicitarRecuperacionPin(prefs)
        }

        dialog.show()
    }

    private fun solicitarRecuperacionPin(prefs: android.content.SharedPreferences) {
        val cod = prefs.getString("codigo_tienda", "") ?: ""
        val cor = prefs.getString("correo_tienda", "tu correo registrado")

        AlertDialog.Builder(this)
            .setTitle("Recuperar PIN")
            .setMessage("¿Deseas enviar tu PIN de administrador a tu correo:\n\n$cor?")
            .setPositiveButton("Enviar Correo") { _, _ ->
                Toast.makeText(this, "Enviando PIN a tu correo...", Toast.LENGTH_SHORT).show()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = JSONObject().apply {
                            put("accion", "recuperar_pin")
                            put("codigoTienda", cod)
                        }
                        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                        val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                        val res = client.newCall(req).execute()
                        val resJson = JSONObject(res.body?.string() ?: "")
                        res.close()
                        runOnUiThread {
                            if (resJson.optString("status") == "exito") {
                                Toast.makeText(this@MainActivity, "✅ ¡PIN enviado a tu correo! Revisa tu bandeja.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Error: ${resJson.optString("mensaje")}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarCajeroLocal(prefs: android.content.SharedPreferences, n: String, p: String) {
        val arr = JSONArray(prefs.getString("lista_cajeros", "[]") ?: "[]")
        arr.put(JSONObject().apply { put("nombre", n); put("pin", p) })
        prefs.edit().putString("lista_cajeros", arr.toString()).apply()
    }

    private fun recargarListaCajeros(prefs: android.content.SharedPreferences) {
        layoutListaCajerosAdmin.removeAllViews()
        val arr = JSONArray(prefs.getString("lista_cajeros", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            layoutListaCajerosAdmin.addView(TextView(this).apply {
                text = "• ${c.getString("nombre")} (PIN: ${c.getString("pin")})"
                textSize = 13f
                setTextColor(Color.parseColor("#334155"))
                setPadding(0, 4, 0, 4)
            })
        }
    }

    private fun generarBitmapQr(contenido: String, ancho: Int, alto: Int): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, ancho, alto)
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            motorVozCajero?.language = Locale("es", "PE")
            motorVozCajero?.setPitch(1.04f)
            motorVozCajero?.setSpeechRate(1.02f)
            vozLista = true
        }
    }

    override fun onDestroy() {
        jobSincronizador?.cancel()
        motorVozCajero?.stop()
        motorVozCajero?.shutdown()
        super.onDestroy()
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
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 20)
            }
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
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }
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
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 6, 0, 14)
            }
        }
    }
}
