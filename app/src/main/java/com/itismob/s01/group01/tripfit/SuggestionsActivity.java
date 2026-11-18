package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.CatalogSuggestions;
import com.itismob.s01.group01.tripfit.prototype.CatalogSuggestions.Filter;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.CatalogDayPicker;
import com.itismob.s01.group01.tripfit.ui.CatalogRows;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SuggestionsActivity extends ScreenActivity {
    public static final String EXTRA_DAY = AddDestinationActivity.EXTRA_DAY;
    private static final String STATE_TAB = "state_tab";
    private static final String STATE_DAY = "state_day";
    private static final String STATE_BUDGET = "state_budget";
    private static final String STATE_DAY_PICKER = "state_day_picker";

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private final Set<String> adding = new HashSet<>();
    private List<ItineraryItemWithPlace> itinerary;
    private TripRepository repository;
    private TabLayout tabs;
    private Chip fitsBudget;
    private MaterialButton dayButton;
    private TripMeta meta;
    private int targetDay;
    private AlertDialog dayPicker;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!setScreen(R.layout.activity_suggestions, R.string.suggestions_title)) {
            return;
        }
        repository = ((TripFitApp) getApplication()).getTripRepository();
        meta = store.meta(tripId);
        int initialDay = getIntent().getIntExtra(EXTRA_DAY, 1);
        targetDay = TripSnapshots.clampDay(state == null ? initialDay
                : state.getInt(STATE_DAY, initialDay), meta.getDays());

        ((RecyclerView) findViewById(R.id.list_suggestions)).setAdapter(adapter);
        tabs = findViewById(R.id.tabs);
        fitsBudget = findViewById(R.id.chip_fits_budget);
        dayButton = findViewById(R.id.button_catalog_day);
        if (state != null) {
            TabLayout.Tab tab = tabs.getTabAt(state.getInt(STATE_TAB));
            if (tab != null) {
                tab.select();
            }
            fitsBudget.setChecked(state.getBoolean(STATE_BUDGET));
        }
        findViewById(R.id.button_show_all).setOnClickListener(v -> fitsBudget.setChecked(false));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { render(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
        fitsBudget.setOnCheckedChangeListener((chip, checked) -> render());
        dayButton.setOnClickListener(v -> chooseDay());
        if (state != null && state.getBoolean(STATE_DAY_PICKER)) {
            chooseDay();
        }
        render();
        repository.observeItineraryItemsWithPlaceNames(tripId).observe(this, rows -> {
            itinerary = rows;
            adding.removeAll(CatalogSuggestions.plannedNames(rows));
            render();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(STATE_TAB, tabs.getSelectedTabPosition());
        state.putInt(STATE_DAY, targetDay);
        state.putBoolean(STATE_BUDGET, fitsBudget.isChecked());
        state.putBoolean(STATE_DAY_PICKER, dayPicker != null && dayPicker.isShowing());
    }

    @Override
    protected void onDestroy() {
        if (dayPicker != null) {
            dayPicker.dismiss();
        }
        super.onDestroy();
    }

    private void chooseDay() {
        dayPicker = CatalogDayPicker.show(this, meta.getDays(), targetDay, day -> {
            targetDay = day;
            render();
        });
    }

    private void render() {
        dayButton.setText(getString(R.string.suggestions_target_day, targetDay));
        if (itinerary == null) {
            return;
        }
        Filter filter = tabs.getSelectedTabPosition() == 0 ? Filter.FOOD : Filter.ACTIVITIES;
        String city = meta.getCity();
        CatalogSuggestions.Anchor anchor = CatalogSuggestions.anchor(city, targetDay, itinerary);
        CatalogRows cards = new CatalogRows(this, tripId, store, itinerary);
        ((TextView) findViewById(R.id.text_catalog_nearby)).setText(cards.nearbyLabel(anchor));
        Set<String> planned = CatalogSuggestions.plannedNames(itinerary);
        List<UiRow> rows = new ArrayList<>();
        for (DemoCatalog.Place place : CatalogSuggestions.nearby(city, "", filter, anchor)) {
            String key = DemoCatalog.nameKey(place.name);
            boolean already = planned.contains(key);
            if (fitsBudget.isChecked() && !already && cards.isOverBudget(place)) {
                continue;
            }
            String subtitle = cards.subtitle(place, anchor.distance(place), already);
            if (already || adding.contains(key)) {
                rows.add(UiRow.plain(place.name, subtitle));
            } else {
                rows.add(UiRow.withAction(place.name, subtitle, null, getString(R.string.action_add),
                                () -> addToItinerary(place))
                        .withAccessibility(getString(R.string.suggestion_add_cd,
                                place.name, targetDay), null));
            }
        }
        adapter.submit(rows);
        findViewById(R.id.group_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.button_show_all).setVisibility(fitsBudget.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void addToItinerary(DemoCatalog.Place place) {
        String key = DemoCatalog.nameKey(place.name);
        if (!adding.add(key)) {
            return;
        }
        int day = targetDay;
        render();
        repository.addCatalogPlaces(tripId, Collections.singletonList(place), day, false, result -> {
            if (isDestroyed()) {
                return;
            }
            if (result.isSuccess()) {
                toast(getString(R.string.suggestion_added, place.name, day));
            } else {
                adding.remove(key);
                if (TripRepository.CATALOG_ALREADY_PLANNED.equals(result.getErrorMessage())) {
                    toast(R.string.catalog_already_planned);
                } else {
                    toast(getString(R.string.suggestion_add_failed, place.name));
                }
            }
            render();
        });
    }
}
