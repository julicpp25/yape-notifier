export type EducationLevel = 'Primaria' | 'Secundaria';

export type AttendanceStatus = 'Puntual' | 'Tardanza' | 'Justificada' | 'Inasistencia';

export interface Student {
  id: string;
  dni: string;
  names: string;
  lastNames: string;
  level: EducationLevel;
  grade: string; // "1°", "2°", "3°", "4°", "5°", "6°"
  section: string; // "A", "B", "C", "D"
  parentName: string;
  parentPhone: string;
  photoUrl?: string;
  createdAt: string;
}

export interface AttendanceRecord {
  id: string;
  studentId: string;
  studentDni: string;
  studentName: string;
  gradeSection: string;
  timestamp: string; // ISO string
  date: string; // YYYY-MM-DD
  time: string; // HH:mm:ss
  status: AttendanceStatus;
  notificationSent: boolean;
  method: 'QR' | 'Manual';
}

export interface SchoolSettings {
  schoolName: string;
  modularCode: string;
  ugel: string;
  entryTime: string; // "07:45"
  lateThreshold: string; // "08:00"
  enableSound: boolean;
  autoNotifyWhatsapp: boolean;
  whatsappMessageTemplate: string;
}
