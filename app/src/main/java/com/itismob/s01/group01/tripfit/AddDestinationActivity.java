package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.itismob.s01.group01.tripfit.viewmodel.AddDestinationViewModel;

import java.util.Locale;

/**
 * Sprint 1 UI for the "Add Destination" and "Booked Activity Lock-In" backlog items.
 * Validates input, then persists the stop to Room via {@link AddDestinationViewModel}.
 */
public class AddDestinationActivity extends AppCompatActivity {

    private TextInputLayout layoutName;
    private TextInputLayout layoutDay;
    private TextInputLayout layoutTime;
    private TextInputEditText inputName;
    private TextInputEditText inputDay;
    private TextInputEditText inputDuration;
    private TextInputEditText inputTime;
    private MaterialSwitch switchBooked;
    private AddDestinationViewModel viewModel;
    private String tripId;

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
        switchBooked = findViewById(R.id.switch_booked);

        switchBooked.setOnCheckedChangeListener((button, checked) ->
                layoutTime.setVisibility(checked ? View.VISIBLE : View.GONE));

        View.OnClickListener openTimePicker = v -> showTimePicker();
        inputTime.setOnClickListener(openTimePicker);
        layoutTime.setEndIconOnClickListener(openTimePicker);

        findViewById(R.id.button_save).setOnClickListener(v -> attemptSave());

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

        viewModel.save(tripId, name, day, durationMin, booked, time);
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
