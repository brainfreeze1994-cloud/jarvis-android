package com.jarvis.ai;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class HenryWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    public static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_henry);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;

        // Tap title area → open app normally
        Intent open = new Intent(ctx, SplashActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(ctx, widgetId, open, flags);
        views.setOnClickPendingIntent(R.id.widget_title, openPi);

        // Tap mic button → open app and start listening immediately
        Intent mic = new Intent(ctx, MainActivity.class);
        mic.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                   | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mic.putExtra("start_listening", true);
        PendingIntent micPi = PendingIntent.getActivity(ctx, widgetId + 1000, mic, flags);
        views.setOnClickPendingIntent(R.id.widget_mic_btn, micPi);

        mgr.updateAppWidget(widgetId, views);
    }

    @Override
    public void onEnabled(Context ctx) {}

    @Override
    public void onDisabled(Context ctx) {}
}
