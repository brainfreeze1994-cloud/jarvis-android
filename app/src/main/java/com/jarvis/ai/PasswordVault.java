package com.jarvis.ai;

import android.content.Context;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Password Vault — AES-256 encrypted on-device password storage.
 * Master PIN protects vault. Passwords never leave the device.
 * "Save password for Netflix: mypass123"
 * "What's my Netflix password?"
 * "List my passwords"
 * "Delete Netflix password"
 * "Generate a password"
 */
public class PasswordVault {

    private static final String PREFS      = "vault_prefs";
    private static final String KEY_DATA   = "vault_data";
    private static final String KEY_SALT   = "vault_salt";
    private static final String KEY_PIN_H  = "vault_pin_hash";
    private static final String ALGO       = "AES/CBC/PKCS5Padding";
    private static final String KDF        = "PBKDF2WithHmacSHA256";

    // ── Detection ─────────────────────────────────────────────────────────────
    public static boolean isVaultCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("password") || lower.contains("vault") ||
               lower.contains("save login") || lower.contains("store password") ||
               lower.contains("my passwords") || lower.contains("generate password") ||
               lower.contains("strong password") || lower.contains("random password");
    }

    // ── PIN management ────────────────────────────────────────────────────────
    public static boolean hasPin(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                  .contains(KEY_PIN_H);
    }

    public static void setPin(Context ctx, String pin) {
        String hash = hashPin(pin);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putString(KEY_PIN_H, hash).apply();
    }

    public static boolean checkPin(Context ctx, String pin) {
        String stored = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                           .getString(KEY_PIN_H, null);
        return stored != null && stored.equals(hashPin(pin));
    }

    private static String hashPin(String pin) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(("henry_vault_" + pin).getBytes("UTF-8"));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) { return pin; }
    }

    // ── Password generation ───────────────────────────────────────────────────
    public static String generate(int length, boolean symbols) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        if (symbols) chars += "!@#$%^&*()_+-=";
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────
    public static String save(Context ctx, String pin, String service, String username, String password) {
        try {
            List<JSONObject> entries = loadRaw(ctx, pin);
            if (entries == null) return "[EMOTION:concerned] Wrong PIN, sir.";
            // Remove existing entry for same service
            entries.removeIf(e -> { try { return e.getString("service").equalsIgnoreCase(service); } catch (Exception ex) { return false; } });
            JSONObject entry = new JSONObject()
                .put("service", service).put("username", username).put("password", password);
            entries.add(entry);
            saveRaw(ctx, pin, entries);
            return "[EMOTION:warm] Password for **" + service + "** saved securely, sir. Vault locked.";
        } catch (Exception e) {
            return "[EMOTION:concerned] Vault error: " + e.getMessage();
        }
    }

    public static String retrieve(Context ctx, String pin, String service) {
        try {
            List<JSONObject> entries = loadRaw(ctx, pin);
            if (entries == null) return "[EMOTION:concerned] Wrong PIN, sir.";
            for (JSONObject e : entries) {
                if (e.optString("service", "").equalsIgnoreCase(service)) {
                    String user = e.optString("username", "");
                    String pass = e.getString("password");
                    return "[EMOTION:neutral] **" + service + "**\n" +
                        (user.isEmpty() ? "" : "Username: **" + user + "**\n") +
                        "Password: **" + pass + "**\n\nVault locked again, sir.";
                }
            }
            return "[EMOTION:neutral] No password found for **" + service + "**, sir.";
        } catch (Exception e) {
            return "[EMOTION:concerned] Vault error: " + e.getMessage();
        }
    }

    public static String listServices(Context ctx, String pin) {
        try {
            List<JSONObject> entries = loadRaw(ctx, pin);
            if (entries == null) return "[EMOTION:concerned] Wrong PIN, sir.";
            if (entries.isEmpty()) return "[EMOTION:neutral] Vault is empty, sir.";
            StringBuilder sb = new StringBuilder("[EMOTION:neutral] **Stored passwords, sir:**\n\n");
            for (int i = 0; i < entries.size(); i++) {
                sb.append(i + 1).append(". **").append(entries.get(i).optString("service")).append("**\n");
            }
            sb.append("\nSay 'What's my [service] password?' to retrieve.");
            return sb.toString();
        } catch (Exception e) { return "[EMOTION:concerned] Vault error: " + e.getMessage(); }
    }

    public static String delete(Context ctx, String pin, String service) {
        try {
            List<JSONObject> entries = loadRaw(ctx, pin);
            if (entries == null) return "[EMOTION:concerned] Wrong PIN, sir.";
            boolean removed = entries.removeIf(e -> {
                try { return e.getString("service").equalsIgnoreCase(service); } catch (Exception ex) { return false; }
            });
            if (removed) { saveRaw(ctx, pin, entries); return "[EMOTION:neutral] **" + service + "** removed from vault, sir."; }
            return "[EMOTION:neutral] **" + service + "** not found in vault, sir.";
        } catch (Exception e) { return "[EMOTION:concerned] Vault error: " + e.getMessage(); }
    }

    // ── Parse command ─────────────────────────────────────────────────────────
    /** Returns {action, service, username, password} or null */
    public static String[] parseCommand(String text) {
        String lower = text.toLowerCase(Locale.US);

        // Generate
        if (lower.contains("generate") || lower.contains("random password") || lower.contains("strong password"))
            return new String[]{"generate", null, null, null};

        // List
        if (lower.contains("my passwords") || lower.contains("list password") || lower.contains("show password"))
            return new String[]{"list", null, null, null};

        // Delete
        if (lower.contains("delete") || lower.contains("remove")) {
            Matcher m = Pattern.compile("(?:delete|remove)\\s+(?:the\\s+)?(?:password\\s+(?:for\\s+)?)?([\\w\\s.]+?)(?:\\s+password)?$",
                Pattern.CASE_INSENSITIVE).matcher(text.trim());
            if (m.find()) return new String[]{"delete", m.group(1).trim(), null, null};
        }

        // Retrieve: "what's my Netflix password"
        Matcher rm = Pattern.compile("(?:what(?:'s|\\s+is)\\s+my\\s+)?([\\w\\s.]+?)\\s+password",
            Pattern.CASE_INSENSITIVE).matcher(text.trim());
        if (rm.find() && !lower.startsWith("save") && !lower.startsWith("store")) {
            String svc = rm.group(1).trim();
            if (!svc.equalsIgnoreCase("my") && !svc.equalsIgnoreCase("a") && !svc.equalsIgnoreCase("the"))
                return new String[]{"get", svc, null, null};
        }

        // Save: "save password for Netflix: user123 / mypass"
        Matcher sm = Pattern.compile(
            "(?:save|store|add)\\s+(?:password\\s+for\\s+|login\\s+for\\s+)?([\\w\\s.]+?):\\s*([\\S]+)?(?:\\s*/\\s*([\\S]+))?",
            Pattern.CASE_INSENSITIVE).matcher(text.trim());
        if (sm.find()) {
            String svc  = sm.group(1).trim();
            String p1   = sm.group(2);
            String p2   = sm.group(3);
            String user = p2 != null ? p1 : null;
            String pass = p2 != null ? p2 : p1;
            return new String[]{"save", svc, user, pass};
        }

        return null;
    }

    // ── Encryption helpers ────────────────────────────────────────────────────
    private static byte[] getSalt(Context ctx) {
        String s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SALT, null);
        if (s != null) return Base64.decode(s, Base64.NO_WRAP);
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP)).apply();
        return salt;
    }

    private static SecretKey deriveKey(Context ctx, String pin) throws Exception {
        byte[] salt = getSalt(ctx);
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 65536, 256);
        SecretKeyFactory f = SecretKeyFactory.getInstance(KDF);
        return new SecretKeySpec(f.generateSecret(spec).getEncoded(), "AES");
    }

    private static String encrypt(Context ctx, String pin, String plaintext) throws Exception {
        SecretKey key = deriveKey(ctx, pin);
        byte[] iv     = new byte[16]; new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] enc = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
               Base64.encodeToString(enc, Base64.NO_WRAP);
    }

    private static String decrypt(Context ctx, String pin, String ciphertext) throws Exception {
        String[] parts = ciphertext.split(":");
        byte[] iv  = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] enc = Base64.decode(parts[1], Base64.NO_WRAP);
        SecretKey key = deriveKey(ctx, pin);
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return new String(cipher.doFinal(enc), "UTF-8");
    }

    private static List<JSONObject> loadRaw(Context ctx, String pin) {
        String stored = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, null);
        List<JSONObject> list = new ArrayList<>();
        if (stored == null || stored.isEmpty()) return list;
        try {
            String json = decrypt(ctx, pin, stored);
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(arr.getJSONObject(i));
            return list;
        } catch (Exception e) { return null; } // wrong pin = null
    }

    private static void saveRaw(Context ctx, String pin, List<JSONObject> entries) throws Exception {
        JSONArray arr = new JSONArray();
        for (JSONObject o : entries) arr.put(o);
        String encrypted = encrypt(ctx, pin, arr.toString());
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putString(KEY_DATA, encrypted).apply();
    }
}
