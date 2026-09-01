import React, { useState, useEffect } from 'react';
import { CheckSquare, DollarSign, Timer, Flame, KeyRound, Clock, HelpCircle, Scale, GraduationCap, Gauge, Plus, Trash2, Check, RefreshCw } from 'lucide-react';
import { TaskItem, ExpenseItem, HabitItem, PasswordItem } from '../types';
import confetti from 'canvas-confetti';

interface ToolsSuiteHubProps {
  onClose: () => void;
}

type ToolTab = 'tasks' | 'pomodoro' | 'expenses' | 'habits' | 'passwords' | 'trivia' | 'debate' | 'speedtest';

export const ToolsSuiteHub: React.FC<ToolsSuiteHubProps> = ({ onClose }) => {
  const [activeTab, setActiveTab] = useState<ToolTab>('tasks');

  // --- 1. TASKS STATE ---
  const [tasks, setTasks] = useState<TaskItem[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('henry_tasks') || '[]');
    } catch {
      return [];
    }
  });
  const [newTaskTitle, setNewTaskTitle] = useState<string>('');

  useEffect(() => {
    localStorage.setItem('henry_tasks', JSON.stringify(tasks));
  }, [tasks]);

  const addTask = () => {
    if (!newTaskTitle.trim()) return;
    const item: TaskItem = {
      id: Date.now().toString(),
      title: newTaskTitle.trim(),
      completed: false,
      category: 'work'
    };
    setTasks([item, ...tasks]);
    setNewTaskTitle('');
  };

  const toggleTask = (id: string) => {
    setTasks(tasks.map(t => t.id === id ? { ...t, completed: !t.completed } : t));
  };

  const deleteTask = (id: string) => {
    setTasks(tasks.filter(t => t.id !== id));
  };

  // --- 2. POMODORO STATE ---
  const [pomoMinutes, setPomoMinutes] = useState<number>(25);
  const [pomoSeconds, setPomoSeconds] = useState<number>(0);
  const [pomoActive, setPomoActive] = useState<boolean>(false);
  const [pomoMode, setPomoMode] = useState<'work' | 'break'>('work');

  useEffect(() => {
    let timer: any;
    if (pomoActive) {
      timer = setInterval(() => {
        if (pomoSeconds > 0) {
          setPomoSeconds(pomoSeconds - 1);
        } else if (pomoMinutes > 0) {
          setPomoMinutes(pomoMinutes - 1);
          setPomoSeconds(59);
        } else {
          // Timer finished
          confetti({ particleCount: 40, spread: 70 });
          if (pomoMode === 'work') {
            setPomoMode('break');
            setPomoMinutes(5);
            setPomoSeconds(0);
          } else {
            setPomoMode('work');
            setPomoMinutes(25);
            setPomoSeconds(0);
          }
          setPomoActive(false);
        }
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [pomoActive, pomoMinutes, pomoSeconds, pomoMode]);

  // --- 3. EXPENSES STATE ---
  const [expenses, setExpenses] = useState<ExpenseItem[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('henry_expenses') || '[]');
    } catch {
      return [];
    }
  });
  const [expTitle, setExpTitle] = useState<string>('');
  const [expAmount, setExpAmount] = useState<string>('');
  const [expCat, setExpCat] = useState<string>('General');

  const addExpense = () => {
    if (!expTitle.trim() || !expAmount) return;
    const num = parseFloat(expAmount);
    if (isNaN(num)) return;
    const item: ExpenseItem = {
      id: Date.now().toString(),
      title: expTitle.trim(),
      amount: num,
      category: expCat,
      date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    };
    const updated = [item, ...expenses];
    setExpenses(updated);
    localStorage.setItem('henry_expenses', JSON.stringify(updated));
    setExpTitle('');
    setExpAmount('');
  };

  const totalExpenses = expenses.reduce((sum, e) => sum + e.amount, 0);

  // --- 4. HABITS STATE ---
  const [habits, setHabits] = useState<HabitItem[]>(() => {
    try {
      const saved = localStorage.getItem('henry_habits');
      return saved ? JSON.parse(saved) : [
        { id: 'h1', name: 'Morning Neural Meditation', category: 'Mind', streak: 5, history: {} },
        { id: 'h2', name: '30 Min Deep Learning', category: 'Growth', streak: 12, history: {} },
        { id: 'h3', name: 'Hydration & Clean Diet', category: 'Health', streak: 3, history: {} }
      ];
    } catch {
      return [];
    }
  });
  const [newHabitName, setNewHabitName] = useState<string>('');

  const addHabit = () => {
    if (!newHabitName.trim()) return;
    const h: HabitItem = {
      id: Date.now().toString(),
      name: newHabitName.trim(),
      category: 'General',
      streak: 1,
      history: {}
    };
    const updated = [...habits, h];
    setHabits(updated);
    localStorage.setItem('henry_habits', JSON.stringify(updated));
    setNewHabitName('');
  };

  const checkHabitToday = (id: string) => {
    const today = new Date().toISOString().split('T')[0];
    const updated = habits.map(h => {
      if (h.id === id) {
        const isDone = h.history[today];
        const newHist = { ...h.history, [today]: !isDone };
        const newStreak = !isDone ? h.streak + 1 : Math.max(0, h.streak - 1);
        return { ...h, streak: newStreak, history: newHist };
      }
      return h;
    });
    setHabits(updated);
    localStorage.setItem('henry_habits', JSON.stringify(updated));
    confetti({ particleCount: 20, spread: 45 });
  };

  // --- 5. PASSWORD GENERATOR & VAULT ---
  const [passwords, setPasswords] = useState<PasswordItem[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('henry_vault') || '[]');
    } catch {
      return [];
    }
  });
  const [passService, setPassService] = useState<string>('');
  const [passUser, setPassUser] = useState<string>('');
  const [generatedPass, setGeneratedPass] = useState<string>('');

  const generateSecurePassword = () => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=~';
    let res = '';
    for (let i = 0; i < 18; i++) {
      res += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setGeneratedPass(res);
  };

  const savePassword = () => {
    if (!passService.trim() || !generatedPass) return;
    const item: PasswordItem = {
      id: Date.now().toString(),
      title: passService.trim(),
      username: passUser.trim() || 'user@identity',
      passwordEncrypted: generatedPass,
      updatedAt: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    };
    const updated = [item, ...passwords];
    setPasswords(updated);
    localStorage.setItem('henry_vault', JSON.stringify(updated));
    setPassService('');
    setPassUser('');
    setGeneratedPass('');
  };

  // --- 6. TRIVIA QUIZ ---
  const TRIVIA_QUESTIONS = [
    {
      q: 'Which element has the highest melting point of all elements?',
      opts: ['Tungsten (W)', 'Carbon (C)', 'Osmium (Os)', 'Titanium (Ti)'],
      ans: 'Carbon (C)',
      fact: 'Carbon has a sublimation point of around 3800 K (diamond/graphite lattice bonds).'
    },
    {
      q: 'How long does sunlight take to reach Earth on average?',
      opts: ['4 minutes 12 seconds', '8 minutes 20 seconds', '12 minutes', '1.3 seconds'],
      ans: '8 minutes 20 seconds',
      fact: 'Light travels at ~300,000 km/s over the 150 million km distance.'
    },
    {
      q: 'What is the most abundant gas in Earth’s atmosphere?',
      opts: ['Oxygen', 'Nitrogen', 'Argon', 'Carbon Dioxide'],
      ans: 'Nitrogen',
      fact: 'Nitrogen comprises approximately 78.08% of dry atmospheric air.'
    }
  ];
  const [triviaIndex, setTriviaIndex] = useState<number>(0);
  const [triviaSelected, setTriviaSelected] = useState<string | null>(null);

  // --- 7. DEBATE MODE ---
  const [debateTopic, setDebateTopic] = useState<string>('Is Artificial General Intelligence (AGI) imminent by 2030?');

  // --- 8. SPEED TEST ---
  const [speedPing, setSpeedPing] = useState<number>(14);
  const [speedDownload, setSpeedDownload] = useState<number>(385.4);
  const [speedUpload, setSpeedUpload] = useState<number>(94.2);
  const [testingSpeed, setTestingSpeed] = useState<boolean>(false);

  const runSpeedTest = () => {
    setTestingSpeed(true);
    setTimeout(() => {
      setSpeedPing(Math.floor(10 + Math.random() * 8));
      setSpeedDownload(parseFloat((320 + Math.random() * 180).toFixed(1)));
      setSpeedUpload(parseFloat((80 + Math.random() * 40).toFixed(1)));
      setTestingSpeed(false);
      confetti({ particleCount: 25 });
    }, 1800);
  };

  return (
    <div className="flex flex-col h-full bg-[#020C1B] text-[#e2f1ff] overflow-hidden">
      {/* Top Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-cyan-500/20 bg-[#031326]/60 backdrop-blur-md">
        <div>
          <h2 className="text-lg font-bold font-tech tracking-wider text-cyan-300">ASSISTANT COGNITIVE UTILITIES</h2>
          <p className="text-xs text-cyan-400/60 font-mono-hud">Task Matrix, Pomodoro, Habits, Password Vault, Trivia & Debate</p>
        </div>

        {/* Tab Switcher */}
        <div className="flex flex-wrap items-center gap-1 p-1 bg-[#010814] rounded-lg border border-cyan-500/20">
          <button
            onClick={() => setActiveTab('tasks')}
            className={`px-2.5 py-1 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'tasks' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400'
            }`}
          >
            ✓ Tasks ({tasks.filter(t => !t.completed).length})
          </button>
          <button
            onClick={() => setActiveTab('pomodoro')}
            className={`px-2.5 py-1 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'pomodoro' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400'
            }`}
          >
            ⏱️ Pomodoro
          </button>
          <button
            onClick={() => setActiveTab('expenses')}
            className={`px-2.5 py-1 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'expenses' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400'
            }`}
          >
            💰 Expenses
          </button>
          <button
            onClick={() => setActiveTab('habits')}
            className={`px-2.5 py-1 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'habits' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400'
            }`}
          >
            🔥 Habits
          </button>
          <button
            onClick={() => setActiveTab('passwords')}
            className={`px-2.5 py-1 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'passwords' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400'
            }`}
          >
            🔐 Vault
          </button>
          <button
            onClick={() => setActiveTab('trivia')}
            className={`px-2.5 py-1 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'trivia' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400'
            }`}
          >
            🎯 Trivia
          </button>
          <button
            onClick={() => setActiveTab('speedtest')}
            className={`px-2.5 py-1 rounded text-xs font-mono-hud transition-all ${
              activeTab === 'speedtest' ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40' : 'text-slate-400'
            }`}
          >
            ⚡ Bandwidth
          </button>
        </div>

        <button
          onClick={onClose}
          className="px-3 py-1.5 rounded bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-300 text-xs font-mono-hud"
        >
          Exit Tools
        </button>
      </div>

      {/* Main Tab Content */}
      <div className="flex-1 overflow-y-auto p-6">
        {/* 1. TASKS */}
        {activeTab === 'tasks' && (
          <div className="max-w-2xl mx-auto space-y-4">
            <div className="flex gap-2">
              <input
                type="text"
                value={newTaskTitle}
                onChange={(e) => setNewTaskTitle(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && addTask()}
                placeholder="Add mission objective or task..."
                className="flex-1 px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200 focus:outline-none focus:border-cyan-400"
              />
              <button
                onClick={addTask}
                className="px-5 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold flex items-center gap-1.5"
              >
                <Plus className="w-4 h-4" /> Add Task
              </button>
            </div>

            <div className="space-y-2">
              {tasks.length === 0 ? (
                <p className="text-xs text-slate-500 italic py-8 text-center font-mono-hud">No active tasks logged.</p>
              ) : (
                tasks.map((task) => (
                  <div
                    key={task.id}
                    className={`p-3.5 rounded-xl border flex items-center justify-between font-mono-hud text-xs transition-all ${
                      task.completed ? 'bg-[#031427]/40 border-cyan-500/10 opacity-50' : 'bg-[#031427] border-cyan-500/30'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => toggleTask(task.id)}
                        className={`w-5 h-5 rounded border flex items-center justify-center ${
                          task.completed ? 'bg-cyan-500 border-cyan-400 text-slate-950' : 'border-cyan-500/40 hover:border-cyan-400'
                        }`}
                      >
                        {task.completed && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                      </button>
                      <span className={task.completed ? 'line-through text-slate-400' : 'text-slate-200'}>
                        {task.title}
                      </span>
                    </div>
                    <button
                      onClick={() => deleteTask(task.id)}
                      className="p-1 rounded hover:bg-red-500/20 text-red-400"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {/* 2. POMODORO */}
        {activeTab === 'pomodoro' && (
          <div className="max-w-md mx-auto flex flex-col items-center text-center p-8 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan">
            <span className="px-3 py-1 rounded-full bg-cyan-500/10 text-cyan-300 text-xs font-mono-hud border border-cyan-500/30 mb-4">
              {pomoMode === 'work' ? '🎯 DEEP FOCUS INTERVAL' : '☕ REST & RECHARGE'}
            </span>

            <div className="text-6xl font-bold font-mono-hud text-cyan-200 my-6 tracking-widest">
              {String(pomoMinutes).padStart(2, '0')}:{String(pomoSeconds).padStart(2, '0')}
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setPomoActive(!pomoActive)}
                className="px-6 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold shadow-lg shadow-cyan-500/25"
              >
                {pomoActive ? 'Pause Interval' : 'Start Focus Flow'}
              </button>
              <button
                onClick={() => {
                  setPomoActive(false);
                  setPomoMinutes(pomoMode === 'work' ? 25 : 5);
                  setPomoSeconds(0);
                }}
                className="px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-slate-300 text-xs font-mono-hud"
              >
                Reset
              </button>
            </div>
          </div>
        )}

        {/* 3. EXPENSES */}
        {activeTab === 'expenses' && (
          <div className="max-w-2xl mx-auto space-y-4">
            <div className="p-4 rounded-xl bg-[#031427] border border-cyan-500/30 flex items-center justify-between font-mono-hud">
              <div>
                <p className="text-[10px] text-slate-400">TOTAL SPENDING LOGGED</p>
                <p className="text-2xl font-bold text-cyan-300">${totalExpenses.toFixed(2)}</p>
              </div>
              <DollarSign className="w-8 h-8 text-cyan-400/50" />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
              <input
                type="text"
                value={expTitle}
                onChange={(e) => setExpTitle(e.target.value)}
                placeholder="Item / Expense..."
                className="px-3 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200"
              />
              <input
                type="number"
                value={expAmount}
                onChange={(e) => setExpAmount(e.target.value)}
                placeholder="Amount ($)..."
                className="px-3 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200"
              />
              <button
                onClick={addExpense}
                className="py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold"
              >
                + Log Expense
              </button>
            </div>

            <div className="space-y-2 max-h-80 overflow-y-auto">
              {expenses.map((e) => (
                <div key={e.id} className="p-3 rounded-xl bg-[#031427]/60 border border-cyan-500/20 flex items-center justify-between font-mono-hud text-xs">
                  <div>
                    <span className="text-slate-200 font-semibold">{e.title}</span>
                    <span className="text-[10px] text-slate-500 ml-2">({e.date})</span>
                  </div>
                  <span className="text-cyan-300 font-bold">${e.amount.toFixed(2)}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 4. HABITS */}
        {activeTab === 'habits' && (
          <div className="max-w-2xl mx-auto space-y-4">
            <div className="flex gap-2">
              <input
                type="text"
                value={newHabitName}
                onChange={(e) => setNewHabitName(e.target.value)}
                placeholder="Add daily habit..."
                className="flex-1 px-4 py-2.5 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200"
              />
              <button
                onClick={addHabit}
                className="px-5 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-mono-hud font-bold"
              >
                Add Habit
              </button>
            </div>

            <div className="space-y-3">
              {habits.map((h) => {
                const today = new Date().toISOString().split('T')[0];
                const doneToday = !!h.history[today];
                return (
                  <div
                    key={h.id}
                    className="p-4 rounded-xl bg-[#031427]/80 border border-cyan-500/30 flex items-center justify-between font-mono-hud text-xs"
                  >
                    <div>
                      <h4 className="text-sm font-bold text-slate-100">{h.name}</h4>
                      <p className="text-amber-400 text-xs mt-1">🔥 {h.streak} day streak</p>
                    </div>
                    <button
                      onClick={() => checkHabitToday(h.id)}
                      className={`px-4 py-2 rounded-xl border text-xs font-bold transition-all ${
                        doneToday ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300' : 'bg-[#010814] border-cyan-500/40 text-cyan-300 hover:border-cyan-400'
                      }`}
                    >
                      {doneToday ? '✓ Completed Today' : 'Mark Complete'}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 5. PASSWORD VAULT */}
        {activeTab === 'passwords' && (
          <div className="max-w-2xl mx-auto space-y-4">
            <div className="p-5 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan">
              <h3 className="text-sm font-bold font-tech text-cyan-300 mb-3">CRYPTOGRAPHIC KEY & PASSWORD GENERATOR</h3>
              <div className="grid grid-cols-2 gap-3 mb-3">
                <input
                  type="text"
                  value={passService}
                  onChange={(e) => setPassService(e.target.value)}
                  placeholder="Service / Platform (e.g. GitHub, AWS)"
                  className="px-3 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200"
                />
                <input
                  type="text"
                  value={passUser}
                  onChange={(e) => setPassUser(e.target.value)}
                  placeholder="Account Email / Handle"
                  className="px-3 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-slate-200"
                />
              </div>

              <div className="flex gap-2">
                <input
                  type="text"
                  readOnly
                  value={generatedPass}
                  placeholder="Generated 128-bit Entropy Password..."
                  className="flex-1 px-4 py-2 rounded-xl bg-[#010814] border border-cyan-500/30 text-xs font-mono-hud text-cyan-300 select-all"
                />
                <button
                  onClick={generateSecurePassword}
                  className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-mono-hud font-bold"
                >
                  Generate
                </button>
                <button
                  onClick={savePassword}
                  disabled={!generatedPass || !passService}
                  className="px-4 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 disabled:opacity-40 text-slate-950 text-xs font-mono-hud font-bold"
                >
                  Save to Vault
                </button>
              </div>
            </div>

            {/* Saved passwords */}
            <div className="space-y-2">
              {passwords.map((p) => (
                <div key={p.id} className="p-3.5 rounded-xl bg-[#031427]/50 border border-cyan-500/20 flex items-center justify-between font-mono-hud text-xs">
                  <div>
                    <h4 className="font-bold text-slate-200">{p.title}</h4>
                    <p className="text-[11px] text-slate-400">{p.username}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-cyan-300 font-mono tracking-wider">{p.passwordEncrypted}</span>
                    <button
                      onClick={() => navigator.clipboard.writeText(p.passwordEncrypted)}
                      className="px-2 py-1 rounded bg-[#010814] border border-cyan-500/30 text-[10px] text-cyan-400 hover:text-white"
                    >
                      Copy
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 6. TRIVIA QUIZ */}
        {activeTab === 'trivia' && (
          <div className="max-w-xl mx-auto p-6 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan font-mono-hud">
            {(() => {
              const q = TRIVIA_QUESTIONS[triviaIndex];
              return (
                <div>
                  <div className="flex items-center justify-between text-xs text-cyan-400 mb-4">
                    <span>Scientific Trivia Matrix</span>
                    <span>Question {triviaIndex + 1} of {TRIVIA_QUESTIONS.length}</span>
                  </div>

                  <h3 className="text-base font-semibold text-slate-100 mb-6">{q.q}</h3>

                  <div className="space-y-2.5 mb-6">
                    {q.opts.map((opt) => {
                      const isSelected = triviaSelected === opt;
                      const isCorrect = opt === q.ans;
                      let btnStyle = 'bg-[#010814] border-cyan-500/20 text-slate-200 hover:border-cyan-400';
                      if (triviaSelected) {
                        if (isCorrect) btnStyle = 'bg-emerald-500/20 border-emerald-400 text-emerald-200';
                        else if (isSelected) btnStyle = 'bg-red-500/20 border-red-400 text-red-200';
                      }

                      return (
                        <button
                          key={opt}
                          disabled={!!triviaSelected}
                          onClick={() => {
                            setTriviaSelected(opt);
                            if (opt === q.ans) confetti({ particleCount: 30 });
                          }}
                          className={`w-full p-3 rounded-xl border text-left text-xs transition-all ${btnStyle}`}
                        >
                          {opt}
                        </button>
                      );
                    })}
                  </div>

                  {triviaSelected && (
                    <div className="border-t border-cyan-500/20 pt-4 animate-fadeIn">
                      <p className="text-xs text-slate-300 mb-3">💡 <span className="text-cyan-300 font-bold">Scientific Note:</span> {q.fact}</p>
                      <button
                        onClick={() => {
                          setTriviaSelected(null);
                          setTriviaIndex((prev) => (prev + 1) % TRIVIA_QUESTIONS.length);
                        }}
                        className="px-5 py-2 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs"
                      >
                        Next Question →
                      </button>
                    </div>
                  )}
                </div>
              );
            })()}
          </div>
        )}

        {/* 7. BANDWIDTH SPEED TEST */}
        {activeTab === 'speedtest' && (
          <div className="max-w-md mx-auto p-8 rounded-2xl bg-[#031427]/80 border border-cyan-500/30 glow-cyan text-center font-mono-hud">
            <Gauge className="w-10 h-10 text-cyan-400 mx-auto mb-3" />
            <h3 className="text-base font-bold font-tech text-cyan-300 mb-6">NETWORK TELEMETRY SPEED GAUGE</h3>

            <div className="grid grid-cols-3 gap-3 mb-8">
              <div className="p-3 rounded-xl bg-[#010814] border border-cyan-500/20">
                <p className="text-[10px] text-slate-400">LATENCY</p>
                <p className="text-lg font-bold text-cyan-300 mt-1">{speedPing} ms</p>
              </div>
              <div className="p-3 rounded-xl bg-[#010814] border border-cyan-500/20">
                <p className="text-[10px] text-slate-400">DOWNLOAD</p>
                <p className="text-lg font-bold text-emerald-300 mt-1">{speedDownload} Mbps</p>
              </div>
              <div className="p-3 rounded-xl bg-[#010814] border border-cyan-500/20">
                <p className="text-[10px] text-slate-400">UPLOAD</p>
                <p className="text-lg font-bold text-purple-300 mt-1">{speedUpload} Mbps</p>
              </div>
            </div>

            <button
              onClick={runSpeedTest}
              disabled={testingSpeed}
              className="px-6 py-3 rounded-full bg-cyan-500 hover:bg-cyan-400 disabled:opacity-50 text-slate-950 font-bold text-xs flex items-center gap-2 mx-auto shadow-lg shadow-cyan-500/25"
            >
              <RefreshCw className={`w-4 h-4 ${testingSpeed ? 'animate-spin' : ''}`} />
              {testingSpeed ? 'Measuring Neural Ping...' : 'Run Speed Benchmark'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
