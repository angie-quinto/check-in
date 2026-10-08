package com.angiequinto.checkin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.widget.Toast;

/** Handles notification actions and privately records only the mood and local check-in time. */
public class CheckInReceiver extends BroadcastReceiver {
    public static final String ACTION_REMINDER = "com.angiequinto.checkin.REMINDER";
    public static final String ACTION_OK = "com.angiequinto.checkin.OK";
    public static final String ACTION_NOT_OK = "com.angiequinto.checkin.NOT_OK";

    private static final String CHANNEL_ID = "mood_check_ins";
    private static final int NOTIFICATION_ID = 1401;
    private static final int REQUEST_OK = 1402;
    private static final int REQUEST_NOT_OK = 1403;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_REMINDER.equals(action)) {
            showReminder(context);
            ReminderScheduler.scheduleNext(context);
            return;
        }

        if (ACTION_OK.equals(action) || ACTION_NOT_OK.equals(action)) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.cancel(NOTIFICATION_ID);

            long lastTime = MoodHistoryStore.getLastCheckInTime(context);
            long interval = UserPreferences.getInterval(context);
            long cooldown = Math.max(5 * 60 * 1000L, interval);
            if (lastTime > 0 && (System.currentTimeMillis() - lastTime) < cooldown) {
                Toast.makeText(context, "You recently checked in. Take a gentle breath.", Toast.LENGTH_SHORT).show();
                CheckInWidgetProvider.updateWidgets(context);
                return;
            }

            boolean okay = ACTION_OK.equals(action);
            MoodHistoryStore.record(context, okay);
            String message = okay
                    ? "Continue being positive. Carry this steady light forward."
                    : "Take a gentle breath. You're doing the best you can.";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            CheckInWidgetProvider.updateWidgets(context);
        }
    }

    public static void showReminder(Context context) {
        if (CheckInWidgetProvider.hasWidgets(context)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        createChannel(manager);

        Notification.Action okayAction = new Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_check),
                "I'm okay",
                actionIntent(context, ACTION_OK, REQUEST_OK)
        ).build();
        Notification.Action notOkayAction = new Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_heart),
                "Not really",
                actionIntent(context, ACTION_NOT_OK, REQUEST_NOT_OK)
        ).build();

        String name = UserPreferences.getName(context);
        String question = name.isEmpty()
                ? "How are you feeling right now?"
                : "How are you feeling, " + name + "?";
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_check_in)
                .setContentTitle(name.isEmpty() ? "A little check-in" : "A little check-in, " + name)
                .setContentText(question)
                .setStyle(new Notification.BigTextStyle().bigText(question))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .addAction(notOkayAction)
                .addAction(okayAction)
                .build();
        manager.notify(NOTIFICATION_ID, notification);
    }

    private static PendingIntent actionIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, CheckInReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void createChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Mood check-ins",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Gentle reminders to take a moment and notice how you feel.");
        channel.enableVibration(false);
        channel.setSound(null, null);
        channel.setLightColor(Color.rgb(40, 81, 56));
        manager.createNotificationChannel(channel);
    }
}
