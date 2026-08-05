package com.jarvis.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.*;

/**
 * HenryGames — Mini-games and puzzles with HENRY
 * Chess (text-based), Word games, Riddles, 20 Questions,
 * Would You Rather, Trivia Tournament
 */
public class HenryGames {

    public interface GameCallback {
        void onOutput(String text);
        void onGameEnd(String summary);
    }

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final Random RNG = new Random();

    // ── Word Games ────────────────────────────────────────────────────────────
    private static final String[] RIDDLES = {
        "I speak without a mouth and hear without ears. I have no body, but come alive with wind. What am I?|An echo",
        "The more you take, the more you leave behind. What am I?|Footsteps",
        "I have cities, but no houses live there. I have mountains, but no trees grow there. I have water, but no fish swim there. What am I?|A map",
        "I can fly without wings. I can cry without eyes. Wherever I go, darkness follows me. What am I?|A cloud",
        "The person who makes it, doesn't need it. The person who buys it, doesn't want it. The person who uses it, doesn't know it. What am I?|A coffin",
        "I have hands but cannot clap. What am I?|A clock",
        "What has to be broken before you can use it?|An egg",
        "I'm light as a feather, yet the strongest man cannot hold me for more than 5 minutes. What am I?|Breath",
        "What can run but never walks, has a mouth but never talks, has a head but never weeps?|A river",
        "The more you remove from me, the bigger I become. What am I?|A hole",
    };

    private static final String[] WOULD_YOU_RATHER = {
        "Have the ability to fly OR be invisible?",
        "Always speak the truth OR always be believed?",
        "Live 100 years in the past OR 100 years in the future?",
        "Be twice as smart OR twice as happy?",
        "Have unlimited money but no friends OR true friendship but average wealth?",
        "Know how you will die OR know when you will die?",
        "Be the most famous person in the world OR the most respected?",
        "Give up the internet for a year OR never leave your city for a year?",
        "Have the power to read minds OR see the future?",
        "Lose all memories from the past 5 years OR never make new memories?",
    };

    private static final String[][] TRIVIA = {
        {"What is the capital of Iceland?", "Reykjavik", "Oslo", "Helsinki", "Copenhagen", "A"},
        {"How many bones are in the adult human body?", "206", "196", "216", "186", "A"},
        {"Which planet has the most moons?", "Saturn", "Jupiter", "Uranus", "Neptune", "A"},
        {"What year did the Berlin Wall fall?", "1989", "1991", "1987", "1993", "A"},
        {"What is the speed of light (approx)?", "299,792 km/s", "199,792 km/s", "399,792 km/s", "150,000 km/s", "A"},
        {"Which element has the symbol 'Au'?", "Gold", "Silver", "Aluminum", "Arsenic", "A"},
        {"Who painted the Sistine Chapel ceiling?", "Michelangelo", "Da Vinci", "Raphael", "Botticelli", "A"},
        {"What is the largest ocean on Earth?", "Pacific", "Atlantic", "Indian", "Arctic", "A"},
        {"How many sides does a dodecagon have?", "12", "10", "8", "14", "A"},
        {"What is the currency of Japan?", "Yen", "Won", "Yuan", "Baht", "A"},
    };

    private static int triviaIdx   = 0;
    private static int triviaScore = 0;
    private static int triviaRound = 0;

    // ── Game State ────────────────────────────────────────────────────────────
    private static String activeGame     = null;
    private static String currentRiddle  = null;
    private static String currentAnswer  = null;
    private static int riddleScore       = 0;
    private static int twentyQNumber     = 0;
    private static String secretThing    = null;

    public static boolean isGameQuery(String input) {
        String t = input.toLowerCase();
        return t.contains("play") || t.contains("game") || t.contains("riddle")
            || t.contains("trivia") || t.contains("quiz") || t.contains("puzzle")
            || t.contains("20 questions") || t.contains("twenty questions")
            || t.contains("would you rather") || t.contains("chess")
            || t.contains("word game") || t.contains("hangman");
    }

    public static String detectGame(String input) {
        String t = input.toLowerCase();
        if (t.contains("riddle")) return "riddle";
        if (t.contains("trivia") || t.contains("quiz")) return "trivia";
        if (t.contains("20 questions") || t.contains("twenty questions")) return "20q";
        if (t.contains("would you rather")) return "wyr";
        if (t.contains("chess")) return "chess";
        if (t.contains("hangman")) return "hangman";
        if (t.contains("word")) return "word";
        return "trivia"; // default
    }

    // ── Riddles ────────────────────────────────────────────────────────────────
    public static String startRiddle() {
        activeGame = "riddle";
        String[] pair = RIDDLES[RNG.nextInt(RIDDLES.length)].split("\\|");
        currentRiddle = pair[0];
        currentAnswer = pair[1].toLowerCase().trim();
        return "🎭 RIDDLE TIME!\n\n" + currentRiddle + "\n\n🤔 What am I? Type your answer, sir.";
    }

    public static String checkRiddleAnswer(String userAnswer) {
        if (currentAnswer == null) return startRiddle();
        String ua = userAnswer.toLowerCase().trim();
        boolean correct = ua.contains(currentAnswer) || currentAnswer.contains(ua);
        if (correct) {
            riddleScore++;
            String result = "✓ Brilliant! You got it — **" + currentAnswer + "**!\n\nScore: " + riddleScore + " 🏆\n\nAnother riddle? Say 'Next riddle' or 'Stop'";
            currentRiddle = null; currentAnswer = null;
            return result;
        } else {
            return "✗ Not quite, sir. Think again… or say 'Give up' for the answer.";
        }
    }

    public static String giveUpRiddle() {
        String ans = currentAnswer != null ? currentAnswer : "unknown";
        currentRiddle = null; currentAnswer = null;
        return "The answer was: **" + ans + "**\n\nSay 'Next riddle' to try another!";
    }

    // ── Trivia Tournament ──────────────────────────────────────────────────────
    public static String startTrivia() {
        activeGame = "trivia";
        triviaIdx = 0; triviaScore = 0; triviaRound = 0;
        return nextTriviaQuestion();
    }

    public static String nextTriviaQuestion() {
        if (triviaIdx >= TRIVIA.length) {
            String grade = triviaScore >= 8 ? "🏆 GENIUS" : triviaScore >= 6 ? "🥇 EXCELLENT" : triviaScore >= 4 ? "🥈 GOOD" : "🥉 KEEP PRACTICING";
            return "🏁 TRIVIA COMPLETE!\n\nFinal Score: " + triviaScore + "/" + TRIVIA.length + " " + grade + "\n\nSay 'Play again' or 'New game'";
        }
        String[] q = TRIVIA[triviaIdx];
        triviaRound++;
        return String.format("🎯 Question %d/%d\n\n%s\n\nA. %s\nB. %s\nC. %s\nD. %s\n\nSay A, B, C or D",
            triviaRound, TRIVIA.length, q[0], q[1], q[2], q[3], q[4]);
    }

    public static String checkTriviaAnswer(String answer) {
        if (triviaIdx >= TRIVIA.length) return startTrivia();
        String[] q = TRIVIA[triviaIdx];
        String correct = q[5];
        String correctText = q[1]; // answer A is always correct in our array
        boolean right = answer.trim().toUpperCase().startsWith(correct);
        triviaIdx++;
        if (right) {
            triviaScore++;
            return "✓ Correct! **" + correctText + "**\nScore: " + triviaScore + "/" + triviaIdx + "\n\n" + nextTriviaQuestion();
        } else {
            return "✗ Wrong. The answer was **" + correctText + "**\nScore: " + triviaScore + "/" + triviaIdx + "\n\n" + nextTriviaQuestion();
        }
    }

    // ── Would You Rather ──────────────────────────────────────────────────────
    public static String wouldYouRather() {
        activeGame = "wyr";
        String q = WOULD_YOU_RATHER[RNG.nextInt(WOULD_YOU_RATHER.length)];
        return "🤔 WOULD YOU RATHER?\n\n" + q + "\n\nTell me your choice and why, sir!";
    }

    // ── 20 Questions ───────────────────────────────────────────────────────────
    private static final String[] SECRET_THINGS = {
        "the Eiffel Tower", "a black hole", "the internet", "a dream", "the moon",
        "Cleopatra", "fire", "a mirror", "the ocean", "time", "a smartphone", "music"
    };

    public static String start20Questions() {
        activeGame = "20q";
        twentyQNumber = 0;
        secretThing = SECRET_THINGS[RNG.nextInt(SECRET_THINGS.length)];
        return "🔍 20 QUESTIONS!\n\nI'm thinking of something…\n\nAsk me yes/no questions to figure out what it is.\nYou have 20 questions. Question 1?";
    }

    public static String build20QSystemPrompt() {
        return "You are playing 20 Questions. The secret thing is: '" + secretThing + "'. " +
            "The user is asking yes/no questions. Answer only YES or NO (with a short hint if needed). " +
            "Count questions: currently on question " + twentyQNumber + "/20. " +
            "If they guess correctly, congratulate them. If they use all 20, reveal the answer.";
    }

    public static void increment20Q() { twentyQNumber++; }
    public static boolean is20QActive() { return "20q".equals(activeGame); }
    public static boolean isTriviaActive() { return "trivia".equals(activeGame); }
    public static boolean isRiddleActive() { return "riddle".equals(activeGame); }
    public static String getActiveGame() { return activeGame; }
    public static void endGame() { activeGame = null; secretThing = null; }
}
