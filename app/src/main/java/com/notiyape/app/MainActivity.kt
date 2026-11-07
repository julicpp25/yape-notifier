package com.notiyape.app

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
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
import android.view.WindowManager
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
    private val NUMERO_SOPORTE_WSP = "51916224843"
    private val URL_POLITICA_PRIVACIDAD = "https://sites.google.com/view/yapealdia/documentaci%C3%B3n"
    private val URL_TUTORIAL_QR = "https://sites.google.com/view/yapealdia/inicio"

    private val REQ_GOOGLE_PICKER_REGISTRO = 1001
    private val REQ_GOOGLE_PICKER_LOGIN = 1002
    private val REQ_SELECCIONAR_IMAGEN_QR = 1005

    private val COLOR_PRIMARY = "#6B1D7B"
    private val COLOR_ACCENT = "#00D2B4"

    private val client = OkHttpClient()
    private var motorVozCajero: TextToSpeech? = null
    private var vozLista = false

    private lateinit var bannerSinInternet: TextView
    private lateinit var rootLayout: FrameLayout

    // SECCIONES
    private lateinit var layoutPortada: LinearLayout
    private lateinit var layoutRegistroDueño: LinearLayout
    private lateinit var layoutLoginDueño: LinearLayout
    private lateinit var layoutLoginCajero: LinearLayout
    private lateinit var layoutBienvenida: LinearLayout
    private lateinit var layoutPermiso1Lector: LinearLayout
    private lateinit var layoutPermiso2Alertas: LinearLayout
    private lateinit var layoutPermiso3Bateria: LinearLayout

    // DASHBOARD PRINCIPAL
    private lateinit var layoutAppPrincipal: RelativeLayout
    private lateinit var scrollPrincipal: ScrollView
    private lateinit var tabCajaPOS: LinearLayout
    private lateinit var tabCajeros: LinearLayout
    private lateinit var tabConfiguracion: LinearLayout
    private lateinit var barraNavegacionInferior: LinearLayout
    private lateinit var btnNavCaja: Button
    private lateinit var btnNavCajeros: Button
    private lateinit var btnNavConfig: Button

    // POS & CAJA
    private lateinit var txtInfoCajero: TextView
    private lateinit var txtMontoDisplayPOS: TextView
    private var montoTecladoPOS: String = ""
    private lateinit var imgQrPOS: ImageView
    private lateinit var txtMontoQrPOS: TextView
    private lateinit var switchVozCaja: Switch
    private lateinit var txtTotalHoyCaja: TextView
    private lateinit var btnOcultarSaldo: Button
    private lateinit var layoutListaCobrosCaja: LinearLayout
    private var ultimoIdCobroConocido = -1
    private var jobSincronizador: Job? = null

    // TURNOS & AUDITORÍA
    private var timestampInicioTurnoCajero: Long = 0L
    private var fechaInicioTurnoStr = ""
    private var horaInicioTurnoStr = ""
    private var saldoOculto = false
    private var totalVentasFiltradas = 0.0
    private var cantidadVentasFiltradas = 0
    private var fechaSeleccionadaFiltro = ""
    private var esModoOscuro = false

    // DIAGNÓSTICO Y CONFIG
    private lateinit var badgeLectorStatus: TextView
    private lateinit var badgeBateriaStatus: TextView
    private lateinit var badgeNotifStatus: TextView
    private lateinit var txtBannerCodigoCajeros: TextView
    private lateinit var inCorreoRegistro: EditText
    private lateinit var inCorreoLoginDueño: EditText
    private lateinit var layoutListaCajerosAdmin: LinearLayout
    private lateinit var btnModoOscuroAjustes: Button

    // QR DE YAPE
    private var qrTemporalDetectado: String = ""
    private lateinit var layoutContenedorQR: LinearLayout
    private lateinit var layoutAccionAdjuntarQr: LinearLayout
    private lateinit var layoutEstadoQrGuardado: LinearLayout
    private lateinit var txtEstadoQrGuardado: TextView
    private lateinit var txtPreviaQrDetectado: TextView
    private lateinit var btnGuardarQrDeYape: Button

    // FILTRO CALENDARIO (DECLARADOS CORRECTAMENTE)
    private lateinit var cardFiltroFechas: LinearLayout
    private lateinit var txtTituloPeriodo: TextView
    private lateinit var btnFiltroHoy: Button
    private lateinit var btnFiltroAyer: Button
    private lateinit var btnFiltroCalendario: Button

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
            setBackgroundColor(Color.parseColor(if (esModoOscuro) "#0B0F19" else "#F8FAFC"))
        }

        bannerSinInternet = TextView(this).apply {
            text = "⚠️ Modo Sin Conexión • Reconectando..."
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#92400E"))
            setBackgroundColor(Color.parseColor("#FEF3C7"))
            gravity = Gravity.CENTER
            setPadding(16, 12, 16, 12)
            visibility = View.GONE
        }

        val scrollOnboarding = ScrollView(this)
        val containerOnboarding = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 35, 30, 45)
        }
        scrollOnboarding.addView(containerOnboarding)

        // 1. PORTADA
        layoutPortada = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardPortada = crearTarjetaFintech(this)
        val imgBanner = ImageView(this).apply {
            setImageResource(R.drawable.logo_horizontal)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 10, 0, 24)
            }
        }
        val btnDueño = crearBotonPrimario(this, "👑 SOY EL DUEÑO\n(Administrador de Negocio)") {
            if (prefs.getBoolean("tienda_registrada", false)) {
                pedirPinDueño(prefs) {
                    esSesionDueño = true
                    verificarAsistentePermisosOEntrar()
                }
            } else {
                mostrarOpcionesDueñoDialog()
            }
        }
        val btnCajero = crearBotonSecundario(this, "🛒 Soy Cajero (Entrar al Punto de Venta)") {
            mostrarSeccion(layoutLoginCajero)
        }
        cardPortada.addView(imgBanner)
        cardPortada.addView(btnDueño)
        cardPortada.addView(btnCajero)
        layoutPortada.addView(cardPortada)

        // 2. REGISTRO DUEÑO
        layoutRegistroDueño = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardReg = crearTarjetaFintech(this)
        val inNom = crearInputFintech(this, "Nombre de tu Negocio")
        inCorreoRegistro = crearInputFintech(this, "Correo Gmail")
        val btnGoogleReg = crearBotonSecundario(this, "G Seleccionar mi Correo de Google") {
            try {
                val intent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                startActivityForResult(intent, REQ_GOOGLE_PICKER_REGISTRO)
            } catch (e: Exception) {}
        }
        val inP1 = crearInputFintech(this, "Crea tu PIN (4 dígitos)").apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val inP2 = crearInputFintech(this, "Confirmar PIN (4 dígitos)").apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val btnCrear = crearBotonPrimario(this, "Crear mi Negocio 🚀") {
            val nom = inNom.text.toString().trim()
            val cor = inCorreoRegistro.text.toString().trim()
            val p1 = inP1.text.toString().trim()
            val p2 = inP2.text.toString().trim()
            if (nom.isEmpty() || cor.isEmpty() || p1.length < 4 || p1 != p2) {
                Toast.makeText(this, "Verifica los datos y que los PIN coincidan", Toast.LENGTH_SHORT).show()
                return@crearBotonPrimario
            }
            val cod = nom.replace(" ", "").take(5).uppercase() + "-" + (100..999).random()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("accion", "registrar_tienda")
                        put("codigoTienda", cod)
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
                        val topic = "tienda_" + cod.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
                        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        prefs.edit().putString("nombre_tienda", nom).putString("correo_tienda", cor)
                            .putString("codigo_tienda", cod).putString("pin_dueño", p1)
                            .putString("sheet_url_cliente", resJson.optString("sheetUrl", ""))
                            .putBoolean("tienda_registrada", true).apply()
                        runOnUiThread {
                            esSesionDueño = true
                            mostrarSeccion(layoutBienvenida)
                        }
                    } else {
                        runOnUiThread {
                            AlertDialog.Builder(this@MainActivity).setTitle("Aviso").setMessage(resJson.optString("mensaje"))
                                .setPositiveButton("Iniciar Sesión") { _, _ ->
                                    inCorreoLoginDueño.setText(cor)
                                    mostrarSeccion(layoutLoginDueño)
                                }.setNegativeButton("Cerrar", null).show()
                        }
                    }
                } catch (e: Exception) {}
            }
        }
        val btnLinkLogin = Button(this).apply {
            text = "¿Ya tienes cuenta? Inicia sesión aquí"
            setTextColor(Color.parseColor("#2563EB"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { mostrarSeccion(layoutLoginDueño) }
        }
        cardReg.addView(TextView(this).apply { text = "Nuevo Negocio 👑"; textSize = 22f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardReg.addView(inNom); cardReg.addView(btnGoogleReg); cardReg.addView(inCorreoRegistro)
        cardReg.addView(inP1); cardReg.addView(inP2); cardReg.addView(btnCrear); cardReg.addView(btnLinkLogin)
        layoutRegistroDueño.addView(cardReg)

        // 3. LOGIN DUEÑO
        layoutLoginDueño = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardLogD = crearTarjetaFintech(this)
        inCorreoLoginDueño = crearInputFintech(this, "Correo Gmail")
        val btnGoogleLogin = crearBotonSecundario(this, "G Seleccionar mi Correo de Google") {
            try {
                val intent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                startActivityForResult(intent, REQ_GOOGLE_PICKER_LOGIN)
            } catch (e: Exception) {}
        }
        val inPinLogD = crearInputFintech(this, "PIN (4 dígitos)").apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val btnEntrarDueño = crearBotonPrimario(this, "Ingresar a mi Negocio 🚀") {
            val cor = inCorreoLoginDueño.text.toString().trim()
            val pin = inPinLogD.text.toString().trim()
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
                        val topic = "tienda_" + cod.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
                        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        prefs.edit().putString("codigo_tienda", cod).putString("nombre_tienda", nom)
                            .putString("correo_tienda", cor).putString("pin_dueño", pin)
                            .putString("sheet_url_cliente", resJson.optString("sheetUrl", ""))
                            .putString("qr_base_yape", resJson.optString("qrBase", ""))
                            .putString("lista_cajeros", resJson.optString("cajeros", "[]"))
                            .putBoolean("tienda_registrada", true).apply()
                        runOnUiThread {
                            esSesionDueño = true
                            verificarAsistentePermisosOEntrar()
                        }
                    } else {
                        runOnUiThread { Toast.makeText(this@MainActivity, resJson.optString("mensaje"), Toast.LENGTH_SHORT).show() }
                    }
                } catch (e: Exception) {}
            }
        }
        val btnOlvido = Button(this).apply {
            text = "¿Olvidaste tu PIN? Recupéralo por correo"
            setTextColor(Color.parseColor("#2563EB"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                val cor = inCorreoLoginDueño.text.toString().trim()
                if (cor.isNotEmpty()) solicitarRecuperacionPinPorCorreo(cor)
            }
        }
        cardLogD.addView(TextView(this).apply { text = "Iniciar Sesión 👑"; textSize = 22f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardLogD.addView(btnGoogleLogin); cardLogD.addView(inCorreoLoginDueño); cardLogD.addView(inPinLogD); cardLogD.addView(btnEntrarDueño); cardLogD.addView(btnOlvido)
        layoutLoginDueño.addView(cardLogD)

        // 4. LOGIN CAJERO
        layoutLoginCajero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardLogC = crearTarjetaFintech(this)
        val inCodC = crearInputFintech(this, "Código de Tienda (Ej: BODEG-123)").apply { setText(prefs.getString("codigo_tienda", "")) }
        val inNomC = crearInputFintech(this, "Tu Nombre (Ej: Carlos)")
        val inPinC = crearInputFintech(this, "PIN").apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val btnEntrarCaja = crearBotonPrimario(this, "Iniciar Turno de Caja 🚀") {
            val cod = inCodC.text.toString().trim()
            val caj = inNomC.text.toString().trim()
            val pin = inPinC.text.toString().trim()
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
                        val topic = "tienda_" + cod.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
                        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        val nowMs = System.currentTimeMillis()
                        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
                        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(nowMs))
                        prefs.edit().putString("codigo_tienda", cod).putString("nombre_tienda", resJson.optString("nombreNegocio", cod))
                            .putString("cajero_activo", caj).putString("qr_base_yape", resJson.optString("qrBase", ""))
                            .putLong("cajero_inicio_turno_ms", nowMs).putString("cajero_hora_inicio", hora)
                            .putString("cajero_fecha_inicio", fecha).putBoolean("cajero_logueado", true).apply()
                        runOnUiThread {
                            timestampInicioTurnoCajero = nowMs
                            horaInicioTurnoStr = hora
                            fechaInicioTurnoStr = fecha
                            esSesionDueño = false
                            verificarAsistentePermisosOEntrar()
                        }
                    } else {
                        runOnUiThread { Toast.makeText(this@MainActivity, resJson.optString("mensaje"), Toast.LENGTH_SHORT).show() }
                    }
                } catch (e: Exception) {}
            }
        }
        cardLogC.addView(TextView(this).apply { text = "Acceso para Cajeros 🛒"; textSize = 22f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardLogC.addView(inCodC); cardLogC.addView(inNomC); cardLogC.addView(inPinC); cardLogC.addView(btnEntrarCaja)
        layoutLoginCajero.addView(cardLogC)

        // 5. BIENVENIDA & ASISTENTE
        layoutBienvenida = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardBv = crearTarjetaFintech(this)
        cardBv.addView(ImageView(this).apply { setImageResource(R.drawable.logo_horizontal); adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER })
        cardBv.addView(TextView(this).apply { text = "“¡Tu Yape ahora habla y administra tu negocio!”"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)); setPadding(0, 16, 0, 16) })
        cardBv.addView(crearBotonPrimario(this, "Comenzar Configuración ➡️") { mostrarSeccion(layoutPermiso1Lector) })
        layoutBienvenida.addView(cardBv)

        // ASISTENTE 1
        layoutPermiso1Lector = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardP1 = crearTarjetaFintech(this)
        cardP1.addView(TextView(this).apply { text = "🔔 Paso 1 de 3: Lector de Notificaciones"; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardP1.addView(TextView(this).apply { text = "🔒 100% Seguro: YapeAlDía NO toca tu dinero ni pide claves bancarias. Solo escucha el aviso para cantar el cobro por el altavoz y guardarlo automáticamente."; textSize = 13f; setTextColor(Color.parseColor("#475569")); setPadding(0, 8, 0, 12) })
        cardP1.addView(crearBotonPrimario(this, "ACTIVAR LECTOR DE NOTIFICACIONES") { mostrarDivulgacionProminenteNotificaciones() })
        cardP1.addView(Button(this).apply { text = "⚠️ ¿Te sale 'Ajuste restringido'? Toca aquí"; setTextColor(Color.parseColor("#B45309")); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { mostrarDialogoAjustesRestringidos() } })
        cardP1.addView(crearBotonSecundario(this, "Siguiente Paso ➡️") { mostrarSeccion(layoutPermiso2Alertas) })
        layoutPermiso1Lector.addView(cardP1)

        // ASISTENTE 2
        layoutPermiso2Alertas = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardP2 = crearTarjetaFintech(this)
        cardP2.addView(TextView(this).apply { text = "📲 Paso 2 de 3: Alertas en Pantalla"; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardP2.addView(TextView(this).apply { text = "🔔 Necesario para que tu teléfono vibre y muestre el pago en la barra superior incluso con la pantalla apagada."; textSize = 13f; setTextColor(Color.parseColor("#475569")); setPadding(0, 8, 0, 12) })
        cardP2.addView(crearBotonPrimario(this, "ACTIVAR NOTIFICACIONES") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        })
        cardP2.addView(crearBotonSecundario(this, "Siguiente Paso ➡️") { mostrarSeccion(layoutPermiso3Bateria) })
        layoutPermiso2Alertas.addView(cardP2)

        // ASISTENTE 3
        layoutPermiso3Bateria = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardP3 = crearTarjetaFintech(this)
        cardP3.addView(TextView(this).apply { text = "🔋 Paso 3 de 3: Batería Sin Restricciones"; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardP3.addView(TextView(this).apply { text = "⚡ Evita que el celular pause la app. Estará despierta todo el día para cantar cada cobro a tiempo."; textSize = 13f; setTextColor(Color.parseColor("#475569")); setPadding(0, 8, 0, 12) })
        cardP3.addView(crearBotonPrimario(this, "OPTIMIZA BATERÍA PARA LA APP") { solicitarSinRestriccionesBateria() })
        cardP3.addView(crearBotonSecundario(this, "Finalizar y Entrar a la App 🚀") {
            prefs.edit().putBoolean("asistente_permisos_listo", true).apply()
            entrarDirectoAlSistema()
        })
        layoutPermiso3Bateria.addView(cardP3)

        // =========================================================================
        // DASHBOARD 3 PESTAÑAS (FINTECH MATERIAL 3)
        // =========================================================================
        layoutAppPrincipal = RelativeLayout(this).apply { visibility = View.GONE }
        scrollPrincipal = ScrollView(this).apply { setPadding(25, 10, 25, 170); clipToPadding = false }
        val containerTabs = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollPrincipal.addView(containerTabs)

        // TAB 1: CAJA / POS CON TECLADO TÁCTIL
        tabCajaPOS = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val barraSubCaja = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 10) }
        txtInfoCajero = TextView(this).apply { textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A")); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        switchVozCaja = Switch(this).apply { text = "🔊 "; isChecked = prefs.getBoolean("voz_activa", true); setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("voz_activa", isChecked).apply() } }
        val btnKeepScreen = Button(this).apply {
            text = "💡 Pantalla Activa"
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            background = crearBordeRedondo("#FEF08A", "#FACC15", 12f)
            setPadding(12, 8, 12, 8)
            var encendida = false
            setOnClickListener {
                encendida = !encendida
                if (encendida) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    text = "💡 Pantalla Fija: ON"
                    background = crearBordeRedondo("#10B981", "#059669", 12f)
                    setTextColor(Color.WHITE)
                    Toast.makeText(this@MainActivity, "💡 Modo Mostrador Activo: La pantalla no se apagará", Toast.LENGTH_SHORT).show()
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    text = "💡 Pantalla Activa"
                    background = crearBordeRedondo("#FEF08A", "#FACC15", 12f)
                    setTextColor(Color.parseColor("#0F172A"))
                }
            }
        }
        val btnSalirCaj = Button(this).apply {
            text = "CERRAR SESIÓN"
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = crearBordeRedondo("#DC2626", "#DC2626", 12f)
            setPadding(14, 8, 14, 8)
            setOnClickListener { confirmarCierreDeTurno() }
        }
        barraSubCaja.addView(txtInfoCajero); barraSubCaja.addView(switchVozCaja); barraSubCaja.addView(btnKeepScreen); barraSubCaja.addView(btnSalirCaj)

        // Filtro Fechas Admin
        cardFiltroFechas = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo(if (esModoOscuro) "#1E293B" else "#FFFFFF", "#E2E8F0", 16f)
            setPadding(14, 10, 14, 10)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 10) }
        }
        txtTituloPeriodo = TextView(this).apply { text = "📅 Período: Ventas de Hoy"; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) }
        val rowF = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        
        btnFiltroHoy = Button(this).apply { 
            text = "Hoy"; textSize = 11f; background = crearBordeRedondo(COLOR_PRIMARY, COLOR_PRIMARY, 10f); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { fechaSeleccionadaFiltro = ""; txtTituloPeriodo.text = "📅 Período: Ventas de Hoy"; consultarCobrosServidorUnaVez() } 
        }
        btnFiltroAyer = Button(this).apply { 
            text = "Ayer"; textSize = 11f; background = crearBordeRedondo("#F1F5F9", "#CBD5E1", 10f); setTextColor(Color.parseColor("#475569")); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { val c = Calendar.getInstance().apply { add(Calendar.DATE, -1) }; fechaSeleccionadaFiltro = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time); txtTituloPeriodo.text = "📅 Período: Ayer ($fechaSeleccionadaFiltro)"; consultarCobrosServidorUnaVez() } 
        }
        btnFiltroCalendario = Button(this).apply { 
            text = "📆 Calendario"; textSize = 11f; background = crearBordeRedondo("#F1F5F9", "#CBD5E1", 10f); setTextColor(Color.parseColor("#475569")); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            setOnClickListener { abrirSelectorCalendario() } 
        }
        
        rowF.addView(btnFiltroHoy); rowF.addView(btnFiltroAyer); rowF.addView(btnFiltroCalendario)
        cardFiltroFechas.addView(txtTituloPeriodo); cardFiltroFechas.addView(rowF)

        // Tarjeta Total
        val cardTotal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#F3E8F7", "#D8B4E2", 20f)
            setPadding(24, 18, 24, 18)
            elevation = 6f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 14) }
        }
        val rowTH = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val lblT = TextView(this).apply { text = "TOTAL DEL PERÍODO"; textSize = 12f; setTextColor(Color.parseColor(COLOR_PRIMARY)); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        btnOcultarSaldo = Button(this).apply { text = "👁️"; textSize = 14f; background = crearBordeRedondo("#FFFFFF", "#D8B4E2", 12f); setPadding(10, 6, 10, 6); setOnClickListener { saldoOculto = !saldoOculto; actualizarVistaSaldo() } }
        val btnRefresh = Button(this).apply { text = "🔄"; textSize = 14f; background = crearBordeRedondo("#FFFFFF", "#D8B4E2", 12f); setPadding(10, 6, 10, 6); setOnClickListener { consultarCobrosServidorUnaVez() } }
        rowTH.addView(lblT); rowTH.addView(btnOcultarSaldo); rowTH.addView(btnRefresh)
        txtTotalHoyCaja = TextView(this).apply { text = "S/ 0.00"; textSize = 32f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)); setPadding(0, 4, 0, 0) }
        cardTotal.addView(rowTH); cardTotal.addView(txtTotalHoyCaja)

        // POS Táctil con Montos Rápidos
        val cardPOS = crearTarjetaFintech(this)
        txtMontoDisplayPOS = TextView(this).apply { text = "S/ 0.00"; textSize = 38f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)); gravity = Gravity.CENTER; setPadding(0, 8, 0, 8) }
        val filaMontosRapidos = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 10)
        }
        listOf("5", "10", "20", "50").forEach { mRapido ->
            val btnR = Button(this).apply {
                text = "S/ $mRapido"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(COLOR_PRIMARY))
                background = crearBordeRedondo("#F3E8F7", "#D8B4E2", 14f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) }
                setOnClickListener {
                    montoTecladoPOS = mRapido
                    txtMontoDisplayPOS.text = "S/ $mRapido.00"
                }
            }
            filaMontosRapidos.addView(btnR)
        }
        val layTeclado = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layTeclado.addView(filaMontosRapidos)
        layTeclado.addView(crearFilaTeclado("1", "2", "3"))
        layTeclado.addView(crearFilaTeclado("4", "5", "6"))
        layTeclado.addView(crearFilaTeclado("7", "8", "9"))
        layTeclado.addView(crearFilaTeclado("C", "0", "⌫"))
        imgQrPOS = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(420, 420).apply { gravity = Gravity.CENTER_HORIZONTAL; setMargins(0, 8, 0, 8) }; visibility = View.GONE }
        txtMontoQrPOS = TextView(this).apply { textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)); gravity = Gravity.CENTER; visibility = View.GONE }
        val btnGenQr = crearBotonPrimario(this, "⚡ MOSTRAR QR DE COBRO") {
            val mVal = montoTecladoPOS.toDoubleOrNull() ?: 0.0
            if (mVal <= 0.0) { Toast.makeText(this, "Ingresa un monto", Toast.LENGTH_SHORT).show(); return@crearBotonPrimario }
            val qBase = prefs.getString("qr_base_yape", "") ?: ""
            if (qBase.isEmpty()) { Toast.makeText(this, "Configura tu QR en Cajeros", Toast.LENGTH_LONG).show(); return@crearBotonPrimario }
            val bmp = generarBitmapQr(qBase, 420, 420)
            if (bmp != null) {
                imgQrPOS.setImageBitmap(bmp); imgQrPOS.visibility = View.VISIBLE
                txtMontoQrPOS.text = "Escanea y paga: S/ " + String.format(Locale.US, "%.2f", mVal)
                txtMontoQrPOS.visibility = View.VISIBLE
            }
        }
        cardPOS.addView(txtMontoDisplayPOS); cardPOS.addView(layTeclado); cardPOS.addView(btnGenQr); cardPOS.addView(imgQrPOS); cardPOS.addView(txtMontoQrPOS)

        // Historial
        val cardHist = crearTarjetaFintech(this)
        layoutListaCobrosCaja = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cardHist.addView(TextView(this).apply { text = "📋 Cobros en Vivo:"; textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A")) })
        cardHist.addView(layoutListaCobrosCaja)
        cardHist.addView(crearBotonSecundario(this, "📲 Enviar Cuadre por WhatsApp") { enviarCuadrePorWhatsApp() })
        cardHist.addView(crearBotonSecundario(this, "📥 Exportar Historial CSV Contable") { exportarHistorialCSV() })

        tabCajaPOS.addView(barraSubCaja); tabCajaPOS.addView(cardFiltroFechas); tabCajaPOS.addView(cardTotal); tabCajaPOS.addView(cardPOS); tabCajaPOS.addView(cardHist)

        // TAB 2: CAJEROS
        tabCajeros = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardCod = crearTarjetaFintech(this)
        txtBannerCodigoCajeros = TextView(this).apply { textSize = 24f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#1D4ED8")); setPadding(0, 4, 0, 4) }
        cardCod.addView(TextView(this).apply { text = "Punto de Caja para Celulares 🏪"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardCod.addView(txtBannerCodigoCajeros)
        cardCod.addView(crearBotonPrimario(this, "📋 Copiar Código") {
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Codigo", cod))
            Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show()
        })
        tabCajeros.addView(cardCod)

        val cardNewCaj = crearTarjetaFintech(this)
        val inNCaj = crearInputFintech(this, "Nombre del Cajero (Ej: Carlos)")
        val inPCaj = crearInputFintech(this, "PIN (4 dígitos)").apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        layoutListaCajerosAdmin = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val btnSaveCaj = crearBotonPrimario(this, "➕ Crear Cajero Nuevo") {
            val n = inNCaj.text.toString().trim()
            val p = inPCaj.text.toString().trim()
            val cod = prefs.getString("codigo_tienda", "") ?: ""
            if (n.isNotEmpty() && p.length >= 4) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = JSONObject().apply { put("accion", "crear_cajero"); put("codigoTienda", cod); put("nombreCajero", n); put("pinCajero", p) }
                        val b = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                        client.newCall(Request.Builder().url(SERVIDOR_URL).post(b).build()).execute().close()
                    } catch (e: Exception) {}
                }
                agregarCajeroLocal(prefs, n, p); inNCaj.setText(""); inPCaj.setText(""); recargarListaCajeros(prefs)
            }
        }
        cardNewCaj.addView(TextView(this).apply { text = "👥 Cajeros Autorizados:"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardNewCaj.addView(inNCaj); cardNewCaj.addView(inPCaj); cardNewCaj.addView(btnSaveCaj); cardNewCaj.addView(layoutListaCajerosAdmin)
        tabCajeros.addView(cardNewCaj)

        val cardQRTab = crearTarjetaFintech(this)
        cardQRTab.addView(TextView(this).apply { text = "📸 Configurar mi Código QR de Yape"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardQRTab.addView(Button(this).apply { text = "▶️ Ver Tutorial: Cómo obtener mi código QR de Yape"; setTextColor(Color.parseColor("#2563EB")); setBackgroundColor(Color.TRANSPARENT); textSize = 13f; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_TUTORIAL_QR))) } })

        layoutContenedorQR = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        layoutAccionAdjuntarQr = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val btnAdjuntarImgQR = crearBotonSecundario(this, "📷 Adjuntar Imagen de mi QR de Yape (Galería)") { abrirGaleriaParaQR() }
        txtPreviaQrDetectado = TextView(this).apply {
            text = "Toca el botón arriba para seleccionar la foto o captura de tu QR de Yape."
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 10, 0, 10)
            gravity = Gravity.CENTER
        }
        btnGuardarQrDeYape = crearBotonPrimario(this, "Guardar mi QR de Yape 💾") {
            guardarQrSeleccionado()
        }.apply { visibility = View.GONE }

        layoutAccionAdjuntarQr.addView(btnAdjuntarImgQR)
        layoutAccionAdjuntarQr.addView(txtPreviaQrDetectado)
        layoutAccionAdjuntarQr.addView(btnGuardarQrDeYape)

        layoutEstadoQrGuardado = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        txtEstadoQrGuardado = TextView(this).apply {
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#15803D"))
            background = crearBordeRedondo(if (esModoOscuro) "#064E3B" else "#ECFDF5", "#10B981", 16f)
            setPadding(24, 20, 24, 20)
            gravity = Gravity.CENTER
        }
        val btnCambiarQR = crearBotonSecundario(this, "🔄 Cambiar o Actualizar Foto de mi QR") {
            qrTemporalDetectado = ""
            txtPreviaQrDetectado.text = "Selecciona una nueva imagen de tu QR de Yape:"
            txtPreviaQrDetectado.setTextColor(Color.parseColor("#64748B"))
            btnGuardarQrDeYape.visibility = View.GONE
            layoutAccionAdjuntarQr.visibility = View.VISIBLE
            layoutEstadoQrGuardado.visibility = View.GONE
        }
        layoutEstadoQrGuardado.addView(txtEstadoQrGuardado)
        layoutEstadoQrGuardado.addView(btnCambiarQR)

        layoutContenedorQR.addView(layoutAccionAdjuntarQr)
        layoutContenedorQR.addView(layoutEstadoQrGuardado)
        cardQRTab.addView(layoutContenedorQR)
        tabCajeros.addView(cardQRTab)

        renderizarVistaQR()

        // TAB 3: AJUSTES
        tabConfiguracion = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val cardCfg = crearTarjetaFintech(this)
        val rowBdg = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        badgeLectorStatus = crearPildoraEstado("Lector Yape/Plin") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        badgeBateriaStatus = crearPildoraEstado("Segundo Plano 24/7") { solicitarSinRestriccionesBateria() }
        badgeNotifStatus = crearPildoraEstado("Alertas de Cobro") { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101) }
        rowBdg.addView(badgeLectorStatus); rowBdg.addView(badgeBateriaStatus); rowBdg.addView(badgeNotifStatus)
        btnModoOscuroAjustes = crearBotonSecundario(this, if (esModoOscuro) "☀️ Cambiar a Modo Claro" else "🌙 Cambiar a Modo Oscuro") {
            esModoOscuro = !esModoOscuro
            prefs.edit().putBoolean("modo_oscuro", esModoOscuro).putInt("restaurar_pestaña", 3).apply()
            recreate()
        }
        val btnPinD = crearBotonPrimario(this, "🔑 Cambiar mi PIN de Dueño") { dialogCambiarPin(prefs) }
        val btnSheet = crearBotonPrimario(this, "📊 Abrir Registro en Google Sheets") {
            val u = prefs.getString("sheet_url_cliente", "") ?: ""
            if (u.isNotEmpty()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        }
        val btnTestSound = crearBotonSecundario(this, "🔊 Probar Sonido de Caja") { probarAudioCaja() }
        val btnWspHelp = crearBotonSecundario(this, "💬 Soporte por WhatsApp") {
            val cod = prefs.getString("codigo_tienda", ""); val nom = prefs.getString("nombre_tienda", "")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$NUMERO_SOPORTE_WSP&text=" + Uri.encode("Hola Soporte YapeAlDía, negocio $nom ($cod)"))))
        }
        val btnPolitica = Button(this).apply {
            text = "📄 Política de Privacidad y Seguridad"; setTextColor(Color.parseColor("#2563EB")); setBackgroundColor(Color.TRANSPARENT); textSize = 13f; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_POLITICA_PRIVACIDAD))) }
        }
        val btnLogout = Button(this).apply {
            text = "CERRAR SESIÓN"; setTextColor(Color.parseColor("#DC2626")); setBackgroundColor(Color.TRANSPARENT); textSize = 14f; setTypeface(null, Typeface.BOLD)
            setOnClickListener { jobSincronizador?.cancel(); prefs.edit().putBoolean("cajero_logueado", false).apply(); mostrarSeccion(layoutPortada) }
        }
        cardCfg.addView(TextView(this).apply { text = "ESTADO DEL SISTEMA"; textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(COLOR_PRIMARY)) })
        cardCfg.addView(rowBdg); cardCfg.addView(btnModoOscuroAjustes); cardCfg.addView(btnPinD); cardCfg.addView(btnSheet); cardCfg.addView(btnTestSound); cardCfg.addView(btnWspHelp); cardCfg.addView(btnPolitica); cardCfg.addView(btnLogout)
        tabConfiguracion.addView(cardCfg)

        containerTabs.addView(tabCajaPOS); containerTabs.addView(tabCajeros); containerTabs.addView(tabConfiguracion)

        // DOCK INFERIOR
        barraNavegacionInferior = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = crearBordeRedondo(if (esModoOscuro) "#1E293B" else "#FFFFFF", if (esModoOscuro) "#334155" else "#E2E8F0", 28f)
            elevation = 16f
            setPadding(16, 10, 16, 10)
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM); setMargins(40, 0, 40, 25)
            }
        }
        btnNavCaja = Button(this).apply { text = "🛒 Cobro"; textSize = 13f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { seleccionarPestaña(1) } }
        btnNavCajeros = Button(this).apply { text = "👥 Cajeros"; textSize = 13f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { seleccionarPestaña(2) } }
        btnNavConfig = Button(this).apply { text = "⚙️ Ajustes"; textSize = 13f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { seleccionarPestaña(3) } }
        barraNavegacionInferior.addView(btnNavCaja); barraNavegacionInferior.addView(btnNavCajeros); barraNavegacionInferior.addView(btnNavConfig)

        layoutAppPrincipal.addView(scrollPrincipal)
        layoutAppPrincipal.addView(barraNavegacionInferior)

        containerOnboarding.addView(layoutPortada); containerOnboarding.addView(layoutRegistroDueño); containerOnboarding.addView(layoutLoginDueño)
        containerOnboarding.addView(layoutLoginCajero); containerOnboarding.addView(layoutBienvenida); containerOnboarding.addView(layoutPermiso1Lector)
        containerOnboarding.addView(layoutPermiso2Alertas); containerOnboarding.addView(layoutPermiso3Bateria)

        val rootVertical = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        rootVertical.addView(bannerSinInternet); rootVertical.addView(scrollOnboarding); rootVertical.addView(layoutAppPrincipal)
        rootLayout.addView(rootVertical)
        setContentView(rootLayout)

        val codG = prefs.getString("codigo_tienda", "") ?: ""
        if (codG.isNotEmpty()) FirebaseMessaging.getInstance().subscribeToTopic("tienda_" + codG.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_"))

        val pRest = prefs.getInt("restaurar_pestaña", 0)
        if (pRest > 0) {
            prefs.edit().remove("restaurar_pestaña").apply()
            esSesionDueño = true; entrarDirectoAlSistema(); seleccionarPestaña(pRest)
        } else if (prefs.getBoolean("cajero_logueado", false)) {
            esSesionDueño = false
            timestampInicioTurnoCajero = prefs.getLong("cajero_inicio_turno_ms", 0L)
            horaInicioTurnoStr = prefs.getString("cajero_hora_inicio", "") ?: ""
            fechaInicioTurnoStr = prefs.getString("cajero_fecha_inicio", "") ?: ""
            entrarDirectoAlSistema()
        } else {
            mostrarSeccion(layoutPortada)
        }
    }

    private fun crearFilaTeclado(b1: String, b2: String, b3: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            setPadding(0, 3, 0, 3)
            addView(crearBotonTeclado(b1))
            addView(crearBotonTeclado(b2))
            addView(crearBotonTeclado(b3))
        }
    }

    private fun crearBotonTeclado(tecla: String): Button {
        return Button(this).apply {
            text = tecla
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A"))
            background = crearBordeRedondo(if (esModoOscuro) "#334155" else "#F1F5F9", "#CBD5E1", 14f)
            layoutParams = LinearLayout.LayoutParams(0, 110, 1f).apply { setMargins(4, 4, 4, 4) }
            setOnClickListener {
                when (tecla) {
                    "C" -> {
                        montoTecladoPOS = ""
                    }
                    "⌫" -> {
                        if (montoTecladoPOS.isNotEmpty()) {
                            montoTecladoPOS = montoTecladoPOS.substring(0, montoTecladoPOS.length - 1)
                        }
                    }
                    else -> {
                        if (montoTecladoPOS.length < 7) {
                            montoTecladoPOS += tecla
                        }
                    }
                }
                val mVal = montoTecladoPOS.toDoubleOrNull() ?: 0.0
                txtMontoDisplayPOS.text = if (montoTecladoPOS.isEmpty()) "S/ 0.00" else String.format(Locale.US, "S/ %.2f", mVal)
            }
        }
    }

    private fun crearPildoraEstado(nombre: String, onClick: () -> Unit) = TextView(this).apply {
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        setPadding(16, 10, 16, 10)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 4, 0, 4) }
        setOnClickListener { onClick() }
    }

    private fun mostrarOpcionesDueñoDialog() {
        AlertDialog.Builder(this).setTitle("Acceso de Dueño 👑")
            .setItems(arrayOf("✨ Registrar Nuevo Negocio", "🔑 Ya tengo cuenta (Iniciar Sesión)")) { _, w ->
                if (w == 0) mostrarSeccion(layoutRegistroDueño) else mostrarSeccion(layoutLoginDueño)
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun mostrarSeccion(v: View) {
        layoutPortada.visibility = View.GONE; layoutRegistroDueño.visibility = View.GONE; layoutLoginDueño.visibility = View.GONE
        layoutLoginCajero.visibility = View.GONE; layoutBienvenida.visibility = View.GONE; layoutPermiso1Lector.visibility = View.GONE
        layoutPermiso2Alertas.visibility = View.GONE; layoutPermiso3Bateria.visibility = View.GONE; layoutAppPrincipal.visibility = View.GONE
        v.visibility = View.VISIBLE
    }

    private fun verificarAsistentePermisosOEntrar() {
        if (getSharedPreferences("YapePrefs", Context.MODE_PRIVATE).getBoolean("asistente_permisos_listo", false)) entrarDirectoAlSistema()
        else mostrarSeccion(layoutPermiso1Lector)
    }

    private fun entrarDirectoAlSistema() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val nom = prefs.getString("nombre_tienda", "Mi Tienda")
        val cod = prefs.getString("codigo_tienda", "")
        val caj = prefs.getString("cajero_activo", if (esSesionDueño) "Dueño (Admin)" else "Cajero")
        txtInfoCajero.text = if (esSesionDueño) "🏪 $nom | 👑 Dueño" else "🏪 $nom | 👤 $caj ($horaInicioTurnoStr)"
        cardFiltroFechas.visibility = if (esSesionDueño) View.VISIBLE else View.GONE
        txtBannerCodigoCajeros.text = "🔑 $cod"
        btnNavCajeros.visibility = if (esSesionDueño) View.VISIBLE else View.GONE
        btnNavConfig.visibility = if (esSesionDueño) View.VISIBLE else View.GONE
        mostrarSeccion(layoutAppPrincipal)
        seleccionarPestaña(1)
        iniciarSincronizador()
        recargarListaCajeros(prefs)
        renderizarVistaQR()
        verificarYMostrarOptimizaciones(alEntrar = true)
    }

    private fun seleccionarPestaña(p: Int) {
        tabCajaPOS.visibility = if (p == 1) View.VISIBLE else View.GONE
        tabCajeros.visibility = if (p == 2) View.VISIBLE else View.GONE
        tabConfiguracion.visibility = if (p == 3) View.VISIBLE else View.GONE
        val cAct = COLOR_PRIMARY
        val cInact = if (esModoOscuro) "#1E293B" else "#FFFFFF"
        btnNavCaja.background = crearBordeRedondo(if (p == 1) cAct else cInact, if (p == 1) cAct else cInact, 18f)
        btnNavCaja.setTextColor(if (p == 1) Color.WHITE else Color.parseColor("#64748B"))
        btnNavCajeros.background = crearBordeRedondo(if (p == 2) cAct else cInact, if (p == 2) cAct else cInact, 18f)
        btnNavCajeros.setTextColor(if (p == 2) Color.WHITE else Color.parseColor("#64748B"))
        btnNavConfig.background = crearBordeRedondo(if (p == 3) cAct else cInact, if (p == 3) cAct else cInact, 18f)
        btnNavConfig.setTextColor(if (p == 3) Color.WHITE else Color.parseColor("#64748B"))
        if (p == 3) actualizarDiagnosticoAjustes()
    }

    private fun actualizarDiagnosticoAjustes() {
        val tLector = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
        badgeLectorStatus.text = "• Lector Yape/Plin: " + if (tLector) "🟢 ACTIVO" else "🔴 DESACTIVADO (Toca)"
        badgeLectorStatus.setTextColor(Color.parseColor(if (tLector) "#15803D" else "#DC2626"))
        badgeLectorStatus.background = crearBordeRedondo(if (tLector) "#ECFDF5" else "#FEF2F2", if (tLector) "#A7F3D0" else "#FCA5A5", 12f)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val tBat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.isIgnoringBatteryOptimizations(packageName) else true
        badgeBateriaStatus.text = "• Segundo Plano: " + if (tBat) "🟢 ACTIVO" else "🔴 DESACTIVADO (Toca)"
        badgeBateriaStatus.setTextColor(Color.parseColor(if (tBat) "#15803D" else "#DC2626"))
        badgeBateriaStatus.background = crearBordeRedondo(if (tBat) "#ECFDF5" else "#FEF2F2", if (tBat) "#A7F3D0" else "#FCA5A5", 12f)

        val tNot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true
        badgeNotifStatus.text = "• Alertas en Pantalla: " + if (tNot) "🟢 ACTIVADO" else "🔴 DESACTIVADO (Toca)"
        badgeNotifStatus.setTextColor(Color.parseColor(if (tNot) "#15803D" else "#DC2626"))
        badgeNotifStatus.background = crearBordeRedondo(if (tNot) "#ECFDF5" else "#FEF2F2", if (tNot) "#A7F3D0" else "#FCA5A5", 12f)
    }

    private fun actualizarVistaSaldo() {
        txtTotalHoyCaja.text = if (saldoOculto) "S/ ••••••" else String.format(Locale.US, "S/ %.2f", totalVentasFiltradas)
        btnOcultarSaldo.text = if (saldoOculto) "👁️‍🗨️" else "👁️"
    }

    private fun verificarConexionInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cap = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
            return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        @Suppress("DEPRECATION")
        return cm.activeNetworkInfo?.isConnected ?: false
    }

    private fun probarAudioCaja() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                delay(220); tg.release()
            } catch (e: Exception) {}
            if (vozLista && motorVozCajero != null) {
                motorVozCajero?.speak("Prueba de audio correcta. YapeAlDía lista para cobrar.", TextToSpeech.QUEUE_FLUSH, null, "TEST_TTS")
            }
        }
    }

    private fun confirmarCierreDeTurno() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val caj = prefs.getString("cajero_activo", "Cajero")
        val hCierre = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val msg = "📊 ARQUEO DE CAJA:\n\n👤 Cajero: $caj\n⏰ Turno: $horaInicioTurnoStr a $hCierre\n💰 Total: S/ ${String.format(Locale.US, "%.2f", totalVentasFiltradas)}\n📋 Ventas: $cantidadVentasFiltradas cobros\n\n¿Deseas enviar tu cierre por WhatsApp y cerrar sesión?"
        AlertDialog.Builder(this).setTitle("Cierre de Turno 🔒").setMessage(msg)
            .setPositiveButton("Enviar y Salir") { _, _ -> enviarCuadrePorWhatsApp(); cerrarSesionCajaFinal() }
            .setNeutralButton("Solo Salir") { _, _ -> cerrarSesionCajaFinal() }
            .setNegativeButton("Continuar", null).show()
    }

    private fun cerrarSesionCajaFinal() {
        jobSincronizador?.cancel()
        getSharedPreferences("YapePrefs", Context.MODE_PRIVATE).edit().putBoolean("cajero_logueado", false).apply()
        timestampInicioTurnoCajero = 0L
        mostrarSeccion(layoutPortada)
    }

    private fun enviarCuadrePorWhatsApp() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val nom = prefs.getString("nombre_tienda", "Mi Tienda")
        val caj = prefs.getString("cajero_activo", if (esSesionDueño) "Dueño" else "Cajero")
        val fHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val hAct = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val msg = "📊 *CIERRE DE CAJA - YapeAlDía*\n\n🏪 *Negocio:* $nom\n📅 *Fecha:* $fHoy ($hAct)\n👤 *Responsable:* $caj\n⏰ *Turno:* $horaInicioTurnoStr a $hAct\n💰 *Total:* S/ ${String.format(Locale.US, "%.2f", totalVentasFiltradas)}\n📋 *Operaciones:* $cantidadVentasFiltradas\n\n✅ _Reporte oficial YapeAlDía._"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg))))
    }

    private fun exportarHistorialCSV() {
        val db = OfflineDatabaseHelper(this)
        val array = db.obtenerHistorialComoJsonArray(200)
        val sb = StringBuilder()
        sb.append("ID,Fecha,Hora,Cliente,Metodo,Monto_PEN,Cajero,Estado_Sincronizacion\n")
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val syncTxt = if (item.optBoolean("sincronizado", true)) "Sincronizado en Nube" else "Guardado Local SQLite"
            sb.append("${item.optLong("id")},${item.optString("fecha")},${item.optString("hora")},\"${item.optString("nombre")}\",${item.optString("metodo")},${item.optString("monto")},${item.optString("cajero")},$syncTxt\n")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Historial Contable YapeAlDía CSV")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Exportar CSV Contable"))
    }

    private fun abrirSelectorCalendario() {
        val cal = Calendar.getInstance()
        val listener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            fechaSeleccionadaFiltro = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, year)
            txtTituloPeriodo.text = "📅 Período: $fechaSeleccionadaFiltro"
            consultarCobrosServidorUnaVez()
        }
        val dpd = DatePickerDialog(
            this,
            listener,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show()
    }

    private fun mostrarDivulgacionProminenteNotificaciones() {
        AlertDialog.Builder(this)
            .setTitle("Divulgación de Privacidad y Notificaciones - YapeAlDía 🔔")
            .setMessage("YapeAlDía requiere acceso a las notificaciones para detectar y cantar los cobros de Yape y Plin automáticamente.\n\n• NO lee mensajes privados ni WhatsApp.\n• NO accede a contraseñas ni saldos bancarios.\n• Solo anuncia el cobro por altavoz y actualiza tu caja.\n• Ningún dato personal se comercializa.")
            .setPositiveButton("Aceptar y Configurar") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAjustesRestringidos() {
        AlertDialog.Builder(this).setTitle("Desbloquear Ajuste Restringido 🛡️")
            .setMessage("1. Toca 'Ir a Ajustes'.\n2. Toca los 3 puntos arriba (⋮).\n3. Selecciona 'Permitir ajustes restringidos' e introduce tu PIN/huella.\n\nRegresa y activa el lector normalmente.")
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:$packageName") })
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun solicitarSinRestriccionesBateria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") })
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            } else {
                Toast.makeText(this, "✅ YapeAlDía ya está optimizada para segundo plano 24/7.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarYMostrarOptimizaciones(alEntrar: Boolean = true) {
        // Iniciar el servicio en primer plano 24/7 para que funcione con pantalla apagada o minimizado
        try {
            val srvIntent = Intent(this, YapeListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(srvIntent)
            } else {
                startService(srvIntent)
            }
        } catch (e: Exception) {}

        val tLector = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val tBat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.isIgnoringBatteryOptimizations(packageName) else true
        val tNot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true

        val completados = (if (tLector) 1 else 0) + (if (tNot) 1 else 0) + (if (tBat) 1 else 0)

        if (tLector && tBat && tNot) {
            if (!alEntrar) {
                Toast.makeText(this, "✅ Escudo 24/7 Activo (3 de 3 listos). Funciona con pantalla apagada o minimizada.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
        }

        val txtTit = TextView(this).apply {
            text = "🛡️ Escudo 24/7 (Dueño y Cajero) • $completados de 3 listos"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(COLOR_PRIMARY))
        }
        val txtDesc = TextView(this).apply {
            text = "Activa estos 3 permisos para que YapeAlDía cante y guarde tus cobros aunque la app esté minimizada o con la pantalla apagada en el bolsillo:"
            textSize = 13f
            setTextColor(Color.parseColor(if (esModoOscuro) "#CBD5E1" else "#475569"))
            setPadding(0, 10, 0, 15)
        }
        lay.addView(txtTit)
        lay.addView(txtDesc)

        val btnLector = crearBotonPrimario(
            this,
            if (tLector) "✅ 1. Lector Yape y Plin (Activado)" else "🔔 1. Activar Lector Yape y Plin"
        ) {
            mostrarDivulgacionProminenteNotificaciones()
        }.apply {
            background = crearBordeRedondo(if (tLector) "#10B981" else "#742284", if (tLector) "#059669" else "#742284", 16f)
        }
        lay.addView(btnLector)

        val btnNotif = crearBotonPrimario(
            this,
            if (tNot) "✅ 2. Barra Guardia 24/7 y Alertas (Permitido)" else "📢 2. Permitir Notificaciones y Guardia 24/7"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            } else {
                Toast.makeText(this@MainActivity, "✅ Permitido en esta versión de Android", Toast.LENGTH_SHORT).show()
            }
        }.apply {
            background = crearBordeRedondo(if (tNot) "#10B981" else "#0284C7", if (tNot) "#059669" else "#0284C7", 16f)
        }
        lay.addView(btnNotif)

        val btnBateria = crearBotonPrimario(
            this,
            if (tBat) "✅ 3. Batería Sin Restricciones 24/7 (Listo)" else "🔋 3. Permitir Segundo Plano con Pantalla Apagada"
        ) {
            solicitarSinRestriccionesBateria()
        }.apply {
            background = crearBordeRedondo(if (tBat) "#10B981" else "#D97706", if (tBat) "#059669" else "#D97706", 16f)
        }
        lay.addView(btnBateria)

        AlertDialog.Builder(this)
            .setView(lay)
            .setPositiveButton("Continuar a mi Caja 🚀") { d, _ -> d.dismiss() }
            .show()
    }

    private fun dialogCambiarPin(prefs: android.content.SharedPreferences) {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 20, 50, 10) }
        val inAct = EditText(this).apply { hint = "PIN Actual"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val inNue = EditText(this).apply { hint = "Nuevo PIN (4 dígitos)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        lay.addView(inAct); lay.addView(inNue)
        AlertDialog.Builder(this).setTitle("Cambiar PIN de Dueño").setView(lay).setPositiveButton("Actualizar") { _, _ ->
            val act = inAct.text.toString().trim()
            val nue = inNue.text.toString().trim()
            if (act == prefs.getString("pin_dueño", "1234") && nue.length >= 4) {
                prefs.edit().putString("pin_dueño", nue).apply()
                Toast.makeText(this, "✅ PIN actualizado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "PIN actual incorrecto o nuevo inválido", Toast.LENGTH_SHORT).show()
            }
        }.setNegativeButton("Cancelar", null).show()
    }

    private fun abrirGaleriaParaQR() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQ_SELECCIONAR_IMAGEN_QR)
    }

    private fun procesarImagenQrSeleccionada(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show() }
                    return@launch
                }

                val intArray = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = QRCodeReader()
                val result = reader.decode(binaryBitmap)
                val textoQr = result.text

                runOnUiThread {
                    qrTemporalDetectado = textoQr
                    txtPreviaQrDetectado.text = "✅ QR de Yape reconocido con éxito.\nAhora toca 'Guardar mi QR de Yape 💾' para vincularlo a tu caja."
                    txtPreviaQrDetectado.setTextColor(Color.parseColor("#15803D"))
                    btnGuardarQrDeYape.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, "¡QR de Yape detectado! Toca 'Guardar mi QR de Yape 💾'", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "No se encontró un código QR válido en la imagen. Intenta con una captura más nítida.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun guardarQrSeleccionado() {
        if (qrTemporalDetectado.isEmpty()) {
            Toast.makeText(this, "Primero selecciona una foto de tu QR", Toast.LENGTH_SHORT).show()
            return
        }
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        var cod = prefs.getString("codigo_tienda", "") ?: ""
        if (cod.isEmpty()) {
            cod = "TIENDA-YAPE-" + (1000..9999).random()
            prefs.edit().putString("codigo_tienda", cod).apply()
        }
        prefs.edit().putString("qr_base_yape", qrTemporalDetectado).apply()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("accion", "guardar_qr")
                    put("codigoTienda", cod)
                    put("qrBase", qrTemporalDetectado)
                }
                val b = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                client.newCall(Request.Builder().url(SERVIDOR_URL).post(b).build()).execute().close()
            } catch (e: Exception) {}
        }

        AlertDialog.Builder(this)
            .setTitle("¡Se creó el ID de su QR correctamente! 🎉")
            .setMessage("Su código QR de Yape ha quedado vinculado con éxito a su negocio.\n\nID de su QR: $cod\n\nSu caja en YapeAlDía ya está lista para generar cobros dinámicos con monto exacto.")
            .setPositiveButton("Continuar a Cobrar 🛒") { _, _ -> }
            .show()

        renderizarVistaQR()
    }

    private fun renderizarVistaQR() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val qrGuardado = prefs.getString("qr_base_yape", "") ?: ""
        val codTienda = prefs.getString("codigo_tienda", "") ?: ""

        if (qrGuardado.isNotEmpty()) {
            layoutAccionAdjuntarQr.visibility = View.GONE
            layoutEstadoQrGuardado.visibility = View.VISIBLE
            txtEstadoQrGuardado.text = "✅ QR DE YAPE VINCULADO Y ACTIVO\n\n🔑 ID DE SU QR: $codTienda\n\nTu caja ya genera cobros automáticos con monto exacto."
        } else {
            layoutAccionAdjuntarQr.visibility = View.VISIBLE
            layoutEstadoQrGuardado.visibility = View.GONE
            btnGuardarQrDeYape.visibility = if (qrTemporalDetectado.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res == RESULT_OK && data != null) {
            val acc = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!acc.isNullOrEmpty()) {
                if (req == REQ_GOOGLE_PICKER_REGISTRO) inCorreoRegistro.setText(acc)
                else if (req == REQ_GOOGLE_PICKER_LOGIN) inCorreoLoginDueño.setText(acc)
            }
            if (req == REQ_SELECCIONAR_IMAGEN_QR) {
                data.data?.let { uri -> procesarImagenQrSeleccionada(uri) }
            }
        }
    }

    private fun iniciarSincronizador() {
        jobSincronizador?.cancel()
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val cod = prefs.getString("codigo_tienda", "") ?: ""
        val dbHelper = OfflineDatabaseHelper(this)

        jobSincronizador = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val hayNet = verificarConexionInternet()
                val pendientesOffline = dbHelper.contarPendientes()

                withContext(Dispatchers.Main) {
                    if (!hayNet) {
                        bannerSinInternet.text = "📡 MODO SIN SEÑAL • Guardando cobros en SQLite local ($pendientesOffline pendientes de sincronizar)"
                        bannerSinInternet.visibility = View.VISIBLE
                    } else {
                        bannerSinInternet.visibility = View.GONE
                    }
                }

                if (hayNet) {
                    // 1. Si hay cobros almacenados en SQLite durante el corte de internet, sincronizarlos a la nube ahora
                    val listaPendientes = dbHelper.obtenerCobrosPendientes()
                    if (listaPendientes.isNotEmpty()) {
                        var sincronizadosAhora = 0
                        for (item in listaPendientes) {
                            try {
                                val json = JSONObject().apply {
                                    put("codigoTienda", item.codigoTienda.ifEmpty { cod })
                                    put("nombre", if (item.nombre.contains(item.metodo)) item.nombre else "${item.nombre} (${item.metodo})")
                                    put("monto", item.monto)
                                    put("cajero", item.cajero)
                                    put("fecha", item.fecha)
                                    put("hora", item.hora)
                                    put("timestamp", item.timestamp)
                                }
                                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                val resSync = client.newCall(Request.Builder().url(SERVIDOR_URL).post(body).build()).execute()
                                if (resSync.isSuccessful) {
                                    dbHelper.marcarComoSincronizado(item.idLocal)
                                    sincronizadosAhora++
                                }
                                resSync.close()
                            } catch (e: Exception) {
                                break
                            }
                        }
                        if (sincronizadosAhora > 0) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "☁️ $sincronizadosAhora cobro(s) guardado(s) sin señal fueron sincronizados con la nube",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    // 2. Consultar cobros actualizados desde el servidor en la nube
                    try {
                        val url = "$SERVIDOR_URL?accion=consultar_cobros&codigoTienda=$cod&fechaFiltro=$fechaSeleccionadaFiltro"
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
                                        reproducirTimbreYVozLocal(monto)
                                    }
                                    imgQrPOS.visibility = View.GONE
                                    txtMontoQrPOS.visibility = View.GONE
                                    montoTecladoPOS = ""
                                    txtMontoDisplayPOS.text = "S/ 0.00"
                                    Toast.makeText(this@MainActivity, "✅ ¡Cobro confirmado! S/ $monto de $nombre", Toast.LENGTH_LONG).show()
                                }
                            }
                            withContext(Dispatchers.Main) { actualizarListaCaja(array) }
                        }
                    } catch (e: Exception) {}
                } else {
                    // 3. MODO SIN SEÑAL (OFFLINE): Mostrar en pantalla los cobros almacenados en SQLite local
                    val localArray = dbHelper.obtenerHistorialComoJsonArray(50)
                    withContext(Dispatchers.Main) { actualizarListaCaja(localArray) }
                }
                delay(2500)
            }
        }
    }

    private fun reproducirTimbreYVozLocal(monto: String) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                delay(220); tg.release()
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
                val url = "$SERVIDOR_URL?accion=consultar_cobros&codigoTienda=$cod&fechaFiltro=$fechaSeleccionadaFiltro"
                val res = client.newCall(Request.Builder().url(url).get().build()).execute()
                val array = JSONArray(res.body?.string() ?: "[]")
                res.close()
                withContext(Dispatchers.Main) { actualizarListaCaja(array) }
            } catch (e: Exception) {}
        }
    }

    private fun actualizarListaCaja(array: JSONArray) {
        layoutListaCobrosCaja.removeAllViews()
        var suma = 0.0
        var conteo = 0
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        for (i in 0 until minOf(array.length(), 25)) {
            val item = array.getJSONObject(i)
            val mStr = item.optString("monto", "0.00")
            val fStr = item.optString("fecha", "")
            val tsItem = item.optLong("timestamp", 0L)

            if (!esSesionDueño) {
                if (timestampInicioTurnoCajero > 0L && tsItem < timestampInicioTurnoCajero) continue
            } else {
                if (fechaSeleccionadaFiltro.isNotEmpty() && fStr != fechaSeleccionadaFiltro) continue
                else if (fechaSeleccionadaFiltro.isEmpty() && fStr != fechaHoy) continue
            }

            val montoLimpio = mStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            suma += montoLimpio
            conteo++

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(14, 10, 14, 10)
                background = crearBordeRedondo(if (esModoOscuro) "#1E293B" else "#FFFFFF", "#E2E8F0", 12f)
                elevation = 2f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 6) }
            }
            row.addView(TextView(this).apply {
                text = "${item.optString("nombre")} (${item.optString("hora")})"
                textSize = 14f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = String.format(Locale.US, "+ S/ %.2f", montoLimpio)
                textSize = 15f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(COLOR_PRIMARY))
            })
            layoutListaCobrosCaja.addView(row)
        }
        totalVentasFiltradas = suma
        cantidadVentasFiltradas = conteo
        actualizarVistaSaldo()
    }

    private fun pedirPinDueño(prefs: android.content.SharedPreferences, onOk: () -> Unit) {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 20, 50, 10) }
        val inP = EditText(this).apply { hint = "PIN (4 dígitos)"; textSize = 18f; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD; gravity = Gravity.CENTER }
        val btnOlv = Button(this).apply {
            text = "¿Olvidaste tu PIN? Recupéralo por correo"
            textSize = 13f; setTextColor(Color.parseColor("#2563EB")); setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { solicitarRecuperacionPinPorCorreo(prefs.getString("correo_tienda", "") ?: "") }
        }
        lay.addView(inP); lay.addView(btnOlv)
        val d = AlertDialog.Builder(this).setTitle("Acceso Administrador 👑").setView(lay)
            .setPositiveButton("Ingresar", null).setNegativeButton("Cancelar", null).create()
        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (inP.text.toString().trim() == prefs.getString("pin_dueño", "1234")) { d.dismiss(); onOk() }
                else Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
            }
        }
        d.show()
    }

    private fun solicitarRecuperacionPinPorCorreo(correo: String) {
        AlertDialog.Builder(this).setTitle("Recuperar PIN").setMessage("¿Enviar PIN a $correo?")
            .setPositiveButton("Enviar") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = JSONObject().apply { put("accion", "recuperar_pin"); put("correo", correo) }
                        val b = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                        val res = client.newCall(Request.Builder().url(SERVIDOR_URL).post(b).build()).execute()
                        val rJ = JSONObject(res.body?.string() ?: "")
                        res.close()
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, if (rJ.optString("status") == "exito") "✅ PIN enviado a tu correo" else rJ.optString("mensaje"), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {}
                }
            }.setNegativeButton("Cancelar", null).show()
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
                textSize = 14f; setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#334155"))
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
        } catch (e: Exception) { null }
    }

    override fun onInit(s: Int) {
        if (s == TextToSpeech.SUCCESS) {
            motorVozCajero?.language = Locale("es", "PE")
            motorVozCajero?.setPitch(1.04f); motorVozCajero?.setSpeechRate(1.02f)
            vozLista = true
        }
    }

    override fun onDestroy() {
        jobSincronizador?.cancel(); motorVozCajero?.stop(); motorVozCajero?.shutdown()
        super.onDestroy()
    }

    // FACTORÍA VISUAL FINTECH
    private fun crearTarjetaFintech(ctx: Context) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL; setPadding(30, 25, 30, 25); elevation = 10f
        background = GradientDrawable().apply {
            setColor(Color.parseColor(if (esModoOscuro) "#1E293B" else "#FFFFFF"))
            cornerRadius = 24f; setStroke(2, Color.parseColor(if (esModoOscuro) "#334155" else "#F1E4F5"))
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 16) }
    }

    private fun crearBordeRedondo(cFondo: String, cBorde: String, rad: Float) = GradientDrawable().apply {
        setColor(Color.parseColor(cFondo)); cornerRadius = rad; setStroke(2, Color.parseColor(cBorde))
    }

    private fun crearBotonPrimario(ctx: Context, txt: String, onClick: () -> Unit) = Button(ctx).apply {
        text = txt; setTextColor(Color.WHITE); textSize = 14f; setTypeface(null, Typeface.BOLD); isAllCaps = false; elevation = 6f
        val shape = GradientDrawable().apply { setColor(Color.parseColor(COLOR_PRIMARY)); cornerRadius = 18f }
        background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null) else shape
        setPadding(20, 16, 20, 16); setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }
    }

    private fun crearBotonSecundario(ctx: Context, txt: String, onClick: () -> Unit) = Button(ctx).apply {
        text = txt; setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A")); textSize = 13f; setTypeface(null, Typeface.BOLD); isAllCaps = false
        val shape = GradientDrawable().apply { setColor(Color.parseColor(if (esModoOscuro) "#334155" else "#F1F5F9")); cornerRadius = 16f; setStroke(2, Color.parseColor("#CBD5E1")) }
        background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) RippleDrawable(ColorStateList.valueOf(Color.parseColor("#1A000000")), shape, null) else shape
        setPadding(16, 14, 16, 14); setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 6, 0, 6) }
    }

    private fun crearInputFintech(ctx: Context, hTxt: String) = EditText(ctx).apply {
        hint = hTxt; setHintTextColor(Color.parseColor("#94A3B8"))
        setTextColor(Color.parseColor(if (esModoOscuro) "#F8FAFC" else "#0F172A")); textSize = 15f
        background = GradientDrawable().apply {
            setColor(Color.parseColor(if (esModoOscuro) "#0F172A" else "#F8FAFC"))
            cornerRadius = 16f; setStroke(2, Color.parseColor(if (esModoOscuro) "#334155" else "#E2E8F0"))
        }
        setPadding(26, 18, 26, 18)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 4, 0, 10) }
    }
}
