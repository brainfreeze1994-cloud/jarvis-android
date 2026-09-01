import React, { useState, useRef } from 'react';
import { Camera, Image as ImageIcon, Sparkles, Scan, Upload, RefreshCw, Eye, Tag, AlertCircle } from 'lucide-react';
import confetti from 'canvas-confetti';

interface VisionScannerHubProps {
  onClose: () => void;
  onSendToChat: (text: string, imageBase64?: string) => void;
}

export const VisionScannerHub: React.FC<VisionScannerHubProps> = ({ onClose, onSendToChat }) => {
  const [activeTab, setActiveTab] = useState<'scan' | 'generate'>('scan');

  // Scanner state
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [analyzing, setAnalyzing] = useState<boolean>(false);
  const [analysisResult, setAnalysisResult] = useState<string>('');
  const [scanType, setScanType] = useState<'general' | 'animal' | 'plant' | 'ocr'>('general');

  // Generator state
  const [genPrompt, setGenPrompt] = useState<string>('');
  const [generatedImgUrl, setGeneratedImgUrl] = useState<string | null>(null);
  const [generating, setGenerating] = useState<boolean>(false);

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [isCameraActive, setIsCameraActive] = useState<boolean>(false);

  // File Upload handler
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result as string);
        setAnalysisResult('');
      };
      reader.readAsDataURL(file);
    }
  };

  // Camera start/stop
  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
        setIsCameraActive(true);
      }
    } catch (err) {
      alert('Camera permission denied or unavailable in this environment. Please upload an image directly.');
    }
  };

  const captureCamera = () => {
    if (videoRef.current) {
      const canvas = document.createElement('canvas');
      canvas.width = videoRef.current.videoWidth || 640;
      canvas.height = videoRef.current.videoHeight || 480;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL('image/jpeg');
        setImagePreview(dataUrl);
        stopCamera();
      }
    }
  };

  const stopCamera = () => {
    if (videoRef.current && videoRef.current.srcObject) {
      const stream = videoRef.current.srcObject as MediaStream;
      stream.getTracks().forEach((track) => track.stop());
      videoRef.current.srcObject = null;
      setIsCameraActive(false);
    }
  };

  // Run AI Vision Analysis via Gemini
  const runVisionAnalysis = async () => {
    if (!imagePreview) return;
    setAnalyzing(true);
    setAnalysisResult('');

    let promptText = 'Examine this image thoroughly and provide a structured breakdown of objects, context, and insights.';
    if (scanType === 'animal') promptText = 'Identify the animal species, biological genus, habitat, diet, and notable traits.';
    if (scanType === 'plant') promptText = 'Identify this plant/flower species, care instructions, and botanical details.';
    if (scanType === 'ocr') promptText = 'Transcribe all visible text in this image accurately, followed by a concise translation or summary.';

    try {
      const res = await fetch('/api/jarvis', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages: [{ role: 'user', content: promptText, imageBase64: imagePreview }],
          responseMode: 'detailed'
        })
      });
      const data = await res.json();
      setAnalysisResult(data.reply || 'Analysis complete.');
      confetti({ particleCount: 25, spread: 50, origin: { y: 0.7 } });
    } catch (e: any) {
      setAnalysisResult('Error during vision processing: ' + e.message);
    } finally {
      setAnalyzing(false);
    }
  };

  // Generate Image with Pollinations Flux
  const handleGenerate = () => {
    if (!genPrompt.trim()) return;
    setGenerating(true);
    const clean = encodeURIComponent(genPrompt.trim());
    const url = `https://image.pollinations.ai/prompt/${clean}?model=flux&width=1024&height=1024&nologo=true&seed=${Date.now()}`;
    setGeneratedImgUrl(url);
    setGenerating(false);
    confetti({ particleCount: 30, spread: 60, origin: { y: 0.8 } });
  };

  return (
    <div className="flex flex-col h-full bg-[#020C1B] text-[#e2f1ff] overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-cyan-500/20 bg-[#031326]/60 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
            <Eye className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold font-tech tracking-wider text-cyan-300">VISION AI & QUANTUM SCANNER</h2>
            <p className="text-xs text-cyan-400/60 font-mono-hud">Multimodal Species Identification, OCR & Neural Image Synthesis</p>
          </div>
        </div>

        <div className="flex items-center gap-1 p-1 bg-[#010814] rounded-lg border border-cyan-500/20">
          <button
            onClick={() => setActiveTab('scan')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'scan' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            📸 Optical Scanner
          </button>
          <button
            onClick={() => setActiveTab('generate')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'generate' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            🎨 Neural Image Generator
          </button>
        </div>

        <button
          onClick={() => {
            stopCamera();
            onClose();
          }}
          className="px-3 py-1.5 rounded bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-300 text-xs font-mono-hud"
        >
          Exit Vision
        </button>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto p-6">
        {/* 1. SCANNER TAB */}
        {activeTab === 'scan' && (
          <div className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Viewport / Input */}
            <div className="flex flex-col gap-4">
              {/* Scan Type selector */}
              <div className="grid grid-cols-4 gap-2">
                {[
                  { id: 'general', label: 'General', icon: '🔍' },
                  { id: 'animal', label: 'Animals', icon: '🐾' },
                  { id: 'plant', label: 'Plants', icon: '🌿' },
                  { id: 'ocr', label: 'Text OCR', icon: '📄' }
                ].map((st) => (
                  <button
                    key={st.id}
                    onClick={() => setScanType(st.id as any)}
                    className={`py-2 px-1 rounded-xl text-xs font-mono-hud border flex flex-col items-center gap-1 transition-all ${
                      scanType === st.id
                        ? 'bg-cyan-500/20 border-cyan-400 text-cyan-200'
                        : 'bg-[#031427] border-cyan-500/20 text-slate-400'
                    }`}
                  >
                    <span>{st.icon}</span>
                    <span>{st.label}</span>
                  </button>
                ))}
              </div>

              {/* Viewport Display Box */}
              <div className="relative aspect-4/3 bg-[#031427] rounded-2xl border border-cyan-500/30 overflow-hidden flex items-center justify-center glow-cyan">
                {isCameraActive ? (
                  <video ref={videoRef} className="w-full h-full object-cover" />
                ) : imagePreview ? (
                  <img src={imagePreview} alt="Preview" className="w-full h-full object-contain" />
                ) : (
                  <div className="text-center p-6 text-slate-500 font-mono-hud text-xs">
                    <Scan className="w-12 h-12 text-cyan-500/40 mx-auto mb-2 animate-pulse" />
                    <p>No optical input loaded</p>
                    <p className="text-[10px] mt-1 text-slate-600">Capture camera or upload snapshot</p>
                  </div>
                )}
              </div>

              {/* Controls */}
              <div className="flex gap-2">
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileChange}
                  accept="image/*"
                  className="hidden"
                />

                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="flex-1 py-2.5 rounded-xl bg-[#031427] border border-cyan-500/30 hover:border-cyan-400 text-xs font-mono-hud text-cyan-300 flex items-center justify-center gap-1.5"
                >
                  <Upload className="w-4 h-4" /> Upload Image
                </button>

                {isCameraActive ? (
                  <button
                    onClick={captureCamera}
                    className="flex-1 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold flex items-center justify-center gap-1.5"
                  >
                    <Camera className="w-4 h-4" /> Capture Frame
                  </button>
                ) : (
                  <button
                    onClick={startCamera}
                    className="flex-1 py-2.5 rounded-xl bg-[#031427] border border-cyan-500/30 hover:border-cyan-400 text-xs font-mono-hud text-cyan-300 flex items-center justify-center gap-1.5"
                  >
                    <Camera className="w-4 h-4" /> Open Camera
                  </button>
                )}
              </div>

              {imagePreview && (
                <button
                  onClick={runVisionAnalysis}
                  disabled={analyzing}
                  className="w-full py-3 rounded-xl bg-cyan-500 hover:bg-cyan-400 disabled:opacity-50 text-slate-950 font-mono-hud font-bold text-xs flex items-center justify-center gap-2 shadow-lg shadow-cyan-500/25"
                >
                  {analyzing ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                  {analyzing ? 'Neural Matrix Analyzing...' : 'Run Vision Scan'}
                </button>
              )}
            </div>

            {/* Right: Analysis Report Output */}
            <div className="bg-[#031427]/80 rounded-2xl border border-cyan-500/30 p-5 glow-cyan flex flex-col justify-between">
              <div>
                <h3 className="text-xs font-bold font-mono-hud text-cyan-400 tracking-wider mb-3 flex items-center gap-1.5">
                  <Tag className="w-4 h-4" /> NEURAL VISION TELEMETRY
                </h3>

                {analysisResult ? (
                  <div className="text-xs text-slate-200 font-mono-hud leading-relaxed whitespace-pre-wrap max-h-[350px] overflow-y-auto pr-1">
                    {analysisResult}
                  </div>
                ) : (
                  <p className="text-xs text-slate-500 italic font-mono-hud py-12 text-center">
                    Load an image and tap 'Run Vision Scan' to decode optical telemetry with Gemini Multimodal AI.
                  </p>
                )}
              </div>

              {analysisResult && (
                <button
                  onClick={() => {
                    onSendToChat(`Vision findings: ${analysisResult}`, imagePreview || undefined);
                    stopCamera();
                    onClose();
                  }}
                  className="mt-4 w-full py-2 rounded-xl bg-[#010814] border border-cyan-500/40 text-cyan-300 text-xs font-mono-hud hover:bg-cyan-500/20"
                >
                  Discuss with H.E.N.R.Y. in Chat ↗
                </button>
              )}
            </div>
          </div>
        )}

        {/* 2. GENERATOR TAB */}
        {activeTab === 'generate' && (
          <div className="max-w-2xl mx-auto space-y-6">
            <div className="p-6 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan">
              <h3 className="text-base font-bold font-tech text-cyan-300 mb-2">NEURAL IMAGE SYNTHESIZER (FLUX)</h3>
              <p className="text-xs text-slate-400 mb-4">
                Generate high-definition visuals from natural language prompts using the Flux model.
              </p>

              <div className="flex gap-2 mb-6">
                <input
                  type="text"
                  value={genPrompt}
                  onChange={(e) => setGenPrompt(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleGenerate()}
                  placeholder="e.g. Futuristic neural cyber-organic brain floating in deep space..."
                  className="flex-1 px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 focus:outline-none focus:border-cyan-400"
                />
                <button
                  onClick={handleGenerate}
                  disabled={!genPrompt.trim() || generating}
                  className="px-5 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold flex items-center gap-1.5"
                >
                  <Sparkles className="w-4 h-4" /> Generate
                </button>
              </div>

              {generatedImgUrl && (
                <div className="rounded-xl overflow-hidden border border-cyan-500/30 bg-[#010814]">
                  <img
                    src={generatedImgUrl}
                    alt="Generated Visual"
                    referrerPolicy="no-referrer"
                    className="w-full max-h-[450px] object-cover"
                  />
                  <div className="p-3 text-right">
                    <a
                      href={generatedImgUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs font-mono-hud text-cyan-400 hover:underline"
                    >
                      Open Full Resolution ↗
                    </a>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
