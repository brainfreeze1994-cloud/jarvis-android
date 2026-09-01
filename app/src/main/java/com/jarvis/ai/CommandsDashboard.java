package com.jarvis.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Voice Commands Dashboard — full categorised list of all HENRY commands.
 * "What can you do?" / "Show commands" / "Help" / "Command list"
 */
public class CommandsDashboard {

    public static class Command {
        public final String category;
        public final String example;
        public final String description;
        Command(String category, String example, String description) {
            this.category = category; this.example = example; this.description = description;
        }
    }

    public static boolean isDashboardCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.equals("help") || lower.equals("commands") ||
               lower.contains("what can you do") || lower.contains("show commands") ||
               lower.contains("command list") || lower.contains("all commands") ||
               lower.contains("what commands") || lower.contains("capabilities") ||
               lower.contains("what do you know") || lower.equals("menu");
    }

    public static List<Command> getAll() {
        List<Command> list = new ArrayList<>();

        // AI & Chat
        list.add(new Command("AI & Chat", "Explain quantum physics like I'm 5", "Socratic tutor"));
        list.add(new Command("AI & Chat", "Give me a flashcard on DNA", "Study flashcard"));
        list.add(new Command("AI & Chat", "Quiz me on history", "Trivia quiz"));
        list.add(new Command("AI & Chat", "Argue both sides of AI ethics", "Debate mode"));
        list.add(new Command("AI & Chat", "Translate hello to Arabic", "Voice translator (30 langs)"));
        list.add(new Command("AI & Chat", "What can I make with eggs and rice?", "AI recipe generator"));
        list.add(new Command("AI & Chat", "Draft a WhatsApp to John: I'm late", "Smart compose"));

        // Weather & Maps
        list.add(new Command("Weather & Maps", "What's the weather forecast this week?", "7-day forecast"));
        list.add(new Command("Weather & Maps", "Navigate to Dubai Mall", "Google Maps navigation"));
        list.add(new Command("Weather & Maps", "How long to Burj Khalifa?", "Drive/walk ETA"));
        list.add(new Command("Weather & Maps", "Open map", "In-app OpenStreetMap"));
        list.add(new Command("Weather & Maps", "Restaurants near me", "Nearby places (OSM)"));
        list.add(new Command("Weather & Maps", "Share my location with Mom", "Location share"));
        list.add(new Command("Weather & Maps", "Tell me about the Eiffel Tower", "Place details + Wikipedia"));

        // Space & Astronomy
        list.add(new Command("Space & Asteroids", "Asteroid watch", "NASA Eyes on Asteroids & close approaches"));
        list.add(new Command("Space & Asteroids", "Where is ISS?", "Live Space Station telemetry & orbit"));
        list.add(new Command("Space & Asteroids", "NASA photo of the day", "NASA APOD deep space discovery"));

        // Finance
        list.add(new Command("Finance", "Bitcoin price", "Live crypto (CoinGecko)"));
        list.add(new Command("Finance", "USD to AED", "Live forex rates"));
        list.add(new Command("Finance", "Apple stock", "Live stock price (Yahoo)"));
        list.add(new Command("Finance", "Convert 500 EUR to GBP", "Currency converter (170+)"));
        list.add(new Command("Finance", "I spent 120 AED on food", "Expense tracker"));
        list.add(new Command("Finance", "My expenses this month", "Monthly spending report"));

        // Health & Fitness
        list.add(new Command("Health", "How many steps today?", "Step counter + goal"));
        list.add(new Command("Health", "Set my step goal to 12000", "Daily step goal"));
        list.add(new Command("Health", "My BMI — I'm 75kg and 175cm", "BMI calculator"));
        list.add(new Command("Health", "How much water should I drink?", "Water intake calculator"));
        list.add(new Command("Health", "How many calories do I need?", "TDEE / calorie calculator"));
        list.add(new Command("Health", "Goodnight Henry, wake me at 7", "Sleep mode + alarm"));
        list.add(new Command("Health", "I feel stressed", "Mood tracker"));
        list.add(new Command("Health", "Mood report", "Mood history & patterns"));

        // Productivity
        list.add(new Command("Productivity", "Add task: call dentist high priority", "Task manager"));
        list.add(new Command("Productivity", "My tasks", "Show to-do list"));
        list.add(new Command("Productivity", "Mark dentist done", "Complete a task"));
        list.add(new Command("Productivity", "Set reminder: meeting at 3pm", "Reminders"));
        list.add(new Command("Productivity", "Start pomodoro", "25/5 focus timer"));
        list.add(new Command("Productivity", "Set timer for 10 minutes", "Voice timer"));
        list.add(new Command("Productivity", "Start stopwatch", "Stopwatch"));
        list.add(new Command("Productivity", "Set alarm for 7am", "Alarm"));
        list.add(new Command("Productivity", "Focus mode", "DND + screen lock + motivation"));
        list.add(new Command("Productivity", "Add habit gym", "Habit tracker"));
        list.add(new Command("Productivity", "Mark gym done", "Habit check-in + streak"));
        list.add(new Command("Productivity", "My streaks", "Habit streak report"));

        // Notes & Journal
        list.add(new Command("Notes", "Note: buy milk", "Voice notes"));
        list.add(new Command("Notes", "My notes", "Read all notes"));
        list.add(new Command("Notes", "Journal entry: today was great", "Voice journal"));
        list.add(new Command("Notes", "Read my journal", "Journal entries"));
        list.add(new Command("Notes", "Word of the day", "Daily vocabulary"));

        // Device & Controls
        list.add(new Command("Device", "Torch on / off", "Flashlight"));
        list.add(new Command("Device", "Set brightness to 80%", "Screen brightness"));
        list.add(new Command("Device", "Do not disturb on", "DND mode"));
        list.add(new Command("Device", "Battery status", "Battery level + charging"));
        list.add(new Command("Device", "Alert me when battery drops below 20%", "Battery guardian"));
        list.add(new Command("Device", "Play Spotify / Next song", "Music control"));
        list.add(new Command("Device", "Volume up / down", "Volume control"));
        list.add(new Command("Device", "Record screen / Stop recording", "HD Screen Recorder + Floating HUD"));
        list.add(new Command("Device", "Screen record studio", "AI TikTok & YouTube caption + video publisher"));
        list.add(new Command("Device", "Test my internet speed", "Speed test"));
        list.add(new Command("Device", "What's on screen?", "Screen reader (Accessibility)"));

        // Contacts & Communication
        list.add(new Command("Contacts", "Call Mom", "Phone call"));
        list.add(new Command("Contacts", "Text Sarah: running late", "SMS"));
        list.add(new Command("Contacts", "WhatsApp John: I'm on my way", "WhatsApp message"));
        list.add(new Command("Contacts", "SOS", "Emergency alert + location"));
        list.add(new Command("Contacts", "Set SOS contact to Dad", "SOS contact setup"));

        // Camera & Scanning
        list.add(new Command("Camera", "What do you see?", "Live camera vision"));
        list.add(new Command("Camera", "Scan document", "OCR document scanner"));
        list.add(new Command("Camera", "Scan QR code", "QR code scanner"));
        list.add(new Command("Camera", "Analyse this photo", "AI image analysis"));

        // Apps & Search
        list.add(new Command("Apps", "Open YouTube", "App launcher (35+ apps)"));
        list.add(new Command("Apps", "Search Google for AI news", "Web search"));
        list.add(new Command("Apps", "Search chat for bitcoin", "Chat history search"));

        // Memory & Security
        list.add(new Command("Memory", "Save password for Netflix: pass123", "Password vault (AES-256)"));
        list.add(new Command("Memory", "What's my Netflix password?", "Retrieve password"));
        list.add(new Command("Memory", "Generate a strong password", "Password generator"));
        list.add(new Command("Memory", "What have we talked about most?", "Conversation insights"));
        list.add(new Command("Memory", "Copy that", "Copy last reply to clipboard"));
        list.add(new Command("Memory", "Export chat", "Export conversation"));

        return list;
    }

    /** Filter by keyword */
    public static List<Command> search(String keyword) {
        String kw = keyword.toLowerCase(Locale.US);
        List<Command> results = new ArrayList<>();
        for (Command c : getAll())
            if (c.example.toLowerCase(Locale.US).contains(kw) ||
                c.description.toLowerCase(Locale.US).contains(kw) ||
                c.category.toLowerCase(Locale.US).contains(kw))
                results.add(c);
        return results;
    }

    /** Format as readable string for chat */
    public static String formatCategory(String category) {
        List<Command> all = getAll();
        StringBuilder sb = new StringBuilder("[EMOTION:excited] **" + category + " Commands, sir:**\n\n");
        for (Command c : all)
            if (c.category.equalsIgnoreCase(category))
                sb.append("• \"").append(c.example).append("\" — ").append(c.description).append("\n");
        return sb.toString().trim();
    }

    public static String formatSummary() {
        List<Command> all = getAll();
        // Collect unique categories
        List<String> cats = new ArrayList<>();
        for (Command c : all)
            if (!cats.contains(c.category)) cats.add(c.category);

        StringBuilder sb = new StringBuilder("[EMOTION:excited] **H.E.N.R.Y Command Centre — " + all.size() + " commands, sir:**\n\n");
        for (String cat : cats) {
            int count = 0;
            for (Command c : all) if (c.category.equals(cat)) count++;
            sb.append("**").append(cat).append("** (").append(count).append(")\n");
        }
        sb.append("\nSay the category name for details, or search: \"show weather commands\".");
        return sb.toString().trim();
    }
}
