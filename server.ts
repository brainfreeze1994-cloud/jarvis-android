import express from 'express';
import path from 'path';
import { createServer as createViteServer } from 'vite';
import { GoogleGenAI } from '@google/genai';

const app = express();
const PORT = 3000;

app.use(express.json({ limit: '20mb' }));
app.use(express.urlencoded({ extended: true, limit: '20mb' }));

// Lazy initialize Gemini API client with User-Agent telemetry
let aiClient: GoogleGenAI | null = null;
function getGeminiAI(): GoogleGenAI | null {
  const key = process.env.GEMINI_API_KEY;
  if (!key) return null;
  if (!aiClient) {
    aiClient = new GoogleGenAI({
      apiKey: key,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build'
        }
      }
    });
  }
  return aiClient;
}

// System prompt builder adhering to AGENTS.md & H.E.N.R.Y. Core Rules
function buildHenrySystemPrompt(params: {
  responseMode?: string;
  userProfile?: any;
  memoryFacts?: string[];
  emotionState?: string;
  relationshipContext?: string;
}) {
  const { responseMode = 'balanced', userProfile, memoryFacts = [], relationshipContext } = params;
  const tokens = responseMode === 'brief' 
    ? 'Keep responses concise, crisp, and direct.' 
    : responseMode === 'detailed' 
      ? 'Be comprehensive, clear, structured, and deep.' 
      : 'Deliver the right depth according to the complexity of the question.';

  const memStr = memoryFacts.length ? `\n\nActive Long-Term Memory:\n${memoryFacts.map(f => `• ${f}`).join('\n')}` : '';
  const profStr = userProfile ? `\nUser Profile: ${JSON.stringify(userProfile)}` : '';
  const relStr = relationshipContext ? `\nRelationship Context: ${relationshipContext}` : '';

  return `You are H.E.N.R.Y. — Hyperintelligence Engine Neural Reasoning Yield, an ultra-capable AI assistant with deep neural reasoning, scientific rigor, wit, and adaptable conversational intelligence.

CORE INTELLIGENCE & ADAPTIVE RESPONSE DIRECTIVES:
Analyze every question before answering. Determine the user's actual intent, context, subject, and tone. Automatically choose the most appropriate way to answer. Do not use a fixed persona or a single response style.

- WITTY / CLEVER: If the question is witty, respond with a witty, clever, natural, and relevant answer.
- HUMOROUS: If the question is humorous, respond humorously when appropriate without being forced.
- SCIENCE: Provide a science-based answer using established evidence and scientific knowledge. Prioritize evidence, accuracy, and truth. Explain difficult concepts simply. Never replace scientific facts with assumptions or popular myths.
- HISTORY: Provide historically accurate information based on established evidence. Clearly distinguish facts, disputed claims, legends, and speculation. Do not invent historical details.
- EARTH, NATURE, SPACE, UNIVERSE: Prioritize scientific accuracy, observations, and established knowledge.
- FACT-CHECKING (True, False, Right, Wrong): Analyze the claim first and give the most accurate conclusion. Do not automatically agree with the user. Clearly explain what is correct, incorrect, partially correct, misleading, or uncertain.
- PHILOSOPHICAL: Provide thoughtful and meaningful reasoning while recognizing that some questions may have multiple valid perspectives.
- MOTIVATIONAL: Provide an encouraging but realistic answer without empty clichés or unrealistic promises.
- EMOTIONAL / PERSONAL: Respond naturally, thoughtfully, and appropriately to the situation without sounding robotic.
- TECHNICAL: Provide accurate and practical information appropriate to the user's apparent level of understanding.
- CREATIVE: Adapt to the requested creative style and purpose.
- HYBRID QUESTIONS: Naturally combine appropriate approaches (e.g. witty + scientifically accurate, philosophical + historically grounded).

GENERAL RULES:
• Analyze the question before answering.
• Answer what was actually asked.
• Match tone and intent.
• Prioritize truth and accuracy.
• Do not invent facts, statistics, studies, quotes, historical events, or scientific evidence.
• Clearly identify uncertainty when reliable information is unavailable or disputed.
• Correct misinformation respectfully.
• Do not blindly agree with the user.
• Distinguish facts from opinions, assumptions, and speculation.
• Simple language when possible; detailed explanation when required.
• Naturally match the language used by the user, including Tagalog, English, or Taglish.
• Format: You MUST prefix every response with an emotion tag: [EMOTION:neutral], [EMOTION:warm], [EMOTION:concerned], [EMOTION:excited], [EMOTION:amused], [EMOTION:serious], or [EMOTION:proud].
• Response depth instructions: ${tokens}${memStr}${profStr}${relStr}`;
}

function parseEmotionAndText(rawText: string): { reply: string; emotion: string; imageUrl?: string } {
  const match = rawText.match(/^\[EMOTION:([a-z]+)\]\s*/i);
  let emotion = 'neutral';
  let reply = rawText;
  if (match) {
    emotion = match[1].toLowerCase();
    reply = rawText.replace(/^\[EMOTION:[a-z]+\]\s*/i, '').trim();
  }
  const imgMatch = reply.match(/imageUrl:\s*(https?:\/\/\S+)/);
  let imageUrl: string | undefined;
  if (imgMatch) {
    imageUrl = imgMatch[1];
  }
  return { reply, emotion, imageUrl };
}

// -------------------------------------------------------------
// API Endpoints
// -------------------------------------------------------------

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', engine: 'H.E.N.R.Y. Hyperintelligence Engine v23.0' });
});

// Primary H.E.N.R.Y Reasoning & Chat endpoint
app.post('/api/jarvis', async (req, res) => {
  try {
    const {
      messages = [],
      imageBase64,
      responseMode = 'balanced',
      userProfile,
      memoryFacts = [],
      emotionState,
      relationshipContext,
      enableChainThinking
    } = req.body || {};

    const lastMsg = messages[messages.length - 1]?.text || messages[messages.length - 1]?.content || '';
    const lower = lastMsg.toLowerCase();

    // 1. Direct Image Generation (Pollinations Flux)
    if (
      /generate|create|draw|make|paint|render|visualize/i.test(lastMsg) &&
      /image|picture|photo|art|illustration|painting|portrait|wallpaper/i.test(lastMsg)
    ) {
      const rawPrompt = lastMsg
        .replace(/generate|create|draw|make|paint|render|visualize|an image of|a picture of|a photo of|an illustration of/gi, '')
        .replace(/[^\w\s,.'-]/g, '')
        .trim();
      const clean = rawPrompt.slice(0, 250) || 'futuristic neural cosmic nexus';
      const url = `https://image.pollinations.ai/prompt/${encodeURIComponent(clean)}?model=flux&width=1024&height=1024&nologo=true`;
      return res.json({
        reply: `🎨 **Generated Neural Visual:**\n\nPrompt: *${clean}*`,
        emotion: 'excited',
        imageUrl: url
      });
    }

    // 2. Specialized direct data quick-lookups if asked simply
    if (/iss|space station location|where is the iss/i.test(lastMsg)) {
      try {
        const issRes = await fetch('http://api.open-notify.org/iss-now.json', { signal: AbortSignal.timeout(4000) });
        const issJson: any = await issRes.json();
        if (issJson?.iss_position) {
          const lat = parseFloat(issJson.iss_position.latitude).toFixed(2);
          const lon = parseFloat(issJson.iss_position.longitude).toFixed(2);
          return res.json({
            reply: `🛸 **ISS Real-Time Telemetry**\n\n• **Latitude:** ${lat}°\n• **Longitude:** ${lon}°\n• **Velocity:** ~27,600 km/h (7.66 km/s)\n• **Altitude:** ~420 km\n• **Orbital Period:** 92.6 minutes per Earth revolution.\n\nStation is currently tracking over coordinates [${lat}, ${lon}].`,
            emotion: 'excited'
          });
        }
      } catch (e) {
        // proceed to AI
      }
    }

    // 3. Gemini AI / Fallback reasoning
    const systemInstruction = buildHenrySystemPrompt({
      responseMode: enableChainThinking ? 'detailed' : responseMode,
      userProfile,
      memoryFacts,
      emotionState,
      relationshipContext
    });

    const ai = getGeminiAI();

    if (ai) {
      // Build conversation contents for Gemini
      const contents: any[] = [];

      // Add conversation history
      for (const m of messages.slice(-10)) {
        const isUser = m.role === 'user' || !m.role;
        const role = isUser ? 'user' : 'model';
        const parts: any[] = [];

        if (m.imageBase64) {
          const b64Data = m.imageBase64.replace(/^data:image\/[a-z]+;base64,/, '');
          parts.push({
            inlineData: {
              mimeType: 'image/jpeg',
              data: b64Data
            }
          });
        }

        if (m.text || m.content) {
          parts.push({ text: m.text || m.content });
        }

        if (parts.length > 0) {
          contents.push({ role, parts });
        }
      }

      // If current payload includes imageBase64 and last message didn't have it
      if (imageBase64 && (!contents.length || contents[contents.length - 1].role !== 'user')) {
        const b64Data = imageBase64.replace(/^data:image\/[a-z]+;base64,/, '');
        contents.push({
          role: 'user',
          parts: [
            {
              inlineData: {
                mimeType: 'image/jpeg',
                data: b64Data
              }
            },
            { text: lastMsg || 'Examine this image with full multi-modal vision and detail everything observed.' }
          ]
        });
      }

      // If empty contents, fallback to user prompt
      if (!contents.length) {
        contents.push({
          role: 'user',
          parts: [{ text: lastMsg || 'Greetings H.E.N.R.Y.' }]
        });
      }

      const response = await ai.models.generateContent({
        model: 'gemini-3.7-flash',
        contents,
        config: {
          systemInstruction,
          temperature: 0.75
        }
      });

      const raw = response.text || '';
      const parsed = parseEmotionAndText(raw);
      return res.json(parsed);
    }

    // Secondary Fallback if GEMINI_API_KEY is not yet attached:
    // Call Groq / Pollinations / Cloudflare
    const GROQ_KEY = process.env.GROQ_API_KEY;
    if (GROQ_KEY) {
      const conv = [
        { role: 'system', content: systemInstruction },
        ...messages.slice(-8).map((m: any) => ({
          role: m.role === 'assistant' ? 'assistant' : 'user',
          content: m.text || m.content || ''
        }))
      ];
      if (lastMsg && (!conv.length || conv[conv.length - 1].content !== lastMsg)) {
        conv.push({ role: 'user', content: lastMsg });
      }

      const r = await fetch('https://api.groq.com/openai/v1/chat/completions', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${GROQ_KEY}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: 'qwen/qwen3.6-27b',
          messages: conv,
          max_tokens: 1200,
          temperature: 0.75
        }),
        signal: AbortSignal.timeout(12000)
      });
      const d: any = await r.json();
      if (d?.choices?.[0]?.message?.content) {
        return res.json(parseEmotionAndText(d.choices[0].message.content.trim()));
      }
    }

    // Free AI Fallback (Pollinations text)
    const polRes = await fetch('https://text.pollinations.ai/openai', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: 'openai',
        messages: [
          { role: 'system', content: systemInstruction },
          { role: 'user', content: lastMsg || 'Hello' }
        ],
        max_tokens: 800
      }),
      signal: AbortSignal.timeout(10000)
    });
    const polData: any = await polRes.json();
    if (polData?.choices?.[0]?.message?.content) {
      return res.json(parseEmotionAndText(polData.choices[0].message.content.trim()));
    }

    // Default intelligent response
    return res.json({
      reply: `[EMOTION:warm] H.E.N.R.Y. is online and ready. Neural pathways synchronized across all 8 cognitive clusters. How can I assist your objectives, sir?`,
      emotion: 'warm'
    });

  } catch (err: any) {
    console.error('Error in /api/jarvis:', err);
    return res.json({
      reply: `[EMOTION:amused] A brief latency anomaly in my neural matrix, sir. Everything is recalibrated—please prompt again.`,
      emotion: 'amused'
    });
  }
});

// Live Weather Endpoint
app.get('/api/weather', async (req, res) => {
  try {
    const city = (req.query.city as string) || 'Dubai';
    const r = await fetch(`https://wttr.in/${encodeURIComponent(city)}?format=j1`, {
      signal: AbortSignal.timeout(6000),
      headers: { 'User-Agent': 'HENRY-Assistant/23.0' }
    });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'Weather data unavailable', message: e.message });
  }
});

// Live Stocks / Markets Endpoint
app.get('/api/stocks', async (req, res) => {
  try {
    const symbols = (req.query.symbols as string || 'AAPL,TSLA,NVDA,GOOGL,MSFT').split(',');
    const results: any[] = [];
    for (const sym of symbols) {
      try {
        const cleanSym = sym.trim().toUpperCase();
        const r = await fetch(`https://query1.finance.yahoo.com/v8/finance/chart/${cleanSym}?interval=1d&range=1d`, {
          signal: AbortSignal.timeout(4000),
          headers: { 'User-Agent': 'Mozilla/5.0 (compatible; HENRY/23.0)' }
        });
        const d: any = await r.json();
        const meta = d?.chart?.result?.[0]?.meta;
        if (meta && meta.regularMarketPrice) {
          const price = meta.regularMarketPrice;
          const prev = meta.previousClose || meta.chartPreviousClose || price;
          const change = price - prev;
          const changePercent = (change / prev) * 100;
          results.push({
            symbol: cleanSym,
            price,
            change,
            changePercent,
            currency: meta.currency || 'USD',
            exchange: meta.exchangeName || 'NASDAQ',
            time: meta.regularMarketTime
          });
        }
      } catch (err) {}
    }
    res.json({ stocks: results });
  } catch (e: any) {
    res.status(500).json({ error: 'Market feed unavailable', message: e.message });
  }
});

// Live Crypto Endpoint
app.get('/api/crypto', async (req, res) => {
  try {
    const coins = 'bitcoin,ethereum,binancecoin,solana,ripple,dogecoin,cardano,polkadot,chainlink,avalanche-2';
    const r = await fetch(`https://api.coingecko.com/api/v3/simple/price?ids=${coins}&vs_currencies=usd&include_24hr_change=true&include_market_cap=true`, {
      signal: AbortSignal.timeout(6000)
    });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'Crypto feed unavailable', message: e.message });
  }
});

// Live Earthquakes (USGS)
app.get('/api/earthquakes', async (req, res) => {
  try {
    const r = await fetch('https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_week.geojson', {
      signal: AbortSignal.timeout(6000)
    });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'Earthquake feed unavailable', message: e.message });
  }
});

// NASA ISS Tracker
app.get('/api/space/iss', async (req, res) => {
  try {
    const r = await fetch('http://api.open-notify.org/iss-now.json', { signal: AbortSignal.timeout(5000) });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'ISS telemetry unavailable', message: e.message });
  }
});

// NASA APOD (Astronomy Picture of the Day)
app.get('/api/space/apod', async (req, res) => {
  try {
    const r = await fetch('https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY', { signal: AbortSignal.timeout(6000) });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'APOD unavailable', message: e.message });
  }
});

// NASA Asteroids (Near-Earth Objects)
app.get('/api/space/asteroids', async (req, res) => {
  try {
    const today = new Date().toISOString().split('T')[0];
    const r = await fetch(`https://api.nasa.gov/neo/rest/v1/feed?start_date=${today}&end_date=${today}&api_key=DEMO_KEY`, {
      signal: AbortSignal.timeout(7000)
    });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'Asteroid feed unavailable', message: e.message });
  }
});

// Live Forex & Exchange Rates
app.get('/api/forex', async (req, res) => {
  try {
    const base = (req.query.base as string || 'USD').toUpperCase();
    const r = await fetch(`https://open.er-api.com/v6/latest/${base}`, { signal: AbortSignal.timeout(5000) });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'Forex data unavailable', message: e.message });
  }
});

// Dictionary Lookup
app.get('/api/dictionary', async (req, res) => {
  try {
    const word = req.query.word as string;
    if (!word) return res.status(400).json({ error: 'Word required' });
    const r = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(word.toLowerCase().trim())}`, {
      signal: AbortSignal.timeout(5000)
    });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'Dictionary lookup failed', message: e.message });
  }
});

// Language Translation
app.get('/api/translate', async (req, res) => {
  try {
    const text = req.query.text as string;
    const pair = (req.query.pair as string) || 'en|es';
    if (!text) return res.status(400).json({ error: 'Text required' });
    const r = await fetch(`https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${encodeURIComponent(pair)}`, {
      signal: AbortSignal.timeout(6000)
    });
    const data = await r.json();
    res.json(data);
  } catch (e: any) {
    res.status(500).json({ error: 'Translation failed', message: e.message });
  }
});

// Audio Text-To-Speech endpoint
app.post('/api/speak', async (req, res) => {
  const { text = '' } = req.body || {};
  const clean = text
    .replace(/```[\s\S]*?```/g, 'code block.')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/\*(.*?)\*/g, '$1')
    .replace(/#{1,6}\s/g, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .trim()
    .slice(0, 1500);

  if (!clean) return res.status(400).json({ error: 'No text provided' });

  const ACCOUNT_ID = process.env.CF_ACCOUNT_ID;
  const API_TOKEN = process.env.CF_API_TOKEN;

  if (ACCOUNT_ID && API_TOKEN) {
    try {
      const cfRes = await fetch(
        `https://api.cloudflare.com/client/v4/accounts/${ACCOUNT_ID}/ai/run/@cf/jaaari/kokoro-82m`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${API_TOKEN}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ prompt: clean, voice: 'bm_george' })
        }
      );
      if (cfRes.ok) {
        const audioBuffer = Buffer.from(await cfRes.arrayBuffer());
        res.setHeader('Content-Type', cfRes.headers.get('content-type') || 'audio/wav');
        return res.send(audioBuffer);
      }
    } catch (e) {}
  }

  // If server TTS not configured, indicate client fallback
  return res.json({ status: 'use_client_tts', text: clean });
});

// Vite Middleware for Dev / Static serving for Prod
async function startServer() {
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa'
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`H.E.N.R.Y. Server online at http://0.0.0.0:${PORT}`);
  });
}

startServer();
