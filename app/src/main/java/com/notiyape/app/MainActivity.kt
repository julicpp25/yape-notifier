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

    private lateinit var layoutCajero: LinearLayout
    private lateinit var layoutAdmin: LinearLayout
    private lateinit var switchVoz: Switch
    private lateinit var imgQrGenerado: ImageView
    private lateinit var txtMontoCobroQr: TextView
    private lateinit var layoutListaCobrosCajero: LinearLayout
    private lateinit var txtTotalHoy: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)

        // PIN por defecto del Dueño: 1234 (se puede cambiar)
        if (!prefs.contains("pin_admin")) {
            prefs.edit().putString("pin_admin", "1234").apply()
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 40, 30, 40)
        }
        scroll.addView(mainContainer)

        // ================= BARRA SUPERIOR (PERFIL Y VOZ) =================
        val barraSuperior = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 20)
        }

        val txtPerfilActual = TextView(this).apply {
            text = "MODO: CAJERO 🛒"
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

        val btnCambiarPerfil = Button(this).apply {
            text = "⚙️ Admin"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = crearBordeRedondo("#0F172A", "#0F172A", 14f)
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                if (layoutAdmin.visibility == View.VISIBLE) {
                    // Volver a modo cajero
                    layoutAdmin.visibility = View.GONE
                    layoutCajero.visibility = View.VISIBLE
                    txtPerfilActual.text = "MODO: CAJERO 🛒"
                } else {
                    // Pedir PIN de seguridad
                    pedirPinDueño(prefs) {
                        layoutCajero.visibility = View.GONE
                        layoutAdmin.visibility = View.VISIBLE
                        txtPerfilActual.text = "MODO: DUEÑO 👑"
                    }
                }
            }
        }

        barraSuperior.addView(txtPerfilActual)
        barraSuperior.addView(switchVoz)
        barraSuperior.addView(btnCambiarPerfil)
        mainContainer.addView(barraSuperior)

        // =========================================================
        // SECCIÓN 1: MODO CAJERO (PUNTO DE VENTA / QR DINÁMICO)
        // =========================================================
        layoutCajero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cardCobro = crearTarjeta(this)

        val titCajero = TextView(this).apply {
            text = "Cobro Rápido con QR 📲"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 10)
        }

        val inputMonto = EditText(this).apply {
            hint = "Ingresa monto: Ej. 15.50"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = crearBordeRedondo("#F8FAFC", "#CBD5E1", 16f)
            setPadding(25, 25, 25, 25)
        }

        imgQrGenerado = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(500, 500).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 20, 0, 10)
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

        val btnGenerarQr = crearBoton(this, "⚡ Generar QR de Pago", "#00A859") {
            val monto = inputMonto.text.toString().trim()
            if (monto.isEmpty()) {
                Toast.makeText(this@MainActivity, "Digita un monto primero", Toast.LENGTH_SHORT).show()
                return@crearBoton
            }

            val codigo = prefs.getString("codigo_tienda", "TEST-01") ?: "TEST-01"
            
            // Payload de pago interoperable (Yape/Plin estándar)
            val datosPagoQr = "YAPE://PAY?MERCHANT=$codigo&AMOUNT=$monto&CURRENCY=PEN"

            val qrBitmap = generarCodigoQr(datosPagoQr, 500, 500)
            if (qrBitmap != null) {
                imgQrGenerado.setImageBitmap(qrBitmap)
                imgQrGenerado.visibility = View.VISIBLE
                txtMontoCobroQr.text = "Escanea para pagar: S/ $monto"
                txtMontoCobroQr.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "QR listo para escanear", Toast.LENGTH_SHORT).show()
            }
        }

        cardCobro.addView(titCajero)
        cardCobro.addView(inputMonto)
        cardCobro.addView(btnGenerarQr)
        cardCobro.addView(imgQrGenerado)
        cardCobro.addView(txtMontoCobroQr)

        // Historial en vivo para el cajero
        val cardHistorial = crearTarjeta(this)
        val titHistorial = TextView(this).apply {
            text = "Últimos Cobros del Turno:"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 15)
        }
        layoutListaCobrosCajero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cardHistorial.addView(titHistorial)
        cardHistorial.addView(layoutListaCobrosCajero)

        layoutCajero.addView(cardCobro)
        layoutCajero.addView(cardHistorial)
        mainContainer.addView(layoutCajero)

        // =========================================================
        // SECCIÓN 2: MODO ADMINISTRACIÓN (SOLO DUEÑO)
        // =========================================================
        layoutAdmin = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        val cardAdmin = crearTarjeta(this)
        val titAdmin = TextView(this).apply {
            text = "Panel de Control del Dueño 👑"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 15)
        }

        val cardTotal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = crearBordeRedondo("#ECFDF5", "#A7F3D0", 18f)
            setPadding(25, 20, 25, 20)
        }
        val lblTotal = TextView(this).apply {
            text = "TOTAL RECAUDADO HOY"
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
        cardTotal.addView(lblTotal)
        cardTotal.addView(txtTotalHoy)

        val btnAbrirSheet = crearBoton(this, "📊 Abrir mi Hoja de Google Sheets", "#00A859") {
            val url = prefs.getString("sheet_url_cliente", "") ?: ""
            if (url.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(this@MainActivity, "Revisa el enlace enviado a tu Telegram", Toast.LENGTH_SHORT).show()
            }
        }

        val btnPermisoLector = crearBoton(this, "🛡️ Configuración de Permisos Android", "#475569") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val btnCambiarPin = crearBoton(this, "🔑 Cambiar PIN de Admin", "#0F172A") {
            cambiarPinDialog(prefs)
        }

        val btnResetTienda = Button(this).apply {
            text = "⚠️ Desvincular y Registrar Nueva Tienda"
            setTextColor(Color.RED)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 12f
            setPadding(0, 30, 0, 10)
            setOnClickListener {
                prefs.edit().clear().apply()
                Toast.makeText(this@MainActivity, "Datos borrados. Reinicia la app.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        cardAdmin.addView(titAdmin)
        cardAdmin.addView(cardTotal)
        cardAdmin.addView(btnAbrirSheet)
        cardAdmin.addView(btnPermisoLector)
        cardAdmin.addView(btnCambiarPin)
        cardAdmin.addView(btnResetTienda)
        layoutAdmin.addView(cardAdmin)
        mainContainer.addView(layoutAdmin)

        setContentView(scroll)
        cargarHistorialCajero()
    }

    // ================= FUNCIONES DE SEGURIDAD Y QR =================
    private fun pedirPinDueño(prefs: android.content.SharedPreferences, onCorrecto: () -> Unit) {
        val inputPin = EditText(this).apply {
            hint = "Ingresa PIN de 4 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }

        AlertDialog.Builder(this)
            .setTitle("Acceso Restringido")
            .setMessage("Solo el dueño del negocio puede acceder a estas funciones:")
            .setView(inputPin)
            .setPositiveButton("Ingresar") { _, _ ->
                val pinIngresado = inputPin.text.toString().trim()
                val pinGuardado = prefs.getString("pin_admin", "1234")
                if (pinIngresado == pinGuardado) {
                    onCorrecto()
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarPinDialog(prefs: android.content.SharedPreferences) {
        val inputNuevoPin = EditText(this).apply {
            hint = "Nuevo PIN de 4 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
        }

        AlertDialog.Builder(this)
            .setTitle("Cambiar PIN de Dueño")
            .setView(inputNuevoPin)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = inputNuevoPin.text.toString().trim()
                if (nuevo.length >= 4) {
                    prefs.edit().putString("pin_admin", nuevo).apply()
                    Toast.makeText(this, "PIN actualizado con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "El PIN debe tener al menos 4 números", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                text = "Esperando el primer cobro del día..."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 30)
            }
            layoutListaCobrosCajero.addView(txtVacio)
        } else {
            for (i in 0 until minOf(array.length(), 10)) {
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
                    setPadding(15, 12, 15, 12)
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
                    textSize = 14f
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
        cargarHistorialCajero()
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
}
