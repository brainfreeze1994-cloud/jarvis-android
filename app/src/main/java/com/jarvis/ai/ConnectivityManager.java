package com.jarvis.ai;

import android.content.Context;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * H.E.N.R.Y. ConnectivityManager Utility Class.
 * Monitors real-time network capability changes and validates live reachability
 * to the Gemini / J.A.R.V.I.S. neural AI endpoint. Dispatches state updates
 * to registered UI listeners for visual indication of connection status.
 */
public class ConnectivityManager {

    private static final String TAG = "HENRY_Connectivity";
    private static final String GEMINI_HEALTH_PROBE_URL = "https://jarvis-ai-seven-dun.vercel.app/api/jarvis";
    private static final String GOOGLE_API_FALLBACK_URL = "https://generativeai.googleapis.com";
    private static final int PROBE_TIMEOUT_MS = 5000;

    public enum ConnectionState {
        CONNECTED("ONLINE • Gemini Neural Link Active", true, true),
        API_LOST("GEMINI CONNECTION LOST • Local Offline Brain Active", true, false),
        OFFLINE("DEVICE OFFLINE • No Internet Connection", false, false),
        CHECKING("CHECKING LINK • Probing Gemini API...", false, false);

        public final String defaultMessage;
        public final boolean hasLocalNetwork;
        public final boolean isGeminiReachable;

        ConnectionState(String defaultMessage, boolean hasLocalNetwork, boolean isGeminiReachable) {
            this.defaultMessage = defaultMessage;
            this.hasLocalNetwork = hasLocalNetwork;
            this.isGeminiReachable = isGeminiReachable;
        }
    }

    public interface OnConnectivityChangeListener {
        void onConnectivityChanged(ConnectionState state, String message);
    }

    private static volatile ConnectivityManager sInstance;

    private final Context appContext;
    private final android.net.ConnectivityManager systemConnectivityManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService probeExecutor = Executors.newSingleThreadExecutor();
    private final Set<OnConnectivityChangeListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private volatile ConnectionState currentState = ConnectionState.CHECKING;
    private volatile String currentMessage = ConnectionState.CHECKING.defaultMessage;
    private final AtomicBoolean isProbing = new AtomicBoolean(false);
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);

    private android.net.ConnectivityManager.NetworkCallback networkCallback;

    public static ConnectivityManager getInstance(Context context) {
        if (sInstance == null && context != null) {
            synchronized (ConnectivityManager.class) {
                if (sInstance == null) {
                    sInstance = new ConnectivityManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private ConnectivityManager(Context context) {
        this.appContext = context;
        this.systemConnectivityManager = (android.net.ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Begins real-time network capability monitoring and triggers an initial Gemini API probe.
     */
    public synchronized void startMonitoring() {
        if (isMonitoring.getAndSet(true)) {
            return;
        }

        if (systemConnectivityManager == null) {
            updateState(ConnectionState.OFFLINE, "Connectivity service unavailable");
            return;
        }

        // Check current network immediately
        boolean currentlyConnected = isSystemNetworkConnected();
        if (!currentlyConnected) {
            updateState(ConnectionState.OFFLINE, ConnectionState.OFFLINE.defaultMessage);
        } else {
            updateState(ConnectionState.CHECKING, ConnectionState.CHECKING.defaultMessage);
            checkGeminiConnectionNow();
        }

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            networkCallback = new android.net.ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.d(TAG, "Network became available. Probing Gemini API link...");
                    mainHandler.post(() -> {
                        if (currentState == ConnectionState.OFFLINE) {
                            updateState(ConnectionState.CHECKING, "Network restored • Probing Gemini API...");
                        }
                    });
                    checkGeminiConnectionNow();
                }

                @Override
                public void onLost(Network network) {
                    Log.d(TAG, "Network lost.");
                    // Check if another network interface is still active
                    boolean stillConnected = isSystemNetworkConnected();
                    if (!stillConnected) {
                        mainHandler.post(() -> updateState(ConnectionState.OFFLINE, ConnectionState.OFFLINE.defaultMessage));
                    } else {
                        checkGeminiConnectionNow();
                    }
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    boolean validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    if (!hasInternet) {
                        mainHandler.post(() -> updateState(ConnectionState.OFFLINE, "No internet access detected"));
                    } else if (validated && currentState != ConnectionState.CONNECTED) {
                        checkGeminiConnectionNow();
                    }
                }
            };

            systemConnectivityManager.registerNetworkCallback(request, networkCallback);
        } catch (Exception e) {
            Log.e(TAG, "Error registering network callback", e);
            // Fallback: check status manually
            checkGeminiConnectionNow();
        }
    }

    /**
     * Stops network monitoring and unregisters callbacks.
     */
    public synchronized void stopMonitoring() {
        if (!isMonitoring.getAndSet(false)) {
            return;
        }
        if (systemConnectivityManager != null && networkCallback != null) {
            try {
                systemConnectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
            networkCallback = null;
        }
    }

    /**
     * Checks if the device has an active physical/system data connection.
     */
    public boolean isSystemNetworkConnected() {
        if (systemConnectivityManager == null) return false;
        try {
            Network activeNetwork = systemConnectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities caps = systemConnectivityManager.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Initiates an asynchronous probe to verify live communication with the Gemini API server.
     */
    public void checkGeminiConnectionNow() {
        if (!isSystemNetworkConnected()) {
            updateState(ConnectionState.OFFLINE, ConnectionState.OFFLINE.defaultMessage);
            return;
        }

        if (isProbing.getAndSet(true)) {
            return; // Probe already in flight
        }

        probeExecutor.execute(() -> {
            boolean reachable = probeUrl(GEMINI_HEALTH_PROBE_URL);
            if (!reachable) {
                // Secondary check against Google API endpoint
                reachable = probeUrl(GOOGLE_API_FALLBACK_URL);
            }

            final boolean success = reachable;
            isProbing.set(false);

            mainHandler.post(() -> {
                if (success) {
                    updateState(ConnectionState.CONNECTED, ConnectionState.CONNECTED.defaultMessage);
                } else {
                    updateState(ConnectionState.API_LOST, ConnectionState.API_LOST.defaultMessage);
                }
            });
        });
    }

    private boolean probeUrl(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(PROBE_TIMEOUT_MS);
            conn.setReadTimeout(PROBE_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "HENRY-Android-Probe");
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            // Any HTTP response (2xx, 3xx, 4xx, 405 Method Not Allowed) proves network & server reachability
            return responseCode > 0;
        } catch (Exception e) {
            Log.w(TAG, "Probe failed for " + urlStr + ": " + e.getMessage());
            // Try GET with zero content if HEAD is not supported by endpoint
            try {
                if (conn != null) conn.disconnect();
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(PROBE_TIMEOUT_MS);
                conn.setReadTimeout(PROBE_TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", "HENRY-Android-Probe");
                int code = conn.getResponseCode();
                return code > 0;
            } catch (Exception e2) {
                return false;
            }
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Reports a successful live API response from JarvisApi to verify connection.
     */
    public void reportApiSuccess() {
        mainHandler.post(() -> {
            if (currentState != ConnectionState.CONNECTED) {
                updateState(ConnectionState.CONNECTED, ConnectionState.CONNECTED.defaultMessage);
            }
        });
    }

    /**
     * Reports a failure from JarvisApi, inspecting if it was caused by connectivity issues.
     */
    public void reportApiFailure(Throwable throwable) {
        mainHandler.post(() -> {
            if (!isSystemNetworkConnected()) {
                updateState(ConnectionState.OFFLINE, ConnectionState.OFFLINE.defaultMessage);
            } else {
                String msg = throwable != null && throwable.getMessage() != null
                        ? "GEMINI API ERROR: " + throwable.getMessage()
                        : ConnectionState.API_LOST.defaultMessage;
                updateState(ConnectionState.API_LOST, msg);
            }
        });
    }

    private void updateState(ConnectionState newState, String message) {
        this.currentState = newState;
        this.currentMessage = message != null ? message : newState.defaultMessage;
        Log.i(TAG, "Connection state changed: " + newState + " (" + this.currentMessage + ")");

        for (OnConnectivityChangeListener listener : listeners) {
            try {
                listener.onConnectivityChanged(newState, this.currentMessage);
            } catch (Exception e) {
                Log.e(TAG, "Error in connectivity listener", e);
            }
        }
    }

    /**
     * Subscribes a listener to real-time connectivity updates.
     * The listener will immediately receive a callback with the current state.
     */
    public void addListener(OnConnectivityChangeListener listener) {
        if (listener == null) return;
        listeners.add(listener);
        // Immediate dispatch of current state
        final ConnectionState state = currentState;
        final String msg = currentMessage;
        mainHandler.post(() -> listener.onConnectivityChanged(state, msg));
    }

    /**
     * Unsubscribes a listener from connectivity updates.
     */
    public void removeListener(OnConnectivityChangeListener listener) {
        if (listener == null) return;
        listeners.remove(listener);
    }

    public ConnectionState getCurrentState() {
        return currentState;
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public boolean isConnected() {
        return currentState == ConnectionState.CONNECTED;
    }

    public boolean isGeminiReachable() {
        return currentState.isGeminiReachable;
    }
}
