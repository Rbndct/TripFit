package com.itismob.s01.group01.tripfit;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.Expense;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.RoutePreview;
import com.itismob.s01.group01.tripfit.prototype.TripDates;
import com.itismob.s01.group01.tripfit.prototype.TripExport;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.viewmodel.DocumentExportViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shows the trip summary and shares or saves it using the common exporter. */
public class SummaryActivity extends ScreenActivity {

    /** One label/value pair inside a section card; the value is data and renders monospace. */
    private static final class Line {
        final String label;
        final String value;

        Line(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private final PrototypeStore store = PrototypeStore.get();
    private List<ItineraryItemWithPlace> items;
    private String tripName;
    private DraftTrip snapshot;
    private DocumentExportViewModel export;
    private ActivityResultLauncher<String> createDocument;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!setScreen(R.layout.activity_summary, R.string.summary_title)) {
            return;
        }
        export = new ViewModelProvider(this).get(DocumentExportViewModel.class);
        createDocument = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),
                uri -> {
                    export.onDocumentPicked(uri);
                    export.writePickedDocument(snapshot, DocumentExportViewModel.Format.PDF);
                });
        ViewCompat.setAccessibilityHeading(findViewById(R.id.text_trip_name), true);
        findViewById(R.id.button_export_pdf).setOnClickListener(v -> savePdf());
        findViewById(R.id.button_share_pdf).setOnClickListener(v -> export.sharePdf(snapshot));
        findViewById(R.id.button_share).setOnClickListener(v -> shareText());
        export.busy().observe(this, busy -> updateActions());
        export.outcome().observe(this, outcome -> {
            if (outcome == null) {
                return;
            }
            export.consumeOutcome();
            if (outcome.sharedPdf != null) {
                sharePdf(outcome.sharedPdf, outcome.tripName);
            } else {
                toast(outcome.message);
            }
        });

        TripRepository repository = ((TripFitApp) getApplication()).getTripRepository();
        repository.observeTrip(tripId).observe(this, trip -> {
            if (trip != null) {
                tripName = trip.getName();
                ((TextView) findViewById(R.id.text_trip_name)).setText(tripName);
                render();
            }
        });
        repository.observeItineraryItemsWithPlaceNames(tripId).observe(this, rows -> {
            items = rows;
            render();
        });
        updateActions();
    }

    private void render() {
        if (tripName == null || items == null) {
            return;
        }
        snapshot = TripSnapshots.forExport(tripId, tripName, items, store);
        ViewGroup container = findViewById(R.id.container_summary);
        container.removeAllViews();
        int warnings = 0;
        for (int day = 1; day <= snapshot.days; day++) {
            List<RoutePreview.Slot> slots = RoutePreview.day(snapshot, day, false);
            addDay(day, slots, container);
            for (RoutePreview.Slot slot : slots) {
                if (slot.conflict) {
                    warnings++;
                }
            }
        }
        addBudget(container);
        TextView warningText = findViewById(R.id.text_schedule_warnings);
        warningText.setVisibility(View.VISIBLE);
        warningText.setText(getResources().getQuantityString(R.plurals.export_schedule_warnings,
                warnings, warnings));
        export.writePickedDocument(snapshot, DocumentExportViewModel.Format.PDF);
        updateActions();
    }

    private void addDay(int day, List<RoutePreview.Slot> slots, ViewGroup container) {
        String date = TripDates.dayDateLabel(snapshot.startDate, day);
        String heading = date.isEmpty() ? getString(R.string.day_label, day)
                : getString(R.string.day_heading_dated, day, date);
        List<Line> lines = new ArrayList<>();
        for (RoutePreview.Slot slot : slots) {
            String time = getString(R.string.calendar_event_time,
                    RoutePreview.time(slot.start), RoutePreview.time(slot.end));
            String label = slot.stop.name;
            if (slot.stop.booked) {
                label += "\n" + getString(R.string.calendar_booked);
            }
            if (slot.conflict) {
                label += "\n" + getString(R.string.calendar_event_conflict);
            }
            lines.add(new Line(label, time));
        }
        String count = getResources().getQuantityString(R.plurals.summary_stop_count,
                slots.size(), slots.size());
        String note = slots.isEmpty() ? getString(R.string.summary_day_empty) : null;
        container.addView(buildSection(container, heading, count, lines, note, false));
    }

    private void addBudget(ViewGroup container) {
        String home = snapshot.homeCurrency;
        double total = snapshot.totalBudget();
        List<Expense> expenses = store.expenses(tripId);
        double spent = BudgetMath.totalSpent(expenses);
        double planned = BudgetMath.plannedTotal(snapshot);
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(getString(R.string.summary_limit), CurrencyRates.format(total, home)));
        lines.add(new Line(getString(R.string.budget_planned_label), CurrencyRates.format(planned, home)));
        lines.add(new Line(getString(R.string.summary_logged), CurrencyRates.format(spent, home)));
        if (total > 0) {
            lines.add(new Line(getString(R.string.summary_remaining),
                    CurrencyRates.format(total - spent, home)));
        }
        Map<Category, Double> loggedByCategory = BudgetMath.spentByCategory(expenses);
        for (Map.Entry<Category, Double> entry : loggedByCategory.entrySet()) {
            lines.add(new Line(getString(R.string.summary_category_logged, Labels.category(this,
                    entry.getKey())), CurrencyRates.format(entry.getValue(), home)));
        }
        boolean over = BudgetMath.isOver(spent, total);
        String note = total <= 0 ? getString(R.string.budget_no_total) : over
                ? getString(R.string.budget_over, CurrencyRates.format(spent - total, home)) : null;
        container.addView(buildSection(container, getString(R.string.summary_budget_title), null,
                lines, note, over));
    }

    private View buildSection(ViewGroup parent, String title, @Nullable String count,
                              List<Line> sectionLines, @Nullable String note, boolean isAlert) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View section = inflater.inflate(R.layout.item_summary_section, parent, false);
        TextView titleView = section.findViewById(R.id.text_section_title);
        titleView.setText(title);
        ViewCompat.setAccessibilityHeading(titleView, true);
        TextView countView = section.findViewById(R.id.text_section_count);
        countView.setText(count);
        countView.setVisibility(count == null ? View.GONE : View.VISIBLE);
        ViewGroup linesContainer = section.findViewById(R.id.container_section_lines);
        for (Line line : sectionLines) {
            View row = inflater.inflate(R.layout.item_summary_line, linesContainer, false);
            ((TextView) row.findViewById(R.id.text_line_label)).setText(line.label);
            ((TextView) row.findViewById(R.id.text_line_value)).setText(line.value);
            linesContainer.addView(row);
        }
        TextView noteView = section.findViewById(R.id.text_section_note);
        noteView.setVisibility(note == null ? View.GONE : View.VISIBLE);
        noteView.setText(note);
        if (isAlert) {
            noteView.setTextColor(MaterialColors.getColor(noteView, androidx.appcompat.R.attr.colorError));
        }
        return section;
    }

    private void updateActions() {
        boolean ready = snapshot != null && !export.isBusy();
        MaterialButton save = findViewById(R.id.button_export_pdf);
        save.setEnabled(ready);
        save.setText(export.isBusy() ? R.string.summary_exporting : R.string.action_export_pdf);
        findViewById(R.id.button_share_pdf).setEnabled(ready);
        findViewById(R.id.button_share).setEnabled(ready);
    }

    private void savePdf() {
        export.beginPicker(snapshot);
        try {
            createDocument.launch(getString(R.string.summary_filename));
        } catch (ActivityNotFoundException error) {
            export.onDocumentPicked(null);
            toast(R.string.export_save_failed);
        }
    }

    private void shareText() {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.summary_share_subject, snapshot.name))
                .putExtra(Intent.EXTRA_TEXT, TripExport.summary(snapshot));
        startActivity(Intent.createChooser(send, getString(R.string.action_share_summary)));
    }

    private void sharePdf(Uri uri, String name) {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.summary_share_subject, name))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newRawUri(getString(R.string.action_share_pdf), uri));
        startActivity(Intent.createChooser(send, getString(R.string.action_share_pdf)));
    }
}
