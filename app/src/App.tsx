import { useState, useEffect } from 'react';
import {
  Volume2,
  Smartphone,
  CreditCard,
  FileCode,
  ShieldCheck,
  Github,
  Sparkles,
  TrendingUp,
  Receipt,
  QrCode,
  Upload,
} from 'lucide-react';

import { CobroRecord } from './types/yape';
import { hablarTexto, FRASES_MOTIVADORAS } from './services/yapeParser';
import { playSuccessBeep } from './utils/sound';
import { NotificationSimulator } from './components/NotificationSimulator';
import { PosCajaView } from './components/PosCajaView';
import { AndroidCodeViewer } from './components/AndroidCodeViewer';
import { PlayStoreDeclarationHub } from './components/PlayStoreDeclarationHub';
import { PhoneTestingGuide } from './components/PhoneTestingGuide';
import { QrUploaderModal } from './components/QrUploaderModal';
import { PWAInstallButton } from './components/PWAInstallButton';
import { OfflineIndicator } from './components/OfflineIndicator';

type Tab = 'simulador' | 'caja' | 'pruebas' | 'mejoras' | 'playstore';

const SEED_COBROS: CobroRecord[] = [
  {
    id: 'cob-1',
    hora: '09:15',
    fecha: new Date().toLocaleDateString('es-PE'),
    timestamp: Date.now() - 3600000,
    nombre: 'Juan Carlos Quispe',
    monto: 25.0,
    metodo: 'Yape',
    cajero: 'Caja Principal',
  },
  {
    id: 'cob-2',
    hora: '10:42',
    fecha: new Date().toLocaleDateString('es-PE'),
    timestamp: Date.now() - 1800000,
    nombre: 'María Gómez',
    monto: 18.5,
    metodo: 'Yape',
    cajero: 'Caja Principal',
  },
  {
    id: 'cob-3',
    hora: '11:05',
    fecha: new Date().toLocaleDateString('es-PE'),
    timestamp: Date.now() - 600000,
    nombre: 'Pedro Sánchez',
    monto: 45.0,
    metodo: 'Plin',
    cajero: 'Caja Principal',
  },
];

export default function App() {
  const [activeTab, setActiveTab] = useState<Tab>('pruebas');
  const [showQrModal, setShowQrModal] = useState(false);

  // Merchant QR & ID state
  const [qrBaseYape, setQrBaseYape] = useState<string>(() => {
    try {
      return localStorage.getItem('noticobros_qr_base') || '';
    } catch {
      return '';
    }
  });

  const [tiendaId, setTiendaId] = useState<string>(() => {
    try {
      return localStorage.getItem('noticobros_tienda_id') || 'TIENDA-YAPE-742';
    } catch {
      return 'TIENDA-YAPE-742';
    }
  });

  const [nombreNegocio, setNombreNegocio] = useState<string>(() => {
    try {
      return localStorage.getItem('noticobros_nombre_negocio') || 'Mi Negocio';
    } catch {
      return 'Mi Negocio';
    }
  });

  const [cobros, setCobros] = useState<CobroRecord[]>(() => {
    try {
      const saved = localStorage.getItem('noticobros_ventas');
      return saved ? JSON.parse(saved) : SEED_COBROS;
    } catch {
      return SEED_COBROS;
    }
  });

  const handleNuevoCobro = (cobro: CobroRecord) => {
    setCobros((prev) => {
      const updated = [cobro, ...prev];
      try {
        localStorage.setItem('noticobros_ventas', JSON.stringify(updated));
      } catch (e) {}
      return updated;
    });
  };

  const handleLimpiarCaja = () => {
    if (confirm('¿Deseas cerrar el turno y reiniciar el historial de cobros de la caja?')) {
      setCobros([]);
      localStorage.removeItem('noticobros_ventas');
    }
  };

  const handleQrConfigurado = (qrBase: string, id: string, nombre?: string) => {
    setQrBaseYape(qrBase);
    setTiendaId(id);
    if (nombre) setNombreNegocio(nombre);

    try {
      localStorage.setItem('noticobros_qr_base', qrBase);
      localStorage.setItem('noticobros_tienda_id', id);
      if (nombre) localStorage.setItem('noticobros_nombre_negocio', nombre);
    } catch (e) {}
  };

  const handleSimularPago = (monto: number) => {
    playSuccessBeep();
    const nombresClientes = [
      'Valeria Mendoza',
      'Rodrigo Castillo',
      'Andrea Torres',
      'Gianfranco Ruiz',
      'Camila Paredes',
    ];
    const clienteAleatorio = nombresClientes[Math.floor(Math.random() * nombresClientes.length)];
    const animo = FRASES_MOTIVADORAS[Math.floor(Math.random() * FRASES_MOTIVADORAS.length)];
    const discurso = `¡Te llegó un Yape de ${monto.toFixed(2)} soles! ${animo}`;

    hablarTexto(discurso);

    const now = new Date();
    const cobro: CobroRecord = {
      id: 'cob-' + Date.now(),
      hora: now.toTimeString().substring(0, 5),
      fecha: now.toLocaleDateString('es-PE'),
      timestamp: Date.now(),
      nombre: clienteAleatorio,
      monto,
      metodo: 'Yape',
      cajero: 'Caja Principal',
    };

    handleNuevoCobro(cobro);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-purple-600 selection:text-white">
      <OfflineIndicator />

      {/* Main Header */}
      <header className="border-b border-slate-800/80 bg-slate-900/80 backdrop-blur-md sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-purple-700 via-indigo-700 to-cyan-500 p-0.5 shadow-lg shadow-purple-600/25 flex items-center justify-center">
              <div className="w-full h-full bg-slate-950 rounded-[14px] flex items-center justify-center">
                <Volume2 className="w-5 h-5 text-cyan-400" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-extrabold text-base sm:text-lg text-white tracking-tight">
                  NotiCobros <span className="text-purple-400">Yape &amp; Plin</span>
                </h1>
                <span className="hidden sm:inline-flex text-[10px] uppercase font-bold tracking-wider bg-purple-500/10 text-purple-300 border border-purple-500/20 px-2 py-0.5 rounded-full">
                  Target SDK 34
                </span>
              </div>
              <p className="text-[11px] text-slate-400 truncate max-w-xs sm:max-w-md">
                Asistente de cobros por voz • {nombreNegocio} ({tiendaId})
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 sm:gap-3">
            <button
              onClick={() => setShowQrModal(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600/20 hover:bg-purple-600/30 text-purple-300 border border-purple-500/30 text-xs font-semibold transition"
            >
              <Upload className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">{qrBaseYape ? 'Mi QR Vinculado' : 'Adjuntar QR'}</span>
              <span className="sm:hidden">QR</span>
            </button>

            <PWAInstallButton />
          </div>
        </div>
      </header>

      {/* Navigation Bar */}
      <nav className="bg-slate-900/50 border-b border-slate-800/80 overflow-x-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex space-x-1 sm:space-x-2 py-2">
          <button
            onClick={() => setActiveTab('pruebas')}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold transition whitespace-nowrap ${
              activeTab === 'pruebas'
                ? 'bg-gradient-to-r from-cyan-600 to-blue-600 text-white shadow-md shadow-cyan-600/30'
                : 'text-cyan-400 hover:text-cyan-300 hover:bg-slate-800/60'
            }`}
          >
            <Smartphone className="w-4 h-4" />
            <span>📱 Cómo Probar en tu Celular</span>
          </button>

          <button
            onClick={() => setActiveTab('simulador')}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold transition whitespace-nowrap ${
              activeTab === 'simulador'
                ? 'bg-purple-600 text-white shadow-md shadow-purple-600/30'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <Volume2 className="w-4 h-4" />
            <span>Simulador de Notificaciones (Voz)</span>
          </button>

          <button
            onClick={() => setActiveTab('caja')}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold transition whitespace-nowrap ${
              activeTab === 'caja'
                ? 'bg-purple-600 text-white shadow-md shadow-purple-600/30'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <CreditCard className="w-4 h-4" />
            <span>Caja POS &amp; QR ({cobros.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('mejoras')}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold transition whitespace-nowrap ${
              activeTab === 'mejoras'
                ? 'bg-purple-600 text-white shadow-md shadow-purple-600/30'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <FileCode className="w-4 h-4" />
            <span>Código Android Mejorado</span>
          </button>

          <button
            onClick={() => setActiveTab('playstore')}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold transition whitespace-nowrap ${
              activeTab === 'playstore'
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30'
                : 'text-emerald-400 hover:bg-emerald-500/10'
            }`}
          >
            <ShieldCheck className="w-4 h-4" />
            <span>Checklist Play Store</span>
          </button>
        </div>
      </nav>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === 'pruebas' && <PhoneTestingGuide />}

        {activeTab === 'simulador' && (
          <NotificationSimulator onCobroRegistrado={handleNuevoCobro} />
        )}

        {activeTab === 'caja' && (
          <PosCajaView
            cobros={cobros}
            qrBaseYape={qrBaseYape}
            tiendaId={tiendaId}
            nombreNegocio={nombreNegocio}
            onAbrirConfiguradorQR={() => setShowQrModal(true)}
            onSimularPago={handleSimularPago}
            onLimpiarCaja={handleLimpiarCaja}
          />
        )}

        {activeTab === 'mejoras' && <AndroidCodeViewer />}

        {activeTab === 'playstore' && <PlayStoreDeclarationHub />}
      </main>

      {/* QR Uploader Modal */}
      {showQrModal && (
        <QrUploaderModal
          qrActual={qrBaseYape}
          tiendaIdActual={tiendaId}
          onQrConfigurado={handleQrConfigurado}
          onClose={() => setShowQrModal(false)}
        />
      )}

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-slate-950 py-6 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Volume2 className="w-4 h-4 text-purple-400" />
            <span>NotiCobros: Asistente de Voz y Caja para Yape &amp; Plin</span>
          </div>

          <div className="flex items-center gap-4 text-slate-400">
            <span>com.notiyape.app</span>
            <span>•</span>
            <span className="text-cyan-400 font-medium">Guía de Pruebas en Móvil Integrada</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
