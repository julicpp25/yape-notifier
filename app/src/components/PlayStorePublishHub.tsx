import React, { useState } from 'react';
import {
  Smartphone,
  CheckCircle2,
  AlertCircle,
  Copy,
  Check,
  ExternalLink,
  ShieldCheck,
  FileCode,
  Download,
  Terminal,
  Cpu,
} from 'lucide-react';

export const PlayStorePublishHub: React.FC = () => {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // AssetLinks State
  const [packageName, setPackageName] = useState('com.colegio.asistenciaescolar');
  const [sha256Fingerprint, setSha256Fingerprint] = useState(
    '14:6D:E9:7D:0F:52:AB:E0:85:47:7E:22:75:A8:14:0A:42:A8:B4:73:72:08:52:95:90:3A:45:9F:50:85:3F:D8'
  );

  // Privacy Policy State
  const [privacyAppName, setPrivacyAppName] = useState('Sistema Escolar QR - Asistencia');
  const [privacyEmail, setPrivacyEmail] = useState('contacto@colegio.edu.pe');

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(id);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  const assetLinksJson = JSON.stringify(
    [
      {
        relation: ['delegate_permission/common.handle_all_urls'],
        target: {
          namespace: 'android_app',
          package_name: packageName.trim() || 'com.colegio.asistenciaescolar',
          sha256_cert_fingerprints: [
            sha256Fingerprint.trim() ||
              '14:6D:E9:7D:0F:52:AB:E0:85:47:7E:22:75:A8:14:0A:42:A8:B4:73:72:08:52:95:90:3A:45:9F:50:85:3F:D8',
          ],
        },
      },
    ],
    null,
    2
  );

  const privacyPolicyText = `# Política de Privacidad de ${privacyAppName}
Última actualización: ${new Date().toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' })}

${privacyAppName} ("la Aplicación") es un sistema de control de asistencia escolar y lectura de códigos QR orientado a instituciones educativas. Esta Política de Privacidad explica el tratamiento de la información conforme a las políticas de Google Play Store y la Ley de Protección de Datos Personales (Ley N° 29733 de la República del Perú).

### 1. Datos que Maneja la Aplicación
- **Datos de Estudiantes:** Nombre, Apellidos, DNI, Grado y Sección educativa.
- **Datos de Asistencia:** Fecha y hora exacta de registro mediante lectura QR.
- **Datos de Apoderados:** Nombre y teléfono móvil exclusivamente para el envío de avisos y notificaciones de ingreso escolar vía WhatsApp o mensajería.
- **Cámara:** La aplicación solicita permiso de uso de la cámara del dispositivo móvil única y exclusivamente para escanear en tiempo real el código QR del carnet escolar. En ningún momento se toman fotos, ni se graban videos, ni se transmiten imágenes a servidores externos.

### 2. Almacenamiento Local y Seguridad
Todos los datos de asistencia y padrón se almacenan localmente en el dispositivo de la institución educativa mediante almacenamiento seguro y caché offline PWA. No se comercializa, arrienda ni transfiere información a terceros bajo ninguna circunstancia.

### 3. Derechos de los Padres y Tutores
Los apoderados pueden solicitar a la dirección del colegio la rectificación o supresión de sus datos de contacto en cualquier momento.

### 4. Contacto del Responsable
Para consultas relacionadas con la privacidad o el tratamiento de datos de la aplicación:
Correo electrónico: ${privacyEmail}
`;

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Overview Banner */}
      <div className="bg-gradient-to-r from-blue-900/40 via-indigo-900/30 to-slate-900 border border-blue-500/30 rounded-3xl p-6 sm:p-8 shadow-2xl">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-400/20 text-emerald-400 text-xs font-semibold mb-3">
          <CheckCircle2 className="w-3.5 h-3.5" />
          <span>Tu Sistema Escolar cumple 100% los requisitos de Play Store</span>
        </div>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          Guía de Publicación en Google Play Store (TWA / AAB)
        </h2>
        <p className="mt-2 text-slate-300 text-xs sm:text-sm max-w-3xl leading-relaxed">
          Ya integramos el lector QR de tu repositorio, el Web App Manifest y el Service Worker.
          Ahora puedes empaquetar la app en un archivo <strong>.aab</strong> oficial para que cualquier colegio o docente la descargue directamente desde Google Play Store.
        </p>
      </div>

      {/* Step by step compilation */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Method 1: PWABuilder */}
        <div className="bg-slate-900/80 border border-blue-500/30 rounded-2xl p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-white text-base">Método 1: PWABuilder (Recomendado)</h3>
                  <p className="text-[11px] text-blue-300">Genera el archivo .aab firmado en 2 minutos</p>
                </div>
              </div>
              <a
                href="https://www.pwabuilder.com/"
                target="_blank"
                rel="noreferrer"
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition"
              >
                <span>PWABuilder.com</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </div>

            <ol className="space-y-3 text-xs text-slate-300 bg-slate-950/60 p-4 rounded-xl border border-slate-800">
              <li className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 font-bold flex items-center justify-center shrink-0 text-[10px]">1</span>
                <span>Ingresa en <strong>pwabuilder.com</strong> y coloca la URL de esta aplicación.</span>
              </li>
              <li className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 font-bold flex items-center justify-center shrink-0 text-[10px]">2</span>
                <span>Haz clic en <strong>Package for Stores</strong> &rarr; elige <strong>Google Play (Android)</strong>.</span>
              </li>
              <li className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 font-bold flex items-center justify-center shrink-0 text-[10px]">3</span>
                <span>Descarga el archivo <code>.aab</code> generado e impórtalo en Google Play Console.</span>
              </li>
            </ol>
          </div>
        </div>

        {/* Method 2: Bubblewrap CLI */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300">
                <Terminal className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-white text-base">Método 2: Google Bubblewrap (Terminal)</h3>
                <p className="text-[11px] text-slate-400">Herramienta oficial CLI de Google Chrome</p>
              </div>
            </div>

            <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 space-y-1 relative">
              <button
                onClick={() =>
                  copyToClipboard(
                    `npm i -g @bubblewrap/cli\nbubblewrap init --manifest=https://tu-colegio.edu.pe/manifest.webmanifest\nbubblewrap build`,
                    'cli'
                  )
                }
                className="absolute top-2 right-2 p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400"
              >
                {copiedKey === 'cli' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              </button>
              <div className="text-slate-500"># Instalar CLI oficial</div>
              <div className="text-emerald-400">npm i -g @bubblewrap/cli</div>
              <div className="text-slate-500 mt-2"># Compilar AAB</div>
              <div className="text-blue-400">bubblewrap init --manifest=URL/manifest.json</div>
              <div className="text-emerald-400">bubblewrap build</div>
            </div>
          </div>
        </div>
      </div>

      {/* Assetlinks.json generator */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">Digital Asset Links (assetlinks.json)</h3>
            <p className="text-xs text-slate-400">
              Obligatorio para que la app se abra sin barra de navegador en los celulares de los profesores
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 pt-2">
          <div className="space-y-3 text-xs">
            <div>
              <label className="text-slate-300 font-medium block mb-1">
                Package Name de la App Android:
              </label>
              <input
                type="text"
                value={packageName}
                onChange={(e) => setPackageName(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>

            <div>
              <label className="text-slate-300 font-medium block mb-1">
                Huella SHA-256 de Google Play Console:
              </label>
              <input
                type="text"
                value={sha256Fingerprint}
                onChange={(e) => setSha256Fingerprint(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Archivo generado:</span>
              <button
                onClick={() => copyToClipboard(assetLinksJson, 'assetlinks')}
                className="flex items-center gap-1 text-blue-400 hover:text-blue-300"
              >
                {copiedKey === 'assetlinks' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>Copiar</span>
              </button>
            </div>
            <pre className="bg-slate-950 p-3 rounded-xl border border-slate-800 font-mono text-[11px] text-emerald-400 max-h-36 overflow-x-auto">
              {assetLinksJson}
            </pre>
          </div>
        </div>
      </div>

      {/* Privacy Policy */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <FileCode className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">Política de Privacidad para Colegios</h3>
              <p className="text-xs text-slate-400">Adaptada a la Ley N° 29733 de Perú y políticas de Google Play</p>
            </div>
          </div>

          <button
            onClick={() => copyToClipboard(privacyPolicyText, 'privacy')}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition border border-slate-700"
          >
            {copiedKey === 'privacy' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>Copiar Política</span>
          </button>
        </div>

        <pre className="bg-slate-950 p-4 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 whitespace-pre-wrap max-h-56 overflow-y-auto leading-relaxed">
          {privacyPolicyText}
        </pre>
      </div>
    </div>
  );
};
