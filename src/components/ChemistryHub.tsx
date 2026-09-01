import React, { useState, useEffect, useRef } from 'react';
import { FlaskConical, Atom, Sparkles, Search, Layers } from 'lucide-react';
import { ChemicalElement } from '../types';

interface ChemistryHubProps {
  onClose: () => void;
}

const CATEGORY_COLORS: Record<string, { bg: string; border: string; text: string }> = {
  alkali: { bg: '#EF444422', border: '#EF4444', text: '#FCA5A5' },
  alkaline: { bg: '#F59E0B22', border: '#F59E0B', text: '#FDE68A' },
  transition: { bg: '#3B82F622', border: '#3B82F6', text: '#93C5FD' },
  'post-transition': { bg: '#10B98122', border: '#10B981', text: '#A7F3D0' },
  metalloid: { bg: '#06B6D422', border: '#06B6D4', text: '#A5F3FC' },
  nonmetal: { bg: '#8B5CF622', border: '#8B5CF6', text: '#DDD6FE' },
  halogen: { bg: '#EC489922', border: '#EC4899', text: '#FBCFE8' },
  noble: { bg: '#6366F122', border: '#6366F1', text: '#C7D2FE' },
  lanthanide: { bg: '#14B8A622', border: '#14B8A6', text: '#99F6E4' },
  actinide: { bg: '#F9731622', border: '#F97316', text: '#FED7AA' }
};

// Initial Core Periodic Table Elements dataset
const ELEMENTS: ChemicalElement[] = [
  { number: 1, symbol: 'H', name: 'Hydrogen', atomicMass: 1.008, category: 'nonmetal', summary: 'The lightest element, fueling the stars and forming the foundation of water and organic life.', electronConfiguration: '1s¹', meltingPoint: 14.01, boilingPoint: 20.28 },
  { number: 2, symbol: 'He', name: 'Helium', atomicMass: 4.0026, category: 'noble', summary: 'Colorless, odorless noble gas produced in stellar fusion; inert and non-reactive.', electronConfiguration: '1s²', meltingPoint: 0.95, boilingPoint: 4.22 },
  { number: 3, symbol: 'Li', name: 'Lithium', atomicMass: 6.94, category: 'alkali', summary: 'Soft, silvery alkali metal with the lowest density of all solid elements. Critical for battery energy storage.', electronConfiguration: '[He] 2s¹', meltingPoint: 453.65, boilingPoint: 1603 },
  { number: 4, symbol: 'Be', name: 'Beryllium', atomicMass: 9.0122, category: 'alkaline', summary: 'Lightweight, strong alkaline earth metal used in aerospace optics and the James Webb Space Telescope mirrors.', electronConfiguration: '[He] 2s²', meltingPoint: 1560, boilingPoint: 2742 },
  { number: 5, symbol: 'B', name: 'Boron', atomicMass: 10.81, category: 'metalloid', summary: 'Hard metalloid found in meteoroids and pyrotechnics (green flares).', electronConfiguration: '[He] 2s² 2p¹', meltingPoint: 2349, boilingPoint: 4200 },
  { number: 6, symbol: 'C', name: 'Carbon', atomicMass: 12.011, category: 'nonmetal', summary: 'The fundamental chemical keystone of organic life and polymers, forming diamond, graphite, and graphene.', electronConfiguration: '[He] 2s² 2p²', meltingPoint: 3800, boilingPoint: 4300 },
  { number: 7, symbol: 'N', name: 'Nitrogen', atomicMass: 14.007, category: 'nonmetal', summary: 'Makes up 78% of Earth’s atmosphere; essential component of amino acids and nucleic acids.', electronConfiguration: '[He] 2s² 2p³', meltingPoint: 63.15, boilingPoint: 77.36 },
  { number: 8, symbol: 'O', name: 'Oxygen', atomicMass: 15.999, category: 'nonmetal', summary: 'Vital for aerobic cellular respiration, ozone layer protection, and water chemistry.', electronConfiguration: '[He] 2s² 2p⁴', meltingPoint: 54.36, boilingPoint: 90.2 },
  { number: 9, symbol: 'F', name: 'Fluorine', atomicMass: 18.998, category: 'halogen', summary: 'Extremely reactive halogen; the most electronegative element in the periodic table.', electronConfiguration: '[He] 2s² 2p⁵', meltingPoint: 53.53, boilingPoint: 85.03 },
  { number: 10, symbol: 'Ne', name: 'Neon', atomicMass: 20.18, category: 'noble', summary: 'Inert noble gas that gives a vivid orange-red glow in high-voltage discharge tubes.', electronConfiguration: '[He] 2s² 2p⁶', meltingPoint: 24.56, boilingPoint: 27.07 },
  { number: 11, symbol: 'Na', name: 'Sodium', atomicMass: 22.99, category: 'alkali', summary: 'Highly reactive alkali metal essential for biological nerve transmission and fluid regulation.', electronConfiguration: '[Ne] 3s¹', meltingPoint: 370.87, boilingPoint: 1156 },
  { number: 12, symbol: 'Mg', name: 'Magnesium', atomicMass: 24.305, category: 'alkaline', summary: 'Light structural metal and central ion in chlorophyll for photosynthesis.', electronConfiguration: '[Ne] 3s²', meltingPoint: 923, boilingPoint: 1363 },
  { number: 13, symbol: 'Al', name: 'Aluminum', atomicMass: 26.982, category: 'post-transition', summary: 'Corrosion-resistant, low-density metal forming the backbone of aerospace engineering.', electronConfiguration: '[Ne] 3s² 3p¹', meltingPoint: 933.47, boilingPoint: 2792 },
  { number: 14, symbol: 'Si', name: 'Silicon', atomicMass: 28.085, category: 'metalloid', summary: 'Semiconductor basis of all modern computing chips and Earth crust minerals.', electronConfiguration: '[Ne] 3s² 3p²', meltingPoint: 1687, boilingPoint: 3538 },
  { number: 15, symbol: 'P', name: 'Phosphorus', atomicMass: 30.974, category: 'nonmetal', summary: 'Core constituent of DNA, RNA, and ATP energy transport in living cells.', electronConfiguration: '[Ne] 3s² 3p³', meltingPoint: 317.3, boilingPoint: 553.6 },
  { number: 16, symbol: 'S', name: 'Sulfur', atomicMass: 32.06, category: 'nonmetal', summary: 'Bright yellow nonmetal with a pungent smell, essential in proteins.', electronConfiguration: '[Ne] 3s² 3p⁴', meltingPoint: 388.36, boilingPoint: 717.8 },
  { number: 26, symbol: 'Fe', name: 'Iron', atomicMass: 55.845, category: 'transition', summary: 'Most abundant element by mass on Earth, core element in hemoglobin oxygen transport.', electronConfiguration: '[Ar] 3d⁶ 4s²', meltingPoint: 1811, boilingPoint: 3134 },
  { number: 29, symbol: 'Cu', name: 'Copper', atomicMass: 63.546, category: 'transition', summary: 'Ductile metal with very high thermal and electrical conductivity.', electronConfiguration: '[Ar] 3d¹⁰ 4s¹', meltingPoint: 1357.77, boilingPoint: 2835 },
  { number: 47, symbol: 'Ag', name: 'Silver', atomicMass: 107.87, category: 'transition', summary: 'Highest electrical conductivity, thermal conductivity, and reflectivity of any metal.', electronConfiguration: '[Kr] 4d¹⁰ 5s¹', meltingPoint: 1234.93, boilingPoint: 2435 },
  { number: 79, symbol: 'Au', name: 'Gold', atomicMass: 196.97, category: 'transition', summary: 'Dense, noble metal resistant to corrosion and tarnishing; formed in neutron star collisions.', electronConfiguration: '[Xe] 4f¹⁴ 5d¹⁰ 6s¹', meltingPoint: 1337.33, boilingPoint: 3129 },
  { number: 92, symbol: 'U', name: 'Uranium', atomicMass: 238.03, category: 'actinide', summary: 'Radioactive actinide with slow radioactive decay, powering nuclear fission reactors.', electronConfiguration: '[Rn] 5f³ 6d¹ 7s²', meltingPoint: 1405.3, boilingPoint: 4404 }
];

export const ChemistryHub: React.FC<ChemistryHubProps> = ({ onClose }) => {
  const [selectedElement, setSelectedElement] = useState<ChemicalElement>(ELEMENTS[0]);
  const [filterCategory, setFilterCategory] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animRef = useRef<number>(0);

  // Bohr Atom visualizer animation
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let angle = 0;
    const render = () => {
      angle += 0.02;
      const w = canvas.width;
      const h = canvas.height;
      const cx = w / 2;
      const cy = h / 2;

      ctx.clearRect(0, 0, w, h);

      // Nucleus
      const nucleusGrad = ctx.createRadialGradient(cx, cy, 2, cx, cy, 16);
      nucleusGrad.addColorStop(0, '#FFFFFF');
      nucleusGrad.addColorStop(0.4, '#EF4444');
      nucleusGrad.addColorStop(1, '#7F1D1D');
      ctx.fillStyle = nucleusGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, 16, 0, Math.PI * 2);
      ctx.fill();

      // Protons/Neutrons count text
      ctx.fillStyle = '#FFFFFF';
      ctx.font = 'bold 9px JetBrains Mono';
      ctx.textAlign = 'center';
      ctx.fillText(`${selectedElement.number}p+`, cx, cy + 3);

      // Shells & Orbiting Electrons
      const shellCount = Math.min(4, Math.ceil(selectedElement.number / 4));
      for (let s = 1; s <= shellCount; s++) {
        const radius = 28 + s * 22;

        // Shell orbit ring
        ctx.strokeStyle = 'rgba(0, 212, 255, 0.35)';
        ctx.lineWidth = 1.2;
        ctx.setLineDash([3, 3]);
        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.stroke();

        // Electrons on this shell
        const electronsOnShell = s === 1 ? Math.min(2, selectedElement.number) : Math.min(8, selectedElement.number);
        for (let e = 0; e < electronsOnShell; e++) {
          const eAngle = angle * (s % 2 === 0 ? 1 : -1) + (e * Math.PI * 2) / electronsOnShell;
          const ex = cx + Math.cos(eAngle) * radius;
          const ey = cy + Math.sin(eAngle) * radius;

          ctx.fillStyle = '#00D4FF';
          ctx.beginPath();
          ctx.arc(ex, ey, 4, 0, Math.PI * 2);
          ctx.fill();
          ctx.shadowColor = '#00D4FF';
          ctx.shadowBlur = 8;
        }
        ctx.shadowBlur = 0;
      }

      animRef.current = requestAnimationFrame(render);
    };

    animRef.current = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animRef.current);
  }, [selectedElement]);

  const filteredElements = ELEMENTS.filter((el) => {
    const matchCat = filterCategory === 'all' || el.category === filterCategory;
    const matchSearch =
      el.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      el.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
      String(el.number) === searchQuery.trim();
    return matchCat && matchSearch;
  });

  return (
    <div className="flex flex-col h-full bg-[#020C1B] text-[#e2f1ff] overflow-hidden">
      {/* Top Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-cyan-500/20 bg-[#031326]/60 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
            <FlaskConical className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold font-tech tracking-wider text-cyan-300">PERIODIC TABLE & ATOMIC STRUCTURE</h2>
            <p className="text-xs text-cyan-400/60 font-mono-hud">Chemical Elements, Quantum Configurations & Bohr Atom Visualizer</p>
          </div>
        </div>

        <button
          onClick={onClose}
          className="px-3 py-1.5 rounded bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-300 text-xs font-mono-hud"
        >
          Exit Chemistry
        </button>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto p-6">
        <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left Column: Periodic Grid & Search */}
          <div className="lg:col-span-8 flex flex-col gap-4">
            {/* Search & Category Filter */}
            <div className="flex flex-wrap gap-2 items-center">
              <div className="relative flex-1 min-w-[200px]">
                <Search className="w-4 h-4 text-cyan-400 absolute left-3 top-3" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search element name, symbol, or atomic #..."
                  className="w-full pl-9 pr-4 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 focus:outline-none focus:border-cyan-400"
                />
              </div>

              <select
                value={filterCategory}
                onChange={(e) => setFilterCategory(e.target.value)}
                className="px-3 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 focus:outline-none"
              >
                <option value="all">All Categories</option>
                <option value="nonmetal">Nonmetals</option>
                <option value="noble">Noble Gases</option>
                <option value="alkali">Alkali Metals</option>
                <option value="alkaline">Alkaline Earth</option>
                <option value="transition">Transition Metals</option>
                <option value="metalloid">Metalloids</option>
                <option value="halogen">Halogens</option>
                <option value="actinide">Actinides</option>
              </select>
            </div>

            {/* Elements Grid */}
            <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2.5">
              {filteredElements.map((el) => {
                const catStyle = CATEGORY_COLORS[el.category] || CATEGORY_COLORS.nonmetal;
                const isSelected = selectedElement.number === el.number;
                return (
                  <div
                    key={el.number}
                    onClick={() => setSelectedElement(el)}
                    className={`p-3 rounded-xl border cursor-pointer transition-all flex flex-col items-center justify-between font-mono-hud relative ${
                      isSelected ? 'border-white scale-105 shadow-lg shadow-cyan-500/30' : 'hover:scale-102'
                    }`}
                    style={{
                      backgroundColor: catStyle.bg,
                      borderColor: isSelected ? '#FFFFFF' : catStyle.border
                    }}
                  >
                    <span className="text-[10px] text-slate-400 self-start">{el.number}</span>
                    <span className="text-2xl font-bold my-1" style={{ color: catStyle.text }}>
                      {el.symbol}
                    </span>
                    <span className="text-[10px] text-slate-200 truncate w-full text-center">{el.name}</span>
                    <span className="text-[9px] text-slate-400">{el.atomicMass.toFixed(2)}</span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right Column: Selected Element Inspector & Bohr Canvas */}
          <div className="lg:col-span-4 flex flex-col gap-4">
            <div className="bg-[#031427]/90 rounded-2xl border border-cyan-500/30 p-5 glow-cyan flex flex-col items-center text-center">
              <div className="flex items-center justify-between w-full text-xs font-mono-hud text-cyan-400/80 mb-2">
                <span>Atomic Inspector</span>
                <span className="capitalize">{selectedElement.category}</span>
              </div>

              {/* Bohr Visualizer */}
              <canvas ref={canvasRef} width={220} height={200} className="my-2" />

              <h3 className="text-xl font-bold font-tech text-cyan-200 mt-2">
                {selectedElement.name} ({selectedElement.symbol})
              </h3>
              <p className="text-xs text-slate-400 font-mono-hud mb-4">Atomic Number: {selectedElement.number}</p>

              <div className="w-full text-left space-y-2 border-t border-cyan-500/20 pt-3 text-xs font-mono-hud">
                <div className="flex justify-between py-1 border-b border-white/5">
                  <span className="text-slate-400">Atomic Mass:</span>
                  <span className="text-cyan-300 font-bold">{selectedElement.atomicMass} u</span>
                </div>
                <div className="flex justify-between py-1 border-b border-white/5">
                  <span className="text-slate-400">Electron Config:</span>
                  <span className="text-purple-300">{selectedElement.electronConfiguration}</span>
                </div>
                {selectedElement.meltingPoint && (
                  <div className="flex justify-between py-1 border-b border-white/5">
                    <span className="text-slate-400">Melting Point:</span>
                    <span className="text-amber-300">{selectedElement.meltingPoint} K</span>
                  </div>
                )}
                {selectedElement.boilingPoint && (
                  <div className="flex justify-between py-1 border-b border-white/5">
                    <span className="text-slate-400">Boiling Point:</span>
                    <span className="text-emerald-300">{selectedElement.boilingPoint} K</span>
                  </div>
                )}
              </div>

              <p className="text-xs text-slate-300 leading-relaxed text-left mt-4 p-3 rounded-xl bg-[#010814] border border-cyan-500/20">
                {selectedElement.summary}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
