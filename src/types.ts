export type EmotionType = 'neutral' | 'warm' | 'concerned' | 'excited' | 'amused' | 'serious' | 'proud';

export type ResponseMode = 'brief' | 'balanced' | 'detailed';

export type OrbState = 'IDLE' | 'LISTENING' | 'THINKING' | 'SPEAKING' | 'WAKE';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  timestamp: string;
  emotion?: EmotionType;
  imageUrl?: string;
  imageBase64?: string;
  isAudioPlaying?: boolean;
}

export interface UserProfile {
  name: string;
  city: string;
  job?: string;
  interests?: string[];
  theme?: string;
}

export interface AsteroidItem {
  id: string;
  name: string;
  estimatedDiameterMinKm: number;
  estimatedDiameterMaxKm: number;
  isHazardous: boolean;
  closeApproachDate: string;
  missDistanceKm: number;
  missDistanceLunar: number;
  relativeVelocityKmh: number;
}

export interface EarthquakeItem {
  id: string;
  magnitude: number;
  place: string;
  time: number;
  url: string;
  tsunami: number;
  depth: number;
}

export interface StockItem {
  symbol: string;
  price: number;
  change: number;
  changePercent: number;
  currency: string;
  exchange: string;
  time?: number;
}

export interface CryptoItem {
  id: string;
  symbol: string;
  name: string;
  usd: number;
  usd_24h_change: number;
}

export interface ChemicalElement {
  number: number;
  symbol: string;
  name: string;
  atomicMass: number;
  category: 'alkali' | 'alkaline' | 'transition' | 'post-transition' | 'metalloid' | 'nonmetal' | 'halogen' | 'noble' | 'lanthanide' | 'actinide';
  summary: string;
  electronConfiguration: string;
  density?: number;
  meltingPoint?: number;
  boilingPoint?: number;
  discoveredBy?: string;
}

export interface TaskItem {
  id: string;
  title: string;
  completed: boolean;
  dueDate?: string;
  category: 'work' | 'personal' | 'priority';
}

export interface ExpenseItem {
  id: string;
  title: string;
  amount: number;
  category: string;
  date: string;
}

export interface HabitItem {
  id: string;
  name: string;
  category: string;
  streak: number;
  history: { [dateStr: string]: boolean };
}

export interface PasswordItem {
  id: string;
  title: string;
  username: string;
  passwordEncrypted: string;
  url?: string;
  notes?: string;
  updatedAt: string;
}

export interface VoiceNoteItem {
  id: string;
  title: string;
  content: string;
  date: string;
  tags: string[];
}
