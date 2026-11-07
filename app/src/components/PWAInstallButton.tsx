import React, { useState } from 'react';
import { usePWAInstall } from '../hooks/usePWAInstall';
import { Download, CheckCircle, Share, PlusSquare, Smartphone } from 'lucide-react';

export const PWAInstallButton: React.FC = () => {
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [showIOSGuide, setShowIOSGuide] = useState(false);
  const [justInstalled, setJustInstalled] = useState(false);

  if (isInstalled || justInstalled) {
    return (
      <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-medium">
        <CheckCircle className="w-3.5 h-3.5" />
        <span>Instalada / Modo App</span>
      </div>
    );
  }

  if (isInstallable) {
    return (
      <button
        onClick={async () => {
          const success = await install();
          if (success) setJustInstalled(true);
        }}
        className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 px-4 py-2 text-sm font-semibold text-white shadow-lg shadow-blue-500/20 active:scale-95 transition-all"
        title="Instalar en tu dispositivo Android o escritorio"
      >
        <Download className="w-4 h-4 animate-pulse" />
        <span>Instalar App</span>
      </button>
    );
  }

  if (isIOS) {
    return (
      <>
        <button
          onClick={() => setShowIOSGuide(true)}
          className="flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800/80 hover:bg-slate-700 px-3 py-1.5 text-xs font-medium text-slate-200 transition"
        >
          <Smartphone className="w-3.5 h-3.5 text-sky-400" />
          <span>Instalar en iOS</span>
        </button>

        {showIOSGuide && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
            <div className="w-full max-w-sm rounded-2xl bg-slate-900 border border-slate-800 p-6 shadow-2xl text-slate-200">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-semibold text-white">Instalar en iPhone / iPad</h3>
                  <p className="text-xs text-slate-400">Safari para iOS</p>
                </div>
              </div>

              <div className="space-y-3 text-sm text-slate-300">
                <div className="flex items-start gap-3 bg-slate-800/50 p-2.5 rounded-xl border border-slate-700/50">
                  <Share className="w-4 h-4 text-blue-400 mt-0.5 shrink-0" />
                  <span>1. Toca el botón <strong>Compartir</strong> en la barra inferior de Safari.</span>
                </div>
                <div className="flex items-start gap-3 bg-slate-800/50 p-2.5 rounded-xl border border-slate-700/50">
                  <PlusSquare className="w-4 h-4 text-emerald-400 mt-0.5 shrink-0" />
                  <span>2. Desplázate hacia abajo y selecciona <strong>Agregar a inicio</strong>.</span>
                </div>
              </div>

              <button
                onClick={() => setShowIOSGuide(false)}
                className="mt-5 w-full rounded-xl bg-blue-600 hover:bg-blue-500 py-2.5 text-sm font-semibold text-white transition shadow-md"
              >
                Entendido
              </button>
            </div>
          </div>
        )}
      </>
    );
  }

  return (
    <button
      onClick={() => {
        alert("Para probar la instalación:\n- En Chrome/Edge: Haz clic en el ícono de instalación en la barra de direcciones o en el menú ⋮ > 'Instalar App'.\n- En móvil Android: Abre el menú del navegador y pulsa 'Instalar aplicación' o 'Agregar a la pantalla principal'.");
      }}
      className="flex items-center gap-1.5 rounded-lg border border-slate-700/80 bg-slate-800/50 hover:bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-300 transition"
      title="Información de instalación PWA"
    >
      <Download className="w-3.5 h-3.5 text-slate-400" />
      <span>Instalable PWA</span>
    </button>
  );
};
