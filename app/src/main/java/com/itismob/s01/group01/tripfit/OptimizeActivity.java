package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
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
    private static final double MIN_SAVING_KM = 0.05;

    private final RowAdapter adapter = new RowAdapter();
    private final List<List<String>> pendingOrders = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TripRepository repository;
    private boolean computed;
    private boolean applying;

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

    /**
     * Adds one summary row for the day and returns the kilometres its reordering saves. A day
     * whose nearest-neighbour order is no shorter keeps its current order and is not queued.
     */
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
        boolean improves = after < before - MIN_SAVING_KM;
        if (!improves) {
            order = RouteMath.identity(n);
            after = before;
        }

        String names = joinNames(stops, order);
        if (improves) {
            List<String> ids = new ArrayList<>();
            for (int index : order) {
                ids.add(stops.get(index).item.getId());
            }
            pendingOrders.add(ids);
        }
        String detail = improves
                ? getString(R.string.optimize_order, names, before)
                : getString(R.string.optimize_order_same, names);
        rows.add(UiRow.withTrailing(getString(R.string.day_label, day), detail,
                getString(R.string.optimize_day_distance, after)));
        return before - after;
    }

    private String joinNames(List<ItineraryItemWithPlace> stops, int[] order) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < order.length; i++) {
            ItineraryItemWithPlace stop = stops.get(order[i]);
            names.append(i == 0 ? "" : " → ").append(stop.item.displayName(stop.placeName));
        }
        return names.toString();
    }

    private void bindSummary(boolean nothingToOptimize, double savedKm) {
        TextView summary = findViewById(R.id.text_summary);
        ViewCompat.setAccessibilityHeading(summary, true);
        int savedMin = (int) Math.round(savedKm / SAVED_TIME_SPEED_KMH * 60);
        if (nothingToOptimize) {
            summary.setText(R.string.optimize_empty);
        } else if (pendingOrders.isEmpty()) {
            summary.setText(R.string.optimize_summary_none);
        } else if (savedMin <= 0) {
            summary.setText(R.string.optimize_summary_small);
        } else {
            summary.setText(getString(R.string.optimize_summary, savedMin));
        }
        findViewById(R.id.text_locked_note)
                .setVisibility(nothingToOptimize ? View.GONE : View.VISIBLE);
        findViewById(R.id.actions).setVisibility(View.VISIBLE);
        findViewById(R.id.button_apply).setEnabled(!pendingOrders.isEmpty());
    }

    private void setApplying(boolean isApplying) {
        applying = isApplying;
        findViewById(R.id.button_apply).setEnabled(!isApplying && !pendingOrders.isEmpty());
        findViewById(R.id.button_keep).setEnabled(!isApplying);
    }

    private void applyOrder() {
        if (applying || pendingOrders.isEmpty()) {
            return;
        }
        setApplying(true);
        int[] remaining = {pendingOrders.size()};
        for (List<String> ids : pendingOrders) {
            repository.applyItemOrder(ids, result -> {
                if (!applying) {
                    return;
                }
                if (!result.isSuccess()) {
                    setApplying(false);
                    toast(R.string.error_update_failed);
                    return;
                }
                remaining[0]--;
                if (remaining[0] == 0) {
                    // A platform Toast outlives this activity, so the result is seen after finish().
                    Toast.makeText(getApplicationContext(), R.string.optimize_applied,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }
}
