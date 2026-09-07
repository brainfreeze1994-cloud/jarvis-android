#!/usr/bin/env python3
import subprocess
import os
import sys

def escape_ps(s):
    return s.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)')

def build_postscript():
    ps = []
    # Header
    ps.append("%!PS-Adobe-3.0")
    ps.append("%%Title: HENRY Voice and Text Commands Manual")
    ps.append("%%Creator: H.E.N.R.Y. Neural Engine")
    ps.append("%%BoundingBox: 0 0 612 792") # Standard Letter 8.5 x 11 inch
    ps.append("%%Pages: (atend)")
    ps.append("%%EndComments")

    # Define reusable procedures
    ps.append("""
/PageWidth 612 def
/PageHeight 792 def
/MarginLeft 40 def
/MarginRight 572 def
/ContentWidth 532 def

/drawHeader {
    gsave
    % Header bar
    0.04 0.08 0.16 setrgbcolor
    0 742 612 50 rectfill
    % Cyan accent line
    0.0 0.83 1.0 setrgbcolor
    0 740 612 2 rectfill
    
    % Title text in header
    /Helvetica-Bold findfont 12 scalefont setfont
    1 1 1 setrgbcolor
    MarginLeft 762 moveto
    (H\\267E\\267N\\267R\\267Y\\231 ANDROID \\227 COMMAND MANUAL) show
    
    /Helvetica findfont 9 scalefont setfont
    0.7 0.85 1.0 setrgbcolor
    MarginLeft 750 moveto
    (HYPERINTELLIGENCE ENGINE NEURAL REASONING YIELD \\227 v24.0.0) show
    
    % Page badge
    /Helvetica-Bold findfont 9 scalefont setfont
    1 1 1 setrgbcolor
    MarginRight 756 moveto
    (PAGE ) show
    pageNum (      ) cvs show
    grestore
} def

/drawFooter {
    gsave
    % Footer line
    0.85 0.88 0.92 setrgbcolor
    MarginLeft 38 ContentWidth 1 rectfill
    
    /Helvetica findfont 8 scalefont setfont
    0.4 0.45 0.5 setrgbcolor
    MarginLeft 26 moveto
    (\\251 2026 H\\267E\\267N\\267R\\267Y\\231 Android \\227 Free & Open System \\227 Voice & Text Commands Reference) show
    
    /Helvetica-Bold findfont 8 scalefont setfont
    0.0 0.5 0.8 setrgbcolor
    MarginRight 26 moveto
    (https://ai.studio/build) show
    grestore
} def

/startPage {
    /pageNum pageNum 1 add def
    drawHeader
    drawFooter
    /currY 715 def
} def

/endPage {
    showpage
} def
""")

    # Data structure for categories and commands
    sections = [
        {
            "cat": "1. Live Web Search, Real-Time Knowledge & Deep Research",
            "desc": "Multi-source verified web search, live news briefings, release dates, and deep research.",
            "cmds": [
                ("Search for [topic] / Search the web for [topic]", "Voice/Text", "Live multi-source search (Wikipedia, DuckDuckGo, Google News)"),
                ("What is the newest iPhone? / Latest [device]", "Voice/Text", "Verified current tech specs, release dates & hardware comparisons"),
                ("Latest tech news today / News about [topic]", "Voice/Text", "Real-time news search, topic summaries & recent headlines"),
                ("Deep research: [complex question]", "Voice/Text", "Multi-model tournament synthesis citing verified live sources"),
                ("Who is [person] / What is [event]?", "Voice/Text", "Biographical and historical encyclopedia verification"),
                ("Tell me about [Country / City / Landmark]", "Voice/Text", "Geographic intel, culture, demographics & tourist spots"),
                ("Explain quantum computing like I'm 5", "Voice/Text", "Socratic breakdown adapting depth to user level"),
                ("Give me a flashcard on cellular respiration", "Voice/Text", "Generates interactive study flashcard"),
                ("Quiz me on world history / anatomy", "Voice/Text", "Launches interactive multiple-choice trivia quiz"),
                ("Argue both sides of AI regulation", "Voice/Text", "Debate mode: analyzes arguments and counterarguments"),
                ("Translate [sentence] to [language]", "Voice/Text", "Translates speech into 30+ spoken languages"),
                ("What can I cook with eggs, tomatoes and rice?", "Voice/Text", "Artisan culinary recipe generator with timing")
            ]
        },
        {
            "cat": "2. H.E.N.R.Y. Document & File Engine",
            "desc": "Autonomous multi-format document creator with APA 7 research citations, styling, and charts.",
            "cmds": [
                ("Create a PDF document on Climate Change", "Voice/Text", "Generates styled PDF with sections, tables & citations"),
                ("Generate a Word document on [topic]", "Voice/Text", "Builds .docx with headings, executive summary & bullet points"),
                ("Make a PowerPoint presentation on Mars Colonization", "Voice/Text", "Creates .pptx presentation deck with presenter notes"),
                ("Create an Excel sheet of monthly business budget", "Voice/Text", "Generates .xlsx workbook with formulas and headers"),
                ("Create a Markdown document on System Architecture", "Voice/Text", "Generates clean .md file with code blocks"),
                ("Generate document with research citations", "Voice/Text", "Injects real academic APA 7 references and data"),
                ("Create a recipe PDF for chocolate chip cookies", "Voice/Text", "Produces styled culinary recipe card with baking science"),
                ("Create a PDF of all commands", "Voice/Text", "Generates this complete commands manual directly as PDF")
            ]
        },
        {
            "cat": "3. Vision Intelligence & Multi-Attachment Vision",
            "desc": "Camera AI, computer vision, document OCR, object tracking, and multi-image analysis.",
            "cmds": [
                ("Classify image / What is this?", "Voice/Text", "ML Kit real-time label identification"),
                ("Detect objects / Locate objects", "Voice/Text", "Bounding box object detection overlay"),
                ("Track object in live camera", "Voice/Text", "Dynamic real-time object tracking with crosshair HUD"),
                ("Facial recognition / Detect face", "Voice/Text", "Facial landmark, contour, and emotion detection"),
                ("Find similar images / Reverse image search", "Voice/Text", "Visual embedding retrieval and similarity matcher"),
                ("Scan document / Clean OCR scan", "Voice/Text", "Auto-edge cropped document scanner with text extraction"),
                ("Scan QR code / Barcode", "Voice/Text", "Instant URL and payload QR reader"),
                ("Animal scanner / What animal is this?", "Voice/Text", "Wildlife species identification and habitat facts"),
                ("Attach multiple photos [up to 10] or PDF", "Voice/Text", "Tap [Paperclip] to analyze multiple visual attachments simultaneously")
            ]
        },
        {
            "cat": "4. System, Device Control & Screen Recording",
            "desc": "Direct hardware control, utility tools, screen capture, and floating overlay controls.",
            "cmds": [
                ("Torch on / Flashlight off", "Voice/Text", "Toggles device LED flashlight instantly"),
                ("Set brightness to 70%", "Voice/Text", "Adjusts screen backlight level"),
                ("Do Not Disturb on / off", "Voice/Text", "Toggles Android system DND mode"),
                ("Battery status / Battery report", "Voice/Text", "Speaks percentage, temperature, and charging state"),
                ("Alert me when battery drops below 25%", "Voice/Text", "Battery Guardian background monitoring alert"),
                ("Record screen / Stop recording", "Voice/Text", "HD system screen recorder with floating HUD control"),
                ("Screen record studio", "Voice/Text", "Studio mode: AI YouTube/TikTok title & script generator"),
                ("Play Spotify / Pause music / Next song", "Voice/Text", "Hardware media session playback control"),
                ("Volume up / Volume down / Set volume to 50%", "Voice/Text", "Adjusts media audio volume"),
                ("Test my internet speed", "Voice/Text", "Executes ping, download, and latency speed test"),
                ("Open [App Name] (e.g., YouTube, WhatsApp)", "Voice/Text", "Launches any of 35+ supported Android applications")
            ]
        },
        {
            "cat": "5. Navigation, 3D Earth Globe & Location",
            "desc": "Geographic intel, 3D globe visualization, turn-by-turn routing, and local services.",
            "cmds": [
                ("Open Earth map / 3D Globe", "Voice/Text", "Interactive 3D WebGL globe with geopolitical briefings"),
                ("Navigate to [Destination]", "Voice/Text", "Opens turn-by-turn Google Maps navigation"),
                ("How long to [Place] / Travel time", "Voice/Text", "Calculates ETA via driving or walking (OSRM)"),
                ("Restaurants near me / Coffee shops nearby", "Voice/Text", "Locates nearby amenities via OpenStreetMap"),
                ("Hospitals near me / Gas station nearby", "Voice/Text", "Emergency proximity navigation"),
                ("Book a ride / Call an Uber / Careem / Grab", "Voice/Text", "Dispatches destination directly to ride apps"),
                ("Share my location with [Contact]", "Voice/Text", "Generates and sends GPS location coordinates"),
                ("Tell me about [Landmark/City]", "Voice/Text", "Wikipedia geographic details and cultural history")
            ]
        },
        {
            "cat": "6. Space & Real-Time Astronomy Radar",
            "desc": "NASA live orbital telemetry, deep space imagery, and near-Earth celestial tracking.",
            "cmds": [
                ("Asteroid watch / Near-Earth asteroids", "Voice/Text", "NASA close-approach telemetry and hazard radar"),
                ("Where is the ISS? / Space station tracker", "Voice/Text", "Live International Space Station coordinates & speed"),
                ("NASA photo of the day / Deep space APOD", "Voice/Text", "Daily high-res astrophysics discovery & explanation"),
                ("Space weather / Solar flare status", "Voice/Text", "NOAA solar storm and geomagnetic condition briefing")
            ]
        },
        {
            "cat": "7. Finance, Crypto & Productivity Suite",
            "desc": "Live market tickers, personal accounting, secure vault, and task management.",
            "cmds": [
                ("Bitcoin price / Ethereum price / Solana", "Voice/Text", "Live cryptocurrency quotes via CoinGecko"),
                ("Gold price today / Crude oil price", "Voice/Text", "Spot commodity market prices"),
                ("Apple stock / Tesla stock / NVIDIA", "Voice/Text", "Stock ticker and daily percentage change"),
                ("Convert 250 USD to AED / EUR / PHP / GBP", "Voice/Text", "Real-time exchange rate for 170+ currencies"),
                ("I spent [amount] on [category]", "Voice/Text", "Logs expense to local SQLite ledger"),
                ("My expenses this month / Spending report", "Voice/Text", "Categorized breakdown of monthly expenditures"),
                ("Save password for [service]: [pass]", "Voice/Text", "Stores credential in AES-256 encrypted local vault"),
                ("What is my [service] password?", "Voice/Text", "Biometric-authenticated credential retrieval"),
                ("Generate a strong password", "Voice/Text", "Generates high-entropy cryptographic password"),
                ("Add task: [title] [high/medium/low priority]", "Voice/Text", "Creates to-do task with priority level"),
                ("My tasks / Mark [task] done", "Voice/Text", "Reviews to-do list and checks off completed items"),
                ("Add [item] to shopping list / My shopping list", "Voice/Text", "Manages voice shopping checklist")
            ]
        },
        {
            "cat": "8. Communication, Calls & Emergency SOS",
            "desc": "Voice-dispatched cellular calls, SMS messaging, WhatsApp, and emergency distress.",
            "cmds": [
                ("Call [Contact Name]", "Voice/Text", "Initiates native cellular phone call"),
                ("Text [Contact Name]: [message]", "Voice/Text", "Dispatches SMS text message to contact"),
                ("WhatsApp [Name]: [message]", "Voice/Text", "Opens direct WhatsApp chat with prefilled text"),
                ("Draft an email to [Name] about [Subject]", "Voice/Text", "Prepares rich email draft ready to send"),
                ("SOS / Emergency distress", "Voice/Text", "Transmits distress alert with live GPS coordinates to SOS contact"),
                ("Set SOS emergency contact to [Name]", "Voice/Text", "Configures primary emergency broadcast recipient")
            ]
        },
        {
            "cat": "9. Health, Fitness & Mental Wellness",
            "desc": "Biometric health calculators, pedometer, sleep mode, and emotional wellness.",
            "cmds": [
                ("How many steps today? / Step counter", "Voice/Text", "Reports daily step count and distance walked"),
                ("Set my step goal to [number]", "Voice/Text", "Updates daily physical activity target"),
                ("My BMI -- I'm [weight] kg and [height] cm", "Voice/Text", "Calculates BMI and clinical classification"),
                ("How much water should I drink today?", "Voice/Text", "Hydration calculation based on climate and activity"),
                ("Calorie calculator / What is my TDEE?", "Voice/Text", "Total Daily Energy Expenditure estimation"),
                ("Goodnight Henry, wake me at 6:30 AM", "Voice/Text", "Activates Sleep Mode: DND + alarm + low brightness"),
                ("Start breathing exercise / 4-7-8 breathing", "Voice/Text", "Haptic-guided diaphragmatic relaxation session"),
                ("I feel stressed / Log my mood", "Voice/Text", "Emotional tracking and reflective journaling"),
                ("Mood report / Mood trends", "Voice/Text", "Historical mood distribution analysis")
            ]
        },
        {
            "cat": "10. Brain Hub & Cognitive Modules",
            "desc": "Neuroscience-inspired cognitive expansion and brain training protocols.",
            "cmds": [
                ("Open brain / Neural command center", "Voice/Text", "Opens interactive 9-region cognitive brain view"),
                ("Guided visualization / Mental imagery", "Voice/Text", "8 immersive audio-guided visualization journeys"),
                ("Ocean calm / Mountain ascent / Cosmic perspective", "Voice/Text", "Specific guided imagery relaxation sessions"),
                ("Neural plasticity training / Brain exercises", "Voice/Text", "Stroop, Dual N-Back, and executive function drills"),
                ("Default mode network / Mind wandering", "Voice/Text", "Incubation reflection and future-self dialogue"),
                ("Sensory substitution / Color to sound", "Voice/Text", "Translates visual frequencies to audio harmonics"),
                ("Haptic braille vibration", "Voice/Text", "Vibrotactile text encoding on device hardware")
            ]
        },
        {
            "cat": "11. Real-Time Trackers & Smart Home",
            "desc": "Live flight tracking, sports scores, logistics tracking, and home automation.",
            "cmds": [
                ("Track flight [Flight Number] (e.g. EK201, AA100)", "Voice/Text", "Live aircraft altitude, route, departure/arrival ETA"),
                ("Live sports scores / NBA / Premier League", "Voice/Text", "Real-time game scores, standings and match recaps"),
                ("Track package [Tracking Number]", "Voice/Text", "Automated carrier detection (DHL, FedEx, UPS, Aramex)"),
                ("Turn on living room lights / Smart home", "Voice/Text", "Direct bridge to Google Home or Amazon Alexa ecosystem")
            ]
        },
        {
            "cat": "12. Time, Notes, Alarms & Security",
            "desc": "Timers, voice notes, daily digests, and biometric app lock.",
            "cmds": [
                ("Set timer for [X] minutes / Start countdown", "Voice/Text", "Background audio countdown timer"),
                ("Start stopwatch / Stop stopwatch", "Voice/Text", "High-precision millisecond stopwatch"),
                ("Set alarm for [time] (e.g. 7:00 AM)", "Voice/Text", "Native Android system alarm"),
                ("Start Pomodoro / 25 minute focus", "Voice/Text", "Productivity interval timer with break cues"),
                ("Note: [text] / Read my voice notes", "Voice/Text", "Quick speech-to-text note capture"),
                ("Journal entry: [thoughts] / Read my journal", "Voice/Text", "Timestamped personal voice journal"),
                ("Lock app / Biometric lock / Enable face unlock", "Voice/Text", "Biometric facial & fingerprint security gate"),
                ("Stealth mode on", "Voice/Text", "Conceals app preview and silences notifications")
            ]
        }
    ]

    ps.append("/pageNum 0 def")
    ps.append("startPage")

    # Cover Banner
    ps.append("""
gsave
% Hero container
0.03 0.07 0.14 setrgbcolor
MarginLeft 620 ContentWidth 85 rectfill
0.0 0.83 1.0 setrgbcolor
MarginLeft 620 ContentWidth 3 rectfill

/Helvetica-Bold findfont 22 scalefont setfont
1 1 1 setrgbcolor
MarginLeft 18 add 675 moveto
(H\\267E\\267N\\267R\\267Y\\231 \\227 COMMAND DIRECTORY) show

/Helvetica-Bold findfont 10 scalefont setfont
0.0 0.83 1.0 setrgbcolor
MarginLeft 18 add 658 moveto
(HYPERINTELLIGENCE ENGINE NEURAL REASONING YIELD \\227 COMPLETE SYSTEM MANUAL) show

/Helvetica findfont 9 scalefont setfont
0.8 0.85 0.9 setrgbcolor
MarginLeft 18 add 638 moveto
(All Voice and Text Commands \\227 Offline Engines \\227 Autonomous Document Creator \\227 Vision Intelligence) show

grestore
/currY 605 def
""")

    # Overview box
    ps.append("""
gsave
0.95 0.97 1.0 setrgbcolor
MarginLeft currY 20 sub ContentWidth 30 rectfill
0.2 0.5 0.8 setrgbcolor
1 setlinewidth
MarginLeft currY 20 sub ContentWidth 30 rectstroke

/Helvetica-Bold findfont 9 scalefont setfont
0.05 0.2 0.4 setrgbcolor
MarginLeft 8 add currY 6 sub moveto
(HOW TO COMMAND H.E.N.R.Y: ) show
/Helvetica findfont 8.5 scalefont setfont
0.1 0.15 0.25 setrgbcolor
(Tap the central animated Orb or [Mic] button for speech, or type in the input bar and press [Send]. Say ) show
/Helvetica-Bold findfont 8.5 scalefont setfont
("HENRY") show
/Helvetica findfont 8.5 scalefont setfont
( to trigger hands-free wake word.) show
grestore
/currY currY 38 sub def
""")

    for s_idx, sec in enumerate(sections):
        # Check if we need a new page for section header (needs at least 110 pt)
        ps.append(f"""
currY 110 lt {{
    endPage
    startPage
}} if
""")
        cat_title = escape_ps(sec["cat"])
        cat_desc = escape_ps(sec["desc"])

        # Section Header Banner
        ps.append(f"""
gsave
0.08 0.16 0.28 setrgbcolor
MarginLeft currY 18 sub ContentWidth 20 rectfill
0.0 0.83 1.0 setrgbcolor
MarginLeft currY 18 sub 4 20 rectfill

/Helvetica-Bold findfont 10.5 scalefont setfont
1 1 1 setrgbcolor
MarginLeft 12 add currY 4 sub moveto
({cat_title}) show
grestore
/currY currY 23 sub def

gsave
/Helvetica-Oblique findfont 8 scalefont setfont
0.35 0.4 0.48 setrgbcolor
MarginLeft 6 add currY moveto
({cat_desc}) show
grestore
/currY currY 12 sub def

% Table Header
gsave
0.9 0.93 0.96 setrgbcolor
MarginLeft currY 12 sub ContentWidth 14 rectfill
0.75 0.8 0.86 setrgbcolor
MarginLeft currY 12 sub ContentWidth 1 rectfill

/Helvetica-Bold findfont 7.5 scalefont setfont
0.1 0.2 0.35 setrgbcolor
MarginLeft 8 add currY 3 sub moveto
(VOICE / TEXT COMMAND PHRASE) show
MarginLeft 210 add currY 3 sub moveto
(INPUT TYPE) show
MarginLeft 275 add currY 3 sub moveto
(ACTION & SYSTEM CAPABILITY) show
grestore
/currY currY 14 sub def
""")

        for row_idx, (cmd, itype, desc) in enumerate(sec["cmds"]):
            # Check if we need a new page for row (needs 20 pt)
            ps.append(f"""
currY 38 lt {{
    endPage
    startPage
    % Re-print mini table header
    gsave
    0.9 0.93 0.96 setrgbcolor
    MarginLeft currY 12 sub ContentWidth 14 rectfill
    /Helvetica-Bold findfont 7.5 scalefont setfont
    0.1 0.2 0.35 setrgbcolor
    MarginLeft 8 add currY 3 sub moveto
    (COMMAND PHRASE \\(CONT.\\)) show
    MarginLeft 210 add currY 3 sub moveto
    (TYPE) show
    MarginLeft 275 add currY 3 sub moveto
    (ACTION & SYSTEM CAPABILITY) show
    grestore
    /currY currY 14 sub def
}} if
""")
            bg_color = "0.98 0.99 1.0" if row_idx % 2 == 0 else "1.0 1.0 1.0"
            esc_cmd = escape_ps(cmd)
            esc_type = escape_ps(itype)
            esc_desc = escape_ps(desc)

            ps.append(f"""
gsave
{bg_color} setrgbcolor
MarginLeft currY 14 sub ContentWidth 14 rectfill
0.92 0.93 0.95 setrgbcolor
MarginLeft currY 14 sub ContentWidth 0.5 rectfill

% Command (Bold)
/Helvetica-Bold findfont 7.8 scalefont setfont
0.0 0.2 0.45 setrgbcolor
MarginLeft 8 add currY 4 sub moveto
({esc_cmd}) show

% Type Badge
/Helvetica-Bold findfont 6.8 scalefont setfont
0.2 0.55 0.3 setrgbcolor
MarginLeft 210 add currY 4 sub moveto
({esc_type}) show

% Description
/Helvetica findfont 7.5 scalefont setfont
0.2 0.22 0.26 setrgbcolor
MarginLeft 275 add currY 4 sub moveto
({esc_desc}) show
grestore
/currY currY 14 sub def
""")

        ps.append("/currY currY 10 sub def\n")

    # Pro Tips Section at the end
    ps.append("""
currY 90 lt {
    endPage
    startPage
} if

gsave
0.08 0.16 0.28 setrgbcolor
MarginLeft currY 18 sub ContentWidth 20 rectfill
0.0 0.83 1.0 setrgbcolor
MarginLeft currY 18 sub 4 20 rectfill

/Helvetica-Bold findfont 10.5 scalefont setfont
1 1 1 setrgbcolor
MarginLeft 12 add currY 4 sub moveto
(PRO TIPS & ADVANCED VOICE WORKFLOWS) show
grestore
/currY currY 24 sub def

gsave
0.96 0.97 0.99 setrgbcolor
MarginLeft currY 48 sub ContentWidth 52 rectfill
0.8 0.85 0.9 setrgbcolor
1 setlinewidth
MarginLeft currY 48 sub ContentWidth 52 rectstroke

/Helvetica-Bold findfont 8 scalefont setfont
0.05 0.2 0.4 setrgbcolor
MarginLeft 10 add currY 2 sub moveto
(1. Multi-Attachment Vision:) show
/Helvetica findfont 7.5 scalefont setfont
0.2 0.25 0.3 setrgbcolor
( Tap the [Paperclip] icon to select up to 10 images or read PDFs. HENRY processes multi-image visual questions.) show

/Helvetica-Bold findfont 8 scalefont setfont
0.05 0.2 0.4 setrgbcolor
MarginLeft 10 add currY 14 sub moveto
(2. Voice Accents & TTS Speed:) show
/Helvetica findfont 7.5 scalefont setfont
0.2 0.25 0.3 setrgbcolor
( Long-press the [Trash] button to select American, British, Filipino, French, Australian, or Japanese TTS accents.) show

/Helvetica-Bold findfont 8 scalefont setfont
0.05 0.2 0.4 setrgbcolor
MarginLeft 10 add currY 26 sub moveto
(3. Suggestion Chips Customization:) show
/Helvetica findfont 7.5 scalefont setfont
0.2 0.25 0.3 setrgbcolor
( Long-press any quick-suggestion chip under the Orb to customize it with your favorite command.) show

/Helvetica-Bold findfont 8 scalefont setfont
0.05 0.2 0.4 setrgbcolor
MarginLeft 10 add currY 38 sub moveto
(4. Direct File Creation & Sharing:) show
/Helvetica findfont 7.5 scalefont setfont
0.2 0.25 0.3 setrgbcolor
( Say "Create a PDF on [topic]" or "Create Word document" \\227 files open directly in your Android viewer or share sheet.) show
grestore
/currY currY 56 sub def
""")

    ps.append("endPage\n")
    ps.append("%%Trailer\n")
    ps.append("%%EOF\n")

    return "\n".join(ps)

if __name__ == "__main__":
    ps_content = build_postscript()
    ps_path = "commands_manual.ps"
    pdf_path = "HENRY_VOICE_AND_TEXT_COMMANDS.pdf"
    
    with open(ps_path, "w", encoding="latin-1") as f:
        f.write(ps_content)
    
    cmd = ["ps2pdf", ps_path, pdf_path]
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print("Error converting PS to PDF:", res.stderr, file=sys.stderr)
        sys.exit(1)
    
    # Also create symlink or copy to HENRY_COMMANDS.pdf for convenience
    subprocess.run(["cp", pdf_path, "HENRY_COMMANDS.pdf"])
    
    print(f"Successfully generated {pdf_path} and HENRY_COMMANDS.pdf")
    os.remove(ps_path)
