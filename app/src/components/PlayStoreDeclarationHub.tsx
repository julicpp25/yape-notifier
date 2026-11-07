import React, { useState } from 'react';
import {
  FileText,
  Copy,
  Check,
  ShieldAlert,
  Terminal,
  ExternalLink,
  Download,
  AlertTriangle,
  Sparkles,
  BadgePercent,
  CheckCircle2,
  FolderArchive,
  Store,
  Info,
} from 'lucide-react';

export const PlayStoreDeclarationHub: React.FC = () => {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(id);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  const nombreApp = 'NotiCobros: Alertas Yape y Plin';
  const descripcionCorta = 'Asistente de voz y caja para cobros de Yape y Plin en tu negocio. 100% gratis.';

  const descripcionCompleta = `NotiCobros es la herramienta gratuita de caja y asistente de voz en tiempo real para comerciantes, emprendedores y pequeños negocios en el Perú que cobran con Yape y Plin.

🔊 CANTA TUS COBROS AL INSTANTE
¡No pierdas tiempo revisando el celular del dueño a cada momento! NotiCobros escucha las notificaciones de pagos entrantes y anuncia en voz alta por el altavoz el monto recibido y el nombre del cliente con frases de ánimo comerciales.

🛒 PUNTO DE VENTA Y QR DINÁMICO
- Adjunta la captura de pantalla de tu código QR de Yape o Plin una sola vez.
- Marca el monto de la venta en el teclado táctil de la app y muestra el QR con el monto predefinido para que el cliente escanee y pague sin errores.
- Genera automáticamente tu identificador de tienda para sincronizar con tus cajeros.

📊 CONTROL DE VENTAS Y CAJA DIARIA
- Historial detallado de cobros con fecha, hora exacta y método (Yape, Plin o Transferencia).
- Total recaudado en el turno con modo de privacidad para ocultar el saldo ante clientes.
- Cierre de turno y exportación para control contable.

🔒 100% SEGURO Y PRIVADO
- NotiCobros es un asistente independiente: NO solicita contraseñas bancarias ni tiene acceso a tu dinero o cuentas de banco.
- Solo lee notificaciones emitidas por las aplicaciones de pago peruanas autorizadas (Yape, Plin de Interbank, BBVA, Scotiabank).
- NO lee mensajes privados de WhatsApp, llamadas ni SMS.
- Completamente gratuita y sin anuncios molestos.

Nota de descargo: NotiCobros es una herramienta desarrollada de forma independiente para apoyar a los comerciantes peruanos. No tiene afiliación ni representa oficialmente al Banco de Crédito del Perú (BCP/Yape) ni al consorcio Plin.`;

  const declaracionGooglePlay = `DECLARACIÓN OFICIAL PARA GOOGLE PLAY CONSOLE (SPECIAL APP ACCESS)

Sección: Declaración de Acceso a Notificaciones (BIND_NOTIFICATION_LISTENER_SERVICE)
Pregunta de Google: "¿Por qué tu aplicación requiere acceso a las notificaciones del dispositivo?"

Respuesta oficial para aprobación (Copiar y pegar en Play Console):
-------------------------------------------------------------------------------------
Nuestra aplicación 'NotiCobros' es una utilidad de punto de venta (POS) y asistencia de cobro diseñada para pequeños comerciantes y negocios en Perú.

1. PROPÓSITO ESENCIAL:
La funcionalidad central de la aplicación consiste en escuchar en tiempo real las notificaciones de comprobantes de pago entrantes emitidas exclusivamente por las aplicaciones bancarias locales (Yape del BCP, Plin de Interbank, BBVA y Scotiabank) para anunciar en voz alta mediante síntesis de voz (Text-to-Speech) el monto cobrado al cajero y registrar la venta en la caja del negocio.

2. MINIMIZACIÓN Y FILTRADO ESTRICTO DE DATOS:
- La aplicación filtra en memoria el nombre del paquete (package name) de origen y descarta de manera instantánea cualquier notificación que no pertenezca a las aplicaciones bancarias autorizadas.
- No se accede, lee, procesa ni almacena ninguna notificación personal, mensajes de chat (como WhatsApp, Telegram o SMS) ni información sensible privada.

3. PRIVACIDAD Y DIVULGACIÓN DESTACADA:
- Antes de que el usuario habilite el permiso en los ajustes del sistema operativo, la aplicación presenta una pantalla de divulgación prominente (in-app disclosure) explicando claramente qué notificaciones se procesan, con qué fin y solicitando el consentimiento explícito del usuario.
- La aplicación es 100% gratuita y los datos de las transacciones no se comparten, transmiten ni comercializan con terceros para publicidad ni telemetría comercial.
-------------------------------------------------------------------------------------`;

  const comandosCompilacion = `# 1. En tu computadora (en la carpeta de tu proyecto Android Studio):
# Genera tu clave de firma privada (Keystore):
keytool -genkey -v -keystore noticobros-release.jks -alias noticobros -keyalg RSA -keysize 2048 -validity 10000

# 2. Compilar el archivo .AAB para Google Play:
./gradlew bundleRelease

# 3. Tu archivo generado estará listo en:
# app/build/outputs/bundle/release/app-release.aab
# ¡Sube este archivo directamente a Google Play Console!`;

  const politicaPrivacidadNotiCobros = `# POLÍTICA DE PRIVACIDAD DE NOTICOBROS (YAPE & PLIN)
Última actualización: Noviembre 2026

NotiCobros ("la Aplicación", desarrollada de forma independiente para comerciantes del Perú) respeta y protege la privacidad de todos sus usuarios conforme a las directrices de Google Play Store y la Ley de Protección de Datos Personales (Ley N° 29733 del Perú).

### 1. Naturaleza Gratuita de la Aplicación
NotiCobros se distribuye de manera 100% gratuita a través de Google Play Store. No contiene costos ocultos, no requiere suscripciones pagadas y no comercializa datos de los usuarios.

### 2. Acceso a Notificaciones (NotificationListenerService)
La Aplicación utiliza el servicio BIND_NOTIFICATION_LISTENER_SERVICE de Android exclusivamente para:
- Detectar comprobantes de pago entrantes de billeteras electrónicas peruanas (Yape y Plin).
- Extraer el monto en Soles (S/) y el nombre del cliente para anunciar el pago por el altavoz del comerciante mediante Text-to-Speech.
- Registrar el cobro en el historial de caja del comerciante.

La Aplicación NO lee, almacena ni transmite mensajes de WhatsApp, redes sociales ni SMS personales.

### 3. Permiso de Notificaciones en Pantalla (POST_NOTIFICATIONS)
Se utiliza para mostrar avisos en la barra superior al comerciante cuando se confirma un cobro en segundo plano.

### 4. Seguridad Financiera
NotiCobros NO solicita contraseñas bancarias, tokens ni números de tarjeta, y no tiene acceso a los fondos bancarios de los usuarios.

### 5. Contacto
Para consultas sobre privacidad:
Correo electrónico: soporte@notiyape.com / WhatsApp: +51 916 224 843`;

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Download Ready Android Package Banner */}
      <div className="bg-gradient-to-r from-emerald-950/70 via-purple-950/40 to-slate-900 border border-emerald-500/40 rounded-3xl p-6 sm:p-8 shadow-2xl flex flex-col md:flex-row items-start md:items-center justify-between gap-5">
        <div className="space-y-1.5 max-w-2xl">
          <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-bold">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Mejoras Aplicadas • Listo para Google Play</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-white">
            Descarga el Proyecto Android Actualizado (.ZIP)
          </h2>
          <p className="text-xs text-slate-300 leading-relaxed">
            Hemos empaquetado todos los archivos corregidos con el soporte para "te yapeó", el modal de privacidad legal, ProGuard y el nuevo selector de QR. Descomprímelo y reemplaza la carpeta en tu Android Studio.
          </p>
        </div>

        <a
          href="/noticobros-playstore-ready.zip"
          download="noticobros-playstore-ready.zip"
          className="flex items-center gap-2 px-5 py-3 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-xs transition shadow-lg shadow-emerald-600/30 shrink-0"
        >
          <Download className="w-4 h-4" />
          <span>Descargar Código Android (.ZIP)</span>
        </a>
      </div>

      {/* Free App Settings Checklist in Play Console */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-400">
            <BadgePercent className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-white text-base">
              Configuración de "App Gratuita" en Google Play Console
            </h3>
            <p className="text-xs text-slate-400">
              Marca estas opciones exactas en el panel de tu app en Play Console
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
          <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
            <span className="text-slate-400 block text-[10px]">Precio de la app</span>
            <span className="font-bold text-emerald-400 text-sm mt-0.5 block">Gratis (Free)</span>
            <span className="text-[10px] text-slate-500 mt-1 block">
              Sin costo de descarga para los comerciantes.
            </span>
          </div>

          <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
            <span className="text-slate-400 block text-[10px]">Publicidad / Anuncios</span>
            <span className="font-bold text-white text-sm mt-0.5 block">No contiene anuncios</span>
            <span className="text-[10px] text-slate-500 mt-1 block">
              Acelera la aprobación de los revisores.
            </span>
          </div>

          <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
            <span className="text-slate-400 block text-[10px]">Compras integradas</span>
            <span className="font-bold text-white text-sm mt-0.5 block">No (Sin compras)</span>
            <span className="text-[10px] text-slate-500 mt-1 block">
              No requiere pasarelas de pago de Google.
            </span>
          </div>

          <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
            <span className="text-slate-400 block text-[10px]">Público objetivo</span>
            <span className="font-bold text-white text-sm mt-0.5 block">18 años o más</span>
            <span className="text-[10px] text-slate-500 mt-1 block">
              Categoría: Negocios / Finanzas.
            </span>
          </div>
        </div>
      </div>

      {/* Play Store Listing Data (Copy & Paste) */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
            <Store className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-white text-base">Ficha de la Tienda de Google Play (Lista para copiar)</h3>
            <p className="text-xs text-slate-400">Textos optimizados conforme a las directrices de marcas registradas</p>
          </div>
        </div>

        {/* Title */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Título de la aplicación (máx. 30 caracteres):</span>
            <button
              onClick={() => copyToClipboard(nombreApp, 'titulo')}
              className="text-purple-400 hover:text-purple-300 font-medium flex items-center gap-1"
            >
              {copiedKey === 'titulo' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>Copiar título</span>
            </button>
          </div>
          <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-xs text-white font-mono font-bold">
            {nombreApp}
          </div>
        </div>

        {/* Short Description */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Descripción corta (máx. 80 caracteres):</span>
            <button
              onClick={() => copyToClipboard(descripcionCorta, 'cortas')}
              className="text-purple-400 hover:text-purple-300 font-medium flex items-center gap-1"
            >
              {copiedKey === 'cortas' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>Copiar descripción corta</span>
            </button>
          </div>
          <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-xs text-slate-200">
            {descripcionCorta}
          </div>
        </div>

        {/* Full Description */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Descripción completa:</span>
            <button
              onClick={() => copyToClipboard(descripcionCompleta, 'larga')}
              className="text-purple-400 hover:text-purple-300 font-medium flex items-center gap-1"
            >
              {copiedKey === 'larga' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>Copiar descripción completa</span>
            </button>
          </div>
          <pre className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-xs text-slate-300 whitespace-pre-wrap max-h-48 overflow-y-auto leading-relaxed">
            {descripcionCompleta}
          </pre>
        </div>
      </div>

      {/* Special App Access Declaration */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-white text-base">
                Declaración de Acceso a Notificaciones (Play Console)
              </h3>
              <p className="text-xs text-slate-400">
                Pega este texto en la sección "Declaración de Acceso Especial"
              </p>
            </div>
          </div>

          <button
            onClick={() => copyToClipboard(declaracionGooglePlay, 'declaracion')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold transition"
          >
            {copiedKey === 'declaracion' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
            <span>Copiar Declaración</span>
          </button>
        </div>

        <pre className="bg-slate-950 p-4 rounded-2xl border border-slate-800 font-mono text-xs text-slate-300 whitespace-pre-wrap leading-relaxed max-h-52 overflow-y-auto">
          {declaracionGooglePlay}
        </pre>
      </div>

      {/* Build Commands for .AAB */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300">
              <Terminal className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-white text-base">Comandos para Generar el .AAB Firmado</h3>
              <p className="text-xs text-slate-400">Ejecuta esto en Android Studio para compilar el paquete final</p>
            </div>
          </div>

          <button
            onClick={() => copyToClipboard(comandosCompilacion, 'comandos')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition border border-slate-700"
          >
            {copiedKey === 'comandos' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>Copiar Comandos</span>
          </button>
        </div>

        <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 font-mono text-xs text-emerald-400 space-y-1">
          <div className="text-slate-500"># 1. Generar tu clave privada de firma:</div>
          <div>keytool -genkey -v -keystore noticobros-release.jks -alias noticobros -keyalg RSA -keysize 2048 -validity 10000</div>
          <div className="text-slate-500 mt-3"># 2. Compilar el archivo .AAB para Google Play:</div>
          <div className="text-cyan-400">./gradlew bundleRelease</div>
          <div className="text-slate-400 mt-2 text-[11px]">
            &rarr; Tu archivo se creará en: <code className="text-white">app/build/outputs/bundle/release/app-release.aab</code>
          </div>
        </div>
      </div>

      {/* Privacy Policy */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-white text-base">Política de Privacidad para App Gratuita</h3>
              <p className="text-xs text-slate-400">Ley N° 29733 de Perú y políticas de Google Play Store</p>
            </div>
          </div>

          <button
            onClick={() => copyToClipboard(politicaPrivacidadNotiCobros, 'politica')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition border border-slate-700"
          >
            {copiedKey === 'politica' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>Copiar Política</span>
          </button>
        </div>

        <pre className="bg-slate-950 p-4 rounded-2xl border border-slate-800 font-mono text-xs text-slate-300 whitespace-pre-wrap max-h-56 overflow-y-auto leading-relaxed">
          {politicaPrivacidadNotiCobros}
        </pre>
      </div>
    </div>
  );
};
