package com.jarvis.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * LocationTrackerService — Android Service to track user coordinates.
 * Allows foreground or bound tracking of coordinates across the application.
 */
public class LocationTrackerService extends Service implements JarvisLocationManager.LocationUpdateListener {

    public static final String ACTION_START_TRACKING = "com.jarvis.ai.START_LOCATION_TRACKING";
    public static final String ACTION_STOP_TRACKING = "com.jarvis.ai.STOP_LOCATION_TRACKING";
    public static final String CHANNEL_ID = "henry_location_channel";
    public static final int NOTIFICATION_ID = 2004;

    private final IBinder binder = new LocalBinder();
    private JarvisLocationManager locationManager;

    public class LocalBinder extends Binder {
        public LocationTrackerService getService() {
            return LocationTrackerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = JarvisLocationManager.getInstance(this);
        locationManager.addListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_TRACKING.equals(action)) {
                stopTrackingService();
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        startTrackingService();
        return START_STICKY;
    }

    private void startTrackingService() {
        if (locationManager != null) {
            locationManager.startTracking();
        }
    }

    private void stopTrackingService() {
        if (locationManager != null) {
            locationManager.stopTracking();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Broadcast or notify if needed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startTrackingService();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (locationManager != null) {
            locationManager.removeListener(this);
            locationManager.stopTracking();
        }
        super.onDestroy();
    }
}
