const express = require('express');
const axios = require('axios');
const { google } = require('googleapis');
const app = express();

app.use(express.json());

const TELEGRAM_TOKEN = '8953997654:AAHnHbM4Oa-OgP_FzF4v_JJzBWZPqKXdXwo';
const SPREADSHEET_ID = '16tuTOojGjw9d_dmiJo-koldetLvruIJYXXLIcv-3Wkw';

// Mapeo opcional: asigna un nombre a cada Chat ID para crear la pestaña con el nombre del negocio
// Si un chat_id no está aquí, usará el propio Chat ID como nombre de pestaña.
const NEGOCIOS = {
  '6061349193': 'Mi_Negocio_1',
  '1234567890': 'Don_Pepe_Bodega',
  '9876543210': 'Farmacia_Santa_Rosa'
};

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

  // Determinar la pestaña según el Chat ID recibido de la app
  const targetSheet = NEGOCIOS[chatId] || `Negocio_${chatId}`;

  try {
    // 1. Enviar notificación al Telegram del dueño correspondiente
    const telegramUrl = `https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`;
    const messageText = `🔔 *¡Nuevo Yape Recibido!*\n\n👤 *De:* ${clientName}\n💵 *Monto:* S/ ${payAmount}\n📅 *Fecha:* ${now}`;

    await axios.post(telegramUrl, {
      chat_id: chatId,
      text: messageText,
      parse_mode: 'Markdown'
    });

    // 2. Registrar en la pestaña correspondiente de Google Sheets
    const sheets = google.sheets({ version: 'v4', auth });
    
    await sheets.spreadsheets.values.append({
      spreadsheetId: SPREADSHEET_ID,
      range: `'${targetSheet}'!A:D`,
      valueInputOption: 'USER_ENTERED',
      requestBody: {
        values: [[now, clientName, payAmount, chatId]],
      },
    });

    console.log(`[OK] Guardado en pestaña [${targetSheet}] y enviado a Telegram ID [${chatId}]`);
    res.status(200).json({ status: 'success', message: 'Notificación procesada.' });

  } catch (error) {
    console.error('Error procesando la solicitud:', error.message);
    res.status(500).json({ status: 'error', message: error.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Servidor activo en el puerto ${PORT}`);
});
