const express = require('express');
const axios = require('axios');
const { google } = require('googleapis');
const app = express();

app.use(express.json());

const TELEGRAM_TOKEN = '8953997654:AAHnHbM4Oa-OgP_FzF4v_JJzBWZPqKXdXwo';

// Coloca aquí el ID largo de tu hoja de Google Sheets
const SPREADSHEET_ID = '16tuTOojGjw9d_dmiJo-koldetLvruIJYXXLIcv-3Wkw';

// Autenticación usando el archivo credentials.json creado en Render
const auth = new google.auth.GoogleAuth({
  keyFile: 'credentials.json',
  scopes: ['https://www.googleapis.com/auth/spreadsheets'],
});

app.post('/api/yape-notification', async (req, res) => {
  const { sender, amount, chat_id } = req.body;

  const clientName = sender || 'Cliente Desconocido';
  const payAmount = amount || '0.00';
  const chatId = chat_id || '6061349193';
  const now = new Date().toLocaleString('es-PE', { timeZone: 'America/Lima' });

  try {
    // 1. Enviar notificación a Telegram
    const telegramUrl = `https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`;
    const messageText = `🔔 *¡Nuevo Yape Recibido!*\n\n👤 *De:* ${clientName}\n💵 *Monto:* S/ ${payAmount}\n📅 *Fecha:* ${now}`;

    await axios.post(telegramUrl, {
      chat_id: chatId,
      text: messageText,
      parse_mode: 'Markdown'
    });

    // 2. Registrar la venta en Google Sheets
    const sheets = google.sheets({ version: 'v4', auth });
    await sheets.spreadsheets.values.append({
      spreadsheetId: SPREADSHEET_ID,
      range: 'Hoja 1!A:D', // Cambia "Hoja 1" si tu pestaña tiene otro nombre
      valueInputOption: 'USER_ENTERED',
      requestBody: {
        values: [[now, clientName, payAmount, chatId]],
      },
    });

    console.log(`[OK] Registrado en Sheets y enviado a Telegram: ${clientName} - S/ ${payAmount}`);
    res.status(200).json({ status: 'success', message: 'Notificación procesada y registrada.' });

  } catch (error) {
    console.error('Error procesando la solicitud:', error.message);
    res.status(500).json({ status: 'error', message: error.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Servidor activo en el puerto ${PORT}`);
});
