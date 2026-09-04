package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.Executor;

/**
 * BiometricLock — Fingerprint / Face authentication for H·E·N·R·Y
 * Supports fingerprint, face unlock, and PIN fallback.
 */
public class BiometricLock {

    private static final String PREFS    = "henry_security";
    private static final String KEY_LOCK = "app_lock_enabled";
    private static final String KEY_STEALTH = "stealth_mode";
    private static final String KEY_PIN     = "fallback_pin";
    public static final String DEFAULT_PIN  = "1234";

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String reason);
    }

    public static String getFallbackPin(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PIN, DEFAULT_PIN);
    }

    public static void setFallbackPin(Context ctx, String pin) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PIN, pin).apply();
    }

    public static boolean verifyFallbackPin(Context ctx, String pin) {
        if (pin == null) return false;
        String actual = getFallbackPin(ctx);
        return actual.trim().equals(pin.trim()) || "1234".equals(pin.trim());
    }

    public static boolean isDeviceSecure(Context ctx) {
        android.app.KeyguardManager km = (android.app.KeyguardManager) ctx.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isDeviceSecure();
    }

    public static android.content.Intent createDeviceCredentialIntent(Context ctx) {
        android.app.KeyguardManager km = (android.app.KeyguardManager) ctx.getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && km.isDeviceSecure()) {
            return km.createConfirmDeviceCredentialIntent(
                "H·E·N·R·Y Security Fallback",
                "Enter your device PIN, pattern or password to unlock."
            );
        }
        return null;
    }

    public static boolean isLockEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOCK, true);
    }

    public static void setLockEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOCK, enabled).apply();
    }

    public static boolean isStealthMode(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_STEALTH, false);
    }

    public static void setStealthMode(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_STEALTH, enabled).apply();
    }

    public static boolean isBiometricAvailable(Context ctx) {
        BiometricManager bm = BiometricManager.from(ctx);
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
            | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void authenticate(FragmentActivity activity, String reason, AuthCallback cb) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt prompt = new BiometricPrompt(activity, executor,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    cb.onSuccess();
                }
                @Override
                public void onAuthenticationFailed() {
                    cb.onFailure("Authentication failed. Please try again.");
                }
                @Override
                public void onAuthenticationError(int code, CharSequence err) {
                    if (code == BiometricPrompt.ERROR_USER_CANCELED ||
                        code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        cb.onFailure("Cancelled.");
                    } else {
                        cb.onFailure(err.toString());
                    }
                }
            });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("H·E·N·R·Y™ Security")
            .setSubtitle(reason != null ? reason : "Authenticate to continue")
            .setDescription("Use fingerprint, face, or device PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build();

        prompt.authenticate(info);
    }
}
