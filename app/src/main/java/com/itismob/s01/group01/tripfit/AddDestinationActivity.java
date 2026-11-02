package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;
import com.itismob.s01.group01.tripfit.ui.PlaceSearchAdapter;
import com.itismob.s01.group01.tripfit.viewmodel.AddDestinationViewModel;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sprint 1 UI for the "Add Destination" and "Booked Activity Lock-In" backlog items, now
 * searching our pipeline-seeded {@code places} database instead of free text only.
 * Validates input, then persists the stop to Room via {@link AddDestinationViewModel}.
 */
public class AddDestinationActivity extends AppCompatActivity {

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
    private View searchEmptyState;
    private AddDestinationViewModel viewModel;
    private PlaceSearchAdapter searchAdapter;
    private String tripId;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private String selectedCityId;
    private PlaceDto selectedPlace;
    private boolean suppressNameWatcher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_destination);

        tripId = getIntent().getStringExtra(HomeActivity.EXTRA_TRIP_ID);

        TripFitApp app = (TripFitApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new AddDestinationViewModel.Factory(app.getTripRepository()))
                .get(AddDestinationViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

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

        viewModel.getSearchResult().observe(this, result -> {
            if (result == null) {
                return;
            }
            List<PlaceDto> places = result.isSuccess() ? result.getData() : Collections.emptyList();
            searchAdapter.submitPlaces(places);
            boolean hasQuery = inputSearch.getText() != null && inputSearch.getText().length() > 0;
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
            chip.setTag(city.getId());
            chipGroupCities.addView(chip);
        }
        chipGroupCities.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedCityId = checkedIds.isEmpty() ? null : (String) group.findViewById(checkedIds.get(0)).getTag();
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
        boolean valid = true;

        if (TextUtils.isEmpty(safeText(inputName))) {
            layoutName.setError(getString(R.string.error_name_required));
            valid = false;
        } else {
            layoutName.setError(null);
        }

        if (TextUtils.isEmpty(safeText(inputDay))) {
            layoutDay.setError(getString(R.string.error_day_required));
            valid = false;
        } else {
            layoutDay.setError(null);
        }

        if (switchBooked.isChecked() && pickedHour < 0) {
            layoutTime.setError(getString(R.string.error_time_required));
            valid = false;
        } else if (!switchBooked.isChecked()) {
            layoutTime.setError(null);
        }

        if (!valid) {
            return;
        }

        String name = safeText(inputName);
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
