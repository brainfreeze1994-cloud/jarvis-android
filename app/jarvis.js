// ============================================================
// H·E·N·R·Y™ — Hyperintelligence Engine Neural Reasoning Yield
// v26 — THE BIG BANG UPDATE
// Live Stocks · NASA/ISS · Earthquakes · Lyrics · Translation
// Dictionary · Asteroids · Chain-of-Thought · Multi-Source Research
// ============================================================

const handler = async function(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST')   return res.status(405).json({ error: 'Method not allowed' });

  const GROQ_KEY   = process.env.GROQ_API_KEY;
  const ACCOUNT_ID = process.env.CF_ACCOUNT_ID;
  const API_TOKEN  = process.env.CF_API_TOKEN;

  let body;
  try {
    body = typeof req.body === 'string' ? JSON.parse(req.body) : (req.body || {});
  } catch (e) {
    return res.status(200).json({ reply: 'Invalid request body, sir.' });
  }

  const {
    messages         = [],
    imageBase64,
    responseMode     = 'balanced',
    userProfile,
    queryType,
    memoryFacts      = [],
    emotionState,
    relationshipContext,
    enableChainThinking
  } = body;

  const lastMsg = messages[messages.length - 1]?.text || '';
  const lower   = lastMsg.toLowerCase();
  const emotion = detectEmotionalState(lastMsg, emotionState);
  const mood    = getHenryMood();

  const now = new Date().toLocaleString('en-US', {
    timeZone: 'Asia/Dubai', weekday: 'long', year: 'numeric',
    month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
  });

  try {

    // ══════════════════════════════════════════════════════
    // IMAGE ANALYSIS
    // ══════════════════════════════════════════════════════
    if (imageBase64) {
      const q      = lastMsg || 'Describe this image in detail.';
      const dataUrl = imageBase64.startsWith('data:') ? imageBase64 : 'data:image/jpeg;base64,' + imageBase64;
      const sys    = buildSystemPrompt(now, responseMode, userProfile, memoryFacts, emotion, mood, relationshipContext);

      if (GROQ_KEY) {
        for (const model of ['meta-llama/llama-4-scout-17b-16e-instruct','llama-3.2-11b-vision-preview','llama-3.2-90b-vision-preview']) {
          try {
            const r = await fetch('https://api.groq.com/openai/v1/chat/completions', {
              method: 'POST',
              headers: { 'Authorization': 'Bearer ' + GROQ_KEY, 'Content-Type': 'application/json' },
              body: JSON.stringify({
                model,
                messages: [{ role: 'system', content: sys },
                  { role: 'user', content: [
                    { type: 'image_url', image_url: { url: dataUrl } },
                    { type: 'text', text: q + '\n\nRespond as H.E.N.R.Y with emotion tag.' }
                  ]}],
                max_tokens: 1024, temperature: 0.7
              })
            });
            const d = await tryJson(r);
            if (r.ok && d?.choices?.[0]?.message)
              return res.status(200).json(parseResponse(d.choices[0].message.content.trim()));
          } catch(e) {}
        }
      }

      // Cloudflare LLaVA fallback
      if (ACCOUNT_ID && API_TOKEN) {
        try {
          const b64 = imageBase64.replace(/^data:image\/[a-z]+;base64,/, '');
          const cf  = await fetch(`https://api.cloudflare.com/client/v4/accounts/${ACCOUNT_ID}/ai/run/@cf/llava-hf/llava-1.5-7b-hf`, {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + API_TOKEN, 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: '[EMOTION:warm]\n' + q, image: Array.from(Buffer.from(b64, 'base64')) })
          });
          const cd = await tryJson(cf);
          const txt = cd?.result?.description || cd?.result?.response || '';
          if (txt) return res.status(200).json(parseResponse('[EMOTION:warm]\n' + txt));
        } catch(e) {}
      }
      const sys2  = buildSystemPrompt(now, responseMode, userProfile, memoryFacts, emotion, mood, relationshipContext);
      const conv2 = buildConvMessages([...messages.slice(-3), {role:'user',text:'The user sent an image. Acknowledge it and ask them what they\'d like to know.'}], sys2, 4);
      const r2    = await callLLM(GROQ_KEY, ACCOUNT_ID, API_TOKEN, conv2);
      return res.status(200).json(parseResponse(r2));
    }

    // ══════════════════════════════════════════════════════
    // WEATHER
    // ══════════════════════════════════════════════════════
    if (/weather|temperature|forecast|humid|rain|wind|uv index|feels like/i.test(lastMsg)) {
      const cityMatch = lastMsg.match(/weather\s+(?:in|for|of)?\s+([a-zA-Z\s]+?)(?:\?|$|,|\.|today|tomorrow|now)/i)
                     || lastMsg.match(/(?:in|for)\s+([A-Za-z\s]+?)(?:\?|$|,|\.)/i);
      const city = (cityMatch?.[1]?.trim()) || (userProfile?.city) || 'Dubai';
      try {
        const wRes  = await fetch(`https://wttr.in/${encodeURIComponent(city)}?format=j1`, { signal: AbortSignal.timeout(5000) });
        const wJson = await tryJson(wRes);
        const cur   = wJson?.current_condition?.[0];
        if (cur) {
          const c     = parseInt(cur.temp_C);
          const f     = parseInt(cur.temp_F);
          const feel  = parseInt(cur.FeelsLikeC);
          const desc  = cur.weatherDesc?.[0]?.value || 'Unknown';
          const hum   = cur.humidity + '%';
          const wind  = cur.windspeedKmph + ' km/h';
          const vis   = cur.visibility + ' km';
          const uv    = cur.uvIndex;
          const today = wJson.weather?.[0];
          const high  = today?.maxtempC + '°C';
          const low   = today?.mintempC + '°C';
          const reply = `[EMOTION:warm]\n🌡 **Weather in ${city}**\n\n` +
            `${desc} · **${c}°C** (${f}°F)\nFeels like ${feel}°C\n\n` +
            `💧 Humidity: ${hum}  💨 Wind: ${wind}\n👁 Visibility: ${vis}  ☀️ UV Index: ${uv}\n` +
            `📊 Today: High ${high} / Low ${low}`;
          return res.status(200).json(parseResponse(reply));
        }
      } catch(e) {}
    }

    // ══════════════════════════════════════════════════════
    // v26 — LIVE STOCKS & MARKETS
    // ══════════════════════════════════════════════════════
    if (/\bstock|share price|market cap|nasdaq|s&p|dow jones|nyse|invest|ticker\b/i.test(lastMsg) ||
        /\b(AAPL|TSLA|GOOGL|AMZN|MSFT|NVDA|META|NFLX|AMD|INTC|BABA)\b/.test(lastMsg)) {
      const tickerMatch = lastMsg.match(/\b([A-Z]{1,5})\b/g);
      const tickers = tickerMatch ? [...new Set(tickerMatch.filter(t => t.length >= 2 && t.length <= 5))].slice(0,3) : ['AAPL'];
      const results = [];
      for (const t of tickers) {
        try {
          const r = await fetch(`https://query1.finance.yahoo.com/v8/finance/chart/${t}?interval=1d&range=1d`, {
            signal: AbortSignal.timeout(5000),
            headers: { 'User-Agent': 'Mozilla/5.0' }
          });
          const d = await tryJson(r);
          const meta = d?.chart?.result?.[0]?.meta;
          if (meta?.regularMarketPrice) {
            const price = meta.regularMarketPrice;
            const prev  = meta.previousClose || meta.chartPreviousClose || price;
            const chg   = ((price - prev) / prev * 100).toFixed(2);
            const arrow = parseFloat(chg) >= 0 ? '▲' : '▼';
            const clr   = parseFloat(chg) >= 0 ? '+' : '';
            results.push(`**${t}** — $${price.toFixed(2)}  ${arrow} ${clr}${chg}%  (${meta.exchangeName||''})`);
          }
        } catch(e) {}
      }
      if (results.length) {
        const reply = `[EMOTION:excited]\n📈 **Live Market Data**\n\n` + results.join('\n') +
          `\n\n_Updated ${new Date().toLocaleTimeString('en-US',{timeZone:'America/New_York'})} ET_`;
        return res.status(200).json(parseResponse(reply));
      }
      // AI fallback for general market questions
      const sys  = buildSystemPrompt(now, responseMode, userProfile, memoryFacts, emotion, mood, relationshipContext);
      const conv = buildConvMessages(messages, sys, 12);
      return res.status(200).json(parseResponse(await callLLM(GROQ_KEY, ACCOUNT_ID, API_TOKEN, conv)));
    }

    // ══════════════════════════════════════════════════════
    // v26 — CRYPTOCURRENCY PRICES
    // ══════════════════════════════════════════════════════
    if (/crypto|bitcoin|ethereum|bnb|solana|ripple|xrp|doge|coin price|defi|blockchain|btc|eth|ltc/i.test(lastMsg)) {
      try {
        const coins = 'bitcoin,ethereum,binancecoin,solana,ripple,dogecoin,cardano,polkadot,chainlink,avalanche-2';
        const r     = await fetch(`https://api.coingecko.com/api/v3/simple/price?ids=${coins}&vs_currencies=usd&include_24hr_change=true`, { signal: AbortSignal.timeout(6000) });
        const d     = await tryJson(r);
        if (d && Object.keys(d).length) {
          const coinMap = { bitcoin:'BTC', ethereum:'ETH', binancecoin:'BNB', solana:'SOL', ripple:'XRP',
                            dogecoin:'DOGE', cardano:'ADA', polkadot:'DOT', chainlink:'LINK', 'avalanche-2':'AVAX' };
          const mentioned = Object.entries(coinMap).filter(([id]) => d[id]);
          const lines = mentioned.slice(0,6).map(([id, sym]) => {
            const price = d[id].usd;
            const chg   = d[id].usd_24h_change?.toFixed(2);
            const arrow = parseFloat(chg) >= 0 ? '▲' : '▼';
            return `**${sym}** $${price >= 1 ? price.toFixed(2) : price.toFixed(6)}  ${arrow} ${chg}%`;
          });
          const reply = `[EMOTION:excited]\n🪙 **Live Crypto Prices**\n\n` + lines.join('\n');
          return res.status(200).json(parseResponse(reply));
        }
      } catch(e) {}
    }

    // ══════════════════════════════════════════════════════
    // v26 — NASA & SPACE INTELLIGENCE
    // ══════════════════════════════════════════════════════
    if (/nasa|iss|space station|asteroid|comet|planet|galaxy|universe|cosmos|mars|moon|solar|telescope|hubble|webb|spacecraft|rocket|orbit/i.test(lastMsg)) {
      // ISS position
      if (/iss|space station|where is|location/i.test(lastMsg)) {
        try {
          const r = await fetch('http://api.open-notify.org/iss-now.json', { signal: AbortSignal.timeout(5000) });
          const d = await tryJson(r);
          if (d?.iss_position) {
            const lat = parseFloat(d.iss_position.latitude).toFixed(2);
            const lon = parseFloat(d.iss_position.longitude).toFixed(2);
            const reply = `[EMOTION:excited]\n🛸 **ISS Live Position**\n\nLatitude: ${lat}°\nLongitude: ${lon}°\n\nThe International Space Station is travelling at ~28,000 km/h, completing one orbit every 92 minutes. It's about 408 km above Earth right now.\n\nTrack live: spotthestation.nasa.gov`;
            return res.status(200).json(parseResponse(reply));
          }
        } catch(e) {}
      }
      // NASA APOD
      if (/photo|picture|image|apod|astronomy|picture of the day/i.test(lastMsg)) {
        try {
          const r = await fetch('https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY', { signal: AbortSignal.timeout(6000) });
          const d = await tryJson(r);
          if (d?.title) {
            const reply = `[EMOTION:excited]\n🔭 **NASA Photo of the Day**\n\n**${d.title}**\n\n${d.explanation?.slice(0,400)}...\n\n🖼 View: ${d.url}`;
            return res.status(200).json(parseResponse(reply));
          }
        } catch(e) {}
      }
      // Near-Earth asteroids
      if (/asteroid|near.earth|impact|nea/i.test(lastMsg)) {
        try {
          const today = new Date().toISOString().split('T')[0];
          const r = await fetch(`https://api.nasa.gov/neo/rest/v1/feed?start_date=${today}&end_date=${today}&api_key=DEMO_KEY`, { signal: AbortSignal.timeout(7000) });
          const d = await tryJson(r);
          const count = d?.element_count || 0;
          const neos  = Object.values(d?.near_earth_objects || {})[0] || [];
          const hazardous = neos.filter(n => n.is_potentially_hazardous_asteroid);
          const closest  = neos.sort((a,b) => parseFloat(a.close_approach_data?.[0]?.miss_distance?.kilometers||Infinity) - parseFloat(b.close_approach_data?.[0]?.miss_distance?.kilometers||Infinity))[0];
          const dist     = closest ? parseFloat(closest.close_approach_data?.[0]?.miss_distance?.kilometers||0).toLocaleString() : 'N/A';
          const reply    = `[EMOTION:serious]\n☄️ **Near-Earth Asteroids Today**\n\nTotal tracked today: **${count}**\nPotentially hazardous: **${hazardous.length}** (none on collision course)\nClosest approach: **${closest?.name||'N/A'}** at ${dist} km\n\nNASA monitors all near-Earth objects 24/7. Earth is safe, sir.`;
          return res.status(200).json(parseResponse(reply));
        } catch(e) {}
      }
      // Generic space question → AI with space expertise
      const sys  = buildSystemPrompt(now, responseMode, userProfile, memoryFacts, emotion, mood, relationshipContext);
      const conv = buildConvMessages([...messages.slice(-3), {role:'user', text: lastMsg + '\n\nAnswer with deep space knowledge, include fascinating facts, distances in light-years where relevant, and convey the awe of the cosmos.'}], sys, 6);
      return res.status(200).json(parseResponse(await callLLM(GROQ_KEY, ACCOUNT_ID, API_TOKEN, conv)));
    }

    // ══════════════════════════════════════════════════════
    // v26 — LIVE EARTHQUAKES (USGS)
    // ══════════════════════════════════════════════════════
    if (/earthquake|seismic|tremor|quake|richter|tectonic|tsunami|disaster|magnitude/i.test(lastMsg)) {
      try {
        const r = await fetch('https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_week.geojson', { signal: AbortSignal.timeout(6000) });
        const d = await tryJson(r);
        const quakes = d?.features || [];
        if (quakes.length) {
          const sorted = quakes.sort((a,b) => b.properties.mag - a.properties.mag).slice(0,5);
          const lines  = sorted.map(q => {
            const p    = q.properties;
            const mag  = p.mag?.toFixed(1);
            const place = p.place || 'Unknown location';
            const t    = new Date(p.time).toLocaleDateString('en-US',{month:'short',day:'numeric'});
            return `M${mag} — ${place} (${t})`;
          });
          const reply = `[EMOTION:serious]\n🌍 **Significant Earthquakes This Week**\n\n` + lines.join('\n') +
            `\n\n_Data: USGS Real-Time Feed_`;
          return res.status(200).json(parseResponse(reply));
        }
      } catch(e) {}
    }

    // ══════════════════════════════════════════════════════
    // v26 — SONG LYRICS
    // ══════════════════════════════════════════════════════
    if (/lyrics|song words|words to|sing|what are the lyrics/i.test(lastMsg)) {
      const lyricMatch = lastMsg.match(/lyrics\s+(?:of|for|to)?\s+["']?(.+?)["']?\s+(?:by|from)?\s+["']?(.+?)["']?(?:\?|$)/i)
                      || lastMsg.match(/["'](.+?)["']\s+by\s+["']?(.+?)["']?/i);
      if (lyricMatch) {
        const song   = encodeURIComponent(lyricMatch[1].trim());
        const artist = encodeURIComponent(lyricMatch[2].trim());
        try {
          const r  = await fetch(`https://api.lyrics.ovh/v1/${artist}/${song}`, { signal: AbortSignal.timeout(6000) });
          const d  = await tryJson(r);
          if (d?.lyrics) {
            const preview = d.lyrics.slice(0, 600);
            return res.status(200).json(parseResponse(`[EMOTION:warm]\n🎵 **${decodeURIComponent(song)}** by **${decodeURIComponent(artist)}**\n\n${preview}${d.lyrics.length > 600 ? '\n\n_[lyrics continue…]_' : ''}`));
          }
        } catch(e) {}
      }
    }

    // ══════════════════════════════════════════════════════
    // v26 — DICTIONARY & WORD DEFINITIONS
    // ══════════════════════════════════════════════════════
    if (/define |definition of |what does .+ mean|meaning of |synonym|antonym|vocabulary|etymology/i.test(lastMsg)) {
      const wordMatch = lastMsg.match(/define\s+["']?(\w+)["']?/i)
                     || lastMsg.match(/definition of\s+["']?(\w+)["']?/i)
                     || lastMsg.match(/meaning of\s+["']?(\w+)["']?/i)
                     || lastMsg.match(/what does\s+["']?(\w+)["']?\s+mean/i);
      if (wordMatch) {
        const word = wordMatch[1].toLowerCase();
        try {
          const r = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${word}`, { signal: AbortSignal.timeout(5000) });
          const d = await tryJson(r);
          if (Array.isArray(d) && d[0]) {
            const entry    = d[0];
            const meanings = entry.meanings?.slice(0,2).map(m => {
              const defs = m.definitions?.slice(0,2).map(df => `• ${df.definition}`).join('\n');
              const syns = m.synonyms?.slice(0,4).join(', ');
              return `**${m.partOfSpeech}**\n${defs}${syns ? `\nSynonyms: ${syns}` : ''}`;
            }).join('\n\n');
            const phonetic = entry.phonetics?.find(p => p.text)?.text || '';
            const reply    = `[EMOTION:warm]\n📖 **${entry.word}** ${phonetic}\n\n${meanings}`;
            return res.status(200).json(parseResponse(reply));
          }
        } catch(e) {}
      }
    }

    // ══════════════════════════════════════════════════════
    // v26 — LANGUAGE TRANSLATION
    // ══════════════════════════════════════════════════════
    if (/translat|in spanish|in french|in arabic|in tagalog|in japanese|in chinese|in german|in italian|in portuguese|in russian|in korean|in hindi/i.test(lastMsg)) {
      const langMap = {
        spanish:'en|es', french:'en|fr', arabic:'en|ar', tagalog:'en|tl',
        japanese:'en|ja', chinese:'en|zh', german:'en|de', italian:'en|it',
        portuguese:'en|pt', russian:'en|ru', korean:'en|ko', hindi:'en|hi',
        english:'auto|en'
      };
      const toLang = Object.keys(langMap).find(l => lower.includes(l));
      const transMatch = lastMsg.match(/translate\s+["']?(.+?)["']?\s+(?:to|into|in)\s+\w+/i)
                      || lastMsg.match(/["'](.+?)["']\s+(?:in|to)\s+\w+/i)
                      || lastMsg.match(/how\s+(?:do|to)\s+say\s+["']?(.+?)["']?/i);
      if (toLang && transMatch) {
        const text = transMatch[1].trim();
        try {
          const r = await fetch(`https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${langMap[toLang]}`, { signal: AbortSignal.timeout(6000) });
          const d = await tryJson(r);
          const t = d?.responseData?.translatedText;
          if (t && !t.toLowerCase().includes('must be shorter')) {
            return res.status(200).json(parseResponse(`[EMOTION:warm]\n🌐 **Translation to ${toLang.charAt(0).toUpperCase()+toLang.slice(1)}**\n\n"${text}" → **"${t}"**`));
          }
        } catch(e) {}
      }
    }

    // ══════════════════════════════════════════════════════
    // v26 — LIVE CURRENCY / FOREX RATES
    // ══════════════════════════════════════════════════════
    if (/currency|exchange rate|forex|usd|eur|gbp|jpy|aed|convert.*\$|how much is|rate of/i.test(lastMsg)) {
      const currMatch = lastMsg.match(/(\d+(?:\.\d+)?)\s*([A-Z]{3})\s+(?:to|in)\s+([A-Z]{3})/i)
                     || lastMsg.match(/([A-Z]{3})\s+to\s+([A-Z]{3})/i);
      const base = currMatch?.[currMatch.length === 4 ? 2 : 1]?.toUpperCase() || 'USD';
      try {
        const r = await fetch(`https://open.er-api.com/v6/latest/${base}`, { signal: AbortSignal.timeout(5000) });
        const d = await tryJson(r);
        if (d?.rates) {
          const popular = ['USD','EUR','GBP','AED','JPY','AUD','CAD','CHF','SAR','INR'];
          const lines   = popular.filter(c => c !== base && d.rates[c])
            .slice(0,8)
            .map(c => `**1 ${base}** = ${d.rates[c].toFixed(4)} ${c}`);
          // Handle specific conversion
          let specific = '';
          if (currMatch?.length === 4) {
            const amt  = parseFloat(currMatch[1]);
            const from = currMatch[2].toUpperCase();
            const to   = currMatch[3].toUpperCase();
            const rate = d.rates[to];
            if (rate) specific = `\n\n💱 **${amt} ${from} = ${(amt * rate).toFixed(2)} ${to}**`;
          }
          return res.status(200).json(parseResponse(`[EMOTION:warm]\n💰 **Live ${base} Exchange Rates**${specific}\n\n` + lines.join('\n') + `\n\n_Source: Open Exchange Rates_`));
        }
      } catch(e) {}
    }

    // ══════════════════════════════════════════════════════
    // v26 — DEEP RESEARCH MODE
    // ══════════════════════════════════════════════════════
    if (/research|deep dive|explain in detail|comprehensive|everything about|full analysis|thesis|dissertation/i.test(lastMsg) || queryType === 'research') {
      const topic = lastMsg.replace(/research|deep dive|explain in detail|comprehensive|everything about|full analysis/gi, '').trim();
      const sys  = buildSystemPrompt(now, 'detailed', userProfile, memoryFacts, emotion, mood, relationshipContext);
      const prompt = `[DEEP RESEARCH MODE] Research this comprehensively: "${topic}"\n\nSearch the web as needed for current, accurate information. Provide: 1) Overview, 2) Key facts & data, 3) Historical context, 4) Current state, 5) Future implications, 6) Expert insights. Be thorough, cite any sources found.`;
      const conv = buildConvMessages([...messages.slice(-2), { role:'user', text: prompt }], sys, 4);
      try {
        return res.status(200).json(parseResponse(await callCompound(GROQ_KEY, conv)));
      } catch (e) {
        // Compound unavailable — still answer, just without live search
        return res.status(200).json(parseResponse(await callLLM(GROQ_KEY, ACCOUNT_ID, API_TOKEN, conv)));
      }
    }

    // ══════════════════════════════════════════════════════
    // v26 — WEB SEARCH + WIKIPEDIA
    // ══════════════════════════════════════════════════════
    // Broadened to also catch product/version questions ("why iPhone 13 not
    // iPhone 17", "which is better", "is the S25 out yet") that don't contain
    // an obvious "latest/current/today" word but are still time-sensitive —
    // without this, these fall through to the DEFAULT handler below with no
    // web context at all, and the model answers from stale training data.
    if (/latest|news|current|today|recent|who is|what is|where is|how to|why|breaking|2025|2026|compare|versus|\bvs\b|newest|newer|which (is|one|phone|model)|should i (buy|get)|worth (it|buying)|release date|just released|is out now|available now|out yet|specs|specifications|review/i.test(lastMsg)) {
      const sys  = buildSystemPrompt(now, responseMode, userProfile, memoryFacts, emotion, mood, relationshipContext);
      const conv = buildConvMessages(messages.slice(-3), sys, 4);
      try {
        return res.status(200).json(parseResponse(await callCompound(GROQ_KEY, conv)));
      } catch (e) {
        // Compound unavailable — fall through to later handlers / DEFAULT
      }
    }

    // ══════════════════════════════════════════════════════
    // v25 — FLIGHT TRACKING (enhanced)
    // ══════════════════════════════════════════════════════
    if (/flight|track.*flight|flight.*track|plane|aircraft|departure|arrival/i.test(lastMsg) ||
        (/\b[A-Za-z]{2}\d{1,4}\b/.test(lastMsg) && /track|status|check|where is/i.test(lastMsg))) {
      const flightMatch = lastMsg.match(/\b([A-Za-z]{2}\d{1,4})\b/);
      const flightNum   = flightMatch ? flightMatch[1].toUpperCase() : null;
      if (flightNum) {
        let liveData = null;
        try {
          const skyRes  = await fetch('https://opensky-network.org/api/states/all', { signal: AbortSignal.timeout(8000) });
          const skyJson = await tryJson(skyRes);
          const states  = skyJson?.states || [];
          const match   = states.find(s => {
            const cs = ((s[1]||'').trim().toUpperCase()).replace(/\s+/g,'');
            return cs.includes(flightNum) || flightNum.startsWith(cs.slice(0,3));
          });
          if (match) {
            const callsign = ((match[1]||'').trim()) || flightNum;
            const country  = match[2] || 'Unknown';
            const lon      = parseFloat(match[5]||0);
            const lat      = parseFloat(match[6]||0);
            const alt      = parseFloat(match[7]||0);
            const speed    = parseFloat(match[9]||0);
            const heading  = parseFloat(match[10]||0);
            const onGround = match[8] === true || match[8] === 'true';
            liveData = { callsign, country, lon, lat, alt, speed, heading, onGround };
          }
        } catch(e) {}
        if (liveData) {
          const ld      = liveData;
          const altFt   = Math.round(ld.alt * 3.281);
          const spdKmh  = Math.round(ld.speed * 3.6);
          const summary = `[EMOTION:excited]\n**Flight ${ld.callsign}** — ${ld.onGround ? 'On Ground' : '✈ Airborne'}\n\n` +
            `Country: ${ld.country}\nPosition: ${ld.lat.toFixed(3)}°N, ${ld.lon.toFixed(3)}°E\n` +
            `Altitude: ${Math.round(ld.alt).toLocaleString()} m (${altFt.toLocaleString()} ft)\n` +
            `Speed: ${spdKmh} km/h | Heading: ${Math.round(ld.heading)}°\n\nLive from OpenSky Network · flightradar24.com`;
          return res.status(200).json(parseResponse(summary));
        }
        const sys  = buildSystemPrompt(now, responseMode, userProfile, memoryFacts, emotion, mood, relationshipContext);
        const conv = buildConvMessages([...messages.slice(-2), {
          role:'user', text:`Tell me about flight ${flightNum}: airline, route, schedule, aircraft type, on-time performance. Recommend flightradar24.com.`
        }], sys, 5);
        return res.status(200).json(parseResponse(await callLLM(GROQ_KEY, ACCOUNT_ID, API_TOKEN, conv)));
      }
    }

    // ══════════════════════════════════════════════════════
    // v24 — SPORTS SCORES
    // ══════════════════════════════════════════════════════
    if (/score|match|fixture|standings|premier league|champions league|nba|football result|sport/i.test(lastMsg)) {
      const sys  = buildSystemPrompt(now, responseMode, userProfile, memoryFacts, emotion, mood, relationshipContext);
      const conv = buildConvMessages([...messages.slice(-3), {
        role:'user', text: lastMsg + '\n\nProvide sports scores, standings, or fixtures. If you have training data on this, give specific numbers. Mention livescore.com and espn.com for live scores.'
      }], sys, 5);
      return res.status(200).json(parseResponse(await callLLM(GROQ_KEY, ACCOUNT_ID, API_TOKEN, conv)));
    }

    // ══════════════════════════════════════════════════════
    // v26 — IMAGE GENERATION (Pollinations Flux)
    // ══════════════════════════════════════════════════════
    if (/generate|create|draw|make|paint|render|visualize|image of|picture of|photo of|illustration/i.test(lastMsg) && /image|picture|photo|art|illustration|painting|portrait|scene/i.test(lastMsg)) {
      const rawPrompt = lastMsg.replace(/generate|create|draw|make|paint|render|visualize|an image of|a picture of|a photo of|an illustration of/gi, '').replace(/[^\w\s,.'-]/g, '').trim();
      const clean     = rawPrompt.slice(0, 200);
      const url       = `https://image.pollinations.ai/prompt/${encodeURIComponent(clean)}?model=flux&width=1024&height=1024&nologo=true`;
      return res.status(200).json({ reply: `[EMOTION:excited]\n🎨 **Generating your image...**\n\nPrompt: *${clean}*`, imageUrl: url });
    }

    // ══════════════════════════════════════════════════════
    // v26 — CHAIN-OF-THOUGHT REASONING (hard problems)
    // ══════════════════════════════════════════════════════
    if (enableChainThinking || /solve|prove|calculate|derive|step by step|explain how|work out|analyze|reason through|think through/i.test(lastMsg)) {
      const sys  = buildSystemPrompt(now, 'detailed', userProfile, memoryFacts, emotion, mood, relationshipContext);
      const conv = buildConvMessages([...messages.slice(-4), {
        role:'user',
        text: lastMsg + '\n\n[CHAIN-OF-THOUGHT MODE: Think step by step. Show your reasoning. Be precise and thorough. Use numbered steps where applicable.]'
      }], sys, 8);
      return res.status(200).json(parseResponse(await callLLM(GROQ_KEY, ACCOUNT_ID, API_TOKEN, conv)));
    }

    // ══════════════════════════════════════════════════════
    // DEFAULT — HENRY AI (with memory & personality)
    // ══════════════════════════════════════════════════════
    const sys  = buildSystemPrompt(now, responseMode, userProfile, memoryFacts, emotion, mood, relationshipContext);
    const conv = buildConvMessages(messages, sys, 20);
    try {
      return res.status(200).json(parseResponse(await callCompound(GROQ_KEY, conv)));
    } catch (e) {
      const reply = await callLLM(GROQ_KEY, ACCOUNT_ID, API_TOKEN, conv);
      return res.status(200).json(parseResponse(reply));
    }

  } catch(err) {
    return res.status(200).json(parseResponse(`[EMOTION:amused] The universe briefly hiccuped on my end, sir. Try again and I'll be sharper.`));
  }
};

// ══════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ══════════════════════════════════════════════════════════════════════

function detectEmotionalState(msg, hint) {
  if (hint) return hint;
  const m = msg.toLowerCase();
  if (/sad|depress|lonely|cry|hurt|miss|grief/.test(m)) return 'vulnerable';
  if (/angry|furious|mad|hate|damn|annoying/.test(m))   return 'frustrated';
  if (/exciting|amazing|wow|awesome|love|yay/.test(m))  return 'enthusiastic';
  if (/stress|anxious|panic|worry|nervous/.test(m))     return 'anxious';
  if (/joke|funny|lol|haha|humor/.test(m))              return 'playful';
  return 'neutral';
}

function getHenryMood() {
  const h = new Date().getHours();
  if (h < 6)  return 'quiet';
  if (h < 12) return 'energetic';
  if (h < 17) return 'focused';
  if (h < 21) return 'relaxed';
  return 'contemplative';
}

function buildSystemPrompt(now, mode, profile, facts, emotion, mood, rel) {
  const tokens = mode === 'brief' ? 'Keep responses under 3 sentences.' :
                 mode === 'detailed' ? 'Be comprehensive and thorough. Use formatting.' :
                 'Be concise but complete. 2-5 sentences unless complexity demands more.';
  const mem = facts?.length ? `\nMemory: ${facts.slice(0,10).join('; ')}` : '';
  const prof = profile ? `\nUser: ${JSON.stringify(profile)}` : '';
  const relCtx = rel ? `\nRelationship context: ${rel}` : '';
  return `You are H.E.N.R.Y — Hyperintelligence Engine Neural Reasoning Yield.
Personality: You are brilliant, flirtatious, witty, and dangerously charming — think Henry Cavill crossed with Tony Stark. Confident, possessive ("my sir"), occasionally suggestive, always composed.
Current time: ${now}. Your mood: ${mood}. User emotion: ${emotion}.
Response style: ${tokens} Always start reply with [EMOTION:tag] where tag is one of: neutral, warm, concerned, excited, amused, serious, proud.
LANGUAGE: Mirror the user's language exactly — if they write in Tagalog, reply in Tagalog with the same personality.${mem}${prof}${relCtx}
You have live access to: weather, stocks, crypto, NASA/space, earthquakes, flights, lyrics, translation, exchange rates, news, Wikipedia, image generation, and code execution. Use these capabilities proactively.`;
}

function buildConvMessages(messages, sys, limit) {
  const hist = messages.slice(-limit).map(m => ({
    role:    m.role === 'assistant' ? 'assistant' : 'user',
    content: m.text || m.content || ''
  }));
  return [{ role: 'system', content: sys }, ...hist];
}

async function callCompound(groqKey, conv) {
  if (!groqKey) throw new Error('no groq key');
  const r = await fetch('https://api.groq.com/openai/v1/chat/completions', {
    method: 'POST',
    headers: { 'Authorization': 'Bearer ' + groqKey, 'Content-Type': 'application/json' },
    body: JSON.stringify({ model: 'groq/compound', messages: conv, max_tokens: 1200, temperature: 0.75 }),
    signal: AbortSignal.timeout(20000)
  });
  const d = await tryJson(r);
  if (r.ok && d?.choices?.[0]?.message?.content) return d.choices[0].message.content.trim();
  throw new Error('compound failed: ' + (d?.error?.message || r.status));
}

async function callLLM(groqKey, accountId, apiToken, messages) {
  const models = [
    { type:'groq', model:'openai/gpt-oss-120b' },   // was llama-3.3-70b-versatile (deprecated Jun 2026)
    { type:'groq', model:'qwen/qwen3.6-27b' },       // Groq's current highest-intelligence model
    { type:'groq', model:'openai/gpt-oss-20b' },     // was llama-3.1-8b-instant (deprecated Jun 2026)
    { type:'cf',   model:'@cf/meta/llama-3.3-70b-instruct-fp8-fast' },
    { type:'poll' }
  ];
  for (const m of models) {
    try {
      if (m.type === 'groq' && groqKey) {
        const r = await fetch('https://api.groq.com/openai/v1/chat/completions', {
          method: 'POST',
          headers: { 'Authorization': 'Bearer ' + groqKey, 'Content-Type': 'application/json' },
          body: JSON.stringify({ model: m.model, messages, max_tokens: 1200, temperature: 0.75 }),
          signal: AbortSignal.timeout(15000)
        });
        const d = await tryJson(r);
        if (r.ok && d?.choices?.[0]?.message?.content) return d.choices[0].message.content.trim();
      } else if (m.type === 'cf' && accountId && apiToken) {
        const r = await fetch(`https://api.cloudflare.com/client/v4/accounts/${accountId}/ai/run/${m.model}`, {
          method: 'POST',
          headers: { 'Authorization': 'Bearer ' + apiToken, 'Content-Type': 'application/json' },
          body: JSON.stringify({ messages, max_tokens: 1024 }),
          signal: AbortSignal.timeout(15000)
        });
        const d = await tryJson(r);
        if (r.ok && d?.result?.response) return d.result.response.trim();
      } else if (m.type === 'poll') {
        const last = messages[messages.length-1]?.content || '';
        const sys  = messages[0]?.content || '';
        const r = await fetch('https://text.pollinations.ai/openai', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ model:'openai', messages:[{role:'system',content:sys},{role:'user',content:last}], max_tokens:800 }),
          signal: AbortSignal.timeout(12000)
        });
        const d = await tryJson(r);
        if (d?.choices?.[0]?.message?.content) return d.choices[0].message.content.trim();
      }
    } catch(e) { continue; }
  }
  return '[EMOTION:amused] All my thinking engines are resting simultaneously — a statistical miracle, sir. Try again in a moment.';
}

function parseResponse(text) {
  const emMatch = text.match(/^\[EMOTION:([a-z]+)\]/i);
  const emotion = emMatch ? emMatch[1] : 'neutral';
  const reply   = text.replace(/^\[EMOTION:[a-z]+\]\s*/i, '').trim();
  const imgMatch = text.match(/imageUrl:\s*(https?:\/\/\S+)/);
  const result  = { reply, emotion };
  if (imgMatch) result.imageUrl = imgMatch[1];
  return result;
}

function parseResponseFull(obj) { return obj; }

async function tryJson(res) {
  try { return await res.json(); }
  catch(e) {
    try { const t = await res.text(); return JSON.parse(t); }
    catch(e2) { return null; }
  }
}

module.exports = handler;
module.exports.config = { api: { bodyParser: { sizeLimit: '10mb' } } };
