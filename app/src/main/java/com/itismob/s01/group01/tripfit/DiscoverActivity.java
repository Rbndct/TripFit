package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.prototype.MockData;
import com.itismob.s01.group01.tripfit.prototype.MockPlace;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Discover Mode: a ready-made itinerary split into famous and hidden-gem spots. Spots can be
 * selected, swapped for an alternative, and added to the trip (spread across its days).
 */
public class DiscoverActivity extends ScreenActivity {

    /** When true, adding spots opens the trip's itinerary afterwards (the New Trip flow). */
    public static final String EXTRA_OPEN_ITINERARY = "extra_open_itinerary";

    private static final int SHOWN_PER_SECTION = 3;
    private static final List<String> SUPPORTED_CITIES = Arrays.asList("Tokyo", "Manila");

    private static final String STATE_CITY = "state_city";
    private static final String STATE_SELECTED = "state_selected";
    private static final String STATE_FAMOUS = "state_famous";
    private static final String STATE_HIDDEN = "state_hidden";

    private final Set<String> selected = new LinkedHashSet<>();
    private TripRepository repository;
    private String city;
    private Section famous;
    private Section hidden;
    private boolean isSaving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setScreen(R.layout.activity_discover, R.string.discover_title)) {
            return;
        }
        repository = ((TripFitApp) getApplication()).getTripRepository();

        String metaCity = PrototypeStore.get().meta(tripId).getCity();
        String savedCity = savedInstanceState == null ? null : savedInstanceState.getString(STATE_CITY);
        city = SUPPORTED_CITIES.contains(savedCity) ? savedCity
                : SUPPORTED_CITIES.contains(metaCity) ? metaCity : SUPPORTED_CITIES.get(0);

        famous = new Section(R.id.list_famous);
        hidden = new Section(R.id.list_hidden);
        findViewById(R.id.button_change_city).setOnClickListener(v -> pickCity());
        findViewById(R.id.button_add_selected).setOnClickListener(v -> addSelected());
        for (int id : new int[]{R.id.text_city, R.id.heading_famous, R.id.heading_hidden}) {
            ViewCompat.setAccessibilityHeading(findViewById(id), true);
        }
        loadCity();
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CITY, city);
        outState.putStringArrayList(STATE_SELECTED, new ArrayList<>(selected));
        outState.putStringArrayList(STATE_FAMOUS, famous.shownNames());
        outState.putStringArrayList(STATE_HIDDEN, hidden.shownNames());
    }

    private void restoreState(Bundle state) {
        List<String> names = state.getStringArrayList(STATE_SELECTED);
        if (names != null) {
            selected.addAll(names);
        }
        famous.restore(state.getStringArrayList(STATE_FAMOUS));
        hidden.restore(state.getStringArrayList(STATE_HIDDEN));
        renderAll();
    }

    private void loadCity() {
        selected.clear();
        famous.reset(MockData.famous(city));
        hidden.reset(MockData.hiddenGems(city));
        ((TextView) findViewById(R.id.text_city)).setText(getString(R.string.discover_city, city));
        renderAll();
    }

    private void renderAll() {
        famous.render();
        hidden.render();
        MaterialButton add = findViewById(R.id.button_add_selected);
        add.setEnabled(!isSaving && !selected.isEmpty());
        add.setText(isSaving ? getString(R.string.discover_adding)
                : selected.isEmpty() ? getString(R.string.discover_none_selected)
                : getString(R.string.action_add_selected, selected.size()));
    }

    private void pickCity() {
        if (isSaving) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.city_dialog_title)
                .setSingleChoiceItems(SUPPORTED_CITIES.toArray(new String[0]),
                        SUPPORTED_CITIES.indexOf(city), (dialog, which) -> {
                            city = SUPPORTED_CITIES.get(which);
                            dialog.dismiss();
                            loadCity();
                        })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void addSelected() {
        if (isSaving || selected.isEmpty()) {
            return;
        }
        int days = Math.max(1, PrototypeStore.get().meta(tripId).getDays());
        List<String> names = new ArrayList<>(selected);
        int[] pending = {names.size()};
        boolean[] failed = {false};
        isSaving = true;
        renderAll();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            repository.addItineraryItem(tripId, name, 1 + (i % days), null, null, false,
                    result -> onSaveResult(result.isSuccess(), name, names.size(), pending, failed));
        }
    }

    /** Spots that saved leave the selection, so a retry after a failure only re-sends the rest. */
    private void onSaveResult(boolean isOk, String name, int total, int[] pending, boolean[] failed) {
        pending[0]--;
        if (isOk) {
            selected.remove(name);
        } else if (!failed[0]) {
            failed[0] = true;
            toast(R.string.error_save_failed);
        }
        if (pending[0] > 0) {
            return;
        }
        if (failed[0]) {
            isSaving = false;
            renderAll();
        } else {
            finishAdding(total);
        }
    }

    private void finishAdding(int total) {
        toast(getResources().getQuantityString(R.plurals.discover_added, total, total));
        if (getIntent().getBooleanExtra(EXTRA_OPEN_ITINERARY, false)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        }
        finish();
    }

    /** One list of suggestions plus the spare alternatives it can be swapped with. */
    private final class Section {
        private final RowAdapter adapter = new RowAdapter();
        private final List<MockPlace> shown = new ArrayList<>();
        private final List<MockPlace> pool = new ArrayList<>();

        Section(int recyclerId) {
            ((RecyclerView) findViewById(recyclerId)).setAdapter(adapter);
        }

        void reset(List<MockPlace> all) {
            shown.clear();
            pool.clear();
            for (int i = 0; i < all.size(); i++) {
                (i < SHOWN_PER_SECTION ? shown : pool).add(all.get(i));
            }
        }

        ArrayList<String> shownNames() {
            ArrayList<String> names = new ArrayList<>();
            for (MockPlace place : shown) {
                names.add(place.getName());
            }
            return names;
        }

        /** Puts the named spots (in order) back into the visible slots; the rest return to the pool. */
        void restore(@Nullable List<String> names) {
            if (names == null) {
                return;
            }
            List<MockPlace> all = new ArrayList<>(shown);
            all.addAll(pool);
            shown.clear();
            for (String name : names) {
                for (Iterator<MockPlace> it = all.iterator(); it.hasNext(); ) {
                    MockPlace place = it.next();
                    if (place.getName().equals(name)) {
                        shown.add(place);
                        it.remove();
                        break;
                    }
                }
            }
            pool.clear();
            pool.addAll(all);
        }

        void render() {
            List<UiRow> rows = new ArrayList<>();
            for (int i = 0; i < shown.size(); i++) {
                MockPlace place = shown.get(i);
                int slot = i;
                boolean isSelected = selected.contains(place.getName());
                rows.add(new UiRow(place.getName(),
                        getString(R.string.discover_row_sub, place.getKind(), place.getRating(),
                                place.getReviews()),
                        null,
                        getString(isSelected ? R.string.action_added : R.string.action_add),
                        () -> toggle(place),
                        () -> offerSwap(slot))
                        .withSelected(isSelected)
                        .withAccessibility(getString(isSelected ? R.string.discover_remove_cd
                                : R.string.discover_add_cd, place.getName()),
                                getString(R.string.discover_swap_cd)));
            }
            adapter.submit(rows);
        }

        private void toggle(MockPlace place) {
            if (isSaving) {
                return;
            }
            if (!selected.remove(place.getName())) {
                selected.add(place.getName());
            }
            renderAll();
        }

        private void offerSwap(int slot) {
            if (isSaving || pool.isEmpty()) {
                return;
            }
            MockPlace current = shown.get(slot);
            String[] names = new String[pool.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = pool.get(i).getName();
            }
            new MaterialAlertDialogBuilder(DiscoverActivity.this)
                    .setTitle(getString(R.string.swap_title, current.getName()))
                    .setItems(names, (dialog, which) -> {
                        MockPlace replacement = pool.remove(which);
                        pool.add(current);
                        shown.set(slot, replacement);
                        selected.remove(current.getName());
                        renderAll();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }
    }
}
