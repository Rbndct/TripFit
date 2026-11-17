package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import com.itismob.s01.group01.tripfit.data.AppExecutors;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.Expense;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.RoutePreview;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.ui.TripPdfWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Printable trip summary: per-day schedule plus budget breakdown. Export PDF writes a real
 * PDF and shares it; Share summary sends the same content as plain text.
 */
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
    private final List<String> lines = new ArrayList<>();
    private String tripName = "";
    private boolean isTripLoaded;
    private boolean isItemsLoaded;
    private boolean isExporting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setScreen(R.layout.activity_summary, R.string.summary_title)) {
            return;
        }
        ViewCompat.setAccessibilityHeading(findViewById(R.id.text_trip_name), true);
        findViewById(R.id.button_export_pdf).setOnClickListener(v -> exportPdf());
        findViewById(R.id.button_share).setOnClickListener(v -> shareText());

        TripRepository repository = ((TripFitApp) getApplication()).getTripRepository();
        repository.observeTrip(tripId).observe(this, trip -> {
            if (trip != null) {
                tripName = trip.getName();
                isTripLoaded = true;
                ((TextView) findViewById(R.id.text_trip_name)).setText(tripName);
                updateActions();
            }
        });
        repository.observeItineraryItemsWithPlaceNames(tripId).observe(this, this::render);
    }

    private void render(List<ItineraryItemWithPlace> items) {
        lines.clear();
        ViewGroup container = findViewById(R.id.container_summary);
        container.removeAllViews();
        Map<Integer, List<RoutePreview.Slot>> byDay = TripSnapshots.schedule(
                tripId, store.meta(tripId), items, store);
        for (Map.Entry<Integer, List<RoutePreview.Slot>> day : byDay.entrySet()) {
            addDay(day.getKey(), day.getValue(), container);
        }
        addBudget(container);
        findViewById(R.id.text_empty).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        isItemsLoaded = true;
        updateActions();
    }

    private void addDay(int day, List<RoutePreview.Slot> slots, ViewGroup container) {
        String heading = getString(R.string.day_label, day);
        List<Line> sectionLines = new ArrayList<>();
        lines.add(heading);
        for (RoutePreview.Slot slot : slots) {
            String time = ScheduleMath.format(slot.start) + "–" + ScheduleMath.format(slot.end);
            sectionLines.add(new Line(slot.stop.name, time));
            lines.add("  " + time + "  " + slot.stop.name);
        }
        lines.add("");
        String count = getResources().getQuantityString(R.plurals.summary_stop_count,
                sectionLines.size(), sectionLines.size());
        container.addView(buildSection(container, heading, count, sectionLines, null, false));
    }

    private void addBudget(ViewGroup container) {
        BudgetSettings budget = store.budget(tripId);
        String home = budget.getHomeCurrency();
        double total = budget.tripTotal(store.meta(tripId).getDays());
        List<Expense> expenses = store.expenses(tripId);
        double spent = BudgetMath.totalSpent(expenses);
        boolean hasBudget = total > 0;
        String spentText = CurrencyRates.format(spent, home);
        String headline = hasBudget
                ? getString(R.string.summary_spent_of, spentText, CurrencyRates.format(total, home))
                : getString(R.string.summary_spent_only, spentText);
        String note = budgetNote(hasBudget, spent, total, home);

        List<Line> sectionLines = new ArrayList<>();
        sectionLines.add(new Line(getString(R.string.summary_spent_label), hasBudget
                ? getString(R.string.summary_spent_value, spentText, CurrencyRates.format(total, home))
                : spentText));
        lines.add(getString(R.string.summary_budget_title));
        lines.add("  " + headline);
        for (Map.Entry<Category, Double> entry
                : BudgetMath.spentByCategory(expenses).entrySet()) {
            String label = Labels.category(this, entry.getKey());
            String amount = CurrencyRates.format(entry.getValue(), home);
            sectionLines.add(new Line(label, amount));
            lines.add("  " + label + ": " + amount);
        }
        if (note != null) {
            lines.add("  " + note);
        }
        container.addView(buildSection(container, getString(R.string.summary_budget_title), null,
                sectionLines, note, hasBudget && BudgetMath.isOver(spent, total)));
    }

    /** Text status for the budget card (so over-budget never relies on color alone). */
    @Nullable
    private String budgetNote(boolean hasBudget, double spent, double total, String home) {
        if (!hasBudget) {
            return getString(R.string.budget_no_total);
        }
        return BudgetMath.isOver(spent, total)
                ? getString(R.string.budget_over, CurrencyRates.format(spent - total, home)) : null;
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

    /** Actions wait for data, and Export PDF shows a busy label (and ignores taps) while writing. */
    private void updateActions() {
        boolean isReady = isTripLoaded && isItemsLoaded;
        MaterialButton export = findViewById(R.id.button_export_pdf);
        export.setEnabled(isReady && !isExporting);
        export.setText(isExporting ? R.string.summary_exporting : R.string.action_export_pdf);
        findViewById(R.id.button_share).setEnabled(isReady);
    }

    private void shareText() {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.summary_share_subject, tripName))
                .putExtra(Intent.EXTRA_TEXT, tripName + "\n\n" + String.join("\n", lines));
        startActivity(Intent.createChooser(send, getString(R.string.action_share_summary)));
    }

    private void exportPdf() {
        if (isExporting || !isTripLoaded || !isItemsLoaded) {
            return;
        }
        isExporting = true;
        updateActions();
        List<String> snapshot = new ArrayList<>(lines);
        String title = getString(R.string.summary_share_subject, tripName);
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                File file = TripPdfWriter.write(getApplicationContext(), title, snapshot);
                Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                        getPackageName() + ".fileprovider", file);
                AppExecutors.getInstance().mainThread().execute(() -> finishExport(uri, title));
            } catch (IOException | RuntimeException e) {
                Log.e("SummaryActivity", "PDF export failed", e);
                AppExecutors.getInstance().mainThread().execute(() -> finishExport(null, title));
            }
        });
    }

    /** Back on the main thread: clears the busy state, then shares the PDF or reports failure. */
    private void finishExport(@Nullable Uri uri, String title) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        isExporting = false;
        updateActions();
        if (uri == null) {
            toast(R.string.pdf_failed);
        } else {
            sharePdf(uri, title);
        }
    }

    private void sharePdf(Uri uri, String title) {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, title)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, getString(R.string.action_export_pdf)));
    }
}
