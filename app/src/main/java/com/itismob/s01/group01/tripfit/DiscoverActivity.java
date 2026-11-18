package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.CatalogSuggestions;
import com.itismob.s01.group01.tripfit.prototype.CatalogSuggestions.Filter;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.CatalogDayPicker;
import com.itismob.s01.group01.tripfit.ui.CatalogRows;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DiscoverActivity extends ScreenActivity {
    public static final String EXTRA_OPEN_ITINERARY = "extra_open_itinerary";
    private static final int[] FILTER_LABELS = {
            R.string.catalog_all_places, R.string.catalog_famous, R.string.discover_hidden,
            R.string.tab_food, R.string.tab_activities
    };
    private static final String STATE_CITY = "state_city";
    private static final String STATE_DAY = "state_day";
    private static final String STATE_FILTER = "state_filter";
    private static final String STATE_QUERY = "state_query";
    private static final String STATE_BUDGET = "state_budget";
    private static final String STATE_SELECTED = "state_selected";
    private static final String STATE_PLAN = "state_plan";
    private static final String STATE_CHECKED = "state_checked";
    private static final String STATE_PICKER = "state_picker";
    private static final String STATE_SWAP = "state_swap";

    private enum Picker { CITY, FILTER, DAY, SWAP }

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private final Set<String> selected = new LinkedHashSet<>();
    private List<ItineraryItemWithPlace> itinerary;
    private TripRepository repository;
    private String city;
    private int tripDays;
    private int day;
    private Filter filter = Filter.ALL;
    private String query = "";
    private boolean saving;
    private Chip fitsBudget;
    private MaterialButton addSelected;
    private MaterialButton buildPlan;
    private MaterialButton dayButton;
    private MaterialButton filterButton;
    private AlertDialog planDialog;
    private List<DemoCatalog.Place> planOptions;
    private boolean[] planChecked;
    private AlertDialog pickerDialog;
    private Picker picker;
    private String swapPlaceId;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!setScreen(R.layout.activity_discover, R.string.discover_title)) {
            return;
        }
        repository = ((TripFitApp) getApplication()).getTripRepository();
        tripDays = store.meta(tripId).getDays();
        city = store.meta(tripId).getCity();
        day = TripSnapshots.clampDay(getIntent().getIntExtra(AddDestinationActivity.EXTRA_DAY, 1),
                tripDays);
        fitsBudget = findViewById(R.id.chip_fits_budget);
        restoreState(state);
        addSelected = findViewById(R.id.button_add_selected);
        buildPlan = findViewById(R.id.button_build_plan);
        dayButton = findViewById(R.id.button_catalog_day);
        filterButton = findViewById(R.id.button_catalog_filter);
        ((RecyclerView) findViewById(R.id.list_discover)).setAdapter(adapter);

        TextInputEditText search = findViewById(R.id.input_catalog_search);
        search.setText(query);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString();
                render();
            }
        });
        fitsBudget.setOnCheckedChangeListener((button, checked) -> render());
        findViewById(R.id.button_change_city).setOnClickListener(v -> chooseCity());
        filterButton.setOnClickListener(v -> chooseFilter());
        dayButton.setOnClickListener(v -> chooseDay());
        buildPlan.setOnClickListener(v -> buildSuggestedPlan());
        addSelected.setOnClickListener(v -> add(selectedPlaces(), false));
        render();
        repository.observeItineraryItemsWithPlaceNames(tripId).observe(this, rows -> {
            boolean firstLoad = itinerary == null;
            itinerary = rows;
            render();
            if (firstLoad && state != null && Picker.SWAP.name().equals(state.getString(STATE_PICKER))) {
                DemoCatalog.Place place = DemoCatalog.findById(state.getString(STATE_SWAP));
                if (place != null) {
                    chooseSwap(place);
                }
            }
        });
        if (planOptions != null && planChecked != null && planOptions.size() == planChecked.length) {
            showPlan(planOptions, planChecked);
        } else if (state != null) {
            restorePicker(state.getString(STATE_PICKER, ""));
        }
    }

    private void restoreState(@Nullable Bundle state) {
        if (state == null) {
            return;
        }
        city = state.getString(STATE_CITY, city);
        filter = Filter.valueOf(state.getString(STATE_FILTER, Filter.ALL.name()));
        query = state.getString(STATE_QUERY, "");
        day = TripSnapshots.clampDay(state.getInt(STATE_DAY, day), tripDays);
        fitsBudget.setChecked(state.getBoolean(STATE_BUDGET));
        ArrayList<String> restored = state.getStringArrayList(STATE_SELECTED);
        if (restored != null) {
            selected.addAll(restored);
        }
        ArrayList<String> plan = state.getStringArrayList(STATE_PLAN);
        if (plan != null) {
            planOptions = placesById(plan);
            planChecked = state.getBooleanArray(STATE_CHECKED);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(STATE_CITY, city);
        state.putString(STATE_FILTER, filter.name());
        state.putString(STATE_QUERY, query);
        state.putInt(STATE_DAY, day);
        state.putBoolean(STATE_BUDGET, fitsBudget.isChecked());
        state.putStringArrayList(STATE_SELECTED, new ArrayList<>(selected));
        if (pickerDialog != null && pickerDialog.isShowing()) {
            state.putString(STATE_PICKER, picker.name());
            state.putString(STATE_SWAP, swapPlaceId);
        }
        if (planDialog != null && planDialog.isShowing()) {
            ArrayList<String> ids = new ArrayList<>();
            for (DemoCatalog.Place place : planOptions) {
                ids.add(place.id);
            }
            state.putStringArrayList(STATE_PLAN, ids);
            state.putBooleanArray(STATE_CHECKED, planChecked);
        }
    }

    @Override
    protected void onDestroy() {
        if (planDialog != null) {
            planDialog.dismiss();
        }
        if (pickerDialog != null) {
            pickerDialog.dismiss();
        }
        super.onDestroy();
    }

    private void restorePicker(String savedPicker) {
        if (Picker.CITY.name().equals(savedPicker)) {
            chooseCity();
        } else if (Picker.FILTER.name().equals(savedPicker)) {
            chooseFilter();
        } else if (Picker.DAY.name().equals(savedPicker)) {
            chooseDay();
        }
    }

    private void chooseDay() {
        picker = Picker.DAY;
        pickerDialog = CatalogDayPicker.show(this, tripDays, day, selectedDay -> {
            day = selectedDay;
            render();
        });
    }

    private void chooseCity() {
        DemoCatalog.City[] cities = DemoCatalog.City.values();
        String[] labels = new String[cities.length];
        for (int i = 0; i < cities.length; i++) {
            labels[i] = cities[i].name;
        }
        picker = Picker.CITY;
        pickerDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.city_dialog_title)
                .setItems(labels, (dialog, index) -> {
                    if (!city.equals(labels[index])) {
                        city = labels[index];
                        selected.clear();
                        render();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void chooseFilter() {
        String[] labels = new String[FILTER_LABELS.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = getString(FILTER_LABELS[i]);
        }
        picker = Picker.FILTER;
        pickerDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.catalog_filter)
                .setSingleChoiceItems(labels, filter.ordinal(), (dialog, index) -> {
                    dialog.dismiss();
                    filter = Filter.values()[index];
                    render();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void render() {
        ((TextView) findViewById(R.id.text_city)).setText(city);
        dayButton.setText(getString(R.string.suggestions_target_day, day));
        filterButton.setText(FILTER_LABELS[filter.ordinal()]);
        findViewById(R.id.button_change_city).setEnabled(!saving);
        dayButton.setEnabled(!saving);
        buildPlan.setEnabled(!saving && itinerary != null);
        updateAddButton();
        if (itinerary == null) {
            return;
        }

        Set<String> planned = CatalogSuggestions.plannedNames(itinerary);
        for (DemoCatalog.Place place : selectedPlaces()) {
            if (planned.contains(DemoCatalog.nameKey(place.name))) {
                selected.remove(place.id);
            }
        }
        updateAddButton();
        CatalogRows cards = new CatalogRows(this, tripId, store, itinerary);
        CatalogSuggestions.Anchor anchor = CatalogSuggestions.anchor(city, day, itinerary);
        ((TextView) findViewById(R.id.text_catalog_nearby)).setText(cards.nearbyLabel(anchor));
        List<UiRow> rows = new ArrayList<>();
        for (DemoCatalog.Place place : CatalogSuggestions.nearby(city, query, filter, anchor)) {
            boolean already = planned.contains(DemoCatalog.nameKey(place.name));
            if (fitsBudget.isChecked() && !already && cards.isOverBudget(place)) {
                continue;
            }
            String subtitle = cards.subtitle(place, anchor.distance(place), already);
            if (already || saving) {
                rows.add(UiRow.plain(place.name, subtitle).withSelected(selected.contains(place.id)));
                continue;
            }
            boolean checked = selected.contains(place.id);
            rows.add(UiRow.withAction(place.name, subtitle, null,
                            getString(checked ? R.string.catalog_remove : R.string.catalog_select),
                            () -> toggle(place))
                    .withSelected(checked)
                    .withAccessibility(getString(checked ? R.string.discover_remove_cd
                            : R.string.discover_add_cd, place.name), null)
                    .withSecondaryAction(getString(R.string.catalog_swap),
                            getString(R.string.catalog_swap_cd, place.name), () -> chooseSwap(place)));
        }
        adapter.submit(rows);
        findViewById(R.id.text_catalog_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void toggle(DemoCatalog.Place place) {
        if (!selected.add(place.id)) {
            selected.remove(place.id);
        }
        render();
    }

    private void updateAddButton() {
        addSelected.setEnabled(!saving && itinerary != null && !selected.isEmpty());
        addSelected.setText(saving ? getString(R.string.discover_adding) : selected.isEmpty()
                ? getString(R.string.discover_none_selected)
                : getString(R.string.action_add_selected, selected.size()));
    }

    private List<DemoCatalog.Place> selectedPlaces() {
        return placesById(selected);
    }

    private static List<DemoCatalog.Place> placesById(Iterable<String> ids) {
        List<DemoCatalog.Place> places = new ArrayList<>();
        for (String id : ids) {
            DemoCatalog.Place place = DemoCatalog.findById(id);
            if (place != null) {
                places.add(place);
            }
        }
        return places;
    }

    private void buildSuggestedPlan() {
        List<DemoCatalog.Place> picks = CatalogSuggestions.suggested(city,
                CatalogSuggestions.plannedNames(itinerary));
        if (picks.isEmpty()) {
            toast(R.string.catalog_plan_empty);
            return;
        }
        boolean[] checked = new boolean[picks.size()];
        java.util.Arrays.fill(checked, true);
        showPlan(picks, checked);
    }

    private void showPlan(List<DemoCatalog.Place> picks, boolean[] checked) {
        planOptions = picks;
        planChecked = checked;
        String[] labels = new String[picks.size()];
        for (int i = 0; i < picks.size(); i++) {
            DemoCatalog.Place place = picks.get(i);
            labels[i] = getString(R.string.catalog_plan_option,
                    getString(place.hidden ? R.string.catalog_hidden : R.string.catalog_famous),
                    place.name);
        }
        planDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.catalog_plan_title)
                .setMultiChoiceItems(labels, checked, (dialog, index, selected) -> checked[index] = selected)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.catalog_plan_add, (dialog, button) -> {
                    List<DemoCatalog.Place> confirmed = new ArrayList<>();
                    for (int i = 0; i < picks.size(); i++) {
                        if (checked[i]) {
                            confirmed.add(picks.get(i));
                        }
                    }
                    add(confirmed, true);
                })
                .show();
    }

    private void add(List<DemoCatalog.Place> places, boolean spread) {
        if (saving || places.isEmpty()) {
            return;
        }
        saving = true;
        render();
        repository.addCatalogPlaces(tripId, places, day, spread, result -> {
            if (isDestroyed()) {
                return;
            }
            saving = false;
            if (!result.isSuccess()) {
                showWriteError(result);
                render();
                return;
            }
            for (DemoCatalog.Place place : places) {
                selected.remove(place.id);
            }
            if (spread && !getIntent().getBooleanExtra(EXTRA_OPEN_ITINERARY, false)) {
                render();
                toast(getResources().getQuantityString(R.plurals.catalog_plan_added,
                        places.size(), places.size()));
            } else {
                finishAdding(places.size());
            }
        });
    }

    private void chooseSwap(DemoCatalog.Place place) {
        if (saving) {
            return;
        }
        if (CatalogSuggestions.plannedNames(itinerary)
                .contains(DemoCatalog.nameKey(place.name))) {
            toast(R.string.catalog_already_planned);
            return;
        }
        List<String> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (ItineraryItemWithPlace entry : itinerary) {
            if (!entry.item.isBooked()) {
                ids.add(entry.item.getId());
                labels.add(getString(R.string.catalog_swap_option, entry.item.getDay(),
                        entry.item.displayName(entry.placeName)));
            }
        }
        if (ids.isEmpty()) {
            toast(R.string.catalog_swap_empty);
            return;
        }
        picker = Picker.SWAP;
        swapPlaceId = place.id;
        pickerDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.catalog_swap_title)
                .setItems(labels.toArray(new String[0]), (dialog, index) -> {
                    saving = true;
                    render();
                    repository.replaceWithCatalogPlace(tripId, ids.get(index), place, result -> {
                        if (isDestroyed()) {
                            return;
                        }
                        saving = false;
                        if (result.isSuccess()) {
                            selected.remove(place.id);
                            toast(R.string.catalog_swapped);
                        } else {
                            showWriteError(result);
                        }
                        render();
                    });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showWriteError(Result<Void> result) {
        String error = result.getErrorMessage();
        if (TripRepository.CATALOG_ALREADY_PLANNED.equals(error)) {
            toast(R.string.catalog_already_planned);
        } else if (TripRepository.CATALOG_STOP_UNAVAILABLE.equals(error)) {
            toast(R.string.catalog_swap_unavailable);
        } else {
            toast(R.string.error_save_failed);
        }
    }

    private void finishAdding(int count) {
        Toast.makeText(this, getResources().getQuantityString(R.plurals.discover_added,
                count, count), Toast.LENGTH_SHORT).show();
        if (getIntent().getBooleanExtra(EXTRA_OPEN_ITINERARY, false)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        }
        finish();
    }
}
