package com.itismob.s01.group01.tripfit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.ItemDetails;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;
import com.itismob.s01.group01.tripfit.prototype.StopFormRules;
import com.itismob.s01.group01.tripfit.prototype.TravelMode;
import com.itismob.s01.group01.tripfit.prototype.WeatherPlanning;
import com.itismob.s01.group01.tripfit.ui.CostFields;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.ui.TimeFields;

import java.util.Locale;

/**
 * Edits one itinerary stop, loaded by id: day, stay, booked time, buffer and travel mode before
 * the stop, its map pin, cost and outdoor status. Room holds the stop; PrototypeStore holds extras.
 * The form is bound from Room once; later edits and rotation never reload over what was typed.
 */
public class StopDetailActivity extends ScreenActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";

    private static final String KEY_BOUND = "form_bound";
    private static final String KEY_DAY = "form_day";
    private static final String KEY_MODE = "form_mode";
    private static final String KEY_TIME = "form_time";
    private static final String KEY_PIN_LAT = "form_pin_lat";
    private static final String KEY_PIN_LNG = "form_pin_lng";

    private TripRepository repository;
    private String itemId;
    private ItineraryItemWithPlace current;
    private boolean formBound;
    private int selectedDay = 1;
    private TravelMode selectedMode;
    private String fixedTime;
    private Double pendingPinLat;
    private Double pendingPinLng;

    private TextInputLayout layoutTime;
    private TextInputEditText inputTime;
    private TextInputLayout layoutDuration;
    private TextInputEditText inputDuration;
    private TextInputLayout layoutBuffer;
    private TextInputEditText inputBuffer;
    private MaterialSwitch switchBooked;
    private MaterialSwitch switchOutdoor;
    private CostFields costFields;
    private View rowDay;
    private View rowMode;
    private MaterialButton saveButton;
    private boolean saving;
    private ActivityResultLauncher<Intent> pinPicker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setScreen(R.layout.activity_stop_detail, R.string.stop_detail_title)) {
            return;
        }
        itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        if (itemId == null || itemId.trim().isEmpty()) {
            finish();
            return;
        }
        repository = ((TripFitApp) getApplication()).getTripRepository();
        restoreDraft(savedInstanceState);

        ViewCompat.setAccessibilityHeading(findViewById(R.id.text_stop_title), true);
        bindDayRow();
        bindModeRow();
        bindDuration();
        bindBooked();
        bindBuffer();
        bindCost();
        switchOutdoor = findViewById(R.id.switch_outdoor);
        bindPin();
        bindPhoto();
        saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(v -> save());
        findViewById(R.id.button_delete).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.button_nearby).setOnClickListener(v -> {
            Intent nearby = new Intent(this, SuggestionsActivity.class);
            nearby.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            nearby.putExtra(SuggestionsActivity.EXTRA_DAY, selectedDay);
            startActivity(nearby);
        });

        repository.observeItemWithPlace(itemId).observe(this, row -> {
            if (row == null) {
                finish(); // deleted elsewhere
                return;
            }
            current = row;
            if (!formBound) {
                populateFromRow(row);
                formBound = true;
            }
            renderStatic();
        });
    }

    private void restoreDraft(@Nullable Bundle state) {
        if (state == null || !state.getBoolean(KEY_BOUND)) {
            return;
        }
        formBound = true; // views restore their own text; the rest comes back from the bundle
        selectedDay = state.getInt(KEY_DAY, 1);
        String mode = state.getString(KEY_MODE);
        selectedMode = mode == null ? null : TravelMode.valueOf(mode);
        fixedTime = state.getString(KEY_TIME);
        if (state.containsKey(KEY_PIN_LAT)) {
            pendingPinLat = state.getDouble(KEY_PIN_LAT);
            pendingPinLng = state.getDouble(KEY_PIN_LNG);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_BOUND, formBound);
        outState.putInt(KEY_DAY, selectedDay);
        outState.putString(KEY_MODE, selectedMode == null ? null : selectedMode.name());
        outState.putString(KEY_TIME, fixedTime);
        if (pendingPinLat != null && pendingPinLng != null) {
            outState.putDouble(KEY_PIN_LAT, pendingPinLat);
            outState.putDouble(KEY_PIN_LNG, pendingPinLng);
        }
    }

    private void populateFromRow(ItineraryItemWithPlace row) {
        selectedDay = row.item.getDay();
        selectedMode = parseMode(row.item.getTransportMode());
        fixedTime = row.item.getTime();
        if (row.item.getDurationMin() != null) {
            inputDuration.setText(String.valueOf(row.item.getDurationMin()));
        }
        // Blank means "use the profile default" (shown as a hint), so saving untouched keeps null.
        inputBuffer.setText(StopFormRules.bufferFieldText(row.item.getBufferMin()));
        costFields.populate(PrototypeStore.get().itemDetails(itemId));
        switchOutdoor.setChecked(WeatherPlanning.isOutdoor(row, PrototypeStore.get()));
        switchBooked.setChecked(row.item.isBooked());
        layoutTime.setVisibility(row.item.isBooked() ? View.VISIBLE : View.GONE);
        if (fixedTime != null) {
            inputTime.setText(fixedTime);
        }
    }

    @Nullable
    private static TravelMode parseMode(@Nullable String name) {
        try {
            return name == null ? null : TravelMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Draws the parts that come from Room or from non-text draft state. */
    private void renderStatic() {
        TextView title = findViewById(R.id.text_stop_title);
        title.setText(current.item.displayName(current.placeName));
        renderDayValue();
        renderModeValue();
        renderPin();
    }

    // ---- day ----

    private void bindDayRow() {
        rowDay = findViewById(R.id.row_day);
        ((TextView) rowDay.findViewById(R.id.text_setting_title)).setText(R.string.move_day_title);
        styleSettingRow(rowDay);
        rowDay.setOnClickListener(v -> pickDay());
    }

    private void styleSettingRow(View row) {
        TextView value = row.findViewById(R.id.text_setting_value);
        value.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more_24, 0);
        value.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.space_xs));
        // Align the row's text with the fields below it (item_setting carries its own gutter).
        row.setPaddingRelative(0, row.getPaddingTop(), 0, row.getPaddingBottom());
    }

    private void renderDayValue() {
        ((TextView) rowDay.findViewById(R.id.text_setting_value))
                .setText(getString(R.string.day_label, selectedDay));
    }

    /** Trip length, but never below this stop's own day so an existing late stop stays valid. */
    private int dayChoices() {
        return Math.max(PrototypeStore.get().meta(tripId).getDays(),
                current == null ? 1 : current.item.getDay());
    }

    private void pickDay() {
        int count = dayChoices();
        String[] labels = new String[count];
        for (int i = 0; i < count; i++) {
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

    // ---- travel mode ----

    private void bindModeRow() {
        rowMode = findViewById(R.id.row_mode);
        ((TextView) rowMode.findViewById(R.id.text_setting_title)).setText(R.string.field_mode_title);
        styleSettingRow(rowMode);
        rowMode.setOnClickListener(v -> pickMode());
    }

    private void renderModeValue() {
        ((TextView) rowMode.findViewById(R.id.text_setting_value)).setText(
                selectedMode == null ? getString(R.string.mode_suggested)
                        : Labels.mode(this, selectedMode));
    }

    private void pickMode() {
        TravelMode[] modes = TravelMode.values();
        String[] labels = new String[modes.length + 1];
        labels[0] = getString(R.string.mode_suggested);
        for (int i = 0; i < modes.length; i++) {
            labels[i + 1] = Labels.mode(this, modes[i]);
        }
        int checked = selectedMode == null ? 0 : selectedMode.ordinal() + 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.mode_dialog_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    selectedMode = which == 0 ? null : modes[which - 1];
                    renderModeValue();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // ---- text fields ----

    private void bindDuration() {
        inputDuration = findViewById(R.id.input_duration);
        layoutDuration = findViewById(R.id.layout_duration);
        layoutDuration.setHelperText(getString(R.string.duration_helper, ScheduleMath.DEFAULT_STAY_MIN));
        clearErrorOnEdit(inputDuration, layoutDuration);
    }

    private void bindBuffer() {
        inputBuffer = findViewById(R.id.input_buffer);
        layoutBuffer = findViewById(R.id.layout_buffer);
        ((TextView) findViewById(R.id.text_buffer_helper)).setText(
                getString(R.string.buffer_helper, PrototypeStore.get().getBufferMin()));
        layoutBuffer.setPlaceholderText(String.valueOf(PrototypeStore.get().getBufferMin()));
        clearErrorOnEdit(inputBuffer, layoutBuffer);
    }

    /** The stop's estimate keeps its own currency; with none saved it starts in the local one. */
    private void bindCost() {
        BudgetSettings budget = PrototypeStore.get().budget(tripId);
        costFields = CostFields.bind(findViewById(R.id.cost_fields), budget.getHomeCurrency(),
                budget.getLocalCurrency());
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

    private void bindBooked() {
        switchBooked = findViewById(R.id.switch_booked);
        layoutTime = findViewById(R.id.layout_time);
        inputTime = findViewById(R.id.input_time);
        layoutTime.setVisibility(switchBooked.isChecked() ? View.VISIBLE : View.GONE);
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

    // ---- pin ----

    private void bindPin() {
        pinPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() != Activity.RESULT_OK || data == null) {
                        return;
                    }
                    double lat = data.getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LAT, Double.NaN);
                    double lng = data.getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LNG, Double.NaN);
                    if (!StopFormRules.isPinValid(lat, lng)) {
                        toast(R.string.error_pin_invalid);
                        return;
                    }
                    pendingPinLat = lat;
                    pendingPinLng = lng;
                    renderPin();
                });
        findViewById(R.id.button_adjust_pin).setOnClickListener(v -> openPinPicker());
    }

    private void openPinPicker() {
        double[] start = currentPin();
        if (start == null) {
            String city = PrototypeStore.get().meta(tripId).getCity();
            start = new double[]{DemoCatalog.lat(city), DemoCatalog.lng(city)};
        }
        Intent intent = new Intent(this, MapPickerActivity.class);
        intent.putExtra(MapPickerActivity.EXTRA_CITY_LAT, start[0]);
        intent.putExtra(MapPickerActivity.EXTRA_CITY_LNG, start[1]);
        pinPicker.launch(intent);
    }

    /** The pin to show: a freshly picked one, else the saved one, else null. */
    @Nullable
    private double[] currentPin() {
        if (pendingPinLat != null && pendingPinLng != null) {
            return new double[]{pendingPinLat, pendingPinLng};
        }
        if (current != null && current.placeLat != null && current.placeLng != null) {
            return new double[]{current.placeLat, current.placeLng};
        }
        return null;
    }

    private void renderPin() {
        double[] pin = currentPin();
        ((TextView) findViewById(R.id.text_pin_status)).setText(pin == null
                ? getString(R.string.pin_none)
                : getString(R.string.pin_selected, String.format(Locale.US, "%.5f, %.5f", pin[0], pin[1])));
    }

    private void bindPhoto() {
        findViewById(R.id.button_attach_photo).setOnClickListener(v -> {
            Intent journal = new Intent(this, JournalActivity.class);
            journal.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            journal.putExtra(JournalActivity.EXTRA_JOURNAL_STOP, itemId);
            startActivity(journal);
        });
    }

    // ---- save / delete ----

    private void save() {
        if (saving || current == null) {
            return;
        }
        String durationText = text(inputDuration);
        String bufferText = text(inputBuffer);
        boolean booked = switchBooked.isChecked();
        View firstInvalid = null;
        if (!StopFormRules.isDurationValid(durationText)) {
            layoutDuration.setError(getString(R.string.error_duration_range));
            firstInvalid = inputDuration;
        }
        if (!StopFormRules.isBufferValid(bufferText)) {
            layoutBuffer.setError(getString(R.string.error_buffer_range));
            firstInvalid = firstInvalid == null ? inputBuffer : firstInvalid;
        }
        if (!costFields.validate()) {
            firstInvalid = firstInvalid == null ? costFields.costInput() : firstInvalid;
        }
        if (booked && TextUtils.isEmpty(fixedTime)) {
            layoutTime.setError(getString(R.string.error_time_required));
            firstInvalid = firstInvalid == null ? inputTime : firstInvalid;
        }
        if (firstInvalid != null) {
            if (firstInvalid == inputTime) {
                // The time field is picker-only (not focusable), so scroll it into view instead.
                layoutTime.requestRectangleOnScreen(
                        new Rect(0, 0, layoutTime.getWidth(), layoutTime.getHeight()));
            } else {
                firstInvalid.requestFocus();
            }
            return;
        }
        setSaving(true);
        ItemDetails details = costFields.toDetails(PrototypeStore.get().itemDetails(itemId))
                .withOutdoor(switchOutdoor.isChecked());
        Result.Callback<Void> done = result -> {
            if (result.isSuccess()) {
                PrototypeStore.get().setItemDetails(itemId, details);
                finish();
            } else {
                setSaving(false);
                toast(R.string.error_update_failed);
            }
        };
        Integer buffer = StopFormRules.bufferOrNull(bufferText);
        String mode = selectedMode == null ? null : selectedMode.name();
        int duration = StopFormRules.durationOrDefault(durationText);
        String time = booked ? fixedTime : null;
        if (pendingPinLat == null || pendingPinLng == null) {
            repository.updateItineraryItem(tripId, itemId, current.item.getDay(),
                    current.item.getPosition(), selectedDay, time, duration, booked, buffer, mode,
                    done);
            return;
        }
        repository.updateItineraryItemWithPin(tripId, itemId, current.item.getDay(),
                current.item.getPosition(), selectedDay, time, duration, booked, buffer, mode,
                current.item.displayName(current.placeName), pendingPinLat, pendingPinLng, done);
    }

    private void setSaving(boolean isSaving) {
        saving = isSaving;
        saveButton.setEnabled(!isSaving);
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void confirmDelete() {
        if (current == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_stop_title)
                .setMessage(getString(R.string.delete_stop_message,
                        current.item.displayName(current.placeName), current.item.getDay()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        repository.deleteItineraryItem(itemId, result -> {
                            if (result.isSuccess()) {
                                PrototypeStore.get().removeItemDetails(itemId);
                            } else {
                                toast(R.string.error_update_failed);
                            }
                        }))
                .show();
    }
}
