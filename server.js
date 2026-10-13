const express = require('express');

const app = express();
app.use(express.json());

// Tus credenciales reales
const TELEGRAM_TOKEN = '8953997654:AAHnHbM4Oa-OgP_FzF4v_JJzBWZPqKXdXwo';
const CHAT_ID = '6061349193';

app.post('/api/yape-notification', async (req, res) => {
  console.log('>>> PETICION RECIBIDA EN EL SERVIDOR <<<');
  const { sender, amount, date } = req.body;

  const text = `🔔 *¡Nuevo Yape Recibido!*\n\n` +
               `👤 *De:* ${sender || 'Desconocido'}\n` +
               `💵 *Monto:* S/ ${amount || '0.00'}\n` +
               `📅 *Fecha:* ${date || new Date().toLocaleString()}`;

  try {
    const url = `https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`;
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        chat_id: CHAT_ID,
        text: text,
        parse_mode: 'Markdown'
      })
    });

    const data = await response.json();
    console.log('>>> RESPUESTA TELEGRAM:', data);

    if (data.ok) {
      return res.status(200).json({ success: true, message: 'Enviado a Telegram' });
    } else {
      return res.status(500).json({ error: data.description });
    }
  } catch (err) {
    console.log('>>> ERROR DE RED:', err.message);
    return res.status(500).json({ error: err.message });
  }
});

// Inicia el servidor HTTP
app.listen(3000, () => {
  console.log('==================================================');
  console.log('  SERVIDOR ACTIVO Y ESCUCHANDO EN PUERTO 3000');
  console.log('==================================================');
});