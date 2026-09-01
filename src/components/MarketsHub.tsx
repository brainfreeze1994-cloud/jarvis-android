import React, { useState, useEffect } from 'react';
import { TrendingUp, DollarSign, Bitcoin, ArrowUpDown, RefreshCw, Search } from 'lucide-react';
import { StockItem, CryptoItem } from '../types';

interface MarketsHubProps {
  onClose: () => void;
}

export const MarketsHub: React.FC<MarketsHubProps> = ({ onClose }) => {
  const [activeTab, setActiveTab] = useState<'stocks' | 'crypto' | 'forex'>('stocks');
  const [loading, setLoading] = useState<boolean>(true);

  // Stocks
  const [stocks, setStocks] = useState<StockItem[]>([]);
  const [customSymbol, setCustomSymbol] = useState<string>('');

  // Crypto
  const [cryptos, setCryptos] = useState<CryptoItem[]>([]);

  // Forex & Currency Converter
  const [forexRates, setForexRates] = useState<Record<string, number>>({});
  const [calcAmount, setCalcAmount] = useState<number>(100);
  const [calcFrom, setCalcFrom] = useState<string>('USD');
  const [calcTo, setCalcTo] = useState<string>('AED');

  const fetchMarkets = async () => {
    setLoading(true);
    try {
      // 1. Fetch Stocks
      const stockRes = await fetch('/api/stocks?symbols=AAPL,TSLA,NVDA,GOOGL,MSFT,AMZN,META');
      const stockJson = await stockRes.json();
      if (stockJson?.stocks?.length) {
        setStocks(stockJson.stocks);
      } else {
        setStocks([
          { symbol: 'NVDA', price: 124.50, change: 3.85, changePercent: 3.19, currency: 'USD', exchange: 'NASDAQ' },
          { symbol: 'AAPL', price: 231.20, change: 1.40, changePercent: 0.61, currency: 'USD', exchange: 'NASDAQ' },
          { symbol: 'TSLA', price: 218.40, change: -4.20, changePercent: -1.89, currency: 'USD', exchange: 'NASDAQ' },
          { symbol: 'GOOGL', price: 182.90, change: 2.10, changePercent: 1.16, currency: 'USD', exchange: 'NASDAQ' },
          { symbol: 'MSFT', price: 412.30, change: 0.80, changePercent: 0.19, currency: 'USD', exchange: 'NASDAQ' }
        ]);
      }

      // 2. Fetch Crypto
      const cryptoRes = await fetch('/api/crypto');
      const cryptoJson = await cryptoRes.json();
      if (cryptoJson && typeof cryptoJson === 'object') {
        const list: CryptoItem[] = Object.keys(cryptoJson).map(key => ({
          id: key,
          symbol: key.toUpperCase().slice(0, 4),
          name: key.charAt(0).toUpperCase() + key.slice(1).replace('-', ' '),
          usd: cryptoJson[key].usd || 0,
          usd_24h_change: cryptoJson[key].usd_24h_change || 0
        }));
        if (list.length) setCryptos(list);
      } else {
        setCryptos([
          { id: 'btc', symbol: 'BTC', name: 'Bitcoin', usd: 92450, usd_24h_change: 2.45 },
          { id: 'eth', symbol: 'ETH', name: 'Ethereum', usd: 2780, usd_24h_change: 1.15 },
          { id: 'sol', symbol: 'SOL', name: 'Solana', usd: 195.4, usd_24h_change: 4.80 },
          { id: 'bnb', symbol: 'BNB', name: 'Binance Coin', usd: 645.2, usd_24h_change: -0.85 }
        ]);
      }

      // 3. Fetch Forex
      const forexRes = await fetch('/api/forex?base=USD');
      const forexJson = await forexRes.json();
      if (forexJson?.rates) {
        setForexRates(forexJson.rates);
      } else {
        setForexRates({
          USD: 1,
          EUR: 0.92,
          GBP: 0.79,
          AED: 3.67,
          PHP: 57.8,
          JPY: 154.2,
          CAD: 1.38,
          AUD: 1.52,
          INR: 84.1
        });
      }
    } catch (e) {
      console.error('Market fetch error:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMarkets();
    const interval = setInterval(fetchMarkets, 45000);
    return () => clearInterval(interval);
  }, []);

  const handleAddCustomStock = async () => {
    if (!customSymbol.trim()) return;
    try {
      const r = await fetch(`/api/stocks?symbols=${encodeURIComponent(customSymbol.trim().toUpperCase())}`);
      const d = await r.json();
      if (d?.stocks?.length) {
        setStocks(prev => [d.stocks[0], ...prev.filter(s => s.symbol !== d.stocks[0].symbol)]);
        setCustomSymbol('');
      }
    } catch {}
  };

  // Convert calculation
  const calculateConversion = () => {
    const rateFrom = forexRates[calcFrom] || 1;
    const rateTo = forexRates[calcTo] || 1;
    const inUsd = calcAmount / rateFrom;
    return (inUsd * rateTo).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  return (
    <div className="flex flex-col h-full bg-[#020C1B] text-[#e2f1ff] overflow-hidden">
      {/* Top Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-cyan-500/20 bg-[#031326]/60 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
            <TrendingUp className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold font-tech tracking-wider text-cyan-300">MARKETS & FINANCIAL INTELLIGENCE</h2>
            <p className="text-xs text-cyan-400/60 font-mono-hud">Live Equities, Crypto Assets & Global Currency Rates</p>
          </div>
        </div>

        {/* Tab switcher */}
        <div className="flex items-center gap-1 p-1 bg-[#010814] rounded-lg border border-cyan-500/20">
          <button
            onClick={() => setActiveTab('stocks')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'stocks' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            📈 Stocks
          </button>
          <button
            onClick={() => setActiveTab('crypto')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'crypto' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            🪙 Crypto
          </button>
          <button
            onClick={() => setActiveTab('forex')}
            className={`px-3 py-1.5 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'forex' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400 hover:text-white'
            }`}
          >
            💱 Forex & Converter
          </button>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchMarkets}
            className="p-2 rounded-lg bg-[#010814] border border-cyan-500/30 text-cyan-400 hover:text-white text-xs font-mono-hud"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={onClose}
            className="px-3 py-1.5 rounded bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-300 text-xs font-mono-hud"
          >
            Exit Markets
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto p-6">
        {/* 1. STOCKS TAB */}
        {activeTab === 'stocks' && (
          <div className="max-w-4xl mx-auto space-y-4">
            {/* Search Symbol */}
            <div className="flex gap-2 mb-4">
              <input
                type="text"
                value={customSymbol}
                onChange={(e) => setCustomSymbol(e.target.value.toUpperCase())}
                onKeyDown={(e) => e.key === 'Enter' && handleAddCustomStock()}
                placeholder="Lookup ticker symbol (e.g. AMD, NFLX, PLTR, BTC-USD)..."
                className="flex-1 px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 focus:outline-none focus:border-cyan-400 uppercase"
              />
              <button
                onClick={handleAddCustomStock}
                className="px-5 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold flex items-center gap-1.5"
              >
                <Search className="w-4 h-4" /> Add Ticker
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
              {stocks.map((stock) => {
                const isPos = stock.change >= 0;
                return (
                  <div
                    key={stock.symbol}
                    className="p-4 rounded-xl bg-[#031427]/80 border border-cyan-500/20 hover:border-cyan-500/50 transition-all font-mono-hud"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-base font-bold text-slate-100">{stock.symbol}</span>
                      <span className="text-[10px] text-slate-500">{stock.exchange}</span>
                    </div>

                    <div className="flex items-baseline justify-between mt-3">
                      <span className="text-xl font-bold text-cyan-200">
                        ${stock.price.toFixed(2)}
                      </span>
                      <span
                        className={`text-xs font-bold px-2 py-0.5 rounded ${
                          isPos ? 'bg-emerald-500/20 text-emerald-300' : 'bg-red-500/20 text-red-300'
                        }`}
                      >
                        {isPos ? '+' : ''}{stock.changePercent.toFixed(2)}%
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 2. CRYPTO TAB */}
        {activeTab === 'crypto' && (
          <div className="max-w-4xl mx-auto space-y-3">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {cryptos.map((c) => {
                const isPos = c.usd_24h_change >= 0;
                return (
                  <div
                    key={c.id}
                    className="p-4 rounded-xl bg-[#031427]/80 border border-cyan-500/20 flex items-center justify-between font-mono-hud"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 font-bold text-xs">
                        {c.symbol.slice(0, 3)}
                      </div>
                      <div>
                        <h4 className="text-sm font-bold text-slate-200">{c.name}</h4>
                        <p className="text-[10px] text-slate-500">{c.symbol}</p>
                      </div>
                    </div>

                    <div className="text-right">
                      <p className="text-base font-bold text-slate-100">
                        ${c.usd > 1 ? c.usd.toLocaleString('en-US', { minimumFractionDigits: 2 }) : c.usd.toFixed(4)}
                      </p>
                      <p className={`text-xs ${isPos ? 'text-emerald-400' : 'text-red-400'}`}>
                        {isPos ? '▲' : '▼'} {Math.abs(c.usd_24h_change).toFixed(2)}%
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 3. FOREX & CURRENCY CONVERTER */}
        {activeTab === 'forex' && (
          <div className="max-w-3xl mx-auto space-y-6">
            <div className="p-6 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan">
              <div className="flex items-center gap-2 mb-4">
                <DollarSign className="w-5 h-5 text-cyan-400" />
                <h3 className="text-base font-bold font-tech text-cyan-300">REAL-TIME FOREX CONVERSION ENGINE</h3>
              </div>

              {/* Converter Form */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div>
                  <label className="block text-[10px] font-mono-hud text-slate-400 mb-1">AMOUNT</label>
                  <input
                    type="number"
                    value={calcAmount}
                    onChange={(e) => setCalcAmount(parseFloat(e.target.value) || 0)}
                    className="w-full px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-base font-mono-hud text-slate-200 focus:outline-none focus:border-cyan-400"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-mono-hud text-slate-400 mb-1">FROM CURRENCY</label>
                  <select
                    value={calcFrom}
                    onChange={(e) => setCalcFrom(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 focus:outline-none focus:border-cyan-400"
                  >
                    {Object.keys(forexRates).length ? (
                      Object.keys(forexRates).map(k => <option key={k} value={k}>{k}</option>)
                    ) : (
                      ['USD', 'EUR', 'GBP', 'AED', 'PHP', 'JPY', 'CAD', 'AUD'].map(k => <option key={k} value={k}>{k}</option>)
                    )}
                  </select>
                </div>

                <div>
                  <label className="block text-[10px] font-mono-hud text-slate-400 mb-1">TO CURRENCY</label>
                  <select
                    value={calcTo}
                    onChange={(e) => setCalcTo(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 focus:outline-none focus:border-cyan-400"
                  >
                    {Object.keys(forexRates).length ? (
                      Object.keys(forexRates).map(k => <option key={k} value={k}>{k}</option>)
                    ) : (
                      ['AED', 'USD', 'EUR', 'GBP', 'PHP', 'JPY', 'CAD', 'AUD'].map(k => <option key={k} value={k}>{k}</option>)
                    )}
                  </select>
                </div>
              </div>

              {/* Conversion Result */}
              <div className="p-4 rounded-xl bg-[#010814] border border-cyan-500/30 text-center font-mono-hud">
                <p className="text-xs text-slate-400">CONVERTED YIELD</p>
                <p className="text-3xl font-bold text-cyan-300 my-1">
                  {calculateConversion()} {calcTo}
                </p>
                <p className="text-[10px] text-slate-500">
                  1 {calcFrom} ≈ {((forexRates[calcTo] || 1) / (forexRates[calcFrom] || 1)).toFixed(4)} {calcTo}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
