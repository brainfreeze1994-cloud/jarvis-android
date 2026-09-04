package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.Executor;

/**
 * HENRY Biometric Security Manager
 * Supports Hardware-backed Facial Recognition, Iris Scanner, and Fingerprint Sensors
 * using AndroidX BiometricPrompt.
 */
public class HenryBiometricManager {

    private static final String PREFS_NAME = "henry_biometrics_prefs";
    private static final String KEY_BIOMETRIC_LOCK_ENABLED = "biometric_lock_enabled";

    public interface AuthCallback {
        void onAuthenticated();
        void onError(String error);
    }

    public static boolean isBiometricAvailable(Context context) {
        BiometricManager bm = BiometricManager.from(context);
        int canAuth = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isBiometricLockEnabled(Context context) {
        return BiometricLock.isLockEnabled(context);
    }

    public static void setBiometricLockEnabled(Context context, boolean enabled) {
        BiometricLock.setLockEnabled(context, enabled);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, enabled)
                .apply();
    }

    public static void authenticate(FragmentActivity activity, AuthCallback callback) {
        BiometricManager bm = BiometricManager.from(activity);
        int canAuth = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            callback.onError("Biometric sensors not configured or unavailable on this device.");
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt prompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onError(errString.toString());
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onAuthenticated();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(activity, "Biometric unrecognized. Please align face/iris or retry fingerprint.", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("H.E.N.R.Y BIOMETRIC SECURITY")
                .setSubtitle("Facial, Iris & Fingerprint Verification")
                .setDescription("Scan your face, iris, or fingerprint sensor to verify identity.")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        prompt.authenticate(promptInfo);
    }
}
