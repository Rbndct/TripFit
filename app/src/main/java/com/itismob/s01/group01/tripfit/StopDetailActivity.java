package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;
import com.itismob.s01.group01.tripfit.ui.TimeFields;

/**
 * Edits one itinerary stop: day, stay duration, booked fixed time, buffer, notes and photo.
 * Day, duration and booked/time persist to Room; buffer, notes and photo are session-only
 * until their backend tables exist.
 */
public class StopDetailActivity extends ScreenActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_DAY = "extra_day";
    public static final String EXTRA_POSITION = "extra_position";
    public static final String EXTRA_TIME = "extra_time";
    public static final String EXTRA_DURATION = "extra_duration";
    public static final String EXTRA_BOOKED = "extra_booked";
    public static final String EXTRA_MAX_DAY = "extra_max_day";

    private TripRepository repository;
    private String itemId;
    private String stopName;
    private int originalDay;
    private int selectedDay;
    private int position;
    private String fixedTime;
    private TextInputLayout layoutTime;
    private TextInputEditText inputTime;
    private TextInputEditText inputDuration;
    private TextInputLayout layoutDuration;
    private MaterialSwitch switchBooked;
    private View rowDay;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPicker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_stop_detail, R.string.stop_detail_title);
        repository = ((TripFitApp) getApplication()).getTripRepository();

        Intent in = getIntent();
        itemId = in.getStringExtra(EXTRA_ITEM_ID);
        stopName = in.getStringExtra(EXTRA_NAME);
        originalDay = in.getIntExtra(EXTRA_DAY, 1);
        selectedDay = originalDay;
        position = in.getIntExtra(EXTRA_POSITION, 0);
        fixedTime = in.getStringExtra(EXTRA_TIME);

        TextView title = findViewById(R.id.text_stop_title);
        title.setText(stopName);
        ViewCompat.setAccessibilityHeading(title, true);
        bindDayRow();
        bindDuration(in.getIntExtra(EXTRA_DURATION, -1));
        bindBooked(in.getBooleanExtra(EXTRA_BOOKED, false));
        bindBuffer();
        bindPhoto();

        findViewById(R.id.button_nearby).setOnClickListener(v -> {
            Intent nearby = new Intent(this, SuggestionsActivity.class);
            nearby.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            nearby.putExtra(SuggestionsActivity.EXTRA_DAY, selectedDay);
            startActivity(nearby);
        });
        findViewById(R.id.button_save).setOnClickListener(v -> save());
        findViewById(R.id.button_delete).setOnClickListener(v -> confirmDelete());
    }

    private void bindDayRow() {
        rowDay = findViewById(R.id.row_day);
        ((TextView) rowDay.findViewById(R.id.text_setting_title)).setText(R.string.move_day_title);
        TextView value = rowDay.findViewById(R.id.text_setting_value);
        value.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more_24, 0);
        value.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.space_xs));
        // Align the row's text with the fields below it (item_setting carries its own gutter).
        rowDay.setPaddingRelative(0, rowDay.getPaddingTop(), 0, rowDay.getPaddingBottom());
        renderDayValue();
        rowDay.setOnClickListener(v -> pickDay());
    }

    private void renderDayValue() {
        ((TextView) rowDay.findViewById(R.id.text_setting_value))
                .setText(getString(R.string.day_label, selectedDay));
    }

    private void pickDay() {
        int maxDay = getIntent().getIntExtra(EXTRA_MAX_DAY, 3);
        String[] labels = new String[maxDay];
        for (int i = 0; i < maxDay; i++) {
            labels[i] = getString(R.string.day_label, i + 1);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.move_day_title)
                .setSingleChoiceItems(labels, selectedDay - 1, (dialog, which) -> {
                    selectedDay = which + 1;
                    renderDayValue();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void bindDuration(int durationMin) {
        inputDuration = findViewById(R.id.input_duration);
        layoutDuration = findViewById(R.id.layout_duration);
        if (durationMin > 0) {
            inputDuration.setText(String.valueOf(durationMin));
        }
        layoutDuration.setHelperText(getString(R.string.duration_helper, ScheduleMath.DEFAULT_STAY_MIN));
        inputDuration.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layoutDuration.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void bindBooked(boolean booked) {
        switchBooked = findViewById(R.id.switch_booked);
        layoutTime = findViewById(R.id.layout_time);
        inputTime = findViewById(R.id.input_time);
        switchBooked.setChecked(booked);
        layoutTime.setVisibility(booked ? View.VISIBLE : View.GONE);
        if (fixedTime != null) {
            inputTime.setText(fixedTime);
        }
        switchBooked.setOnCheckedChangeListener((button, checked) -> {
            layoutTime.setVisibility(checked ? View.VISIBLE : View.GONE);
            layoutTime.setError(null);
        });
        View.OnClickListener picker = v -> TimeFields.pick(this, fixedTime, time -> {
            fixedTime = time;
            inputTime.setText(time);
            layoutTime.setError(null);
        });
        inputTime.setOnClickListener(picker);
        layoutTime.setEndIconOnClickListener(picker);
    }

    private void bindBuffer() {
        TextView helper = findViewById(R.id.text_buffer_helper);
        MaterialSwitch bufferSwitch = findViewById(R.id.switch_buffer);
        int minutes = PrototypeStore.get().getBufferMin();
        if (minutes > 0) {
            helper.setText(getString(R.string.buffer_helper, minutes));
        } else {
            helper.setText(R.string.buffer_none);
            bufferSwitch.setChecked(false);
        }
        bufferSwitch.setEnabled(minutes > 0);
        bufferSwitch.setOnCheckedChangeListener((button, checked) ->
                helper.setVisibility(checked ? View.VISIBLE : View.INVISIBLE));
    }

    private void bindPhoto() {
        MaterialButton attach = findViewById(R.id.button_attach_photo);
        photoPicker = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                (Uri uri) -> {
                    if (uri != null) {
                        attach.setText(R.string.photo_attached);
                    }
                });
        attach.setOnClickListener(v -> photoPicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
    }

    private void save() {
        boolean booked = switchBooked.isChecked();
        String durationText = inputDuration.getText() == null
                ? "" : inputDuration.getText().toString().trim();
        Integer duration = durationText.isEmpty() ? null : Integer.parseInt(durationText);
        if (duration != null && duration < 1) {
            layoutDuration.setError(getString(R.string.error_duration_invalid));
            inputDuration.requestFocus();
            return;
        }
        if (booked && TextUtils.isEmpty(fixedTime)) {
            layoutTime.setError(getString(R.string.error_time_required));
            layoutTime.requestRectangleOnScreen(
                    new Rect(0, 0, layoutTime.getWidth(), layoutTime.getHeight()));
            return;
        }
        View saveButton = findViewById(R.id.button_save);
        saveButton.setEnabled(false);
        repository.updateItineraryItem(tripId, itemId, originalDay, position, selectedDay,
                booked ? fixedTime : null, duration, booked, result -> {
                    if (result.isSuccess()) {
                        finish();
                    } else {
                        saveButton.setEnabled(true);
                        toast(R.string.error_update_failed);
                    }
                });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_stop_title)
                .setMessage(getString(R.string.delete_stop_message, stopName, originalDay))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        repository.deleteItineraryItem(itemId, result -> {
                            if (result.isSuccess()) {
                                finish();
                            } else {
                                toast(R.string.error_update_failed);
                            }
                        }))
                .show();
    }
}
