import React, { useEffect, useRef } from 'react';
import { OrbState, EmotionType } from '../types';

interface OrbVisualizerProps {
  state: OrbState;
  emotion: EmotionType;
  onClick?: () => void;
  size?: number;
}

const EMOTION_COLORS: Record<EmotionType, { accent: string; core: string; dim: string }> = {
  neutral: { accent: '#00D4FF', core: '#020C1B', dim: '#004466' },
  warm: { accent: '#40E0FF', core: '#011520', dim: '#006680' },
  concerned: { accent: '#E09040', core: '#1A0E04', dim: '#6B4018' },
  excited: { accent: '#80DFFF', core: '#021A28', dim: '#0088BB' },
  amused: { accent: '#00E5CC', core: '#001A18', dim: '#007060' },
  serious: { accent: '#CC3030', core: '#180404', dim: '#5C1010' },
  proud: { accent: '#8B5CF6', core: '#0E0820', dim: '#3B1F70' },
};

export const OrbVisualizer: React.FC<OrbVisualizerProps> = ({
  state,
  emotion = 'neutral',
  onClick,
  size = 220
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animFrameId = useRef<number>(0);
  const timeRef = useRef<number>(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let startTime = performance.now();

    const render = (now: number) => {
      const dt = (now - startTime) / 1000;
      timeRef.current += 0.025;
      const t = timeRef.current;

      const colors = EMOTION_COLORS[emotion] || EMOTION_COLORS.neutral;
      const w = canvas.width;
      const h = canvas.height;
      const cx = w / 2;
      const cy = h / 2;
      const r = Math.min(w, h) * 0.42;

      ctx.clearRect(0, 0, w, h);

      // Background subtle glow
      const bgGrad = ctx.createRadialGradient(cx, cy, r * 0.1, cx, cy, r * 1.2);
      bgGrad.addColorStop(0, colors.dim + '33');
      bgGrad.addColorStop(0.6, colors.core + '88');
      bgGrad.addColorStop(1, 'transparent');
      ctx.fillStyle = bgGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, r * 1.2, 0, Math.PI * 2);
      ctx.fill();

      // State-specific behavior
      if (state === 'LISTENING') {
        // Gold / Amber burst audio bars
        ctx.strokeStyle = '#F59E0B';
        ctx.lineWidth = 2.5;
        const numBars = 36;
        for (let i = 0; i < numBars; i++) {
          const angle = (i / numBars) * Math.PI * 2 + t * 0.5;
          const amp = Math.sin(t * 8 + i * 0.8) * 14 + 18;
          const innerR = r * 0.72;
          const outerR = innerR + amp;
          const x1 = cx + Math.cos(angle) * innerR;
          const y1 = cy + Math.sin(angle) * innerR;
          const x2 = cx + Math.cos(angle) * outerR;
          const y2 = cy + Math.sin(angle) * outerR;
          ctx.beginPath();
          ctx.moveTo(x1, y1);
          ctx.lineTo(x2, y2);
          ctx.stroke();
        }
      } else if (state === 'THINKING') {
        // Orbiting glowing nodes + fast spinning dashed rings
        for (let i = 0; i < 4; i++) {
          const angle = t * 3 + (i * Math.PI) / 2;
          const nodeR = r * (0.6 + i * 0.1);
          const nx = cx + Math.cos(angle) * nodeR;
          const ny = cy + Math.sin(angle) * nodeR;

          ctx.fillStyle = colors.accent;
          ctx.beginPath();
          ctx.arc(nx, ny, 4.5, 0, Math.PI * 2);
          ctx.fill();
          ctx.shadowColor = colors.accent;
          ctx.shadowBlur = 10;
        }
        ctx.shadowBlur = 0;
      } else if (state === 'SPEAKING') {
        // Green / Cyan ripple waves
        for (let i = 0; i < 3; i++) {
          const wavePhase = (t * 2 + i * 0.8) % 2;
          const waveR = r * (0.4 + wavePhase * 0.55);
          const alpha = Math.max(0, 1 - wavePhase);
          ctx.strokeStyle = `rgba(16, 185, 129, ${alpha * 0.8})`;
          ctx.lineWidth = 2;
          ctx.beginPath();
          ctx.arc(cx, cy, waveR, 0, Math.PI * 2);
          ctx.stroke();
        }
      }

      // Outer Rotating Ring with Dashes
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(t * 0.4);
      ctx.strokeStyle = colors.accent;
      ctx.lineWidth = 1.8;
      ctx.setLineDash([8, 6]);
      ctx.beginPath();
      ctx.arc(0, 0, r, 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();

      // Counter-rotating Inner Ring
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(-t * 0.6);
      ctx.strokeStyle = colors.accent + '88';
      ctx.lineWidth = 1.2;
      ctx.setLineDash([4, 8]);
      ctx.beginPath();
      ctx.arc(0, 0, r * 0.85, 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();

      // Precision Tick Marks
      const ticks = 48;
      ctx.strokeStyle = colors.accent + '66';
      ctx.lineWidth = 1;
      ctx.setLineDash([]);
      for (let i = 0; i < ticks; i++) {
        const a = (i / ticks) * Math.PI * 2;
        const tickLen = i % 6 === 0 ? 8 : 4;
        const r1 = r * 0.94;
        const r2 = r1 - tickLen;
        ctx.beginPath();
        ctx.moveTo(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1);
        ctx.lineTo(cx + Math.cos(a) * r2, cy + Math.sin(a) * r2);
        ctx.stroke();
      }

      // Arc Reactor Central Core
      const coreGrad = ctx.createRadialGradient(cx, cy, 2, cx, cy, r * 0.48);
      coreGrad.addColorStop(0, '#FFFFFF');
      coreGrad.addColorStop(0.3, colors.accent);
      coreGrad.addColorStop(0.8, colors.dim);
      coreGrad.addColorStop(1, colors.core);

      ctx.fillStyle = coreGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.48, 0, Math.PI * 2);
      ctx.fill();

      // Inner Core Ring
      ctx.strokeStyle = colors.accent;
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.3, 0, Math.PI * 2);
      ctx.stroke();

      // Inner pulsating symbol or dots
      const pulseSize = (Math.sin(t * 4) + 1) * 2 + 4;
      ctx.fillStyle = '#FFFFFF';
      ctx.beginPath();
      ctx.arc(cx, cy, pulseSize, 0, Math.PI * 2);
      ctx.fill();

      animFrameId.current = requestAnimationFrame(render);
    };

    animFrameId.current = requestAnimationFrame(render);

    return () => {
      cancelAnimationFrame(animFrameId.current);
    };
  }, [state, emotion]);

  return (
    <div 
      className="relative flex flex-col items-center justify-center cursor-pointer group select-none"
      onClick={onClick}
      title="Tap to speak with H.E.N.R.Y."
    >
      <canvas
        ref={canvasRef}
        width={size}
        height={size}
        className="transition-transform duration-300 group-hover:scale-105"
      />
      <div className="absolute bottom-2 flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-[#031427]/80 border border-cyan-500/30 text-[10px] tracking-widest font-mono-hud text-cyan-300">
        <span className={`w-1.5 h-1.5 rounded-full ${
          state === 'LISTENING' ? 'bg-amber-400 animate-ping' :
          state === 'THINKING' ? 'bg-purple-400 animate-pulse' :
          state === 'SPEAKING' ? 'bg-emerald-400 animate-pulse' :
          'bg-cyan-400'
        }`} />
        <span>{state}</span>
      </div>
    </div>
  );
};
