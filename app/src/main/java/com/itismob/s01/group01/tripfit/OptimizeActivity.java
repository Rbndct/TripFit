package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.RoutePreview;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;
import com.itismob.s01.group01.tripfit.viewmodel.OptimizeWriteViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Previews a nearest-neighbour reordering of each day against the current order (travel minutes
 * and the timed new order, conflicts marked in text) and applies it in one transaction. A second
 * action balances flexible stops across the trip days. Booked stops never move, and every
 * estimate is straight-line, not live routing.
 */
public class OptimizeActivity extends ScreenActivity {

    private final RowAdapter adapter = new RowAdapter();
    private final List<List<String>> pendingOrders = new ArrayList<>();
    private List<ItineraryItemWithPlace> latestItems = new ArrayList<>();
    private DraftTrip latestTrip;
    private TripRepository repository;
    private List<ItineraryItemWithPlace> observedItems = new ArrayList<>();
    private OptimizeWriteViewModel writes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setScreen(R.layout.activity_optimize, R.string.optimize_title)) {
            return;
        }
        repository = ((TripFitApp) getApplication()).getTripRepository();
        writes = new ViewModelProvider(this).get(OptimizeWriteViewModel.class);
        writes.getOutcome().observe(this, event -> {
            OptimizeWriteViewModel.Outcome outcome = event == null ? null : event.consume();
            if (outcome != null) {
                onWriteOutcome(outcome);
            }
        });

        ((RecyclerView) findViewById(R.id.list_days)).setAdapter(adapter);
        findViewById(R.id.button_keep).setOnClickListener(v -> finish());
        findViewById(R.id.button_apply).setOnClickListener(v -> applyOrder());
        findViewById(R.id.button_balance).setOnClickListener(v -> confirmBalance());

        repository.observeItineraryItemsWithPlaceNames(tripId).observe(this, items -> {
            observedItems = items;
            if (writes.isBusy() && latestTrip != null) {
                return; // our own write is landing; the screen closes right after
            }
            latestItems = items;
            showResult();
        });
    }

    private void showResult() {
        PrototypeStore store = PrototypeStore.get();
        latestTrip = TripSnapshots.from(tripId, store.meta(tripId), latestItems, store);
        pendingOrders.clear();
        List<UiRow> rows = new ArrayList<>();
        int savedMin = 0;
        for (int day = 1; day <= latestTrip.days; day++) {
            RoutePreview.Comparison cmp = RoutePreview.compare(latestTrip, day);
            if (cmp.current.size() < 2) {
                continue;
            }
            boolean improves = cmp.orderChanged && cmp.previewTravel < cmp.currentTravel;
            if (improves) {
                savedMin += cmp.currentTravel - cmp.previewTravel;
                pendingOrders.add(idsOf(cmp.preview));
            }
            rows.add(describeDay(day, cmp, improves));
        }
        findViewById(R.id.loading).setVisibility(View.GONE);
        findViewById(R.id.content).setVisibility(View.VISIBLE);
        bindSummary(rows.isEmpty(), savedMin);
        adapter.submit(rows);
    }

    private UiRow describeDay(int day, RoutePreview.Comparison cmp, boolean improves) {
        List<RoutePreview.Slot> shown = improves ? cmp.preview : cmp.current;
        StringBuilder detail = new StringBuilder(improves
                ? getString(R.string.optimize_day_compare, cmp.currentTravel, cmp.previewTravel)
                : getString(R.string.optimize_day_same, cmp.currentTravel));
        for (RoutePreview.Slot slot : shown) {
            detail.append('\n').append(getString(
                    slot.conflict ? R.string.optimize_slot_conflict : R.string.optimize_slot,
                    ScheduleMath.format(slot.start), ScheduleMath.format(slot.end),
                    slot.stop.name));
        }
        return UiRow.withTrailing(getString(R.string.day_label, day), detail.toString(),
                getString(R.string.optimize_day_trailing, cmp.currentTravel,
                        improves ? cmp.previewTravel : cmp.currentTravel));
    }

    private static List<String> idsOf(List<RoutePreview.Slot> slots) {
        List<String> ids = new ArrayList<>();
        for (RoutePreview.Slot slot : slots) {
            ids.add(slot.stop.id);
        }
        return ids;
    }

    private void bindSummary(boolean nothingToOptimize, int savedMin) {
        TextView summary = findViewById(R.id.text_summary);
        ViewCompat.setAccessibilityHeading(summary, true);
        if (nothingToOptimize) {
            summary.setText(R.string.optimize_empty);
        } else if (pendingOrders.isEmpty()) {
            summary.setText(R.string.optimize_summary_none);
        } else {
            summary.setText(getString(R.string.optimize_summary, savedMin));
        }
        findViewById(R.id.text_locked_note).setVisibility(View.VISIBLE);
        findViewById(R.id.actions).setVisibility(View.VISIBLE);
        updateButtons();
    }

    private void updateButtons() {
        boolean hasFlexible = latestTrip != null && !RoutePreview.balance(latestTrip).isEmpty();
        findViewById(R.id.button_apply).setEnabled(!writes.isBusy() && !pendingOrders.isEmpty());
        findViewById(R.id.button_balance).setEnabled(!writes.isBusy() && hasFlexible);
        findViewById(R.id.button_keep).setEnabled(!writes.isBusy());
    }

    private void applyOrder() {
        if (pendingOrders.isEmpty() || !writes.begin()) {
            return;
        }
        updateButtons();
        repository.applyItemOrders(new ArrayList<>(pendingOrders),
                result -> writes.finish(result.isSuccess(), R.string.optimize_applied));
    }

    private void confirmBalance() {
        if (writes.isBusy()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.balance_title)
                .setMessage(R.string.balance_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.balance_confirm, (dialog, which) -> balanceDays())
                .show();
    }

    private void balanceDays() {
        if (latestTrip == null || !writes.begin()) {
            return;
        }
        Map<String, Integer> balance = RoutePreview.balance(latestTrip);
        updateButtons();
        repository.applyMoves(TripSnapshots.moves(balance, latestItems),
                result -> writes.finish(result.isSuccess(), R.string.balance_applied));
    }

    private void onWriteOutcome(OptimizeWriteViewModel.Outcome outcome) {
        if (!outcome.success) {
            latestItems = observedItems; // the observer skipped updates while the write ran
            showResult();
            toast(R.string.error_update_failed);
            return;
        }
        // A platform Toast outlives this activity, so the result is seen after finish().
        Toast.makeText(getApplicationContext(), outcome.successMessage, Toast.LENGTH_SHORT).show();
        finish();
    }
}
