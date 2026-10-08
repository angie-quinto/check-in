package com.angiequinto.checkin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** A private, on-device record of check-ins. It is never uploaded or shared. */
public final class MoodHistoryStore {
    private static final String DATABASE_NAME = "check_in_history.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE = "check_ins";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_MOOD = "mood";
    private static final String COLUMN_TIME = "checked_at";

    private MoodHistoryStore() { }

    public static void record(Context context, boolean okay) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MOOD, okay ? "okay" : "not_okay");
        values.put(COLUMN_TIME, System.currentTimeMillis());
        new Database(context).getWritableDatabase().insertOrThrow(TABLE, null, values);
    }

    public static List<Entry> loadAll(Context context) {
        List<Entry> entries = new ArrayList<>();
        try (Cursor cursor = new Database(context).getReadableDatabase().query(
                TABLE,
                new String[] { COLUMN_MOOD, COLUMN_TIME },
                null,
                null,
                null,
                null,
                COLUMN_TIME + " DESC"
        )) {
            int moodIndex = cursor.getColumnIndexOrThrow(COLUMN_MOOD);
            int timeIndex = cursor.getColumnIndexOrThrow(COLUMN_TIME);
            while (cursor.moveToNext()) {
                entries.add(new Entry("okay".equals(cursor.getString(moodIndex)), cursor.getLong(timeIndex)));
            }
        }
        return entries;
    }

    public static long getLastCheckInTime(Context context) {
        List<Entry> entries = loadAll(context);
        if (entries.isEmpty()) return 0L;
        return entries.get(0).checkedAt;
    }

    public static void clear(Context context) {
        new Database(context).getWritableDatabase().delete(TABLE, null, null);
    }

    public static Stats loadStats(Context context) {
        List<Entry> entries = loadAll(context);
        int okayCount = 0;
        for (Entry entry : entries) {
            if (entry.okay) {
                okayCount++;
            }
        }
        int total = entries.size();
        return new Stats(total, okayCount, total - okayCount);
    }

    public static final class Stats {
        public final int total;
        public final int okayCount;
        public final int notOkayCount;

        Stats(int total, int okayCount, int notOkayCount) {
            this.total = total;
            this.okayCount = okayCount;
            this.notOkayCount = notOkayCount;
        }

        public int okayPercentage() {
            if (total == 0) return 0;
            return Math.round((float) okayCount * 100f / total);
        }

        public int notOkayPercentage() {
            if (total == 0) return 0;
            return 100 - okayPercentage();
        }
    }

    public static final class Entry {
        public final boolean okay;
        public final long checkedAt;

        Entry(boolean okay, long checkedAt) {
            this.okay = okay;
            this.checkedAt = checkedAt;
        }
    }

    private static final class Database extends SQLiteOpenHelper {
        Database(Context context) {
            super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE " + TABLE + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_MOOD + " TEXT NOT NULL, "
                    + COLUMN_TIME + " INTEGER NOT NULL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            // Version one has no upgrade path yet.
        }
    }
}
