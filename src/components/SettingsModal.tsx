import React, { useState } from 'react';
import { Settings, Volume2, User, Cpu, Trash2, X, Check } from 'lucide-react';
import { ResponseMode, UserProfile } from '../types';

interface SettingsModalProps {
  onClose: () => void;
  responseMode: ResponseMode;
  onSetResponseMode: (mode: ResponseMode) => void;
  userProfile: UserProfile;
  onSaveProfile: (profile: UserProfile) => void;
  selectedVoice: string;
  onSetVoice: (voice: string) => void;
  availableVoices: SpeechSynthesisVoice[];
  speechRate: number;
  onSetSpeechRate: (rate: number) => void;
  autoTts: boolean;
  onSetAutoTts: (val: boolean) => void;
  onClearAllData: () => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  onClose,
  responseMode,
  onSetResponseMode,
  userProfile,
  onSaveProfile,
  selectedVoice,
  onSetVoice,
  availableVoices,
  speechRate,
  onSetSpeechRate,
  autoTts,
  onSetAutoTts,
  onClearAllData
}) => {
  const [name, setName] = useState(userProfile.name);
  const [city, setCity] = useState(userProfile.city);
  const [job, setJob] = useState(userProfile.job || '');

  const handleSave = () => {
    onSaveProfile({
      ...userProfile,
      name: name.trim() || 'Sir',
      city: city.trim() || 'Dubai',
      job: job.trim()
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-lg bg-[#020C1B] rounded-2xl border border-cyan-500/30 p-6 glow-cyan flex flex-col gap-6 text-[#e2f1ff] font-mono-hud text-xs">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-cyan-500/20 pb-3">
          <div className="flex items-center gap-2">
            <Settings className="w-5 h-5 text-cyan-400" />
            <h3 className="text-base font-bold font-tech text-cyan-300">H.E.N.R.Y. SYSTEM CONFIGURATION</h3>
          </div>
          <button onClick={onClose} className="p-1 rounded hover:bg-white/10 text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* 1. Response Mode */}
        <div>
          <label className="block text-slate-400 mb-2 font-bold flex items-center gap-1.5">
            <Cpu className="w-4 h-4 text-cyan-400" /> NEURAL RESPONSE DEPTH
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['brief', 'balanced', 'detailed'] as ResponseMode[]).map((mode) => (
              <button
                key={mode}
                onClick={() => onSetResponseMode(mode)}
                className={`py-2 px-3 rounded-xl border capitalize transition-all ${
                  responseMode === mode
                    ? 'bg-cyan-500/20 border-cyan-400 text-cyan-200 glow-cyan'
                    : 'bg-[#010814] border-cyan-500/20 text-slate-400 hover:border-cyan-500/40'
                }`}
              >
                {mode}
              </button>
            ))}
          </div>
        </div>

        {/* 2. Audio & Speech Persona */}
        <div>
          <label className="block text-slate-400 mb-2 font-bold flex items-center gap-1.5">
            <Volume2 className="w-4 h-4 text-cyan-400" /> VOICE PERSONA & TTS
          </label>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 rounded-xl bg-[#010814] border border-cyan-500/20">
              <span>Auto-Speak AI Responses</span>
              <input
                type="checkbox"
                checked={autoTts}
                onChange={(e) => onSetAutoTts(e.target.checked)}
                className="w-4 h-4 accent-cyan-500 rounded"
              />
            </div>

            <div>
              <span className="text-[10px] text-slate-500 block mb-1">Synthesizer Voice ({availableVoices.length} detected)</span>
              <select
                value={selectedVoice}
                onChange={(e) => onSetVoice(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-slate-200 focus:outline-none"
              >
                {availableVoices.map((v, idx) => (
                  <option key={idx} value={v.name}>
                    {v.name} ({v.lang})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <div className="flex justify-between text-[10px] text-slate-500 mb-1">
                <span>Speech Cadence Rate</span>
                <span>{speechRate}x</span>
              </div>
              <input
                type="range"
                min="0.7"
                max="1.5"
                step="0.1"
                value={speechRate}
                onChange={(e) => onSetSpeechRate(parseFloat(e.target.value))}
                className="w-full accent-cyan-500"
              />
            </div>
          </div>
        </div>

        {/* 3. User Identity */}
        <div>
          <label className="block text-slate-400 mb-2 font-bold flex items-center gap-1.5">
            <User className="w-4 h-4 text-cyan-400" /> USER IDENTITY MATRIX
          </label>
          <div className="grid grid-cols-2 gap-2">
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Your Name (e.g. Owen)"
              className="px-3 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-slate-200 focus:outline-none"
            />
            <input
              type="text"
              value={city}
              onChange={(e) => setCity(e.target.value)}
              placeholder="Primary Location (e.g. Dubai)"
              className="px-3 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-slate-200 focus:outline-none"
            />
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center justify-between border-t border-cyan-500/20 pt-4">
          <button
            onClick={() => {
              if (confirm('Erase all conversations, tasks, and stored memories?')) {
                onClearAllData();
                onClose();
              }
            }}
            className="px-3 py-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-300 flex items-center gap-1.5"
          >
            <Trash2 className="w-3.5 h-3.5" /> Reset Matrix
          </button>

          <button
            onClick={handleSave}
            className="px-5 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold flex items-center gap-1.5 shadow-lg shadow-cyan-500/25"
          >
            <Check className="w-4 h-4" /> Save Configuration
          </button>
        </div>
      </div>
    </div>
  );
};
