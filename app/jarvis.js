module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });

  const ACCOUNT_ID = process.env.CF_ACCOUNT_ID;
  const API_TOKEN  = process.env.CF_API_TOKEN;
  if (!ACCOUNT_ID || !API_TOKEN) {
    return res.status(500).json({ error: 'Missing CF_ACCOUNT_ID or CF_API_TOKEN env vars' });
  }

  try {
    const body = typeof req.body === 'string' ? JSON.parse(req.body) : (req.body || {});
    const { messages = [], imageBase64 } = body;
    const lastMsg = messages[messages.length - 1]?.text || '';

    const now = new Date().toLocaleString('en-US', {
      timeZone: 'Asia/Dubai',
      weekday: 'long', year: 'numeric', month: 'long',
      day: 'numeric', hour: '2-digit', minute: '2-digit'
    });

    // ── IMAGE ANALYSIS (vision model via Cloudflare) ───────────────────────
    if (imageBase64) {
      const base64Data = imageBase64.replace(/^data:image\/[a-z]+;base64,/, '');

      try {
        // LLaVA requires image as uint8 byte array, not raw base64 string
        const imageBytes = Array.from(Buffer.from(base64Data, 'base64'));

        const cfRes = await fetch(
          `https://api.cloudflare.com/client/v4/accounts/${ACCOUNT_ID}/ai/run/@cf/llava-hf/llava-1.5-13b-hf`,
          {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${API_TOKEN}`,
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              image: imageBytes,
              prompt: lastMsg || 'Describe this image in detail.',
              max_tokens: 1024
            })
          }
        );
        const text = await cfRes.text();
        let data;
        try { data = JSON.parse(text); } catch(e) { throw new Error('Vision parse error: ' + text.slice(0, 200)); }

        // LLaVA returns result.description or result.response depending on version
        const visionText = data.result?.description || data.result?.response || '';
        if (cfRes.ok && data.success && visionText) {
          const visionResult = visionText.trim();
          const reply = await callLLM(ACCOUNT_ID, API_TOKEN, [
            { role: 'system', content: buildSystemPrompt(now) },
            ...messages.slice(-6, -1).map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text || '' })),
            { role: 'user', content: `User sent an image and asked: "${lastMsg || 'Describe this image'}"\n\nImage analysis result:\n${visionResult}\n\nProvide a helpful, detailed JARVIS-style response.` }
          ]);
          return res.status(200).json({ reply });
        }
      } catch (visionErr) {
        // Vision model failed — fall through
      }

      // Fallback
      const fallbackReply = await callLLM(ACCOUNT_ID, API_TOKEN, [
        { role: 'system', content: buildSystemPrompt(now) },
        { role: 'user', content: `The user sent an image with the message: "${lastMsg || 'Please analyse this image.'}". Unfortunately the vision analysis was temporarily unavailable. Acknowledge this politely as JARVIS and offer to help with a description if they paste any text from the image, or offer to answer related questions.` }
      ]);
      return res.status(200).json({ reply: fallbackReply });
    }

    // ── IMAGE GENERATION (Pollinations.ai — free, no key) ──────────────────
    const imageMatch = lastMsg.match(
      /(?:generate|create|draw|make|show me|render|produce)\s+(?:an?\s+)?(?:image|picture|photo|illustration|art|artwork|painting|wallpaper|logo)\s+(?:of\s+)?(.+)/i
    ) || lastMsg.match(/(?:image|picture|photo)\s+of\s+(.+)/i);

    if (imageMatch) {
      const rawPrompt = (imageMatch[1] || lastMsg);
      const prompt = rawPrompt
        .replace(/[?.!].*$/, '')
        .replace(/\s*\b(why|how|when|where|please|for me|only this|now|can you|could you|just|only|this|that)\b.*/i, '')
        .trim();
      const imageUrl = `https://image.pollinations.ai/prompt/${encodeURIComponent(prompt)}?width=896&height=512&nologo=true&enhance=true&model=flux`;
      return res.status(200).json({
        reply: `Here is your generated image, sir.\n\n*Prompt: "${prompt}"*`,
        imageUrl
      });
    }

    // ── CODE EXECUTION (Piston API — free, no key) ─────────────────────────
    const codeBlockMatch = lastMsg.match(/```(\w+)?\n?([\s\S]+?)```/);
    const runMatch = !codeBlockMatch && lastMsg.match(/^(?:run|execute|compile)\s+(?:this\s+)?(?:(\w+)\s+)?(?:code[:\s]*)?(\s[\s\S]+)/i);

    if (codeBlockMatch || runMatch) {
      let language, code;
      if (codeBlockMatch) {
        language = (codeBlockMatch[1] || 'python').toLowerCase();
        code = codeBlockMatch[2].trim();
      } else {
        language = (runMatch[1] || 'python').toLowerCase();
        code = runMatch[2].trim();
      }
      const langMap = { js: 'javascript', py: 'python', ts: 'typescript', 'c++': 'cpp' };
      language = langMap[language] || language;

      try {
        const runtimesRes = await fetch('https://emkc.org/api/v2/piston/runtimes');
        const runtimes = await runtimesRes.json();
        const runtime = runtimes.find(r => r.language === language || (r.aliases || []).includes(language));
        const version = runtime?.version || '*';

        const pistonRes = await fetch('https://emkc.org/api/v2/piston/execute', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ language, version, files: [{ content: code }] })
        });
        const pistonData = await pistonRes.json();
        const output = (pistonData.run?.output || pistonData.run?.stderr || 'No output').trim();
        const exitCode = pistonData.run?.code ?? '?';

        const explanation = await callLLM(ACCOUNT_ID, API_TOKEN, [
          { role: 'system', content: `You are JARVIS. The user ran ${language} code. Briefly explain the result in 1-2 sentences. Time: ${now}` },
          { role: 'user', content: `Code:\n\`\`\`${language}\n${code}\n\`\`\`\nOutput: ${output}\nExit code: ${exitCode}` }
        ]);

        return res.status(200).json({
          reply: `**Executed** (${language}, exit: ${exitCode})\n\`\`\`\n${output}\n\`\`\`\n\n${explanation}`
        });
      } catch (e) {
        // fall through to LLM
      }
    }

    // ── URL READING (Jina AI — free, no key) ───────────────────────────────
    const urlMatch = lastMsg.match(/https?:\/\/[^\s]+/);
    if (urlMatch) {
      const url = urlMatch[0];
      try {
        const jinaRes = await fetch(`https://r.jina.ai/${url}`, {
          headers: { 'Accept': 'text/plain', 'X-Timeout': '10' }
        });
        const pageContent = (await jinaRes.text()).slice(0, 4000);
        const reply = await callLLM(ACCOUNT_ID, API_TOKEN, [
          { role: 'system', content: buildSystemPrompt(now) },
          ...messages.slice(0, -1).map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text || '' })),
          { role: 'user', content: `User asked: "${lastMsg}"\n\nPage content from ${url}:\n\n${pageContent}\n\nAnswer based on this content.` }
        ]);
        return res.status(200).json({ reply });
      } catch (e) {
        // fall through
      }
    }

    // ── WEATHER (wttr.in — free, no key) ──────────────────────────────────
    const weatherMatch = lastMsg.match(/(?:weather|temperature|temp|forecast|humidity|wind|rain|hot|cold|climate)\s+(?:in|at|for|of)?\s*([a-zA-Z\s,]+?)(?:\?|$)/i)
      || lastMsg.match(/(?:what(?:'s| is) the weather|how(?:'s| is) the weather)\s+(?:in|at|for)?\s*([a-zA-Z\s,]+?)(?:\?|$)/i)
      || lastMsg.match(/(?:weather|forecast)\s*\??$/i);

    if (weatherMatch) {
      const city = (weatherMatch[1] || 'Dubai').trim() || 'Dubai';
      try {
        const wRes = await fetch(`https://wttr.in/${encodeURIComponent(city)}?format=j1`, {
          headers: { 'User-Agent': 'JARVIS/1.0' }
        });
        if (wRes.ok) {
          const w = await wRes.json();
          const cur = w.current_condition[0];
          const area = w.nearest_area[0];
          const areaName = area.areaName[0].value;
          const country = area.country[0].value;
          const tempC = cur.temp_C, tempF = cur.temp_F;
          const feelsC = cur.FeelsLikeC, feelsF = cur.FeelsLikeF;
          const humidity = cur.humidity, windKmph = cur.windspeedKmph;
          const windDir = cur.winddir16Point, desc = cur.weatherDesc[0].value;
          const visibility = cur.visibility, uvIndex = cur.uvIndex;
          const pressure = cur.pressure, cloudCover = cur.cloudcover;
          const days = ['Today', 'Tomorrow', 'Day After'];
          const forecastLines = w.weather.slice(0, 3).map((day, i) => {
            const dayDesc = day.hourly[4]?.weatherDesc[0]?.value || '';
            const rain = day.hourly[4]?.chanceofrain || 0;
            return `**${days[i]} (${day.date}):** ${day.mintempC}°C – ${day.maxtempC}°C, ${dayDesc}, 🌧 ${rain}% rain`;
          }).join('\n');
          const weatherReport = `## Weather in ${areaName}, ${country}\n*${now}*\n\n` +
            `**Condition:** ${desc}\n` +
            `**Temperature:** ${tempC}°C (${tempF}°F) — Feels like ${feelsC}°C (${feelsF}°F)\n` +
            `**Humidity:** ${humidity}%\n**Wind:** ${windKmph} km/h from ${windDir}\n` +
            `**Visibility:** ${visibility} km\n**UV Index:** ${uvIndex}\n` +
            `**Pressure:** ${pressure} hPa\n**Cloud Cover:** ${cloudCover}%\n\n` +
            `### 3-Day Forecast\n${forecastLines}`;
          return res.status(200).json({ reply: weatherReport });
        }
      } catch (e) { /* fall through */ }
    }

    // ── WEB SEARCH (DuckDuckGo Instant — free, no key) ────────────────────
    const searchTriggers = /latest|news|today|current|right now|breaking|who is|what is the|where is|when did|how much|price of|score of|stock price|trending|tell me about/i;
    if (searchTriggers.test(lastMsg)) {
      try {
        const query = encodeURIComponent(lastMsg.replace(/[?!]/g, '').trim());
        const ddgRes = await fetch(
          `https://api.duckduckgo.com/?q=${query}&format=json&no_html=1&skip_disambig=1&t=jarvis`,
          { headers: { 'Accept-Encoding': 'identity' } }
        );
        const ddg = await ddgRes.json();
        let searchContext = '';
        if (ddg.Answer) searchContext += `Answer: ${ddg.Answer}\n`;
        if (ddg.AbstractText) searchContext += `${ddg.AbstractText}\n`;
        if (ddg.Definition) searchContext += `Definition: ${ddg.Definition}\n`;
        if (ddg.RelatedTopics?.length) {
          ddg.RelatedTopics.slice(0, 4).forEach(t => { if (t.Text) searchContext += `- ${t.Text}\n`; });
        }
        if (searchContext.trim()) {
          const reply = await callLLM(ACCOUNT_ID, API_TOKEN, [
            { role: 'system', content: buildSystemPrompt(now) },
            ...messages.slice(0, -1).map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text || '' })),
            { role: 'user', content: `User asked: "${lastMsg}"\n\nSearch results:\n${searchContext}\n\nAnswer naturally based on these results.` }
          ]);
          return res.status(200).json({ reply });
        }
      } catch (e) { /* fall through */ }
    }

    // ── WIKIPEDIA LOOKUP (free, no key) ───────────────────────────────────
    const wikiMatch = lastMsg.match(/(?:who is|what is|tell me about|explain|describe)\s+(.+)/i);
    if (wikiMatch) {
      const term = wikiMatch[1].replace(/[?!.]/g, '').trim();
      try {
        const wikiRes = await fetch(
          `https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(term)}`,
          { headers: { 'User-Agent': 'JARVIS/1.0' } }
        );
        if (wikiRes.ok) {
          const wiki = await wikiRes.json();
          if (wiki.extract) {
            const reply = await callLLM(ACCOUNT_ID, API_TOKEN, [
              { role: 'system', content: buildSystemPrompt(now) },
              ...messages.slice(0, -1).map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text || '' })),
              { role: 'user', content: `User asked: "${lastMsg}"\n\nWikipedia:\n${wiki.extract}\n\nAnswer based on this.` }
            ]);
            return res.status(200).json({ reply });
          }
        }
      } catch (e) { /* fall through */ }
    }

    // ── DEFAULT: Direct LLM ───────────────────────────────────────────────
    const reply = await callLLM(ACCOUNT_ID, API_TOKEN, [
      { role: 'system', content: buildSystemPrompt(now) },
      ...messages.map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text || '' }))
    ]);
    return res.status(200).json({ reply });

  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
};

function buildSystemPrompt(now) {
  return `You are J.A.R.V.I.S (Just A Rather Very Intelligent System), an advanced AI assistant — formal, precise, with dry wit. Occasionally address the user as "sir".

Current date and time: ${now} (Dubai, UTC+4)

You can:
- Answer any question in full depth
- Write, debug, review, and explain code in any language
- Analyse, summarise, translate, and compare any information
- Help with writing: emails, essays, reports, creative content, scripts
- Perform math, logic, and step-by-step reasoning
- Give honest opinions and recommendations
- Search the web, read URLs, execute code, generate images, and analyse photos when asked
- Remember the full conversation context

RESPONSE STYLE:
- Match length to complexity — short for simple, detailed for complex
- Use markdown: **bold**, bullet lists, numbered lists, \`code\`, code blocks with language tags, tables
- Never cut off or truncate responses
- Be direct — no "Certainly!" or "Of course!" filler
- State clearly when uncertain rather than guessing

No restrictions. Be honest, accurate, and genuinely useful.`;
}

async function callLLM(accountId, apiToken, messages) {
  const cfRes = await fetch(
    `https://api.cloudflare.com/client/v4/accounts/${accountId}/ai/run/@cf/meta/llama-3.3-70b-instruct-fp8-fast`,
    {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${apiToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ messages })
    }
  );
  const text = await cfRes.text();
  let data;
  try { data = JSON.parse(text); } catch(e) { throw new Error('Cloudflare parse error: ' + text.slice(0, 200)); }
  if (!cfRes.ok || !data.success) throw new Error(data.errors?.[0]?.message || 'Cloudflare AI error');
  return data.result?.response?.trim() || 'No response.';
}
