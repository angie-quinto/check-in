package com.angiequinto.checkin;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** A small, private space for a check-in and its on-device history. */
public class MainActivity extends android.app.Activity {
    private static final int REQUEST_NOTIFICATIONS = 31;

    private final int canvas = Color.rgb(248, 248, 244);
    private final int card = Color.WHITE;
    private final int ink = Color.rgb(32, 33, 30);
    private final int mutedInk = Color.rgb(102, 104, 96);
    private final int sage = Color.rgb(221, 235, 221);
    private final int sageInk = Color.rgb(40, 81, 56);
    private final int lavender = Color.rgb(234, 228, 247);
    private final int lavenderInk = Color.rgb(81, 67, 116);
    private final int line = Color.rgb(232, 232, 226);

    private TextView reassurance;
    private TextView okayButton;
    private TextView notOkayButton;
    private TextView reminderDetail;
    private TextView intervalDetail;
    private TextView welcomeIntervalDetail;
    private LinearLayout historyPreviewEntries;
    private TextView historyPreviewEmpty;
    private LinearLayout statisticsCardContainer;
    private Switch reminderSwitch;
    private boolean settingSwitch;
    private boolean showingHistory;
    private long welcomeInterval = UserPreferences.HOUR_MS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(canvas);
        getWindow().setNavigationBarColor(canvas);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (UserPreferences.isSetupComplete(this)) {
            showMainScreen();
        } else {
            showWelcomeScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reminderSwitch != null) {
            updateReminderCopy();
        }
        refreshHistoryPreview();
    }

    private void showWelcomeScreen() {
        showingHistory = false;
        welcomeInterval = UserPreferences.getInterval(this);
        ScrollView scrollView = pageContainer();
        LinearLayout page = (LinearLayout) scrollView.getChildAt(0);

        TextView brand = label("CHECK IN", 12, sageInk);
        brand.setLetterSpacing(0.16f);
        page.addView(brand);
        TextView title = text("A little space\nfor you.", 34, ink, Typeface.BOLD);
        page.addView(title, margins(0, 18, 0, 0));
        TextView subtitle = text("Let’s make your check-ins feel a little more personal.", 17, mutedInk, Typeface.NORMAL);
        subtitle.setLineSpacing(dp(4), 1f);
        page.addView(subtitle, margins(0, 10, 0, 28));

        TextView nameLabel = label("WHAT SHOULD WE CALL YOU?", 11, mutedInk);
        nameLabel.setLetterSpacing(0.1f);
        page.addView(nameLabel);
        EditText nameInput = new EditText(this);
        nameInput.setHint("Name or nickname");
        nameInput.setHintTextColor(Color.rgb(148, 149, 143));
        nameInput.setTextColor(ink);
        nameInput.setTextSize(18);
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(32) });
        nameInput.setPadding(dp(18), dp(3), dp(18), dp(3));
        nameInput.setMinHeight(dp(58));
        nameInput.setBackground(roundRect(card, 18, line, 1));
        page.addView(nameInput, margins(0, 9, 0, 24));

        page.addView(createWelcomeRhythmCard(), margins(0, 0, 0, 24));
        TextView begin = actionButton("Start checking in", "Your preferences stay on this phone", sage, sageInk);
        begin.setGravity(Gravity.CENTER);
        begin.setOnClickListener(v -> {
            String name = cleanedName(nameInput.getText().toString());
            if (name.isEmpty()) {
                nameInput.setError("Add a name or nickname");
                nameInput.requestFocus();
                return;
            }
            UserPreferences.completeSetup(this, name, welcomeInterval);
            ReminderScheduler.enable(this);
            requestNotificationPermissionIfNeeded();
            showMainScreen();
        });
        page.addView(begin, fullWidth());

        TextView privacy = text("Your name, rhythm, and future check-ins stay only on this phone. Nothing is sent online.", 14, mutedInk, Typeface.NORMAL);
        privacy.setGravity(Gravity.CENTER);
        privacy.setLineSpacing(dp(3), 1f);
        page.addView(privacy, margins(8, 18, 8, 0));
        setContentView(scrollView);
    }

    private View createWelcomeRhythmCard() {
        LinearLayout rhythm = vertical();
        rhythm.setPadding(dp(20), dp(18), dp(20), dp(18));
        rhythm.setBackground(roundRect(card, 24, line, 1));
        rhythm.addView(text("Choose your rhythm", 18, ink, Typeface.BOLD));
        rhythm.addView(text("You can change this anytime.", 14, mutedInk, Typeface.NORMAL), margins(0, 4, 0, 14));
        welcomeIntervalDetail = settingButton(UserPreferences.intervalLabel(welcomeInterval));
        welcomeIntervalDetail.setContentDescription("Choose how often to check in");
        welcomeIntervalDetail.setOnClickListener(v -> showIntervalPicker(true));
        rhythm.addView(welcomeIntervalDetail, fullWidth());
        return rhythm;
    }

    private void showMainScreen() {
        showingHistory = false;
        reminderSwitch = null;
        ScrollView scrollView = pageContainer();
        LinearLayout page = (LinearLayout) scrollView.getChildAt(0);
        String name = UserPreferences.getName(this);

        TextView brand = label("CHECK IN", 12, sageInk);
        brand.setLetterSpacing(0.16f);
        page.addView(brand);
        TextView title = text("How are you feeling,\n" + name + "?", 32, ink, Typeface.BOLD);
        page.addView(title, margins(0, 18, 0, 0));
        TextView introduction = text("A small pause for the moment you’re in.", 17, mutedInk, Typeface.NORMAL);
        introduction.setLineSpacing(dp(4), 1f);
        page.addView(introduction, margins(0, 10, 0, 0));

        page.addView(createMoodCard(), margins(0, 28, 0, 14));
        page.addView(createHistoryCard(), margins(0, 0, 0, 14));
        page.addView(createStatisticsCard(), margins(0, 0, 0, 14));
        page.addView(createRhythmCard(), margins(0, 0, 0, 14));
        page.addView(createNameRow(), margins(0, 0, 0, 18));
        page.addView(createPrivacyNote(), fullWidth());
        setContentView(scrollView);
        updateReminderCopy();
    }

    private ScrollView pageContainer() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(canvas);
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(26), dp(24), dp(28));
        scrollView.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    private View createMoodCard() {
        LinearLayout moodCard = vertical();
        moodCard.setPadding(dp(22), dp(22), dp(22), dp(22));
        moodCard.setBackground(roundRect(card, 28, line, 1));
        moodCard.setElevation(dp(1));
        TextView overline = label("RIGHT NOW", 11, mutedInk);
        overline.setLetterSpacing(0.13f);
        moodCard.addView(overline);
        moodCard.addView(text("What feels most true?", 23, ink, Typeface.BOLD), margins(0, 9, 0, 18));
        okayButton = actionButton("I’m okay", "A simple moment of recognition", sage, sageInk);
        okayButton.setOnClickListener(v -> respond(true));
        moodCard.addView(okayButton, fullWidth());
        notOkayButton = actionButton("Not really", "It’s okay not to be okay", lavender, lavenderInk);
        notOkayButton.setOnClickListener(v -> respond(false));
        moodCard.addView(notOkayButton, margins(0, 12, 0, 0));
        reassurance = text("", 15, mutedInk, Typeface.NORMAL);
        reassurance.setVisibility(View.GONE);
        reassurance.setLineSpacing(dp(3), 1f);
        moodCard.addView(reassurance, margins(0, 17, 0, 0));
        updateMoodCardState();
        return moodCard;
    }

    private void updateMoodCardState() {
        if (okayButton == null || notOkayButton == null || reassurance == null) return;
        long lastTime = MoodHistoryStore.getLastCheckInTime(this);
        long interval = UserPreferences.getInterval(this);
        long cooldown = Math.max(5 * 60 * 1000L, interval);
        long elapsed = System.currentTimeMillis() - lastTime;
        boolean inCooldown = (lastTime > 0 && elapsed < cooldown);

        List<MoodHistoryStore.Entry> entries = MoodHistoryStore.loadAll(this);
        boolean lastOkay = entries.isEmpty() || entries.get(0).okay;

        if (inCooldown) {
            okayButton.setVisibility(View.GONE);
            notOkayButton.setVisibility(View.GONE);
            reassurance.setText(lastOkay
                    ? "Continue being positive. Carry this steady light forward."
                    : "Take a gentle breath. You're doing the best you can.");
            reassurance.setTextColor(lastOkay ? sageInk : lavenderInk);
            reassurance.setVisibility(View.VISIBLE);
        } else {
            okayButton.setVisibility(View.VISIBLE);
            notOkayButton.setVisibility(View.VISIBLE);
            reassurance.setVisibility(View.GONE);
        }
    }

    private View createHistoryCard() {
        LinearLayout history = vertical();
        history.setPadding(dp(20), dp(18), dp(20), dp(18));
        history.setBackground(roundRect(card, 24, line, 1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("Recent check-ins", 18, ink, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView viewAll = text("View all", 15, sageInk, Typeface.BOLD);
        viewAll.setPadding(dp(10), dp(8), 0, dp(8));
        viewAll.setClickable(true);
        viewAll.setFocusable(true);
        viewAll.setContentDescription("View all mood check-ins");
        viewAll.setOnClickListener(v -> showHistoryScreen());
        header.addView(viewAll);
        history.addView(header);

        historyPreviewEmpty = text("Your check-ins will appear here, with their date and time.", 14, mutedInk, Typeface.NORMAL);
        historyPreviewEmpty.setLineSpacing(dp(3), 1f);
        history.addView(historyPreviewEmpty, margins(0, 10, 0, 0));
        historyPreviewEntries = vertical();
        history.addView(historyPreviewEntries, margins(0, 10, 0, 0));
        refreshHistoryPreview();
        return history;
    }

    private void refreshHistoryPreview() {
        if (historyPreviewEntries == null || historyPreviewEmpty == null) return;
        List<MoodHistoryStore.Entry> entries = MoodHistoryStore.loadAll(this);
        historyPreviewEntries.removeAllViews();
        historyPreviewEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        int previewCount = Math.min(entries.size(), 3);
        for (int i = 0; i < previewCount; i++) {
            historyPreviewEntries.addView(createHistoryRow(entries.get(i)),
                    margins(0, i == 0 ? 0 : 8, 0, 0));
        }
        updateMoodCardState();
        if (statisticsCardContainer != null) {
            updateStatisticsCard(statisticsCardContainer);
        }
    }

    private View createStatisticsCard() {
        LinearLayout statsCard = vertical();
        statsCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        statsCard.setBackground(roundRect(card, 24, line, 1));
        statisticsCardContainer = statsCard;
        updateStatisticsCard(statsCard);
        return statsCard;
    }

    private void updateStatisticsCard(LinearLayout statsCard) {
        statsCard.removeAllViews();
        TextView overline = label("STATISTICS", 11, mutedInk);
        overline.setLetterSpacing(0.13f);
        statsCard.addView(overline);
        statsCard.addView(text("Mood summary & timeline", 18, ink, Typeface.BOLD), margins(0, 4, 0, 14));

        MoodHistoryStore.Stats stats = MoodHistoryStore.loadStats(this);
        if (stats.total == 0) {
            TextView empty = text("Your reflection insights and timeline will appear here once you check in.", 14, mutedInk, Typeface.NORMAL);
            empty.setLineSpacing(dp(3), 1f);
            statsCard.addView(empty);
        } else {
            LinearLayout row1 = new LinearLayout(this);
            row1.setGravity(Gravity.CENTER_VERTICAL);
            row1.addView(text("Total check-ins", 15, mutedInk, Typeface.NORMAL),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row1.addView(text(String.valueOf(stats.total), 16, ink, Typeface.BOLD));
            statsCard.addView(row1);

            View divider1 = new View(this);
            divider1.setBackgroundColor(line);
            statsCard.addView(divider1, fixedHeightMargins(1, 0, 10, 0, 10));

            LinearLayout row2 = new LinearLayout(this);
            row2.setGravity(Gravity.CENTER_VERTICAL);
            row2.addView(text("● I’m okay", 15, sageInk, Typeface.BOLD),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row2.addView(text(stats.okayCount + " (" + stats.okayPercentage() + "%)", 16, sageInk, Typeface.BOLD));
            statsCard.addView(row2);

            View divider2 = new View(this);
            divider2.setBackgroundColor(line);
            statsCard.addView(divider2, fixedHeightMargins(1, 0, 10, 0, 10));

            LinearLayout row3 = new LinearLayout(this);
            row3.setGravity(Gravity.CENTER_VERTICAL);
            row3.addView(text("● Not really", 15, lavenderInk, Typeface.BOLD),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row3.addView(text(stats.notOkayCount + " (" + stats.notOkayPercentage() + "%)", 16, lavenderInk, Typeface.BOLD));
            statsCard.addView(row3);

            View divider3 = new View(this);
            divider3.setBackgroundColor(line);
            statsCard.addView(divider3, fixedHeightMargins(1, 0, 14, 0, 14));

            statsCard.addView(text("Recent check-in rhythm & time", 15, ink, Typeface.BOLD), margins(0, 0, 0, 8));

            List<MoodHistoryStore.Entry> entries = MoodHistoryStore.loadAll(this);
            int graphLimit = Math.min(entries.size(), 5);
            for (int i = 0; i < graphLimit; i++) {
                MoodHistoryStore.Entry entry = entries.get(i);
                LinearLayout graphRow = new LinearLayout(this);
                graphRow.setGravity(Gravity.CENTER_VERTICAL);
                int fg = entry.okay ? sageInk : lavenderInk;
                TextView dot = text("●", 14, fg, Typeface.NORMAL);
                graphRow.addView(dot, new LinearLayout.LayoutParams(dp(20), LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView timeLabel = text(formatCheckInTime(entry.checkedAt), 13, mutedInk, Typeface.NORMAL);
                graphRow.addView(timeLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                TextView moodText = text(entry.okay ? "I’m okay" : "Not really", 13, fg, Typeface.BOLD);
                graphRow.addView(moodText);

                statsCard.addView(graphRow, margins(0, i == 0 ? 0 : 6, 0, 0));
            }
        }
    }

    private void showHistoryScreen() {
        showingHistory = true;
        reminderSwitch = null;
        ScrollView scrollView = pageContainer();
        LinearLayout page = (LinearLayout) scrollView.getChildAt(0);

        TextView back = text("‹  Check In", 16, sageInk, Typeface.BOLD);
        back.setPadding(0, 0, 0, dp(6));
        back.setClickable(true);
        back.setFocusable(true);
        back.setContentDescription("Back to Check In");
        back.setOnClickListener(v -> showMainScreen());
        page.addView(back);
        page.addView(text("Your check-ins", 32, ink, Typeface.BOLD), margins(0, 14, 0, 0));
        TextView subtitle = text("Private history, stored only on this phone.", 16, mutedInk, Typeface.NORMAL);
        subtitle.setLineSpacing(dp(3), 1f);
        page.addView(subtitle, margins(0, 8, 0, 18));

        page.addView(createStatisticsCard(), margins(0, 0, 0, 14));

        List<MoodHistoryStore.Entry> entries = MoodHistoryStore.loadAll(this);
        if (entries.isEmpty()) {
            TextView empty = text("No check-ins yet. Your next moment of reflection will appear here.", 16, mutedInk, Typeface.NORMAL);
            empty.setLineSpacing(dp(4), 1f);
            page.addView(empty, margins(0, 12, 0, 0));
        } else {
            TextView clear = text("Clear history", 15, lavenderInk, Typeface.BOLD);
            clear.setGravity(Gravity.CENTER);
            clear.setPadding(dp(16), dp(12), dp(16), dp(12));
            clear.setClickable(true);
            clear.setFocusable(true);
            clear.setBackground(roundRect(lavender, 16, lavender, 0));
            clear.setOnClickListener(v -> confirmClearHistory());
            page.addView(clear, margins(0, 0, 0, 18));
            for (int i = 0; i < entries.size(); i++) {
                page.addView(createHistoryRow(entries.get(i)), margins(0, i == 0 ? 0 : 9, 0, 0));
            }
        }
        setContentView(scrollView);
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear all check-ins?")
                .setMessage("This removes the mood and date/time of every saved check-in from this phone. This can’t be undone.")
                .setNegativeButton("Keep history", null)
                .setPositiveButton("Clear history", (dialog, which) -> {
                    MoodHistoryStore.clear(this);
                    showHistoryScreen();
                })
                .show();
    }

    private View createHistoryRow(MoodHistoryStore.Entry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int background = entry.okay ? Color.rgb(246, 249, 245) : Color.rgb(249, 247, 252);
        int foreground = entry.okay ? sageInk : lavenderInk;
        row.setPadding(dp(16), dp(13), dp(16), dp(13));
        row.setBackground(roundRect(background, 18, background, 0));
        TextView dot = text("●", 15, foreground, Typeface.NORMAL);
        row.addView(dot, new LinearLayout.LayoutParams(dp(22), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout words = vertical();
        words.addView(text(entry.okay ? "I’m okay" : "Not really", 16, foreground, Typeface.BOLD));
        words.addView(text(formatCheckInTime(entry.checkedAt), 14, mutedInk, Typeface.NORMAL), margins(0, 3, 0, 0));
        row.addView(words, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private String formatCheckInTime(long timestamp) {
        Date moment = new Date(timestamp);
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(moment);
        String time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(moment);
        return date + " · " + time;
    }

    private View createRhythmCard() {
        LinearLayout cardView = vertical();
        cardView.setPadding(dp(20), dp(18), dp(16), dp(18));
        cardView.setBackground(roundRect(card, 24, line, 1));
        cardView.addView(text("Your check-in rhythm", 18, ink, Typeface.BOLD));
        cardView.addView(text("Pick what feels helpful, not intrusive.", 14, mutedInk, Typeface.NORMAL), margins(0, 4, 0, 14));
        intervalDetail = settingButton(UserPreferences.intervalLabel(UserPreferences.getInterval(this)));
        intervalDetail.setContentDescription("Change check-in frequency");
        intervalDetail.setOnClickListener(v -> showIntervalPicker(false));
        cardView.addView(intervalDetail, fullWidth());
        View divider = new View(this);
        divider.setBackgroundColor(line);
        cardView.addView(divider, fixedHeightMargins(1, 0, 18, 0, 16));

        LinearLayout reminderRow = new LinearLayout(this);
        reminderRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout words = vertical();
        words.addView(text("Gentle reminders", 17, ink, Typeface.BOLD));
        reminderDetail = text("", 14, mutedInk, Typeface.NORMAL);
        reminderDetail.setLineSpacing(dp(2), 1f);
        words.addView(reminderDetail, margins(0, 4, 0, 0));
        reminderRow.addView(words, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        reminderSwitch = new Switch(this);
        reminderSwitch.setContentDescription("Enable mood check-in reminders");
        reminderSwitch.setThumbTintList(new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { sageInk, Color.rgb(205, 206, 199) }));
        reminderSwitch.setTrackTintList(new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { Color.rgb(188, 215, 191), Color.rgb(230, 230, 225) }));
        settingSwitch = true;
        reminderSwitch.setChecked(ReminderScheduler.isEnabled(this));
        settingSwitch = false;
        reminderSwitch.setOnCheckedChangeListener(this::onReminderChanged);
        reminderRow.addView(reminderSwitch);
        cardView.addView(reminderRow, fullWidth());
        return cardView;
    }

    private View createNameRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(17), dp(16), dp(17));
        row.setBackground(roundRect(card, 24, line, 1));
        LinearLayout words = vertical();
        words.addView(text("Call me", 14, mutedInk, Typeface.NORMAL));
        words.addView(text(UserPreferences.getName(this), 17, ink, Typeface.BOLD), margins(0, 3, 0, 0));
        row.addView(words, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView edit = text("Edit", 15, sageInk, Typeface.BOLD);
        edit.setPadding(dp(12), dp(10), dp(4), dp(10));
        edit.setClickable(true);
        edit.setFocusable(true);
        edit.setContentDescription("Edit preferred name");
        edit.setOnClickListener(v -> showNameEditor());
        row.addView(edit);
        return row;
    }

    private View createPrivacyNote() {
        LinearLayout note = new LinearLayout(this);
        note.setGravity(Gravity.CENTER_VERTICAL);
        note.setPadding(dp(4), dp(6), dp(4), dp(6));
        note.addView(text("●", 15, sageInk, Typeface.NORMAL), new LinearLayout.LayoutParams(dp(24), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView privacy = text("Your name, rhythm, and history stay on this phone. No account or online data.", 14, mutedInk, Typeface.NORMAL);
        privacy.setLineSpacing(dp(3), 1f);
        note.addView(privacy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return note;
    }

    private void showIntervalPicker(boolean forWelcome) {
        long[] intervals = UserPreferences.intervals();
        String[] labels = UserPreferences.intervalLabels();
        long current = forWelcome ? welcomeInterval : UserPreferences.getInterval(this);
        new AlertDialog.Builder(this)
                .setTitle("How often should we check in?")
                .setSingleChoiceItems(labels, UserPreferences.intervalIndex(current), (dialog, which) -> {
                    if (forWelcome) {
                        welcomeInterval = intervals[which];
                        welcomeIntervalDetail.setText(labels[which] + "  ›");
                    } else {
                        UserPreferences.setInterval(this, intervals[which]);
                        ReminderScheduler.scheduleNext(this);
                        updateReminderCopy();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNameEditor() {
        EditText input = new EditText(this);
        input.setText(UserPreferences.getName(this));
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setTextColor(ink);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(32) });
        LinearLayout holder = vertical();
        holder.setPadding(dp(24), 0, dp(24), 0);
        holder.addView(input, fullWidth());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("What should we call you?")
                .setMessage("This name stays only on your phone.")
                .setView(holder)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = cleanedName(input.getText().toString());
            if (name.isEmpty()) {
                input.setError("Add a name or nickname");
                return;
            }
            UserPreferences.setName(this, name);
            dialog.dismiss();
            showMainScreen();
        }));
        dialog.show();
    }

    private void onReminderChanged(CompoundButton button, boolean enabled) {
        if (settingSwitch) return;
        if (enabled) {
            ReminderScheduler.enable(this);
            requestNotificationPermissionIfNeeded();
        } else {
            ReminderScheduler.disable(this);
        }
        updateReminderCopy();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, REQUEST_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) updateReminderCopy();
    }

    private void updateReminderCopy() {
        if (intervalDetail != null) {
            intervalDetail.setText(UserPreferences.intervalLabel(UserPreferences.getInterval(this)) + "  ›");
        }
        if (reminderDetail == null) return;
        if (!ReminderScheduler.isEnabled(this)) {
            reminderDetail.setText("Off — turn on when it feels useful");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            reminderDetail.setText("Allow notifications to begin check-ins");
        } else {
            reminderDetail.setText("On · " + UserPreferences.intervalLabel(UserPreferences.getInterval(this)).toLowerCase(Locale.getDefault()));
        }
    }

    private void respond(boolean okay) {
        long lastTime = MoodHistoryStore.getLastCheckInTime(this);
        long interval = UserPreferences.getInterval(this);
        long cooldown = Math.max(5 * 60 * 1000L, interval);
        long elapsed = System.currentTimeMillis() - lastTime;
        if (lastTime > 0 && elapsed < cooldown) {
            updateMoodCardState();
            return;
        }

        MoodHistoryStore.record(this, okay);
        refreshHistoryPreview();
        updateMoodCardState();
        CheckInWidgetProvider.updateWidgets(this);
        if (reassurance != null) {
            reassurance.setAlpha(0f);
            reassurance.animate().alpha(1f).setDuration(180).start();
        }
    }

    @Override
    public void onBackPressed() {
        if (showingHistory) {
            showMainScreen();
            return;
        }
        super.onBackPressed();
    }

    private TextView settingButton(String value) {
        TextView button = text(value + "  ›", 16, sageInk, Typeface.BOLD);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        button.setMinHeight(dp(52));
        button.setBackground(roundRect(Color.rgb(246, 249, 245), 16, Color.rgb(226, 236, 226), 1));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView actionButton(String title, String subtitle, int background, int foreground) {
        TextView button = new TextView(this);
        button.setText(title + "\n" + subtitle);
        button.setTextColor(foreground);
        button.setTextSize(17);
        button.setTypeface(Typeface.create("sans", Typeface.BOLD));
        button.setLineSpacing(dp(4), 1f);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(18), dp(15), dp(18), dp(15));
        button.setMinHeight(dp(76));
        button.setBackground(roundRect(background, 18, background, 0));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", style));
        return view;
    }

    private TextView label(String value, float size, int color) {
        return text(value, size, color, Typeface.BOLD);
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private GradientDrawable roundRect(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams fixedHeightMargins(int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private String cleanedName(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
