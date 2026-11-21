package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.MockData;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripDates;
import com.itismob.s01.group01.tripfit.prototype.TripFormRules;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.viewmodel.NewTripViewModel;
import com.itismob.s01.group01.tripfit.viewmodel.TripUpdateOutcome;

import java.util.Arrays;
import java.util.List;

/**
 * The one trip form. Without a trip id it creates a trip (name, city, ISO start date, length,
 * home currency, budget) and hands off to the itinerary or Discover; with
 * {@link HomeActivity#EXTRA_TRIP_ID} it edits that trip's settings in place. Trip details live in
 * {@link PrototypeStore}'s {@link TripMeta}; only the name is a Room column.
 */
public class NewTripActivity extends ScreenActivity {

    /** Discovery, catalog, pins and time zones only cover these two cities. */
    private static final List<String> CITIES = Arrays.asList("Tokyo", "Manila");
    private static final int DEFAULT_TRIP_DAYS = 3;
    private static final double DEFAULT_BUDGET = 15000;
    private static final String DEFAULT_CURRENCY = "PHP";

    private final PrototypeStore store = PrototypeStore.get();
    private TextInputLayout layoutName;
    private TextInputLayout layoutCity;
    private TextInputLayout layoutDates;
    private TextInputLayout layoutDays;
    private TextInputLayout layoutBudget;
    private MaterialButton buttonSave;
    private TextInputEditText inputName;
    private TextInputEditText inputDates;
    private TextInputEditText inputDays;
    private TextInputEditText inputBudget;
    private MaterialAutoCompleteTextView inputCity;
    private MaterialAutoCompleteTextView inputCurrency;
    private MaterialSwitch switchDaily;
    private MaterialButtonToggleGroup togglePlanMode;
    private NewTripViewModel viewModel;
    private boolean editing;

    @Override
    protected boolean requiresTrip() {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setScreen(R.layout.activity_new_trip, R.string.new_trip_title)) {
            return;
        }
        editing = tripId != null && !tripId.trim().isEmpty();
        TripFitApp app = (TripFitApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new NewTripViewModel.Factory(app.getTripRepository())).get(NewTripViewModel.class);
        bindViews();
        fillFields();
        bindListeners();
        buttonSave.setEnabled(!viewModel.isPending());
        viewModel.getCreateResult().observe(this, event -> onCreateResult(event == null ? null : event.consume()));
        viewModel.getUpdateResult().observe(this, event -> onUpdateResult(event == null ? null : event.consume()));
    }

    private void bindViews() {
        layoutName = findViewById(R.id.layout_trip_name);
        layoutCity = findViewById(R.id.layout_city);
        layoutDates = findViewById(R.id.layout_dates);
        layoutDays = findViewById(R.id.layout_days);
        layoutBudget = findViewById(R.id.layout_budget);
        buttonSave = findViewById(R.id.button_create_trip);
        inputName = findViewById(R.id.input_trip_name);
        inputDates = findViewById(R.id.input_dates);
        inputDays = findViewById(R.id.input_days);
        inputBudget = findViewById(R.id.input_budget);
        inputCity = findViewById(R.id.input_city);
        inputCurrency = findViewById(R.id.input_currency);
        switchDaily = findViewById(R.id.switch_daily_budget);
        togglePlanMode = findViewById(R.id.toggle_plan_mode);
        // Monospace is for the values; hints, helper text and errors stay Roboto.
        for (int id : new int[]{R.id.layout_dates, R.id.layout_days, R.id.layout_currency,
                R.id.layout_budget}) {
            ((TextInputLayout) findViewById(id)).setTypeface(Typeface.DEFAULT);
        }
    }

    /** Sets defaults or the trip's current values; the framework restores edited text afterwards. */
    private void fillFields() {
        inputCity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, CITIES));
        inputCurrency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                CurrencyRates.codes()));
        togglePlanMode.check(R.id.button_plan_manual);
        if (editing) {
            ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle(R.string.trip_settings_title);
            buttonSave.setText(R.string.action_save_trip_settings);
            findViewById(R.id.text_edit_note).setVisibility(View.VISIBLE);
            findViewById(R.id.plan_mode_group).setVisibility(View.GONE);
            layoutBudget.setVisibility(View.GONE);
            switchDaily.setVisibility(View.GONE);
            View budgetButton = findViewById(R.id.button_edit_budget_settings);
            budgetButton.setVisibility(View.VISIBLE);
            budgetButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, BudgetSetupActivity.class);
                intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
                startActivity(intent);
            });
            inputDays.setImeOptions(EditorInfo.IME_ACTION_DONE);
            TripMeta meta = store.meta(tripId);
            inputCity.setText(meta.getCity(), false);
            inputDates.setText(meta.getStartDateIso());
            inputDays.setText(String.valueOf(meta.getDays()));
            inputCurrency.setText(meta.getHomeCurrency(), false);
            loadName(); // its isEmpty guard keeps any text restored after rotation
        } else {
            inputCity.setText(CITIES.get(0), false);
            inputDates.setText(TripDates.todayIso());
            inputDays.setText(String.valueOf(DEFAULT_TRIP_DAYS));
            inputCurrency.setText(defaultCurrency(), false);
            inputBudget.setText(String.valueOf((int) DEFAULT_BUDGET));
        }
    }

    private String defaultCurrency() {
        String code = store.getDefaultHomeCurrency();
        return code == null || code.isEmpty() ? DEFAULT_CURRENCY : code;
    }

    private void loadName() {
        ((TripFitApp) getApplication()).getTripRepository().observeTrip(tripId)
                .observe(this, trip -> {
                    if (trip != null && TextUtils.isEmpty(inputName.getText())) {
                        inputName.setText(trip.getName());
                    }
                });
    }

    private void bindListeners() {
        layoutDates.setEndIconOnClickListener(v -> pickDates());
        for (Pair<TextInputEditText, TextInputLayout> pair : Arrays.asList(
                Pair.create(inputName, layoutName), Pair.create(inputDates, layoutDates),
                Pair.create(inputDays, layoutDays), Pair.create(inputBudget, layoutBudget))) {
            pair.first.addTextChangedListener(new ErrorClearer(pair.second));
        }
        inputCity.setOnItemClickListener((parent, view, position, id) -> layoutCity.setError(null));
        TextInputEditText last = editing ? inputDays : inputBudget;
        last.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            attemptSave();
            return true;
        });
        buttonSave.setOnClickListener(v -> attemptSave());
    }

    /** The range picker feeds both the ISO start date and the trip length. */
    private void pickDates() {
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.select_dates_title)
                        .build();
        picker.addOnPositiveButtonClickListener(range -> {
            int days = TripDates.daysBetweenInclusive(range.first, range.second);
            inputDates.setText(TripDates.isoFromUtcMillis(range.first));
            inputDays.setText(String.valueOf(days));
            if (TripFormRules.parseDays(String.valueOf(days)) == null) {
                layoutDays.setError(getString(R.string.error_days_range));
            }
        });
        picker.show(getSupportFragmentManager(), "date_range");
    }

    private String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    /** Validates every field, shows each error, and focuses the first invalid one. */
    private boolean validate() {
        boolean nameOk = !TextUtils.isEmpty(text(inputName));
        boolean cityOk = !TextUtils.isEmpty(inputCity.getText().toString().trim());
        boolean dateOk = TripFormRules.isValidIsoDate(text(inputDates));
        boolean daysOk = TripFormRules.parseDays(text(inputDays)) != null;
        boolean budgetOk = editing || TripFormRules.parseBudget(text(inputBudget)) != null;
        layoutName.setError(nameOk ? null : getString(R.string.error_trip_name_required));
        layoutCity.setError(cityOk ? null : getString(R.string.error_city_required));
        layoutDates.setError(dateOk ? null : getString(R.string.error_start_date_invalid));
        layoutDays.setError(daysOk ? null : getString(R.string.error_days_range));
        layoutBudget.setError(budgetOk ? null : getString(R.string.error_budget_range));
        if (!nameOk) {
            inputName.requestFocus();
        } else if (!cityOk) {
            inputCity.requestFocus();
        } else if (!dateOk) {
            inputDates.requestFocus();
        } else if (!daysOk) {
            inputDays.requestFocus();
        } else if (!budgetOk) {
            inputBudget.requestFocus();
        }
        return nameOk && cityOk && dateOk && daysOk && budgetOk;
    }

    private void attemptSave() {
        if (!buttonSave.isEnabled() || !validate()) {
            return;
        }
        buttonSave.setEnabled(false);
        if (editing) {
            viewModel.updateTrip(tripId, text(inputName),
                    TripFormRules.parseDays(text(inputDays)));
        } else {
            viewModel.createTrip(text(inputName));
        }
    }

    private TripMeta formMeta() {
        int days = orDefault(TripFormRules.parseDays(text(inputDays)), DEFAULT_TRIP_DAYS);
        String start = text(inputDates);
        return new TripMeta(inputCity.getText().toString().trim(), TripDates.label(start, days),
                days, inputCurrency.getText().toString().trim(), start);
    }

    private static int orDefault(@Nullable Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private void onUpdateResult(@Nullable TripUpdateOutcome outcome) {
        if (outcome == null) {
            return;
        }
        if (outcome.getStatus() == TripUpdateOutcome.Status.SAVED) {
            store.setMeta(tripId, formMeta());
            finish();
            return;
        }
        buttonSave.setEnabled(true);
        if (outcome.getStatus() == TripUpdateOutcome.Status.BLOCKED_BY_STOPS) {
            layoutDays.setError(getString(R.string.error_shorten_blocked_day,
                    outcome.getMaxStopDay()));
            inputDays.requestFocus();
        } else {
            toast(R.string.error_save_trip_failed);
        }
    }

    private void onCreateResult(@Nullable Result<String> result) {
        if (result == null) {
            return;
        }
        if (!result.isSuccess()) {
            buttonSave.setEnabled(true);
            toast(R.string.error_create_trip_failed);
            return;
        }
        String newTripId = result.getData();
        TripMeta meta = formMeta();
        store.setMeta(newTripId, meta);
        store.setBudget(newTripId, newBudget(meta));

        boolean discover = togglePlanMode.getCheckedButtonId() == R.id.button_plan_discover;
        Intent intent = new Intent(this, discover ? DiscoverActivity.class : MainActivity.class);
        intent.putExtra(HomeActivity.EXTRA_TRIP_ID, newTripId);
        intent.putExtra(DiscoverActivity.EXTRA_OPEN_ITINERARY, discover);
        startActivity(intent);
        finish();
    }

    /** Budget with the default 30/40/20/10 category split of the amount (also per day in daily mode). */
    private BudgetSettings newBudget(TripMeta meta) {
        Double parsed = TripFormRules.parseBudget(text(inputBudget));
        double amount = parsed == null ? DEFAULT_BUDGET : parsed;
        return new BudgetSettings(meta.getHomeCurrency(), MockData.currencyFor(meta.getCity()),
                amount, switchDaily.isChecked(), BudgetMath.defaultLimits(amount));
    }

    /** Clears a field's error as soon as the user edits it. */
    private static final class ErrorClearer implements TextWatcher {
        private final TextInputLayout layout;

        ErrorClearer(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            layout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
