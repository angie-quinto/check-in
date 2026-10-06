package com.angiequinto.checkin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores the user's reminder preference after a device restart. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && ReminderScheduler.isEnabled(context)) {
            ReminderScheduler.scheduleNext(context);
        }
    }
}
