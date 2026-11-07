import React, { useState } from 'react';
import { AttendanceRecord, Student, SchoolSettings } from '../types';
import {
  exportAttendancesToCSV,
  saveAttendances,
  createWhatsAppUrl,
  findStudentByDni,
} from '../services/storage';
import {
  ClipboardList,
  Calendar,
  Download,
  Trash2,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Send,
  PieChart,
} from 'lucide-react';

interface Props {
  attendances: AttendanceRecord[];
  students: Student[];
  settings: SchoolSettings;
  onAttendancesChange: () => void;
}

export const AttendanceReports: React.FC<Props> = ({
  attendances,
  students,
  settings,
  onAttendancesChange,
}) => {
  const todayStr = new Date().toISOString().split('T')[0];
  const [selectedDate, setSelectedDate] = useState<string>(todayStr);
  const [filterStatus, setFilterStatus] = useState<string>('all');

  const filtered = attendances.filter((a) => {
    const matchesDate = !selectedDate || a.date === selectedDate;
    const matchesStatus = filterStatus === 'all' || a.status === filterStatus;
    return matchesDate && matchesStatus;
  });

  // Calculate statistics for the selected date
  const dateRecords = attendances.filter((a) => a.date === (selectedDate || todayStr));
  const puntualCount = dateRecords.filter((a) => a.status === 'Puntual').length;
  const tardanzaCount = dateRecords.filter((a) => a.status === 'Tardanza').length;
  const totalAssisted = puntualCount + tardanzaCount;
  const totalStudents = students.length;
  const absentCount = Math.max(0, totalStudents - totalAssisted);
  const puntualPercentage = totalAssisted > 0 ? Math.round((puntualCount / totalAssisted) * 100) : 0;

  const handleExportCSV = () => {
    const csv = exportAttendancesToCSV(filtered);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Asistencia_Escolar_${selectedDate || 'historico'}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleClearToday = () => {
    if (confirm('¿Deseas reiniciar los registros de asistencia de la fecha seleccionada?')) {
      const remaining = attendances.filter((a) => a.date !== selectedDate);
      saveAttendances(remaining);
      onAttendancesChange();
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Assisted */}
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
            <span>Asistencias Hoy</span>
            <CheckCircle2 className="w-4 h-4 text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white">
            {totalAssisted} <span className="text-xs font-normal text-slate-500">/ {totalStudents} alumnos</span>
          </div>
          <div className="text-[11px] text-slate-400 mt-1">
            {totalStudents > 0 ? Math.round((totalAssisted / totalStudents) * 100) : 0}% de asistencia escolar
          </div>
        </div>

        {/* Puntuales */}
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-emerald-500/20 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
            <span>Puntuales</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">{puntualCount}</div>
          <div className="text-[11px] text-slate-400 mt-1">
            {puntualPercentage}% ingresaron a tiempo
          </div>
        </div>

        {/* Tardanzas */}
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-amber-500/20 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
            <span>Tardanzas</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-black text-amber-400">{tardanzaCount}</div>
          <div className="text-[11px] text-slate-400 mt-1">
            Ingreso posterior a {settings.entryTime}
          </div>
        </div>

        {/* Inasistencias Estimadas */}
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-rose-500/20 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-2">
            <span>Sin Registro / Faltas</span>
            <AlertTriangle className="w-4 h-4 text-rose-400" />
          </div>
          <div className="text-2xl font-black text-rose-400">{absentCount}</div>
          <div className="text-[11px] text-slate-400 mt-1">
            Alumnos pendientes de escaneo
          </div>
        </div>
      </div>

      {/* Filter and Action Bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-2xl border border-slate-800">
        <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
          {/* Date Picker */}
          <div className="flex items-center gap-2 bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-1.5 text-xs text-white">
            <Calendar className="w-3.5 h-3.5 text-slate-400" />
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="bg-transparent text-white outline-none"
            />
          </div>

          {/* Status filter */}
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-xs text-slate-300 outline-none"
          >
            <option value="all">Todos los Estados</option>
            <option value="Puntual">Solo Puntuales</option>
            <option value="Tardanza">Solo Tardanzas</option>
          </select>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <button
            onClick={handleExportCSV}
            className="flex-1 sm:flex-initial flex items-center justify-center gap-2 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold px-4 py-2 rounded-xl text-xs transition shadow-md shadow-emerald-600/20"
          >
            <Download className="w-4 h-4" />
            <span>Exportar para SIAGIE / Excel</span>
          </button>

          {filtered.length > 0 && (
            <button
              onClick={handleClearToday}
              className="p-2 rounded-xl bg-slate-800 hover:bg-rose-500/20 text-slate-400 hover:text-rose-400 transition border border-slate-700"
              title="Borrar registros de esta fecha"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Attendance Log Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-[10px]">
              <tr>
                <th className="py-3.5 px-4">Hora de Ingreso</th>
                <th className="py-3.5 px-4">Estudiante</th>
                <th className="py-3.5 px-4">DNI</th>
                <th className="py-3.5 px-4">Grado y Sección</th>
                <th className="py-3.5 px-4">Estado</th>
                <th className="py-3.5 px-4 text-right">Aviso WhatsApp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {filtered.length > 0 ? (
                filtered.map((record) => {
                  const student = findStudentByDni(record.studentDni);
                  return (
                    <tr key={record.id} className="hover:bg-slate-800/40 transition">
                      <td className="py-3 px-4 font-mono font-bold text-white">
                        {record.time.substring(0, 5)}
                        <span className="text-[10px] text-slate-500 ml-1">
                          {record.time.substring(5)}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-100">
                        {record.studentName}
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-400">
                        {record.studentDni}
                      </td>
                      <td className="py-3 px-4">
                        <span className="text-slate-300">{record.gradeSection}</span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${
                            record.status === 'Puntual'
                              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                              : 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                          }`}
                        >
                          <span
                            className={`w-1.5 h-1.5 rounded-full ${
                              record.status === 'Puntual' ? 'bg-emerald-400' : 'bg-amber-400'
                            }`}
                          />
                          {record.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        {student && student.parentPhone ? (
                          <a
                            href={createWhatsAppUrl(student, record)}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 text-xs font-medium transition"
                          >
                            <Send className="w-3 h-3" />
                            <span>Avisar</span>
                          </a>
                        ) : (
                          <span className="text-[11px] text-slate-500">Sin celular</span>
                        )}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    No hay registros de asistencia para esta fecha ({selectedDate || todayStr}).
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
