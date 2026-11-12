package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.MockData;
import com.itismob.s01.group01.tripfit.prototype.RouteMath;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Previews a nearest-neighbour reordering of each day (booked stops stay put) with the distance
 * saved, and persists the new positions to Room when confirmed. Distances come from deterministic
 * demo coordinates until the OpenRouteService Matrix API is wired.
 */
public class OptimizeActivity extends ScreenActivity {

    private static final long CALCULATE_DELAY_MS = 900;
    private static final double SAVED_TIME_SPEED_KMH = 22.0;

    private final RowAdapter adapter = new RowAdapter();
    private final List<List<String>> pendingOrders = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TripRepository repository;
    private boolean computed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_optimize, R.string.optimize_title);
        repository = ((TripFitApp) getApplication()).getTripRepository();

        ((RecyclerView) findViewById(R.id.list_days)).setAdapter(adapter);
        findViewById(R.id.button_keep).setOnClickListener(v -> finish());
        findViewById(R.id.button_apply).setOnClickListener(v -> applyOrder());

        repository.observeItineraryItemsWithPlaceNames(tripId).observe(this, items -> {
            if (computed) {
                return;
            }
            computed = true;
            handler.postDelayed(() -> showResult(items), CALCULATE_DELAY_MS);
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void showResult(List<ItineraryItemWithPlace> items) {
        Map<Integer, List<ItineraryItemWithPlace>> byDay = new TreeMap<>();
        for (ItineraryItemWithPlace entry : items) {
            byDay.computeIfAbsent(entry.item.getDay(), d -> new ArrayList<>()).add(entry);
        }
        List<UiRow> rows = new ArrayList<>();
        double savedKm = 0;
        for (Map.Entry<Integer, List<ItineraryItemWithPlace>> day : byDay.entrySet()) {
            if (day.getValue().size() < 2) {
                continue;
            }
            savedKm += describeDay(day.getKey(), day.getValue(), rows);
        }
        findViewById(R.id.loading).setVisibility(View.GONE);
        findViewById(R.id.content).setVisibility(View.VISIBLE);
        bindSummary(rows.isEmpty(), savedKm);
        adapter.submit(rows);
    }

    /** Adds one summary row for the day and returns the kilometres its reordering saves. */
    private double describeDay(int day, List<ItineraryItemWithPlace> stops, List<UiRow> rows) {
        int n = stops.size();
        double[][] points = new double[n][];
        boolean[] locked = new boolean[n];
        for (int i = 0; i < n; i++) {
            points[i] = MockData.coordFor(stops.get(i).item.displayName(stops.get(i).placeName));
            locked[i] = stops.get(i).item.isBooked();
        }
        int[] order = RouteMath.optimize(points, locked);
        double before = RouteMath.totalKm(points, RouteMath.identity(n));
        double after = RouteMath.totalKm(points, order);

        StringBuilder names = new StringBuilder();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ItineraryItemWithPlace stop = stops.get(order[i]);
            names.append(i == 0 ? "" : " → ")
                    .append(stop.item.displayName(stop.placeName));
            ids.add(stop.item.getId());
        }
        pendingOrders.add(ids);
        rows.add(UiRow.withTrailing(getString(R.string.day_label, day),
                getString(R.string.optimize_order, names),
                getString(R.string.optimize_day_detail, before, after)));
        return Math.max(0, before - after);
    }

    private void bindSummary(boolean nothingToOptimize, double savedKm) {
        TextView summary = findViewById(R.id.text_summary);
        int savedMin = (int) Math.round(savedKm / SAVED_TIME_SPEED_KMH * 60);
        if (nothingToOptimize) {
            summary.setText(R.string.optimize_empty);
        } else if (savedMin <= 0) {
            summary.setText(R.string.optimize_summary_none);
        } else {
            summary.setText(getString(R.string.optimize_summary, savedMin));
        }
        findViewById(R.id.actions).setVisibility(View.VISIBLE);
        findViewById(R.id.button_apply).setEnabled(!nothingToOptimize);
    }

    private void applyOrder() {
        int[] remaining = {pendingOrders.size()};
        for (List<String> ids : pendingOrders) {
            repository.applyItemOrder(ids, result -> {
                if (!result.isSuccess()) {
                    toast(R.string.error_update_failed);
                    return;
                }
                remaining[0]--;
                if (remaining[0] == 0) {
                    finish();
                }
            });
        }
    }
}
