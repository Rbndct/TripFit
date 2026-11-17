package com.itismob.s01.group01.tripfit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;
import com.itismob.s01.group01.tripfit.prototype.StopFormRules;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.CostFields;
import com.itismob.s01.group01.tripfit.ui.PlaceSearchAdapter;
import com.itismob.s01.group01.tripfit.viewmodel.AddDestinationViewModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Sprint 1 UI for the "Add Destination" and "Booked Activity Lock-In" backlog items, now
 * searching our pipeline-seeded {@code places} database instead of free text only.
 * Validates input, then persists the stop to Room via {@link AddDestinationViewModel}.
 */
public class AddDestinationActivity extends ScreenActivity {

    /** Optional: the itinerary's selected day, used as the new stop's default (P-118). */
    public static final String EXTRA_DAY = "extra_day";
    private static final long SEARCH_DEBOUNCE_MS = 350;
    private static final String KEY_PENDING_ITEM = "pending_item_id";
    private static final String KEY_PLACE_ID = "place_id";
    private static final String KEY_PLACE_CITY = "place_city";
    private static final String KEY_PLACE_NAME = "place_name";
    private static final String KEY_PLACE_CATEGORY = "place_category";
    private static final String KEY_PLACE_ADDRESS = "place_address";
    private static final String KEY_PLACE_LAT = "place_lat";
    private static final String KEY_PLACE_LNG = "place_lng";

    private TextInputLayout layoutName;
    private TextInputLayout layoutDay;
    private TextInputLayout layoutTime;
    private TextInputLayout layoutDuration;
    private int tripDays = 1;
    private TextInputEditText inputName;
    private TextInputEditText inputDay;
    private TextInputEditText inputDuration;
    private TextInputEditText inputTime;
    private TextInputEditText inputSearch;
    private MaterialSwitch switchBooked;
    private ChipGroup chipGroupCities;
    private TextView searchEmptyState;
    private AddDestinationViewModel viewModel;
    private CostFields costFields;
    private String pendingItemId;
    private PlaceSearchAdapter searchAdapter;
    private final Set<String> existingNames = new HashSet<>();

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private String selectedCityId;
    private CityDto selectedCity;
    private PlaceDto selectedPlace;
    private boolean suppressNameWatcher;
    private ActivityResultLauncher<Intent> mapPickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setScreen(R.layout.activity_add_destination, R.string.add_destination_title)) {
            return;
        }

        TripFitApp app = (TripFitApp) getApplication();
        if (savedInstanceState != null) {
            pendingItemId = savedInstanceState.getString(KEY_PENDING_ITEM);
        }
        viewModel = new ViewModelProvider(this,
                new AddDestinationViewModel.Factory(app.getTripRepository()))
                .get(AddDestinationViewModel.class);

        layoutName = findViewById(R.id.layout_name);
        layoutDay = findViewById(R.id.layout_day);
        layoutTime = findViewById(R.id.layout_time);
        layoutDuration = findViewById(R.id.layout_duration);
        inputName = findViewById(R.id.input_name);
        inputDay = findViewById(R.id.input_day);
        inputDuration = findViewById(R.id.input_duration);
        inputTime = findViewById(R.id.input_time);
        inputSearch = findViewById(R.id.input_search);
        switchBooked = findViewById(R.id.switch_booked);
        chipGroupCities = findViewById(R.id.chip_group_cities);
        searchEmptyState = findViewById(R.id.text_search_empty);

        tripDays = PrototypeStore.get().meta(tripId).getDays();
        BudgetSettings budget = PrototypeStore.get().budget(tripId);
        costFields = CostFields.bind(findViewById(R.id.cost_fields), budget.getHomeCurrency(),
                budget.getLocalCurrency());
        if (savedInstanceState == null) {
            // The new stop starts on the itinerary's selected day (P-118).
            inputDay.setText(String.valueOf(TripSnapshots.clampDay(
                    getIntent().getIntExtra(EXTRA_DAY, 1), tripDays)));
        }
        clearErrorOnEdit(inputName, layoutName);
        clearErrorOnEdit(inputDay, layoutDay);
        clearErrorOnEdit(inputDuration, layoutDuration);
        setUpSearch();
        setUpMapPicker();
        renderPin();

        switchBooked.setOnCheckedChangeListener((button, checked) ->
                layoutTime.setVisibility(checked ? View.VISIBLE : View.GONE));

        View.OnClickListener openTimePicker = v -> showTimePicker();
        inputTime.setOnClickListener(openTimePicker);
        layoutTime.setEndIconOnClickListener(openTimePicker);

        findViewById(R.id.button_save).setOnClickListener(v -> attemptSave());

        viewModel.getCitiesResult().observe(this, result -> {
            if (result == null) {
                return;
            }
            if (result.isSuccess()) {
                bindCityChips(result.getData());
            } else {
                bindCityChips(java.util.Arrays.asList(
                        new CityDto("demo-tokyo", "Tokyo", "JP", 35.6762, 139.6503),
                        new CityDto("demo-manila", "Manila", "PH", 14.5995, 120.9842)));
            }
        });
        viewModel.loadCities();
        observeExistingNames(app);

        viewModel.getSearchResult().observe(this, result -> {
            if (result == null) {
                return;
            }
            List<PlaceDto> places = result.isSuccess() ? result.getData() : Collections.emptyList();
            searchAdapter.submitPlaces(places);
            boolean hasQuery = inputSearch.getText() != null && inputSearch.getText().length() > 0;
            searchEmptyState.setText(result.isSuccess()
                    ? R.string.search_empty_state : R.string.search_error);
            searchEmptyState.setVisibility(hasQuery && places.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getSaveResult().observe(this, result -> {
            if (result == null) {
                return;
            }
            if (result.isSuccess()) {
                Snackbar.make(findViewById(R.id.add_destination_root), R.string.destination_saved,
                        Snackbar.LENGTH_SHORT).show();
                finish();
            } else {
                if (pendingItemId != null) {
                    PrototypeStore.get().removeItemDetails(pendingItemId); // no stop was created
                    pendingItemId = null;
                }
                findViewById(R.id.button_save).setEnabled(true);
                Snackbar.make(findViewById(R.id.add_destination_root), R.string.error_save_failed,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void setUpSearch() {
        RecyclerView recyclerView = findViewById(R.id.recycler_search_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new PlaceSearchAdapter(this::onPlaceSelected);
        recyclerView.setAdapter(searchAdapter);

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressNameWatcher) return;
                selectedPlace = null;
                renderPin();
                scheduleSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        inputName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!suppressNameWatcher) {
                    selectedPlace = null;
                    renderPin();
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void setUpMapPicker() {
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Intent data = result.getData();
                    double lat = data.getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LAT, Double.NaN);
                    double lng = data.getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LNG, Double.NaN);
                    if (!StopFormRules.isPinValid(lat, lng)) {
                        Snackbar.make(findViewById(R.id.add_destination_root),
                                R.string.error_pin_invalid, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    String name = data.getStringExtra(MapPickerActivity.EXTRA_RESULT_NAME);
                    if (TextUtils.isEmpty(name)) {
                        name = getString(R.string.map_picker_fallback_name);
                    }
                    onPlaceSelected(new PlaceDto(UUID.randomUUID().toString(), selectedCityId,
                            name, null, lat, lng, null, null, null, false, null, null));
                });

        findViewById(R.id.button_pick_on_map).setOnClickListener(v -> {
            if (selectedCity == null) {
                return;
            }
            Intent intent = new Intent(AddDestinationActivity.this, MapPickerActivity.class);
            intent.putExtra(MapPickerActivity.EXTRA_CITY_LAT, selectedCity.getLat());
            intent.putExtra(MapPickerActivity.EXTRA_CITY_LNG, selectedCity.getLng());
            mapPickerLauncher.launch(intent);
        });
    }

    private void scheduleSearch(String query) {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> viewModel.search(selectedCityId, query);
        searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    private void bindCityChips(List<CityDto> cities) {
        chipGroupCities.removeAllViews();
        for (CityDto city : cities) {
            Chip chip = new Chip(this);
            chip.setText(city.getName());
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            chip.setTag(city);
            chipGroupCities.addView(chip);
        }
        chipGroupCities.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedCity = checkedIds.isEmpty() ? null : (CityDto) group.findViewById(checkedIds.get(0)).getTag();
            selectedCityId = selectedCity == null ? null : selectedCity.getId();
            findViewById(R.id.button_pick_on_map).setEnabled(selectedCity != null);
            String query = inputSearch.getText() == null ? "" : inputSearch.getText().toString();
            scheduleSearch(query);
        });
        if (!cities.isEmpty()) {
            ((Chip) chipGroupCities.getChildAt(0)).setChecked(true);
        }
    }

    private void onPlaceSelected(PlaceDto place) {
        suppressNameWatcher = true;
        inputName.setText(place.getName());
        layoutName.setError(null);
        inputSearch.setText("");
        suppressNameWatcher = false;
        selectedPlace = place;
        renderPin();
    }

    private void showTimePicker() {
        String typed = safeText(inputTime);
        int minuteOfDay = typed.isEmpty() ? ScheduleMath.DAY_START_MIN : ScheduleMath.parse(typed);
        int hour = minuteOfDay / 60;
        int minute = minuteOfDay % 60;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(R.string.select_time)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            inputTime.setText(ScheduleMath.format(picker.getHour() * 60 + picker.getMinute()));
            layoutTime.setError(null);
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private void attemptSave() {
        View firstInvalid = null;

        if (TextUtils.isEmpty(safeText(inputName))) {
            layoutName.setError(getString(R.string.error_name_required));
            firstInvalid = inputName;
        } else {
            layoutName.setError(null);
        }

        String dayError = validateDay();
        layoutDay.setError(dayError);
        if (dayError != null && firstInvalid == null) {
            firstInvalid = inputDay;
        }

        if (!StopFormRules.isDurationValid(safeText(inputDuration))) {
            layoutDuration.setError(getString(R.string.error_duration_range));
            firstInvalid = firstInvalid == null ? inputDuration : firstInvalid;
        }

        if (!costFields.validate()) {
            firstInvalid = firstInvalid == null ? costFields.costInput() : firstInvalid;
        }

        if (switchBooked.isChecked() && safeText(inputTime).isEmpty()) {
            layoutTime.setError(getString(R.string.error_time_required));
            firstInvalid = firstInvalid == null ? inputTime : firstInvalid;
        } else if (!switchBooked.isChecked()) {
            layoutTime.setError(null);
        }

        if (firstInvalid != null) {
            if (firstInvalid == inputTime) {
                // Picker-only field: scroll to it instead of focusing.
                layoutTime.requestRectangleOnScreen(
                        new android.graphics.Rect(0, 0, layoutTime.getWidth(), layoutTime.getHeight()));
            } else {
                firstInvalid.requestFocus();
            }
            return;
        }

        String name = safeText(inputName);
        if (existingNames.contains(name.toLowerCase(Locale.ROOT))) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.duplicate_title)
                    .setMessage(getString(R.string.duplicate_message, name))
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_add_anyway, (dialog, which) -> persist(name))
                    .show();
            return;
        }
        persist(name);
    }

    /** Returns an error message for the day field, or null when it is a day of 1..trip days. */
    @Nullable
    private String validateDay() {
        String text = safeText(inputDay);
        if (TextUtils.isEmpty(text)) {
            return getString(R.string.error_day_required);
        }
        return StopFormRules.isDayValid(text, tripDays)
                ? null : getString(R.string.error_day_range, tripDays);
    }

    private static void clearErrorOnEdit(TextInputEditText input, TextInputLayout layout) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    // ---- picked place / pin draft (survives rotation) ----

    private void renderPin() {
        TextView status = findViewById(R.id.text_pin_status);
        status.setText(selectedPlace == null ? getString(R.string.pin_none)
                : getString(R.string.pin_selected, String.format(Locale.US, "%.5f, %.5f",
                        selectedPlace.getLat(), selectedPlace.getLng())));
    }

    @Override
    protected void onDestroy() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PENDING_ITEM, pendingItemId);
        if (selectedPlace != null) {
            outState.putString(KEY_PLACE_ID, selectedPlace.getId());
            outState.putString(KEY_PLACE_CITY, selectedPlace.getCityId());
            outState.putString(KEY_PLACE_NAME, selectedPlace.getName());
            outState.putString(KEY_PLACE_CATEGORY, selectedPlace.getCategory());
            outState.putString(KEY_PLACE_ADDRESS, selectedPlace.getAddress());
            outState.putDouble(KEY_PLACE_LAT, selectedPlace.getLat());
            outState.putDouble(KEY_PLACE_LNG, selectedPlace.getLng());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        // After the views restored their text: the name watcher has already cleared any pick.
        restorePlace(state);
        renderPin();
    }

    private void restorePlace(Bundle state) {
        if (state.getString(KEY_PLACE_ID) != null) {
            selectedPlace = new PlaceDto(state.getString(KEY_PLACE_ID), state.getString(KEY_PLACE_CITY),
                    state.getString(KEY_PLACE_NAME), state.getString(KEY_PLACE_CATEGORY),
                    state.getDouble(KEY_PLACE_LAT), state.getDouble(KEY_PLACE_LNG),
                    state.getString(KEY_PLACE_ADDRESS), null, null, false, null, null);
        }
    }

    private void observeExistingNames(TripFitApp app) {
        app.getTripRepository().observeItineraryItemsWithPlaceNames(tripId).observe(this, items -> {
            existingNames.clear();
            for (ItineraryItemWithPlace entry : items) {
                existingNames.add(entry.item.displayName(entry.placeName).toLowerCase(Locale.ROOT));
            }
        });
    }

    private void persist(String name) {
        findViewById(R.id.button_save).setEnabled(false); // re-enabled if the save fails
        int day = Integer.parseInt(safeText(inputDay));
        int durationMin = StopFormRules.durationOrDefault(safeText(inputDuration));
        boolean booked = switchBooked.isChecked();
        String time = booked ? safeText(inputTime) : null;

        // The details are keyed by the new row's id; they are dropped again if the save fails.
        pendingItemId = UUID.randomUUID().toString();
        PrototypeStore.get().setItemDetails(pendingItemId, costFields.toDetails(null));
        if (selectedPlace != null && selectedPlace.getName().equals(name)) {
            viewModel.saveFromPlace(pendingItemId, tripId, selectedPlace, day, durationMin, booked,
                    time);
        } else {
            viewModel.save(pendingItemId, tripId, name, day, durationMin, booked, time);
        }
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
