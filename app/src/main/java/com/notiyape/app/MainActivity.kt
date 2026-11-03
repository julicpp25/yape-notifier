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
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbw0_0UBYuP2yVjPHOmMDyLlehzvT41nuJPPCOnRw2Zd1V1Y57xI2nHUyVI1nSBhR7F_3w/exec"
    private val NUMERO_SOPORTE_WSP = "51999999999" // Cambia a tu número de WhatsApp
    private val REQ_GOOGLE_PICKER_REGISTRO = 1001
    private val REQ_GOOGLE_PICKER_LOGIN = 1002

    // PALETA ESTILO YAPE
    private val COLOR_YAPE_PURPLE = "#732483"
    private val COLOR_YAPE_CYAN = "#00D2B4"

    private val client = OkHttpClient()
    private var motorVozCajero: TextToSpeech? = null
    private var vozLista = false

    // BARRA DE AVISO DE INTERNET
    private lateinit var bannerSinInternet: TextView

    // PANTALLAS
    private lateinit var rootLayout: FrameLayout
    private lateinit var layoutPortada: LinearLayout
    private lateinit var layoutRegistroDueño: LinearLayout
    private lateinit var layoutLoginDueño: LinearLayout
    private lateinit var layoutLoginCajero: LinearLayout
    private lateinit var layoutBienvenida: LinearLayout

    // ASISTENTE DE PERMISOS
    private lateinit var layoutPermiso1Lector: LinearLayout
    private lateinit var layoutPermiso2Alertas: LinearLayout
    private lateinit var layoutPermiso3Bateria: LinearLayout

    // CONTENEDOR PRINCIPAL Y PESTAÑAS
    private lateinit var layoutAppPrincipal: RelativeLayout
    private lateinit var scrollPrincipal: ScrollView
    private lateinit var tabCajaPOS: LinearLayout
    private lateinit var tabCajeros: LinearLayout
    private lateinit var tabConfiguracion: LinearLayout
    private lateinit var barraNavegacionInferior: LinearLayout
    private lateinit var btnNavCaja: Button
    private lateinit var btnNavCajeros: Button
    private lateinit var btnNavConfig: Button

    // ELEMENTOS CAJA / POS
    private lateinit var txtInfoCajero: TextView
    private lateinit var inMontoPOS: EditText
    private lateinit var imgQrPOS: ImageView
    private lateinit var txtMontoQrPOS: TextView
    private lateinit var switchVozCaja: Switch
    private lateinit var txtTotalHoyCaja: TextView
    private lateinit var btnOcultarSaldo: Button
    private lateinit var layoutListaCobrosCaja: LinearLayout
    private var ultimoIdCobroConocido = -1
    private var jobSincronizador: Job? = null

    // ESTADO DE PRIVACIDAD Y TOTALES
    private var saldoOculto = false
    private var totalVentasHoyNumero = 0.0
    private var cantidadVentasHoy = 0
    private var esModoOscuro = false

    // ELEMENTOS DIAGNÓSTICO EN AJUSTES
    private lateinit var txtEstadoLectorDiag: TextView
    private lateinit var txtEstadoBateriaDiag: TextView
    private lateinit var txtEstadoNotifDiag: TextView
    private lateinit var txtBannerCodigoCajeros: TextView
    private lateinit var inCorreoRegistro: EditText
    private lateinit var inCorreoLoginDueño: EditText
    private lateinit var inQrConfigTabCajeros: EditText
    private lateinit var layoutListaCajerosAdmin: LinearLayout

    private var esSesionDueño = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        esModoOscuro = prefs.getBoolean("modo_oscuro", false)

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

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(if (esModoOscuro) "#0F172A" else "#F8FAFC"))
        }

        bannerSinInternet = TextView(this).apply {
            text = "⚠️ Sin conexión a internet. Los cobros se actualizarán al reconectar."
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#78350F"))
            setBackgroundColor(Color.parseColor("#FEF3C7"))
            gravity = Gravity.CENTER
            setPadding(20, 12, 20, 12)
            visibility = View.GONE
        }

        val scrollOnboarding = ScrollView(this)
        val containerOnboarding = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 30, 35, 40)
        }
        scrollOnboarding.addView(containerOnboarding)

        // =========================================================================
        // PANTALLA 1: PORTADA OFICIAL (YAPENEGOCIOSVOZ)
        // =========================================================================
        layoutPortada = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardPortada = crearTarjetaFintech(this)

        val imgBannerPortada = ImageView(this).apply {
            setImageResource(R.drawable.logo_horizontal)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 10, 0, 25)
            }
        }

        val btnDueño = crearBotonFintech(this, "👑 SOY EL DUEÑO\n(Administrador de Negocio)", COLOR_YAPE_PURPLE) {
            if (prefs.getBoolean("tienda_registrada", false)) {
                pedirPinDueño(prefs) {
                    esSesionDueño = true
                    verificarAsistentePermisosOEntrar()
                }
            } else {
                mostrarOpcionesDueñoDialog()
            }
        }.apply { textSize = 16f }

        val btnCajeroPequeno = crearBotonFintech(this, "🛒 Soy Cajero (Entrar al Punto de Venta) - Clic aquí", COLOR_YAPE_CYAN) {
            mostrarSeccion(layoutLoginCajero)
        }.apply {
            textSize = 14f
            setTextColor(Color.parseColor("#0F172A"))
        }

        cardPortada.addView(imgBannerPortada)
        cardPortada.addView(btnDueño)
        cardPortada.addView(btnCajeroPequeno)
        layoutPortada.addView(cardPortada)

        // =========================================================================
        // PANTALLA 2: REGISTRO DE DUEÑO
        // =========================================================================
        layoutRegistroDueño = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardReg = crearTarjetaFintech(this)

        val titReg = TextView(this).apply {
            text = "Registro de Nuevo Negocio 👑"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 15)
        }

        val inNom = crearInputFintech(this, "Nombre de tu Negocio (Ej: Bodega Central)")
        inCorreoRegistro = crearInputFintech(this, "Correo Gmail del Dueño")

        val btnGooglePickerReg = crearBotonFintech(this, "G Seleccionar mi Correo de Google", "#4285F4") {
            try {
                val intent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                startActivityForResult(intent, REQ_GOOGLE_PICKER_REGISTRO)
            } catch (e: Exception) {
                Toast.makeText(this, "Escribe tu correo manualmente", Toast.LENGTH_SHORT).show()
            }
        }

        val inPin1 = crearInputFintech(this, "Crea tu PIN de Dueño (4 dígitos)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val inPin2 = crearInputFintech(this, "Confirmar PIN de Dueño (4 dígitos)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val btnCrearTienda = crearBotonFintech(this, "Crear mi Negocio 🚀", COLOR_YAPE_PURPLE) {
            val nom = inNom.text.toString().trim()
            val cor = inCorreoRegistro.text.toString().trim()
            val p1 = inPin1.text.toString().trim()
            val p2 = inPin2.text.toString().trim()

            if (nom.isEmpty() || cor.isEmpty() || p1.length < 4) {
                Toast.makeText(this, "Completa todos los campos (PIN mín. 4 números)", Toast.LENGTH_SHORT).show()
                return@crearBotonFintech
            }

            if (p1 != p2) {
                Toast.makeText(this, "❌ Los dos PIN no coinciden", Toast.LENGTH_SHORT).show()
                return@crearBotonFintech
            }

            val codGenerado = nom.replace(" ", "").take(5).uppercase() + "-" + (100..999).random()
            Toast.makeText(this, "Validando registro...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("accion", "registrar_tienda")
                        put("codigoTienda", codGenerado)
                        put("nombreNegocio", nom)
                        put("correo", cor)
                        put("pinDueño", p1)
                        put("qrBase", "")
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    val res = client.newCall(req).execute()
                    val resJson = JSONObject(res.body?.string() ?: "")
                    res.close()

                    if (resJson.optString("status") == "exito") {
                        val sheetUrl = resJson.optString("sheetUrl", "")
                        val topicLimpio = "tienda_" + codGenerado.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
                        FirebaseMessaging.getInstance().subscribeToTopic(topicLimpio)

                        prefs.edit()
                            .putString("nombre_tienda", nom)
                            .putString("correo_tienda", cor)
                            .putString("codigo_tienda", codGenerado)
                            .putString("pin_dueño", p1)
                            .putString("sheet_url_cliente", sheetUrl)
                            .putBoolean("tienda_registrada", true)
                            .apply()

                        runOnUiThread {
                            esSesionDueño = true
                            mostrarSeccion(layoutBienvenida)
                        }
                    } else {
                        runOnUiThread {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Aviso de Registro")
                                .setMessage(resJson.optString("mensaje"))
                                .setPositiveButton("Iniciar Sesión") { _, _ ->
                                    inCorreoLoginDueño.setText(cor)
                                    mostrarSeccion(layoutLoginDueño)
                                }
                                .setNegativeButton("Cerrar", null)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val btnYaTengoCuentaLink = Button(this).apply {
            text = "¿Ya tienes un negocio registrado? Inicia sesión aquí"
            textSize = 14f
            setTextColor(Color.parseColor("#2563EB"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { mostrarSeccion(layoutLoginDueño) }
        }

        val btnVolverReg = Button(this).apply {
            text = "⬅️ Volver a la Portada"
            textSize = 14f
            setOnClickListener { mostrarSeccion(layoutPortada) }
        }

        cardReg.addView(titReg)
        cardReg.addView(inNom)
        cardReg.addView(btnGooglePickerReg)
        cardReg.addView(inCorreoRegistro)
        cardReg.addView(inPin1)
        cardReg.addView(inPin2)
        cardReg.addView(btnCrearTienda)
        cardReg.addView(btnYaTengoCuentaLink)
        cardReg.addView(btnVolverReg)
        layoutRegistroDueño.addView(cardReg)

        // =========================================================================
        // PANTALLA: LOGIN DE DUEÑO
        // =========================================================================
        layoutLoginDueño = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardLoginDueño = crearTarjetaFintech(this)

        val titLogDueño = TextView(this).apply {
            text = "Iniciar Sesión como Dueño 👑"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 15)
        }

        inCorreoLoginDueño = crearInputFintech(this, "Tu Correo Gmail Registrado")
        val btnGooglePickerLogin = crearBotonFintech(this, "G Seleccionar mi Correo de Google", "#4285F4") {
            try {
                val intent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                startActivityForResult(intent, REQ_GOOGLE_PICKER_LOGIN)
            } catch (e: Exception) {
                Toast.makeText(this, "Escribe tu correo manualmente", Toast.LENGTH_SHORT).show()
            }
        }

        val inPinLoginDueño = crearInputFintech(this, "Tu PIN de Dueño (4 dígitos)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val btnIngresarDueño = crearBotonFintech(this, "Ingresar a mi Negocio 🚀", COLOR_YAPE_PURPLE) {
            val cor = inCorreoLoginDueño.text.toString().trim()
            val pin = inPinLoginDueño.text.toString().trim()

            if (cor.isEmpty() || pin.isEmpty()) {
                Toast.makeText(this, "Completa tu correo y PIN", Toast.LENGTH_SHORT).show()
                return@crearBotonFintech
            }

            Toast.makeText(this, "Recuperando datos de tu negocio...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("accion", "login_dueño")
                        put("correo", cor)
                        put("pinDueño", pin)
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(SERVIDOR_URL).post(body).build()
                    val res = client.newCall(req).execute()
                    val resJson = JSONObject(res.body?.string() ?: "")
                    res.close()

                    if (resJson.optString("status") == "exito") {
                        val cod = resJson.getString("codigoTienda")
                        val nom = resJson.getString("nombreNegocio")
                        val sheetUrl = resJson.optString("sheetUrl", "")
                        val qrBase = resJson.optString("qrBase", "")
                        val cajeros = resJson.optString("cajeros", "[]")

                        val topicLimpio = "tienda_" + cod.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
                        FirebaseMessaging.getInstance().subscribeToTopic(topicLimpio)

                        prefs.edit()
                            .putString("codigo_tienda", cod)
                            .putString("nombre_tienda", nom)
                            .putString("correo_tienda", cor)
                            .putString("pin_dueño", pin)
                            .putString("sheet_url_cliente", sheetUrl)
                            .putString("qr_base_yape", qrBase)
                            .putString("lista_cajeros", cajeros)
                            .putBoolean("tienda_registrada", true)
                            .apply()

                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "✅ ¡Bienvenido de nuevo, $nom!", Toast.LENGTH_LONG).show()
                            esSesionDueño = true
                            verificarAsistentePermisosOEntrar()
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

        val btnOlvidoPinLogin = Button(this).apply {
            text = "¿Olvidaste tu PIN? Recupéralo por correo"
            textSize = 14f
            setTextColor(Color.parseColor("#2563EB"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                val cor = inCorreoLoginDueño.text.toString().trim()
                if (cor.isNotEmpty()) solicitarRecuperacionPinPorCorreo(cor)
                else Toast.makeText(this@MainActivity, "Ingresa tu correo arriba primero", Toast.LENGTH_SHORT).show()
            }
        }

        val btnVolverLoginDueño = Button(this).apply {
            text = "⬅️ Volver a la Portada"
            textSize = 14f
            setOnClickListener { mostrarSeccion(layoutPortada) }
        }

        cardLoginDueño.addView(titLogDueño)
        cardLoginDueño.addView(btnGooglePickerLogin)
        cardLoginDueño.addView(inCorreoLoginDueño)
        cardLoginDueño.addView(inPinLoginDueño)
        cardLoginDueño.addView(btnIngresarDueño)
        cardLoginDueño.addView(btnOlvidoPinLogin)
        cardLoginDueño.addView(btnVolverLoginDueño)
        layoutLoginDueño.addView(cardLoginDueño)

        // =========================================================================
        // PANTALLA: LOGIN DE CAJERO
        // =========================================================================
        layoutLoginCajero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardLogin = crearTarjetaFintech(this)

        val titLog = TextView(this).apply {
            text = "Acceso para Cajeros 🛒"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 15)
        }

        val inCodTienda = crearInputFintech(this, "Código de Tienda (Ej: BODEG-123)").apply {
            setText(prefs.getString("codigo_tienda", ""))
        }
        val inNomCajero = crearInputFintech(this, "Nombre del Cajero (Ej: Carlos)")
        val inPinCajero = crearInputFintech(this, "PIN de Acceso").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val btnEntrarCaja = crearBotonFintech(this, "Iniciar Turno de Caja 🚀", COLOR_YAPE_PURPLE) {
            val cod = inCodTienda.text.toString().trim()
            val caj = inNomCajero.text.toString().trim()
            val pin = inPinCajero.text.toString().trim()

            if (cod.isEmpty() || caj.isEmpty() || pin.isEmpty()) {
                Toast.makeText(this, "Completa todos los datos", Toast.LENGTH_SHORT).show()
                return@crearBotonFintech
            }

            Toast.makeText(this, "Validando turno...", Toast.LENGTH_SHORT).show()

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
                            esSesionDueño = false
                            verificarAsistentePermisosOEntrar()
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
            text = "⬅️ Volver a la Portada"
            textSize = 14f
            setOnClickListener { mostrarSeccion(layoutPortada) }
        }

        cardLogin.addView(titLog)
        cardLogin.addView(inCodTienda)
        cardLogin.addView(inNomCajero)
        cardLogin.addView(inPinCajero)
        cardLogin.addView(btnEntrarCaja)
        cardLogin.addView(btnVolverLogin)
        layoutLoginCajero.addView(cardLogin)

        // =========================================================================
        // PANTALLA 3: BIENVENIDA
        // =========================================================================
        layoutBienvenida = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardBienvenida = crearTarjetaFintech(this)

        val imgBannerBienvenida = ImageView(this).apply {
            setImageResource(R.drawable.logo_horizontal)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 20)
            }
        }

        val titBv = TextView(this).apply {
            text = "🎉 ¡Bienvenido a YAPENEGOCIOSVOZ!"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 10)
        }
        val fraseBv = TextView(this).apply {
            text = "“¡Tu Yape ahora habla y administra tu negocio!”"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            background = crearBordeRedondo("#F3E8F7", "#D8B4E2", 16f)
            setPadding(20, 16, 20, 16)
        }
        val descBv = TextView(this).apply {
            text = "\nConfiguraremos unos permisos rápidos para empezar a usar la app y garantizar que tus cobros se canten siempre a tiempo sin interrupciones."
            textSize = 14f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 0, 0, 25)
        }
        val btnIniciarAsistente = crearBotonFintech(this, "Comenzar Configuración ➡️", COLOR_YAPE_PURPLE) {
            mostrarSeccion(layoutPermiso1Lector)
        }

        cardBienvenida.addView(imgBannerBienvenida)
        cardBienvenida.addView(titBv)
        cardBienvenida.addView(fraseBv)
        cardBienvenida.addView(descBv)
        cardBienvenida.addView(btnIniciarAsistente)
        layoutBienvenida.addView(cardBienvenida)

        // =========================================================================
        // ASISTENTE DE PERMISOS (PASO 1, 2 Y 3 ESTANDARIZADOS)
        // =========================================================================
        
        // PASO 1
        layoutPermiso1Lector = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardP1 = crearTarjetaFintech(this)
        val titP1 = TextView(this).apply {
            text = "🔔 Paso 1 de 3: Lector de Notificaciones"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 10)
        }
        val txtClicActivar1 = TextView(this).apply {
            text = "👉 Clic para activar:"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 8, 0, 4)
        }
        val btnActivarP1 = crearBotonFintech(this, "ACTIVAR LECTOR DE NOTIFICACIONES", COLOR_YAPE_PURPLE) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }.apply { textSize = 15f }

        val btnAyudaRestringidos = Button(this).apply {
            text = "⚠️ ¿Te sale 'Ajuste restringido'? Toca aquí para desbloquear"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#B45309"))
            background = crearBordeRedondo("#FEF3C7", "#FDE68A", 14f)
            setPadding(20, 14, 20, 14)
            setOnClickListener { mostrarDialogoAjustesRestringidos() }
        }
        val btnSigP1 = crearBotonFintech(this, "Siguiente Paso ➡️", COLOR_YAPE_CYAN) {
            mostrarSeccion(layoutPermiso2Alertas)
        }.apply { setTextColor(Color.parseColor("#0F172A")) }

        val txtAdvertencia1 = TextView(this).apply {
            text = "⚠️ Es necesario activar este permiso para que la app funcione correctamente."
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#DC2626"))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }

        cardP1.addView(titP1)
        cardP1.addView(txtClicActivar1)
        cardP1.addView(btnActivarP1)
        cardP1.addView(btnAyudaRestringidos)
        cardP1.addView(btnSigP1)
        cardP1.addView(txtAdvertencia1)
        layoutPermiso1Lector.addView(cardP1)

        // PASO 2
        layoutPermiso2Alertas = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardP2 = crearTarjetaFintech(this)
        val titP2 = TextView(this).apply {
            text = "📲 Paso 2 de 3: Alertas en Pantalla"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 10)
        }
        val txtClicActivar2 = TextView(this).apply {
            text = "👉 Clic para activar:"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 8, 0, 4)
        }
        val btnActivarP2 = crearBotonFintech(this, "ACTIVAR NOTIFICACIONES", COLOR_YAPE_PURPLE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            } else {
                Toast.makeText(this, "Las notificaciones ya están activas en tu sistema.", Toast.LENGTH_SHORT).show()
            }
        }.apply { textSize = 15f }

        val btnSigP2 = crearBotonFintech(this, "Siguiente Paso ➡️", COLOR_YAPE_CYAN) {
            mostrarSeccion(layoutPermiso3Bateria)
        }.apply { setTextColor(Color.parseColor("#0F172A")) }

        val txtAdvertencia2 = TextView(this).apply {
            text = "⚠️ Es necesario activar este permiso para que la app funcione correctamente."
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#DC2626"))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }

        cardP2.addView(titP2)
        cardP2.addView(txtClicActivar2)
        cardP2.addView(btnActivarP2)
        cardP2.addView(btnSigP2)
        cardP2.addView(txtAdvertencia2)
        layoutPermiso2Alertas.addView(cardP2)

        // PASO 3
        layoutPermiso3Bateria = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardP3 = crearTarjetaFintech(this)
        val titP3 = TextView(this).apply {
            text = "🔋 Paso 3 de 3: Batería Sin Restricciones"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 10)
        }
        val txtClicActivar3 = TextView(this).apply {
            text = "👉 Clic para activar:"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 8, 0, 4)
        }
        val btnActivarP3 = crearBotonFintech(this, "OPTIMIZA BATERÍA PARA LA APP", COLOR_YAPE_PURPLE) {
            solicitarSinRestriccionesBateria()
        }.apply { textSize = 15f }

        val btnFinalizarWizard = crearBotonFintech(this, "Finalizar y Entrar a la App 🚀", COLOR_YAPE_CYAN) {
            prefs.edit().putBoolean("asistente_permisos_listo", true).apply()
            entrarDirectoAlSistema()
        }.apply { setTextColor(Color.parseColor("#0F172A")) }

        val txtAdvertencia3 = TextView(this).apply {
            text = "⚠️ Es necesario activar este permiso para que la app funcione correctamente."
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#DC2626"))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }

        cardP3.addView(titP3)
        cardP3.addView(txtClicActivar3)
        cardP3.addView(btnActivarP3)
        cardP3.addView(btnFinalizarWizard)
        cardP3.addView(txtAdvertencia3)
        layoutPermiso3Bateria.addView(cardP3)

        // =========================================================================
        // PANTALLA PRINCIPAL: 3 PESTAÑAS CON DOCK INFERIOR
        // =========================================================================
        layoutAppPrincipal = RelativeLayout(this).apply { visibility = View.GONE }

        scrollPrincipal = ScrollView(this).apply {
            setPadding(30, 10, 30, 180)
            clipToPadding = false
        }
        val containerTabs = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollPrincipal.addView(containerTabs)

        // --- PESTAÑA 1: CAJA / POS ---
        tabCajaPOS = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val barraSubCaja = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 12)
        }
        txtInfoCajero = TextView(this).apply {
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchVozCaja = Switch(this).apply {
            text = "🔊 Voz "
            textSize = 14f
            isChecked = prefs.getBoolean("voz_activa", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("voz_activa", isChecked).apply()
            }
        }
        val btnCerrarSesionCajero = Button(this).apply {
            text = "CERRAR SESIÓN"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = crearBordeRedondo("#DC2626", "#DC2626", 14f)
            setPadding(18, 10, 18, 10)
            setOnClickListener {
                jobSincronizador?.cancel()
                prefs.edit().putBoolean("cajero_logueado", false).apply()
                mostrarSeccion(layoutPortada)
            }
        }
        barraSubCaja.addView(txtInfoCajero)
        barraSubCaja.addView(switchVozCaja)
        barraSubCaja.addView(btnCerrarSesionCajero)

        val cardTotal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#F3E8F7", "#D8B4E2", 22f)
            setPadding(28, 22, 28, 22)
            elevation = 10f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 16)
            }
        }
        val barraTotalHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val lblT = TextView(this).apply {
            text = "VENTAS TOTALES DE HOY"
            textSize = 13f
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        btnOcultarSaldo = Button(this).apply {
            text = "👁️"
            textSize = 15f
            background = crearBordeRedondo("#FFFFFF", "#D8B4E2", 14f)
            setPadding(14, 8, 14, 8)
            setOnClickListener {
                saldoOculto = !saldoOculto
                actualizarVistaSaldo()
            }
        }
        val btnRefreshCaja = Button(this).apply {
            text = "🔄"
            textSize = 15f
            background = crearBordeRedondo("#FFFFFF", "#D8B4E2", 14f)
            setPadding(14, 8, 14, 8)
            setOnClickListener {
                Toast.makeText(this@MainActivity, "Actualizando saldo...", Toast.LENGTH_SHORT).show()
                consultarCobrosServidorUnaVez()
            }
        }
        barraTotalHeader.addView(lblT)
        barraTotalHeader.addView(btnOcultarSaldo)
        barraTotalHeader.addView(btnRefreshCaja)

        txtTotalHoyCaja = TextView(this).apply {
            text = "S/ 0.00"
            textSize = 34f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 8, 0, 0)
        }
        cardTotal.addView(barraTotalHeader)
        cardTotal.addView(txtTotalHoyCaja)

        val cardCobroPOS = crearTarjetaFintech(this)
        inMontoPOS = EditText(this).apply {
            hint = "S/ 0.00"
            textSize = 30f
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
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val btnGenerarQr = crearBotonFintech(this, "⚡ Mostrar QR de Cobro", COLOR_YAPE_PURPLE) {
            val monto = inMontoPOS.text.toString().trim()
            if (monto.isEmpty()) {
                Toast.makeText(this, "Ingresa un monto primero", Toast.LENGTH_SHORT).show()
                return@crearBotonFintech
            }
            val qrBase = prefs.getString("qr_base_yape", "") ?: ""
            if (qrBase.isEmpty()) {
                Toast.makeText(this, "El dueño debe configurar el QR en la pestaña Cajeros", Toast.LENGTH_LONG).show()
                return@crearBotonFintech
            }
            val bmp = generarBitmapQr(qrBase, 500, 500)
            if (bmp != null) {
                imgQrPOS.setImageBitmap(bmp)
                imgQrPOS.visibility = View.VISIBLE
                txtMontoQrPOS.text = "Monto a pagar: S/ $monto"
                txtMontoQrPOS.visibility = View.VISIBLE
            }
        }.apply { textSize = 16f }

        cardCobroPOS.addView(inMontoPOS)
        cardCobroPOS.addView(btnGenerarQr)
        cardCobroPOS.addView(imgQrPOS)
        cardCobroPOS.addView(txtMontoQrPOS)

        val cardHistorialCaja = crearTarjetaFintech(this)
        val titHist = TextView(this).apply {
            text = "📋 Cobros en Vivo:"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        layoutListaCobrosCaja = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val btnCompartirCuadreWsp = crearBotonFintech(this, "📲 Enviar Cuadre de Caja por WhatsApp", "#25D366") {
            enviarCuadrePorWhatsApp()
        }

        cardHistorialCaja.addView(titHist)
        cardHistorialCaja.addView(layoutListaCobrosCaja)
        cardHistorialCaja.addView(btnCompartirCuadreWsp)

        tabCajaPOS.addView(barraSubCaja)
        tabCajaPOS.addView(cardTotal)
        tabCajaPOS.addView(cardCobroPOS)
        tabCajaPOS.addView(cardHistorialCaja)

        // --- PESTAÑA 2: CAJEROS ---
        tabCajeros = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardCodCaja = crearTarjetaFintech(this)
        val titCodCaja = TextView(this).apply {
            text = "Configuración de Caja 🏪"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
        }
        txtBannerCodigoCajeros = TextView(this).apply {
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1D4ED8"))
            setPadding(0, 6, 0, 8)
        }
        val descCodCaja = TextView(this).apply {
            text = "Este código le servirá para agregar un punto de caja en otro celular:"
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 0, 0, 12)
        }
        val btnCopiarCodCaja = crearBotonFintech(this, "📋 Copiar Código de Tienda", COLOR_YAPE_PURPLE) {
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Codigo Tienda", cod))
            Toast.makeText(this, "Código copiado: $cod", Toast.LENGTH_SHORT).show()
        }
        cardCodCaja.addView(titCodCaja)
        cardCodCaja.addView(txtBannerCodigoCajeros)
        cardCodCaja.addView(descCodCaja)
        cardCodCaja.addView(btnCopiarCodCaja)
        tabCajeros.addView(cardCodCaja)

        val cardCajeros = crearTarjetaFintech(this)
        val titGestCaj = TextView(this).apply {
            text = "👥 Gestión de Cajeros (Otros Celulares):"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 10)
        }
        val inNomCaj = crearInputFintech(this, "Nombre del Cajero (Ej: María López)")
        val inPinCaj = crearInputFintech(this, "PIN para este Cajero (4 dígitos)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val btnGuardarCaj = crearBotonFintech(this, "➕ Crear Cajero Nuevo", COLOR_YAPE_PURPLE) {
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
        cardCajeros.addView(titGestCaj)
        cardCajeros.addView(inNomCaj)
        cardCajeros.addView(inPinCaj)
        cardCajeros.addView(btnGuardarCaj)
        cardCajeros.addView(layoutListaCajerosAdmin)
        tabCajeros.addView(cardCajeros)

        val cardSoloLectura = crearTarjetaFintech(this)
        val titSoloLectura = TextView(this).apply {
            text = "🔒 Compartir Reporte en Solo Lectura"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#15803D"))
        }
        val descSoloLectura = TextView(this).apply {
            text = "Permite a tu personal o contador revisar las ventas desde su celular sin poder alterar fórmulas ni datos:"
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 4, 0, 10)
        }
        val inCorreoLectura = crearInputFintech(this, "Correo Gmail del personal")
        val btnCompartirLectura = crearBotonFintech(this, "Compartir Reporte en Solo Lectura", "#15803D") {
            val cor = inCorreoLectura.text.toString().trim()
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            if (cor.isEmpty() || !cor.contains("@")) {
                Toast.makeText(this, "Ingresa un Gmail válido", Toast.LENGTH_SHORT).show()
                return@crearBotonFintech
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
        cardSoloLectura.addView(titSoloLectura)
        cardSoloLectura.addView(descSoloLectura)
        cardSoloLectura.addView(inCorreoLectura)
        cardSoloLectura.addView(btnCompartirLectura)
        tabCajeros.addView(cardSoloLectura)

        val cardConfigQr = crearTarjetaFintech(this)
        val titQrConfig = TextView(this).apply {
            text = "📸 Configurar mi Código QR de Yape"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
        }
        val descQrConfig = TextView(this).apply {
            text = "¿Por qué es necesario?\nPara que la pantalla de cobro de tus cajeros muestre tu QR oficial a los clientes y paguen directamente a tu cuenta sin equivocarse de número ni de titular."
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 6, 0, 8)
        }
        val btnVideoTutorial = Button(this).apply {
            text = "▶️ Ver Video Tutorial: Cómo obtener el texto de mi QR"
            textSize = 13f
            setTextColor(Color.parseColor("#2563EB"))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 8)
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=como+obtener+texto+codigo+qr+yape")))
            }
        }
        inQrConfigTabCajeros = crearInputFintech(this, "Pega aquí el texto de tu QR (000201...)").apply {
            setText(prefs.getString("qr_base_yape", ""))
        }
        val btnGuardarQr = crearBotonFintech(this, "Guardar mi QR de Yape 💾", COLOR_YAPE_PURPLE) {
            val qrRaw = inQrConfigTabCajeros.text.toString().trim()
            if (qrRaw.isNotEmpty()) {
                prefs.edit().putString("qr_base_yape", qrRaw).apply()
                Toast.makeText(this, "✅ QR de Yape guardado exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Pega un código de QR válido", Toast.LENGTH_SHORT).show()
            }
        }
        cardConfigQr.addView(titQrConfig)
        cardConfigQr.addView(descQrConfig)
        cardConfigQr.addView(btnVideoTutorial)
        cardConfigQr.addView(inQrConfigTabCajeros)
        cardConfigQr.addView(btnGuardarQr)
        tabCajeros.addView(cardConfigQr)

        // -------------------------------------------------------------
        // PESTAÑA 3: AJUSTES (DIAGNÓSTICO COMPLETO + TU CUENTA)
        // -------------------------------------------------------------
        tabConfiguracion = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardConfig = crearTarjetaFintech(this)

        // PANEL DE DIAGNÓSTICO INTEGRAL CON LOS 3 SEMÁFOROS
        val cardDiagnostico = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#F3E8F7", "#D8B4E2", 18f)
            setPadding(22, 18, 22, 18)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 16)
            }
        }
        val titDiag = TextView(this).apply {
            text = "APP OPTIMIZADA CORRECTAMENTE"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            setPadding(0, 0, 0, 8)
        }
        txtEstadoLectorDiag = TextView(this).apply {
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 4, 0, 4)
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        txtEstadoBateriaDiag = TextView(this).apply {
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 4, 0, 4)
            setOnClickListener { solicitarSinRestriccionesBateria() }
        }
        txtEstadoNotifDiag = TextView(this).apply {
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 4, 0, 10)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        }
        val txtRecordatorioDiag = TextView(this).apply {
            text = "ℹ️ Recuerda que tienes que estar optimizado para que lleguen las notificaciones por audio y tener el control total de tu negocio."
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 4, 0, 0)
        }
        cardDiagnostico.addView(titDiag)
        cardDiagnostico.addView(txtEstadoLectorDiag)
        cardDiagnostico.addView(txtEstadoBateriaDiag)
        cardDiagnostico.addView(txtEstadoNotifDiag)
        cardDiagnostico.addView(txtRecordatorioDiag)

        // SECCIÓN TU CUENTA
        val titTuCuenta = TextView(this).apply {
            text = "TU CUENTA"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 12, 0, 8)
        }

        val btnCambiarPin = crearBotonFintech(this, "🔑 Cambiar mi PIN de Dueño", COLOR_YAPE_PURPLE) {
            dialogCambiarPin(prefs)
        }

        val btnAbrirSheet = crearBotonFintech(this, "📊 Abrir el Registro de mi Negocio en Vivo", COLOR_YAPE_PURPLE) {
            val url = prefs.getString("sheet_url_cliente", "") ?: ""
            if (url.isNotEmpty()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else Toast.makeText(this, "Revisa el enlace enviado a tu correo", Toast.LENGTH_SHORT).show()
        }

        val btnProbarAudio = crearBotonFintech(this, "🔊 Probar Sonido de Caja", COLOR_YAPE_CYAN) {
            probarAudioCaja()
        }.apply { setTextColor(Color.parseColor("#0F172A")) }

        val btnSoporte = crearBotonFintech(this, "💬 Contactar a Soporte por WhatsApp", "#25D366") {
            val cod = prefs.getString("codigo_tienda", "")
            val nom = prefs.getString("nombre_tienda", "")
            val mensaje = "Hola Soporte YAPENEGOCIOSVOZ, necesito ayuda con mi negocio $nom (Código: $cod)."
            val url = "https://api.whatsapp.com/send?phone=$NUMERO_SOPORTE_WSP&text=" + Uri.encode(mensaje)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        val btnCerrarSesion = Button(this).apply {
            text = "CERRAR SESIÓN"
            setTextColor(Color.parseColor("#DC2626"))
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 20, 0, 10)
            setOnClickListener {
                jobSincronizador?.cancel()
                prefs.edit().putBoolean("cajero_logueado", false).apply()
                mostrarSeccion(layoutPortada)
            }
        }

        cardConfig.addView(cardDiagnostico)
        cardConfig.addView(titTuCuenta)
        cardConfig.addView(btnCambiarPin)
        cardConfig.addView(btnAbrirSheet)
        cardConfig.addView(btnProbarAudio)
        cardConfig.addView(btnSoporte)
        cardConfig.addView(btnCerrarSesion)
        tabConfiguracion.addView(cardConfig)

        containerTabs.addView(tabCajaPOS)
        containerTabs.addView(tabCajeros)
        containerTabs.addView(tabConfiguracion)

        // BARRA INFERIOR FLOTANTE
        barraNavegacionInferior = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = crearBordeRedondo("#FFFFFF", "#E2E8F0", 30f)
            elevation = 20f
            setPadding(20, 14, 20, 14)
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                setMargins(40, 0, 40, 35)
            }
        }

        btnNavCaja = Button(this).apply {
            text = "🛒 Cobro"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { seleccionarPestaña(1) }
        }

        btnNavCajeros = Button(this).apply {
            text = "👥 Cajeros"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { seleccionarPestaña(2) }
        }

        btnNavConfig = Button(this).apply {
            text = "⚙️ Ajustes"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { seleccionarPestaña(3) }
        }

        barraNavegacionInferior.addView(btnNavCaja)
        barraNavegacionInferior.addView(btnNavCajeros)
        barraNavegacionInferior.addView(btnNavConfig)

        layoutAppPrincipal.addView(scrollPrincipal)
        layoutAppPrincipal.addView(barraNavegacionInferior)

        containerOnboarding.addView(layoutPortada)
        containerOnboarding.addView(layoutRegistroDueño)
        containerOnboarding.addView(layoutLoginDueño)
        containerOnboarding.addView(layoutLoginCajero)
        containerOnboarding.addView(layoutBienvenida)
        containerOnboarding.addView(layoutPermiso1Lector)
        containerOnboarding.addView(layoutPermiso2Alertas)
        containerOnboarding.addView(layoutPermiso3Bateria)

        val rootVertical = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // CABECERA SUPERIOR CON SELECTOR DE MODO OSCURO / CLARO
        val barraHeaderGlobal = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(35, 15, 35, 0)
        }
        val txtEspacioHeader = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnToggleModoOscuro = Button(this).apply {
            text = if (esModoOscuro) "☀️ Modo Claro" else "🌙 Modo Oscuro"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A"))
            background = crearBordeRedondo(if (esModoOscuro) "#1E293B" else "#F1F5F9", "#CBD5E1", 14f)
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                esModoOscuro = !esModoOscuro
                prefs.edit().putBoolean("modo_oscuro", esModoOscuro).apply()
                recreate() // Recarga la interfaz con el tema elegido
            }
        }
        barraHeaderGlobal.addView(txtEspacioHeader)
        barraHeaderGlobal.addView(btnToggleModoOscuro)

        rootVertical.addView(barraHeaderGlobal)
        rootVertical.addView(bannerSinInternet)
        rootVertical.addView(scrollOnboarding)
        rootVertical.addView(layoutAppPrincipal)

        rootLayout.addView(rootVertical)
        setContentView(rootLayout)

        val codGuardado = prefs.getString("codigo_tienda", "") ?: ""
        if (codGuardado.isNotEmpty()) {
            val topicLimpio = "tienda_" + codGuardado.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
            FirebaseMessaging.getInstance().subscribeToTopic(topicLimpio)
        }

        if (prefs.getBoolean("cajero_logueado", false)) {
            esSesionDueño = false
            entrarDirectoAlSistema()
        } else {
            mostrarSeccion(layoutPortada)
        }
    }

    private fun mostrarOpcionesDueñoDialog() {
        val opciones = arrayOf("✨ Registrar Nuevo Negocio", "🔑 Ya tengo una cuenta (Iniciar Sesión)")
        AlertDialog.Builder(this)
            .setTitle("Acceso de Dueño 👑")
            .setItems(opciones) { _, which ->
                if (which == 0) mostrarSeccion(layoutRegistroDueño)
                else mostrarSeccion(layoutLoginDueño)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarSeccion(layoutVisible: View) {
        layoutPortada.visibility = View.GONE
        layoutRegistroDueño.visibility = View.GONE
        layoutLoginDueño.visibility = View.GONE
        layoutLoginCajero.visibility = View.GONE
        layoutBienvenida.visibility = View.GONE
        layoutPermiso1Lector.visibility = View.GONE
        layoutPermiso2Alertas.visibility = View.GONE
        layoutPermiso3Bateria.visibility = View.GONE
        layoutAppPrincipal.visibility = View.GONE

        layoutVisible.visibility = View.VISIBLE
    }

    private fun verificarAsistentePermisosOEntrar() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val wizardListo = prefs.getBoolean("asistente_permisos_listo", false)
        if (wizardListo) entrarDirectoAlSistema()
        else mostrarSeccion(layoutPermiso1Lector)
    }

    private fun entrarDirectoAlSistema() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val nom = prefs.getString("nombre_tienda", "Mi Tienda")
        val cod = prefs.getString("codigo_tienda", "")
        val caj = prefs.getString("cajero_activo", if (esSesionDueño) "Dueño (Admin)" else "Cajero")

        txtInfoCajero.text = "🏪 $nom | 👤 $caj"
        txtBannerCodigoCajeros.text = "🔑 $cod"

        btnNavCajeros.visibility = if (esSesionDueño) View.VISIBLE else View.GONE
        btnNavConfig.visibility = if (esSesionDueño) View.VISIBLE else View.GONE

        mostrarSeccion(layoutAppPrincipal)
        seleccionarPestaña(1)
        iniciarSincronizador()
        recargarListaCajeros(prefs)
    }

    private fun seleccionarPestaña(pestaña: Int) {
        tabCajaPOS.visibility = if (pestaña == 1) View.VISIBLE else View.GONE
        tabCajeros.visibility = if (pestaña == 2) View.VISIBLE else View.GONE
        tabConfiguracion.visibility = if (pestaña == 3) View.VISIBLE else View.GONE

        btnNavCaja.background = crearBordeRedondo(if (pestaña == 1) COLOR_YAPE_PURPLE else "#FFFFFF", if (pestaña == 1) COLOR_YAPE_PURPLE else "#FFFFFF", 20f)
        btnNavCaja.setTextColor(if (pestaña == 1) Color.WHITE else Color.parseColor("#64748B"))

        btnNavCajeros.background = crearBordeRedondo(if (pestaña == 2) COLOR_YAPE_PURPLE else "#FFFFFF", if (pestaña == 2) COLOR_YAPE_PURPLE else "#FFFFFF", 20f)
        btnNavCajeros.setTextColor(if (pestaña == 2) Color.WHITE else Color.parseColor("#64748B"))

        btnNavConfig.background = crearBordeRedondo(if (pestaña == 3) COLOR_YAPE_PURPLE else "#FFFFFF", if (pestaña == 3) COLOR_YAPE_PURPLE else "#FFFFFF", 20f)
        btnNavConfig.setTextColor(if (pestaña == 3) Color.WHITE else Color.parseColor("#64748B"))

        if (pestaña == 3) actualizarDiagnosticoAjustes()
    }

    private fun actualizarDiagnosticoAjustes() {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val tieneLector = listeners.contains(packageName)
        txtEstadoLectorDiag.text = "• Lector de Yape / Plin: " + if (tieneLector) "🟢 ACTIVO" else "🔴 DESACTIVADO (Toca)"
        txtEstadoLectorDiag.setTextColor(Color.parseColor(if (tieneLector) "#15803D" else "#DC2626"))

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val tieneBateria = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.isIgnoringBatteryOptimizations(packageName) else true
        txtEstadoBateriaDiag.text = "• Optimización Segundo Plano: " + if (tieneBateria) "🟢 ACTIVO" else "🔴 DESACTIVADO (Toca)"
        txtEstadoBateriaDiag.setTextColor(Color.parseColor(if (tieneBateria) "#15803D" else "#DC2626"))

        val tieneNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        txtEstadoNotifDiag.text = "• Notificaciones en Pantalla: " + if (tieneNotif) "🟢 ACTIVADO" else "🔴 DESACTIVADO (Toca)"
        txtEstadoNotifDiag.setTextColor(Color.parseColor(if (tieneNotif) "#15803D" else "#DC2626"))
    }

    private fun actualizarVistaSaldo() {
        if (saldoOculto) {
            txtTotalHoyCaja.text = "S/ ••••••"
            btnOcultarSaldo.text = "👁️‍🗨️"
        } else {
            txtTotalHoyCaja.text = String.format(Locale.US, "S/ %.2f", totalVentasHoyNumero)
            btnOcultarSaldo.text = "👁️"
        }
    }

    private fun verificarConexionInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val net = cm.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(net) ?: return false
            return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        @Suppress("DEPRECATION")
        return cm.activeNetworkInfo?.isConnected ?: false
    }

    private fun probarAudioCaja() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)

                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                delay(220)
                toneG.release()
            } catch (e: Exception) {}

            if (vozLista && motorVozCajero != null) {
                motorVozCajero?.speak("Prueba de audio correcta. YAPENEGOCIOSVOZ lista para cobrar.", TextToSpeech.QUEUE_FLUSH, null, "TEST_TTS")
            }
        }
    }

    private fun enviarCuadrePorWhatsApp() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val nom = prefs.getString("nombre_tienda", "Mi Tienda")
        val caj = prefs.getString("cajero_activo", "Cajero")
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val mensaje = "📊 *CIERRE DE CAJA - YAPENEGOCIOSVOZ*\n\n" +
                "🏪 *Negocio:* $nom\n" +
                "📅 *Fecha:* $fechaHoy ($horaActual)\n" +
                "👤 *Responsable:* $caj\n" +
                "💰 *Total Recaudado:* S/ ${String.format(Locale.US, "%.2f", totalVentasHoyNumero)}\n" +
                "📋 *Total Operaciones:* $cantidadVentasHoy cobros confirmados\n\n" +
                "✅ _Reporte generado automáticamente._"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(mensaje)))
        startActivity(intent)
    }

    private fun mostrarDialogoAjustesRestringidos() {
        val mensaje = "Si Android bloqueó el interruptor con el mensaje 'Ajuste restringido', sigue estos 3 pasos:\n\n" +
                "1. Al presionar 'Ir a Ajustes', se abrirá la información de YAPENEGOCIOSVOZ.\n\n" +
                "2. Toca los 3 puntos arriba a la derecha (⋮).\n\n" +
                "3. Selecciona 'Permitir ajustes restringidos' e introduce tu huella o PIN.\n\n" +
                "¡Listo! Regresa a la app y podrás activar el interruptor normalmente."

        AlertDialog.Builder(this)
            .setTitle("Desbloquear Ajuste Restringido 🛡️")
            .setMessage(mensaje)
            .setPositiveButton("Ir a Ajustes de la App") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                Toast.makeText(this, "✅ YAPENEGOCIOSVOZ ya está optimizada para segundo plano.", Toast.LENGTH_SHORT).show()
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
            textSize = 16f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val inNuevo = EditText(this).apply {
            hint = "Nuevo PIN (4 dígitos)"
            textSize = 16f
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
        if (resultCode == RESULT_OK && data != null) {
            val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrEmpty()) {
                if (requestCode == REQ_GOOGLE_PICKER_REGISTRO) inCorreoRegistro.setText(accountName)
                else if (requestCode == REQ_GOOGLE_PICKER_LOGIN) inCorreoLoginDueño.setText(accountName)
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
                val hayNet = verificarConexionInternet()
                withContext(Dispatchers.Main) {
                    bannerSinInternet.visibility = if (hayNet) View.GONE else View.VISIBLE
                }

                if (hayNet) {
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
                                        reproducirTimbreYVozLocal("Yape", monto)
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
                }
                delay(2500)
            }
        }
    }

    private fun reproducirTimbreYVozLocal(metodo: String, monto: String) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                delay(220)
                toneG.release()
            } catch (e: Exception) {}

            val frase = if (monto == "1" || monto == "1.00") "un sol" else "$monto soles"
            motorVozCajero?.speak("¡Te llegó un cobro de $frase! ¡A seguir sumando!", TextToSpeech.QUEUE_FLUSH, null, "TTS_CAJA")
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
        var suma = 0.0
        var conteo = 0
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        for (i in 0 until minOf(array.length(), 15)) {
            val item = array.getJSONObject(i)
            val mStr = item.optString("monto", "0.00")
            val fStr = item.optString("fecha", "")

            if (fStr == fechaHoy) {
                suma += mStr.toDoubleOrNull() ?: 0.0
                conteo++
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(18, 14, 18, 14)
                background = crearBordeRedondo(if (esModoOscuro) "#1E293B" else "#FFFFFF", "#E2E8F0", 14f)
                elevation = 4f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 8)
                }
            }
            val txtInfo = TextView(this).apply {
                text = "${item.optString("nombre")} (${item.optString("hora")})"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val txtM = TextView(this).apply {
                text = "+ S/ $mStr"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(COLOR_YAPE_PURPLE))
            }
            row.addView(txtInfo)
            row.addView(txtM)
            layoutListaCobrosCaja.addView(row)
        }

        totalVentasHoyNumero = suma
        cantidadVentasHoy = conteo
        actualizarVistaSaldo()
    }

    private fun pedirPinDueño(prefs: android.content.SharedPreferences, onOk: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }
        val inP = EditText(this).apply {
            hint = "Ingresa tu PIN de 4 dígitos"
            textSize = 18f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }
        val btnOlvido = Button(this).apply {
            text = "¿Olvidaste tu PIN? Recupéralo por correo"
            textSize = 14f
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
            val cor = prefs.getString("correo_tienda", "") ?: ""
            solicitarRecuperacionPinPorCorreo(cor)
        }

        dialog.show()
    }

    private fun solicitarRecuperacionPinPorCorreo(correo: String) {
        AlertDialog.Builder(this)
            .setTitle("Recuperar PIN")
            .setMessage("¿Deseas enviar tu PIN de administrador a tu correo:\n\n$correo?")
            .setPositiveButton("Enviar Correo") { _, _ ->
                Toast.makeText(this, "Enviando PIN a tu correo...", Toast.LENGTH_SHORT).show()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = JSONObject().apply {
                            put("accion", "recuperar_pin")
                            put("correo", correo)
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
                textSize = 15f
                setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#334155"))
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

    // FACTORÍA VISUAL FINTECH
    private fun crearTarjetaFintech(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 35, 35, 35)
            elevation = 14f
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (esModoOscuro) "#1E293B" else "#FFFFFF"))
                cornerRadius = 28f
                setStroke(2, Color.parseColor(if (esModoOscuro) "#334155" else "#F1E4F5"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 22)
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

    private fun crearBotonFintech(context: Context, texto: String, colorHex: String, accion: () -> Unit): Button {
        return Button(context).apply {
            text = texto
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            isAllCaps = false
            elevation = 8f

            val baseShape = GradientDrawable().apply {
                setColor(Color.parseColor(colorHex))
                cornerRadius = 20f
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), baseShape, null)
            } else {
                background = baseShape
            }

            setPadding(24, 20, 24, 20)
            setOnClickListener { accion() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 10, 0, 10)
            }
        }
    }

    private fun crearInputFintech(context: Context, hintText: String): EditText {
        return EditText(context).apply {
            hint = hintText
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A"))
            textSize = 16f
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (esModoOscuro) "#0F172A" else "#F8FAFC"))
                cornerRadius = 18f
                setStroke(2, Color.parseColor(if (esModoOscuro) "#334155" else "#E2E8F0"))
            }
            setPadding(32, 22, 32, 22)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 6, 0, 14)
            }
        }
    }
}
