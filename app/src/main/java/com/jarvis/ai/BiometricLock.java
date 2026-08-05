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

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String reason);
    }

    public static boolean isLockEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOCK, false);
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
