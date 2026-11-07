import React, { useState } from 'react';
import { SchoolSettings } from '../types';
import { saveSchoolSettings } from '../services/storage';
import { Settings, Save, Check, Volume2, MessageSquare, School, Clock } from 'lucide-react';

interface Props {
  settings: SchoolSettings;
  onSettingsSaved: (newSettings: SchoolSettings) => void;
}

export const SchoolSettingsView: React.FC<Props> = ({ settings, onSettingsSaved }) => {
  const [formData, setFormData] = useState<SchoolSettings>(settings);
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    saveSchoolSettings(formData);
    onSettingsSaved(formData);
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 2500);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6 animate-fadeIn">
      <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
            <School className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">Configuración de la Institución Educativa</h2>
            <p className="text-xs text-slate-400">Datos para los carnets escolares y reglas de asistencia</p>
          </div>
        </div>

        {savedSuccess && (
          <div className="p-4 mb-6 rounded-2xl bg-emerald-950/60 border border-emerald-500/40 text-emerald-300 text-xs flex items-center gap-2">
            <Check className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>Configuración guardada exitosamente.</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5 text-xs">
          {/* School Name & Modular Code */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-slate-300 font-medium block mb-1.5">
                Nombre de la Institución Educativa:
              </label>
              <input
                type="text"
                required
                value={formData.schoolName}
                onChange={(e) => setFormData({ ...formData, schoolName: e.target.value })}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-white focus:border-blue-500 outline-none transition"
              />
            </div>

            <div>
              <label className="text-slate-300 font-medium block mb-1.5">
                Código Modular MINEDU:
              </label>
              <input
                type="text"
                value={formData.modularCode}
                onChange={(e) => setFormData({ ...formData, modularCode: e.target.value })}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-white font-mono focus:border-blue-500 outline-none transition"
              />
            </div>
          </div>

          <div>
            <label className="text-slate-300 font-medium block mb-1.5">
              UGEL / Dirección Regional de Educación:
            </label>
            <input
              type="text"
              value={formData.ugel}
              onChange={(e) => setFormData({ ...formData, ugel: e.target.value })}
              className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-white focus:border-blue-500 outline-none transition"
            />
          </div>

          {/* Times */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2 border-t border-slate-800">
            <div>
              <label className="text-slate-300 font-medium block mb-1.5 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-emerald-400" />
                <span>Hora Límite de Entrada Puntual (HH:MM):</span>
              </label>
              <input
                type="time"
                value={formData.entryTime}
                onChange={(e) => setFormData({ ...formData, entryTime: e.target.value })}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-white font-mono focus:border-blue-500 outline-none transition"
              />
              <p className="text-[11px] text-slate-400 mt-1">
                Los escaneos hasta esta hora se marcan como Puntuales.
              </p>
            </div>

            <div>
              <label className="text-slate-300 font-medium block mb-1.5 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-amber-400" />
                <span>Tardanza Considerada hasta (HH:MM):</span>
              </label>
              <input
                type="time"
                value={formData.lateThreshold}
                onChange={(e) => setFormData({ ...formData, lateThreshold: e.target.value })}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-white font-mono focus:border-blue-500 outline-none transition"
              />
            </div>
          </div>

          {/* Sound & Toggle */}
          <div className="pt-2 border-t border-slate-800 space-y-3">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={formData.enableSound}
                onChange={(e) => setFormData({ ...formData, enableSound: e.target.checked })}
                className="w-4 h-4 rounded text-blue-600 bg-slate-950 border-slate-700 focus:ring-0"
              />
              <span className="text-slate-200 font-medium flex items-center gap-1.5">
                <Volume2 className="w-4 h-4 text-blue-400" />
                <span>Emitir sonido de confirmación 'Beep' al escanear</span>
              </span>
            </label>
          </div>

          {/* WhatsApp message template */}
          <div className="pt-2 border-t border-slate-800">
            <label className="text-slate-300 font-medium block mb-1.5 flex items-center gap-1.5">
              <MessageSquare className="w-3.5 h-3.5 text-emerald-400" />
              <span>Plantilla de Mensaje WhatsApp para Apoderados:</span>
            </label>
            <textarea
              rows={3}
              value={formData.whatsappMessageTemplate}
              onChange={(e) => setFormData({ ...formData, whatsappMessageTemplate: e.target.value })}
              className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-white focus:border-blue-500 outline-none transition"
            />
            <p className="text-[11px] text-slate-400 mt-1">
              Variables disponibles: <code className="text-blue-300">{`{APODERADO}`}</code>,{' '}
              <code className="text-blue-300">{`{ESTUDIANTE}`}</code>,{' '}
              <code className="text-blue-300">{`{FECHA}`}</code>,{' '}
              <code className="text-blue-300">{`{HORA}`}</code>,{' '}
              <code className="text-blue-300">{`{ESTADO}`}</code>,{' '}
              <code className="text-blue-300">{`{COLEGIO}`}</code>.
            </p>
          </div>

          <div className="pt-4">
            <button
              type="submit"
              className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-500 text-white font-semibold py-3 px-6 rounded-xl transition shadow-lg shadow-blue-500/20 text-xs"
            >
              <Save className="w-4 h-4" />
              <span>Guardar Configuración Institucional</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
