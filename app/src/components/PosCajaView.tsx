import React, { useState, useEffect } from 'react';
import QRCode from 'qrcode';
import { CobroRecord } from '../types/yape';
import {
  CreditCard,
  QrCode,
  Eye,
  EyeOff,
  Trash2,
  Receipt,
  Upload,
  Sparkles,
  CheckCircle2,
  Volume2,
  Store,
} from 'lucide-react';

interface Props {
  cobros: CobroRecord[];
  qrBaseYape: string;
  tiendaId: string;
  nombreNegocio: string;
  onAbrirConfiguradorQR: () => void;
  onSimularPago: (monto: number) => void;
  onLimpiarCaja: () => void;
}

export const PosCajaView: React.FC<Props> = ({
  cobros,
  qrBaseYape,
  tiendaId,
  nombreNegocio,
  onAbrirConfiguradorQR,
  onSimularPago,
  onLimpiarCaja,
}) => {
  const [tecladoMonto, setTecladoMonto] = useState<string>('0');
  const [saldoOculto, setSaldoOculto] = useState<boolean>(false);
  const [qrDataUrl, setQrDataUrl] = useState<string>('');

  const totalVentas = cobros.reduce((acc, c) => acc + c.monto, 0);

  // Keypad inputs
  const handleNumeroClick = (num: string) => {
    if (num === '.') {
      if (tecladoMonto.includes('.')) return;
      setTecladoMonto(tecladoMonto + '.');
    } else {
      if (tecladoMonto === '0') {
        setTecladoMonto(num);
      } else {
        if (tecladoMonto.includes('.') && tecladoMonto.split('.')[1].length >= 2) return;
        setTecladoMonto(tecladoMonto + num);
      }
    }
  };

  const handleBorrar = () => {
    if (tecladoMonto.length <= 1) {
      setTecladoMonto('0');
    } else {
      setTecladoMonto(tecladoMonto.slice(0, -1));
    }
  };

  const handleLimpiarTeclado = () => {
    setTecladoMonto('0');
  };

  // Generate dynamic QR when tecladoMonto or qrBaseYape changes
  useEffect(() => {
    const montoNum = parseFloat(tecladoMonto) || 0;
    // Base payload: if merchant uploaded QR, use their raw string, otherwise generic demo
    const base = qrBaseYape || `00020101021153036045802PE5915${nombreNegocio.replace(/\s/g, '')}6004LIMA`;
    
    // Yape / EMVCo dynamic tag or payload
    const payload = qrBaseYape
      ? `${qrBaseYape}#monto=${montoNum}`
      : `yape://pago?monto=${montoNum}&comercio=${encodeURIComponent(nombreNegocio)}&id=${tiendaId}`;

    QRCode.toDataURL(payload, {
      width: 240,
      margin: 1,
      color: {
        dark: '#6B1D7B', // Yape Purple
        light: '#FFFFFF',
      },
    })
      .then((url) => setQrDataUrl(url))
      .catch((e) => console.error(e));
  }, [tecladoMonto, qrBaseYape, nombreNegocio, tiendaId]);

  const montoActualNum = parseFloat(tecladoMonto || '0');

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Sales Summary Bar */}
      <div className="bg-gradient-to-r from-purple-950/80 via-slate-900 to-slate-900 border border-purple-500/30 rounded-3xl p-6 shadow-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Total Cobrado Hoy en Caja
            </span>
            <button
              onClick={() => setSaldoOculto(!saldoOculto)}
              className="p-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
              title={saldoOculto ? 'Mostrar saldo' : 'Ocultar saldo'}
            >
              {saldoOculto ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
            </button>
          </div>
          <div className="text-3xl font-black text-white">
            {saldoOculto ? (
              <span className="font-mono tracking-widest text-slate-500">S/ ••••••</span>
            ) : (
              <span className="text-emerald-400 font-mono">S/ {totalVentas.toFixed(2)}</span>
            )}
          </div>
          <div className="text-xs text-slate-400 mt-1 flex items-center gap-2">
            <span>{cobros.length} transacciones</span>
            <span>•</span>
            <span className="text-purple-300 font-mono">ID Tienda: {tiendaId}</span>
          </div>
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto">
          <button
            onClick={onAbrirConfiguradorQR}
            className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-purple-600/20 hover:bg-purple-600/30 text-purple-300 border border-purple-500/30 text-xs font-semibold transition"
          >
            <Upload className="w-3.5 h-3.5" />
            <span>{qrBaseYape ? 'Actualizar mi QR' : 'Adjuntar mi QR de Yape'}</span>
          </button>

          {cobros.length > 0 && (
            <button
              onClick={onLimpiarCaja}
              className="px-3.5 py-2.5 rounded-xl bg-slate-800 hover:bg-rose-500/20 text-slate-400 hover:text-rose-400 transition text-xs font-semibold border border-slate-700 flex items-center gap-1.5"
              title="Cerrar turno"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* POS Grid: Keypad & Live QR */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Virtual Keypad & Dynamic QR */}
        <div className="lg:col-span-6 bg-slate-900/80 border border-slate-800 rounded-3xl p-6 shadow-xl space-y-5">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-white text-base flex items-center gap-2">
              <CreditCard className="w-4 h-4 text-purple-400" />
              <span>Punto de Venta (POS)</span>
            </h3>
            <span className="text-[11px] bg-purple-500/10 text-purple-300 border border-purple-500/20 px-2.5 py-0.5 rounded-full font-semibold">
              {nombreNegocio}
            </span>
          </div>

          {/* Amount Display */}
          <div className="bg-slate-950 rounded-2xl p-4 border border-slate-800 text-center relative shadow-inner">
            <span className="text-[11px] text-slate-400 block mb-0.5">Monto a cobrar</span>
            <div className="text-3xl font-black text-purple-400 font-mono tracking-tight">
              S/ {montoActualNum.toFixed(2)}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 items-center">
            {/* Keypad */}
            <div className="grid grid-cols-3 gap-2">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9', 'C', '0', '.'].map((key) => (
                <button
                  key={key}
                  onClick={() => {
                    if (key === 'C') handleLimpiarTeclado();
                    else handleNumeroClick(key);
                  }}
                  className={`h-12 rounded-xl font-bold text-sm transition active:scale-95 flex items-center justify-center ${
                    key === 'C'
                      ? 'bg-rose-600/20 hover:bg-rose-600/30 text-rose-400 border border-rose-500/30'
                      : 'bg-slate-800 hover:bg-slate-700 text-white border border-slate-700/80 shadow-xs'
                  }`}
                >
                  {key}
                </button>
              ))}
            </div>

            {/* Generated QR Card */}
            <div className="bg-white rounded-2xl p-3 flex flex-col items-center justify-center shadow-xl border-2 border-purple-900/30 relative">
              <div className="bg-purple-900 text-white text-[9px] font-black uppercase tracking-wider py-0.5 px-3 rounded-full mb-1">
                Yape &amp; Plin
              </div>

              {qrDataUrl ? (
                <img src={qrDataUrl} alt="QR Yape" className="w-32 h-32 object-contain" />
              ) : (
                <div className="w-32 h-32 bg-slate-100 flex items-center justify-center text-[10px] text-slate-400">
                  Cargando QR...
                </div>
              )}

              <div className="text-center mt-1">
                <span className="text-[10px] font-black text-purple-950 block leading-tight">
                  {montoActualNum > 0 ? `PAGA S/ ${montoActualNum.toFixed(2)}` : 'ESCANEA Y PAGA'}
                </span>
                <span className="text-[9px] text-slate-600 font-mono block">
                  {tiendaId}
                </span>
              </div>
            </div>
          </div>

          {/* Quick Pay / Test Button */}
          {montoActualNum > 0 && (
            <div className="pt-2">
              <button
                onClick={() => onSimularPago(montoActualNum)}
                className="w-full flex items-center justify-center gap-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold py-2.5 px-4 rounded-xl text-xs transition shadow-lg shadow-emerald-600/20"
              >
                <Sparkles className="w-4 h-4" />
                <span>Simular Pago Exitoso del Cliente (S/ {montoActualNum.toFixed(2)})</span>
              </button>
            </div>
          )}
        </div>

        {/* Right: Real-Time Transactions List */}
        <div className="lg:col-span-6 bg-slate-900/80 border border-slate-800 rounded-3xl p-6 shadow-xl flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-bold text-white text-base flex items-center gap-2">
                <Receipt className="w-4 h-4 text-cyan-400" />
                <span>Cobros Registrados en Vivo</span>
              </h3>
              <span className="text-xs text-slate-400 font-mono">
                {cobros.length} recibidos
              </span>
            </div>

            <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
              {cobros.length > 0 ? (
                cobros.map((cobro) => (
                  <div
                    key={cobro.id}
                    className="p-3 rounded-2xl bg-slate-950/80 border border-slate-800/80 flex items-center justify-between hover:border-slate-700 transition"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-9 h-9 rounded-xl flex items-center justify-center font-bold text-xs ${
                          cobro.metodo === 'Yape'
                            ? 'bg-purple-600/20 text-purple-400 border border-purple-500/30'
                            : 'bg-cyan-600/20 text-cyan-400 border border-cyan-500/30'
                        }`}
                      >
                        {cobro.metodo === 'Yape' ? 'Y' : 'P'}
                      </div>
                      <div>
                        <div className="text-xs font-bold text-white leading-tight">
                          {cobro.nombre}
                        </div>
                        <div className="text-[10px] text-slate-400 mt-0.5">
                          {cobro.metodo} • {cobro.hora} hrs
                        </div>
                      </div>
                    </div>

                    <div className="text-right">
                      <div className="text-sm font-black text-emerald-400 font-mono">
                        + S/ {cobro.monto.toFixed(2)}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="py-12 text-center text-slate-500 text-xs">
                  Aún no hay cobros registrados en este turno.
                  <p className="mt-1 text-[11px] text-slate-600">
                    Marca un monto en el teclado y toca "Simular Pago" o usa la pestaña del Simulador.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
