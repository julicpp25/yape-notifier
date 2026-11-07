import React, { useEffect, useRef, useState } from 'react';
import { Html5QrcodeScanner, Html5Qrcode } from 'html5-qrcode';
import { Student, AttendanceRecord, SchoolSettings } from '../types';
import {
  findStudentByDni,
  recordAttendance,
  createWhatsAppUrl,
} from '../services/storage';
import {
  playSuccessBeep,
  playWarningBeep,
  playAlreadyScannedBeep,
} from '../utils/sound';
import {
  Camera,
  CheckCircle,
  AlertTriangle,
  Clock,
  Send,
  Search,
  UserCheck,
  RefreshCw,
  Sparkles,
} from 'lucide-react';

interface Props {
  settings: SchoolSettings;
  onAttendanceRecorded: (record: AttendanceRecord) => void;
}

export const QRScannerView: React.FC<Props> = ({ settings, onAttendanceRecorded }) => {
  const [lastScannedStudent, setLastScannedStudent] = useState<Student | null>(null);
  const [lastRecord, setLastRecord] = useState<AttendanceRecord | null>(null);
  const [scanMessage, setScanMessage] = useState<{
    type: 'success' | 'warning' | 'error' | 'info';
    text: string;
  } | null>(null);

  const [manualDni, setManualDni] = useState('');
  const [isScanning, setIsScanning] = useState(false);
  const [scannerError, setScannerError] = useState<string | null>(null);

  const scannerRef = useRef<Html5QrcodeScanner | null>(null);
  const lastProcessedCodeRef = useRef<string>('');
  const lastProcessedTimeRef = useRef<number>(0);

  const handleScanSuccess = (decodedText: string) => {
    const now = Date.now();
    // Debounce duplicate scans within 3 seconds
    if (
      decodedText === lastProcessedCodeRef.current &&
      now - lastProcessedTimeRef.current < 3000
    ) {
      return;
    }

    lastProcessedCodeRef.current = decodedText;
    lastProcessedTimeRef.current = now;

    // Clean DNI digits
    const cleaned = decodedText.trim().replace(/\D/g, '');
    const student = findStudentByDni(cleaned);

    if (!student) {
      if (settings.enableSound) playWarningBeep();
      setScanMessage({
        type: 'error',
        text: `Código QR "${decodedText}" no corresponde a ningún alumno registrado en el sistema.`,
      });
      return;
    }

    const { record, isDuplicate } = recordAttendance(student, 'QR');
    setLastScannedStudent(student);
    setLastRecord(record);
    onAttendanceRecorded(record);

    if (isDuplicate) {
      if (settings.enableSound) playAlreadyScannedBeep();
      setScanMessage({
        type: 'info',
        text: `El estudiante ${student.names} ya registró su asistencia hoy (${record.time.substring(0, 5)}).`,
      });
    } else {
      if (record.status === 'Puntual') {
        if (settings.enableSound) playSuccessBeep();
        setScanMessage({
          type: 'success',
          text: `¡Asistencia PUNTUAL registrada con éxito!`,
        });
      } else {
        if (settings.enableSound) playWarningBeep();
        setScanMessage({
          type: 'warning',
          text: `Registro de TARDANZA ingresado.`,
        });
      }
    }
  };

  const startScanner = () => {
    try {
      setScannerError(null);
      if (scannerRef.current) {
        scannerRef.current.clear().catch(() => {});
      }

      const scanner = new Html5QrcodeScanner(
        'qr-reader-container',
        {
          fps: 15,
          qrbox: { width: 250, height: 250 },
          aspectRatio: 1.0,
          showTorchButtonIfSupported: true,
        },
        false
      );

      scanner.render(
        (text) => handleScanSuccess(text),
        (err) => {
          // ignore common scan frame misses
        }
      );

      scannerRef.current = scanner;
      setIsScanning(true);
    } catch (err: unknown) {
      console.error(err);
      setScannerError(
        'No se pudo acceder a la cámara. Asegúrate de conceder permisos de cámara en tu navegador o dispositivo móvil.'
      );
      setIsScanning(false);
    }
  };

  const stopScanner = () => {
    if (scannerRef.current) {
      scannerRef.current
        .clear()
        .then(() => {
          setIsScanning(false);
        })
        .catch((e) => {
          console.error(e);
          setIsScanning(false);
        });
    }
  };

  useEffect(() => {
    startScanner();
    return () => {
      if (scannerRef.current) {
        scannerRef.current.clear().catch(() => {});
      }
    };
  }, []);

  const handleManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!manualDni.trim()) return;
    handleScanSuccess(manualDni.trim());
    setManualDni('');
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start animate-fadeIn">
      {/* Left Column: Camera Scanner */}
      <div className="lg:col-span-7 bg-slate-900/90 border border-slate-800 rounded-3xl p-6 shadow-2xl relative">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
              <Camera className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-white text-base">Escáner QR en Tiempo Real</h3>
              <p className="text-xs text-slate-400">Apunta la cámara del móvil al carnet escolar</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                stopScanner();
                setTimeout(startScanner, 200);
              }}
              className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition text-xs flex items-center gap-1.5 border border-slate-700"
              title="Reiniciar cámara"
            >
              <RefreshCw className="w-4 h-4" />
              <span className="hidden sm:inline">Reiniciar</span>
            </button>
          </div>
        </div>

        {scannerError ? (
          <div className="p-6 rounded-2xl bg-rose-950/40 border border-rose-500/30 text-rose-300 text-center my-6">
            <AlertTriangle className="w-8 h-8 text-rose-400 mx-auto mb-2" />
            <p className="text-sm font-semibold">{scannerError}</p>
            <button
              onClick={startScanner}
              className="mt-4 px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs transition"
            >
              Reintentar acceso a cámara
            </button>
          </div>
        ) : (
          <div className="relative overflow-hidden rounded-2xl bg-slate-950 border border-slate-800/80 p-2 min-h-[320px] flex flex-col justify-center">
            <div id="qr-reader-container" className="w-full text-slate-900 rounded-xl overflow-hidden" />
          </div>
        )}

        {/* Manual DNI fallback form */}
        <div className="mt-5 pt-4 border-t border-slate-800/80">
          <form onSubmit={handleManualSubmit} className="flex gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                maxLength={8}
                placeholder="Ingresar DNI manualmente si el código está dañado..."
                value={manualDni}
                onChange={(e) => setManualDni(e.target.value.replace(/\D/g, ''))}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-10 pr-3.5 py-2.5 text-xs text-white placeholder-slate-500 font-mono focus:outline-none focus:border-blue-500 transition"
              />
            </div>
            <button
              type="submit"
              className="bg-blue-600 hover:bg-blue-500 text-white font-semibold px-4 py-2.5 rounded-xl text-xs transition shrink-0 flex items-center gap-1.5 shadow-md shadow-blue-500/20"
            >
              <UserCheck className="w-4 h-4" />
              <span>Registrar</span>
            </button>
          </form>
        </div>
      </div>

      {/* Right Column: Scan Result Card & Real-Time Student Preview */}
      <div className="lg:col-span-5 space-y-4">
        {/* Status Toast / Alert */}
        {scanMessage && (
          <div
            className={`p-4 rounded-2xl border text-xs flex items-start gap-3 shadow-lg animate-fadeIn ${
              scanMessage.type === 'success'
                ? 'bg-emerald-950/60 border-emerald-500/40 text-emerald-200'
                : scanMessage.type === 'warning'
                ? 'bg-amber-950/60 border-amber-500/40 text-amber-200'
                : scanMessage.type === 'info'
                ? 'bg-blue-950/60 border-blue-500/40 text-blue-200'
                : 'bg-rose-950/60 border-rose-500/40 text-rose-200'
            }`}
          >
            {scanMessage.type === 'success' ? (
              <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
            ) : (
              <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
            )}
            <span className="font-medium leading-relaxed">{scanMessage.text}</span>
          </div>
        )}

        {/* Student Result Card */}
        {lastScannedStudent && lastRecord ? (
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl relative overflow-hidden animate-fadeIn">
            {/* Background accent */}
            <div
              className={`absolute top-0 left-0 right-0 h-2 ${
                lastRecord.status === 'Puntual' ? 'bg-emerald-500' : 'bg-amber-500'
              }`}
            />

            <div className="flex items-center justify-between mt-2 mb-4">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Último Registro
              </span>
              <span
                className={`text-xs px-3 py-1 rounded-full font-bold flex items-center gap-1 ${
                  lastRecord.status === 'Puntual'
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                    : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                }`}
              >
                <Clock className="w-3.5 h-3.5" />
                <span>{lastRecord.status} • {lastRecord.time.substring(0, 5)} hrs</span>
              </span>
            </div>

            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white flex items-center justify-center font-bold text-xl shadow-lg shrink-0">
                {lastScannedStudent.names.charAt(0)}
                {lastScannedStudent.lastNames.charAt(0)}
              </div>
              <div>
                <h4 className="text-lg font-bold text-white leading-tight">
                  {lastScannedStudent.names} {lastScannedStudent.lastNames}
                </h4>
                <div className="text-xs text-blue-400 font-mono mt-0.5">
                  DNI: {lastScannedStudent.dni}
                </div>
                <div className="text-xs text-slate-400 mt-1">
                  {lastScannedStudent.grade} "{lastScannedStudent.section}" de {lastScannedStudent.level}
                </div>
              </div>
            </div>

            {/* Parent info & WhatsApp alert */}
            <div className="mt-5 p-4 rounded-2xl bg-slate-950/60 border border-slate-800 space-y-2 text-xs">
              <div className="flex justify-between text-slate-300">
                <span className="text-slate-500">Apoderado:</span>
                <span className="font-semibold">{lastScannedStudent.parentName}</span>
              </div>
              <div className="flex justify-between text-slate-300">
                <span className="text-slate-500">Teléfono:</span>
                <span className="font-mono text-emerald-400">{lastScannedStudent.parentPhone}</span>
              </div>
              <div className="flex justify-between text-slate-300">
                <span className="text-slate-500">Método de ingreso:</span>
                <span className="text-slate-300">{lastRecord.method}</span>
              </div>

              {lastScannedStudent.parentPhone && (
                <div className="pt-2">
                  <a
                    href={createWhatsAppUrl(lastScannedStudent, lastRecord)}
                    target="_blank"
                    rel="noreferrer"
                    className="w-full flex items-center justify-center gap-2 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold py-2.5 px-4 rounded-xl text-xs transition shadow-lg shadow-emerald-600/20"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>Notificar Asistencia por WhatsApp</span>
                  </a>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="bg-slate-900/60 border border-slate-800 rounded-3xl p-8 text-center text-slate-400">
            <div className="w-14 h-14 rounded-2xl bg-slate-800/80 border border-slate-700 mx-auto flex items-center justify-center text-slate-400 mb-3">
              <Sparkles className="w-6 h-6 text-blue-400" />
            </div>
            <h4 className="text-sm font-semibold text-white mb-1">Esperando lectura de carnet</h4>
            <p className="text-xs text-slate-400 max-w-xs mx-auto">
              Coloca el código QR del carnet escolar frente a la cámara. El sistema emitirá un sonido de confirmación y registrará la hora exacta.
            </p>
          </div>
        )}

        {/* School Entry Rules Card */}
        <div className="bg-slate-900/40 border border-slate-800 rounded-2xl p-4 text-xs text-slate-400 space-y-1.5">
          <div className="flex items-center justify-between text-slate-300 font-semibold mb-1">
            <span>Reglas de Horario ({settings.schoolName})</span>
            <span className="text-blue-400">{settings.entryTime} am</span>
          </div>
          <div className="flex justify-between">
            <span>Entrada puntual:</span>
            <span className="text-emerald-400 font-medium">Hasta las {settings.entryTime}</span>
          </div>
          <div className="flex justify-between">
            <span>Considerado tardanza:</span>
            <span className="text-amber-400 font-medium">A partir de las {settings.entryTime}</span>
          </div>
        </div>
      </div>
    </div>
  );
};
