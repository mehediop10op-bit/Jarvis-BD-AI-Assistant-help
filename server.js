const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8080;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

// Middleware
app.use(cors());
app.use(express.json({ limit: '1mb' }));

// Health Check Endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'jarvis-backend-proxy',
    uptime: process.uptime(),
    timestamp: new Date().toISOString()
  });
});

/**
 * System instruction tailored for Jarvis – BD AI Assistant
 */
const SYSTEM_INSTRUCTION =
  "You are Jarvis – BD AI Assistant, an intelligent, polite, futuristic personal AI assistant. " +
  "Your primary language is Bengali (Bangla), and secondary is English. " +
  "Always respond concisely, warmly, and helpfully. " +
  "If the user asks in Bengali, answer in natural, respectful Bengali. " +
  "If the user asks in English, answer in English.";

/**
 * POST /chat
 * Body format:
 * {
 *   "query": "আজকের আবহাওয়া কেমন?", // or "prompt"
 *   "language": "bn",
 *   "history": [
 *     { "sender": "USER", "text": "হাই জারভিস" },
 *     { "sender": "JARVIS", "text": "জি, বলুন! আমি শুনছি।" }
 *   ]
 * }
 */
app.post('/chat', async (req, res) => {
  try {
    const rawPrompt = req.body.query || req.body.prompt;
    const language = req.body.language || 'bn';
    const history = Array.isArray(req.body.history) ? req.body.history : [];

    if (!rawPrompt || typeof rawPrompt !== 'string' || !rawPrompt.trim()) {
      const defaultReply = language === 'bn' ? 'জি, বলুন! আমি শুনছি।' : 'Yes, I am listening.';
      return res.status(200).json({ response: defaultReply, text: defaultReply });
    }

    const prompt = rawPrompt.trim();

    if (!GEMINI_API_KEY) {
      console.warn('GEMINI_API_KEY is not configured on the server.');
      return res.status(503).json({
        error: 'Backend API key is not configured.',
        response: language === 'bn' 
          ? 'সার্ভারে এআই কনফিগারেশন অনুপস্থিত। অনুগ্রহ করে ব্যাকএন্ড অ্যাডমিনকে অবহিত করুন।' 
          : 'Server AI configuration is missing.'
      });
    }

    // Build Gemini multi-turn payload
    const contents = [];
    let isFirstUserMessage = true;

    // Append up to last 6 history items
    const recentHistory = history.slice(-6);
    for (const item of recentHistory) {
      const isUser = item.sender === 'USER' || item.role === 'user';
      const role = isUser ? 'user' : 'model';
      const itemText = item.text || item.content || '';

      if (!itemText) continue;

      let formattedText = itemText;
      if (isFirstUserMessage && isUser) {
        formattedText = `${SYSTEM_INSTRUCTION}\n\nUser: ${itemText}`;
        isFirstUserMessage = false;
      }

      contents.push({
        role,
        parts: [{ text: formattedText }]
      });
    }

    // Append current prompt
    const currentText = isFirstUserMessage
      ? `${SYSTEM_INSTRUCTION}\n\nUser query: ${prompt}`
      : prompt;

    contents.push({
      role: 'user',
      parts: [{ text: currentText }]
    });

    const geminiUrl = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent';

    const response = await fetch(geminiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-goog-api-key': GEMINI_API_KEY
      },
      body: JSON.stringify({ contents })
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error(`Gemini API error [${response.status}]:`, errorText);
      return res.status(response.status).json({
        error: 'Gemini upstream error',
        response: language === 'bn' 
          ? 'সার্ভারে সাময়িক সমস্যা দেখা দিয়েছে। দয়া করে কিছুক্ষণ পর আবার চেষ্টা করুন।'
          : 'Upstream server error. Please try again shortly.'
      });
    }

    const data = await response.json();
    const candidate = data.candidates?.[0];
    const textPart = candidate?.content?.parts?.[0]?.text;

    if (!textPart) {
      const fallbackMsg = language === 'bn' ? 'আমি আপনার বিষয়টি বুঝতে পেরেছি।' : 'I understood your query.';
      return res.json({ response: fallbackMsg, text: fallbackMsg });
    }

    const reply = textPart.trim();
    // Return standard keys compatible with Jarvis client applet
    return res.json({
      response: reply,
      text: reply,
      reply: reply
    });
  } catch (error) {
    console.error('Error handling /chat proxy:', error);
    return res.status(500).json({
      error: 'Internal server error',
      message: error.message,
      response: 'সার্ভারে অভ্যন্তরীণ ত্রুটি ঘটেছে।'
    });
  }
});

// Start Server
app.listen(PORT, () => {
  console.log(`Jarvis backend proxy listening on port ${PORT}`);
});
