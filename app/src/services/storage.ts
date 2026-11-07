import { Student, AttendanceRecord, SchoolSettings } from '../types';

const STUDENTS_KEY = 'se_students_v1';
const ATTENDANCE_KEY = 'se_attendance_v1';
const SETTINGS_KEY = 'se_settings_v1';

const DEFAULT_SETTINGS: SchoolSettings = {
  schoolName: 'I.E. 2084 Trompeteros',
  modularCode: '0485921',
  ugel: 'UGEL 04 - Comas (Lima Norte)',
  entryTime: '07:45',
  lateThreshold: '08:00',
  enableSound: true,
  autoNotifyWhatsapp: true,
  whatsappMessageTemplate: 'Estimado(a) {APODERADO}: Le informamos que su hijo(a) {ESTUDIANTE} ha registrado su ingreso al colegio hoy {FECHA} a las {HORA} hrs. Estado: {ESTADO}. I.E. {COLEGIO}.',
};

const SEED_STUDENTS: Student[] = [
  {
    id: 'std-1',
    dni: '72345678',
    names: 'Mateo Alejandro',
    lastNames: 'Quispe Huamán',
    level: 'Secundaria',
    grade: '3°',
    section: 'A',
    parentName: 'Rosa Huamán Flores',
    parentPhone: '987654321',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'std-2',
    dni: '73456789',
    names: 'Valeria Nicole',
    lastNames: 'Flores Condori',
    level: 'Secundaria',
    grade: '3°',
    section: 'A',
    parentName: 'Carlos Flores Mendoza',
    parentPhone: '912345678',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'std-3',
    dni: '74567890',
    names: 'Thiago Benjamín',
    lastNames: 'Ramos Salazar',
    level: 'Secundaria',
    grade: '3°',
    section: 'B',
    parentName: 'Elena Salazar Vera',
    parentPhone: '923456789',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'std-4',
    dni: '75678901',
    names: 'Luciana Sofía',
    lastNames: 'Mendoza Chávez',
    level: 'Secundaria',
    grade: '4°',
    section: 'A',
    parentName: 'Jorge Mendoza Castillo',
    parentPhone: '934567890',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'std-5',
    dni: '76789012',
    names: 'Joaquín Gabriel',
    lastNames: 'Castro Paredes',
    level: 'Secundaria',
    grade: '5°',
    section: 'A',
    parentName: 'Carmen Paredes Ríos',
    parentPhone: '945678901',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'std-6',
    dni: '77890123',
    names: 'Camila Esperanza',
    lastNames: 'Vargas Mamani',
    level: 'Primaria',
    grade: '5°',
    section: 'A',
    parentName: 'Manuel Vargas Ticona',
    parentPhone: '956789012',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'std-7',
    dni: '78901234',
    names: 'Sebastián Alonso',
    lastNames: 'Gutiérrez Díaz',
    level: 'Primaria',
    grade: '6°',
    section: 'B',
    parentName: 'Patricia Díaz Silva',
    parentPhone: '967890123',
    createdAt: new Date().toISOString(),
  },
];

export function getStudents(): Student[] {
  try {
    const raw = localStorage.getItem(STUDENTS_KEY);
    if (!raw) {
      localStorage.setItem(STUDENTS_KEY, JSON.stringify(SEED_STUDENTS));
      return SEED_STUDENTS;
    }
    return JSON.parse(raw);
  } catch {
    return SEED_STUDENTS;
  }
}

export function saveStudents(students: Student[]) {
  try {
    localStorage.setItem(STUDENTS_KEY, JSON.stringify(students));
  } catch (e) {
    console.error('Error saving students', e);
  }
}

export function addStudent(student: Omit<Student, 'id' | 'createdAt'>): Student {
  const students = getStudents();
  const newStudent: Student = {
    ...student,
    id: 'std-' + Date.now() + '-' + Math.random().toString(36).substring(2, 6),
    createdAt: new Date().toISOString(),
  };
  const updated = [newStudent, ...students];
  saveStudents(updated);
  return newStudent;
}

export function updateStudent(id: string, updates: Partial<Student>): boolean {
  const students = getStudents();
  const index = students.findIndex((s) => s.id === id);
  if (index === -1) return false;
  students[index] = { ...students[index], ...updates };
  saveStudents(students);
  return true;
}

export function deleteStudent(id: string): boolean {
  const students = getStudents();
  const filtered = students.filter((s) => s.id !== id);
  saveStudents(filtered);
  return true;
}

export function findStudentByDni(dni: string): Student | undefined {
  const cleaned = dni.trim().replace(/\D/g, '');
  const students = getStudents();
  return students.find((s) => s.dni.trim() === cleaned);
}

export function getAttendances(): AttendanceRecord[] {
  try {
    const raw = localStorage.getItem(ATTENDANCE_KEY);
    if (!raw) return [];
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

export function saveAttendances(records: AttendanceRecord[]) {
  try {
    localStorage.setItem(ATTENDANCE_KEY, JSON.stringify(records));
  } catch (e) {
    console.error('Error saving attendances', e);
  }
}

export function recordAttendance(
  student: Student,
  method: 'QR' | 'Manual' = 'QR'
): { record: AttendanceRecord; isDuplicate: boolean } {
  const attendances = getAttendances();
  const now = new Date();
  const dateStr = now.toISOString().split('T')[0];
  const timeStr = now.toTimeString().split(' ')[0]; // HH:mm:ss

  // Check duplicate for today
  const existing = attendances.find(
    (a) => a.studentId === student.id && a.date === dateStr
  );

  if (existing) {
    return { record: existing, isDuplicate: true };
  }

  const settings = getSchoolSettings();
  const [entryH, entryM] = settings.entryTime.split(':').map(Number);
  const nowH = now.getHours();
  const nowM = now.getMinutes();

  let status: AttendanceRecord['status'] = 'Puntual';
  if (nowH > entryH || (nowH === entryH && nowM > entryM)) {
    status = 'Tardanza';
  }

  const newRecord: AttendanceRecord = {
    id: 'att-' + Date.now() + '-' + Math.random().toString(36).substring(2, 6),
    studentId: student.id,
    studentDni: student.dni,
    studentName: `${student.lastNames}, ${student.names}`,
    gradeSection: `${student.grade} "${student.section}" ${student.level}`,
    timestamp: now.toISOString(),
    date: dateStr,
    time: timeStr,
    status,
    notificationSent: false,
    method,
  };

  const updated = [newRecord, ...attendances];
  saveAttendances(updated);
  return { record: newRecord, isDuplicate: false };
}

export function getSchoolSettings(): SchoolSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(DEFAULT_SETTINGS));
      return DEFAULT_SETTINGS;
    }
    return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function saveSchoolSettings(settings: SchoolSettings) {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  } catch (e) {
    console.error('Error saving settings', e);
  }
}

export function exportAttendancesToCSV(records: AttendanceRecord[]): string {
  const headers = ['DNI', 'Estudiante', 'Grado y Sección', 'Fecha', 'Hora', 'Estado', 'Método'];
  const rows = records.map((r) => [
    `"${r.studentDni}"`,
    `"${r.studentName}"`,
    `"${r.gradeSection}"`,
    `"${r.date}"`,
    `"${r.time}"`,
    `"${r.status}"`,
    `"${r.method}"`,
  ]);

  return [headers.join(','), ...rows.map((row) => row.join(','))].join('\n');
}

export function createWhatsAppUrl(student: Student, record: AttendanceRecord): string {
  const settings = getSchoolSettings();
  const phone = student.parentPhone.replace(/\D/g, '');
  // Peru mobile phones start with 9 and have 9 digits. Prefix +51 if not present
  const fullPhone = phone.startsWith('51') ? phone : `51${phone}`;

  const message = settings.whatsappMessageTemplate
    .replace('{APODERADO}', student.parentName)
    .replace('{ESTUDIANTE}', `${student.names} ${student.lastNames}`)
    .replace('{FECHA}', record.date)
    .replace('{HORA}', record.time.substring(0, 5))
    .replace('{ESTADO}', record.status)
    .replace('{COLEGIO}', settings.schoolName);

  return `https://wa.me/${fullPhone}?text=${encodeURIComponent(message)}`;
}
