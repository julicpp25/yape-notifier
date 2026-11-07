export interface CobroRecord {
  id: string;
  hora: string;
  fecha: string;
  timestamp: number;
  nombre: string;
  monto: number;
  metodo: 'Yape' | 'Plin' | 'Transferencia' | 'Efectivo';
  cajero: string;
}

export interface ParseResult {
  esValido: boolean;
  esPublicidad: boolean;
  metodo: string;
  monto: number;
  remitente: string;
  fraseVoz: string;
  razonDescarte?: string;
}
