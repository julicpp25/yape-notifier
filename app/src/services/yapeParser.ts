import { ParseResult } from '../types/yape';

export const FRASES_MOTIVADORAS = [
  "¡A seguir vendiendo!",
  "¡Excelente venta!",
  "¡Negocio en marcha!",
  "¡Sumando ganancias!",
  "¡Cliente satisfecho!",
  "¡El negocio crece!",
  "¡Buena racha!",
  "¡Caja llena!",
  "¡Éxito total!",
  "¡Seguimos facturando!",
  "¡Otro cliente feliz!",
  "¡Meta del día más cerca!",
  "¡Venta confirmada!",
  "¡Dinero en cuenta!",
  "¡Vamos por más!",
  "¡Caja activa!",
  "¡Produciendo al cien!",
  "¡Negocio imparable!",
  "¡Así da gusto vender!",
  "¡Cobro recibido!",
  "¡Directo a la ganancia!",
  "¡Suma y sigue!",
  "¡Día productivo!",
  "¡Gran trabajo en caja!",
  "¡Facturando con fuerza!",
  "¡Pago verificado!",
  "¡Negocio próspero!",
  "¡Récord de ventas!",
  "¡Sonríe, estamos vendiendo!",
  "¡Plata en mano!",
  "¡Ventas arriba!",
  "¡Día ganador!",
  "¡Así se hace!",
  "¡Caja sonriente!",
  "¡Comercio activo!",
  "¡Ganancia asegurada!",
  "¡Caja en positivo!",
  "¡Cero deudas, pura venta!",
  "¡Cosechando frutos!",
  "¡Venta limpia!",
  "¡Caja actualizada!"
];

export function parseNotificacion(texto: string, pkgName?: string): ParseResult {
  const t = texto.trim();

  // 1. Escudo Publicidad
  const esPublicidad = /(crédito|credito|préstamo|prestamo|pre-aprobado|aprobado al instante|pide tu|promoción|promo|descuento|sorteo|gana|premios|seguro|paga tus servicios|solicita)/i.test(t);
  if (esPublicidad) {
    return {
      esValido: false,
      esPublicidad: true,
      metodo: 'Desconocido',
      monto: 0,
      remitente: '',
      fraseVoz: '',
      razonDescarte: 'Descartado por Escudo 1: Es una notificación de publicidad, préstamos o servicios del banco, no un pago de cliente.',
    };
  }

  // 2. Escudo Verbo de Pago Obligatorio
  const tieneVerboPago = /(te envi|te mand|transfiri|recibiste un pago|pago de|te yape|yapeó|yapeo|te pline|plineó|plineo|recibiste un yape|recibiste un plin|recibiste)/i.test(t);
  if (!tieneVerboPago) {
    return {
      esValido: false,
      esPublicidad: false,
      metodo: 'Desconocido',
      monto: 0,
      remitente: '',
      fraseVoz: '',
      razonDescarte: 'Descartado por Escudo 2: No contiene ningún verbo de pago válido (ej: "te envió", "te yapeó", "recibiste un pago").',
    };
  }

  // 3. Extraer Monto en Soles
  const matchMonto = t.match(/S\/\.?\s*([\d]+(?:[.,]\d+)?)/i);
  if (!matchMonto) {
    return {
      esValido: false,
      esPublicidad: false,
      metodo: 'Desconocido',
      monto: 0,
      remitente: '',
      fraseVoz: '',
      razonDescarte: 'Descartado por Escudo 3: No se pudo identificar el monto en Soles (S/).',
    };
  }

  const montoNum = parseFloat(matchMonto[1].replace(',', '.'));
  if (isNaN(montoNum) || montoNum <= 0) {
    return {
      esValido: false,
      esPublicidad: false,
      metodo: 'Desconocido',
      monto: 0,
      remitente: '',
      fraseVoz: '',
      razonDescarte: 'El monto detectado es menor o igual a cero.',
    };
  }

  // Detectar Método
  let metodo = 'Yape';
  if (/plin/i.test(t) || (pkgName && /interbank|bbva|scotiabank|banbif/i.test(pkgName))) {
    metodo = 'Plin';
  } else if (/transferencia/i.test(t)) {
    metodo = 'Transferencia';
  }

  // 4. Extraer Remitente Limpio
  let remitente = 'Cliente';
  const regexVerbos = /(te envi[oó]|transfiri[oó]|te mand[oó]|recibiste un pago de|pago de|te yape[oó]|yape[oó]|te pline[oó]|pline[oó])/i;

  if (regexVerbos.test(t)) {
    const antes = t.split(regexVerbos)[0];
    const limpia = antes.replace(/(¡|!|Te llegó un Yape|Yape|Plin|Pago|Notificación)/gi, '').trim();
    if (limpia.length >= 3) {
      remitente = limpia;
    }
  }

  // Si el nombre aparece después ("de Juan Pérez")
  if (remitente === 'Cliente' || remitente.length < 3) {
    const matchDe = t.match(/(?:de|por)\s+([A-Za-zÀ-ÿ\s]{3,35})/i);
    if (matchDe && matchDe[1]) {
      const candidata = matchDe[1].trim();
      if (!/yape|plin/i.test(candidata)) {
        remitente = candidata;
      }
    }
  }

  const animo = FRASES_MOTIVADORAS[Math.floor(Math.random() * FRASES_MOTIVADORAS.length)];
  const fraseMonto = montoNum === 1 ? 'un sol' : `${montoNum.toFixed(2)} soles`;
  const fraseVoz = `¡Te llegó un ${metodo} de ${fraseMonto}! ${animo}`;

  return {
    esValido: true,
    esPublicidad: false,
    metodo,
    monto: montoNum,
    remitente,
    fraseVoz,
  };
}

export function hablarTexto(texto: string, onEnd?: () => void) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return;

  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(texto);
  utterance.lang = 'es-PE';
  utterance.rate = 1.05;
  utterance.pitch = 1.04;

  const voices = window.speechSynthesis.getVoices();
  const esVoice = voices.find(
    (v) => v.lang.startsWith('es-PE') || v.lang.startsWith('es-419') || v.lang.startsWith('es-')
  );
  if (esVoice) utterance.voice = esVoice;

  if (onEnd) {
    utterance.onend = onEnd;
  }

  window.speechSynthesis.speak(utterance);
}
