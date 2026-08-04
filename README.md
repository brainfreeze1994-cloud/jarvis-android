[README.md](https://github.com/user-attachments/files/30723654/README.md)
# H·E·N·R·Y™ Android
### Hyperintelligence Engine Neural Reasoning Yield

> A fully native Android AI assistant with voice, vision, brain training, device control, and live data — built on free services. No subscription. No API key needed on device.

**Version:** 23.0.0 · **Min Android:** 7.0 (API 24) · **Target:** Android 14 (API 34)

---

## Install

1. Go to the **Actions** tab in this GitHub repo
2. Click the latest **Build Debug APK** workflow run
3. Download the **JARVIS-Android-APK** artifact
4. Transfer to your Android device
5. Enable **Install Unknown Apps** in Settings if prompted
6. Install and open **H·E·N·R·Y**

> If Google Play Protect blocks the install, tap **Install Anyway** — the app is safe.

---

## First Launch

1. The splash screen loads — H·E·N·R·Y initialises
2. **Grant all permissions** when prompted:
   - Microphone (voice input)
   - Camera (image analysis, QR scanner)
   - Contacts (call & message by name)
   - Storage (file reading, PDF, attachments)
   - Notifications (reminders, alarms)
   - Phone (make calls by voice)
3. Tap the **orb** or **🎤 mic button** to start talking

---

## Interface

```
┌───────────────────────────────────────────────┐
│ H·E·N·R·Y  STANDBY  VOICE  🧠  🌍  🐾  🗑️   │  ← Top bar
├───────────────────────────────────────────────┤
│                                               │
│              [ ANIMATED ORB ]                 │  ← Tap to speak
│                                               │
│              H · E · N · R · Y               │
│                                               │
│  [Weather?]  [Generate image]                 │  ← Suggestion chips
│  [Python script]  [Fascinating fact]          │
│                                               │
│  ┌─────────────────────────────────────────┐  │
│  │  HNR  HENRY reply text here...          │  │  ← Chat bubbles
│  └─────────────────────────────────────────┘  │
│         ┌──────────────────────────────┐       │
│         │  YOU  Your message here...   │       │
│         └──────────────────────────────┘       │
├───────────────────────────────────────────────┤
│  📎  [Command HENRY…]          🎤  ➤          │  ← Input bar
└───────────────────────────────────────────────┘
```

### Top Bar Buttons

| Button | Action |
|---|---|
| **VOICE** | Change TTS accent (long-press trash to open) |
| **🧠** | Open H·E·N·R·Y Brain hub (9 cognitive modules) |
| **🌍** | Open 3D Earth map |
| **🐾** | Open Animal Scanner |
| **🗑️** | Clear chat / long-press to open accent picker |

---

## Features — All 109 Classes

### Core
| Class | Purpose |
|---|---|
| `MainActivity` | Central hub — voice, chat, TTS, orb control |
| `SplashActivity` | Animated startup screen |
| `OrbView` | Custom canvas orb with 4 animated states |
| `HudTickerView` | Scrolling HUD status ticker |
| `JarvisApi` | Vercel backend caller (LLM, vision, TTS) |
| `ChatAdapter` | RecyclerView chat renderer |
| `SmartMemory` | Persistent memory — auto-learns about user |
| `UserProfile` | Stores name, city, job, interests |
| `HistoryItem` | Chat history model |
| `Message` | Chat message model (text, image, URL) |

### 🧠 Brain Modules
| Class | Purpose |
|---|---|
| `BrainActivity` | Brain hub — routes to all 5 neural modules |
| `HenryBrainView` | Interactive brain map canvas with tap regions |
| `MentalImageryActivity` | 8 guided visualizations (Ocean, Mountain, Cosmic, etc.) |
| `NeuralPlasticityActivity` | 8 brain training exercises (Stroop, N-back, Dual Task, etc.) |
| `DefaultModeNetworkActivity` | DMN — mind-wandering, reflection journal, future self |
| `SensorySubstitutionActivity` | Color→Sound, Motion→Haptic, Braille vibration |
| `SmartMemoryActivity` | View, add, edit, delete all HENRY memories |

### 👁 Vision & Camera
| Class | Purpose |
|---|---|
| `VisionActivity` | Live camera AI — face detection, object tracking, labels |
| `VisionOverlayView` | Bounding box overlay for camera view |
| `AnimalScannerActivity` | Photo → species identification |
| `LiveCameraActivity` | Real-time camera with ML Kit |
| `DocumentScanner` | Camera → clean document scan |
| `QrScanner` | QR/barcode reader |
| `ImageGenerator` | Pollinations.ai Flux image generation |

### 🌍 Maps & Navigation
| Class | Purpose |
|---|---|
| `EarthMapActivity` | 3D WebView globe — country intel briefings |
| `MapActivity` | OpenStreetMap navigation |
| `NavigationHelper` | Google Maps voice navigation |
| `NearbyPlaces` | Nearby restaurants, hospitals, shops |
| `TransitHelper` | Ride apps (Uber, Careem, Grab, Lyft) |
| `TravelTime` | OSRM route time calculation |
| `LocationShare` | Share live location |
| `PlaceDetails` | Wikipedia-powered place info |

### 📱 Device Control
| Class | Purpose |
|---|---|
| `DeviceCommands` | Flashlight, brightness, DND, battery, datetime |
| `MusicControl` | Play, pause, skip, volume |
| `AppLauncher` | Launch 35+ apps by voice |
| `AlarmHelper` | Set alarms by voice |
| `VoiceTimer` | Countdown timer |
| `Stopwatch` | Stopwatch |
| `BatteryGuardian` | Low battery alerts and monitoring |
| `ScreenReader` | Accessibility text reader |

### 💬 Communication
| Class | Purpose |
|---|---|
| `ContactsHelper` | Find contacts, find email |
| `WhatsAppHelper` | Send WhatsApp messages by voice |
| `EmailDrafter` | Compose and send email drafts |
| `SocialCaptionGenerator` | AI-written social media captions |
| `ShareReceiver` | Receive shared content from other apps |
| `NotificationService` | Read incoming notifications aloud |
| `NotificationSummary` | Summarise recent notifications |

### ⏰ Reminders & Scheduling
| Class | Purpose |
|---|---|
| `ReminderManager` | Create/manage voice reminders |
| `ReminderReceiver` | Broadcast receiver for reminder alerts |
| `MorningAlarmReceiver` | Daily morning briefing alarm |
| `DailyDigest` | Auto morning summary (weather, news, tasks) |
| `DailySummary` | End-of-day recap |
| `WordOfTheDay` | Daily vocabulary word on startup |
| `BirthdayTracker` | Birthday reminders |

### 💰 Finance & Productivity
| Class | Purpose |
|---|---|
| `LivePrices` | Crypto, gold, oil, forex live prices |
| `ExpenseTracker` | Log and track daily spending |
| `CurrencyConverter` | Real-time currency conversion |
| `Calculator` | Math + unit conversion |
| `TaskManager` | To-do list by voice |
| `ShoppingList` | Voice-controlled shopping list |
| `PasswordVault` | AES-256 encrypted local password manager |
| `SpeedTest` | Internet speed test |

### 🏋 Health & Wellness
| Class | Purpose |
|---|---|
| `FitnessTracker` | Log workouts and fitness data |
| `FitnessCoach` | AI personal trainer |
| `HealthCalculator` | BMI, calorie, water intake |
| `BreathingExercise` | Guided breathing sessions |
| `SleepMode` | Sleep timer + DND |
| `SleepTracker` | Track sleep patterns |
| `MoodTracker` | Daily mood logging |
| `PomodoroTimer` | Focus/break timer |
| `HabitTracker` | Build and track daily habits |

### 🧠 AI Intelligence
| Class | Purpose |
|---|---|
| `EmotionDetector` | Detects emotion from user text |
| `HenryMood` | HENRY's own emotional state |
| `RelationshipBrain` | Tracks relationship with user over time |
| `ProactiveSuggestions` | Suggests actions based on context |
| `ConversationInsights` | Analyses chat patterns |
| `SmartCompose` | AI-assisted text composition |

### 📚 Knowledge & Learning
| Class | Purpose |
|---|---|
| `NewsReader` | Live news headlines |
| `WeatherForecast` | Detailed weather + 3-day forecast |
| `VoiceTranslator` | Real-time voice translation |
| `LanguageLearning` | Language lessons and practice |
| `TriviaQuiz` | Knowledge quiz game |
| `DebateMode` | AI argues both sides of any topic |
| `StudyMode` | Focus mode with study timer |
| `RecipeGenerator` | AI recipe suggestions |
| `MealPlanner` | Weekly meal planning |

### 🛠 Tools & Utilities
| Class | Purpose |
|---|---|
| `CodeRunner` | Run code snippets (Piston API) |
| `PdfReader` | Read and summarise PDF files |
| `SmartFileManager` | Voice-controlled file browser |
| `ClipboardManager2` | Clipboard history and management |
| `InAppBrowser` | Built-in web browser |
| `ChatSearch` | Search through chat history |
| `ChipPrefs` | Custom suggestion chip storage |
| `CustomShortcuts` | User-defined voice shortcuts |
| `CommandsDashboard` | Browse all 70+ commands |

### 🚨 Emergency & Safety
| Class | Purpose |
|---|---|
| `EmergencySOS` | Sends emergency message with location |
| `UAELawHelper` | UAE laws and legal information |

### 🎭 Fun & Creative
| Class | Purpose |
|---|---|
| `VoiceJournal` | Voice-recorded personal journal |
| `VoiceNotes` | Quick voice notes |
| `MeetingRecorder` | Record and transcribe meetings |
| `FocusMode` | Deep work mode — blocks distractions |

### 🔧 Services & Receivers
| Class | Purpose |
|---|---|
| `WakeWordService` | Background "HENRY" wake word listener |
| `FloatingBubbleService` | Floating bubble overlay |
| `HenryWidget` | Home screen widget |
| `VoiceShortcutWidget` | Voice shortcut home screen widget |

### 🌐 Google Integration
| Class | Purpose |
|---|---|
| `GoogleWorkspaceHelper` | Create Google Docs, Sheets, Slides |

---

## Architecture

```
┌─────────────────────────────────────────┐
│         H·E·N·R·Y Android App          │
│                                         │
│  MainActivity (hub)                     │
│    ├── Voice Input (SpeechRecognizer)   │
│    ├── TTS Output (Android TTS engine)  │
│    ├── OrbView (animated canvas)        │
│    └── JarvisApi ──────────────────────┼──► Vercel Backend
│                                         │        │
│  Feature Activities (109 classes)       │   api/jarvis.js (LLM)
│    ├── BrainActivity                    │   api/speak.js  (TTS)
│    ├── VisionActivity                   │        │
│    ├── EarthMapActivity                 │   AI Cascade:
│    └── [100+ more]                      │   1. Groq 70B
│                                         │   2. Groq 8B
│  Local Storage (SharedPreferences)      │   3. Cloudflare AI
│    ├── Chat history                     │   4. Pollinations
│    └── Memory facts                     │   5. OpenRouter
└─────────────────────────────────────────┘
```

---

## Build Requirements

| Requirement | Version |
|---|---|
| Android Studio | Flamingo or newer |
| Java | 11+ |
| Gradle | 8.6 |
| Android Gradle Plugin | 8.3.0 |
| Compile SDK | 34 (Android 14) |
| Min SDK | 24 (Android 7.0) |

### Key Dependencies
```gradle
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.cardview:cardview:1.0.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'androidx.camera:camera-core:1.3.1'
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'androidx.camera:camera-lifecycle:1.3.1'
implementation 'androidx.camera:camera-view:1.3.1'
implementation 'com.google.mlkit:text-recognition:16.0.0'
implementation 'com.google.mlkit:object-detection:17.0.0'
implementation 'com.google.mlkit:face-detection:16.1.5'
implementation 'com.google.mlkit:image-labeling:17.0.7'
```

---

## Permissions Used

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice input and speech recognition |
| `CAMERA` | Image analysis, QR scanner, document scan |
| `READ_CONTACTS` | Call and message contacts by name |
| `CALL_PHONE` | Make calls by voice command |
| `SEND_SMS` | Send text messages by voice |
| `READ_EXTERNAL_STORAGE` | Read files and PDFs |
| `READ_CALENDAR` | Read calendar events |
| `WRITE_CALENDAR` | Create calendar events |
| `VIBRATE` | Sensory substitution haptic feedback |
| `RECEIVE_BOOT_COMPLETED` | Restart morning alarm on reboot |
| `FOREGROUND_SERVICE` | Wake word and floating bubble |
| `SYSTEM_ALERT_WINDOW` | Floating bubble overlay |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read notifications aloud |
| `FLASHLIGHT` | Torch on/off by voice |
| `ACCESS_FINE_LOCATION` | Nearby places, weather, navigation |

---

## Voice System

HENRY uses Android's native TTS engine with intelligent voice selection:

1. **Preferred:** Google TTS engine (`com.google.android.tts`)
2. **Voice priority:** Best male/female voice matching chosen accent
3. **Accent options:** American ♂♀, Filipino ♂♀, French ♂♀
4. **Pitch/Rate:** Tuned per gender (male: 0.75 pitch, female: 1.0)

**Change accent:** Long-press the 🗑️ trash button → select from picker

---

## Backend Connection

The Android app connects to the Vercel backend:

```
Base URL: https://jarvis-ai-seven-dun.vercel.app

Endpoints:
  POST /api/jarvis   ← AI chat, vision, search, weather
  POST /api/speak    ← Text-to-speech (Edge TTS neural voices)
```

No API keys are stored on device. All secrets live in Vercel environment variables.

---

## GitHub Actions — Auto Build

Every push to `main` triggers an automatic APK build:

```yaml
# .github/workflows/build.yml
# Builds debug APK using Gradle 8.6
# Artifact: JARVIS-Android-APK (downloadable for 90 days)
```

To manually trigger: **Actions → Build Debug APK → Run workflow**

---

## Version History

| Version | Highlights |
|---|---|
| v23 | Brain button in top bar, acronym fix |
| v22 | Iron Man HUD orb, 3D Earth globe |
| v21 | Animal Scanner, Earth Map activity |
| v20 | ULTIMATE — Emotion Detection, UAE Law, Relationship Brain |
| v19 | Persistent memory injection, compound AI |
| v18 | Follow-up chips, live crypto/forex/news |
| v17 | Meeting Recorder, Live Subtitles, 15 features |
| v16 | Car Mode, In-App Browser, Code Runner |
| v15 | Commands Dashboard, Focus Mode, Debate Mode |
| v14 | Study Mode, Word of the Day, Currency Converter |
| v13 | Navigation, Maps, Transit, Location Share |
| v12 | Recipe Generator, Password Vault, Speed Test |
| v11 | Weather Forecast, Habit Tracker, Emergency SOS |
| v10 | Fitness Tracker, Voice Journal, Live Prices |
| v9 | Battery Guardian, Live Camera, Music Control |
| v8 | Floating Bubble, Smart Shortcuts, Daily Digest |
| v7 | User Profile, Web Search, Smarter routing |
| v6 | App Launcher, Stopwatch, QR Scanner, Alarms |
| v5 | Device Control, Voice Notes, News Reader |
| v4 | Home Widget, PDF Reader, Chat Export |
| v3 | Working APK, native TTS, blue theme |

---

## Legal

© 2026 H·E·N·R·Y Project. All rights reserved.

**H·E·N·R·Y™** and **Hyperintelligence Engine Neural Reasoning Yield™** are trademarks of their creator. Unauthorized reproduction, distribution, or commercial use without explicit written permission is prohibited.

Third-party services (Groq, Cloudflare, Microsoft, Google, Pollinations.ai) are property of their respective owners, used under free-tier terms.

---

*H·E·N·R·Y™ — Hyperintelligence Engine Neural Reasoning Yield*
*Built to be brilliant. Designed to be free.*
