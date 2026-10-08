package com.angiequinto.checkin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/** Schedules only the next prompt; no check-in response is ever written to storage. */
public final class ReminderScheduler {
    private static final int REQUEST_REMINDER = 1201;

    private ReminderScheduler() { }

    public static void enable(Context context) {
        UserPreferences.setRemindersEnabled(context, true);
        scheduleNext(context);
    }

    public static void disable(Context context) {
        UserPreferences.setRemindersEnabled(context, false);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(reminderPendingIntent(context));
    }

    public static boolean isEnabled(Context context) {
        return UserPreferences.remindersEnabled(context);
    }

    public static void scheduleNext(Context context) {
        if (!isEnabled(context) || CheckInWidgetProvider.hasWidgets(context)) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAt = SystemClock.elapsedRealtime() + UserPreferences.getInterval(context);
        // This remains reliable under normal use and does not require collecting any data
        // or asking for the special exact-alarm permission.
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                reminderPendingIntent(context)
        );
    }

    private static PendingIntent reminderPendingIntent(Context context) {
        Intent intent = new Intent(context, CheckInReceiver.class)
                .setAction(CheckInReceiver.ACTION_REMINDER);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
