package com.notiyape.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
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

    private val BOT_USERNAME = "alertasaudioyape_pe_bot"
    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbyOiEdKFVc08ti51lu4Q4UT2qAadmiR3D6jIjUxul8coZHtTQNHgUvO-0LzUuu11ewOFQ/exec"
    private val client = OkHttpClient()

    // MOTOR DE VOZ EN EL CELULAR DEL CAJERO
    private var motorVozCajero: TextToSpeech? = null
    private var vozLista = false

    // VISTAS
    private lateinit var layoutRolSelector: LinearLayout
    private lateinit var layoutRegistroDueño: LinearLayout
    private lateinit var layoutLoginCajero: LinearLayout
    private lateinit var layoutCajeroPos: LinearLayout
    private lateinit var layoutAdminPanel: LinearLayout

    // CAJERO
    private lateinit var bannerPermisoCajero: LinearLayout
    private lateinit var txtEstadoPermisoCajero: TextView
    private lateinit var txtInfoCajero: TextView
    private lateinit var imgQr: ImageView
    private lateinit var txtMontoQr: TextView
    private lateinit var switchVozCajero: Switch
    private lateinit var layoutListaCobrosCajero: LinearLayout
    private var ultimoIdCobroConocido = -1
    private var jobSincronizadorCajero: Job? = null

    // ADMIN
    private lateinit var txtBannerCodigo: TextView
    private lateinit var txtBannerNombre: TextView
    private lateinit var txtTotalHoyAdmin: TextView
    private lateinit var layoutHistorialAdmin: LinearLayout
    private lateinit var layoutListaCajerosAdmin: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        // Inicializar voz en este celular
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
            setPadding(35, 45, 35, 45)
        }
        scroll.addView(mainContainer)

        // =========================================================================
        // PANTALLA 1: SELECTOR DE ROL
        // =========================================================================
        layoutRolSelector = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardRol = crearTarjeta(this)

        val titRol = TextView(this).apply {
            text = "NotiCobros Perú 🇵🇪"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 8)
        }
        val descRol = TextView(this).apply {
            text = "Selecciona cómo vas a utilizar este celular:"
            textSize = 14f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 0, 0, 25)
        }

        val btnSoyDueño = crearBoton(this, "👑 Soy el Dueño (Admin)", "#0F172A") {
            if (prefs.getBoolean("tienda_registrada", false)) {
                pedirPinDueño(prefs) { mostrarVista(5) }
            } else {
                mostrarVista(2)
            }
        }

        val btnSoyCajero = crearBoton(this, "🛒 Soy Cajero (Punto de Venta)", "#00A859") {
            mostrarVista(3)
        }

        cardRol.addView(titRol)
        cardRol.addView(descRol)
        cardRol.addView(btnSoyDueño)
        cardRol.addView(btnSoyCajero)
        layoutRolSelector.addView(cardRol)

        // =========================================================================
        // PANTALLA 2: REGISTRO DEL DUEÑO
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
        val inCor = crearInput(this, "Correo Gmail del Dueño")
        val inPin = crearInput(this, "Crea tu PIN de Dueño (4 dígitos)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val inQrPayload = crearInput(this, "Texto de tu QR de Yape (000201...)")

        val btnCrearTienda = crearBoton(this, "Guardar Tienda y Crear Excel 🚀", "#00A859") {
            val nom = inNom.text.toString().trim()
            val cor = inCor.text.toString().trim()
            val pin = inPin.text.toString().trim()
            val qrRaw = inQrPayload.text.toString().trim()

            if (nom.isEmpty() || cor.isEmpty() || pin.length < 4) {
                Toast.makeText(this, "Completa todos los campos (PIN mín. 4 números)", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            val codGenerado = nom.replace(" ", "").take(5).uppercase() + "-" + (100..999).random()
            Toast.makeText(this, "Registrando tienda y creando Excel...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                var sheetUrl = ""
                try {
                    val json = JSONObject().apply {
                        put("accion", "registrar_tienda")
                        put("codigoTienda", codGenerado)
                        put("nombreNegocio", nom)
                        put("correo", cor)
                        put("qrBase", qrRaw)
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    val res = client.newCall(req).execute()
                    val resJson = JSONObject(res.body?.string() ?: "")
                    res.close()
                    sheetUrl = resJson.optString("sheetUrl", "")
                } catch (e: Exception) {}

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
                    val urlTelegram = "https://t.me/$BOT_USERNAME?start=$codGenerado"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlTelegram)))
                    Toast.makeText(this@MainActivity, "Tienda registrada: $codGenerado", Toast.LENGTH_LONG).show()
                    mostrarVista(5)
                }
            }
        }

        cardReg.addView(titReg)
        cardReg.addView(inNom)
        cardReg.addView(inCor)
        cardReg.addView(inPin)
        cardReg.addView(inQrPayload)
        cardReg.addView(btnCrearTienda)
        layoutRegistroDueño.addView(cardReg)

        // =========================================================================
        // PANTALLA 3: LOGIN DEL CAJERO (EN OTRO CELULAR)
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

        val inCodTienda = crearInput(this, "Código de Tienda (Pídeselo al Dueño)").apply {
            setText(prefs.getString("codigo_tienda", ""))
        }
        val inNomCajero = crearInput(this, "Nombre del Cajero (Ej: Carlos)")
        val inPinCajero = crearInput(this, "PIN de Acceso").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val btnEntrarCaja = crearBoton(this, "Entrar a Caja 🚀", "#00A859") {
            val cod = inCodTienda.text.toString().trim()
            val caj = inNomCajero.text.toString().trim()
            val pin = inPinCajero.text.toString().trim()

            if (cod.isEmpty() || caj.isEmpty() || pin.isEmpty()) {
                Toast.makeText(this, "Completa todos los datos", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            Toast.makeText(this, "Validando con la tienda...", Toast.LENGTH_SHORT).show()

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

                        prefs.edit()
                            .putString("codigo_tienda", cod)
                            .putString("nombre_tienda", nomNeg)
                            .putString("cajero_activo", caj)
                            .putString("qr_base_yape", qrDescargado)
                            .putBoolean("cajero_logueado", true)
                            .apply()

                        runOnUiThread {
                            txtInfoCajero.text = "👤 $caj | Tienda: $nomNeg"
                            mostrarVista(4)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error: ${resJson.optString("mensaje")}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val btnVolverDeLogin = Button(this).apply {
            text = "⬅️ Volver"
            setOnClickListener { mostrarVista(1) }
        }

        cardLogin.addView(titLog)
        cardLogin.addView(inCodTienda)
        cardLogin.addView(inNomCajero)
        cardLogin.addView(inPinCajero)
        cardLogin.addView(btnEntrarCaja)
        cardLogin.addView(btnVolverDeLogin)
        layoutLoginCajero.addView(cardLogin)

        // =========================================================================
        // PANTALLA 4: PUNTO DE VENTA (CAJERO: COBRO CON QR + VOZ EN VIVO)
        // =========================================================================
        layoutCajeroPos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        bannerPermisoCajero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 18, 25, 18)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 15) }
        }
        txtEstadoPermisoCajero = TextView(this).apply {
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        bannerPermisoCajero.addView(txtEstadoPermisoCajero)

        val barraCaja = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 15)
        }
        txtInfoCajero = TextView(this).apply {
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchVozCajero = Switch(this).apply {
            text = "🔊 Voz "
            isChecked = prefs.getBoolean("voz_activa", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("voz_activa", isChecked).apply()
            }
        }
        val btnSalirCajero = Button(this).apply {
            text = "Cerrar Turno"
            textSize = 10f
            setTextColor(Color.WHITE)
            background = crearBordeRedondo("#64748B", "#64748B", 12f)
            setPadding(15, 8, 15, 8)
            setOnClickListener {
                jobSincronizadorCajero?.cancel()
                prefs.edit().putBoolean("cajero_logueado", false).apply()
                mostrarVista(1)
            }
        }
        barraCaja.addView(txtInfoCajero)
        barraCaja.addView(switchVozCajero)
        barraCaja.addView(btnSalirCajero)

        val cardCobro = crearTarjeta(this)
        val inMonto = EditText(this).apply {
            hint = "S/ 0.00"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = crearBordeRedondo("#F8FAFC", "#CBD5E1", 16f)
            setPadding(20, 20, 20, 20)
        }
        imgQr = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(500, 500).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 15, 0, 10)
            }
            visibility = View.GONE
        }
        txtMontoQr = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00A859"))
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        val btnGenerarQr = crearBoton(this, "⚡ Generar QR para el Cliente", "#00A859") {
            val monto = inMonto.text.toString().trim()
            if (monto.isEmpty()) {
                Toast.makeText(this, "Ingresa un monto primero", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            val qrBase = prefs.getString("qr_base_yape", "") ?: ""
            if (qrBase.isEmpty()) {
                Toast.makeText(this, "El dueño debe configurar el QR en Admin", Toast.LENGTH_LONG).show()
                return@crearBoton
            }

            val payloadFinal = armarQrEmvcoConMonto(qrBase, monto)
            val bmp = generarBitmapQr(payloadFinal, 500, 500)
            if (bmp != null) {
                imgQr.setImageBitmap(bmp)
                imgQr.visibility = View.VISIBLE
                txtMontoQr.text = "Escanea con Yape/Plin: S/ $monto"
                txtMontoQr.visibility = View.VISIBLE
            }
        }

        cardCobro.addView(inMonto)
        cardCobro.addView(btnGenerarQr)
        cardCobro.addView(imgQr)
        cardCobro.addView(txtMontoQr)

        val cardRec = crearTarjeta(this)
        val titRec = TextView(this).apply {
            text = "📋 Cobros Recibidos en este Turno:"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        layoutListaCobrosCajero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cardRec.addView(titRec)
        cardRec.addView(layoutListaCobrosCajero)

        layoutCajeroPos.addView(bannerPermisoCajero)
        layoutCajeroPos.addView(barraCaja)
        layoutCajeroPos.addView(cardCobro)
        layoutCajeroPos.addView(cardRec)

        // =========================================================================
        // PANTALLA 5: PANEL ADMINISTRADOR (DUEÑO)
        // =========================================================================
        layoutAdminPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardAdm = crearTarjeta(this)

        val cardCodigoDestacado = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#EFF6FF", "#93C5FD", 20f)
            setPadding(25, 20, 25, 20)
        }
        txtBannerNombre = TextView(this).apply {
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E3A8A"))
        }
        txtBannerCodigo = TextView(this).apply {
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1D4ED8"))
            setPadding(0, 4, 0, 10)
        }
        val descComparte = TextView(this).apply {
            text = "Código de Tienda para que tus cajeros inicien sesión en sus celulares:"
            textSize = 12f
            setTextColor(Color.parseColor("#3B82F6"))
            setPadding(0, 0, 0, 12)
        }

        val barraBotonesCodigo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnCopiarCod = crearBoton(this, "📋 Copiar", "#1E40AF") {
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Codigo Tienda", cod))
            Toast.makeText(this, "Código copiado: $cod", Toast.LENGTH_SHORT).show()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }

        val btnCompartirWsp = crearBoton(this, "📲 WhatsApp", "#059669") {
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            val nom = prefs.getString("nombre_tienda", "Mi Tienda")
            val mensaje = "Hola! Para entrar al turno de caja de *$nom*, abre la app NotiCobros e ingresa el Código de Tienda: *$cod*"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(mensaje)))
            startActivity(intent)
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }

        barraBotonesCodigo.addView(btnCopiarCod)
        barraBotonesCodigo.addView(btnCompartirWsp)

        cardCodigoDestacado.addView(txtBannerNombre)
        cardCodigoDestacado.addView(txtBannerCodigo)
        cardCodigoDestacado.addView(descComparte)
        cardCodigoDestacado.addView(barraBotonesCodigo)

        // TARJETA VENTAS HOY (CONSULTA DENTRO DE LA APP)
        val cardTotal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#ECFDF5", "#A7F3D0", 18f)
            setPadding(25, 20, 25, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 15, 0, 15) }
        }
        val lblT = TextView(this).apply {
            text = "VENTAS TOTALES DE HOY"
            textSize = 11f
            setTextColor(Color.parseColor("#047857"))
            setTypeface(null, Typeface.BOLD)
        }
        txtTotalHoyAdmin = TextView(this).apply {
            text = "S/ 0.00"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#065F46"))
        }
        cardTotal.addView(lblT)
        cardTotal.addView(txtTotalHoyAdmin)

        val btnVerSheet = crearBoton(this, "📊 Abrir mi Hoja de Google Sheets en vivo", "#00A859") {
            val url = prefs.getString("sheet_url_cliente", "") ?: ""
            if (url.isNotEmpty()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else Toast.makeText(this, "Revisa el enlace en tu Telegram", Toast.LENGTH_SHORT).show()
        }

        // COMPARTIR EN SOLO LECTURA
        val inCorreoLectura = crearInput(this, "Correo Gmail para darle Solo Lectura")
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
                            Toast.makeText(this@MainActivity, "Acceso de lectura concedido a $cor", Toast.LENGTH_LONG).show()
                            inCorreoLectura.setText("")
                        } else {
                            Toast.makeText(this@MainActivity, "Error: ${resJson.optString("mensaje")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        // GESTIÓN DE CAJEROS
        val titGestCaj = TextView(this).apply {
            text = "\n👥 Cajeros Autorizados (Otros Celulares):"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 8, 0, 8)
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

        // CONSULTA DE VENTAS DIRECTAMENTE EN LA APP
        val titHistAdmin = TextView(this).apply {
            text = "\n📋 Cobros del Negocio en Tiempo Real:"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 10, 0, 8)
        }
        layoutHistorialAdmin = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val btnSalirAdmin = Button(this).apply {
            text = "⬅️ Volver al Inicio"
            setOnClickListener { mostrarVista(1) }
        }

        cardAdm.addView(cardCodigoDestacado)
        cardAdm.addView(cardTotal)
        cardAdm.addView(btnVerSheet)
        cardAdm.addView(inCorreoLectura)
        cardAdm.addView(btnCompartirLectura)
        cardAdm.addView(titGestCaj)
        cardAdm.addView(inNomCaj)
        cardAdm.addView(inPinCaj)
        cardAdm.addView(btnGuardarCaj)
        cardAdm.addView(layoutListaCajerosAdmin)
        cardAdm.addView(titHistAdmin)
        cardAdm.addView(layoutHistorialAdmin)
        cardAdm.addView(btnSalirAdmin)
        layoutAdminPanel.addView(cardAdm)

        // AGREGAR VISTAS
        mainContainer.addView(layoutRolSelector)
        mainContainer.addView(layoutRegistroDueño)
        mainContainer.addView(layoutLoginCajero)
        mainContainer.addView(layoutCajeroPos)
        mainContainer.addView(layoutAdminPanel)

        setContentView(scroll)

        if (prefs.getBoolean("cajero_logueado", false)) {
            txtInfoCajero.text = "👤 " + prefs.getString("cajero_activo", "") + " | Tienda: " + prefs.getString("codigo_tienda", "")
            mostrarVista(4)
        } else {
            mostrarVista(1)
        }
    }

    private fun mostrarVista(vista: Int) {
        layoutRolSelector.visibility = if (vista == 1) View.VISIBLE else View.GONE
        layoutRegistroDueño.visibility = if (vista == 2) View.VISIBLE else View.GONE
        layoutLoginCajero.visibility = if (vista == 3) View.VISIBLE else View.GONE
        layoutCajeroPos.visibility = if (vista == 4) View.VISIBLE else View.GONE
        layoutAdminPanel.visibility = if (vista == 5) View.VISIBLE else View.GONE

        if (vista == 4) {
            verificarPermisoLector()
            iniciarSincronizadorCajero()
        } else {
            jobSincronizadorCajero?.cancel()
        }

        if (vista == 5) {
            val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
            txtBannerNombre.text = "🏪 " + prefs.getString("nombre_tienda", "Mi Tienda")
            txtBannerCodigo.text = "🔑 " + prefs.getString("codigo_tienda", "NO ASIGNADO")
            recargarListaCajeros(prefs)
            cargarConsultaAdmin()
        }
    }

    // =========================================================================
    // SINCRONIZADOR EN VIVO (EL CELULAR DEL CAJERO ESCUCHA A LA NUBE Y HABLA)
    // =========================================================================
    private fun iniciarSincronizadorCajero() {
        jobSincronizadorCajero?.cancel()
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val cod = prefs.getString("codigo_tienda", "") ?: ""

        jobSincronizadorCajero = CoroutineScope(Dispatchers.IO).launch {
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

                        // Si es la primera vez que sincroniza, fija el último ID sin cantar
                        if (ultimoIdCobroConocido == -1) {
                            ultimoIdCobroConocido = idCobro
                        } else if (idCobro > ultimoIdCobroConocido) {
                            // ¡PAGO NUEVO DETECTADO EN LA NUBE!
                            ultimoIdCobroConocido = idCobro

                            withContext(Dispatchers.Main) {
                                // 1. HABLAR EN VOZ ALTA EN ESTE CELULAR DE CAJA
                                if (prefs.getBoolean("voz_activa", true) && vozLista) {
                                    val frase = if (monto == "1" || monto == "1.00") "un sol" else "$monto soles"
                                    motorVozCajero?.speak("¡Te llegó un Yape de $frase! ¡A seguir sumando!", TextToSpeech.QUEUE_FLUSH, null, "TTS_CAJA")
                                }

                                // 2. Ocultar el QR generado
                                imgQr.visibility = View.GONE
                                txtMontoQr.visibility = View.GONE

                                Toast.makeText(this@MainActivity, "✅ ¡Cobro Confirmado! S/ $monto de $nombre", Toast.LENGTH_LONG).show()
                            }
                        }

                        // Actualizar lista en pantalla
                        withContext(Dispatchers.Main) {
                            actualizarListaCajero(array)
                        }
                    }
                } catch (e: Exception) {}

                delay(2500) // Consulta cada 2.5 segundos
            }
        }
    }

    private fun actualizarListaCajero(array: JSONArray) {
        layoutListaCobrosCajero.removeAllViews()
        for (i in 0 until minOf(array.length(), 10)) {
            val item = array.getJSONObject(i)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(15, 12, 15, 12)
                background = crearBordeRedondo("#FFFFFF", "#E2E8F0", 14f)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
            }
            val txtInfo = TextView(this).apply {
                text = "${item.optString("nombre")} (${item.optString("hora")})"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val txtMonto = TextView(this).apply {
                text = "+ S/ ${item.optString("monto")}"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#00A859"))
            }
            row.addView(txtInfo)
            row.addView(txtMonto)
            layoutListaCobrosCajero.addView(row)
        }
    }

    private fun cargarConsultaAdmin() {
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

                var total = 0.0
                val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                withContext(Dispatchers.Main) {
                    layoutHistorialAdmin.removeAllViews()
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val mStr = item.optString("monto", "0.00")
                        val fStr = item.optString("fecha", "")

                        if (fStr == fechaHoy) {
                            total += mStr.toDoubleOrNull() ?: 0.0
                        }

                        val row = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(15, 10, 15, 10)
                        }
                        val tInfo = TextView(this@MainActivity).apply {
                            text = "${item.optString("nombre")} (${item.optString("hora")})"
                            textSize = 13f
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        val tM = TextView(this@MainActivity).apply {
                            text = "+ S/ $mStr"
                            textSize = 14f
                            setTypeface(null, Typeface.BOLD)
                            setTextColor(Color.parseColor("#00A859"))
                        }
                        row.addView(tInfo)
                        row.addView(tM)
                        layoutHistorialAdmin.addView(row)
                    }
                    txtTotalHoyAdmin.text = String.format(Locale.US, "S/ %.2f", total)
                }
            } catch (e: Exception) {}
        }
    }

    private fun verificarPermisoLector() {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val tienePermiso = listeners.contains(packageName)

        if (tienePermiso) {
            bannerPermisoCajero.background = crearBordeRedondo("#ECFDF5", "#A7F3D0", 14f)
            txtEstadoPermisoCajero.text = "🟢 LECTOR DE YAPE ACTIVO Y ESCUCHANDO"
            txtEstadoPermisoCajero.setTextColor(Color.parseColor("#065F46"))
            bannerPermisoCajero.setOnClickListener(null)
        } else {
            bannerPermisoCajero.background = crearBordeRedondo("#FEF2F2", "#FCA5A5", 14f)
            txtEstadoPermisoCajero.text = "🔴 ALERTA: Permiso de Yape apagado\nToca aquí para activarlo en Android"
            txtEstadoPermisoCajero.setTextColor(Color.parseColor("#991B1B"))
            bannerPermisoCajero.setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }

    private fun pedirPinDueño(prefs: android.content.SharedPreferences, onOk: () -> Unit) {
        val inP = EditText(this).apply {
            hint = "PIN de Dueño"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }
        AlertDialog.Builder(this)
            .setTitle("Acceso Administrador")
            .setMessage("Ingresa tu PIN de 4 dígitos:")
            .setView(inP)
            .setPositiveButton("Ingresar") { _, _ ->
                if (inP.text.toString().trim() == prefs.getString("pin_dueño", "1234")) onOk()
                else Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
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

    // =========================================================================
    // MOTOR EMVCO DINÁMICO (YAPE / PLIN)
    // =========================================================================
    private fun armarQrEmvcoConMonto(qrBase: String, montoStr: String): String {
        return try {
            val montoNum = montoStr.replace(",", ".").toDoubleOrNull() ?: return qrBase
            val montoFormateado = String.format(Locale.US, "%.2f", montoNum)
            val tag54 = "54" + String.format(Locale.US, "%02d", montoFormateado.length) + montoFormateado

            if (qrBase.startsWith("000201")) {
                var base = qrBase
                val idxCrc = base.lastIndexOf("6304")
                if (idxCrc != -1) {
                    base = base.substring(0, idxCrc)
                }

                if (base.contains("010211")) {
                    base = base.replaceFirst("010211", "010212")
                }

                base = base.replace(Regex("54\\d{2}[0-9.]+"), "")

                val payloadPreCrc = base + tag54 + "6304"
                val nuevoCrc = calcularCrc16Oficial(payloadPreCrc)
                payloadPreCrc + nuevoCrc
            } else {
                qrBase
            }
        } catch (e: Exception) {
            qrBase
        }
    }

    private fun calcularCrc16Oficial(str: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = str.toByteArray(Charsets.UTF_8)
        for (b in bytes) {
            val byteVal = b.toInt() and 0xFF
            for (i in 0 until 8) {
                val bit = ((byteVal shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = (crc shl 1) and 0xFFFF
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return String.format(Locale.US, "%04X", crc and 0xFFFF)
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
        jobSincronizadorCajero?.cancel()
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
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 6, 0, 14) }
        }
    }
}
