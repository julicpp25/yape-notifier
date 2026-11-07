import React, { useState, useEffect } from 'react';
import QRCode from 'qrcode';
import {
  Smartphone,
  QrCode,
  Usb,
  Download,
  AlertTriangle,
  CheckCircle2,
  ExternalLink,
  Copy,
  Check,
  ShieldCheck,
  Volume2,
  Play,
  ArrowRight,
} from 'lucide-react';

export const PhoneTestingGuide: React.FC = () => {
  const [qrAppUrl, setQrAppUrl] = useState<string>('');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Selector for QR target:
  // 'github-actions': direct public link to the compiled APK workflow
  // 'github-repo': public GitHub repository
  // 'web-preview': cloud run preview
  const [qrType, setQrType] = useState<'github-actions' | 'github-repo' | 'web-preview'>('github-actions');

  const githubActionsUrl = 'https://github.com/julicpp25/yape-notifier/actions/runs/42036189200';
  const githubRepoUrl = 'https://github.com/julicpp25/yape-notifier';
  const sharedUrl =
    typeof window !== 'undefined'
      ? window.location.origin
      : 'https://ais-pre-clato73fea7d4jbgsu6ig6-373507003832.us-east1.run.app';

  const currentQrTarget =
    qrType === 'github-actions'
      ? githubActionsUrl
      : qrType === 'github-repo'
      ? githubRepoUrl
      : sharedUrl;

  useEffect(() => {
    QRCode.toDataURL(currentQrTarget, {
      width: 240,
      margin: 1,
      color: {
        dark: '#1E1B4B',
        light: '#FFFFFF',
      },
    })
      .then((url) => setQrAppUrl(url))
      .catch((e) => console.error(e));
  }, [currentQrTarget]);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(id);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  return (
    <div className="space-y-8 animate-fadeIn max-w-5xl mx-auto">
      {/* EXPLICACION AL ERROR "NO TIENES ACCESO A LA PAGINA" */}
      <div className="bg-gradient-to-r from-red-950/60 via-amber-950/40 to-slate-900 border-2 border-amber-500/50 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-500/20 border border-amber-400/40 flex items-center justify-center shrink-0 text-amber-400">
            <AlertTriangle className="w-7 h-7" />
          </div>
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/20 border border-amber-400/30 text-amber-300 text-xs font-bold">
              <span>Resolución al error: "No tienes acceso a la página"</span>
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
              ¿Por qué te salió ese mensaje al escanear con la cámara de tu celular?
            </h2>
            <p className="text-slate-300 text-xs sm:text-sm leading-relaxed">
              El QR anterior abría la URL de desarrollo de <strong>Google AI Studio</strong> (<code className="text-amber-300">ais-pre-...run.app</code>). Ese enlace está protegido y <strong>requiere que tu navegador tenga abierta la sesión de desarrollador de Google</strong>. Al escanear con la cámara normal del celular, se abre un navegador sin esa sesión y Google muestra el mensaje <em>"No tienes acceso a la página"</em>.
            </p>
            <div className="mt-3 p-3.5 bg-slate-950/80 rounded-2xl border border-amber-500/30 text-xs space-y-2">
              <div className="font-bold text-amber-300 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span>¿Cómo probar Yape y Plin de verdad en tu celular?</span>
              </div>
              <p className="text-slate-200">
                Incluso si se abriera la web en tu celular, <strong>un navegador web NO puede escuchar las notificaciones de Yape</strong> por seguridad de Android (ninguna página web puede espiar las notificaciones de otras aplicaciones).
              </p>
              <p className="text-emerald-300 font-semibold">
                &rarr; Para que tu celular escuche los cobros de Yape/Plin y los cante en voz alta en tu tienda, <strong>debes instalar el archivo APK directamente en tu celular Android</strong>. A continuación tienes el QR público y los pasos exactos:
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* METODO RECOMENDADO: INSTALACION DEL APK NATIVO */}
      <div className="bg-slate-900/90 border border-cyan-500/30 rounded-3xl p-6 sm:p-8 shadow-xl space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-cyan-600/20 border border-cyan-500/40 flex items-center justify-center text-cyan-400 shrink-0">
              <Download className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] uppercase font-bold bg-cyan-500/20 text-cyan-300 px-2 py-0.5 rounded-full">
                  Método 1 • APK Oficial
                </span>
                <h3 className="font-bold text-white text-lg">Descarga e Instala el APK en tu Celular</h3>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Escanea el código con tu celular (es público y no da error de acceso) o envíatelo por WhatsApp
              </p>
            </div>
          </div>

          <a
            href="/noticobros-playstore-ready.zip"
            download
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white font-bold text-xs shadow-lg shadow-purple-600/30 transition shrink-0"
          >
            <Download className="w-4 h-4" />
            <span>Descargar Código Modificado (ZIP)</span>
          </a>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-start">
          {/* QR Code Container with Selector */}
          <div className="md:col-span-5 bg-white p-5 rounded-2xl flex flex-col items-center justify-center shadow-2xl border border-slate-700">
            {/* Tab switch for QR */}
            <div className="flex gap-1 bg-slate-100 p-1 rounded-xl mb-3 w-full text-[10px] font-bold">
              <button
                onClick={() => setQrType('github-actions')}
                className={`flex-1 py-1.5 px-2 rounded-lg transition text-center ${
                  qrType === 'github-actions'
                    ? 'bg-cyan-600 text-white shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                📦 APK (GitHub)
              </button>
              <button
                onClick={() => setQrType('github-repo')}
                className={`flex-1 py-1.5 px-2 rounded-lg transition text-center ${
                  qrType === 'github-repo'
                    ? 'bg-purple-600 text-white shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                💻 Repositorio
              </button>
              <button
                onClick={() => setQrType('web-preview')}
                className={`flex-1 py-1.5 px-2 rounded-lg transition text-center ${
                  qrType === 'web-preview'
                    ? 'bg-slate-700 text-white shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                🌐 Web POS
              </button>
            </div>

            {qrAppUrl ? (
              <img src={qrAppUrl} alt="Escanear en celular" className="w-48 h-48 object-contain" />
            ) : (
              <div className="w-48 h-48 bg-slate-100 flex items-center justify-center text-xs text-slate-400">
                Cargando QR...
              </div>
            )}

            <div className="mt-3 text-center">
              <span className="text-xs font-black text-slate-900 uppercase tracking-wider block">
                {qrType === 'github-actions' && '📲 Escanea para descargar el APK en tu celular'}
                {qrType === 'github-repo' && '📂 Escanea para ver tu código en GitHub'}
                {qrType === 'web-preview' && '⚠️ Requiere cuenta de Google iniciada'}
              </span>
              <span className="text-[10px] text-slate-600 block mt-1 max-w-[220px]">
                {qrType === 'github-actions' && 'Abre la compilación pública de tu app en GitHub (sin error 403)'}
                {qrType === 'github-repo' && 'Abre github.com/julicpp25/yape-notifier'}
                {qrType === 'web-preview' && 'Abre la vista previa web de la caja'}
              </span>
            </div>

            {/* Direct link button */}
            <a
              href={currentQrTarget}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-3 w-full py-2 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-[11px] font-bold flex items-center justify-center gap-1.5 transition text-center"
            >
              <span>Abrir enlace en pestaña nueva</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>

            <div className="mt-2 w-full flex items-center gap-1.5">
              <input
                type="text"
                readOnly
                value={currentQrTarget}
                className="w-full bg-slate-100 border border-slate-300 rounded-lg px-2 py-1 text-[10px] text-slate-700 font-mono outline-none"
              />
              <button
                onClick={() => copyToClipboard(currentQrTarget, 'qr-url')}
                className="p-1 rounded-lg bg-slate-200 hover:bg-slate-300 text-slate-700 transition shrink-0"
                title="Copiar URL"
              >
                {copiedKey === 'qr-url' ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          {/* Step-by-Step Instructions */}
          <div className="md:col-span-7 space-y-3.5 text-xs text-slate-300">
            <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 flex items-start gap-3">
              <span className="w-6 h-6 rounded-full bg-cyan-600/30 text-cyan-400 font-bold flex items-center justify-center shrink-0">1</span>
              <div className="space-y-1">
                <strong className="text-white block text-sm">Descarga el APK desde GitHub en tu celular:</strong>
                <p className="text-slate-300 leading-relaxed">
                  Tu repositorio de GitHub ya compila automáticamente el APK con cada cambio mediante <strong>GitHub Actions</strong>.
                </p>
                <div className="mt-2 p-2.5 bg-slate-900 rounded-xl border border-slate-700/80 space-y-1">
                  <span className="text-cyan-400 font-semibold block text-[11px]">Pasos en tu celular:</span>
                  <ol className="list-decimal list-inside text-[11px] text-slate-300 space-y-1">
                    <li>Escanea el QR blanco de la izquierda con la cámara de tu celular o abre <a href={githubActionsUrl} target="_blank" rel="noopener noreferrer" className="text-cyan-400 underline font-mono">este enlace</a>.</li>
                    <li>Baja hasta el final de la pantalla hasta la sección llamada <strong className="text-white">"Artifacts"</strong>.</li>
                    <li>Toca el botón <strong className="text-emerald-400">"App-Yape-Notifier"</strong> (pesa 4.8 MB).</li>
                    <li>Se descargará un archivo <code className="text-cyan-300">.zip</code>. Ábrelo desde tus descargas y toca <strong className="text-white">app-debug.apk</strong> para instalarlo.</li>
                  </ol>
                </div>
              </div>
            </div>

            <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 flex items-start gap-3">
              <span className="w-6 h-6 rounded-full bg-purple-600/30 text-purple-400 font-bold flex items-center justify-center shrink-0">2</span>
              <div className="space-y-1">
                <strong className="text-white block text-sm">Método alternativo: Envíate el APK por WhatsApp</strong>
                <p className="text-slate-300 leading-relaxed">
                  Si tienes Android Studio abierto en tu computadora:
                </p>
                <div className="mt-1.5 p-2 bg-slate-900 rounded-lg font-mono text-[11px] text-purple-300">
                  ./gradlew assembleDebug
                </div>
                <p className="text-[11px] text-slate-400 mt-1">
                  El archivo estará listo en: <code className="text-white">app/build/outputs/apk/debug/app-debug.apk</code>. Arrástralo a tu WhatsApp Web o Telegram Web para enviártelo a tu celular y tocarlo para instalar.
                </p>
              </div>
            </div>

            <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 flex items-start gap-3">
              <span className="w-6 h-6 rounded-full bg-emerald-600/30 text-emerald-400 font-bold flex items-center justify-center shrink-0">3</span>
              <div className="space-y-1">
                <strong className="text-white block text-sm">Al instalar: "Instalar de todas formas"</strong>
                <p className="text-slate-300">
                  Como el APK es tuyo propio y aún no está firmado con la clave pública de Google Play Store, Android te preguntará si deseas instalarlo: pulsa <strong className="text-white">"Instalar de todas formas"</strong>.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* METODO 2: PROBAR APK NATIVO CON NOTIFICACIONES REALES */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-cyan-600/20 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
            <Smartphone className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-[10px] uppercase font-bold bg-cyan-500/20 text-cyan-300 px-2 py-0.5 rounded-full">
                Método 2 • App Nativa
              </span>
              <h3 className="font-bold text-white text-base">Instalar el APK Nativo de Android (Lector Real de Yape/Plin)</h3>
            </div>
            <p className="text-xs text-slate-400">
              Para que la app escuche en segundo plano las notificaciones reales que te lleguen de Yape y Plin
            </p>
          </div>
        </div>

        {/* Option 2.1: Cable USB */}
        <div className="bg-slate-950 p-5 rounded-2xl border border-slate-800 space-y-3 text-xs">
          <div className="flex items-center gap-2 text-cyan-400 font-bold">
            <Usb className="w-4 h-4" />
            <span>Opción A: Conectando el celular a tu PC por cable USB (Recomendada)</span>
          </div>
          <ol className="list-decimal list-inside space-y-2 text-slate-300 leading-relaxed pl-1">
            <li>
              En tu celular Android, ve a <strong className="text-white">Ajustes &gt; Acerca del teléfono</strong>.
            </li>
            <li>
              Busca <strong className="text-white">"Número de compilación"</strong> (o versión de MIUI/HyperOS si es Xiaomi) y tócalo <strong className="text-purple-300">7 veces seguidas</strong> hasta que diga <em>"¡Ya eres desarrollador!"</em>.
            </li>
            <li>
              Regresa a Ajustes &gt; busca <strong className="text-white">Opciones de desarrollador</strong> y activa la casilla <strong className="text-emerald-400">Depuración USB (USB Debugging)</strong>.
            </li>
            <li>
              Conecta tu celular a tu computadora mediante el cable USB. (En la pantalla del celular saldrá un aviso: marca <em>"Permitir siempre desde esta computadora"</em>).
            </li>
            <li>
              Abre el proyecto en <strong className="text-white">Android Studio</strong>. En la barra superior verás el nombre de tu teléfono.
            </li>
            <li>
              Haz clic en el botón verde de reproducir <strong className="text-emerald-400 font-bold">Run (▶)</strong>. La app se compilará y se instalará sola en tu celular en menos de un minuto.
            </li>
          </ol>
        </div>

        {/* Option 2.2: Generar APK manual */}
        <div className="bg-slate-950 p-5 rounded-2xl border border-slate-800 space-y-3 text-xs">
          <div className="flex items-center gap-2 text-purple-400 font-bold">
            <Download className="w-4 h-4" />
            <span>Opción B: Generar el archivo APK e instalarlo directamente</span>
          </div>
          <p className="text-slate-300">
            Si prefieres generar el archivo instalador <code className="text-emerald-400">.apk</code> para enviártelo por WhatsApp Web o Telegram a tu celular:
          </p>
          <div className="bg-slate-900 p-3 rounded-xl border border-slate-800 font-mono text-xs text-cyan-300 space-y-1">
            <div className="text-slate-500"># En la terminal de Android Studio ejecuta:</div>
            <div>./gradlew assembleDebug</div>
            <div className="text-slate-400 text-[11px] mt-1">
              &rarr; Tu archivo APK estará en: <code className="text-white">app/build/outputs/apk/debug/app-debug.apk</code>
            </div>
          </div>
          <p className="text-slate-400 text-[11px]">
            Pásate ese archivo a tu celular, tócalo para instalar y selecciona "Instalar de todas formas".
          </p>
        </div>

        {/* CRITICAL STEP: PERMISSIONS & AJUSTE RESTRINGIDO */}
        <div className="bg-amber-950/40 border border-amber-500/40 rounded-2xl p-5 space-y-3 text-xs">
          <div className="flex items-center gap-2 text-amber-400 font-bold">
            <AlertTriangle className="w-4 h-4" />
            <span>Paso Crucial en tu Celular: Activar el Lector de Notificaciones</span>
          </div>
          <p className="text-slate-200 leading-relaxed">
            Una vez que abras NotiCobros en tu celular, debes conceder el permiso para que lea los cobros de Yape:
          </p>
          <div className="bg-slate-950/80 p-3.5 rounded-xl border border-amber-500/20 space-y-2 text-slate-300">
            <div className="font-semibold text-white">
              ⚠️ ¿Te sale el mensaje "Ajuste Restringido" en Android 13 o 14 (Xiaomi, Samsung, Motorola)?
            </div>
            <p className="text-[11px] text-slate-300 leading-relaxed">
              Google introdujo una protección para apps instaladas fuera de Play Store. Se soluciona en 5 segundos así:
            </p>
            <ol className="list-decimal list-inside space-y-1 text-[11px] text-slate-300 pl-1">
              <li>Ve a los <strong className="text-white">Ajustes</strong> de tu celular &gt; <strong className="text-white">Aplicaciones</strong>.</li>
              <li>Busca y toca <strong className="text-purple-300 font-bold">NotiCobros</strong>.</li>
              <li>Arriba a la derecha, toca los <strong className="text-white">3 puntos (⋮)</strong>.</li>
              <li>Toca <strong className="text-emerald-400">"Permitir ajustes restringidos"</strong> e introduce tu huella o PIN.</li>
              <li>Vuelve a abrir la app y ¡listo! Ya podrás encender el interruptor de lectura de notificaciones.</li>
            </ol>
          </div>
        </div>

        {/* First real test */}
        <div className="bg-emerald-950/30 border border-emerald-500/30 rounded-2xl p-5 space-y-2 text-xs">
          <div className="flex items-center gap-2 text-emerald-400 font-bold">
            <CheckCircle2 className="w-4 h-4" />
            <span>¡Tu primera prueba de cobro en vivo!</span>
          </div>
          <p className="text-slate-300 leading-relaxed">
            Una vez activado el lector:
          </p>
          <ul className="list-disc list-inside space-y-1 text-slate-300 text-[11px] pl-1">
            <li>Deja la app abierta o minimizada en segundo plano.</li>
            <li>Pídele a un amigo o cliente que te haga un Yape de prueba (por ejemplo: S/ 1.00).</li>
            <li>En cuanto Yape muestre la notificación en tu barra superior, tu celular emitirá el timbre de caja y anunciará por el altavoz a todo volumen:</li>
          </ul>
          <div className="bg-slate-950 p-3 rounded-xl border border-emerald-500/30 text-emerald-300 font-bold italic mt-2">
            "¡Te llegó un Yape de un sol! ¡A seguir vendiendo!"
          </div>
        </div>
      </div>
    </div>
  );
};
