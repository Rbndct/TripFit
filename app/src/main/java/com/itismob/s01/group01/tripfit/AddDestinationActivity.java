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

    private static final long SEARCH_DEBOUNCE_MS = 350;

    private TextInputLayout layoutName;
    private TextInputLayout layoutDay;
    private TextInputLayout layoutTime;
    private TextInputEditText inputName;
    private TextInputEditText inputDay;
    private TextInputEditText inputDuration;
    private TextInputEditText inputTime;
    private TextInputEditText inputSearch;
    private MaterialSwitch switchBooked;
    private ChipGroup chipGroupCities;
    private TextView searchEmptyState;
    private AddDestinationViewModel viewModel;
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
        setScreen(R.layout.activity_add_destination, R.string.add_destination_title);

        TripFitApp app = (TripFitApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new AddDestinationViewModel.Factory(app.getTripRepository()))
                .get(AddDestinationViewModel.class);

        layoutName = findViewById(R.id.layout_name);
        layoutDay = findViewById(R.id.layout_day);
        layoutTime = findViewById(R.id.layout_time);
        inputName = findViewById(R.id.input_name);
        inputDay = findViewById(R.id.input_day);
        inputDuration = findViewById(R.id.input_duration);
        inputTime = findViewById(R.id.input_time);
        inputSearch = findViewById(R.id.input_search);
        switchBooked = findViewById(R.id.switch_booked);
        chipGroupCities = findViewById(R.id.chip_group_cities);
        searchEmptyState = findViewById(R.id.text_search_empty);

        setUpSearch();
        setUpMapPicker();

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
                Snackbar.make(findViewById(R.id.add_destination_root), R.string.cities_load_error,
                        Snackbar.LENGTH_SHORT).show();
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
                selectedPlace = null;
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
                    double lat = data.getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LAT, 0);
                    double lng = data.getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LNG, 0);
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
        selectedPlace = place;
        suppressNameWatcher = true;
        inputName.setText(place.getName());
        suppressNameWatcher = false;
        layoutName.setError(null);
        inputSearch.setText("");
    }

    private int pickedHour = -1;
    private int pickedMinute = -1;

    private void showTimePicker() {
        int hour = pickedHour >= 0 ? pickedHour : 9;
        int minute = pickedMinute >= 0 ? pickedMinute : 0;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(R.string.select_time)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            pickedHour = picker.getHour();
            pickedMinute = picker.getMinute();
            inputTime.setText(String.format(Locale.getDefault(), "%02d:%02d", pickedHour, pickedMinute));
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

        if (switchBooked.isChecked() && pickedHour < 0) {
            layoutTime.setError(getString(R.string.error_time_required));
            firstInvalid = firstInvalid == null ? inputTime : firstInvalid;
        } else if (!switchBooked.isChecked()) {
            layoutTime.setError(null);
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
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

    /** Returns an error message for the day field, or null when it holds a day of 1 or more. */
    @Nullable
    private String validateDay() {
        String text = safeText(inputDay);
        if (TextUtils.isEmpty(text)) {
            return getString(R.string.error_day_required);
        }
        return Integer.parseInt(text) < 1 ? getString(R.string.error_day_invalid) : null;
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
        int day = Integer.parseInt(safeText(inputDay));
        String durationText = safeText(inputDuration);
        Integer durationMin = durationText.isEmpty() ? null : Integer.parseInt(durationText);
        boolean booked = switchBooked.isChecked();
        String time = booked
                ? String.format(Locale.getDefault(), "%02d:%02d", pickedHour, pickedMinute)
                : null;

        if (selectedPlace != null && selectedPlace.getName().equals(name)) {
            viewModel.saveFromPlace(tripId, selectedPlace, day, durationMin, booked, time);
        } else {
            viewModel.save(tripId, name, day, durationMin, booked, time);
        }
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
