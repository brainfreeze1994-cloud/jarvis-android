import React, { useState, useEffect } from 'react';
import { Globe, Activity, CloudSun, AlertCircle, RefreshCw, Search, Wind, Droplets, Sun, Compass } from 'lucide-react';
import { EarthquakeItem } from '../types';

interface EarthRadarHubProps {
  onClose: () => void;
}

export const EarthRadarHub: React.FC<EarthRadarHubProps> = ({ onClose }) => {
  const [activeTab, setActiveTab] = useState<'quakes' | 'weather' | 'intel'>('quakes');
  const [loading, setLoading] = useState<boolean>(true);

  // Earthquakes
  const [quakes, setQuakes] = useState<EarthquakeItem[]>([]);
  const [quakeHeader, setQuakeHeader] = useState<string>('Scanning USGS seismic network...');

  // Weather
  const [cityInput, setCityInput] = useState<string>('Dubai');
  const [weatherData, setWeatherData] = useState<any>(null);
  const [weatherLoading, setWeatherLoading] = useState<boolean>(false);

  // World Intel
  const WORLD_INTEL = [
    { region: 'Global Seismicity', status: 'Moderate', desc: 'Active subduction along the Pacific Ring of Fire.' },
    { region: 'Atmospheric Jet Stream', status: 'Normal Flow', desc: 'Polar vortex contained in upper northern latitudes.' },
    { region: 'Geomagnetic Field', status: 'Kp 2 (Quiet)', desc: 'Solar wind speeds normal (~380 km/s). No major storm warnings.' },
    { region: 'Oceanic Temperature Anomaly', status: 'ENSO Neutral', desc: 'Equatorial Pacific surface temperatures near 30-year baseline.' }
  ];

  const fetchQuakes = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/earthquakes');
      const data = await res.json();
      if (data?.features) {
        const list: EarthquakeItem[] = data.features.map((f: any) => ({
          id: f.id,
          magnitude: f.properties?.mag || 0,
          place: f.properties?.place || 'Unknown Location',
          time: f.properties?.time || Date.now(),
          url: f.properties?.url || 'https://earthquake.usgs.gov',
          tsunami: f.properties?.tsunami || 0,
          depth: f.geometry?.coordinates?.[2] || 10
        }));
        setQuakes(list);
        setQuakeHeader(`${list.length} Significant Global Earthquakes Detected (Past 7 Days)`);
      } else {
        // Sample recent significant earthquakes
        setQuakes([
          { id: 'q1', magnitude: 6.8, place: 'Near coast of Central Chile', time: Date.now() - 3600000 * 5, url: '', tsunami: 0, depth: 32 },
          { id: 'q2', magnitude: 6.2, place: 'Southern Mid-Atlantic Ridge', time: Date.now() - 3600000 * 18, url: '', tsunami: 0, depth: 10 },
          { id: 'q3', magnitude: 5.9, place: '124 km E of Tokyo, Japan', time: Date.now() - 3600000 * 36, url: '', tsunami: 0, depth: 45 },
          { id: 'q4', magnitude: 5.7, place: 'Mindanao, Philippines', time: Date.now() - 3600000 * 52, url: '', tsunami: 0, depth: 68 }
        ]);
        setQuakeHeader('USGS Significant Global Earthquakes (Simulated Feed)');
      }
    } catch (e) {
      console.error('Earthquake fetch error:', e);
    } finally {
      setLoading(false);
    }
  };

  const fetchWeather = async (city: string) => {
    setWeatherLoading(true);
    try {
      const res = await fetch(`/api/weather?city=${encodeURIComponent(city)}`);
      const data = await res.json();
      setWeatherData(data);
    } catch (e) {
      console.error('Weather error:', e);
    } finally {
      setWeatherLoading(false);
    }
  };

  useEffect(() => {
    fetchQuakes();
    fetchWeather('Dubai');
  }, []);

  const currentWeather = weatherData?.current_condition?.[0];
  const nearestArea = weatherData?.nearest_area?.[0];

  return (
    <div className="flex flex-col h-full bg-[#020C1B] text-[#e2f1ff] overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-cyan-500/20 bg-[#031326]/60 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
            <Globe className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold font-tech tracking-wider text-cyan-300">EARTH RADAR & PLANETARY TELEMETRY</h2>
            <p className="text-xs text-cyan-400/60 font-mono-hud">USGS Live Seismicity, Global Weather & Intelligence</p>
          </div>
        </div>

        {/* Tab switcher */}
        <div className="flex items-center gap-1 p-1 bg-[#010814] rounded-lg border border-cyan-500/20">
          <button
            onClick={() => setActiveTab('quakes')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'quakes' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            🌋 Earthquakes ({quakes.length})
          </button>
          <button
            onClick={() => setActiveTab('weather')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'weather' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            ⛅ Global Weather
          </button>
          <button
            onClick={() => setActiveTab('intel')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'intel' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            🌐 Planetary Intel
          </button>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchQuakes}
            className="p-2 rounded-lg bg-[#010814] border border-cyan-500/30 text-cyan-400 hover:text-white"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={onClose}
            className="px-3 py-1.5 rounded bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-300 text-xs font-mono-hud"
          >
            Exit Radar
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto p-6">
        {/* 1. SEISMIC QUAKES TAB */}
        {activeTab === 'quakes' && (
          <div className="max-w-4xl mx-auto space-y-4">
            <div className="flex items-center justify-between p-4 rounded-xl bg-[#031427]/80 border border-cyan-500/30 font-mono-hud text-xs text-cyan-300">
              <span className="flex items-center gap-2">
                <Activity className="w-4 h-4 text-red-400" />
                {quakeHeader}
              </span>
              <a
                href="https://earthquake.usgs.gov/earthquakes/map/"
                target="_blank"
                rel="noreferrer"
                className="text-cyan-400 hover:underline flex items-center gap-1"
              >
                Official USGS Feed ↗
              </a>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {quakes.map((q) => {
                const isHigh = q.magnitude >= 6.5;
                const isMed = q.magnitude >= 5.5;
                return (
                  <div
                    key={q.id}
                    className={`p-4 rounded-xl border transition-all ${
                      isHigh
                        ? 'bg-red-950/30 border-red-500/40 glow-cyan'
                        : isMed
                        ? 'bg-amber-950/20 border-amber-500/30'
                        : 'bg-[#031427]/60 border-cyan-500/20'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <span
                          className={`text-2xl font-bold font-mono-hud px-2.5 py-1 rounded-lg ${
                            isHigh ? 'bg-red-500/20 text-red-400 border border-red-500/50' : 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40'
                          }`}
                        >
                          M{q.magnitude.toFixed(1)}
                        </span>
                        <div>
                          <h4 className="text-sm font-semibold text-slate-200">{q.place}</h4>
                          <p className="text-[11px] text-slate-400 font-mono-hud">
                            {new Date(q.time).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                          </p>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-4 mt-3 pt-2 border-t border-white/5 text-[11px] font-mono-hud text-slate-400">
                      <span>Depth: {q.depth} km</span>
                      {q.tsunami === 1 && <span className="text-amber-400 font-bold">⚠️ Tsunami Warning Issued</span>}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 2. GLOBAL WEATHER TAB */}
        {activeTab === 'weather' && (
          <div className="max-w-3xl mx-auto space-y-6">
            {/* City search */}
            <div className="flex gap-2">
              <input
                type="text"
                value={cityInput}
                onChange={(e) => setCityInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && fetchWeather(cityInput)}
                placeholder="Enter city (e.g. Dubai, Tokyo, London, New York)..."
                className="flex-1 px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 focus:outline-none focus:border-cyan-400"
              />
              <button
                onClick={() => fetchWeather(cityInput)}
                className="px-5 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold flex items-center gap-1.5"
              >
                <Search className="w-4 h-4" /> Scan Atmosphere
              </button>
            </div>

            {/* Weather Result Card */}
            {currentWeather ? (
              <div className="p-6 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan">
                <div className="flex items-center justify-between border-b border-cyan-500/20 pb-4">
                  <div>
                    <h3 className="text-2xl font-bold font-tech text-cyan-300">
                      {nearestArea?.areaName?.[0]?.value || cityInput}, {nearestArea?.country?.[0]?.value}
                    </h3>
                    <p className="text-xs text-slate-400 font-mono-hud">{currentWeather.weatherDesc?.[0]?.value}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-4xl font-bold font-mono-hud text-cyan-200">{currentWeather.temp_C}°C</p>
                    <p className="text-xs text-slate-400 font-mono-hud">Feels like {currentWeather.FeelsLikeC}°C ({currentWeather.temp_F}°F)</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
                  <div className="p-3.5 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud text-center">
                    <Wind className="w-5 h-5 text-cyan-400 mx-auto mb-1" />
                    <p className="text-[10px] text-slate-400">WIND</p>
                    <p className="text-sm font-bold text-slate-200">{currentWeather.windspeedKmph} km/h</p>
                    <p className="text-[10px] text-slate-500">{currentWeather.winddir16Point}</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud text-center">
                    <Droplets className="w-5 h-5 text-cyan-400 mx-auto mb-1" />
                    <p className="text-[10px] text-slate-400">HUMIDITY</p>
                    <p className="text-sm font-bold text-slate-200">{currentWeather.humidity}%</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud text-center">
                    <Sun className="w-5 h-5 text-amber-400 mx-auto mb-1" />
                    <p className="text-[10px] text-slate-400">UV INDEX</p>
                    <p className="text-sm font-bold text-amber-300">{currentWeather.uvIndex}</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-[#010814] border border-cyan-500/20 font-mono-hud text-center">
                    <Compass className="w-5 h-5 text-purple-400 mx-auto mb-1" />
                    <p className="text-[10px] text-slate-400">PRESSURE</p>
                    <p className="text-sm font-bold text-slate-200">{currentWeather.pressure} hPa</p>
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center py-10 text-slate-500 font-mono-hud text-xs">
                {weatherLoading ? 'Interfacing with meteorological sensors...' : 'Search any city above to view atmosphere telemetry.'}
              </div>
            )}
          </div>
        )}

        {/* 3. WORLD INTEL TAB */}
        {activeTab === 'intel' && (
          <div className="max-w-3xl mx-auto space-y-4">
            <h3 className="text-xs font-bold font-mono-hud text-cyan-400 tracking-wider">GLOBAL GEOPHYSICAL STATUS</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {WORLD_INTEL.map((item, idx) => (
                <div key={idx} className="p-4 rounded-xl bg-[#031427]/70 border border-cyan-500/20">
                  <div className="flex items-center justify-between mb-2">
                    <h4 className="text-sm font-bold font-tech text-cyan-200">{item.region}</h4>
                    <span className="px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-300 text-[10px] font-mono-hud">
                      {item.status}
                    </span>
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed">{item.desc}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
