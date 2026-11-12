package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
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

    private final Set<String> selected = new LinkedHashSet<>();
    private TripRepository repository;
    private String city;
    private Section famous;
    private Section hidden;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_discover, R.string.discover_title);
        repository = ((TripFitApp) getApplication()).getTripRepository();

        String metaCity = PrototypeStore.get().meta(tripId).getCity();
        city = SUPPORTED_CITIES.contains(metaCity) ? metaCity : SUPPORTED_CITIES.get(0);

        famous = new Section(R.id.list_famous);
        hidden = new Section(R.id.list_hidden);
        findViewById(R.id.button_change_city).setOnClickListener(v -> pickCity());
        findViewById(R.id.button_add_selected).setOnClickListener(v -> addSelected());
        loadCity();
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
        add.setEnabled(!selected.isEmpty());
        add.setText(selected.isEmpty() ? getString(R.string.discover_none_selected)
                : getString(R.string.action_add_selected, selected.size()));
    }

    private void pickCity() {
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
        int days = Math.max(1, PrototypeStore.get().meta(tripId).getDays());
        int total = selected.size();
        int[] remaining = {total};
        int index = 0;
        for (String name : selected) {
            int day = 1 + (index++ % days);
            repository.addItineraryItem(tripId, name, day, null, null, false, result -> {
                if (!result.isSuccess()) {
                    toast(R.string.error_save_failed);
                    return;
                }
                remaining[0]--;
                if (remaining[0] == 0) {
                    finishAdding(total);
                }
            });
        }
    }

    private void finishAdding(int total) {
        toast(getString(R.string.discover_added, total));
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
                        () -> offerSwap(slot)));
            }
            adapter.submit(rows);
        }

        private void toggle(MockPlace place) {
            if (!selected.remove(place.getName())) {
                selected.add(place.getName());
            }
            renderAll();
        }

        private void offerSwap(int slot) {
            if (pool.isEmpty()) {
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
