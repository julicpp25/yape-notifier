import React, { useState } from 'react';
import {
  FileCode,
  Copy,
  Check,
  CheckCircle2,
  Download,
  Terminal,
  ShieldCheck,
  Zap,
  Globe,
  Laptop,
  FolderUp,
  Code2,
  ExternalLink,
} from 'lucide-react';

export const AndroidCodeViewer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<
    'listener' | 'manifest' | 'gradle' | 'proguard' | 'main'
  >('listener');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(id);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  const filesContent = {
    listener: `// Archivo: app/src/main/java/com/notiyape/app/YapeListenerService.kt
// MEJORAS: Soporte para 'te yapeó', 'te plineó', Scotiabank/BBVA/Interbank y voz TTS optimizada

package com.notiyape.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class YapeListenerService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private val SERVIDOR_URL = "https://script.google.com/macros/s/AKfycbw0_0UBYuP2yVjPHOmMDyLlehzvT41nuJPPCOnRw2Zd1V1Y57xI2nHUyVI1nSBhR7F_3w/exec"
    private var motorVoz: TextToSpeech? = null
    private var vozLista = false
    private val client = OkHttpClient()

    override fun onCreate() {
        super.onCreate()
        try {
            motorVoz = TextToSpeech(this, this, "com.google.android.tts")
        } catch (e: Exception) {
            motorVoz = TextToSpeech(this, this)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        val esYape = (pkg == "com.bcp.innovacxion.yapeapp")
        val esInterbank = (pkg == "pe.com.interbank.mobilebanking")
        val esBBVA = (pkg == "com.bbva.bbvacontigo")
        val esScotiabank = (pkg == "com.scotiabank.peru.pe")
        val esBanbif = (pkg == "com.banbif.bifmovil")

        if (esYape || esInterbank || esBBVA || esScotiabank || esBanbif) {
            val extras = sbn.notification.extras
            val text = extras.getCharSequence("android.text")?.toString()
                ?: extras.getCharSequence("android.bigText")?.toString()
                ?: extras.getString("android.title") ?: ""

            if (text.isNotEmpty()) {
                val metodo = when {
                    esYape -> "Yape"
                    esInterbank -> "Plin Interbank"
                    esBBVA -> "Plin BBVA"
                    esScotiabank -> "Plin Scotiabank"
                    else -> "Plin / Transferencia"
                }
                procesarPago(text, metodo)
            }
        }
    }

    private fun procesarPago(texto: String, metodo: String) {
        val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
        val codigoTienda = prefs.getString("codigo_tienda", "") ?: return

        // ESCUDO 1: Descarta publicidad, créditos bancarios y préstamos
        val esPublicidad = texto.contains(
            Regex("(crédito|credito|préstamo|prestamo|pre-aprobado|aprobado al instante|pide tu|promoción|promo|descuento|sorteo|gana|premios|seguro|paga tus servicios|solicita)", RegexOption.IGNORE_CASE)
        )
        if (esPublicidad) return

        // ESCUDO 2: Verbos de pago (Soporta 'te yapeó', 'te plineó', etc.)
        val tieneVerboPago = texto.contains(
            Regex("(te envi|te mand|transfiri|recibiste un pago|pago de|te yape|yapeó|yapeo|te pline|plineó|plineo|recibiste un yape|recibiste un plin|recibiste)", RegexOption.IGNORE_CASE)
        )
        if (!tieneVerboPago) return

        // ESCUDO 3: Extraer Monto en Soles
        val regexMonto = Regex("""S/\\.?\\s*([\\d]+(?:[.,]\\d+)?)""", RegexOption.IGNORE_CASE)
        val matchMonto = regexMonto.find(texto) ?: return
        val monto = matchMonto.groups[1]?.value?.replace(",", ".") ?: return
        if ((monto.toDoubleOrNull() ?: 0.0) <= 0.0) return

        // ESCUDO 4: Extraer Remitente Limpio
        var remitente = "Cliente"
        val regexVerbos = Regex("(te envi[oó]|transfiri[oó]|te mand[oó]|recibiste un pago de|pago de|te yape[oó]|yape[oó]|te pline[oó]|pline[oó])", RegexOption.IGNORE_CASE)

        if (texto.contains(regexVerbos)) {
            val antes = texto.split(regexVerbos)[0]
            val posible = antes.replace(Regex("(¡|!|Te llegó un Yape|Yape|Plin|Pago|Notificación)", RegexOption.IGNORE_CASE), "").trim()
            if (posible.isNotEmpty() && posible.length >= 3) {
                remitente = posible
            }
        }

        if (remitente == "Cliente" || remitente.length < 3) {
            val matchDe = Regex("""(?:de|por)\\s+([A-Za-zÀ-ÿ\\s]{3,35})""", RegexOption.IGNORE_CASE).find(texto)
            if (matchDe != null) {
                val candidata = matchDe.groups[1]?.value?.trim() ?: ""
                if (candidata.isNotEmpty() && !candidata.contains("Yape", ignoreCase = true)) {
                    remitente = candidata
                }
            }
        }

        if (prefs.getBoolean("voz_activa", true)) {
            reproducirTimbreYVoz(metodo, monto)
        }

        guardarCobroLocal(remitente, monto, metodo)
    }
}`,
    manifest: `<!-- Archivo: app/src/main/AndroidManifest.xml -->
<!-- MEJORAS: allowBackup='false' (seguridad fintech), eliminada la optimización de batería prohibida por Google Play -->

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="false"
        android:icon="@drawable/ic_launcher"
        android:roundIcon="@drawable/ic_launcher"
        android:label="NotiCobros: Alertas Yape y Plin"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault.Light">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Receptor de Notificaciones Push de Firebase -->
        <service
            android:name=".MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <!-- Lector de Notificaciones de Yape/Plin -->
        <service
            android:name=".YapeListenerService"
            android:label="Lector de Pagos NotiCobros"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

    </application>

</manifest>`,
    main: `// Fragmentos clave agregados a: app/src/main/java/com/notiyape/app/MainActivity.kt
// 1. MEJORA OBLIGATORIA GOOGLE PLAY: Modal de Divulgación Prominente
private fun mostrarDivulgacionProminenteNotificaciones() {
    AlertDialog.Builder(this)
        .setTitle("Divulgación de Privacidad y Notificaciones 🔔")
        .setMessage(
            "NotiCobros requiere acceso a las notificaciones del sistema con el único propósito de detectar automáticamente los cobros recibidos de Yape, Plin y bancos (BCP, Interbank, BBVA, Scotiabank).\\n\\n" +
            "• NO lee ni recopila mensajes privados, chats de WhatsApp, SMS ni notificaciones personales.\\n" +
            "• NO tiene acceso a contraseñas bancarias, saldo de cuentas ni datos financieros sensibles.\\n" +
            "• La información de pagos se usa exclusivamente para anunciar el cobro por altavoz a tus cajeros y registrarlo en tu caja.\\n" +
            "• Ningún dato se comercializa ni se comparte con terceros."
        )
        .setPositiveButton("Aceptar y Configurar") { _, _ ->
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        .setNegativeButton("Cancelar", null)
        .show()
}

// 2. NUEVA FUNCIÓN: Adjuntar imagen del QR de Yape desde Galería y auto-generar ID
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

            if (bitmap == null) return@launch

            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = QRCodeReader().decode(binaryBitmap)
            val textoQr = result.text

            // Generar ID único automático de tienda
            val prefs = getSharedPreferences("YapePrefs", Context.MODE_PRIVATE)
            var cod = prefs.getString("codigo_tienda", "") ?: ""
            if (cod.isEmpty()) {
                cod = "TIENDA-" + (1000..9999).random()
                prefs.edit().putString("codigo_tienda", cod).apply()
            }

            prefs.edit().putString("qr_base_yape", textoQr).apply()

            // Sincronizar a Google Apps Script
            val json = JSONObject().apply {
                put("accion", "guardar_qr")
                put("codigoTienda", cod)
                put("qrBase", textoQr)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            client.newCall(Request.Builder().url(SERVIDOR_URL).post(body).build()).execute().close()

            runOnUiThread {
                inQrConfigTabCajeros.setText(textoQr)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("¡QR de Yape Vinculado! 🎉")
                    .setMessage("Código QR decodificado exitosamente.\\n\\nID de Tienda: $cod\\n\\nAhora tu QR está listo para cobrar en la caja con el monto dinámico.")
                    .setPositiveButton("Genial", null)
                    .show()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "No se encontró un código QR válido en la imagen. Intenta con una captura más nítida.", Toast.LENGTH_LONG).show()
            }
        }
    }
}`,
    gradle: `// Archivo: app/build.gradle.kts
// MEJORAS: Configuración de build release con ProGuard para Google Play

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.notiyape.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.notiyape.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // Configurar tu clave de Play Console
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.zxing:core:3.5.3")
    
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-messaging")
}`,
    proguard: `# Archivo nuevo: app/proguard-rules.pro
# Evita que el compilador R8 de Android rompa OkHttp, Firebase o Coroutines en Google Play

# OkHttp3
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembers class * extends okhttp3.ResponseBody { *; }

# Google Firebase Cloud Messaging
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes *Annotation*,InnerClasses,EnclosingMethod

# Google ZXing (Generador de QR)
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**`,
  };

  // Tab for GitHub Web update tutorial:
  const [guideTab, setGuideTab] = useState<'editor-web' | 'upload-files' | 'pencil'>('editor-web');

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* GUIA PASO A PASO: ACTUALIZAR EN GITHUB DESDE EL NAVEGADOR SIN CONSOLA */}
      <div className="bg-gradient-to-br from-slate-900 via-indigo-950/40 to-purple-950/30 border-2 border-cyan-500/40 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/20 border border-cyan-400/30 text-cyan-300 text-xs font-bold mb-2">
              <Globe className="w-3.5 h-3.5 text-cyan-400" />
              <span>Para quienes trabajan 100% en GitHub Web (Sin consola ni Visual Studio)</span>
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
              ¿Cómo actualizar tu código directamente en la web de GitHub?
            </h2>
            <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-2xl">
              No necesitas instalar Visual Studio ni usar comandos de terminal. Puedes actualizar tu repositorio en menos de 2 minutos directamente desde tu navegador:
            </p>
          </div>

          <a
            href="/noticobros-playstore-ready.zip"
            download
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white font-bold text-xs shadow-lg shadow-purple-600/30 transition shrink-0"
          >
            <Download className="w-4 h-4" />
            <span>Descargar Archivos (ZIP)</span>
          </a>
        </div>

        {/* Tutorial Tabs */}
        <div className="flex gap-2 border-b border-slate-800/80 pb-3 overflow-x-auto text-xs font-bold">
          <button
            onClick={() => setGuideTab('editor-web')}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl transition shrink-0 ${
              guideTab === 'editor-web'
                ? 'bg-cyan-600 text-white shadow-md shadow-cyan-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <Laptop className="w-4 h-4" />
            <span>Método 1: Editor Web de GitHub (Tecla Punto .) ⭐ Recomendado</span>
          </button>
          <button
            onClick={() => setGuideTab('upload-files')}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl transition shrink-0 ${
              guideTab === 'upload-files'
                ? 'bg-purple-600 text-white shadow-md shadow-purple-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <FolderUp className="w-4 h-4" />
            <span>Método 2: Botón "Upload files" (Arrastrar)</span>
          </button>
          <button
            onClick={() => setGuideTab('pencil')}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl transition shrink-0 ${
              guideTab === 'pencil'
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <Code2 className="w-4 h-4" />
            <span>Método 3: Editar con el Lápiz ✏️</span>
          </button>
        </div>

        {/* Tab 1: Editor Web de GitHub */}
        {guideTab === 'editor-web' && (
          <div className="space-y-4 animate-fadeIn text-xs text-slate-300">
            <div className="p-3 bg-cyan-950/40 border border-cyan-500/30 rounded-2xl flex items-center justify-between flex-wrap gap-3">
              <span className="text-cyan-200">
                💡 <strong>GitHub tiene su propio Visual Studio integrado en la web:</strong> se abre con solo presionar la tecla punto <kbd className="px-2 py-0.5 bg-slate-800 text-cyan-300 rounded border border-slate-700 font-mono text-xs">.</kbd> en tu teclado.
              </span>
              <a
                href="https://github.dev/julicpp25/yape-notifier"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white font-bold transition"
              >
                <span>Abrir Editor en github.dev</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </div>

            <ol className="list-decimal list-inside space-y-2.5 leading-relaxed pl-1 text-slate-200">
              <li>
                Descarga el archivo <strong className="text-purple-300">noticobros-playstore-ready.zip</strong> con el botón morado de arriba y <strong>descomprímelo</strong> en tu computadora (verás la carpeta <code className="text-cyan-300 font-mono">app</code>).
              </li>
              <li>
                Haz clic en <a href="https://github.dev/julicpp25/yape-notifier" target="_blank" rel="noopener noreferrer" className="text-cyan-400 font-bold underline">Abrir Editor en github.dev</a> (o abre tu repo en github.com y presiona la tecla punto <kbd className="px-1.5 py-0.5 bg-slate-800 rounded font-mono">.</kbd>).
              </li>
              <li>
                Verás un editor idéntico a VS Code dentro de tu navegador Chrome. A la izquierda está la lista de carpetas de tu proyecto.
              </li>
              <li>
                <strong>Arrastra la carpeta "app" descomprimida</strong> directamente sobre el panel izquierdo de archivos del navegador. Si te pregunta si deseas sobreescribir, pulsa <strong>"Reemplazar"</strong>.
              </li>
              <li>
                En el menú de la izquierda, haz clic en el icono de <strong>Control de Código</strong> (el tercer icono, con ramas) o presiona <kbd className="px-1.5 py-0.5 bg-slate-800 rounded font-mono">Ctrl + Shift + G</kbd>.
              </li>
              <li>
                Escribe un mensaje de cambio: <code className="text-emerald-400 font-mono bg-slate-900 px-2 py-0.5 rounded">feat: adjuntar qr y mejoras playstore</code> y haz clic en el botón azul <strong>"Commit &amp; Push"</strong>.
              </li>
              <li>
                ¡Listo! GitHub Actions empezará a compilar tu nuevo APK al instante.
              </li>
            </ol>
          </div>
        )}

        {/* Tab 2: Upload Files */}
        {guideTab === 'upload-files' && (
          <div className="space-y-4 animate-fadeIn text-xs text-slate-300">
            <p className="leading-relaxed">
              Puedes subir los archivos reemplazados directamente a través de la web normal de GitHub usando el botón <strong>"Add file &gt; Upload files"</strong>:
            </p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-2">
                <span className="font-bold text-white block">Paso 1: Clases Kotlin</span>
                <p className="text-[11px] text-slate-400">
                  Abre la carpeta de código fuente en GitHub:
                </p>
                <a
                  href="https://github.com/julicpp25/yape-notifier/tree/main/app/src/main/java/com/notiyape/app"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-cyan-400 underline text-[11px] block truncate font-mono"
                >
                  .../com/notiyape/app
                </a>
                <p className="text-[11px] text-slate-300">
                  Pulsa <strong>"Add file &gt; Upload files"</strong> y arrastra <code className="text-purple-300">MainActivity.kt</code> y <code className="text-purple-300">YapeListenerService.kt</code>. Pulsa "Commit changes".
                </p>
              </div>

              <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-2">
                <span className="font-bold text-white block">Paso 2: AndroidManifest</span>
                <p className="text-[11px] text-slate-400">
                  Abre la carpeta main en GitHub:
                </p>
                <a
                  href="https://github.com/julicpp25/yape-notifier/tree/main/app/src/main"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-cyan-400 underline text-[11px] block truncate font-mono"
                >
                  .../app/src/main
                </a>
                <p className="text-[11px] text-slate-300">
                  Pulsa <strong>"Add file &gt; Upload files"</strong> y arrastra el archivo <code className="text-blue-300">AndroidManifest.xml</code>. Pulsa "Commit changes".
                </p>
              </div>

              <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-2">
                <span className="font-bold text-white block">Paso 3: Gradle &amp; ProGuard</span>
                <p className="text-[11px] text-slate-400">
                  Abre la carpeta app en GitHub:
                </p>
                <a
                  href="https://github.com/julicpp25/yape-notifier/tree/main/app"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-cyan-400 underline text-[11px] block truncate font-mono"
                >
                  .../yape-notifier/app
                </a>
                <p className="text-[11px] text-slate-300">
                  Pulsa <strong>"Add file &gt; Upload files"</strong> y arrastra <code className="text-purple-300">build.gradle.kts</code> y <code className="text-emerald-300">proguard-rules.pro</code>. Pulsa "Commit changes".
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Lapiz */}
        {guideTab === 'pencil' && (
          <div className="space-y-4 animate-fadeIn text-xs text-slate-300">
            <p className="leading-relaxed">
              Si no quieres descargar ningún ZIP y prefieres copiar y pegar el código directamente en los archivos de GitHub con el icono de lápiz ✏️:
            </p>
            <div className="space-y-2">
              <div className="flex items-center justify-between p-2.5 bg-slate-950 rounded-xl border border-slate-800">
                <div>
                  <span className="font-bold text-white block">1. MainActivity.kt (Función Adjuntar QR y Divulgación)</span>
                  <span className="text-[10px] text-slate-400">Selecciona la pestaña "MainActivity.kt" abajo, copia el código y pégalo en GitHub</span>
                </div>
                <a
                  href="https://github.com/julicpp25/yape-notifier/edit/main/app/src/main/java/com/notiyape/app/MainActivity.kt"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-cyan-400 font-bold text-xs flex items-center gap-1 transition"
                >
                  <span>Editar en GitHub</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>

              <div className="flex items-center justify-between p-2.5 bg-slate-950 rounded-xl border border-slate-800">
                <div>
                  <span className="font-bold text-white block">2. YapeListenerService.kt (Lector Plin y Yape)</span>
                  <span className="text-[10px] text-slate-400">Pega el código nuevo para escuchar Plin y Yape</span>
                </div>
                <a
                  href="https://github.com/julicpp25/yape-notifier/edit/main/app/src/main/java/com/notiyape/app/YapeListenerService.kt"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-cyan-400 font-bold text-xs flex items-center gap-1 transition"
                >
                  <span>Editar en GitHub</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>

              <div className="flex items-center justify-between p-2.5 bg-slate-950 rounded-xl border border-slate-800">
                <div>
                  <span className="font-bold text-white block">3. AndroidManifest.xml (Permisos Play Store)</span>
                  <span className="text-[10px] text-slate-400">Reemplaza el manifiesto sin el permiso de batería prohibido</span>
                </div>
                <a
                  href="https://github.com/julicpp25/yape-notifier/edit/main/app/src/main/AndroidManifest.xml"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-cyan-400 font-bold text-xs flex items-center gap-1 transition"
                >
                  <span>Editar en GitHub</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>

              <div className="flex items-center justify-between p-2.5 bg-slate-950 rounded-xl border border-slate-800">
                <div>
                  <span className="font-bold text-white block">4. Crear proguard-rules.pro (Nuevo archivo)</span>
                  <span className="text-[10px] text-slate-400">Crea este archivo en la carpeta /app para que no falle al compilar para Play Store</span>
                </div>
                <a
                  href="https://github.com/julicpp25/yape-notifier/new/main/app?filename=proguard-rules.pro"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-cyan-400 font-bold text-xs flex items-center gap-1 transition"
                >
                  <span>Crear en GitHub</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>
          </div>
        )}

        {/* Verification in GitHub Actions */}
        <div className="p-3 bg-emerald-950/40 border border-emerald-500/30 rounded-2xl flex items-center justify-between flex-wrap gap-2 text-xs">
          <div className="flex items-center gap-2 text-emerald-300">
            <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
            <span>
              <strong>¿Dónde descargas el nuevo APK compilado?</strong> Cuando guardes los cambios en GitHub, ve a la pestaña <strong>Actions</strong> de tu repositorio:
            </span>
          </div>
          <a
            href="https://github.com/julicpp25/yape-notifier/actions"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold transition text-xs"
          >
            <span>Ver GitHub Actions</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      </div>

      {/* Improvements Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-slate-900/80 border border-emerald-500/30 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center gap-2 mb-2 text-emerald-400 font-bold text-xs">
            <CheckCircle2 className="w-4 h-4" />
            <span>1. Adjuntar QR &amp; Variantes</span>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            Se agregó el lector de imagen de QR de Yape desde galería con ZXing, soporte para <code className="text-purple-300 font-mono">te yapeó</code> y <code className="text-cyan-300 font-mono">te plineó</code>.
          </p>
        </div>

        <div className="bg-slate-900/80 border border-blue-500/30 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center gap-2 mb-2 text-blue-400 font-bold text-xs">
            <ShieldCheck className="w-4 h-4" />
            <span>2. Divulgación Prominente</span>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            Se implementó el diálogo de privacidad obligatorio de Google Play para <code className="text-blue-300 font-mono">BIND_NOTIFICATION_LISTENER_SERVICE</code>.
          </p>
        </div>

        <div className="bg-slate-900/80 border border-purple-500/30 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center gap-2 mb-2 text-purple-400 font-bold text-xs">
            <Zap className="w-4 h-4" />
            <span>3. ProGuard &amp; Release .AAB</span>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            Se creó <code className="text-purple-300 font-mono">proguard-rules.pro</code> y se optimizó el Gradle para que Firebase y OkHttp no fallen en producción.
          </p>
        </div>
      </div>

      {/* Code Viewer */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-3xl overflow-hidden shadow-2xl">
        {/* Tab Header */}
        <div className="bg-slate-950 border-b border-slate-800 p-3 flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-1.5 text-xs">
            <button
              onClick={() => setSelectedFile('listener')}
              className={`px-3 py-1.5 rounded-xl font-medium transition ${
                selectedFile === 'listener'
                  ? 'bg-purple-600 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-slate-850'
              }`}
            >
              YapeListenerService.kt
            </button>
            <button
              onClick={() => setSelectedFile('main')}
              className={`px-3 py-1.5 rounded-xl font-medium transition ${
                selectedFile === 'main'
                  ? 'bg-purple-600 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-slate-850'
              }`}
            >
              MainActivity.kt (Divulgación)
            </button>
            <button
              onClick={() => setSelectedFile('manifest')}
              className={`px-3 py-1.5 rounded-xl font-medium transition ${
                selectedFile === 'manifest'
                  ? 'bg-purple-600 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-slate-850'
              }`}
            >
              AndroidManifest.xml
            </button>
            <button
              onClick={() => setSelectedFile('gradle')}
              className={`px-3 py-1.5 rounded-xl font-medium transition ${
                selectedFile === 'gradle'
                  ? 'bg-purple-600 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-slate-850'
              }`}
            >
              build.gradle.kts
            </button>
            <button
              onClick={() => setSelectedFile('proguard')}
              className={`px-3 py-1.5 rounded-xl font-medium transition ${
                selectedFile === 'proguard'
                  ? 'bg-purple-600 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-slate-850'
              }`}
            >
              proguard-rules.pro
            </button>
          </div>

          <button
            onClick={() => copyToClipboard(filesContent[selectedFile], selectedFile)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition border border-slate-700"
          >
            {copiedKey === selectedFile ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copiedKey === selectedFile ? 'Copiado al portapapeles' : 'Copiar Archivo'}</span>
          </button>
        </div>

        {/* Code Content */}
        <pre className="p-5 font-mono text-xs text-slate-200 bg-slate-950 overflow-x-auto max-h-[500px] leading-relaxed">
          {filesContent[selectedFile]}
        </pre>
      </div>
    </div>
  );
};
