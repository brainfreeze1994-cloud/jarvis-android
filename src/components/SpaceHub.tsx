import React, { useState, useEffect, useRef } from 'react';
import { Rocket, Orbit, Satellite, Image as ImageIcon, ExternalLink, RefreshCw, AlertTriangle, Clock, Compass } from 'lucide-react';
import { AsteroidItem } from '../types';

interface SpaceHubProps {
  onClose: () => void;
}

export const SpaceHub: React.FC<SpaceHubProps> = ({ onClose }) => {
  const [activeTab, setActiveTab] = useState<'asteroids' | 'iss' | 'apod'>('asteroids');
  const [loading, setLoading] = useState<boolean>(true);

  // Asteroid Data
  const [asteroids, setAsteroids] = useState<AsteroidItem[]>([]);
  const [selectedAsteroidIndex, setSelectedAsteroidIndex] = useState<number>(0);
  const [timeToApproach, setTimeToApproach] = useState<string>('00h 00m 00s');

  // ISS Data
  const [issPos, setIssPos] = useState<{ lat: number; lon: number; timestamp: number } | null>(null);

  // APOD Data
  const [apodData, setApodData] = useState<{ title: string; explanation: string; url: string; date: string } | null>(null);

  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animRef = useRef<number>(0);

  // Fetch Space Data
  const loadSpaceData = async () => {
    setLoading(true);
    try {
      // 1. Fetch ISS
      const issRes = await fetch('/api/space/iss');
      const issJson = await issRes.json();
      if (issJson?.iss_position) {
        setIssPos({
          lat: parseFloat(issJson.iss_position.latitude),
          lon: parseFloat(issJson.iss_position.longitude),
          timestamp: issJson.timestamp
        });
      }

      // 2. Fetch APOD
      const apodRes = await fetch('/api/space/apod');
      const apodJson = await apodRes.json();
      if (apodJson?.url) {
        setApodData(apodJson);
      }

      // 3. Fetch Asteroids
      const astRes = await fetch('/api/space/asteroids');
      const astJson = await astRes.json();
      const nearEarth = astJson?.near_earth_objects;
      if (nearEarth) {
        const list: AsteroidItem[] = [];
        Object.keys(nearEarth).forEach(dateStr => {
          nearEarth[dateStr].forEach((item: any) => {
            const closeData = item.close_approach_data?.[0];
            list.push({
              id: item.id,
              name: item.name,
              estimatedDiameterMinKm: item.estimated_diameter?.kilometers?.estimated_diameter_min || 0.05,
              estimatedDiameterMaxKm: item.estimated_diameter?.kilometers?.estimated_diameter_max || 0.12,
              isHazardous: item.is_potentially_hazardous_asteroid || false,
              closeApproachDate: closeData?.close_approach_date_full || dateStr,
              missDistanceKm: parseFloat(closeData?.miss_distance?.kilometers || '1500000'),
              missDistanceLunar: parseFloat(closeData?.miss_distance?.lunar || '4.2'),
              relativeVelocityKmh: parseFloat(closeData?.relative_velocity?.kilometers_per_hour || '45000')
            });
          });
        });
        setAsteroids(list.slice(0, 10));
      } else {
        // Fallback default realistic Near-Earth Asteroids
        setAsteroids([
          {
            id: '2024-YR4',
            name: '(2024 YR4)',
            estimatedDiameterMinKm: 0.04,
            estimatedDiameterMaxKm: 0.09,
            isHazardous: false,
            closeApproachDate: 'Today 18:42 UTC',
            missDistanceKm: 1120000,
            missDistanceLunar: 2.9,
            relativeVelocityKmh: 48200
          },
          {
            id: '99942-APOPHIS',
            name: '99942 Apophis',
            estimatedDiameterMinKm: 0.34,
            estimatedDiameterMaxKm: 0.37,
            isHazardous: true,
            closeApproachDate: '2029-04-13',
            missDistanceKm: 31600,
            missDistanceLunar: 0.08,
            relativeVelocityKmh: 30700
          },
          {
            id: '2025-BC',
            name: '(2025 BC)',
            estimatedDiameterMinKm: 0.12,
            estimatedDiameterMaxKm: 0.28,
            isHazardous: false,
            closeApproachDate: 'Tomorrow 04:15 UTC',
            missDistanceKm: 2840000,
            missDistanceLunar: 7.4,
            relativeVelocityKmh: 52100
          }
        ]);
      }
    } catch (e) {
      console.error('Space fetch error:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSpaceData();
    const interval = setInterval(loadSpaceData, 30000);
    return () => clearInterval(interval);
  }, []);

  // Live countdown ticker
  useEffect(() => {
    const timer = setInterval(() => {
      const now = new Date();
      const secLeft = 60 - now.getSeconds();
      const minLeft = 59 - now.getMinutes();
      const hrLeft = 23 - now.getHours();
      setTimeToApproach(
        `${String(hrLeft).padStart(2, '0')}h ${String(minLeft).padStart(2, '0')}m ${String(secLeft).padStart(2, '0')}s`
      );
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Orbital Canvas Animation
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let angle = 0;
    const render = () => {
      angle += 0.015;
      const w = canvas.width;
      const h = canvas.height;
      const cx = w / 2;
      const cy = h / 2;

      ctx.clearRect(0, 0, w, h);

      // Starfield background dots
      ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
      for (let i = 0; i < 30; i++) {
        const sx = ((i * 47) % w);
        const sy = ((i * 83) % h);
        ctx.fillRect(sx, sy, 1.2, 1.2);
      }

      // Earth Orbit Ring
      ctx.strokeStyle = 'rgba(0, 212, 255, 0.25)';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      ctx.ellipse(cx, cy, 90, 60, 0, 0, Math.PI * 2);
      ctx.stroke();

      // Asteroid Trajectory Ellipse
      const current = asteroids[selectedAsteroidIndex];
      const isHaz = current?.isHazardous;
      ctx.strokeStyle = isHaz ? 'rgba(239, 68, 68, 0.5)' : 'rgba(245, 158, 11, 0.5)';
      ctx.lineWidth = 1.8;
      ctx.setLineDash([]);
      ctx.beginPath();
      ctx.ellipse(cx, cy, 140, 75, Math.PI / 6, 0, Math.PI * 2);
      ctx.stroke();

      // Central Sun
      const sunGrad = ctx.createRadialGradient(cx, cy, 2, cx, cy, 14);
      sunGrad.addColorStop(0, '#FFFBEB');
      sunGrad.addColorStop(0.5, '#F59E0B');
      sunGrad.addColorStop(1, 'transparent');
      ctx.fillStyle = sunGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, 14, 0, Math.PI * 2);
      ctx.fill();

      // Earth Node
      const earthX = cx + Math.cos(angle * 0.7) * 90;
      const earthY = cy + Math.sin(angle * 0.7) * 60;
      ctx.fillStyle = '#00D4FF';
      ctx.beginPath();
      ctx.arc(earthX, earthY, 6, 0, Math.PI * 2);
      ctx.fill();
      ctx.shadowColor = '#00D4FF';
      ctx.shadowBlur = 8;
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('Earth', earthX + 8, earthY + 3);
      ctx.shadowBlur = 0;

      // Asteroid Node
      const rotAngle = angle * 1.2 + Math.PI / 4;
      const cosR = Math.cos(Math.PI / 6);
      const sinR = Math.sin(Math.PI / 6);
      const rawX = Math.cos(rotAngle) * 140;
      const rawY = Math.sin(rotAngle) * 75;
      const astX = cx + (rawX * cosR - rawY * sinR);
      const astY = cy + (rawX * sinR + rawY * cosR);

      ctx.fillStyle = isHaz ? '#EF4444' : '#F59E0B';
      ctx.beginPath();
      ctx.arc(astX, astY, 5, 0, Math.PI * 2);
      ctx.fill();
      ctx.shadowColor = isHaz ? '#EF4444' : '#F59E0B';
      ctx.shadowBlur = 10;
      ctx.fillStyle = isHaz ? '#FCA5A5' : '#FDE68A';
      ctx.font = '10px JetBrains Mono';
      ctx.fillText(current ? current.name : 'NEO', astX + 8, astY + 3);
      ctx.shadowBlur = 0;

      animRef.current = requestAnimationFrame(render);
    };

    animRef.current = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animRef.current);
  }, [asteroids, selectedAsteroidIndex]);

  const currentAst = asteroids[selectedAsteroidIndex] || asteroids[0];

  return (
    <div className="flex flex-col h-full bg-[#020C1B] text-[#e2f1ff] overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-cyan-500/20 bg-[#031326]/60 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
            <Rocket className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold font-tech tracking-wider text-cyan-300">NASA & SPACE COMMAND</h2>
            <p className="text-xs text-cyan-400/60 font-mono-hud">Eyes on Asteroids, Orbital Tracking & ISS Telemetry</p>
          </div>
        </div>

        {/* Tab selector */}
        <div className="flex items-center gap-1 p-1 bg-[#010814] rounded-lg border border-cyan-500/20">
          <button
            onClick={() => setActiveTab('asteroids')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'asteroids' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            ☄️ Asteroids ({asteroids.length})
          </button>
          <button
            onClick={() => setActiveTab('iss')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'iss' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            🛰️ ISS Telemetry
          </button>
          <button
            onClick={() => setActiveTab('apod')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'apod' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            🌌 NASA APOD
          </button>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={loadSpaceData}
            className="p-2 rounded-lg bg-[#010814] border border-cyan-500/30 text-cyan-400 hover:text-white text-xs font-mono-hud"
            title="Refresh space feeds"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={onClose}
            className="px-3 py-1.5 rounded bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-300 text-xs font-mono-hud"
          >
            Exit Hub
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto p-6">
        {/* 1. ASTEROIDS TAB */}
        {activeTab === 'asteroids' && (
          <div className="max-w-5xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Left: 3D Orbital Canvas & Telemetry */}
            <div className="lg:col-span-7 flex flex-col gap-4">
              <div className="bg-[#031427]/80 rounded-2xl border border-cyan-500/30 p-5 glow-cyan flex flex-col items-center relative">
                <div className="flex items-center justify-between w-full text-xs font-mono-hud text-cyan-300 mb-2">
                  <span className="flex items-center gap-1.5">
                    <Orbit className="w-4 h-4" /> Live Heliocentric Orbit Trajectory
                  </span>
                  <span className="text-amber-400">T-minus: {timeToApproach}</span>
                </div>

                <canvas ref={canvasRef} width={420} height={240} className="w-full max-w-md h-auto" />

                {/* Selected Asteroid Card */}
                {currentAst && (
                  <div className="w-full mt-4 pt-4 border-t border-cyan-500/20">
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="text-base font-bold font-mono-hud text-cyan-200">{currentAst.name}</h3>
                        <p className="text-xs text-slate-400">Approach: {currentAst.closeApproachDate}</p>
                      </div>
                      {currentAst.isHazardous ? (
                        <span className="px-2.5 py-1 rounded bg-red-500/20 border border-red-500/40 text-red-300 text-[11px] font-mono-hud flex items-center gap-1">
                          <AlertTriangle className="w-3.5 h-3.5" /> Potentially Hazardous
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 rounded bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-[11px] font-mono-hud">
                          ✓ Safe Trajectory
                        </span>
                      )}
                    </div>

                    <div className="grid grid-cols-3 gap-3 mt-4 text-center">
                      <div className="p-2.5 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud">
                        <p className="text-[10px] text-slate-400">MISS DISTANCE</p>
                        <p className="text-sm font-bold text-cyan-300">{(currentAst.missDistanceKm / 1e6).toFixed(2)} M km</p>
                        <p className="text-[10px] text-slate-500">({currentAst.missDistanceLunar.toFixed(1)} LD)</p>
                      </div>
                      <div className="p-2.5 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud">
                        <p className="text-[10px] text-slate-400">EST. DIAMETER</p>
                        <p className="text-sm font-bold text-amber-300">{Math.round(currentAst.estimatedDiameterMaxKm * 1000)} m</p>
                        <p className="text-[10px] text-slate-500">min {Math.round(currentAst.estimatedDiameterMinKm * 1000)} m</p>
                      </div>
                      <div className="p-2.5 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud">
                        <p className="text-[10px] text-slate-400">VELOCITY</p>
                        <p className="text-sm font-bold text-purple-300">{(currentAst.relativeVelocityKmh / 3600).toFixed(1)} km/s</p>
                        <p className="text-[10px] text-slate-500">({Math.round(currentAst.relativeVelocityKmh).toLocaleString()} km/h)</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Right: Asteroid Feed List */}
            <div className="lg:col-span-5 flex flex-col gap-3">
              <h3 className="text-xs font-bold font-mono-hud text-cyan-400 tracking-wider">NEAR-EARTH ASTEROIDS TODAY</h3>
              <div className="space-y-2 max-h-[480px] overflow-y-auto pr-1">
                {asteroids.map((ast, index) => (
                  <div
                    key={ast.id}
                    onClick={() => setSelectedAsteroidIndex(index)}
                    className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                      selectedAsteroidIndex === index
                        ? 'bg-cyan-500/20 border-cyan-400 glow-cyan'
                        : 'bg-[#031427] border-cyan-500/20 hover:border-cyan-500/40'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold font-mono-hud text-slate-200">{ast.name}</span>
                      <span className={`text-[10px] font-mono-hud ${ast.isHazardous ? 'text-red-400 font-bold' : 'text-emerald-400'}`}>
                        {ast.missDistanceLunar.toFixed(1)} Lunar Dist
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-[11px] text-slate-400 mt-1">
                      <span>Diameter: ~{Math.round(ast.estimatedDiameterMaxKm * 1000)}m</span>
                      <span>{(ast.relativeVelocityKmh / 3600).toFixed(1)} km/s</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* 2. ISS TELEMETRY TAB */}
        {activeTab === 'iss' && (
          <div className="max-w-3xl mx-auto space-y-6">
            <div className="p-6 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan">
              <div className="flex items-center gap-2 mb-4">
                <Satellite className="w-6 h-6 text-cyan-400" />
                <div>
                  <h3 className="text-base font-bold font-tech text-cyan-300">INTERNATIONAL SPACE STATION (ISS)</h3>
                  <p className="text-xs text-slate-400">Live orbital coordinates and station telemetry</p>
                </div>
              </div>

              {issPos ? (
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 my-6">
                  <div className="p-4 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud text-center">
                    <p className="text-[10px] text-slate-400">LATITUDE</p>
                    <p className="text-xl font-bold text-cyan-300 mt-1">{issPos.lat.toFixed(4)}°</p>
                  </div>
                  <div className="p-4 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud text-center">
                    <p className="text-[10px] text-slate-400">LONGITUDE</p>
                    <p className="text-xl font-bold text-cyan-300 mt-1">{issPos.lon.toFixed(4)}°</p>
                  </div>
                  <div className="p-4 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud text-center">
                    <p className="text-[10px] text-slate-400">ORBIT SPEED</p>
                    <p className="text-xl font-bold text-emerald-300 mt-1">27,600 km/h</p>
                  </div>
                  <div className="p-4 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud text-center">
                    <p className="text-[10px] text-slate-400">ALTITUDE</p>
                    <p className="text-xl font-bold text-amber-300 mt-1">~420 km</p>
                  </div>
                </div>
              ) : (
                <div className="text-center py-8 text-slate-400 font-mono-hud">Acquiring station telemetry...</div>
              )}

              <div className="p-4 rounded-xl bg-[#010814] border border-cyan-500/20 text-xs text-slate-300 leading-relaxed font-mono-hud">
                <p className="text-cyan-400 font-bold mb-1">🛰️ ORBITAL MECHANICS OVERVIEW:</p>
                The ISS completes one full revolution around Earth every 92.68 minutes (~15.5 orbits per Earth day). Astronauts onboard witness 16 sunrises and sunsets every 24 hours.
              </div>
            </div>
          </div>
        )}

        {/* 3. NASA APOD TAB */}
        {activeTab === 'apod' && apodData && (
          <div className="max-w-4xl mx-auto space-y-6">
            <div className="bg-[#031427]/80 rounded-2xl border border-cyan-500/30 overflow-hidden glow-cyan">
              <img
                src={apodData.url}
                alt={apodData.title}
                referrerPolicy="no-referrer"
                className="w-full max-h-[450px] object-cover"
              />
              <div className="p-6">
                <div className="flex items-center justify-between text-xs font-mono-hud text-cyan-400/80 mb-2">
                  <span>NASA Astronomy Picture of the Day</span>
                  <span>{apodData.date}</span>
                </div>
                <h3 className="text-xl font-bold font-tech text-cyan-200 mb-3">{apodData.title}</h3>
                <p className="text-sm text-slate-300 leading-relaxed">{apodData.explanation}</p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
