# H·E·N·R·Y™ Android — Complete User Guide
### Hyperintelligence Engine Neural Reasoning Yield

© 2026 H·E·N·R·Y Project · Version 23.0.0

---

## Table of Contents

1. [Installation](#installation)
2. [First Launch & Permissions](#first-launch)
3. [Interface Overview](#interface)
4. [Talking to HENRY](#talking)
5. [Voice Commands — Full Reference](#voice-commands)
6. [Brain Modules](#brain)
7. [Vision & Camera](#vision)
8. [Device Control](#device)
9. [Health & Wellness](#health)
10. [Maps & Navigation](#maps)
11. [Communication](#communication)
12. [Productivity](#productivity)
13. [Finance & Shopping](#finance)
14. [Voice Settings](#voice-settings)
15. [Memory & Personalisation](#memory)
16. [Tips & Tricks](#tips)
17. [Troubleshooting](#troubleshooting)

---

## 1. Installation {#installation}

### From GitHub Actions (Recommended)
1. Open this GitHub repository in your browser
2. Tap the **Actions** tab at the top
3. Select the latest **Build Debug APK** workflow run
4. Scroll down to **Artifacts** → tap **JARVIS-Android-APK**
5. Download and extract the ZIP
6. Transfer the `.apk` file to your Android phone
7. On your phone: **Settings → Apps → Special app access → Install unknown apps**
8. Enable for your file manager or browser
9. Tap the APK to install
10. If Google Play Protect shows a warning — tap **Install Anyway**

### Requirements
- Android 7.0 (Nougat) or higher
- Internet connection for AI responses
- ~80MB storage space
- Microphone for voice input

---

## 2. First Launch & Permissions {#first-launch}

When HENRY opens for the first time:

1. **Splash screen** — H·E·N·R·Y logo animates in
2. **Permission prompts** appear one by one — tap **Allow** for all:

| Permission | Why HENRY needs it |
|---|---|
| 🎤 Microphone | Voice input and speech recognition |
| 📷 Camera | Image analysis, QR scanner, document scanning |
| 👥 Contacts | Call and text people by name |
| 📞 Phone | Make calls by voice command |
| 💬 SMS | Send text messages by voice |
| 📁 Storage | Read files, PDFs, attachments |
| 📅 Calendar | Read and create calendar events |
| 📍 Location | Weather, nearby places, navigation |
| 🔔 Notifications | HENRY reads your notifications aloud |

> **Tip:** HENRY still works if you deny some permissions, but those specific features will be unavailable.

### Special Permissions (Manual)
These must be enabled manually in Android Settings:

- **Notification Access** — Settings → Apps → Special app access → Notification access → enable H·E·N·R·Y
- **Draw over other apps** — Settings → Apps → Special app access → Display over other apps → enable H·E·N·R·Y (for floating bubble)

---

## 3. Interface Overview {#interface}

### Top Bar
```
┌──────────────────────────────────────────────────────┐
│ H·E·N·R·Y                                            │
│ HYPERINTELLIGENCE ENGINE NEURAL REASONING YIELD       │
│                   STANDBY  [VOICE] [🧠] [🌍] [🐾] [🗑️]│
└──────────────────────────────────────────────────────┘
```

| Element | Function |
|---|---|
| **H·E·N·R·Y** logo | Tapping does nothing — it's your brand |
| **STANDBY** badge | Shows current state: STANDBY / LISTENING / THINKING / SPEAKING |
| **VOICE** button | Open voice accent picker |
| **🧠** button | Open Brain hub (9 cognitive modules) |
| **🌍** button | Open 3D Earth Map |
| **🐾** button | Open Animal Scanner |
| **🗑️** button | Clear chat · Long-press = open voice accent picker |

### The Orb
The central animated orb shows HENRY's state:

| Orb State | Color | Meaning |
|---|---|---|
| Idle | 🔵 Blue rotating rings | Ready and waiting |
| Listening | 🟡 Gold audio bars | Hearing your voice |
| Thinking | 🟣 Purple spinning rings | Processing your request |
| Speaking | 🟢 Green ripple waves | Playing response |

**Tap the orb** to start/stop listening.

### Suggestion Chips
Six quick-tap commands below the orb. Long-press any chip to edit the text and set your own custom command.

### Chat Area
- **Left bubbles (HNR)** — HENRY's responses
- **Right bubbles (YOU)** — Your messages
- **Image bubbles** — Attached photos shown inline
- **Long-press any message** — Copy text to clipboard

### Input Bar
```
[ 📎 ]  [ Command HENRY… ]  [ 🎤 ]  [ ➤ ]
```

| Button | Function |
|---|---|
| **📎** | Attach image (gallery, camera, or file) |
| **Text field** | Type your message |
| **🎤** | Start voice input |
| **➤** | Send message |

---

## 4. Talking to HENRY {#talking}

### By Voice
1. Tap the **orb** or the **🎤 mic button**
2. Wait for the orb to turn gold (listening)
3. Speak your command clearly
4. HENRY will respond in text + voice

### By Text
1. Tap the text input field
2. Type your message
3. Press the ➤ send button or tap **Enter/Send** on keyboard

### Wake Word
HENRY can listen continuously in the background:
- Enable: say **"Enable wake word"** or go to Settings
- Trigger: say **"HENRY"** from anywhere in the app
- HENRY wakes up and starts listening automatically

### Response Modes
Control how long HENRY's answers are:

| Mode | Say this | Best for |
|---|---|---|
| **Brief** | "Keep it short" / "Brief mode" | Quick facts |
| **Balanced** | Default — no command needed | General conversation |
| **Detailed** | "Give me a detailed answer" / "Explain fully" | Research, learning |

---

## 5. Voice Commands — Full Reference {#voice-commands}

---

### 🌦 Weather

| Command | Result |
|---|---|
| `What's the weather?` | Current local weather |
| `Weather in Tokyo` | Weather for any city |
| `Will it rain today?` | Rain forecast |
| `3-day forecast` | Extended forecast |
| `What's the humidity?` | Humidity, wind, UV index |
| `What's the temperature outside?` | Current temperature |

---

### 📰 News & Search

| Command | Result |
|---|---|
| `Latest news` | Top global headlines |
| `Tech news today` | Technology news |
| `Sports news` | Sports headlines |
| `Search for [topic]` | DuckDuckGo live search |
| `Look up [anything]` | Web search + AI summary |
| `Who is [person]?` | Wikipedia lookup |
| `What is [topic]?` | AI-powered explanation |

---

### 💰 Finance & Prices

| Command | Result |
|---|---|
| `Bitcoin price` | Live BTC/USD |
| `Ethereum price` | Live ETH price |
| `Gold price today` | Spot gold price |
| `Oil price` | Crude oil price |
| `Dollar to Peso` | Live USD/PHP rate |
| `Convert 100 USD to AED` | Any currency conversion |
| `Stock price of Apple` | Stock lookup |

---

### 🖼 Image Generation

| Command | Result |
|---|---|
| `Generate an image of a sunset` | AI image via Pollinations Flux |
| `Draw a futuristic city` | Any image |
| `Create a picture of [anything]` | Free, unlimited |
| `Make an illustration of [scene]` | Detailed illustrations |

---

### 📸 Photos & Vision

Tap **📎** to attach a photo first, then say:

| Command | Result |
|---|---|
| `What is this?` | Identifies objects and scene |
| `Read the text in this photo` | OCR text extraction |
| `Analyse this image` | Full AI description |
| `What animal is this?` | Species identification |
| `How many calories in this food?` | Food nutrition analysis |
| `Describe what you see` | Detailed scene description |
| `What emotion does this person show?` | Facial emotion reading |
| `Translate this text` | Translates text in image |

---

### 🧠 Brain & Cognitive Modules

Tap **🧠** in the top bar or say:

| Command | Opens |
|---|---|
| `Open brain` | H·E·N·R·Y Brain hub |
| `Start visualization` | Mental Imagery menu |
| `Ocean calm` | Ocean relaxation script |
| `Mountain visualization` | Mountain peak script |
| `Memory palace` | Memory technique session |
| `Creative vision` | Creative imagination session |
| `Cosmic journey` | Space visualization |
| `Performance visualization` | Peak performance prep |
| `Brain training` | Neural Plasticity exercises |
| `Stroop challenge` | Color-word Stroop test |
| `Working memory exercise` | N-back memory training |
| `Pattern recognition` | Pattern completion exercise |
| `Mind wandering session` | DMN mind-wandering |
| `Reflection journal` | Guided self-reflection |
| `Future self simulation` | 5-year future vision |
| `Empathy exercise` | Perspective-taking session |
| `Color sound synesthesia` | Color → tone experience |
| `Open memory banks` | View HENRY's memories |

---

### 🌍 Maps & Navigation

| Command | Result |
|---|---|
| `Open the map` | 3D Earth globe |
| `Navigate to [place]` | Google Maps turn-by-turn |
| `Get me an Uber` | Opens Uber app |
| `Order a Careem` | Opens Careem app |
| `Nearby restaurants` | Restaurants near you |
| `Nearby hospitals` | Hospitals near you |
| `How far is [city]?` | Distance + travel time |
| `What's the transit to [place]?` | Transit options |
| `Share my location` | Sends location link |
| `Tell me about [country]` | Country intel briefing |

---

### 📱 Device Control

| Command | Result |
|---|---|
| `Turn on flashlight` | Torch on |
| `Turn off flashlight` | Torch off |
| `Increase brightness` | Raises screen brightness |
| `Decrease brightness` | Lowers screen brightness |
| `Battery level` | Current battery % |
| `Enable Do Not Disturb` | Activates DND |
| `Disable Do Not Disturb` | Deactivates DND |
| `What time is it?` | Reads current time |
| `What is today's date?` | Reads current date |
| `Take a screenshot` | Captures screen |

---

### ⏰ Alarms, Timers & Reminders

| Command | Result |
|---|---|
| `Set an alarm for 7am` | Sets alarm |
| `Wake me up at 6:30` | Morning alarm |
| `Set a timer for 10 minutes` | Countdown timer |
| `Start a 25-minute Pomodoro` | Pomodoro timer |
| `Start stopwatch` | Stopwatch starts |
| `Stop stopwatch` | Stopwatch stops |
| `Remind me to [task] at [time]` | Reminder notification |
| `Remind me to drink water every hour` | Recurring reminder |

---

### 📞 Calls & Messages

| Command | Result |
|---|---|
| `Call [name]` | Dials from contacts |
| `Text [name]: [message]` | Sends SMS |
| `WhatsApp [name]` | Opens WhatsApp chat |
| `Send WhatsApp to [name]: [message]` | Sends WhatsApp message |
| `Email [name]` | Opens email draft |
| `Draft an email to [name] about [topic]` | AI-written email |

---

### 📅 Calendar & Scheduling

| Command | Result |
|---|---|
| `What's on my calendar today?` | Today's events |
| `What do I have tomorrow?` | Tomorrow's events |
| `Add meeting tomorrow at 10am` | Creates event |
| `Schedule [event] on [date] at [time]` | Calendar entry |
| `Cancel my 3pm meeting` | Removes event |

---

### 📄 Google Workspace

| Command | Result |
|---|---|
| `Create a Google Doc` | Opens new Doc |
| `Make a spreadsheet for my budget` | Google Sheet |
| `Create a presentation about [topic]` | Google Slides |
| `Open Google Drive` | Launches Drive |

---

### 🎵 Music & Media

| Command | Result |
|---|---|
| `Play music` | Starts playback |
| `Pause music` | Pauses |
| `Next track` | Skips to next |
| `Previous song` | Goes back |
| `Volume up` | Raises media volume |
| `Volume down` | Lowers volume |
| `What song is this?` | Music identification |

---

### 📱 App Launcher (35+ apps)

| Command | Opens |
|---|---|
| `Open YouTube` | YouTube |
| `Open Instagram` | Instagram |
| `Open Facebook` | Facebook |
| `Open Twitter` / `Open X` | X (Twitter) |
| `Open TikTok` | TikTok |
| `Open Spotify` | Spotify |
| `Open Netflix` | Netflix |
| `Open Gmail` | Gmail |
| `Open Chrome` | Chrome browser |
| `Open Maps` | Google Maps |
| `Open Settings` | Android Settings |
| `Open Camera` | Camera app |
| `Open Calculator` | Calculator |
| `Open [any app name]` | Launches matching app |

---

### 🏋 Health & Wellness

| Command | Result |
|---|---|
| `Start a workout` | Fitness coach session |
| `Log my weight as 75kg` | Fitness log |
| `How many calories in a banana?` | Nutrition info |
| `Calculate my BMI` | BMI calculator (asks height/weight) |
| `How much water should I drink?` | Hydration guide |
| `Breathing exercise` | Guided breathing |
| `Start sleep mode` | Sleep timer + DND |
| `Log my mood` | Mood tracker |
| `I feel stressed` | Breathing + calming guidance |
| `Start Pomodoro` | 25-minute focus timer |
| `Track my habit: [habit]` | Habit tracker |
| `How is my streak?` | Habit streak check |

---

### 💰 Finance & Shopping

| Command | Result |
|---|---|
| `Add milk to my shopping list` | Shopping list |
| `Show my shopping list` | Reads list |
| `Remove eggs from my list` | Updates list |
| `Add task: finish report` | Task manager |
| `What's on my task list?` | Reads tasks |
| `Mark [task] as done` | Completes task |
| `I spent 50 AED on lunch` | Expense log |
| `How much have I spent today?` | Expense summary |
| `My monthly budget is 5000 AED` | Budget setting |

---

### 📚 Knowledge & Learning

| Command | Result |
|---|---|
| `Explain [topic] simply` | Plain-English explanation |
| `Translate "[phrase]" to [language]` | Translation |
| `Teach me a word in French` | Vocabulary lesson |
| `Quiz me on [subject]` | Trivia quiz |
| `Run this code: [code snippet]` | Code execution |
| `Solve [math problem]` | Math solver |
| `Scan this QR code` | QR/barcode reader |
| `Read this PDF` | PDF summariser |
| `Debate me on [topic]` | HENRY argues both sides |
| `Word of the day` | Vocabulary + definition |
| `Recipe for [dish]` | AI-generated recipe |
| `Meal plan for this week` | Weekly meal planner |

---

### 🔒 Security & Utilities

| Command | Result |
|---|---|
| `Save this password: [label] [password]` | Encrypted password vault |
| `What is my [label] password?` | Retrieves saved password |
| `Run a speed test` | Internet speed |
| `Scan this document` | Camera → clean scan |
| `Search my chats for [keyword]` | Chat history search |
| `Browse [website]` | In-app browser |
| `Copy [text]` | Clipboard manager |

---

### 🚨 Emergency

| Command | Result |
|---|---|
| `Emergency SOS` | Sends SMS with location to emergency contact |
| `Call emergency` | Dials 999 / 911 / local emergency |
| `I need help` | Emergency protocol |

---

### ⚙ HENRY Settings & Memory

| Command | Result |
|---|---|
| `Remember that I live in Dubai` | Saves to memory |
| `My name is [name]` | HENRY stores your name |
| `I work as a [job]` | Stores profession |
| `I'm interested in [topic]` | Adds to profile |
| `What do you know about me?` | Reads all memories |
| `Forget everything` | Clears all memory |
| `Clear chat` | Erases conversation |
| `Change voice` | Opens accent picker |
| `Brief mode` | Short answers |
| `Detailed mode` | Long answers |

---

## 6. Brain Modules {#brain}

Tap **🧠** in the top bar to open the Brain hub.

### Mental Imagery
Guided visual meditations narrated by HENRY:
- 🌊 **Ocean Calm** — peaceful beach relaxation
- 🏔 **Mountain Peak** — clarity and strength
- 🧠 **Memory Palace** — ancient memorisation technique
- ✨ **Creative Vision** — unlock your creative mind
- 🌌 **Cosmic Journey** — perspective through space
- 🌿 **Forest Healing** — nature therapy
- 🔥 **Peak Performance** — mental rehearsal for success
- 💡 **Problem Solving** — subconscious solution finding

### Neural Plasticity
Brain training exercises with score and streak tracking:
- **Working Memory** — N-back number sequence
- **Cognitive Flip** — reverse thinking challenges
- **Divergent Thinking** — creative open-ended problems
- **Dual Task** — do two things simultaneously
- **Number Sense** — mental arithmetic
- **Pattern Break** — complete the sequence
- **Word Reversal** — backwards spelling
- **Stroop Challenge** — color vs word conflict

### Default Mode Network
Structured mind-wandering and reflection:
- **Mind-Wandering Session** — guided open awareness
- **Reflection Journal** — HENRY asks deep questions, saves your answers
- **Creative Incubation** — state a problem and let it go
- **Future Self Simulation** — vivid 5-year vision + HENRY analysis
- **Empathy Expansion** — perspective-taking exercises
- **DMN Journal** — all your saved reflections

### Sensory Substitution
Experience senses differently:
- **Color → Sound** — each color plays its healing frequency
- **Motion → Haptic** — device motion becomes vibration patterns
- **Text → Touch** — words become screen flash Morse patterns
- **Braille Vibration** — braille-style haptic pulses

### Smart Memory Viewer
- See every fact HENRY has learned about you
- Add new memories manually
- Delete individual entries
- Clear all memories at once

---

## 7. Vision & Camera {#vision}

### Attaching Images
1. Tap **📎** in the input bar
2. Choose: **Camera** / **Gallery** / **File**
3. Image appears as preview above the input
4. Type or say your question
5. HENRY analyses the image with your question

### Vision Activity (🧠 → Vision Scanner)
Live camera AI with real-time detection:
- **Face detection** — bounding boxes around faces
- **Object tracking** — identifies and tracks objects
- **Image labelling** — labels everything in the scene
- Powered by Google ML Kit (on-device, no internet needed)

### Animal Scanner (🐾 button)
1. Tap **🐾** or say "Open animal scanner"
2. Take a photo or choose from gallery
3. HENRY identifies: species, habitat, diet, conservation status

---

## 8. Device Control {#device}

HENRY can control your phone by voice:

| Feature | How |
|---|---|
| **Flashlight** | "Turn on/off flashlight" |
| **Brightness** | "Increase/decrease brightness" |
| **Volume** | "Volume up/down" |
| **Do Not Disturb** | "Enable/disable DND" |
| **Battery** | "Battery level" |
| **Time/Date** | "What time is it?" / "What's the date?" |

### Car Mode
Say **"Car mode"** or tap → **CommandsDashboard → Car Mode**
- Large easy-to-tap buttons
- Voice-only navigation optimised for driving
- Hands-free calls and music control

---

## 9. Health & Wellness {#health}

### Fitness Coach
- Say **"Start a workout"**
- HENRY guides you through exercises with reps and timing
- Tracks your fitness log over time

### Breathing Exercise
- Say **"Breathing exercise"**
- Choose: 4-7-8 breathing, box breathing, or wim hof
- HENRY paces and narrates

### Sleep Mode
- Say **"Sleep mode"** or **"Goodnight HENRY"**
- Sets a sleep timer
- Enables Do Not Disturb
- Dims screen
- HENRY says goodnight

### Mood Tracker
- Say **"Log my mood"** or **"I feel [emotion]"**
- Tracks daily mood patterns
- HENRY responds with empathy based on your mood

---

## 10. Maps & Navigation {#maps}

### Earth Map (🌍)
1. Tap **🌍** in the top bar
2. A 3D globe loads
3. Tap any country for a full intel briefing:
   - Flag, capital, population
   - History, culture, tourism
   - Food, economy
4. Ask follow-up questions about the country

### Navigation
- **"Navigate to [place]"** → opens Google Maps with directions
- **"Get me an Uber"** → opens Uber with pickup from your location
- **"Careem to [place]"** → opens Careem

---

## 11. Communication {#communication}

### Calls
- Say **"Call [contact name]"**
- HENRY searches your contacts and dials

### SMS
- Say **"Text [name]: [your message]"**
- HENRY confirms and sends

### WhatsApp
- Say **"WhatsApp [name]"** → opens chat
- Say **"Send WhatsApp to [name]: [message]"** → sends directly

### Notifications
- After granting Notification Access:
- Say **"Read my notifications"** → HENRY reads them aloud
- Say **"Summarise my notifications"** → AI summary of all recent notifications

---

## 12. Productivity {#productivity}

### Google Workspace
Say what you want and HENRY creates it:
- **"Create a Google Doc about [topic]"**
- **"Make a budget spreadsheet"**
- **"Create a presentation on [topic]"**

### Task Manager
- **"Add task: [task name]"** — adds to list
- **"Show my tasks"** — reads all tasks
- **"Mark [task] as done"** — completes item
- **"Delete [task]"** — removes task

### Shopping List
- **"Add [item] to my shopping list"**
- **"Show my shopping list"**
- **"Remove [item] from list"**
- **"Clear my shopping list"**

### PDF Reader
- Attach a PDF via 📎
- Say **"Summarise this PDF"** or **"What does this document say?"**

---

## 13. Finance & Shopping {#finance}

### Expense Tracker
- **"I spent 50 AED on groceries"** — logs expense
- **"How much have I spent today?"** — daily total
- **"Monthly expenses"** — monthly summary

### Live Prices
- Crypto: Bitcoin, Ethereum, Solana, Cardano
- Metals: Gold, Silver
- Energy: Oil
- Forex: Any currency pair

### Password Vault
- **"Save password for Gmail: [password]"** — encrypts and stores
- **"What's my Gmail password?"** — retrieves it
- All passwords are AES-256 encrypted locally on your device

---

## 14. Voice Settings {#voice-settings}

### Changing Accent
**Method 1:** Tap the **VOICE** button in the top bar
**Method 2:** Long-press the **🗑️** trash icon

| Option | Voice | Character |
|---|---|---|
| 🇺🇸 American ♂ | American Male | Warm, confident |
| 🇺🇸 American ♀ | American Female | Friendly, clear |
| 🇵🇭 Filipino ♂ | Filipino Male | Natural Filipino |
| 🇵🇭 Filipino ♀ | Filipino Female | Warm Filipino |
| 🇫🇷 French ♂ | French Male | Sophisticated |
| 🇫🇷 French ♀ | French Female | Elegant |

### TTS Engine
HENRY uses the **Google TTS engine** for best quality. If voices sound robotic:
1. Go to Android **Settings**
2. **Accessibility → Text-to-speech output**
3. Set engine to **Google Text-to-speech**
4. Download a language pack if needed

---

## 15. Memory & Personalisation {#memory}

HENRY remembers things about you automatically from your conversations and stores them locally.

### What HENRY Remembers
- Your name
- Your city / location
- Your job or profession
- Your interests and hobbies
- Preferences you mention
- Important facts you share

### Managing Memory
| Command | Action |
|---|---|
| `What do you know about me?` | Shows all stored facts |
| `Remember that [fact]` | Manually adds a memory |
| `Forget that I [fact]` | Removes specific memory |
| `Forget everything` | Wipes all memory |

**Memory Viewer:** Tap **🧠 → Smart Memory** to see, add, and delete individual facts.

### Bilingual Support
HENRY detects your language automatically:
- Write or speak in **English** → HENRY replies in English
- Write or speak in **Filipino/Tagalog** → HENRY replies in Tagalog
- No setting needed — it's automatic

---

## 16. Tips & Tricks {#tips}

1. **Chain commands** — *"Check the weather, then tell me today's news, then set an alarm for 7am"*
2. **Be natural** — speak like you're talking to a person, not a machine
3. **Follow-up chips** — after every reply, HENRY shows 2-3 suggested follow-ups — tap them for instant continuation
4. **Long-press chips** — hold a suggestion chip to edit it and save your own custom command
5. **Attach before asking** — attach your image first, then say your question
6. **Give context** — *"I have a job interview tomorrow, help me prepare"* gets a much better response than *"interview tips"*
7. **Response mode** — say *"be detailed"* before asking something complex
8. **Wake word** — enable wake word and just say "HENRY" without tapping anything
9. **Orb tap** — tapping the orb starts listening; tapping again cancels
10. **The more you talk, the smarter HENRY gets** — his memory builds your personal profile over time
11. **Car Mode** — use Car Mode while driving for safe hands-free operation
12. **Emergency contact** — set up your emergency contact in the app for SOS features

---

## 17. Troubleshooting {#troubleshooting}

| Problem | Solution |
|---|---|
| **Mic button not working** | Go to Settings → Apps → H·E·N·R·Y → Permissions → enable Microphone |
| **Voice input not recognising** | Speak slowly, reduce background noise, check internet connection |
| **TTS voice is silent** | Settings → Accessibility → TTS → install Google TTS |
| **TTS sounds robotic** | Change accent in VOICE picker; install Google TTS engine |
| **"All systems resting" message** | AI daily limit reached — wait a few hours or try again |
| **Server error 500** | Check internet connection; Vercel backend may be restarting |
| **Image not recognised** | Use JPG under 2MB; compress before attaching |
| **Map not loading** | Check internet; allow JavaScript in WebView |
| **App crashes on launch** | Uninstall and reinstall; grant all permissions on fresh install |
| **Contacts not found** | Grant Contacts permission in Settings → Apps → H·E·N·R·Y |
| **Notifications not being read** | Enable Notification Access in Settings → Apps → Special app access |
| **Wake word not working** | Enable in app settings; keep app running in background |
| **Alarm not firing** | Grant alarm permissions; disable battery optimisation for H·E·N·R·Y |
| **Black screen on launch** | Force-stop, clear cache, reopen |
| **Build fails in GitHub Actions** | Check Actions tab for error; ensure all Java files are committed |

### Battery Optimisation
For best performance, disable battery optimisation for HENRY:
1. Settings → Battery → Battery optimisation
2. Find H·E·N·R·Y → select **Don't optimise**

This ensures wake word, reminders, and notifications work reliably in the background.

---

## Quick Reference Card

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  H·E·N·R·Y™ QUICK COMMANDS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🌦  "What's the weather?"
  📰  "Latest news"
  🖼  "Generate an image of..."
  🧭  "Navigate to..."
  📞  "Call [name]"
  ⏰  "Set alarm for [time]"
  🧠  "Open brain"
  🌍  "Open the map"
  🐾  "What animal is this?" + 📎
  💰  "Bitcoin price"
  🔦  "Turn on flashlight"
  📝  "Add task: [task]"
  🛒  "Add [item] to shopping list"
  🤫  "Enable Do Not Disturb"
  😴  "Sleep mode"
  🆘  "Emergency SOS"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  TAP ORB or 🎤 to speak
  📎 to attach image
  🧠 for Brain modules
  Long-press 🗑️ for voice settings
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

© 2026 H·E·N·R·Y Project. All rights reserved.
**H·E·N·R·Y™** — Hyperintelligence Engine Neural Reasoning Yield
*Built to be brilliant. Designed to be free.*
