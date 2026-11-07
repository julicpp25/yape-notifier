import React, { useState } from 'react';
import { parseNotificacion, hablarTexto } from '../services/yapeParser';
import { ParseResult, CobroRecord } from '../types/yape';
import { playSuccessBeep, playWarningBeep } from '../utils/sound';
import {
  Volume2,
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  Play,
  RotateCcw,
  PlusCircle,
  ShieldCheck,
  Check,
  Smartphone,
} from 'lucide-react';

interface Props {
  onCobroRegistrado: (cobro: CobroRecord) => void;
}

const EJEMPLOS = [
  {
    titulo: 'Yape Clásico',
    texto: '¡Te llegó un Yape! Juan Carlos Quispe te envió S/ 25.00',
    color: 'border-purple-500/40 bg-purple-950/20 text-purple-300',
    icon: '🟣',
  },
  {
    titulo: 'Yape Nueva Versión (te yapeó)',
    texto: 'María Gómez te yapeó S/ 18.50',
    color: 'border-purple-500/40 bg-purple-950/20 text-purple-300',
    icon: '🟣',
  },
  {
    titulo: 'Plin Interbank / BBVA',
    texto: 'Plin: Recibiste un pago de S/ 45.00 de Pedro Sánchez',
    color: 'border-cyan-500/40 bg-cyan-950/20 text-cyan-300',
    icon: '🟢',
  },
  {
    titulo: 'Transferencia Bancaria',
    texto: 'Sofía Mendoza te transfirió S/ 120.00',
    color: 'border-blue-500/40 bg-blue-950/20 text-blue-300',
    icon: '🔵',
  },
  {
    titulo: 'Publicidad (Escudo 1)',
    texto: '¡Pide tu préstamo al instante de S/ 5,000 en cuotas fijas! Aprovecha la promoción hoy.',
    color: 'border-rose-500/40 bg-rose-950/20 text-rose-300',
    icon: '🚫',
  },
];

export const NotificationSimulator: React.FC<Props> = ({ onCobroRegistrado }) => {
  const [inputText, setInputText] = useState(
    '¡Te llegó un Yape! Juan Carlos Quispe te envió S/ 25.00'
  );
  const [resultado, setResultado] = useState<ParseResult | null>(() =>
    parseNotificacion('¡Te llegó un Yape! Juan Carlos Quispe te envió S/ 25.00')
  );
  const [estaHablando, setEstaHablando] = useState(false);
  const [registradoConfirmado, setRegistradoConfirmado] = useState(false);

  const handleAnalizar = (texto: string) => {
    setInputText(texto);
    const res = parseNotificacion(texto);
    setResultado(res);
    setRegistradoConfirmado(false);

    if (res.esValido) {
      playSuccessBeep();
      setEstaHablando(true);
      hablarTexto(res.fraseVoz, () => setEstaHablando(false));
    } else {
      playWarningBeep();
    }
  };

  const handleReproducirVoz = () => {
    if (!resultado || !resultado.esValido) return;
    setEstaHablando(true);
    playSuccessBeep();
    hablarTexto(resultado.fraseVoz, () => setEstaHablando(false));
  };

  const handleRegistrarEnCaja = () => {
    if (!resultado || !resultado.esValido) return;

    const now = new Date();
    const cobro: CobroRecord = {
      id: 'cob-' + Date.now(),
      hora: now.toTimeString().substring(0, 5),
      fecha: now.toLocaleDateString('es-PE'),
      timestamp: Date.now(),
      nombre: resultado.remitente,
      monto: resultado.monto,
      metodo: resultado.metodo as CobroRecord['metodo'],
      cajero: 'Caja Principal',
    };

    onCobroRegistrado(cobro);
    setRegistradoConfirmado(true);
    setTimeout(() => setRegistradoConfirmado(false), 3000);
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Hero Banner */}
      <div className="bg-gradient-to-r from-purple-950/60 via-indigo-950/40 to-slate-900 border border-purple-500/30 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden">
        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/20 border border-purple-400/30 text-purple-300 text-xs font-semibold mb-3">
            <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
            <span>Simulador de Notificaciones NotiCobros</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Prueba en vivo cómo tu app detecta y canta los cobros de Yape y Plin
          </h2>
          <p className="mt-2 text-slate-300 text-xs sm:text-sm leading-relaxed">
            Este simulador ejecuta en tiempo real la misma lógica de{' '}
            <code className="text-purple-300 font-semibold font-mono">YapeListenerService.kt</code> de tu app de Android.
            Escribe o selecciona una notificación para ver los escudos de seguridad, la extracción de monto y escuchar el anuncio por voz.
          </p>
        </div>
      </div>

      {/* Preset Example Buttons */}
      <div className="space-y-2">
        <label className="text-xs font-semibold text-slate-300 uppercase tracking-wider block">
          Ejemplos rápidos de notificaciones en Perú:
        </label>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {EJEMPLOS.map((ex, idx) => (
            <button
              key={idx}
              onClick={() => handleAnalizar(ex.texto)}
              className={`p-3 rounded-2xl border text-left transition hover:scale-[1.01] active:scale-[0.99] flex flex-col justify-between ${ex.color}`}
            >
              <div className="flex items-center gap-2 mb-1.5">
                <span>{ex.icon}</span>
                <span className="font-bold text-xs text-white">{ex.titulo}</span>
              </div>
              <p className="text-[11px] text-slate-300 line-clamp-2 italic">
                "{ex.texto}"
              </p>
            </button>
          ))}
        </div>
      </div>

      {/* Input Textarea and Analysis Result */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Input Box */}
        <div className="lg:col-span-7 bg-slate-900/80 border border-slate-800 rounded-3xl p-6 shadow-xl flex flex-col justify-between space-y-4">
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-semibold text-white flex items-center gap-2">
                <Smartphone className="w-4 h-4 text-purple-400" />
                <span>Texto de la Notificación Recibida por Android:</span>
              </label>
              <button
                onClick={() => setInputText('')}
                className="text-[11px] text-slate-400 hover:text-white transition flex items-center gap-1"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Limpiar</span>
              </button>
            </div>
            <textarea
              rows={4}
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              placeholder="Pega aquí el texto de cualquier notificación de Yape, Plin o banco..."
              className="w-full bg-slate-950 border border-slate-700/80 rounded-2xl p-3.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-purple-500 font-mono transition leading-relaxed"
            />
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              onClick={() => handleAnalizar(inputText)}
              className="flex-1 flex items-center justify-center gap-2 bg-gradient-to-r from-purple-700 to-indigo-600 hover:from-purple-600 hover:to-indigo-500 text-white font-semibold py-3 px-6 rounded-2xl text-xs transition shadow-lg shadow-purple-600/25"
            >
              <Play className="w-4 h-4" />
              <span>Procesar Notificación y Cantar Cobro</span>
            </button>
          </div>
        </div>

        {/* Right: Parser Result Card */}
        <div className="lg:col-span-5 bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-xl flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Resultado del Algoritmo
              </span>
              {resultado?.esValido ? (
                <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-bold">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Cobro Válido</span>
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/30 text-xs font-bold">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  <span>No es Cobro</span>
                </span>
              )}
            </div>

            {resultado?.esValido ? (
              <div className="space-y-4">
                {/* Method & Amount highlight */}
                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800/80 flex items-center justify-between">
                  <div>
                    <span className="text-[11px] text-slate-400 block">Billetera / Método</span>
                    <span className="text-base font-extrabold text-purple-400">
                      {resultado.metodo}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-[11px] text-slate-400 block">Monto en Soles</span>
                    <span className="text-2xl font-black text-emerald-400">
                      S/ {resultado.monto.toFixed(2)}
                    </span>
                  </div>
                </div>

                {/* Sender */}
                <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-800 text-xs">
                  <span className="text-slate-500 block text-[10px]">Cliente / Remitente extraído:</span>
                  <span className="font-bold text-white text-sm">{resultado.remitente}</span>
                </div>

                {/* Voice speech text */}
                <div className="bg-purple-950/30 border border-purple-500/30 p-3.5 rounded-xl text-xs space-y-1">
                  <div className="flex items-center justify-between text-purple-300 font-semibold text-[11px]">
                    <span className="flex items-center gap-1.5">
                      <Volume2 className="w-3.5 h-3.5 text-purple-400" />
                      <span>Anuncio en Voz Alta (TTS):</span>
                    </span>
                    {estaHablando && <span className="animate-pulse text-cyan-400 text-[10px]">Hablando...</span>}
                  </div>
                  <p className="text-white italic font-medium leading-relaxed">
                    "{resultado.fraseVoz}"
                  </p>
                </div>
              </div>
            ) : (
              <div className="p-5 rounded-2xl bg-slate-950 border border-slate-800 text-center space-y-3">
                <div className="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center mx-auto text-slate-400">
                  <ShieldCheck className="w-6 h-6" />
                </div>
                <div>
                  <h4 className="font-bold text-white text-xs">Notificación Ignorada</h4>
                  <p className="text-[11px] text-slate-400 mt-1 leading-relaxed">
                    {resultado?.razonDescarte || 'Escribe o selecciona una notificación para analizar.'}
                  </p>
                </div>
              </div>
            )}
          </div>

          {resultado?.esValido && (
            <div className="mt-4 pt-4 border-t border-slate-800/80 flex flex-col sm:flex-row gap-2">
              <button
                onClick={handleReproducirVoz}
                className="flex-1 flex items-center justify-center gap-2 bg-slate-800 hover:bg-slate-700 text-white font-medium py-2.5 rounded-xl text-xs transition border border-slate-700"
              >
                <Volume2 className="w-3.5 h-3.5 text-purple-400" />
                <span>Repetir Voz 🔊</span>
              </button>

              <button
                onClick={handleRegistrarEnCaja}
                className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs font-semibold transition ${
                  registradoConfirmado
                    ? 'bg-emerald-600 text-white'
                    : 'bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30'
                }`}
              >
                {registradoConfirmado ? <Check className="w-3.5 h-3.5" /> : <PlusCircle className="w-3.5 h-3.5" />}
                <span>{registradoConfirmado ? '¡Sumado a Caja!' : 'Sumar a Caja POS'}</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
