import React, { useState } from 'react';
import { Student, SchoolSettings } from '../types';
import { addStudent, deleteStudent, updateStudent } from '../services/storage';
import { StudentCardModal } from './StudentCardModal';
import {
  Users,
  UserPlus,
  Search,
  Trash2,
  Edit2,
  QrCode,
  Download,
  Filter,
  Check,
  X,
  FileSpreadsheet,
} from 'lucide-react';

interface Props {
  students: Student[];
  settings: SchoolSettings;
  onStudentsChange: () => void;
}

export const StudentsManagement: React.FC<Props> = ({
  students,
  settings,
  onStudentsChange,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedLevel, setSelectedLevel] = useState<string>('all');
  const [selectedGrade, setSelectedGrade] = useState<string>('all');
  const [selectedSection, setSelectedSection] = useState<string>('all');

  const [selectedStudentForCard, setSelectedStudentForCard] = useState<Student | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingStudent, setEditingStudent] = useState<Student | null>(null);

  // Form State
  const [formData, setFormData] = useState({
    dni: '',
    names: '',
    lastNames: '',
    level: 'Secundaria' as 'Primaria' | 'Secundaria',
    grade: '1°',
    section: 'A',
    parentName: '',
    parentPhone: '',
  });

  const [formError, setFormError] = useState('');

  const filteredStudents = students.filter((s) => {
    const matchesSearch =
      s.dni.includes(searchTerm) ||
      s.names.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.lastNames.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesLevel = selectedLevel === 'all' || s.level === selectedLevel;
    const matchesGrade = selectedGrade === 'all' || s.grade === selectedGrade;
    const matchesSection = selectedSection === 'all' || s.section === selectedSection;
    return matchesSearch && matchesLevel && matchesGrade && matchesSection;
  });

  const handleOpenAdd = () => {
    setEditingStudent(null);
    setFormData({
      dni: '',
      names: '',
      lastNames: '',
      level: 'Secundaria',
      grade: '1°',
      section: 'A',
      parentName: '',
      parentPhone: '',
    });
    setFormError('');
    setShowAddModal(true);
  };

  const handleOpenEdit = (student: Student) => {
    setEditingStudent(student);
    setFormData({
      dni: student.dni,
      names: student.names,
      lastNames: student.lastNames,
      level: student.level,
      grade: student.grade,
      section: student.section,
      parentName: student.parentName,
      parentPhone: student.parentPhone,
    });
    setFormError('');
    setShowAddModal(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.dni.trim() || formData.dni.trim().length !== 8) {
      setFormError('El DNI debe tener exactamente 8 dígitos.');
      return;
    }
    if (!formData.names.trim() || !formData.lastNames.trim()) {
      setFormError('Nombres y apellidos son requeridos.');
      return;
    }

    if (editingStudent) {
      updateStudent(editingStudent.id, formData);
    } else {
      // Check if DNI already exists
      const exists = students.some((s) => s.dni === formData.dni.trim());
      if (exists) {
        setFormError('Ya existe un estudiante con este número de DNI.');
        return;
      }
      addStudent(formData);
    }

    setShowAddModal(false);
    onStudentsChange();
  };

  const handleDelete = (id: string, name: string) => {
    if (confirm(`¿Estás seguro de eliminar al estudiante "${name}"?`)) {
      deleteStudent(id);
      onStudentsChange();
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header and Controls */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-slate-900/60 border border-slate-800 p-6 rounded-3xl">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2.5">
            <Users className="w-5 h-5 text-blue-400" />
            <span>Padrón de Estudiantes Matriculados</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Total registrados: <strong>{students.length}</strong> alumnos • Con carnet QR asignado
          </p>
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto">
          <button
            onClick={handleOpenAdd}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-500 text-white font-semibold px-4 py-2.5 rounded-xl text-xs transition shadow-lg shadow-blue-500/20"
          >
            <UserPlus className="w-4 h-4" />
            <span>Matricular Alumno</span>
          </button>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 bg-slate-900/40 p-4 rounded-2xl border border-slate-800/80">
        {/* Search */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Buscar por DNI o apellidos..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700/80 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
          />
        </div>

        {/* Level filter */}
        <select
          value={selectedLevel}
          onChange={(e) => setSelectedLevel(e.target.value)}
          className="bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-xs text-slate-300 focus:outline-none focus:border-blue-500 transition"
        >
          <option value="all">Todos los Niveles</option>
          <option value="Primaria">Primaria</option>
          <option value="Secundaria">Secundaria</option>
        </select>

        {/* Grade filter */}
        <select
          value={selectedGrade}
          onChange={(e) => setSelectedGrade(e.target.value)}
          className="bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-xs text-slate-300 focus:outline-none focus:border-blue-500 transition"
        >
          <option value="all">Todos los Grados</option>
          <option value="1°">1° Grado</option>
          <option value="2°">2° Grado</option>
          <option value="3°">3° Grado</option>
          <option value="4°">4° Grado</option>
          <option value="5°">5° Grado</option>
          <option value="6°">6° Grado</option>
        </select>

        {/* Section filter */}
        <select
          value={selectedSection}
          onChange={(e) => setSelectedSection(e.target.value)}
          className="bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-xs text-slate-300 focus:outline-none focus:border-blue-500 transition"
        >
          <option value="all">Todas las Secciones</option>
          <option value="A">Sección A</option>
          <option value="B">Sección B</option>
          <option value="C">Sección C</option>
          <option value="D">Sección D</option>
        </select>
      </div>

      {/* Students Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-[10px]">
              <tr>
                <th className="py-3.5 px-4">DNI</th>
                <th className="py-3.5 px-4">Estudiante</th>
                <th className="py-3.5 px-4">Nivel y Grado</th>
                <th className="py-3.5 px-4">Apoderado / Celular</th>
                <th className="py-3.5 px-4 text-center">Carnet QR</th>
                <th className="py-3.5 px-4 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {filteredStudents.length > 0 ? (
                filteredStudents.map((student) => (
                  <tr key={student.id} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 font-mono font-bold text-white">
                      {student.dni}
                    </td>
                    <td className="py-3 px-4 font-medium text-slate-100">
                      <div>
                        {student.lastNames}, {student.names}
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 font-semibold text-[11px] border border-slate-700">
                        {student.grade} "{student.section}" • {student.level}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <div className="text-slate-300">{student.parentName}</div>
                      <div className="text-emerald-400 font-mono text-[11px]">
                        +51 {student.parentPhone}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-center">
                      <button
                        onClick={() => setSelectedStudentForCard(student)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-600/10 hover:bg-blue-600/20 text-blue-400 border border-blue-500/20 transition font-medium text-xs"
                        title="Ver e Imprimir Carnet Escolar"
                      >
                        <QrCode className="w-3.5 h-3.5" />
                        <span>Ver Carnet</span>
                      </button>
                    </td>
                    <td className="py-3 px-4 text-right space-x-2">
                      <button
                        onClick={() => handleOpenEdit(student)}
                        className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition"
                        title="Editar datos"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleDelete(student.id, `${student.names} ${student.lastNames}`)}
                        className="p-1.5 rounded-lg hover:bg-rose-500/10 text-slate-400 hover:text-rose-400 transition"
                        title="Eliminar estudiante"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="py-10 text-center text-slate-500">
                    No se encontraron estudiantes con los filtros seleccionados.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Card Modal */}
      {selectedStudentForCard && (
        <StudentCardModal
          student={selectedStudentForCard}
          settings={settings}
          onClose={() => setSelectedStudentForCard(null)}
        />
      )}

      {/* Add / Edit Student Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl relative">
            <button
              onClick={() => setShowAddModal(false)}
              className="absolute top-4 right-4 p-2 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-lg font-bold text-white mb-1">
              {editingStudent ? 'Editar Datos de Estudiante' : 'Matricular Nuevo Estudiante'}
            </h3>
            <p className="text-xs text-slate-400 mb-5">
              Los datos generarán automáticamente su código QR para el control de asistencia.
            </p>

            {formError && (
              <div className="p-3 mb-4 rounded-xl bg-rose-950/60 border border-rose-500/40 text-rose-300 text-xs">
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-3.5 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-300 font-medium block mb-1">DNI (8 dígitos):</label>
                  <input
                    type="text"
                    maxLength={8}
                    required
                    placeholder="72345678"
                    value={formData.dni}
                    onChange={(e) => setFormData({ ...formData, dni: e.target.value.replace(/\D/g, '') })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white font-mono focus:border-blue-500 outline-none"
                  />
                </div>

                <div>
                  <label className="text-slate-300 font-medium block mb-1">Nivel Educativo:</label>
                  <select
                    value={formData.level}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        level: e.target.value as 'Primaria' | 'Secundaria',
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white focus:border-blue-500 outline-none"
                  >
                    <option value="Primaria">Primaria</option>
                    <option value="Secundaria">Secundaria</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-300 font-medium block mb-1">Nombres:</label>
                  <input
                    type="text"
                    required
                    placeholder="Mateo Alejandro"
                    value={formData.names}
                    onChange={(e) => setFormData({ ...formData, names: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white focus:border-blue-500 outline-none"
                  />
                </div>

                <div>
                  <label className="text-slate-300 font-medium block mb-1">Apellidos:</label>
                  <input
                    type="text"
                    required
                    placeholder="Quispe Huamán"
                    value={formData.lastNames}
                    onChange={(e) => setFormData({ ...formData, lastNames: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white focus:border-blue-500 outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-300 font-medium block mb-1">Grado:</label>
                  <select
                    value={formData.grade}
                    onChange={(e) => setFormData({ ...formData, grade: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white focus:border-blue-500 outline-none"
                  >
                    <option value="1°">1° de {formData.level}</option>
                    <option value="2°">2° de {formData.level}</option>
                    <option value="3°">3° de {formData.level}</option>
                    <option value="4°">4° de {formData.level}</option>
                    <option value="5°">5° de {formData.level}</option>
                    {formData.level === 'Primaria' && <option value="6°">6° de Primaria</option>}
                  </select>
                </div>

                <div>
                  <label className="text-slate-300 font-medium block mb-1">Sección:</label>
                  <select
                    value={formData.section}
                    onChange={(e) => setFormData({ ...formData, section: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white focus:border-blue-500 outline-none"
                  >
                    <option value="A">Sección "A"</option>
                    <option value="B">Sección "B"</option>
                    <option value="C">Sección "C"</option>
                    <option value="D">Sección "D"</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-300 font-medium block mb-1">Nombre del Apoderado:</label>
                  <input
                    type="text"
                    placeholder="Rosa Huamán Flores"
                    value={formData.parentName}
                    onChange={(e) => setFormData({ ...formData, parentName: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white focus:border-blue-500 outline-none"
                  />
                </div>

                <div>
                  <label className="text-slate-300 font-medium block mb-1">WhatsApp / Celular:</label>
                  <input
                    type="text"
                    maxLength={9}
                    placeholder="987654321"
                    value={formData.parentPhone}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        parentPhone: e.target.value.replace(/\D/g, ''),
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white font-mono focus:border-blue-500 outline-none"
                  />
                </div>
              </div>

              <div className="pt-4 flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-300 py-2.5 rounded-xl transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 bg-blue-600 hover:bg-blue-500 text-white font-semibold py-2.5 rounded-xl transition shadow-lg shadow-blue-500/20"
                >
                  {editingStudent ? 'Guardar Cambios' : 'Registrar y Generar QR'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
