package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Locale;

/**
 * Sprint 1 UI for the "Add Destination" and "Booked Activity Lock-In" backlog items.
 * Validates input and shows a confirmation; wiring to persistence lands with the DB task.
 */
public class AddDestinationActivity extends AppCompatActivity {

    private TextInputLayout layoutName;
    private TextInputLayout layoutDay;
    private TextInputLayout layoutTime;
    private TextInputEditText inputName;
    private TextInputEditText inputDay;
    private TextInputEditText inputTime;
    private MaterialSwitch switchBooked;

    private int pickedHour = -1;
    private int pickedMinute = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_destination);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        layoutName = findViewById(R.id.layout_name);
        layoutDay = findViewById(R.id.layout_day);
        layoutTime = findViewById(R.id.layout_time);
        inputName = findViewById(R.id.input_name);
        inputDay = findViewById(R.id.input_day);
        inputTime = findViewById(R.id.input_time);
        switchBooked = findViewById(R.id.switch_booked);

        switchBooked.setOnCheckedChangeListener((button, checked) ->
                layoutTime.setVisibility(checked ? View.VISIBLE : View.GONE));

        View.OnClickListener openTimePicker = v -> showTimePicker();
        inputTime.setOnClickListener(openTimePicker);
        layoutTime.setEndIconOnClickListener(openTimePicker);

        findViewById(R.id.button_save).setOnClickListener(v -> attemptSave());
    }

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

        Snackbar.make(findViewById(R.id.add_destination_root), R.string.saved_ui_only,
                Snackbar.LENGTH_SHORT).show();
        finish();
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
