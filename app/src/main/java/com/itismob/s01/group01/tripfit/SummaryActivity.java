package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s01.group01.tripfit.data.AppExecutors;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.CurrencyMock;
import com.itismob.s01.group01.tripfit.prototype.Expense;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.TripPdfWriter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Printable trip summary: per-day schedule plus budget breakdown. Export PDF writes a real
 * PDF and shares it; Share summary sends the same content as plain text.
 */
public class SummaryActivity extends ScreenActivity {

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private final List<String> lines = new ArrayList<>();
    private String tripName = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_summary, R.string.summary_title);
        ((RecyclerView) findViewById(R.id.list_summary)).setAdapter(adapter);
        findViewById(R.id.button_export_pdf).setOnClickListener(v -> exportPdf());
        findViewById(R.id.button_share).setOnClickListener(v -> shareText());

        TripRepository repository = ((TripFitApp) getApplication()).getTripRepository();
        repository.observeTrip(tripId).observe(this, trip -> {
            if (trip != null) {
                tripName = trip.getName();
                ((TextView) findViewById(R.id.text_trip_name)).setText(tripName);
            }
        });
        repository.observeItineraryItemsWithPlaceNames(tripId).observe(this, this::render);
    }

    private void render(List<ItineraryItemWithPlace> items) {
        lines.clear();
        List<UiRow> rows = new ArrayList<>();
        Map<Integer, List<ItineraryItemWithPlace>> byDay = new TreeMap<>();
        for (ItineraryItemWithPlace entry : items) {
            byDay.computeIfAbsent(entry.item.getDay(), d -> new ArrayList<>()).add(entry);
        }
        for (Map.Entry<Integer, List<ItineraryItemWithPlace>> day : byDay.entrySet()) {
            addDay(day.getKey(), day.getValue(), rows);
        }
        addBudget(rows);
        adapter.submit(rows);
        findViewById(R.id.text_empty).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addDay(int day, List<ItineraryItemWithPlace> entries, List<UiRow> rows) {
        List<ScheduleMath.Stop> stops = new ArrayList<>();
        for (ItineraryItemWithPlace entry : entries) {
            stops.add(new ScheduleMath.Stop(entry.item.displayName(entry.placeName),
                    entry.item.isBooked() ? entry.item.getTime() : null,
                    entry.item.getDurationMin()));
        }
        String heading = getString(R.string.day_label, day);
        StringBuilder body = new StringBuilder();
        lines.add(heading);
        for (ScheduleMath.Slot slot : ScheduleMath.build(stops, store.getBufferMin())) {
            String line = ScheduleMath.format(slot.getStartMin()) + "–"
                    + ScheduleMath.format(slot.getEndMin()) + "  " + slot.getName();
            body.append(body.length() == 0 ? "" : "\n").append(line);
            lines.add("  " + line);
        }
        lines.add("");
        rows.add(UiRow.plain(heading, body.toString()));
    }

    private void addBudget(List<UiRow> rows) {
        BudgetSettings budget = store.budget(tripId);
        String home = budget.getHomeCurrency();
        double total = budget.tripTotal(store.meta(tripId).getDays());
        List<Expense> expenses = store.expenses(tripId);
        double spent = BudgetMath.totalSpent(expenses);
        String headline = getString(R.string.summary_spent_of,
                CurrencyMock.format(spent, home), CurrencyMock.format(total, home));

        StringBuilder body = new StringBuilder(headline);
        lines.add(getString(R.string.summary_budget_title));
        lines.add("  " + headline);
        for (Map.Entry<Category, Double> entry
                : BudgetMath.spentByCategory(expenses).entrySet()) {
            String line = Labels.category(this, entry.getKey()) + ": "
                    + CurrencyMock.format(entry.getValue(), home);
            body.append('\n').append(line);
            lines.add("  " + line);
        }
        rows.add(UiRow.plain(getString(R.string.summary_budget_title), body.toString()));
    }

    private void shareText() {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.summary_share_subject, tripName))
                .putExtra(Intent.EXTRA_TEXT, tripName + "\n\n" + String.join("\n", lines));
        startActivity(Intent.createChooser(send, getString(R.string.action_share_summary)));
    }

    private void exportPdf() {
        List<String> snapshot = new ArrayList<>(lines);
        String title = getString(R.string.summary_share_subject, tripName);
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                File file = TripPdfWriter.write(getApplicationContext(), title, snapshot);
                Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                        getPackageName() + ".fileprovider", file);
                AppExecutors.getInstance().mainThread().execute(() -> sharePdf(uri, title));
            } catch (IOException | RuntimeException e) {
                Log.e("SummaryActivity", "PDF export failed", e);
                AppExecutors.getInstance().mainThread().execute(() -> toast(R.string.pdf_failed));
            }
        });
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
