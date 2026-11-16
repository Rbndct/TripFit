package com.itismob.s01.group01.tripfit;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Previews the timed Google Calendar events built from the itinerary and simulates the one-tap
 * sync. Re-syncing reports an update rather than duplicates, per the backlog's acceptance
 * criteria. The real Calendar API replaces the simulated delay later.
 */
public class CalendarSyncActivity extends ScreenActivity {

    private static final long SYNC_DELAY_MS = 1500;
    private static final String KEY_SYNCING = "syncing";
    private static final String KEY_RESULT = "result";
    private static final int RESULT_NONE = 0;
    private static final int RESULT_ADDED = 1;
    private static final int RESULT_UPDATED = 2;

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MaterialButton syncButton;
    private int eventCount;
    private boolean eventsLoaded;
    private boolean isSyncing;
    private int resultKind = RESULT_NONE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_calendar_sync, R.string.calendar_title);
        ((RecyclerView) findViewById(R.id.list_events)).setAdapter(adapter);
        ViewCompat.setAccessibilityHeading(findViewById(R.id.text_events_header), true);
        syncButton = findViewById(R.id.button_sync);
        syncButton.setOnClickListener(v -> startSync());
        findViewById(R.id.button_open_calendar).setOnClickListener(v -> openCalendar());
        if (savedInstanceState != null) {
            resultKind = savedInstanceState.getInt(KEY_RESULT, RESULT_NONE);
            if (savedInstanceState.getBoolean(KEY_SYNCING)) {
                // The simulated sync was in flight when the screen was recreated; run it again.
                isSyncing = true;
                handler.postDelayed(this::finishSync, SYNC_DELAY_MS);
            }
        }
        render();

        ((TripFitApp) getApplication()).getTripRepository()
                .observeItineraryItemsWithPlaceNames(tripId).observe(this, this::showEvents);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SYNCING, isSyncing);
        outState.putInt(KEY_RESULT, resultKind);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void showEvents(List<ItineraryItemWithPlace> items) {
        Map<Integer, List<ScheduleMath.Stop>> byDay = new TreeMap<>();
        for (ItineraryItemWithPlace entry : items) {
            byDay.computeIfAbsent(entry.item.getDay(), d -> new ArrayList<>()).add(
                    new ScheduleMath.Stop(entry.item.displayName(entry.placeName),
                            entry.item.isBooked() ? entry.item.getTime() : null,
                            entry.item.getDurationMin()));
        }
        List<UiRow> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<ScheduleMath.Stop>> day : byDay.entrySet()) {
            for (ScheduleMath.Slot slot : ScheduleMath.build(day.getValue(),
                    store.getBufferMin())) {
                rows.add(UiRow.withTrailing(slot.getName(),
                        getString(R.string.calendar_event_day, day.getKey()),
                        getString(R.string.calendar_event_time,
                                ScheduleMath.format(slot.getStartMin()),
                                ScheduleMath.format(slot.getEndMin()))));
            }
        }
        eventCount = rows.size();
        eventsLoaded = true;
        adapter.submit(rows);
        render();
    }

    /** Draws every state-dependent view from the fields, so rotation and data reloads agree. */
    private void render() {
        boolean hasEvents = eventCount > 0;
        boolean isSynced = store.isSynced(tripId);
        TextView header = findViewById(R.id.text_events_header);
        header.setVisibility(eventsLoaded && hasEvents ? View.VISIBLE : View.GONE);
        header.setText(getResources().getQuantityString(isSynced
                ? R.plurals.calendar_events_synced : R.plurals.calendar_events_header,
                eventCount, eventCount));
        findViewById(R.id.text_empty).setVisibility(
                eventsLoaded && !hasEvents ? View.VISIBLE : View.GONE);
        findViewById(R.id.progress_sync).setVisibility(isSyncing ? View.VISIBLE : View.GONE);
        findViewById(R.id.button_open_calendar)
                .setVisibility(isSynced && !isSyncing ? View.VISIBLE : View.GONE);
        renderResult();
        syncButton.setEnabled(hasEvents && !isSyncing);
        syncButton.setText(syncButtonLabel(isSynced, hasEvents));
    }

    private void renderResult() {
        TextView result = findViewById(R.id.text_sync_result);
        // Wait for the events: before they load, eventCount is 0 and the line would read "0 events".
        boolean isShown = eventsLoaded && !isSyncing && resultKind != RESULT_NONE;
        result.setVisibility(isShown ? View.VISIBLE : View.GONE);
        if (isShown) {
            result.setText(resultKind == RESULT_UPDATED ? getString(R.string.calendar_resynced)
                    : getResources().getQuantityString(R.plurals.calendar_success,
                            eventCount, eventCount));
        }
    }

    private String syncButtonLabel(boolean isSynced, boolean hasEvents) {
        if (isSyncing) {
            return getString(R.string.calendar_syncing);
        }
        if (!hasEvents) {
            return getString(R.string.action_calendar);
        }
        return isSynced ? getString(R.string.action_resync)
                : getResources().getQuantityString(R.plurals.action_sync_calendar,
                        eventCount, eventCount);
    }

    private void startSync() {
        if (isSyncing) {
            return;
        }
        isSyncing = true;
        resultKind = RESULT_NONE;
        render();
        syncButton.announceForAccessibility(getString(R.string.calendar_syncing));
        handler.postDelayed(this::finishSync, SYNC_DELAY_MS);
    }

    private void finishSync() {
        resultKind = store.isSynced(tripId) ? RESULT_UPDATED : RESULT_ADDED;
        store.markSynced(tripId);
        isSyncing = false;
        render();
    }

    private void openCalendar() {
        Uri uri = Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis());
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            toast(R.string.calendar_open_failed);
        }
    }
}
