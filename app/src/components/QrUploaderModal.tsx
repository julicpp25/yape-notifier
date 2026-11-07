import React, { useState, useRef } from 'react';
import jsQR from 'jsqr';
import {
  Upload,
  Image as ImageIcon,
  CheckCircle2,
  AlertCircle,
  QrCode,
  Sparkles,
  ArrowRight,
  Check,
  X,
  RefreshCw,
} from 'lucide-react';

interface Props {
  qrActual: string;
  tiendaIdActual: string;
  onQrConfigurado: (qrBase: string, tiendaId: string, nombreComercio?: string) => void;
  onClose: () => void;
}

export const QrUploaderModal: React.FC<Props> = ({
  qrActual,
  tiendaIdActual,
  onQrConfigurado,
  onClose,
}) => {
  const [imagenPreview, setImagenPreview] = useState<string | null>(null);
  const [decodificando, setDecodificando] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [qrDetectado, setQrDetectado] = useState<string | null>(null);
  const [idGenerado, setIdGenerado] = useState<string>('');
  const [nombreNegocio, setNombreNegocio] = useState<string>('Mi Negocio');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const procesarArchivoImagen = (file: File) => {
    setErrorMsg(null);
    setDecodificando(true);
    setQrDetectado(null);

    const reader = new FileReader();
    reader.onload = (e) => {
      const dataUrl = e.target?.result as string;
      setImagenPreview(dataUrl);

      const img = new Image();
      img.onload = () => {
        try {
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d');
          canvas.width = img.width;
          canvas.height = img.height;

          if (!ctx) {
            setErrorMsg('No se pudo inicializar el procesador de imágenes.');
            setDecodificando(false);
            return;
          }

          ctx.drawImage(img, 0, 0, img.width, img.height);
          const imageData = ctx.getImageData(0, 0, img.width, img.height);

          // 1. First pass with raw image
          let code = jsQR(imageData.data, imageData.width, imageData.height, {
            inversionAttempts: 'attemptBoth',
          });

          // 2. Second pass: High contrast grayscale if first pass failed
          if (!code) {
            const d = imageData.data;
            for (let i = 0; i < d.length; i += 4) {
              const avg = (d[i] + d[i + 1] + d[i + 2]) / 3;
              const threshold = avg > 128 ? 255 : 0;
              d[i] = threshold;
              d[i + 1] = threshold;
              d[i + 2] = threshold;
            }
            code = jsQR(d, imageData.width, imageData.height, {
              inversionAttempts: 'attemptBoth',
            });
          }

          if (code && code.data) {
            const raw = code.data;
            setQrDetectado(raw);

            // Generar ID único para la tienda basado en el QR o aleatorio
            const idHash = Math.abs(
              raw.split('').reduce((acc, char) => (acc << 5) - acc + char.charCodeAt(0), 0)
            )
              .toString(36)
              .toUpperCase()
              .substring(0, 4);

            const nuevoId = `TIENDA-YAPE-${idHash || Math.floor(1000 + Math.random() * 9000)}`;
            setIdGenerado(nuevoId);
          } else {
            setErrorMsg(
              'No se detectó un código QR válido en la imagen. Asegúrate de subir una captura clara de tu QR de Yape o Plin.'
            );
          }
        } catch (err) {
          console.error(err);
          setErrorMsg('Ocurrió un error al procesar la imagen.');
        } finally {
          setDecodificando(false);
        }
      };
      img.src = dataUrl;
    };
    reader.readAsDataURL(file);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      procesarArchivoImagen(file);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) {
      procesarArchivoImagen(file);
    }
  };

  const handleGuardarConfiguracion = () => {
    if (!qrDetectado || !idGenerado) return;
    onQrConfigurado(qrDetectado, idGenerado, nombreNegocio);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 overflow-y-auto animate-fadeIn">
      <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-400">
            <QrCode className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">Adjuntar mi Código QR de Yape</h3>
            <p className="text-xs text-slate-400">Genera tu ID de tienda y configura la caja POS</p>
          </div>
        </div>

        <p className="text-xs text-slate-300 leading-relaxed mb-4">
          Sube una captura de pantalla o foto de tu QR de Yape o Plin. La app lo escaneará automáticamente, registrará tu identificador y lo mostrará a los clientes al cobrar.
        </p>

        {/* Drop zone */}
        <div
          onDragOver={(e) => e.preventDefault()}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className="border-2 border-dashed border-purple-500/40 hover:border-purple-500 bg-purple-950/20 hover:bg-purple-950/30 rounded-2xl p-6 text-center cursor-pointer transition flex flex-col items-center justify-center min-h-[160px] relative overflow-hidden"
        >
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept="image/*"
            className="hidden"
          />

          {imagenPreview ? (
            <div className="flex flex-col items-center gap-2">
              <img
                src={imagenPreview}
                alt="Vista previa QR"
                className="w-28 h-28 object-contain rounded-xl border border-purple-500/30 bg-white p-1"
              />
              <span className="text-[11px] text-purple-300 font-medium">
                Toca aquí para cambiar de imagen
              </span>
            </div>
          ) : (
            <>
              <div className="w-12 h-12 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center mb-2">
                <Upload className="w-6 h-6 animate-pulse" />
              </div>
              <span className="text-xs font-bold text-white">
                Haz clic o arrastra la foto de tu QR de Yape aquí
              </span>
              <span className="text-[11px] text-slate-400 mt-0.5">
                PNG, JPG o captura de pantalla desde tu celular
              </span>
            </>
          )}

          {decodificando && (
            <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center gap-2 text-xs font-semibold text-purple-400">
              <RefreshCw className="w-4 h-4 animate-spin" />
              <span>Decodificando Código QR...</span>
            </div>
          )}
        </div>

        {/* Error message */}
        {errorMsg && (
          <div className="mt-4 p-3.5 rounded-xl bg-rose-950/60 border border-rose-500/30 text-rose-300 text-xs flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Detected QR & Auto-Generated ID */}
        {qrDetectado && (
          <div className="mt-4 space-y-3 p-4 rounded-2xl bg-emerald-950/30 border border-emerald-500/30 animate-fadeIn">
            <div className="flex items-center gap-2 text-emerald-400 font-bold text-xs">
              <CheckCircle2 className="w-4 h-4" />
              <span>¡Código QR decodificado exitosamente!</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div>
                <label className="text-[11px] text-slate-400 block mb-1">
                  ID de Tienda Auto-Generado:
                </label>
                <div className="font-mono font-bold text-white bg-slate-950 px-3 py-2 rounded-xl border border-slate-800 text-xs text-purple-300">
                  {idGenerado}
                </div>
              </div>

              <div>
                <label className="text-[11px] text-slate-400 block mb-1">
                  Nombre de tu Negocio / Caja:
                </label>
                <input
                  type="text"
                  value={nombreNegocio}
                  onChange={(e) => setNombreNegocio(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white text-xs outline-none focus:border-purple-500"
                />
              </div>
            </div>

            <div>
              <span className="text-[10px] text-slate-500 block">Contenido decodificado del QR:</span>
              <p className="font-mono text-[11px] text-slate-300 bg-slate-950/70 p-2 rounded-lg border border-slate-800/80 truncate">
                {qrDetectado}
              </p>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="mt-5 flex gap-3">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-300 py-2.5 rounded-xl text-xs font-semibold transition"
          >
            Cancelar
          </button>

          <button
            type="button"
            disabled={!qrDetectado}
            onClick={handleGuardarConfiguracion}
            className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs font-bold transition shadow-lg ${
              qrDetectado
                ? 'bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white shadow-purple-600/25'
                : 'bg-slate-800 text-slate-500 cursor-not-allowed'
            }`}
          >
            <Check className="w-4 h-4" />
            <span>Vincular y Usar en Caja</span>
          </button>
        </div>
      </div>
    </div>
  );
};
