package com.itismob.s01.group01.tripfit;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.RoutePreview;
import com.itismob.s01.group01.tripfit.prototype.TripDates;
import com.itismob.s01.group01.tripfit.prototype.TripExport;
import com.itismob.s01.group01.tripfit.prototype.TripFormRules;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;
import com.itismob.s01.group01.tripfit.viewmodel.DocumentExportViewModel;

import java.util.ArrayList;
import java.util.List;

/** Reviews a trip's local calendar events and saves an importable iCalendar file. */
public class CalendarSyncActivity extends ScreenActivity {
    private static final String STATE_PREVIEW = "preview_open";

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private DraftTrip trip;
    private int eventCount;
    private int warningCount;
    private boolean restorePreview;
    private AlertDialog preview;
    private DocumentExportViewModel export;
    private ActivityResultLauncher<String> createDocument;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!setScreen(R.layout.activity_calendar_sync, R.string.calendar_title)) {
            return;
        }
        export = new ViewModelProvider(this).get(DocumentExportViewModel.class);
        createDocument = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/calendar"),
                uri -> {
                    export.onDocumentPicked(uri);
                    export.writePickedDocument(trip, DocumentExportViewModel.Format.CALENDAR);
                });
        restorePreview = state != null && state.getBoolean(STATE_PREVIEW);
        ((RecyclerView) findViewById(R.id.list_events)).setAdapter(adapter);
        ViewCompat.setAccessibilityHeading(findViewById(R.id.text_events_header), true);
        findViewById(R.id.button_sync).setOnClickListener(v -> reviewPreview());
        findViewById(R.id.button_save_calendar).setOnClickListener(v -> saveCalendar());
        findViewById(R.id.button_open_calendar).setOnClickListener(v -> openCalendar());
        export.busy().observe(this, busy -> renderActions());
        export.outcome().observe(this, outcome -> {
            if (outcome != null) {
                export.consumeOutcome();
                toast(outcome.message);
            }
        });
        renderActions();
        ((TripFitApp) getApplication()).getTripRepository()
                .observeItineraryItemsWithPlaceNames(tripId).observe(this, this::showEvents);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean(STATE_PREVIEW, restorePreview || preview != null && preview.isShowing());
    }

    @Override
    protected void onDestroy() {
        if (preview != null) {
            preview.dismiss();
        }
        super.onDestroy();
    }

    private void showEvents(List<ItineraryItemWithPlace> items) {
        trip = TripSnapshots.forExport(tripId, "", items, store);
        List<UiRow> rows = new ArrayList<>();
        eventCount = 0;
        warningCount = 0;
        for (int day = 1; day <= trip.days; day++) {
            List<RoutePreview.Slot> slots = RoutePreview.day(trip, day, false);
            if (slots.isEmpty()) {
                continue;
            }
            String date = TripDates.dayDateLabel(trip.startDate, day);
            rows.add(UiRow.plain(date.isEmpty() ? getString(R.string.day_label, day)
                    : getString(R.string.day_heading_dated, day, date), null));
            for (RoutePreview.Slot slot : slots) {
                rows.add(eventRow(slot));
                eventCount++;
                if (slot.conflict) {
                    warningCount++;
                }
            }
        }
        adapter.submit(rows);
        text(R.id.text_events_header).setText(getResources().getQuantityString(
                R.plurals.calendar_events_header, eventCount, eventCount));
        text(R.id.text_schedule_warnings).setText(warnings());
        findViewById(R.id.text_empty).setVisibility(eventCount == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.text_schedule_warnings).setVisibility(eventCount > 0 ? View.VISIBLE : View.GONE);
        export.writePickedDocument(trip, DocumentExportViewModel.Format.CALENDAR);
        renderActions();
        if (restorePreview && eventCount > 0) {
            restorePreview = false;
            reviewPreview();
        }
    }

    private UiRow eventRow(RoutePreview.Slot slot) {
        String subtitle = TripExport.location(trip, slot.stop);
        if (slot.stop.booked) {
            subtitle += "\n" + getString(R.string.calendar_booked);
        }
        if (slot.conflict) {
            subtitle += "\n" + getString(R.string.calendar_event_conflict);
        }
        return UiRow.withTrailing(slot.stop.name, subtitle, getString(R.string.calendar_event_time,
                RoutePreview.time(slot.start), RoutePreview.time(slot.end)));
    }

    private void renderActions() {
        boolean hasEvents = eventCount > 0;
        findViewById(R.id.text_events_header).setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        boolean reviewed = hasEvents && store.isSynced(tripId);
        findViewById(R.id.text_sync_result).setVisibility(reviewed ? View.VISIBLE : View.GONE);
        findViewById(R.id.button_open_calendar).setVisibility(reviewed ? View.VISIBLE : View.GONE);
        findViewById(R.id.button_sync).setEnabled(hasEvents && !export.isBusy());
        TextView save = text(R.id.button_save_calendar);
        save.setEnabled(trip != null && !export.isBusy());
        save.setText(export.isBusy() ? R.string.export_saving : R.string.calendar_save_ics);
    }

    private String warnings() {
        return getResources().getQuantityString(R.plurals.export_schedule_warnings,
                warningCount, warningCount);
    }

    private void reviewPreview() {
        String count = getResources().getQuantityString(R.plurals.calendar_events_header,
                eventCount, eventCount);
        preview = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calendar_review_preview)
                .setMessage(getString(R.string.calendar_preview_message, count,
                        TripExport.calendarZoneId(trip.city), warnings()))
                .setPositiveButton(R.string.calendar_mark_reviewed, (dialog, which) -> {
                    store.markSynced(tripId);
                    renderActions();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void saveCalendar() {
        if (export.isBusy() || trip == null) {
            return;
        }
        if (eventCount == 0) {
            toast(R.string.calendar_empty);
            return;
        }
        if (!TripFormRules.isValidIsoDate(trip.startDate)) {
            toast(R.string.calendar_invalid_date);
            return;
        }
        export.beginPicker(trip);
        try {
            createDocument.launch(getString(R.string.calendar_filename));
        } catch (ActivityNotFoundException error) {
            export.onDocumentPicked(null);
            toast(R.string.export_save_failed);
        }
    }

    private void openCalendar() {
        Uri uri = Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis());
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException error) {
            toast(R.string.calendar_open_failed);
        }
    }

    private TextView text(int id) {
        return findViewById(id);
    }
}
