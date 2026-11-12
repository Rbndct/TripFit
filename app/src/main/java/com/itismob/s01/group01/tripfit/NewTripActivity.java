package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.prototype.CurrencyMock;
import com.itismob.s01.group01.tripfit.prototype.MockData;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.viewmodel.NewTripViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Creates a new trip (name, city, dates, home currency), then hands off either to the empty
 * itinerary ("I'll add stops") or to Discover ("Suggest for me").
 */
public class NewTripActivity extends ScreenActivity {

    private static final int DEFAULT_TRIP_DAYS = 3;

    private TextInputLayout layoutName;
    private TextInputLayout layoutCity;
    private TextInputEditText inputName;
    private TextInputEditText inputDates;
    private MaterialAutoCompleteTextView inputCity;
    private MaterialAutoCompleteTextView inputCurrency;
    private MaterialButtonToggleGroup togglePlanMode;
    private NewTripViewModel viewModel;
    private String datesLabel = "";
    private int tripDays = DEFAULT_TRIP_DAYS;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_new_trip, R.string.new_trip_title);

        TripFitApp app = (TripFitApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new NewTripViewModel.Factory(app.getTripRepository())).get(NewTripViewModel.class);

        layoutName = findViewById(R.id.layout_trip_name);
        layoutCity = findViewById(R.id.layout_city);
        inputName = findViewById(R.id.input_trip_name);
        inputDates = findViewById(R.id.input_dates);
        inputCity = findViewById(R.id.input_city);
        inputCurrency = findViewById(R.id.input_currency);
        togglePlanMode = findViewById(R.id.toggle_plan_mode);

        inputCity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                MockData.CITIES));
        inputCity.setText(MockData.CITIES.get(0), false);
        inputCurrency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                CurrencyMock.codes()));
        inputCurrency.setText(PrototypeStore.get().getDefaultHomeCurrency(), false);
        togglePlanMode.check(R.id.button_plan_manual);

        inputDates.setOnClickListener(v -> pickDates());
        findViewById(R.id.layout_dates).setOnClickListener(v -> pickDates());
        findViewById(R.id.button_create_trip).setOnClickListener(v -> attemptCreate());
        viewModel.getCreateResult().observe(this, this::onCreateResult);
    }

    private void pickDates() {
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.select_dates_title)
                        .build();
        picker.addOnPositiveButtonClickListener(range -> {
            SimpleDateFormat format = new SimpleDateFormat("MMM d", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            datesLabel = format.format(new Date(range.first)) + " – "
                    + format.format(new Date(range.second));
            tripDays = (int) TimeUnit.MILLISECONDS.toDays(range.second - range.first) + 1;
            inputDates.setText(datesLabel);
        });
        picker.show(getSupportFragmentManager(), "date_range");
    }

    private void attemptCreate() {
        String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
        String city = inputCity.getText().toString().trim();
        layoutName.setError(TextUtils.isEmpty(name)
                ? getString(R.string.error_trip_name_required) : null);
        layoutCity.setError(TextUtils.isEmpty(city) ? getString(R.string.error_city_required) : null);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(city)) {
            return;
        }
        viewModel.createTrip(name);
    }

    private void onCreateResult(Result<String> result) {
        if (result == null) {
            return;
        }
        if (!result.isSuccess()) {
            toast(R.string.error_create_trip_failed);
            return;
        }
        String newTripId = result.getData();
        PrototypeStore.get().setMeta(newTripId, new TripMeta(inputCity.getText().toString().trim(),
                datesLabel, tripDays, inputCurrency.getText().toString().trim()));

        boolean discover = togglePlanMode.getCheckedButtonId() == R.id.button_plan_discover;
        Intent intent = new Intent(this, discover ? DiscoverActivity.class : MainActivity.class);
        intent.putExtra(HomeActivity.EXTRA_TRIP_ID, newTripId);
        intent.putExtra(DiscoverActivity.EXTRA_OPEN_ITINERARY, discover);
        startActivity(intent);
        finish();
    }
}
