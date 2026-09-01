import React, { useState, useEffect, useRef } from 'react';
import { Brain, Sparkles, Zap, Eye, Database, Compass, BookOpen, Volume2, Play, Pause, RotateCcw, Check, ArrowRight, Plus, Trash2, Search, Lock } from 'lucide-react';
import confetti from 'canvas-confetti';

interface BrainHubProps {
  onClose: () => void;
  onSelectPrompt: (prompt: string) => void;
  memories: string[];
  onAddMemory: (mem: string) => void;
  onDeleteMemory: (index: number) => void;
}

type BrainTab = 'map' | 'imagery' | 'plasticity' | 'dmn' | 'sensory' | 'memory';

export const BrainHub: React.FC<BrainHubProps> = ({
  onClose,
  onSelectPrompt,
  memories,
  onAddMemory,
  onDeleteMemory
}) => {
  const [activeTab, setActiveTab] = useState<BrainTab>('map');

  // --- Mental Imagery State ---
  const [imageryCategory, setImageryCategory] = useState<string>('ocean_calm');
  const [imageryStep, setImageryStep] = useState<number>(0);
  const [imageryPlaying, setImageryPlaying] = useState<boolean>(false);

  // --- Neural Plasticity State ---
  const [npModule, setNpModule] = useState<string | null>(null);
  const [npScore, setNpScore] = useState<number>(() => {
    return parseInt(localStorage.getItem('henry_np_score') || '0', 10);
  });
  const [npStreak, setNpStreak] = useState<number>(() => {
    return parseInt(localStorage.getItem('henry_np_streak') || '0', 10);
  });
  const [npExerciseIndex, setNpExerciseIndex] = useState<number>(0);
  const [npFeedback, setNpFeedback] = useState<string>('');
  const [npAnswerSelected, setNpAnswerSelected] = useState<string | null>(null);

  // --- DMN State ---
  const [dmnPrompt, setDmnPrompt] = useState<string>('If you could send a single 3-word message to yourself 5 years ago, what would it be?');
  const [dmnJournal, setDmnJournal] = useState<string>('');
  const [dmnSavedEntries, setDmnSavedEntries] = useState<Array<{ id: string; date: string; text: string; prompt: string }>>(() => {
    try {
      return JSON.parse(localStorage.getItem('henry_dmn_entries') || '[]');
    } catch {
      return [];
    }
  });

  // --- Sensory Substitution Audio Context ---
  const [sensoryColor, setSensoryColor] = useState<string>('#00D4FF');
  const [sensoryFrequency, setSensoryFrequency] = useState<number>(440);
  const [sensoryAudioPlaying, setSensoryAudioPlaying] = useState<boolean>(false);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const oscRef = useRef<OscillatorNode | null>(null);
  const gainRef = useRef<GainNode | null>(null);

  // --- Memory state ---
  const [newMemoryInput, setNewMemoryInput] = useState<string>('');
  const [memorySearch, setMemorySearch] = useState<string>('');

  const IMAGERY_SCRIPTS: Record<string, { title: string; icon: string; steps: string[] }> = {
    ocean_calm: {
      title: 'Ocean Calm',
      icon: '🌊',
      steps: [
        'Close your eyes. Take a slow, measured breath in... and gently release.',
        'You are standing at the edge of a vast, calm ocean at golden hour. Warm sand beneath your feet.',
        'A gentle crystal wave rolls in, soothingly touching your ankles. The temperature is perfect.',
        'Synchronize your breath to the rhythm: slow surge, soft retreat, peaceful stillness.',
        'The horizon is bathed in amber and rose light. You are safe, grounded, and centered.',
        'Every breath in invites clarity; every breath out lets go of tension.',
        'You are boundless and calm like the ocean depths.',
        'Open your eyes when you feel renewed, carrying this tranquil state forward.'
      ]
    },
    mountain: {
      title: 'Mountain Peak',
      icon: '🏔️',
      steps: [
        'Stand tall in your mind at the foot of an ancient, snow-capped mountain. The air is crisp and pure.',
        'Each upward step builds quiet power. Leave any heavy thoughts on the trail below.',
        'At the midway ridge, pause and gaze at the sprawling horizon below—challenges shrink into perspective.',
        'Breathe the thin, exhilarating mountain air as you ascend toward the summit.',
        'You stand on the peak. Sunlight strikes the summit stone. You are above the clouds.',
        'From this height, every obstacle is a minor valley you have already mastered.',
        'Absorb the mountain’s enduring resilience and calm.',
        'Carry this summit clarity as you return to your day.'
      ]
    },
    memory_palace: {
      title: 'Memory Palace',
      icon: '🧠',
      steps: [
        'Envision the grand entrance of a building you know intimately.',
        'Step through the heavy double doors into the sunlit foyer.',
        'In the living chamber, place your first key concept as an exaggerated, vivid artifact.',
        'Move to the library wing. Anchor your next concept onto the polished wooden desk.',
        'Spatial associations trigger superhuman recall in the human hippocampus.',
        'Walk backward along your mental corridor, observing each artifact in its exact vault location.',
        'Your palace is permanent and expandable. Return here whenever retrieving key knowledge.'
      ]
    },
    cosmic: {
      title: 'Cosmic Journey',
      icon: '🌌',
      steps: [
        'Drift upward past the atmosphere. Below rests Earth, a glowing sapphire orb suspended in void.',
        'Feel total weightlessness. Silence surrounds you in serene cosmic expanse.',
        'Stars and nebulae swirl in infinite violet and cyan currents.',
        'Remember that every atom in your body was forged in the hearts of dying stars billions of years ago.',
        'You are not just a visitor in the cosmos; you are the universe observing itself.',
        'Gently float back toward Earth, bringing infinite perspective to finite worries.'
      ]
    },
    forest: {
      title: 'Forest Healing',
      icon: '🌿',
      steps: [
        'Step onto the soft moss path of an ancient pine and cedar forest.',
        'Sunlight filters down in gentle green and gold pillars.',
        'Inhale the grounding scent of pine needles, damp earth, and clean rain.',
        'Hear birdsong echoing through the high canopy and a nearby babbling brook.',
        'Rest your hand against a giant cedar trunk. Feel its slow, deep vitality.',
        'Breathe out fatigue; breathe in pure cellular restoration.'
      ]
    }
  };

  // Mental imagery auto-step interval
  useEffect(() => {
    let timer: any;
    if (imageryPlaying) {
      timer = setInterval(() => {
        setImageryStep(prev => {
          const total = IMAGERY_SCRIPTS[imageryCategory].steps.length;
          if (prev >= total - 1) {
            setImageryPlaying(false);
            return prev;
          }
          return prev + 1;
        });
      }, 5500);
    }
    return () => clearInterval(timer);
  }, [imageryPlaying, imageryCategory]);

  // Sensory Color to Frequency synthesizer
  const toggleSensoryAudio = () => {
    if (sensoryAudioPlaying) {
      if (oscRef.current) {
        oscRef.current.stop();
        oscRef.current.disconnect();
      }
      setSensoryAudioPlaying(false);
    } else {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!audioCtxRef.current) {
        audioCtxRef.current = new AudioCtx();
      }
      const ctx = audioCtxRef.current;
      if (ctx.state === 'suspended') {
        ctx.resume();
      }
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(sensoryFrequency, ctx.currentTime);
      gain.gain.setValueAtTime(0.12, ctx.currentTime);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();

      oscRef.current = osc;
      gainRef.current = gain;
      setSensoryAudioPlaying(true);
    }
  };

  const handleColorChange = (hex: string) => {
    setSensoryColor(hex);
    // Map hex color to frequency (200Hz - 900Hz)
    const r = parseInt(hex.slice(1, 3), 16) || 0;
    const g = parseInt(hex.slice(3, 5), 16) || 0;
    const b = parseInt(hex.slice(5, 7), 16) || 0;
    const freq = Math.round(200 + ((r * 0.3 + g * 0.5 + b * 0.2) / 255) * 700);
    setSensoryFrequency(freq);
    if (oscRef.current && audioCtxRef.current) {
      oscRef.current.frequency.setTargetAtTime(freq, audioCtxRef.current.currentTime, 0.05);
    }
  };

  useEffect(() => {
    return () => {
      if (oscRef.current) {
        try {
          oscRef.current.stop();
          oscRef.current.disconnect();
        } catch {}
      }
    };
  }, []);

  // Neural Plasticity Exercises Data
  const NP_EXERCISES = [
    {
      module: 'stroop',
      title: '🔵 Stroop Neuro-Flexibility Drill',
      question: 'What is the INK COLOR of this word?',
      word: 'YELLOW',
      textColor: '#EF4444', // Red ink
      options: ['Red', 'Yellow', 'Blue', 'Green'],
      answer: 'Red'
    },
    {
      module: 'memory',
      title: '🧮 Working Memory Sequence',
      question: 'Which sequence reverses: 7 - 3 - 9 - 1 - 4 ?',
      word: '7 3 9 1 4',
      textColor: '#00D4FF',
      options: ['4 - 1 - 9 - 3 - 7', '4 - 9 - 1 - 3 - 7', '7 - 1 - 9 - 3 - 4', '1 - 4 - 9 - 3 - 7'],
      answer: '4 - 1 - 9 - 3 - 7'
    },
    {
      module: 'flip',
      title: '🔄 Cognitive Perspective Flip',
      question: 'If North becomes Left and East becomes Up, where does South point?',
      word: 'Navigation Matrix',
      textColor: '#10B981',
      options: ['Right', 'Down', 'Up', 'North'],
      answer: 'Right'
    },
    {
      module: 'numbers',
      title: '🔢 Neural Number Sense',
      question: 'Identify the missing prime in the series: 13, 17, 19, [ ? ], 29',
      word: '13 · 17 · 19 · ? · 29',
      textColor: '#F59E0B',
      options: ['21', '23', '25', '27'],
      answer: '23'
    },
    {
      module: 'word_rev',
      title: '🔤 Anagram & Word Reversal',
      question: 'Unscramble the neural term: N E U R O N',
      word: 'R O N N U E',
      textColor: '#A855F7',
      options: ['NEURON', 'PROTON', 'NUCLEUS', 'NEURITE'],
      answer: 'NEURON'
    }
  ];

  const handleNpAnswer = (option: string) => {
    const curr = NP_EXERCISES[npExerciseIndex];
    setNpAnswerSelected(option);
    if (option === curr.answer) {
      const newScore = npScore + 10;
      const newStreak = npStreak + 1;
      setNpScore(newScore);
      setNpStreak(newStreak);
      localStorage.setItem('henry_np_score', String(newScore));
      localStorage.setItem('henry_np_streak', String(newStreak));
      setNpFeedback('⚡ Correct! Neural pathway reinforced.');
      confetti({ particleCount: 35, spread: 60, origin: { y: 0.7 } });
    } else {
      setNpStreak(0);
      localStorage.setItem('henry_np_streak', '0');
      setNpFeedback(`❌ Incorrect. The reinforced answer was: ${curr.answer}`);
    }
  };

  const nextNpExercise = () => {
    setNpAnswerSelected(null);
    setNpFeedback('');
    setNpExerciseIndex((prev) => (prev + 1) % NP_EXERCISES.length);
  };

  // DMN prompt generator
  const DMN_PROMPTS = [
    'If you could send a single 3-word message to yourself 5 years ago, what would it be?',
    'What is a belief you held passionately 3 years ago that you have now completely evolved away from?',
    'If you had unlimited energy and zero fear of failure for 48 hours, what would you construct?',
    'What subtle beauty did you notice today that most people would have walked right past?',
    'Imagine yourself at 80 years old looking back at today. What advice would they whisper to you?'
  ];

  const handleSaveDmn = () => {
    if (!dmnJournal.trim()) return;
    const entry = {
      id: Date.now().toString(),
      date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }),
      text: dmnJournal,
      prompt: dmnPrompt
    };
    const updated = [entry, ...dmnSavedEntries];
    setDmnSavedEntries(updated);
    localStorage.setItem('henry_dmn_entries', JSON.stringify(updated));
    setDmnJournal('');
    confetti({ particleCount: 20, spread: 45, origin: { y: 0.8 } });
  };

  return (
    <div className="flex flex-col h-full bg-[#020C1B] text-[#e2f1ff] overflow-hidden">
      {/* Top Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-cyan-500/20 bg-[#031326]/60 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
            <Brain className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold font-tech tracking-wider text-cyan-300">H.E.N.R.Y. BRAIN MATRIX</h2>
            <p className="text-xs text-cyan-400/60 font-mono-hud">Cognitive Training, Memory Vault & Neural Synthesizer</p>
          </div>
        </div>

        {/* Tab Switcher */}
        <div className="flex items-center gap-1 p-1 bg-[#010814] rounded-lg border border-cyan-500/20">
          <button
            onClick={() => setActiveTab('map')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'map' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm' : 'text-slate-400 hover:text-white'
            }`}
          >
            🗺️ Cortex Map
          </button>
          <button
            onClick={() => setActiveTab('imagery')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'imagery' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            ✨ Mental Imagery
          </button>
          <button
            onClick={() => setActiveTab('plasticity')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'plasticity' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            ⚡ Neuroplasticity
          </button>
          <button
            onClick={() => setActiveTab('dmn')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'dmn' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            🌌 DMN Journal
          </button>
          <button
            onClick={() => setActiveTab('sensory')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'sensory' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            🎨 Sensory Synth
          </button>
          <button
            onClick={() => setActiveTab('memory')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'memory' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            💾 Memory ({memories.length})
          </button>
        </div>

        <button
          onClick={onClose}
          className="px-3 py-1.5 rounded bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-300 text-xs font-mono-hud transition-colors"
        >
          Exit Matrix
        </button>
      </div>

      {/* Main Tab Content */}
      <div className="flex-1 overflow-y-auto p-6">
        {/* 1. CORTEX MAP TAB */}
        {activeTab === 'map' && (
          <div className="max-w-4xl mx-auto flex flex-col items-center">
            <div className="text-center mb-6">
              <h3 className="text-xl font-tech text-cyan-300 tracking-wider">INTERACTIVE NEURAL ATLAS</h3>
              <p className="text-xs text-slate-400 mt-1">Tap any cortex lobe or cognitive node to initialize neural exercise</p>
            </div>

            {/* Glowing Brain SVG Map */}
            <div className="relative w-full max-w-xl aspect-4/3 bg-[#031326]/50 rounded-2xl border border-cyan-500/30 p-6 flex items-center justify-center glow-cyan">
              <svg viewBox="0 0 600 450" className="w-full h-full filter drop-shadow-[0_0_15px_rgba(0,212,255,0.4)]">
                {/* Outer Brain Contours */}
                <path
                  d="M300 40 C180 40, 70 120, 70 240 C70 340, 160 410, 300 410 C440 410, 530 340, 530 240 C530 120, 420 40, 300 40 Z"
                  fill="#051c33"
                  stroke="#00D4FF"
                  strokeWidth="2"
                  strokeDasharray="6 4"
                />
                <line x1="300" y1="50" x2="300" y2="400" stroke="#00D4FF" strokeWidth="1.5" strokeOpacity="0.4" />

                {/* Decorative Sulci curves */}
                <path d="M140 180 Q200 150 250 190 Q220 250 170 240" fill="none" stroke="#00D4FF" strokeWidth="1" strokeOpacity="0.3" />
                <path d="M460 180 Q400 150 350 190 Q380 250 430 240" fill="none" stroke="#00D4FF" strokeWidth="1" strokeOpacity="0.3" />
                <path d="M190 280 Q250 320 280 280" fill="none" stroke="#00D4FF" strokeWidth="1" strokeOpacity="0.3" />
                <path d="M410 280 Q350 320 320 280" fill="none" stroke="#00D4FF" strokeWidth="1" strokeOpacity="0.3" />

                {/* Interactive Node: Mental Imagery */}
                <g onClick={() => setActiveTab('imagery')} className="cursor-pointer group">
                  <ellipse cx="300" cy="110" rx="65" ry="35" fill="#00D4FF22" stroke="#00D4FF" strokeWidth="1.5" className="transition-all group-hover:fill-cyan-500/40" />
                  <text x="300" y="105" textAnchor="middle" fill="#00D4FF" fontSize="13" fontWeight="bold" fontFamily="monospace">✨ Mental</text>
                  <text x="300" y="122" textAnchor="middle" fill="#00D4FF" fontSize="13" fontWeight="bold" fontFamily="monospace">Imagery</text>
                </g>

                {/* Interactive Node: Neuroplasticity */}
                <g onClick={() => setActiveTab('plasticity')} className="cursor-pointer group">
                  <ellipse cx="180" cy="180" rx="55" ry="35" fill="#10B98122" stroke="#10B981" strokeWidth="1.5" className="transition-all group-hover:fill-emerald-500/40" />
                  <text x="180" y="175" textAnchor="middle" fill="#10B981" fontSize="12" fontWeight="bold" fontFamily="monospace">⚡ Neural</text>
                  <text x="180" y="192" textAnchor="middle" fill="#10B981" fontSize="12" fontWeight="bold" fontFamily="monospace">Plasticity</text>
                </g>

                {/* Interactive Node: Default Mode Network */}
                <g onClick={() => setActiveTab('dmn')} className="cursor-pointer group">
                  <ellipse cx="420" cy="180" rx="55" ry="35" fill="#A855F722" stroke="#A855F7" strokeWidth="1.5" className="transition-all group-hover:fill-purple-500/40" />
                  <text x="420" y="175" textAnchor="middle" fill="#A855F7" fontSize="12" fontWeight="bold" fontFamily="monospace">🌌 Default</text>
                  <text x="420" y="192" textAnchor="middle" fill="#A855F7" fontSize="12" fontWeight="bold" fontFamily="monospace">Mode (DMN)</text>
                </g>

                {/* Interactive Node: Sensory Substitution */}
                <g onClick={() => setActiveTab('sensory')} className="cursor-pointer group">
                  <ellipse cx="160" cy="270" rx="55" ry="35" fill="#F59E0B22" stroke="#F59E0B" strokeWidth="1.5" className="transition-all group-hover:fill-amber-500/40" />
                  <text x="160" y="265" textAnchor="middle" fill="#F59E0B" fontSize="12" fontWeight="bold" fontFamily="monospace">🎨 Sensory</text>
                  <text x="160" y="282" textAnchor="middle" fill="#F59E0B" fontSize="12" fontWeight="bold" fontFamily="monospace">Synth</text>
                </g>

                {/* Interactive Node: Smart Memory Vault */}
                <g onClick={() => setActiveTab('memory')} className="cursor-pointer group">
                  <ellipse cx="440" cy="270" rx="55" ry="35" fill="#EC489922" stroke="#EC4899" strokeWidth="1.5" className="transition-all group-hover:fill-pink-500/40" />
                  <text x="440" y="265" textAnchor="middle" fill="#EC4899" fontSize="12" fontWeight="bold" fontFamily="monospace">💾 Memory</text>
                  <text x="440" y="282" textAnchor="middle" fill="#EC4899" fontSize="12" fontWeight="bold" fontFamily="monospace">Vault</text>
                </g>

                {/* Interactive Node: Workspace Tools */}
                <g onClick={() => onSelectPrompt('Create a comprehensive project outline and research brief')} className="cursor-pointer group">
                  <ellipse cx="300" cy="330" rx="60" ry="32" fill="#3B82F622" stroke="#3B82F6" strokeWidth="1.5" className="transition-all group-hover:fill-blue-500/40" />
                  <text x="300" y="325" textAnchor="middle" fill="#60A5FA" fontSize="12" fontWeight="bold" fontFamily="monospace">📄 Project</text>
                  <text x="300" y="342" textAnchor="middle" fill="#60A5FA" fontSize="12" fontWeight="bold" fontFamily="monospace">Synthesizer</text>
                </g>
              </svg>
            </div>

            {/* Quick module launcher buttons */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3 w-full max-w-2xl mt-6">
              <button
                onClick={() => setActiveTab('imagery')}
                className="p-3 rounded-xl bg-[#031427] border border-cyan-500/20 hover:border-cyan-500/50 flex items-center gap-3 text-left transition-all"
              >
                <span className="text-xl">🌊</span>
                <div>
                  <h4 className="text-xs font-bold text-cyan-300 font-mono-hud">Mental Imagery</h4>
                  <p className="text-[10px] text-slate-400">8 Guided visual journeys</p>
                </div>
              </button>

              <button
                onClick={() => setActiveTab('plasticity')}
                className="p-3 rounded-xl bg-[#031427] border border-emerald-500/20 hover:border-emerald-500/50 flex items-center gap-3 text-left transition-all"
              >
                <span className="text-xl">⚡</span>
                <div>
                  <h4 className="text-xs font-bold text-emerald-300 font-mono-hud">Neuroplasticity</h4>
                  <p className="text-[10px] text-slate-400">Streak: {npStreak} · Score: {npScore}</p>
                </div>
              </button>

              <button
                onClick={() => setActiveTab('dmn')}
                className="p-3 rounded-xl bg-[#031427] border border-purple-500/20 hover:border-purple-500/50 flex items-center gap-3 text-left transition-all"
              >
                <span className="text-xl">🌌</span>
                <div>
                  <h4 className="text-xs font-bold text-purple-300 font-mono-hud">DMN Journal</h4>
                  <p className="text-[10px] text-slate-400">Mind-wandering & reflection</p>
                </div>
              </button>
            </div>
          </div>
        )}

        {/* 2. MENTAL IMAGERY TAB */}
        {activeTab === 'imagery' && (
          <div className="max-w-3xl mx-auto flex flex-col items-center">
            {/* Category Selector */}
            <div className="flex flex-wrap justify-center gap-2 mb-6">
              {Object.entries(IMAGERY_SCRIPTS).map(([key, item]) => (
                <button
                  key={key}
                  onClick={() => {
                    setImageryCategory(key);
                    setImageryStep(0);
                    setImageryPlaying(false);
                  }}
                  className={`px-4 py-2 rounded-xl text-xs font-mono-hud flex items-center gap-2 border transition-all ${
                    imageryCategory === key
                      ? 'bg-cyan-500/20 border-cyan-400 text-cyan-200 glow-cyan'
                      : 'bg-[#031427] border-cyan-500/20 text-slate-400 hover:text-white'
                  }`}
                >
                  <span>{item.icon}</span>
                  <span>{item.title}</span>
                </button>
              ))}
            </div>

            {/* Guided Visual Stage */}
            <div className="w-full bg-[#031427]/80 rounded-2xl border border-cyan-500/30 p-8 flex flex-col items-center text-center relative overflow-hidden glow-cyan min-h-[300px] justify-between">
              <div className="flex items-center justify-between w-full text-xs font-mono-hud text-cyan-400/70 border-b border-cyan-500/20 pb-3">
                <span className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-cyan-300" />
                  {IMAGERY_SCRIPTS[imageryCategory].title} Journey
                </span>
                <span>Step {imageryStep + 1} of {IMAGERY_SCRIPTS[imageryCategory].steps.length}</span>
              </div>

              {/* Central Guided Prompt */}
              <div className="my-8 max-w-xl">
                <p className="text-xl md:text-2xl font-light text-cyan-100 leading-relaxed italic animate-fadeIn">
                  "{IMAGERY_SCRIPTS[imageryCategory].steps[imageryStep]}"
                </p>
              </div>

              {/* Controls */}
              <div className="flex items-center gap-4 w-full justify-between pt-4 border-t border-cyan-500/20">
                <button
                  onClick={() => setImageryStep(prev => Math.max(0, prev - 1))}
                  disabled={imageryStep === 0}
                  className="px-4 py-2 rounded-lg bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud disabled:opacity-30"
                >
                  Previous
                </button>

                <button
                  onClick={() => setImageryPlaying(!imageryPlaying)}
                  className="px-6 py-2.5 rounded-full bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold font-mono-hud flex items-center gap-2 shadow-lg shadow-cyan-500/25 transition-all"
                >
                  {imageryPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                  {imageryPlaying ? 'Pause Guide' : 'Begin Guided Flow'}
                </button>

                <button
                  onClick={() => setImageryStep(prev => Math.min(IMAGERY_SCRIPTS[imageryCategory].steps.length - 1, prev + 1))}
                  disabled={imageryStep === IMAGERY_SCRIPTS[imageryCategory].steps.length - 1}
                  className="px-4 py-2 rounded-lg bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud disabled:opacity-30"
                >
                  Next
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 3. NEURAL PLASTICITY TAB */}
        {activeTab === 'plasticity' && (
          <div className="max-w-2xl mx-auto flex flex-col items-center">
            {/* Scoreboard */}
            <div className="grid grid-cols-2 gap-4 w-full mb-6">
              <div className="p-4 rounded-xl bg-[#031427] border border-cyan-500/30 flex items-center justify-between">
                <div>
                  <p className="text-[10px] font-mono-hud text-slate-400">TOTAL NEURAL POINTS</p>
                  <p className="text-2xl font-bold font-tech text-cyan-300">◈ {npScore}</p>
                </div>
                <Zap className="w-6 h-6 text-cyan-400" />
              </div>
              <div className="p-4 rounded-xl bg-[#031427] border border-amber-500/30 flex items-center justify-between">
                <div>
                  <p className="text-[10px] font-mono-hud text-slate-400">ACTIVE STREAK</p>
                  <p className="text-2xl font-bold font-tech text-amber-300">🔥 {npStreak}</p>
                </div>
                <Sparkles className="w-6 h-6 text-amber-400" />
              </div>
            </div>

            {/* Exercise Card */}
            {(() => {
              const curr = NP_EXERCISES[npExerciseIndex];
              return (
                <div className="w-full bg-[#031427]/90 rounded-2xl border border-cyan-500/30 p-6 flex flex-col items-center text-center glow-cyan">
                  <span className="px-3 py-1 rounded-full bg-cyan-500/10 text-cyan-300 text-xs font-mono-hud border border-cyan-500/30 mb-4">
                    {curr.title}
                  </span>

                  <h3 className="text-lg font-medium text-slate-200 mb-6">{curr.question}</h3>

                  {/* Highlight Word / Stimulus */}
                  <div
                    className="px-6 py-4 rounded-xl bg-[#010814] border border-cyan-500/30 text-3xl font-bold font-mono-hud tracking-widest mb-8 select-none"
                    style={{ color: curr.textColor }}
                  >
                    {curr.word}
                  </div>

                  {/* Options */}
                  <div className="grid grid-cols-2 gap-3 w-full mb-6">
                    {curr.options.map((opt) => {
                      const isSelected = npAnswerSelected === opt;
                      const isCorrect = opt === curr.answer;
                      let btnStyle = 'bg-[#010814] border-cyan-500/20 text-slate-200 hover:border-cyan-400';
                      if (npAnswerSelected) {
                        if (isCorrect) btnStyle = 'bg-emerald-500/20 border-emerald-400 text-emerald-200';
                        else if (isSelected) btnStyle = 'bg-red-500/20 border-red-400 text-red-200';
                      }

                      return (
                        <button
                          key={opt}
                          disabled={!!npAnswerSelected}
                          onClick={() => handleNpAnswer(opt)}
                          className={`p-3.5 rounded-xl border text-sm font-mono-hud transition-all ${btnStyle}`}
                        >
                          {opt}
                        </button>
                      );
                    })}
                  </div>

                  {/* Feedback and Next */}
                  {npFeedback && (
                    <div className="flex flex-col items-center gap-4 w-full pt-4 border-t border-cyan-500/20 animate-fadeIn">
                      <p className="text-xs font-mono-hud text-cyan-300">{npFeedback}</p>
                      <button
                        onClick={nextNpExercise}
                        className="px-6 py-2 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold font-mono-hud flex items-center gap-2"
                      >
                        Next Challenge <ArrowRight className="w-4 h-4" />
                      </button>
                    </div>
                  )}
                </div>
              );
            })()}
          </div>
        )}

        {/* 4. DEFAULT MODE NETWORK (DMN) TAB */}
        {activeTab === 'dmn' && (
          <div className="max-w-3xl mx-auto space-y-6">
            <div className="p-6 rounded-2xl bg-[#031427]/80 border border-purple-500/30 glow-purple">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Compass className="w-5 h-5 text-purple-400" />
                  <h3 className="text-base font-bold font-tech text-purple-300">MIND-WANDERING & REFLECTION CATALYST</h3>
                </div>
                <button
                  onClick={() => {
                    const nextP = DMN_PROMPTS[(DMN_PROMPTS.indexOf(dmnPrompt) + 1) % DMN_PROMPTS.length];
                    setDmnPrompt(nextP);
                  }}
                  className="px-3 py-1 rounded bg-purple-500/20 border border-purple-500/40 text-purple-300 text-xs font-mono-hud hover:bg-purple-500/30"
                >
                  🔄 New Prompt
                </button>
              </div>

              <p className="text-base text-purple-100 italic mb-4 font-light">"{dmnPrompt}"</p>

              <textarea
                value={dmnJournal}
                onChange={(e) => setDmnJournal(e.target.value)}
                placeholder="Let your thoughts flow freely without self-censorship..."
                rows={4}
                className="w-full p-4 rounded-xl bg-[#010814] border border-purple-500/30 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-purple-400"
              />

              <div className="flex justify-end mt-3">
                <button
                  onClick={handleSaveDmn}
                  disabled={!dmnJournal.trim()}
                  className="px-5 py-2 rounded-lg bg-purple-600 hover:bg-purple-500 disabled:opacity-40 text-white text-xs font-mono-hud font-bold"
                >
                  Save to Neural Journal
                </button>
              </div>
            </div>

            {/* Past Saved Reflections */}
            {dmnSavedEntries.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-xs font-bold font-mono-hud text-purple-400 tracking-wider">SAVED REFLECTIONS ({dmnSavedEntries.length})</h4>
                {dmnSavedEntries.map((entry) => (
                  <div key={entry.id} className="p-4 rounded-xl bg-[#031427]/50 border border-purple-500/20">
                    <div className="flex items-center justify-between text-[11px] font-mono-hud text-purple-400/60 mb-1">
                      <span>{entry.date}</span>
                    </div>
                    <p className="text-xs font-medium text-purple-300 mb-1 italic">Prompt: {entry.prompt}</p>
                    <p className="text-sm text-slate-300">{entry.text}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 5. SENSORY SUBSTITUTION TAB */}
        {activeTab === 'sensory' && (
          <div className="max-w-2xl mx-auto flex flex-col items-center text-center">
            <div className="p-6 rounded-2xl bg-[#031427]/80 border border-amber-500/30 w-full glow-amber mb-6">
              <div className="flex items-center justify-center gap-2 mb-2">
                <Volume2 className="w-5 h-5 text-amber-400" />
                <h3 className="text-base font-bold font-tech text-amber-300">COLOR → AUDIO FREQUENCY TRANSDUCER</h3>
              </div>
              <p className="text-xs text-slate-400 mb-6">
                Sensory substitution converts optical wavelengths into audio pitches to train cross-modal neuro-perception.
              </p>

              {/* Color swatch selector */}
              <div className="flex justify-center gap-3 mb-6">
                {['#00D4FF', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#FFFFFF'].map((hex) => (
                  <button
                    key={hex}
                    onClick={() => handleColorChange(hex)}
                    className={`w-10 h-10 rounded-full border-2 transition-transform ${
                      sensoryColor === hex ? 'scale-125 border-white shadow-lg' : 'border-transparent opacity-80 hover:opacity-100'
                    }`}
                    style={{ backgroundColor: hex }}
                  />
                ))}
              </div>

              {/* Real-time Frequency Gauge */}
              <div className="p-4 rounded-xl bg-[#010814] border border-amber-500/30 mb-6 font-mono-hud">
                <p className="text-xs text-amber-400/80">SYNTHESIZED HARMONIC PITCH</p>
                <p className="text-3xl font-bold text-amber-300 my-1">{sensoryFrequency} Hz</p>
                <p className="text-[10px] text-slate-500">Color Index: {sensoryColor}</p>
              </div>

              <button
                onClick={toggleSensoryAudio}
                className="px-6 py-3 rounded-full bg-amber-500 hover:bg-amber-400 text-slate-950 font-mono-hud font-bold text-xs flex items-center gap-2 mx-auto shadow-lg shadow-amber-500/25 transition-all"
              >
                {sensoryAudioPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                {sensoryAudioPlaying ? 'Mute Harmonic Tone' : 'Play Color Harmonic'}
              </button>
            </div>
          </div>
        )}

        {/* 6. SMART MEMORY BANK TAB */}
        {activeTab === 'memory' && (
          <div className="max-w-3xl mx-auto space-y-6">
            <div className="p-6 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan">
              <div className="flex items-center gap-2 mb-2">
                <Database className="w-5 h-5 text-cyan-400" />
                <h3 className="text-base font-bold font-tech text-cyan-300">ACTIVE LONG-TERM KNOWLEDGE VAULT</h3>
              </div>
              <p className="text-xs text-slate-400 mb-4">
                These facts are persistently injected into H.E.N.R.Y.’s neural reasoning engine across all sessions.
              </p>

              {/* Add Memory Form */}
              <div className="flex gap-2 mb-4">
                <input
                  type="text"
                  value={newMemoryInput}
                  onChange={(e) => setNewMemoryInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && newMemoryInput.trim()) {
                      onAddMemory(newMemoryInput.trim());
                      setNewMemoryInput('');
                    }
                  }}
                  placeholder="e.g. Owen is a software architect living in Dubai focusing on AI..."
                  className="flex-1 px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                />
                <button
                  onClick={() => {
                    if (newMemoryInput.trim()) {
                      onAddMemory(newMemoryInput.trim());
                      setNewMemoryInput('');
                    }
                  }}
                  className="px-4 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold flex items-center gap-1.5"
                >
                  <Plus className="w-4 h-4" /> Add Fact
                </button>
              </div>

              {/* Memory List */}
              <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
                {memories.length === 0 ? (
                  <p className="text-xs text-slate-500 italic py-4 text-center">No persistent memories stored yet. Add facts above or chat with HENRY.</p>
                ) : (
                  memories.map((mem, idx) => (
                    <div
                      key={idx}
                      className="p-3 rounded-xl bg-[#010814] border border-cyan-500/20 flex items-center justify-between text-xs font-mono-hud group hover:border-cyan-500/40 transition-colors"
                    >
                      <span className="text-slate-200">◈ {mem}</span>
                      <button
                        onClick={() => onDeleteMemory(idx)}
                        className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-red-500/20 text-red-400 transition-all"
                        title="Delete memory"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
