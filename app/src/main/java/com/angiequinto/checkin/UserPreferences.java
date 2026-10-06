package com.angiequinto.checkin;

import android.content.Context;
import android.content.SharedPreferences;

/** Keeps only the user's on-device app preferences, never their check-in responses. */
public final class UserPreferences {
    public static final long HOUR_MS = 60L * 60L * 1000L;

    private static final String PREFS = "check_in_preferences";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";
    private static final String KEY_NAME = "preferred_name";
    private static final String KEY_INTERVAL = "reminder_interval_ms";
    private static final String KEY_ENABLED = "reminders_enabled";

    private static final long[] INTERVALS = {
            HOUR_MS,
            2L * HOUR_MS,
            3L * HOUR_MS,
            4L * HOUR_MS,
            6L * HOUR_MS,
            12L * HOUR_MS,
            24L * HOUR_MS
    };

    private static final String[] INTERVAL_LABELS = {
            "Every hour",
            "Every 2 hours",
            "Every 3 hours",
            "Every 4 hours",
            "Every 6 hours",
            "Twice a day",
            "Once a day"
    };

    private UserPreferences() { }

    public static boolean isSetupComplete(Context context) {
        return preferences(context).getBoolean(KEY_SETUP_COMPLETE, false);
    }

    public static void completeSetup(Context context, String name, long intervalMs) {
        preferences(context).edit()
                .putBoolean(KEY_SETUP_COMPLETE, true)
                .putString(KEY_NAME, name)
                .putLong(KEY_INTERVAL, intervalMs)
                .apply();
    }

    public static String getName(Context context) {
        return preferences(context).getString(KEY_NAME, "");
    }

    public static void setName(Context context, String name) {
        preferences(context).edit().putString(KEY_NAME, name).apply();
    }

    public static long getInterval(Context context) {
        return preferences(context).getLong(KEY_INTERVAL, HOUR_MS);
    }

    public static void setInterval(Context context, long intervalMs) {
        preferences(context).edit().putLong(KEY_INTERVAL, intervalMs).apply();
    }

    public static boolean remindersEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setRemindersEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static long[] intervals() {
        return INTERVALS.clone();
    }

    public static String[] intervalLabels() {
        return INTERVAL_LABELS.clone();
    }

    public static int intervalIndex(long intervalMs) {
        for (int i = 0; i < INTERVALS.length; i++) {
            if (INTERVALS[i] == intervalMs) {
                return i;
            }
        }
        return 0;
    }

    public static String intervalLabel(long intervalMs) {
        return INTERVAL_LABELS[intervalIndex(intervalMs)];
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
