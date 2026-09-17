// ============================================================
// H·E·N·R·Y™ — Dev & Production Server (Port 3000)
// Hyperintelligence Engine Neural Reasoning Yield
// ============================================================

const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const HOST = '0.0.0.0';

// Import API handlers
const jarvisHandler = require('./api/jarvis.js');
const speakHandler = require('./api/speak.js');

function enhanceResponse(res) {
  res.status = function(code) {
    this.statusCode = code;
    return this;
  };
  res.json = function(data) {
    this.setHeader('Content-Type', 'application/json; charset=utf-8');
    this.end(JSON.stringify(data));
    return this;
  };
  res.send = function(data) {
    this.end(data);
    return this;
  };
}

const server = http.createServer(async (req, res) => {
  enhanceResponse(res);

  // Global CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Requested-With');

  if (req.method === 'OPTIONS') {
    res.statusCode = 204;
    return res.end();
  }

  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // Health check endpoint
  if (pathname === '/health') {
    return res.status(200).json({
      status: 'ok',
      app: 'HENRY',
      tagline: 'Hyperintelligence Engine Neural Reasoning Yield',
      geminiConfigured: Boolean(process.env.GEMINI_API_KEY),
      groqConfigured: Boolean(process.env.GROQ_API_KEY),
      cloudflareConfigured: Boolean(process.env.CF_ACCOUNT_ID && process.env.CF_API_TOKEN),
      timestamp: new Date().toISOString()
    });
  }

  // API Routes
  if (pathname === '/api/jarvis') {
    let raw = '';
    req.on('data', chunk => { raw += chunk; });
    req.on('end', async () => {
      try {
        req.body = raw ? JSON.parse(raw) : {};
      } catch (e) {
        req.body = raw;
      }
      try {
        await jarvisHandler(req, res);
      } catch (err) {
        console.error('Error in /api/jarvis handler:', err);
        if (!res.writableEnded) {
          res.status(500).json({ error: 'Internal server error in Jarvis handler', message: err.message });
        }
      }
    });
    return;
  }

  if (pathname === '/api/speak') {
    let raw = '';
    req.on('data', chunk => { raw += chunk; });
    req.on('end', async () => {
      try {
        req.body = raw ? JSON.parse(raw) : {};
      } catch (e) {
        req.body = raw;
      }
      try {
        await speakHandler(req, res);
      } catch (err) {
        console.error('Error in /api/speak handler:', err);
        if (!res.writableEnded) {
          res.status(500).json({ error: 'Internal server error in Speak handler', message: err.message });
        }
      }
    });
    return;
  }

  // Serve Single-Page Interactive Web Client for HENRY
  if (pathname === '/' || pathname === '/index.html') {
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    return res.status(200).end(getHenryWebHtml());
  }

  // 404 for other routes
  res.status(404).json({ error: 'Route not found', path: pathname });
});

server.listen(PORT, HOST, () => {
  console.log(`[HENRY Server] Running on http://${HOST}:${PORT}`);
  console.log(`[HENRY Server] Health check available at http://${HOST}:${PORT}/health`);
  console.log(`[HENRY Server] Gemini Vision & Reasoning Key configured: ${Boolean(process.env.GEMINI_API_KEY)}`);
});

// Handle graceful shutdown
process.on('SIGTERM', () => {
  console.log('[HENRY Server] SIGTERM received, shutting down...');
  server.close(() => process.exit(0));
});

process.on('SIGINT', () => {
  console.log('[HENRY Server] SIGINT received, shutting down...');
  server.close(() => process.exit(0));
});

function getHenryWebHtml() {
  return `<!DOCTYPE html>
<html lang="en" class="h-full bg-slate-950 text-slate-100">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>HENRY — Hyperintelligence Engine Neural Reasoning Yield</title>
  <meta name="description" content="AI Assistant specializing in multimodal vision, pop culture commentary, and witty Kiss Marry Date breakdowns." />
  <meta property="og:title" content="HENRY — Pop Culture Commentator & Vision Intelligence" />
  <meta property="og:description" content="Instant celebrity identification, multimodal vision breakdowns, and sharp pop culture commentary." />
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&family=Space+Grotesk:wght@500;700&display=swap" rel="stylesheet">
  <style>
    body { font-family: 'Plus Jakarta Sans', -apple-system, sans-serif; }
    .font-display { font-family: 'Space Grotesk', monospace, sans-serif; }
    .prose-henry h2 { font-size: 1.25rem; font-weight: 700; margin-top: 1.2rem; margin-bottom: 0.5rem; color: #38bdf8; }
    .prose-henry hr { border-color: rgba(56, 189, 248, 0.25); margin: 1.2rem 0; }
    .prose-henry ul { list-style-type: none; padding-left: 0; margin-top: 0.5rem; margin-bottom: 0.75rem; }
    .prose-henry li { position: relative; padding-left: 1.5rem; margin-bottom: 0.5rem; line-height: 1.6; }
    .prose-henry li::before { content: "•"; position: absolute; left: 0.4rem; color: #38bdf8; font-weight: bold; }
    .prose-henry p { margin-bottom: 0.8rem; line-height: 1.65; }
    .prose-henry strong { color: #f8fafc; font-weight: 600; }
    .custom-scroll::-webkit-scrollbar { width: 6px; height: 6px; }
    .custom-scroll::-webkit-scrollbar-track { background: #020617; }
    .custom-scroll::-webkit-scrollbar-thumb { background: #1e293b; border-radius: 9999px; }
    .custom-scroll::-webkit-scrollbar-thumb:hover { background: #38bdf8; }
  </style>
</head>
<body class="h-full flex flex-col antialiased bg-[#070b14] text-slate-100">

  <!-- Top Header Navigation -->
  <header id="header-bar" class="border-b border-slate-800/80 bg-slate-950/80 backdrop-blur sticky top-0 z-50">
    <div class="max-w-6xl mx-auto px-4 sm:px-6 py-3.5 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="relative flex items-center justify-center w-9 h-9 rounded-xl bg-gradient-to-br from-sky-500 to-indigo-600 text-white font-display font-bold shadow-lg shadow-sky-500/20">
          H
          <span class="absolute -bottom-0.5 -right-0.5 flex h-3 w-3">
            <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span class="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
          </span>
        </div>
        <div>
          <div class="flex items-center gap-2">
            <h1 class="font-display font-bold text-base tracking-wide text-white">H·E·N·R·Y™</h1>
            <span class="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-sky-950 text-sky-400 border border-sky-800/60 uppercase tracking-wider">v27 Core</span>
          </div>
          <p class="text-xs text-slate-400 font-medium">Hyperintelligence Engine Neural Reasoning Yield</p>
        </div>
      </div>

      <div class="flex items-center gap-2 sm:gap-3">
        <div class="hidden md:flex items-center gap-1.5 px-3 py-1 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-300">
          <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
          <span>Vision & Audio Engine Active</span>
        </div>
        <button id="btn-clear-chat" class="px-3 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-xs font-medium text-slate-300 transition-colors border border-slate-800">
          Clear Chat
        </button>
      </div>
    </div>
  </header>

  <!-- Main Container -->
  <main class="flex-1 flex flex-col max-w-6xl w-full mx-auto px-4 sm:px-6 py-4 overflow-hidden">
    
    <!-- Controls & Topic Presets Bar -->
    <div id="controls-bar" class="mb-3 flex flex-wrap items-center justify-between gap-2.5 pb-2 border-b border-slate-800/60 text-xs">
      <div class="flex items-center gap-1.5 overflow-x-auto py-1">
        <span class="text-slate-400 font-medium whitespace-nowrap">Mode:</span>
        <button class="mode-btn px-2.5 py-1 rounded-md bg-sky-600 text-white font-medium shadow-sm" data-mode="witty">Witty / Pop Culture</button>
        <button class="mode-btn px-2.5 py-1 rounded-md bg-slate-900 text-slate-300 hover:bg-slate-800 border border-slate-800" data-mode="balanced">Balanced</button>
        <button class="mode-btn px-2.5 py-1 rounded-md bg-slate-900 text-slate-300 hover:bg-slate-800 border border-slate-800" data-mode="detailed">Detailed</button>
      </div>

      <div class="flex items-center gap-1.5 overflow-x-auto py-1">
        <span class="text-slate-400 font-medium whitespace-nowrap">Wit Level:</span>
        <button class="intensity-btn px-2 py-0.5 rounded-md bg-slate-900 text-slate-300 hover:bg-slate-800 border border-slate-800" data-intensity="NORMAL">Normal</button>
        <button class="intensity-btn px-2 py-0.5 rounded-md bg-pink-950/80 text-pink-300 border border-pink-700/60 font-semibold" data-intensity="SAVAGE">Savage</button>
        <button class="intensity-btn px-2 py-0.5 rounded-md bg-slate-900 text-slate-300 hover:bg-slate-800 border border-slate-800" data-intensity="BARDAGULAN">Bardagulan</button>
      </div>
    </div>

    <!-- Quick Prompts Pill Row -->
    <div id="quick-prompts-row" class="flex items-center gap-2 overflow-x-auto pb-2.5 mb-2 custom-scroll text-xs">
      <button class="quick-pill whitespace-nowrap px-3 py-1.5 rounded-full bg-slate-900 hover:bg-slate-800 text-sky-400 border border-sky-900/60 flex items-center gap-1.5 font-medium transition-all"
        data-prompt="Kiss, Marry or Date?">
        <span>💍 🌹 💋</span> Kiss, Marry, Date
      </button>
      <button class="quick-pill whitespace-nowrap px-3 py-1.5 rounded-full bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 flex items-center gap-1.5 transition-all"
        data-prompt="Analyse this image and describe what you see in detail.">
        <span>🔍</span> Detailed Vision Analysis
      </button>
      <button class="quick-pill whitespace-nowrap px-3 py-1.5 rounded-full bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 flex items-center gap-1.5 transition-all"
        data-prompt="Enrique Gil, Daniel Padilla, and James Reid: Kiss, Marry or Date?">
        <span>🎭</span> Celebrity Breakdown
      </button>
      <button class="quick-pill whitespace-nowrap px-3 py-1.5 rounded-full bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 flex items-center gap-1.5 transition-all"
        data-prompt="Give me a witty roast of modern smartphone designs.">
        <span>⚡</span> Pop Culture Roast
      </button>
    </div>

    <!-- Chat Messages Scroll View -->
    <div id="chat-scroller" class="flex-1 overflow-y-auto custom-scroll space-y-4 pr-1 pb-4">
      
      <!-- Welcome Message -->
      <div id="welcome-message" class="flex gap-3 max-w-3xl">
        <div class="flex-shrink-0 w-8 h-8 rounded-lg bg-sky-600/90 text-white font-display font-bold flex items-center justify-center text-sm shadow">
          H
        </div>
        <div class="flex-1 bg-slate-900/90 border border-slate-800/80 rounded-2xl rounded-tl-sm px-4 py-3.5 shadow-sm">
          <div class="flex items-center justify-between mb-1.5">
            <span class="text-xs font-semibold text-sky-400 font-display">H·E·N·R·Y</span>
            <span class="text-[11px] text-slate-500">Live Assistant</span>
          </div>
          <div class="text-sm text-slate-200 leading-relaxed prose-henry">
            <p>Ready to dissect pop culture, run celebrity <strong>Kiss, Marry, Date</strong> rounds, or analyze your photos with razor-sharp multimodal vision.</p>
            <p class="text-xs text-slate-400">Upload up to three images or name three personalities to activate the two-step breakdown.</p>
          </div>
        </div>
      </div>

    </div>

    <!-- Image Attachment Preview Bar -->
    <div id="image-previews-container" class="hidden flex items-center gap-3 py-2 px-3 bg-slate-900/90 border border-sky-900/50 rounded-xl mb-2">
      <div class="text-xs text-slate-400 font-medium flex items-center gap-1.5">
        <span>📷 Attached Photos (<span id="photo-count-badge">0</span>/3):</span>
      </div>
      <div id="preview-thumbnails-list" class="flex items-center gap-2"></div>
      <button id="btn-clear-all-images" class="ml-auto text-xs text-rose-400 hover:text-rose-300 font-medium">Clear All</button>
    </div>

    <!-- Input Form & Controls -->
    <div id="input-container" class="bg-slate-950 border border-slate-800 rounded-2xl p-2.5 shadow-xl">
      <form id="chat-form" class="flex items-end gap-2">
        
        <!-- Upload button -->
        <label id="upload-label" class="cursor-pointer flex-shrink-0 p-2.5 text-slate-400 hover:text-sky-400 hover:bg-slate-900 rounded-xl transition-colors" title="Attach up to 3 images">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
          </svg>
          <input type="file" id="file-input" multiple accept="image/*" class="hidden" />
        </label>

        <!-- Text area -->
        <div class="flex-1">
          <textarea id="prompt-input" rows="1" placeholder="Ask HENRY anything or play Kiss, Marry, Date..."
            class="w-full bg-transparent border-0 resize-none text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-0 max-h-36 py-1.5 leading-relaxed"></textarea>
        </div>

        <!-- Send button -->
        <button type="submit" id="btn-send"
          class="flex-shrink-0 px-4 py-2.5 bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-medium text-sm rounded-xl transition-all shadow-md shadow-sky-500/20 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1.5">
          <span>Send</span>
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path>
          </svg>
        </button>
      </form>
    </div>

  </main>

  <script>
    const chatScroller = document.getElementById('chat-scroller');
    const chatForm = document.getElementById('chat-form');
    const promptInput = document.getElementById('prompt-input');
    const fileInput = document.getElementById('file-input');
    const previewsContainer = document.getElementById('image-previews-container');
    const thumbnailsList = document.getElementById('preview-thumbnails-list');
    const photoCountBadge = document.getElementById('photo-count-badge');
    const btnClearImages = document.getElementById('btn-clear-all-images');
    const btnClearChat = document.getElementById('btn-clear-chat');
    const btnSend = document.getElementById('btn-send');

    let currentMode = 'witty';
    let currentIntensity = 'SAVAGE';
    let attachedImagesBase64 = [];
    let conversationHistory = [];

    // Mode toggles
    document.querySelectorAll('.mode-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.mode-btn').forEach(b => {
          b.classList.remove('bg-sky-600', 'text-white');
          b.classList.add('bg-slate-900', 'text-slate-300');
        });
        btn.classList.remove('bg-slate-900', 'text-slate-300');
        btn.classList.add('bg-sky-600', 'text-white');
        currentMode = btn.getAttribute('data-mode');
      });
    });

    // Intensity toggles
    document.querySelectorAll('.intensity-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.intensity-btn').forEach(b => {
          b.classList.remove('bg-pink-950/80', 'text-pink-300', 'border-pink-700/60', 'font-semibold');
          b.classList.add('bg-slate-900', 'text-slate-300', 'border-slate-800');
        });
        btn.classList.remove('bg-slate-900', 'text-slate-300', 'border-slate-800');
        btn.classList.add('bg-pink-950/80', 'text-pink-300', 'border-pink-700/60', 'font-semibold');
        currentIntensity = btn.getAttribute('data-intensity');
      });
    });

    // Quick pills
    document.querySelectorAll('.quick-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        promptInput.value = pill.getAttribute('data-prompt');
        promptInput.focus();
      });
    });

    // Auto-resize textarea
    promptInput.addEventListener('input', () => {
      promptInput.style.height = 'auto';
      promptInput.style.height = Math.min(promptInput.scrollHeight, 140) + 'px';
    });

    // File attachments
    fileInput.addEventListener('change', async (e) => {
      const files = Array.from(e.target.files);
      if (!files.length) return;
      await processFiles(files);
      fileInput.value = '';
    });

    async function processFiles(files) {
      for (const file of files) {
        if (attachedImagesBase64.length >= 3) break;
        if (!file.type || !file.type.startsWith('image/')) continue;
        const b64 = await fileToBase64(file);
        attachedImagesBase64.push(b64);
      }
      renderImagePreviews();
    }

    // Drag-and-drop support on entire window and input container
    ['dragenter', 'dragover'].forEach(eventName => {
      window.addEventListener(eventName, (e) => {
        e.preventDefault();
        e.stopPropagation();
      }, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
      window.addEventListener(eventName, (e) => {
        e.preventDefault();
        e.stopPropagation();
      }, false);
    });

    window.addEventListener('drop', async (e) => {
      const dt = e.dataTransfer;
      if (dt && dt.files && dt.files.length > 0) {
        await processFiles(Array.from(dt.files));
      }
    });

    // Clipboard paste support (paste screenshot directly with Ctrl+V / Cmd+V)
    window.addEventListener('paste', async (e) => {
      const items = (e.clipboardData || window.clipboardData)?.items;
      if (!items) return;
      const imageFiles = [];
      for (let i = 0; i < items.length; i++) {
        if (items[i].type && items[i].type.startsWith('image/')) {
          const blob = items[i].getAsFile();
          if (blob) imageFiles.push(blob);
        }
      }
      if (imageFiles.length > 0) {
        e.preventDefault();
        await processFiles(imageFiles);
      }
    });

    function fileToBase64(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
    }

    function renderImagePreviews() {
      if (attachedImagesBase64.length === 0) {
        previewsContainer.classList.add('hidden');
        return;
      }
      previewsContainer.classList.remove('hidden');
      photoCountBadge.textContent = attachedImagesBase64.length;
      thumbnailsList.innerHTML = '';

      attachedImagesBase64.forEach((b64, idx) => {
        const thumbWrap = document.createElement('div');
        thumbWrap.className = 'relative w-12 h-12 rounded-lg overflow-hidden border border-slate-700 bg-slate-950 flex-shrink-0';
        thumbWrap.innerHTML = \`
          <img src="\${b64}" class="w-full h-full object-cover" />
          <button data-idx="\${idx}" class="remove-img-btn absolute top-0.5 right-0.5 w-4 h-4 rounded-full bg-slate-900/90 text-slate-300 hover:text-white flex items-center justify-center text-[10px] leading-none">&times;</button>
        \`;
        thumbnailsList.appendChild(thumbWrap);
      });

      document.querySelectorAll('.remove-img-btn').forEach(b => {
        b.addEventListener('click', (ev) => {
          const idx = parseInt(ev.target.getAttribute('data-idx'), 10);
          attachedImagesBase64.splice(idx, 1);
          renderImagePreviews();
        });
      });
    }

    btnClearImages.addEventListener('click', () => {
      attachedImagesBase64 = [];
      renderImagePreviews();
    });

    btnClearChat.addEventListener('click', () => {
      conversationHistory = [];
      chatScroller.innerHTML = '';
    });

    // Form submit
    chatForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const text = promptInput.value.trim();
      if (!text && attachedImagesBase64.length === 0) return;

      const currentImages = [...attachedImagesBase64];
      promptInput.value = '';
      promptInput.style.height = 'auto';
      attachedImagesBase64 = [];
      renderImagePreviews();

      // Append User message to UI
      const displayText = text || (currentImages.length > 0 ? 'Analyze attached image' : '');
      appendUserMessage(displayText, currentImages);

      // Add to conversation state
      conversationHistory.push({
        role: 'user',
        text: text || 'Inspect and analyze the attached photo/screenshot in detail.',
        images: currentImages
      });

      // Show typing indicator
      const typingElem = showTypingIndicator();

      btnSend.disabled = true;

      try {
        const res = await fetch('/api/jarvis', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            messages: conversationHistory,
            imagesBase64: currentImages,
            imageBase64: currentImages[0] || null,
            responseMode: currentMode,
            wittyIntensity: currentIntensity,
            wittyPersonality: 'SAVAGE'
          })
        });

        const data = await res.json();
        typingElem.remove();

        const reply = data.reply || 'No response received.';
        appendAssistantMessage(reply);

        conversationHistory.push({
          role: 'assistant',
          text: reply
        });

      } catch (err) {
        typingElem.remove();
        appendAssistantMessage('⚠️ Connection error: ' + err.message);
      } finally {
        btnSend.disabled = false;
      }
    });

    function appendUserMessage(text, images) {
      const msgDiv = document.createElement('div');
      msgDiv.className = 'flex justify-end gap-3 max-w-3xl ml-auto';

      let imgHtml = '';
      if (images && images.length > 0) {
        imgHtml = '<div class="flex flex-wrap gap-2 mb-2">';
        images.forEach(img => {
          imgHtml += \`<img src="\${img}" class="w-20 h-20 object-cover rounded-xl border border-slate-700 shadow-sm" />\`;
        });
        imgHtml += '</div>';
      }

      msgDiv.innerHTML = \`
        <div class="bg-gradient-to-r from-sky-600 to-indigo-600 text-white rounded-2xl rounded-tr-sm px-4 py-3 shadow-md">
          \${imgHtml}
          \${text ? \`<p class="text-sm leading-relaxed whitespace-pre-wrap">\${escapeHtml(text)}</p>\` : ''}
        </div>
      \`;
      chatScroller.appendChild(msgDiv);
      scrollToBottom();
    }

    function appendAssistantMessage(text) {
      const msgDiv = document.createElement('div');
      msgDiv.className = 'flex gap-3 max-w-3xl';

      const formatted = renderMarkdown(text);

      msgDiv.innerHTML = \`
        <div class="flex-shrink-0 w-8 h-8 rounded-lg bg-sky-600/90 text-white font-display font-bold flex items-center justify-center text-sm shadow">
          H
        </div>
        <div class="flex-1 bg-slate-900/90 border border-slate-800/80 rounded-2xl rounded-tl-sm px-4 py-3.5 shadow-sm">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs font-semibold text-sky-400 font-display">H·E·N·R·Y</span>
            <button class="btn-speak text-xs text-slate-400 hover:text-sky-400 flex items-center gap-1 transition-colors" title="Read aloud">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"></path></svg>
              <span>Play</span>
            </button>
          </div>
          <div class="text-sm text-slate-200 leading-relaxed prose-henry">\${formatted}</div>
        </div>
      \`;

      // Wire TTS button
      const speakBtn = msgDiv.querySelector('.btn-speak');
      speakBtn.addEventListener('click', () => playAudioTTS(text, speakBtn));

      chatScroller.appendChild(msgDiv);
      scrollToBottom();
    }

    function showTypingIndicator() {
      const typingDiv = document.createElement('div');
      typingDiv.className = 'flex gap-3 max-w-3xl';
      typingDiv.innerHTML = \`
        <div class="flex-shrink-0 w-8 h-8 rounded-lg bg-sky-600/90 text-white font-display font-bold flex items-center justify-center text-sm shadow">
          H
        </div>
        <div class="bg-slate-900/90 border border-slate-800/80 rounded-2xl rounded-tl-sm px-4 py-3 flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-sky-400 animate-pulse"></span>
          <span class="w-2 h-2 rounded-full bg-sky-400 animate-pulse delay-150"></span>
          <span class="w-2 h-2 rounded-full bg-sky-400 animate-pulse delay-300"></span>
          <span class="text-xs text-slate-400 font-medium ml-1">Analyzing multimodal patterns...</span>
        </div>
      \`;
      chatScroller.appendChild(typingDiv);
      scrollToBottom();
      return typingDiv;
    }

    async function playAudioTTS(text, btn) {
      const origText = btn.innerHTML;
      btn.innerHTML = '<span>Loading audio...</span>';
      try {
        const res = await fetch('/api/speak', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ text: text.slice(0, 800) })
        });
        if (!res.ok) throw new Error('Audio generation failed');
        const blob = await res.blob();
        const audio = new Audio(URL.createObjectURL(blob));
        audio.play();
        btn.innerHTML = '<span>Playing...</span>';
        audio.onended = () => { btn.innerHTML = origText; };
      } catch (e) {
        if ('speechSynthesis' in window) {
          window.speechSynthesis.cancel();
          const cleanSpeech = text
            .replace(/[#*\\x60_~]/g, '')
            .replace(/💍|🌹|💋|🛸|🌡|📊|💧|💨|👁|☀️/g, '')
            .slice(0, 500);
          const u = new SpeechSynthesisUtterance(cleanSpeech);
          u.rate = 1.05;
          u.pitch = 1.0;
          btn.innerHTML = '<span>Speaking...</span>';
          u.onend = () => { btn.innerHTML = origText; };
          u.onerror = () => { btn.innerHTML = origText; };
          window.speechSynthesis.speak(u);
          return;
        }
        console.error(e);
        btn.innerHTML = '<span class="text-rose-400">Audio error</span>';
        setTimeout(() => { btn.innerHTML = origText; }, 2000);
      }
    }

    function renderMarkdown(md) {
      if (!md) return '';
      let s = escapeHtml(md);

      // Headers (e.g. ### 💍 The Case for Marrying or ## 💋)
      s = s.replace(/^###\\s*(.+)$/gm, '<h3 class="text-base font-bold text-sky-400 mt-3 mb-1">$1</h3>');
      s = s.replace(/^##\\s*(.+)$/gm, '<h2 class="text-lg font-bold text-sky-400 mt-4 mb-1.5">$1</h2>');
      s = s.replace(/^#\\s*(.+)$/gm, '<h1 class="text-xl font-bold text-white mt-4 mb-2">$1</h1>');

      // Horizontal rules
      s = s.replace(/^\\*\\*\\*$/gm, '<hr class="border-sky-800/40 my-3" />');
      s = s.replace(/^---$/gm, '<hr class="border-sky-800/40 my-3" />');

      // Bold & italics
      s = s.replace(/\\*\\*(.+?)\\*\\*/g, '<strong>$1</strong>');
      s = s.replace(/\\*(.+?)\\*/g, '<em>$1</em>');

      // Bullet points
      s = s.replace(/^[-*•]\\s+(.+)$/gm, '<li class="my-1">$1</li>');
      s = s.replace(/((?:<li class="my-1">.*?<\\/li>\\n?)+)/g, '<ul class="list-none pl-3 my-2 border-l border-sky-900/40">$1</ul>');

      // Paragraph breaks
      s = s.replace(/\\n\\n+/g, '</p><p class="mt-2.5">');
      s = '<p>' + s + '</p>';
      s = s.replace(/<p><\\/p>/g, '');

      return s;
    }

    function escapeHtml(str) {
      return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }

    function scrollToBottom() {
      chatScroller.scrollTop = chatScroller.scrollHeight;
    }
  </script>
</body>
</html>
`;
}
