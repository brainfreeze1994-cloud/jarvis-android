package com.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JarvisLocationManager — High-Precision Coordinates & Real-Time Tracking Service.
 *
 * Manages device location tracking via Android's LocationManager (GPS, Network, and Passive).
 * Continuously monitors user coordinates, provides real-time position updates,
 * calculates accuracy/bearing/speed, and provides reverse-geocoded human addresses.
 */
public class JarvisLocationManager {

    private static final String TAG = "JarvisLocationManager";
    private static final long MIN_TIME_MS = 1500; // 1.5 seconds
    private static final float MIN_DISTANCE_M = 1.0f; // 1 meter

    private static volatile JarvisLocationManager instance;

    private final Context appContext;
    private final LocationManager systemLocationManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();

    private final Set<LocationUpdateListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Location lastKnownLocation;
    private boolean isTracking = false;

    public interface LocationUpdateListener {
        void onLocationChanged(@NonNull Location location);
        default void onStatusChanged(String provider, int status, Bundle extras) {}
        default void onProviderEnabled(@NonNull String provider) {}
        default void onProviderDisabled(@NonNull String provider) {}
    }

    public interface AddressCallback {
        void onAddressResolved(String addressLine, String city, String country);
        void onError(String error);
    }

    private final LocationListener internalLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateLocationInternal(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            mainHandler.post(() -> {
                for (LocationUpdateListener l : listeners) {
                    try {
                        l.onStatusChanged(provider, status, extras);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in listener onStatusChanged", e);
                    }
                }
            });
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            mainHandler.post(() -> {
                for (LocationUpdateListener l : listeners) {
                    try {
                        l.onProviderEnabled(provider);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in listener onProviderEnabled", e);
                    }
                }
            });
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            mainHandler.post(() -> {
                for (LocationUpdateListener l : listeners) {
                    try {
                        l.onProviderDisabled(provider);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in listener onProviderDisabled", e);
                    }
                }
            });
        }
    };

    private JarvisLocationManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.systemLocationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        loadBestInitialLocation();
    }

    public static JarvisLocationManager getInstance(Context context) {
        if (instance == null) {
            synchronized (JarvisLocationManager.class) {
                if (instance == null) {
                    instance = new JarvisLocationManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Check if location permissions are granted.
     */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if GPS or Network location providers are enabled on device.
     */
    public boolean isLocationProviderEnabled() {
        if (systemLocationManager == null) return false;
        boolean gps = false;
        boolean network = false;
        try {
            gps = systemLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {}
        try {
            network = systemLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {}
        return gps || network;
    }

    /**
     * Start continuous real-time coordinates tracking.
     */
    public synchronized void startTracking() {
        if (isTracking) return;
        if (!hasLocationPermission()) {
            Log.w(TAG, "startTracking failed: location permissions not granted.");
            return;
        }

        if (systemLocationManager == null) return;

        loadBestInitialLocation();

        try {
            // Register for GPS updates
            if (systemLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                systemLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        internalLocationListener,
                        Looper.getMainLooper()
                );
            }

            // Register for Network updates as fallback/faster initial fix
            if (systemLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                systemLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        internalLocationListener,
                        Looper.getMainLooper()
                );
            }

            isTracking = true;
            Log.d(TAG, "JarvisLocationManager tracking started.");
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException while requesting location updates", se);
        } catch (Exception e) {
            Log.e(TAG, "Exception while requesting location updates", e);
        }
    }

    /**
     * Stop location updates to conserve power.
     */
    public synchronized void stopTracking() {
        if (!isTracking) return;
        if (systemLocationManager != null) {
            try {
                systemLocationManager.removeUpdates(internalLocationListener);
            } catch (Exception e) {
                Log.e(TAG, "Error removing location updates", e);
            }
        }
        isTracking = false;
        Log.d(TAG, "JarvisLocationManager tracking stopped.");
    }

    public boolean isTracking() {
        return isTracking;
    }

    public synchronized void addListener(LocationUpdateListener listener) {
        if (listener == null) return;
        listeners.add(listener);
        if (lastKnownLocation != null) {
            mainHandler.post(() -> listener.onLocationChanged(lastKnownLocation));
        }
    }

    public synchronized void removeListener(LocationUpdateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void updateLocationInternal(Location location) {
        if (location == null) return;

        // Verify if new location is better than old location
        if (isBetterLocation(location, lastKnownLocation)) {
            lastKnownLocation = location;
        }

        final Location current = lastKnownLocation != null ? lastKnownLocation : location;
        mainHandler.post(() -> {
            for (LocationUpdateListener listener : listeners) {
                try {
                    listener.onLocationChanged(current);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying location listener", e);
                }
            }
        });
    }

    /**
     * Load best available cached location from available providers.
     */
    private void loadBestInitialLocation() {
        if (!hasLocationPermission() || systemLocationManager == null) return;

        try {
            Location best = null;
            List<String> providers = systemLocationManager.getAllProviders();
            if (providers != null) {
                for (String provider : providers) {
                    Location loc = systemLocationManager.getLastKnownLocation(provider);
                    if (loc != null) {
                        if (best == null || isBetterLocation(loc, best)) {
                            best = loc;
                        }
                    }
                }
            }
            if (best != null) {
                this.lastKnownLocation = best;
            }
        } catch (SecurityException ignored) {}
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) return true;

        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) return true;
        if (isSignificantlyOlder) return false;

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        if (isMoreAccurate) return true;
        if (isNewer && !isLessAccurate) return true;
        if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) return true;

        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) return provider2 == null;
        return provider1.equals(provider2);
    }

    public Location getLastLocation() {
        if (lastKnownLocation == null) {
            loadBestInitialLocation();
        }
        return lastKnownLocation;
    }

    public LatLng getCurrentLatLng() {
        Location loc = getLastLocation();
        if (loc != null) {
            return new LatLng(loc.getLatitude(), loc.getLongitude());
        }
        // Fallback to center coordinates if not yet determined
        return new LatLng(0, 0);
    }

    public boolean hasLocation() {
        return lastKnownLocation != null;
    }

    public double getLatitude() {
        Location loc = getLastLocation();
        return loc != null ? loc.getLatitude() : 0.0;
    }

    public double getLongitude() {
        Location loc = getLastLocation();
        return loc != null ? loc.getLongitude() : 0.0;
    }

    public float getAccuracy() {
        Location loc = getLastLocation();
        return loc != null ? loc.getAccuracy() : 0f;
    }

    /**
     * Human-friendly coordinates string (e.g. "37.7749° N, 122.4194° W").
     */
    public String getFormattedCoordinates() {
        Location loc = getLastLocation();
        if (loc == null) return "Unknown Coordinates";
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        String latDir = lat >= 0 ? "N" : "S";
        String lonDir = lon >= 0 ? "E" : "W";
        return String.format(Locale.US, "%.5f° %s, %.5f° %s", Math.abs(lat), latDir, Math.abs(lon), lonDir);
    }

    /**
     * Resolve street address for given location asynchronously.
     */
    public void getAddressAsync(Location location, AddressCallback callback) {
        if (location == null) {
            if (callback != null) callback.onError("Location is null");
            return;
        }

        geocoderExecutor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(addr.getAddressLine(i));
                    }
                    String fullAddress = sb.length() > 0 ? sb.toString() : "Current Location";
                    String city = addr.getLocality() != null ? addr.getLocality() : addr.getSubAdminArea();
                    String country = addr.getCountryName();

                    mainHandler.post(() -> {
                        if (callback != null) callback.onAddressResolved(fullAddress, city, country);
                    });
                } else {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError("No address found for coordinates");
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }
}
