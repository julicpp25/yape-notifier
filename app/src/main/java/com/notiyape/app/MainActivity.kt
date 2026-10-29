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

    // VISTAS
    private lateinit var layoutRolSelector: LinearLayout
    private lateinit var layoutRegistroDueño: LinearLayout
    private lateinit var layoutLoginCajero: LinearLayout
    private lateinit var layoutCajeroPos: LinearLayout
    private lateinit var layoutAdminPanel: LinearLayout

    // CAJERO
    private lateinit var txtInfoCajero: TextView
    private lateinit var imgQr: ImageView
    private lateinit var txtMontoQr: TextView
    private lateinit var switchVozCajero: Switch
    private lateinit var layoutListaCobros: LinearLayout

    // ADMIN
    private lateinit var txtTotalAdmin: TextView
    private lateinit var layoutListaCajerosAdmin: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 45, 35, 45)
        }
        scroll.addView(mainContainer)

        // =========================================================================
        // PANTALLA 0: SELECTOR DE ROL (¿Quién está abriendo la app en este celular?)
        // =========================================================================
        layoutRolSelector = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardRol = crearTarjeta(this)

        val titRol = TextView(this).apply {
            text = "Bienvenido a NotiCobros 🇵🇪"
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

        val btnSoyDueño = crearBoton(this, "👑 Soy el Dueño (Administrar)", "#0F172A") {
            if (prefs.getBoolean("tienda_registrada", false)) {
                // Si la tienda ya está registrada en este cel, pide PIN
                pedirPinDueño(prefs) {
                    mostrarVista(5) // Va al panel admin
                }
            } else {
                mostrarVista(2) // Va al registro inicial
            }
        }

        val btnSoyCajero = crearBoton(this, "🛒 Soy Cajero (Ingresar a Caja)", "#00A859") {
            mostrarVista(3) // Va al login de cajero
        }

        cardRol.addView(titRol)
        cardRol.addView(descRol)
        cardRol.addView(btnSoyDueño)
        cardRol.addView(btnSoyCajero)
        layoutRolRol(cardRol)
        layoutRolSelector.addView(cardRol)

        // =========================================================================
        // PANTALLA 1: REGISTRO DEL DUEÑO (Configuración inicial)
        // =========================================================================
        layoutRegistroDueño = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardReg = crearTarjeta(this)

        val titReg = TextView(this).apply {
            text = "Registro de Tienda (Solo Dueño) 👑"
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
        val inQrPayload = crearInput(this, "Pega el texto de tu QR de Yape (Opcional)")

        val btnCrearTienda = crearBoton(this, "Guardar Tienda y Vincular Telegram ➡️", "#00A859") {
            val nom = inNom.text.toString().trim()
            val cor = inCor.text.toString().trim()
            val pin = inPin.text.toString().trim()
            val qrRaw = inQrPayload.text.toString().trim()

            if (nom.isEmpty() || cor.isEmpty() || pin.length < 4) {
                Toast.makeText(this, "Completa todos los campos (PIN mín. 4 dígitos)", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            val codGenerado = nom.replace(" ", "").take(5).uppercase() + "-" + (100..999).random()

            prefs.edit()
                .putString("nombre_tienda", nom)
                .putString("correo_tienda", cor)
                .putString("codigo_tienda", codGenerado)
                .putString("pin_dueño", pin)
                .putString("qr_base_yape", qrRaw)
                .putBoolean("tienda_registrada", true)
                .apply()

            // Abrir Telegram para vincular alertas
            val urlTelegram = "https://t.me/$BOT_USERNAME?start=$codGenerado"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlTelegram)))

            Toast.makeText(this, "Tienda creada. Código: $codGenerado", Toast.LENGTH_LONG).show()
            mostrarVista(5) // Abre panel de control del dueño
        }

        cardReg.addView(titReg)
        cardReg.addView(inNom)
        cardReg.addView(inCor)
        cardReg.addView(inPin)
        cardReg.addView(inQrPayload)
        cardReg.addView(btnCrearTienda)
        layoutRegistroDueño.addView(cardReg)

        // =========================================================================
        // PANTALLA 2: LOGIN DEL CAJERO (En el celular que usará el cajero)
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
        val inNomCajero = crearInput(this, "Tu Nombre (Ej: Carlos - Turno Tarde)")
        val inPinCajero = crearInput(this, "PIN de Caja").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val btnEntrarCaja = crearBoton(this, "Iniciar Turno de Caja 🚀", "#00A859") {
            val cod = inCodTienda.text.toString().trim()
            val caj = inNomCajero.text.toString().trim()
            val pin = inPinCajero.text.toString().trim()

            if (cod.isEmpty() || caj.isEmpty() || pin.isEmpty()) {
                Toast.makeText(this, "Completa todos los datos para entrar", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            // Validar PIN de cajero o PIN maestro
            val listaCajeros = JSONArray(prefs.getString("lista_cajeros", "[]") ?: "[]")
            var accesoPermitido = (pin == prefs.getString("pin_dueño", "1234")) // El dueño siempre puede entrar

            for (i in 0 until listaCajeros.length()) {
                val c = listaCajeros.getJSONObject(i)
                if (c.getString("nombre").equals(caj, true) && c.getString("pin") == pin) {
                    accesoPermitido = true
                    break
                }
            }

            if (accesoPermitido) {
                prefs.edit()
                    .putString("codigo_tienda", cod)
                    .putString("cajero_activo", caj)
                    .putBoolean("cajero_logueado", true)
                    .apply()

                txtInfoCajero.text = "👤 $caj | Tienda: $cod"
                mostrarVista(4) // Entra al POS de cobro
            } else {
                Toast.makeText(this, "PIN o usuario incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        cardLogin.addView(titLog)
        cardLogin.addView(inCodTienda)
        cardLogin.addView(inNomCajero)
        cardLogin.addView(inPinCajero)
        cardLogin.addView(btnEntrarCaja)
        layoutLoginCajero.addView(cardLogin)

        // =========================================================================
        // PANTALLA 3: PUNTO DE VENTA (POS CON QR VÁLIDO Y VOZ)
        // =========================================================================
        layoutCajeroPos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        // Cabecera cajero
        val cabeceraCaja = LinearLayout(this).apply {
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
        val btnSalirTurno = Button(this).apply {
            text = "Cerrar Turno"
            textSize = 10f
            setTextColor(Color.WHITE)
            background = crearBordeRedondo("#64748B", "#64748B", 12f)
            setPadding(15, 8, 15, 8)
            setOnClickListener {
                prefs.edit().putBoolean("cajero_logueado", false).apply()
                mostrarVista(1) // Vuelve al inicio
            }
        }
        cabeceraCaja.addView(txtInfoCajero)
        cabeceraCaja.addView(switchVozCajero)
        cabeceraCaja.addView(btnSalirTurno)

        val cardCobroPos = crearTarjeta(this)
        val inMontoCobro = EditText(this).apply {
            hint = "S/ 0.00"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = crearBordeRedondo("#F8FAFC", "#CBD5E1", 16f)
            setPadding(20, 20, 20, 20)
        }
        imgQr = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(480, 480).apply {
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

        val btnGenerarQrValido = crearBoton(this, "⚡ Generar QR de Cobro", "#00A859") {
            val monto = inMontoCobro.text.toString().trim()
            if (monto.isEmpty()) {
                Toast.makeText(this, "Digita el monto a cobrar", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            val qrBase = prefs.getString("qr_base_yape", "") ?: ""
            val payloadFinal = if (qrBase.length > 30) {
                // Inyectar monto en EMVCo oficial con CRC16
                armarQrEmvcoConMonto(qrBase, monto)
            } else {
                qrBase
            }

            val qrBmp = generarBitmapQr(payloadFinal, 480, 480)
            if (qrBmp != null) {
                imgQr.setImageBitmap(qrBmp)
                imgQr.visibility = View.VISIBLE
                txtMontoQr.text = "Escanea para pagar: S/ $monto"
                txtMontoQr.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "El dueño debe configurar el QR oficial en ajustes de Admin", Toast.LENGTH_LONG).show()
            }
        }

        cardCobroPos.addView(inMontoCobro)
        cardCobroPos.addView(btnGenerarQrValido)
        cardCobroPos.addView(imgQr)
        cardCobroPos.addView(txtMontoQr)

        val cardCobrosRecientes = crearTarjeta(this)
        val titRec = TextView(this).apply {
            text = "📋 Cobros Recibidos en este Turno:"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }
        layoutListaCobros = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cardCobrosRecientes.addView(titRec)
        cardCobrosRecientes.addView(layoutListaCobros)

        layoutCajeroPos.addView(cabeceraCaja)
        layoutCajeroPos.addView(cardCobroPos)
        layoutCajeroPos.addView(cardCobrosRecientes)

        // =========================================================================
        // PANTALLA 4: PANEL ADMINISTRADOR (SOLO DUEÑO CON PIN)
        // =========================================================================
        layoutAdminPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardAdm = crearTarjeta(this)

        val titAdm = TextView(this).apply {
            text = "Panel de Administración 👑"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 15)
        }

        val btnSalirAdmin = Button(this).apply {
            text = "⬅️ Salir al Inicio"
            setOnClickListener { mostrarVista(1) }
        }

        val btnVerSheet = crearBoton(this, "📊 Abrir mi Google Sheets en vivo", "#00A859") {
            val url = prefs.getString("sheet_url_cliente", "") ?: ""
            if (url.isNotEmpty()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            else Toast.makeText(this, "Revisa el enlace en tu Telegram", Toast.LENGTH_SHORT).show()
        }

        // SECCIÓN CREAR CAJEROS
        val titCaj = TextView(this).apply {
            text = "\n👥 Gestión de Cajeros por Turnos:"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 10, 0, 10)
        }
        val inNuevoCajeroNom = crearInput(this, "Nombre del Cajero (Ej: María Lopez)")
        val inNuevoCajeroPin = crearInput(this, "PIN de 4 dígitos para este Cajero")
        val btnCrearCajero = crearBoton(this, "➕ Crear Cajero", "#0F172A") {
            val n = inNuevoCajeroNom.text.toString().trim()
            val p = inNuevoCajeroPin.text.toString().trim()
            if (n.isNotEmpty() && p.length >= 4) {
                agregarCajero(prefs, n, p)
                inNuevoCajeroNom.setText("")
                inNuevoCajeroPin.setText("")
                Toast.makeText(this, "Cajero registrado con éxito", Toast.LENGTH_SHORT).show()
                recargarCajerosAdmin(prefs)
            } else {
                Toast.makeText(this, "Ingresa nombre y PIN de mín. 4 números", Toast.LENGTH_SHORT).show()
            }
        }

        layoutListaCajerosAdmin = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        cardAdm.addView(titAdm)
        cardAdm.addView(btnSalirAdmin)
        cardAdm.addView(btnVerSheet)
        cardAdm.addView(titCaj)
        cardAdm.addView(inNuevoCajeroNom)
        cardAdm.addView(inNuevoCajeroPin)
        cardAdm.addView(btnCrearCajero)
        cardAdm.addView(layoutListaCajerosAdmin)
        layoutAdminPanel.addView(cardAdm)

        // AGREGAR TODAS LAS PANTALLAS
        mainContainer.addView(layoutRolSelector)
        mainContainer.addView(layoutRegistroDueño)
        mainContainer.addView(layoutLoginCajero)
        mainContainer.addView(layoutCajeroPos)
        mainContainer.addView(layoutAdminPanel)

        setContentView(scroll)

        // Decidir qué pantalla mostrar al inicio
        if (prefs.getBoolean("cajero_logueado", false)) {
            txtInfoCajero.text = "👤 " + prefs.getString("cajero_activo", "") + " | Tienda: " + prefs.getString("codigo_tienda", "")
            mostrarVista(4) // Directo a la caja
        } else {
            mostrarVista(1) // Selector inicial
        }
    }

    private fun layoutRolRol(card: LinearLayout) {}

    private fun mostrarVista(vista: Int) {
        layoutRolSelector.visibility = if (vista == 1) View.VISIBLE else View.GONE
        layoutRegistroDueño.visibility = if (vista == 2) View.VISIBLE else View.GONE
        layoutLoginCajero.visibility = if (vista == 3) View.VISIBLE else View.GONE
        layoutCajeroPos.visibility = if (vista == 4) View.VISIBLE else View.GONE
        layoutAdminPanel.visibility = if (vista == 5) View.VISIBLE else View.GONE

        if (vista == 4) cargarHistorialCajero()
        if (vista == 5) recargarCajerosAdmin(getSharedPreferences("YapePrefs", Context.MODE_PRIVATE))
    }

    private fun pedirPinDueño(prefs: android.content.SharedPreferences, onCorrecto: () -> Unit) {
        val inPin = EditText(this).apply {
            hint = "PIN de Dueño"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }
        AlertDialog.Builder(this)
            .setTitle("Acceso Administrador")
            .setMessage("Ingresa el PIN de 4 dígitos creado al registrar la tienda:")
            .setView(inPin)
            .setPositiveButton("Ingresar") { _, _ ->
                val ingresado = inPin.text.toString().trim()
                val guardado = prefs.getString("pin_dueño", "1234")
                if (ingresado == guardado) onCorrecto()
                else Toast.makeText(this, "PIN de Dueño incorrecto", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarCajero(prefs: android.content.SharedPreferences, nombre: String, pin: String) {
        val arr = JSONArray(prefs.getString("lista_cajeros", "[]") ?: "[]")
        val obj = JSONObject().apply {
            put("nombre", nombre)
            put("pin", pin)
        }
        arr.put(obj)
        prefs.edit().putString("lista_cajeros", arr.toString()).apply()
    }

    private fun recargarCajerosAdmin(prefs: android.content.SharedPreferences) {
        layoutListaCajerosAdmin.removeAllViews()
        val arr = JSONArray(prefs.getString("lista_cajeros", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val t = TextView(this).apply {
                text = "• ${c.getString("nombre")} (PIN: ${c.getString("pin")})"
                textSize = 13f
                setTextColor(Color.parseColor("#334155"))
                setPadding(0, 4, 0, 4)
            }
            layoutListaCajerosAdmin.addView(t)
        }
    }

    // ================= MOTOR EMVCO BCRP OFICIAL =================
    private fun armarQrEmvcoConMonto(qrBase: String, montoStr: String): String {
        return try {
            // Si ya es un QR EMVCo de Yape (empieza con 000201)
            if (qrBase.startsWith("000201")) {
                var payloadSinCrc = qrBase
                val idxCrc = payloadSinCrc.lastIndexOf("6304")
                if (idxCrc != -1) {
                    payloadSinCrc = payloadSinCrc.substring(0, idxCrc)
                }

                // Inyectar Tag 54 (Monto) con formato EMVCo
                val montoFormateado = String.format(Locale.US, "%.2f", montoStr.toDouble())
                val tag54 = "54" + String.format("%02d", montoFormateado.length) + montoFormateado

                val payloadConMonto = payloadSinCrc + tag54 + "6304"
                val nuevoCrc = calcularCrc16Emvco(payloadConMonto)
                payloadConMonto + nuevoCrc
            } else {
                qrBase
            }
        } catch (e: Exception) {
            qrBase
        }
    }

    private fun calcularCrc16Emvco(str: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        for (b in str.toByteArray(Charsets.ISO_8859_1)) {
            for (i in 0 until 8) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        crc = crc and 0xFFFF
        return String.format("%04X", crc)
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

    private fun cargarHistorialCajero() {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("historial_cobros", "[]") ?: "[]")
        layoutListaCobros.removeAllViews()

        if (arr.length() == 0) {
            layoutListaCobros.addView(TextView(this).apply {
                text = "No hay cobros en este turno todavía."
                setTextColor(Color.GRAY)
                textSize = 12f
                setPadding(0, 20, 0, 20)
            })
        } else {
            for (i in 0 until minOf(arr.length(), 10)) {
                val item = arr.getJSONObject(i)
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(15, 12, 15, 12)
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
                layoutListaCobros.addView(row)
            }
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
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 6, 0, 14) }
        }
    }
}
