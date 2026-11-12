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

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MaterialButton syncButton;
    private int eventCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_calendar_sync, R.string.calendar_title);
        ((RecyclerView) findViewById(R.id.list_events)).setAdapter(adapter);
        syncButton = findViewById(R.id.button_sync);
        syncButton.setOnClickListener(v -> startSync());
        findViewById(R.id.button_open_calendar).setOnClickListener(v -> openCalendar());
        bindButtonLabel();

        ((TripFitApp) getApplication()).getTripRepository()
                .observeItineraryItemsWithPlaceNames(tripId).observe(this, this::showEvents);
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
                rows.add(UiRow.plain(slot.getName(), getString(R.string.calendar_event_sub,
                        day.getKey(), ScheduleMath.format(slot.getStartMin()),
                        ScheduleMath.format(slot.getEndMin()))));
            }
        }
        eventCount = rows.size();
        adapter.submit(rows);
        ((TextView) findViewById(R.id.text_events_header))
                .setText(getString(R.string.calendar_events_header, eventCount));
        findViewById(R.id.text_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        syncButton.setEnabled(eventCount > 0);
        bindButtonLabel();
    }

    private void bindButtonLabel() {
        syncButton.setText(store.isSynced(tripId) ? getString(R.string.action_resync)
                : getString(R.string.action_sync_calendar, eventCount));
    }

    private void startSync() {
        boolean wasSynced = store.isSynced(tripId);
        syncButton.setEnabled(false);
        syncButton.setText(R.string.calendar_syncing);
        findViewById(R.id.progress_sync).setVisibility(View.VISIBLE);
        findViewById(R.id.text_sync_result).setVisibility(View.GONE);
        handler.postDelayed(() -> finishSync(wasSynced), SYNC_DELAY_MS);
    }

    private void finishSync(boolean wasSynced) {
        store.markSynced(tripId);
        findViewById(R.id.progress_sync).setVisibility(View.GONE);
        TextView result = findViewById(R.id.text_sync_result);
        result.setVisibility(View.VISIBLE);
        result.setText(wasSynced ? getString(R.string.calendar_resynced)
                : getString(R.string.calendar_success, eventCount));
        findViewById(R.id.button_open_calendar).setVisibility(View.VISIBLE);
        syncButton.setEnabled(true);
        bindButtonLabel();
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
