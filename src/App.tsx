import React, { useState, useEffect, useRef } from 'react';
import {
  Brain, Rocket, Globe, TrendingUp, FlaskConical, Eye, Wrench, Settings,
  Mic, MicOff, Send, Image as ImageIcon, Volume2, VolumeX, Copy, Check,
  Trash2, Sparkles, RefreshCw, ChevronRight, X
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { OrbVisualizer } from './components/OrbVisualizer';
import { BrainHub } from './components/BrainHub';
import { SpaceHub } from './components/SpaceHub';
import { EarthRadarHub } from './components/EarthRadarHub';
import { MarketsHub } from './components/MarketsHub';
import { ChemistryHub } from './components/ChemistryHub';
import { VisionScannerHub } from './components/VisionScannerHub';
import { ToolsSuiteHub } from './components/ToolsSuiteHub';
import { SettingsModal } from './components/SettingsModal';
import { ChatMessage, OrbState, EmotionType, ResponseMode, UserProfile } from './types';
import confetti from 'canvas-confetti';

export function App() {
  // --- Assistant Core State ---
  const [orbState, setOrbState] = useState<OrbState>('IDLE');
  const [currentEmotion, setCurrentEmotion] = useState<EmotionType>('neutral');
  const [activeHub, setActiveHub] = useState<string | null>(null);

  // --- Configuration & Identity ---
  const [responseMode, setResponseMode] = useState<ResponseMode>('balanced');
  const [userProfile, setUserProfile] = useState<UserProfile>(() => {
    try {
      const saved = localStorage.getItem('henry_profile');
      return saved ? JSON.parse(saved) : { name: 'Sir', city: 'Dubai', job: 'Engineer' };
    } catch {
      return { name: 'Sir', city: 'Dubai', job: 'Engineer' };
    }
  });

  const [memories, setMemories] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem('henry_memories');
      return saved ? JSON.parse(saved) : [
        'User is an innovator with high technical aptitude',
        'Prioritizes precision, scientific evidence, and elegant execution',
        'Primary timezone coordinate: UTC+4 (Dubai)'
      ];
    } catch {
      return [];
    }
  });

  // --- Voice & Speech State ---
  const [isListening, setIsListening] = useState<boolean>(false);
  const [autoTts, setAutoTts] = useState<boolean>(true);
  const [speechRate, setSpeechRate] = useState<number>(1.0);
  const [selectedVoice, setSelectedVoice] = useState<string>('');
  const [availableVoices, setAvailableVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [isSpeaking, setIsSpeaking] = useState<boolean>(false);
  const speechRef = useRef<SpeechRecognition | null>(null);

  // --- Chat State ---
  const [messages, setMessages] = useState<ChatMessage[]>(() => {
    try {
      const saved = localStorage.getItem('henry_chat_history');
      if (saved) return JSON.parse(saved);
    } catch {}
    return [
      {
        id: 'init-1',
        role: 'assistant',
        text: `Greetings. I am **H.E.N.R.Y.** — Hyperintelligence Engine Neural Reasoning Yield.\n\nAll 8 cognitive lobes, orbital telemetry feeds, and real-time planetary sensors are fully synchronized. How may I direct our objectives today, sir?`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        emotion: 'warm'
      }
    ];
  });

  const [inputVal, setInputVal] = useState<string>('');
  const [attachedImage, setAttachedImage] = useState<string | null>(null);
  const [isSending, setIsSending] = useState<boolean>(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const chatEndRef = useRef<HTMLDivElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Persist storage
  useEffect(() => {
    localStorage.setItem('henry_chat_history', JSON.stringify(messages));
  }, [messages]);

  useEffect(() => {
    localStorage.setItem('henry_memories', JSON.stringify(memories));
  }, [memories]);

  useEffect(() => {
    localStorage.setItem('henry_profile', JSON.stringify(userProfile));
  }, [userProfile]);

  // Scroll to bottom on new message
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, orbState]);

  // Load available speech synthesis voices
  useEffect(() => {
    const loadVoices = () => {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        const v = window.speechSynthesis.getVoices();
        setAvailableVoices(v);
        if (v.length && !selectedVoice) {
          const defaultVoice = v.find(x => x.lang.startsWith('en') && (x.name.includes('Natural') || x.name.includes('Google') || x.name.includes('David') || x.name.includes('George'))) || v[0];
          setSelectedVoice(defaultVoice.name);
        }
      }
    };

    loadVoices();
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.onvoiceschanged = loadVoices;
    }
  }, [selectedVoice]);

  // Speech Recognition Setup (Web Speech API)
  useEffect(() => {
    if (typeof window !== 'undefined' && ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)) {
      const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      const rec = new SpeechRecognition();
      rec.continuous = false;
      rec.interimResults = true;
      rec.lang = 'en-US';

      rec.onstart = () => {
        setIsListening(true);
        setOrbState('LISTENING');
      };

      rec.onresult = (event: any) => {
        let transcript = '';
        for (let i = event.resultIndex; i < event.results.length; ++i) {
          transcript += event.results[i][0].transcript;
        }
        setInputVal(transcript);
      };

      rec.onerror = (e: any) => {
        console.warn('Speech recognition error:', e);
        setIsListening(false);
        setOrbState('IDLE');
      };

      rec.onend = () => {
        setIsListening(false);
        if (orbState === 'LISTENING') setOrbState('IDLE');
      };

      speechRef.current = rec;
    }
  }, [orbState]);

  const toggleVoiceInput = () => {
    if (isListening) {
      speechRef.current?.stop();
      setIsListening(false);
      setOrbState('IDLE');
    } else {
      try {
        speechRef.current?.start();
      } catch (err) {
        console.warn('Cannot start recognition:', err);
      }
    }
  };

  // Text-To-Speech engine
  const speakText = (text: string) => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();

    const clean = text
      .replace(/```[\s\S]*?```/g, 'code block.')
      .replace(/`([^`]+)`/g, '$1')
      .replace(/\*\*(.*?)\*\*/g, '$1')
      .replace(/\*(.*?)\*/g, '$1')
      .replace(/#{1,6}\s/g, '')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .trim();

    if (!clean) return;

    const utterance = new SpeechSynthesisUtterance(clean);
    if (selectedVoice) {
      const voiceObj = availableVoices.find(v => v.name === selectedVoice);
      if (voiceObj) utterance.voice = voiceObj;
    }
    utterance.rate = speechRate;
    utterance.pitch = 1.0;

    utterance.onstart = () => {
      setIsSpeaking(true);
      setOrbState('SPEAKING');
    };

    utterance.onend = () => {
      setIsSpeaking(false);
      setOrbState('IDLE');
    };

    utterance.onerror = () => {
      setIsSpeaking(false);
      setOrbState('IDLE');
    };

    window.speechSynthesis.speak(utterance);
  };

  const stopSpeaking = () => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
      if (orbState === 'SPEAKING') setOrbState('IDLE');
    }
  };

  // Send Message Handler
  const handleSendMessage = async (customPrompt?: string, customImage?: string) => {
    const textToSend = (customPrompt || inputVal).trim();
    const imageToSend = customImage || attachedImage;

    if (!textToSend && !imageToSend) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      text: textToSend,
      imageBase64: imageToSend || undefined,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    const newHistory = [...messages, userMsg];
    setMessages(newHistory);
    setInputVal('');
    setAttachedImage(null);
    setIsSending(true);
    setOrbState('THINKING');

    try {
      const res = await fetch('/api/jarvis', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages: newHistory.map(m => ({
            role: m.role,
            content: m.text,
            imageBase64: m.imageBase64
          })),
          imageBase64: imageToSend || undefined,
          responseMode,
          userProfile,
          memoryFacts: memories
        })
      });

      const data = await res.json();
      const emotion: EmotionType = (data.emotion as EmotionType) || 'neutral';
      setCurrentEmotion(emotion);

      const aiMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        text: data.reply || 'System acknowledged.',
        imageUrl: data.imageUrl,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        emotion
      };

      setMessages([...newHistory, aiMsg]);
      setOrbState('IDLE');

      if (autoTts && data.reply) {
        speakText(data.reply);
      }
    } catch (e: any) {
      const errorMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        text: 'A neural matrix exception occurred. Please verify telemetry connection and try again.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        emotion: 'concerned'
      };
      setMessages([...newHistory, errorMsg]);
      setOrbState('IDLE');
    } finally {
      setIsSending(false);
    }
  };

  const copyMessage = (id: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  // Image attachment
  const handleAttachImage = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setAttachedImage(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  // Memory management
  const addMemory = (mem: string) => {
    setMemories([mem, ...memories]);
  };

  const deleteMemory = (index: number) => {
    setMemories(memories.filter((_, i) => i !== index));
  };

  const clearAllData = () => {
    localStorage.removeItem('henry_chat_history');
    localStorage.removeItem('henry_memories');
    localStorage.removeItem('henry_tasks');
    localStorage.removeItem('henry_expenses');
    localStorage.removeItem('henry_vault');
    localStorage.removeItem('henry_habits');
    localStorage.removeItem('henry_np_score');
    localStorage.removeItem('henry_np_streak');
    setMessages([
      {
        id: 'init-reset',
        role: 'assistant',
        text: 'Neural memory banks sanitized. H.E.N.R.Y. is recalibrated and ready.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        emotion: 'warm'
      }
    ]);
    setMemories([]);
  };

  // Suggestion Chips
  const SUGGESTIONS = [
    { label: '🛰️ ISS Telemetry', prompt: 'Where is the ISS right now and what is its orbital speed?' },
    { label: '☄️ Asteroid Watch', prompt: 'What are the closest near-Earth asteroids passing this week?' },
    { label: '🌋 Earth Radar', prompt: 'Provide a real-time summary of significant seismic activity and global earthquakes.' },
    { label: '🎨 Generate Artwork', prompt: 'Generate an image of a cybernetic neural quantum core orbiting Saturn' },
    { label: '📈 Markets Pulse', prompt: 'Give me the current prices and trends for NVDA, TSLA, and Bitcoin.' },
    { label: '🧠 Brain Training', prompt: 'Let’s start a cognitive neuroplasticity challenge to test my working memory.' }
  ];

  return (
    <div className="flex flex-col h-screen w-screen bg-[#020C1B] text-[#e2f1ff] overflow-hidden select-none">
      {/* 1. TOP SCI-FI HUD STATUS BAR */}
      <header className="flex items-center justify-between px-4 lg:px-6 py-2.5 border-b border-cyan-500/20 bg-[#031326]/70 backdrop-blur-md z-20 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-cyan-400 animate-pulse glow-cyan" />
          <div>
            <h1 className="text-base font-bold font-tech tracking-wider text-cyan-300 flex items-center gap-2">
              H.E.N.R.Y. <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 font-mono-hud border border-cyan-500/30">v23.0 PRO</span>
            </h1>
          </div>
        </div>

        {/* Global Navigation Hub Buttons */}
        <div className="hidden md:flex items-center gap-1 p-1 bg-[#010814] rounded-xl border border-cyan-500/20">
          <button
            onClick={() => setActiveHub('brain')}
            className={`px-3 py-1 rounded-lg text-xs font-mono-hud flex items-center gap-1.5 transition-all ${
              activeHub === 'brain' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 glow-cyan' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Brain className="w-3.5 h-3.5 text-cyan-400" /> Brain Hub
          </button>
          <button
            onClick={() => setActiveHub('space')}
            className={`px-3 py-1 rounded-lg text-xs font-mono-hud flex items-center gap-1.5 transition-all ${
              activeHub === 'space' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 glow-cyan' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Rocket className="w-3.5 h-3.5 text-cyan-400" /> Space
          </button>
          <button
            onClick={() => setActiveHub('earth')}
            className={`px-3 py-1 rounded-lg text-xs font-mono-hud flex items-center gap-1.5 transition-all ${
              activeHub === 'earth' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 glow-cyan' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Globe className="w-3.5 h-3.5 text-cyan-400" /> Radar
          </button>
          <button
            onClick={() => setActiveHub('markets')}
            className={`px-3 py-1 rounded-lg text-xs font-mono-hud flex items-center gap-1.5 transition-all ${
              activeHub === 'markets' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 glow-cyan' : 'text-slate-400 hover:text-white'
            }`}
          >
            <TrendingUp className="w-3.5 h-3.5 text-cyan-400" /> Markets
          </button>
          <button
            onClick={() => setActiveHub('chemistry')}
            className={`px-3 py-1 rounded-lg text-xs font-mono-hud flex items-center gap-1.5 transition-all ${
              activeHub === 'chemistry' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 glow-cyan' : 'text-slate-400 hover:text-white'
            }`}
          >
            <FlaskConical className="w-3.5 h-3.5 text-cyan-400" /> Chemistry
          </button>
          <button
            onClick={() => setActiveHub('vision')}
            className={`px-3 py-1 rounded-lg text-xs font-mono-hud flex items-center gap-1.5 transition-all ${
              activeHub === 'vision' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 glow-cyan' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Eye className="w-3.5 h-3.5 text-cyan-400" /> Vision
          </button>
          <button
            onClick={() => setActiveHub('tools')}
            className={`px-3 py-1 rounded-lg text-xs font-mono-hud flex items-center gap-1.5 transition-all ${
              activeHub === 'tools' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 glow-cyan' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Wrench className="w-3.5 h-3.5 text-cyan-400" /> Utilities
          </button>
        </div>

        {/* Right utility buttons */}
        <div className="flex items-center gap-2">
          {isSpeaking && (
            <button
              onClick={stopSpeaking}
              className="px-2.5 py-1 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-xs font-mono-hud flex items-center gap-1 animate-pulse"
              title="Stop TTS"
            >
              <VolumeX className="w-3.5 h-3.5" /> Mute
            </button>
          )}

          <button
            onClick={() => setActiveHub('settings')}
            className="p-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-cyan-400 hover:text-white hover:border-cyan-400 transition-colors"
            title="Configure System"
          >
            <Settings className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Mobile Hub Navigation Strip */}
      <div className="md:hidden flex items-center gap-1.5 px-3 py-2 border-b border-cyan-500/10 bg-[#020b18] overflow-x-auto shrink-0 scrollbar-none">
        <button onClick={() => setActiveHub('brain')} className="px-2.5 py-1 rounded-lg bg-[#031427] border border-cyan-500/20 text-[11px] font-mono-hud text-cyan-300 whitespace-nowrap">🧠 Brain</button>
        <button onClick={() => setActiveHub('space')} className="px-2.5 py-1 rounded-lg bg-[#031427] border border-cyan-500/20 text-[11px] font-mono-hud text-cyan-300 whitespace-nowrap">🚀 Space</button>
        <button onClick={() => setActiveHub('earth')} className="px-2.5 py-1 rounded-lg bg-[#031427] border border-cyan-500/20 text-[11px] font-mono-hud text-cyan-300 whitespace-nowrap">🌍 Radar</button>
        <button onClick={() => setActiveHub('markets')} className="px-2.5 py-1 rounded-lg bg-[#031427] border border-cyan-500/20 text-[11px] font-mono-hud text-cyan-300 whitespace-nowrap">📈 Markets</button>
        <button onClick={() => setActiveHub('chemistry')} className="px-2.5 py-1 rounded-lg bg-[#031427] border border-cyan-500/20 text-[11px] font-mono-hud text-cyan-300 whitespace-nowrap">🧪 Chemistry</button>
        <button onClick={() => setActiveHub('vision')} className="px-2.5 py-1 rounded-lg bg-[#031427] border border-cyan-500/20 text-[11px] font-mono-hud text-cyan-300 whitespace-nowrap">👁 Vision</button>
        <button onClick={() => setActiveHub('tools')} className="px-2.5 py-1 rounded-lg bg-[#031427] border border-cyan-500/20 text-[11px] font-mono-hud text-cyan-300 whitespace-nowrap">🛠 Utilities</button>
      </div>

      {/* 2. MAIN VIEWPORT: SPLIT HUD & CHAT */}
      <div className="flex-1 flex flex-col md:flex-row overflow-hidden relative">
        {/* Left / Top: Interactive Reactor Orb & Status Matrix */}
        <div className="md:w-80 lg:w-96 p-4 border-b md:border-b-0 md:border-r border-cyan-500/20 bg-[#020d1c]/80 flex flex-col items-center justify-between shrink-0 overflow-y-auto">
          <div className="flex flex-col items-center w-full">
            <OrbVisualizer
              state={orbState}
              emotion={currentEmotion}
              onClick={toggleVoiceInput}
              size={200}
            />

            {/* Sub-status badges */}
            <div className="grid grid-cols-2 gap-2 w-full mt-4 text-[11px] font-mono-hud">
              <div className="p-2.5 rounded-xl bg-[#010814] border border-cyan-500/20 text-center">
                <span className="text-slate-400 block text-[9px]">MODE</span>
                <span className="text-cyan-300 font-bold capitalize">{responseMode}</span>
              </div>
              <div className="p-2.5 rounded-xl bg-[#010814] border border-cyan-500/20 text-center">
                <span className="text-slate-400 block text-[9px]">EMOTION</span>
                <span className="text-amber-300 font-bold capitalize">{currentEmotion}</span>
              </div>
            </div>

            {/* Quick action triggers */}
            <div className="w-full mt-4 space-y-1.5">
              <span className="text-[10px] font-mono-hud text-cyan-400/80 tracking-wider block">NEURAL DIRECTIVES</span>
              {SUGGESTIONS.slice(0, 4).map((s, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSendMessage(s.prompt)}
                  className="w-full p-2 rounded-xl bg-[#010814] border border-cyan-500/20 hover:border-cyan-500/50 text-left text-[11px] font-mono-hud text-slate-300 flex items-center justify-between transition-colors"
                >
                  <span className="truncate">{s.label}</span>
                  <ChevronRight className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                </button>
              ))}
            </div>
          </div>

          {/* Bottom Telemetry Ticker */}
          <div className="w-full pt-4 mt-4 border-t border-cyan-500/10 text-[10px] font-mono-hud text-slate-500 flex items-center justify-between">
            <span>MEM: {memories.length} FACTS</span>
            <span>SECURE AES-256</span>
          </div>
        </div>

        {/* Right: Conversational Stream & Input Matrix */}
        <div className="flex-1 flex flex-col h-full overflow-hidden bg-grid-pattern relative">
          {/* Messages Stream */}
          <div className="flex-1 overflow-y-auto p-4 lg:p-6 space-y-4">
            {messages.map((msg) => {
              const isAssistant = msg.role === 'assistant';
              return (
                <div
                  key={msg.id}
                  className={`flex flex-col ${isAssistant ? 'items-start' : 'items-end'} animate-fadeIn`}
                >
                  <div className="flex items-center gap-2 mb-1 px-1 text-[10px] font-mono-hud text-slate-400">
                    <span>{isAssistant ? 'H.E.N.R.Y.' : userProfile.name || 'You'}</span>
                    <span>•</span>
                    <span>{msg.timestamp}</span>
                    {msg.emotion && (
                      <span className="px-1.5 py-0.2 rounded bg-cyan-500/10 text-cyan-300 border border-cyan-500/30 text-[9px] uppercase">
                        {msg.emotion}
                      </span>
                    )}
                  </div>

                  <div
                    className={`max-w-[88%] md:max-w-[78%] rounded-2xl p-4 text-sm leading-relaxed transition-all ${
                      isAssistant
                        ? 'bg-[#031427]/90 border border-cyan-500/30 text-[#e2f1ff] glow-cyan'
                        : 'bg-cyan-600/20 border border-cyan-400/40 text-cyan-50'
                    }`}
                  >
                    {/* Attached Image if any */}
                    {msg.imageBase64 && (
                      <img
                        src={msg.imageBase64}
                        alt="Upload"
                        className="rounded-xl max-h-60 mb-3 border border-cyan-500/30 object-cover"
                      />
                    )}

                    {/* Generated Visual if any */}
                    {msg.imageUrl && (
                      <div className="mb-3 rounded-xl overflow-hidden border border-cyan-500/40 bg-[#010814]">
                        <img
                          src={msg.imageUrl}
                          alt="Neural Synthesized Visual"
                          referrerPolicy="no-referrer"
                          className="w-full max-h-80 object-cover"
                        />
                      </div>
                    )}

                    {/* Markdown Body */}
                    <div className="prose prose-invert prose-cyan max-w-none text-xs md:text-sm font-sans break-words space-y-2">
                      <ReactMarkdown>{msg.text}</ReactMarkdown>
                    </div>

                    {/* Assistant message action bar */}
                    {isAssistant && (
                      <div className="flex items-center gap-3 mt-3 pt-2 border-t border-cyan-500/15 text-[11px] font-mono-hud text-slate-400">
                        <button
                          onClick={() => copyMessage(msg.id, msg.text)}
                          className="hover:text-cyan-300 flex items-center gap-1 transition-colors"
                        >
                          {copiedId === msg.id ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                          <span>{copiedId === msg.id ? 'Copied' : 'Copy'}</span>
                        </button>
                        <button
                          onClick={() => speakText(msg.text)}
                          className="hover:text-cyan-300 flex items-center gap-1 transition-colors"
                        >
                          <Volume2 className="w-3.5 h-3.5" />
                          <span>Speak</span>
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
            <div ref={chatEndRef} />
          </div>

          {/* Quick Suggestion Chips Horizontal Bar */}
          <div className="px-4 py-2 flex items-center gap-2 overflow-x-auto scrollbar-none border-t border-cyan-500/10 bg-[#020c1a]/70 shrink-0">
            {SUGGESTIONS.map((s, idx) => (
              <button
                key={idx}
                onClick={() => handleSendMessage(s.prompt)}
                className="px-3 py-1 rounded-full bg-[#031427] border border-cyan-500/25 hover:border-cyan-400 text-[11px] font-mono-hud text-cyan-200 whitespace-nowrap transition-all"
              >
                {s.label}
              </button>
            ))}
          </div>

          {/* Image Attachment Preview */}
          {attachedImage && (
            <div className="px-4 py-2 bg-[#010814] border-t border-cyan-500/20 flex items-center gap-3">
              <img src={attachedImage} alt="Attachment" className="w-12 h-12 object-cover rounded-lg border border-cyan-500/40" />
              <div className="flex-1 text-xs font-mono-hud text-cyan-300">Image attached for multimodal vision analysis</div>
              <button onClick={() => setAttachedImage(null)} className="p-1 text-red-400 hover:text-red-300">
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Input Control Console */}
          <div className="p-4 border-t border-cyan-500/20 bg-[#031326]/80 backdrop-blur-md shrink-0">
            <div className="flex items-center gap-2 max-w-5xl mx-auto">
              {/* Image upload trigger */}
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleAttachImage}
                accept="image/*"
                className="hidden"
              />
              <button
                onClick={() => fileInputRef.current?.click()}
                className="p-3 rounded-xl bg-[#010814] border border-cyan-500/30 text-cyan-400 hover:text-white hover:border-cyan-400 transition-colors"
                title="Attach image for vision scan"
              >
                <ImageIcon className="w-4 h-4" />
              </button>

              {/* Voice input button */}
              <button
                onClick={toggleVoiceInput}
                className={`p-3 rounded-xl border transition-all ${
                  isListening
                    ? 'bg-amber-500 border-amber-400 text-slate-950 animate-pulse glow-amber'
                    : 'bg-[#010814] border-cyan-500/30 text-cyan-400 hover:text-white hover:border-cyan-400'
                }`}
                title={isListening ? 'Stop speech input' : 'Speak to H.E.N.R.Y.'}
              >
                {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
              </button>

              {/* Text Input Field */}
              <input
                type="text"
                value={inputVal}
                onChange={(e) => setInputVal(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage();
                  }
                }}
                placeholder={isListening ? 'Listening to voice stream...' : 'Query H.E.N.R.Y. or enter neural command...'}
                className="flex-1 px-4 py-3 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs md:text-sm font-mono-hud text-slate-100 placeholder-slate-500 focus:outline-none focus:border-cyan-400 transition-colors"
              />

              {/* Send Button */}
              <button
                onClick={() => handleSendMessage()}
                disabled={(!inputVal.trim() && !attachedImage) || isSending}
                className="p-3 rounded-xl bg-cyan-500 hover:bg-cyan-400 disabled:opacity-40 text-slate-950 font-bold transition-all shadow-lg shadow-cyan-500/25"
                title="Transmit prompt"
              >
                {isSending ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* 3. MODAL HUBS */}
      {activeHub === 'brain' && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md animate-fadeIn">
          <BrainHub
            onClose={() => setActiveHub(null)}
            onSelectPrompt={(p) => {
              setActiveHub(null);
              handleSendMessage(p);
            }}
            memories={memories}
            onAddMemory={addMemory}
            onDeleteMemory={deleteMemory}
          />
        </div>
      )}

      {activeHub === 'space' && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md animate-fadeIn">
          <SpaceHub onClose={() => setActiveHub(null)} />
        </div>
      )}

      {activeHub === 'earth' && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md animate-fadeIn">
          <EarthRadarHub onClose={() => setActiveHub(null)} />
        </div>
      )}

      {activeHub === 'markets' && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md animate-fadeIn">
          <MarketsHub onClose={() => setActiveHub(null)} />
        </div>
      )}

      {activeHub === 'chemistry' && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md animate-fadeIn">
          <ChemistryHub onClose={() => setActiveHub(null)} />
        </div>
      )}

      {activeHub === 'vision' && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md animate-fadeIn">
          <VisionScannerHub
            onClose={() => setActiveHub(null)}
            onSendToChat={(text, img) => {
              handleSendMessage(text, img);
            }}
          />
        </div>
      )}

      {activeHub === 'tools' && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md animate-fadeIn">
          <ToolsSuiteHub onClose={() => setActiveHub(null)} />
        </div>
      )}

      {activeHub === 'settings' && (
        <SettingsModal
          onClose={() => setActiveHub(null)}
          responseMode={responseMode}
          onSetResponseMode={setResponseMode}
          userProfile={userProfile}
          onSaveProfile={setUserProfile}
          selectedVoice={selectedVoice}
          onSetVoice={setSelectedVoice}
          availableVoices={availableVoices}
          speechRate={speechRate}
          onSetSpeechRate={setSpeechRate}
          autoTts={autoTts}
          onSetAutoTts={setAutoTts}
          onClearAllData={clearAllData}
        />
      )}
    </div>
  );
}
export default App;
