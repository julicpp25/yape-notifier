import React, { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { Student, SchoolSettings } from '../types';
import { X, Printer, Download, GraduationCap, School } from 'lucide-react';

interface Props {
  student: Student;
  settings: SchoolSettings;
  onClose: () => void;
}

export const StudentCardModal: React.FC<Props> = ({ student, settings, onClose }) => {
  const [qrDataUrl, setQrDataUrl] = useState<string>('');

  useEffect(() => {
    // Generate QR with DNI as primary scanner payload
    const payload = student.dni;
    QRCode.toDataURL(payload, {
      width: 260,
      margin: 1,
      color: {
        dark: '#0f172a',
        light: '#ffffff',
      },
    })
      .then((url) => setQrDataUrl(url))
      .catch((err) => console.error(err));
  }, [student]);

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 overflow-y-auto">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
        >
          <X className="w-5 h-5" />
        </button>

        <h3 className="text-lg font-bold text-white mb-1 flex items-center gap-2">
          <GraduationCap className="w-5 h-5 text-blue-400" />
          <span>Carnet Escolar con Código QR</span>
        </h3>
        <p className="text-xs text-slate-400 mb-6">
          Listo para imprimir, recortar y enmicar para el control de asistencia.
        </p>

        {/* Printable Card Area */}
        <div id="printable-card" className="bg-white text-slate-900 rounded-2xl p-5 shadow-xl border-2 border-blue-900/40 relative overflow-hidden">
          {/* Card Header */}
          <div className="bg-gradient-to-r from-blue-900 via-indigo-900 to-blue-950 -mx-5 -mt-5 p-4 text-white text-center relative">
            <div className="text-[10px] uppercase font-bold tracking-widest text-emerald-400 mb-0.5">
              REPÚBLICA DEL PERÚ • MINEDU
            </div>
            <div className="text-xs font-semibold text-blue-200">
              {settings.ugel}
            </div>
            <div className="text-base font-extrabold tracking-tight mt-1">
              {settings.schoolName}
            </div>
            <div className="text-[10px] text-blue-300">
              Cód. Modular: {settings.modularCode}
            </div>
          </div>

          {/* Student Info & QR */}
          <div className="mt-4 flex flex-col items-center text-center">
            <div className="w-16 h-16 rounded-full bg-slate-100 border-2 border-blue-600 flex items-center justify-center text-blue-800 font-bold text-lg mb-2 shadow-inner">
              {student.names.charAt(0)}{student.lastNames.charAt(0)}
            </div>

            <div className="text-base font-black text-slate-900 leading-snug">
              {student.lastNames}
            </div>
            <div className="text-sm font-semibold text-slate-700">
              {student.names}
            </div>

            <div className="mt-2 inline-flex items-center gap-2 bg-blue-50 border border-blue-200 px-3 py-1 rounded-full text-xs font-bold text-blue-900">
              <span>{student.grade} "{student.section}"</span>
              <span>•</span>
              <span>{student.level}</span>
            </div>

            <div className="mt-3 p-2 bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col items-center">
              {qrDataUrl ? (
                <img src={qrDataUrl} alt="QR Escolar" className="w-40 h-40" />
              ) : (
                <div className="w-40 h-40 bg-slate-100 flex items-center justify-center text-xs text-slate-400">
                  Generando QR...
                </div>
              )}
              <div className="text-xs font-mono font-bold tracking-widest text-slate-800 mt-1">
                DNI: {student.dni}
              </div>
            </div>

            <div className="mt-3 text-[10px] text-slate-500 flex items-center justify-between w-full border-t border-slate-200 pt-2 px-1">
              <span>Apoderado: {student.parentName}</span>
              <span className="font-semibold text-blue-700">Año 2026</span>
            </div>
          </div>
        </div>

        {/* Action buttons */}
        <div className="mt-6 flex gap-3">
          <button
            onClick={handlePrint}
            className="flex-1 flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-500 text-white font-semibold py-2.5 px-4 rounded-xl text-xs transition shadow-lg shadow-blue-500/25"
          >
            <Printer className="w-4 h-4" />
            <span>Imprimir Carnet</span>
          </button>

          {qrDataUrl && (
            <a
              href={qrDataUrl}
              download={`QR_${student.dni}_${student.lastNames}.png`}
              className="flex items-center justify-center gap-2 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold py-2.5 px-4 rounded-xl text-xs transition border border-slate-700"
            >
              <Download className="w-4 h-4" />
              <span>Guardar QR</span>
            </a>
          )}
        </div>
      </div>
    </div>
  );
};
