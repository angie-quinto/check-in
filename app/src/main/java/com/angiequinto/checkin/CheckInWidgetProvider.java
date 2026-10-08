package com.angiequinto.checkin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.List;

/** Provides the home screen widget matching the app theme and suppressing notifications when active. */
public class CheckInWidgetProvider extends AppWidgetProvider {

    public static boolean hasWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, CheckInWidgetProvider.class));
            return ids != null && ids.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        long lastTime = MoodHistoryStore.getLastCheckInTime(context);
        long interval = UserPreferences.getInterval(context);
        long cooldown = Math.max(5 * 60 * 1000L, interval);
        long elapsed = System.currentTimeMillis() - lastTime;
        boolean inCooldown = (lastTime > 0 && elapsed < cooldown);

        List<MoodHistoryStore.Entry> entries = MoodHistoryStore.loadAll(context);
        boolean lastOkay = entries.isEmpty() || entries.get(0).okay;

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_check_in);

            if (inCooldown) {
                views.setViewVisibility(R.id.widget_title, android.view.View.GONE);
                views.setViewVisibility(R.id.widget_buttons_container, android.view.View.GONE);
                views.setViewVisibility(R.id.widget_message, android.view.View.VISIBLE);
                String msg = lastOkay
                        ? "Continue being positive. Carry this steady light forward."
                        : "Take a gentle breath. You're doing the best you can.";
                views.setTextViewText(R.id.widget_message, msg);
                views.setTextColor(R.id.widget_message, lastOkay ? android.graphics.Color.rgb(40, 81, 56) : android.graphics.Color.rgb(81, 67, 116));
            } else {
                views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE);
                views.setViewVisibility(R.id.widget_buttons_container, android.view.View.VISIBLE);
                views.setViewVisibility(R.id.widget_message, android.view.View.GONE);

                Intent okIntent = new Intent(context, CheckInReceiver.class).setAction(CheckInReceiver.ACTION_OK);
                PendingIntent okPendingIntent = PendingIntent.getBroadcast(
                        context, 1501, okIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                views.setOnClickPendingIntent(R.id.widget_ok, okPendingIntent);

                Intent notOkIntent = new Intent(context, CheckInReceiver.class).setAction(CheckInReceiver.ACTION_NOT_OK);
                PendingIntent notOkPendingIntent = PendingIntent.getBroadcast(
                        context, 1502, notOkIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                views.setOnClickPendingIntent(R.id.widget_not_ok, notOkPendingIntent);
            }

            Intent openIntent = new Intent(context, MainActivity.class);
            PendingIntent openPendingIntent = PendingIntent.getActivity(
                    context, 1503, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_brand, openPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_title, openPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        ReminderScheduler.scheduleNext(context);
    }

    public static void updateWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, CheckInWidgetProvider.class));
            if (ids != null && ids.length > 0) {
                new CheckInWidgetProvider().onUpdate(context, manager, ids);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        ReminderScheduler.scheduleNext(context);
    }
}
