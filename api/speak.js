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
    const { text = '' } = body;
    if (!text.trim()) return res.status(400).json({ error: 'No text provided' });

    const plain = text
      .replace(/```[\s\S]*?```/g,        'code block.')
      .replace(/`([^`]+)`/g,              '$1')
      .replace(/\*\*(.*?)\*\*/g,          '$1')
      .replace(/\*(.*?)\*/g,              '$1')
      .replace(/#{1,6}\s/g,               '')
      .replace(/\[([^\]]+)\]\([^)]+\)/g,  '$1')
      .replace(/(?m)^\s*[-*+]\s/gm,       '')
      .replace(/(?m)^\s*\d+\.\s/gm,       '')
      .replace(/\n{3,}/g,                 '\n\n')
      .trim()
      .slice(0, 5000);

    if (!plain) return res.status(400).json({ error: 'Empty text after cleanup' });

    const cfRes = await fetch(
      `https://api.cloudflare.com/client/v4/accounts/${ACCOUNT_ID}/ai/run/@cf/jaaari/kokoro-82m`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${API_TOKEN}`,
          'Content-Type':  'application/json'
        },
        body: JSON.stringify({
          prompt: plain,
          voice:  'bm_george'
        })
      }
    );

    if (!cfRes.ok) {
      const errText = await cfRes.text();
      return res.status(cfRes.status).json({ error: 'Cloudflare TTS error: ' + errText.slice(0, 300) });
    }

    const audioBuffer = Buffer.from(await cfRes.arrayBuffer());
    const contentType = cfRes.headers.get('content-type') || 'audio/wav';

    res.writeHead(200, {
      'Content-Type':   contentType,
      'Content-Length': audioBuffer.length
    });
    res.end(audioBuffer);

  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
};
