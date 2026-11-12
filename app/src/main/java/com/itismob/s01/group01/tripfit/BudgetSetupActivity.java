package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.CurrencyMock;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;

import java.util.EnumMap;
import java.util.Map;

/** Sets an overall or per-day trip budget, home currency, and optional category split. */
public class BudgetSetupActivity extends ScreenActivity {

    private final PrototypeStore store = PrototypeStore.get();
    private final Map<Category, TextInputEditText> limitInputs = new EnumMap<>(Category.class);
    private BudgetSettings current;
    private MaterialButtonToggleGroup toggleMode;
    private TextInputLayout layoutBudget;
    private TextInputEditText inputBudget;
    private MaterialAutoCompleteTextView inputCurrency;
    private MaterialSwitch switchSplit;
    private TextView splitSum;
    private TextView currencyNote;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_budget_setup, R.string.budget_setup_title);
        current = store.budget(tripId);

        toggleMode = findViewById(R.id.toggle_mode);
        layoutBudget = findViewById(R.id.layout_budget);
        inputBudget = findViewById(R.id.input_budget);
        inputCurrency = findViewById(R.id.input_home_currency);
        switchSplit = findViewById(R.id.switch_split);
        splitSum = findViewById(R.id.text_split_sum);
        currencyNote = findViewById(R.id.text_currency_note);
        limitInputs.put(Category.FOOD, findViewById(R.id.input_limit_food));
        limitInputs.put(Category.ACTIVITIES, findViewById(R.id.input_limit_activities));
        limitInputs.put(Category.TRANSPORT, findViewById(R.id.input_limit_transport));
        limitInputs.put(Category.OTHER, findViewById(R.id.input_limit_other));

        bindCurrent();
        findViewById(R.id.button_save_budget).setOnClickListener(v -> save());
    }

    private void bindCurrent() {
        toggleMode.check(current.isPerDay() ? R.id.button_mode_daily : R.id.button_mode_overall);
        inputBudget.setText(String.valueOf((long) current.getAmount()));
        inputCurrency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                CurrencyMock.codes()));
        inputCurrency.setText(current.getHomeCurrency(), false);
        inputCurrency.setOnItemClickListener((parent, v, position, id) -> {
            updateCurrencyHint(CurrencyMock.codes().get(position));
        });
        updateCurrencyHint(current.getHomeCurrency());

        boolean hasSplit = !current.getCategoryLimits().isEmpty();
        switchSplit.setChecked(hasSplit);
        findViewById(R.id.split_container).setVisibility(hasSplit ? View.VISIBLE : View.GONE);
        switchSplit.setOnCheckedChangeListener((button, checked) -> {
            findViewById(R.id.split_container).setVisibility(checked ? View.VISIBLE : View.GONE);
            updateSplitSum();
        });
        TextWatcher watcher = new SimpleWatcher(this::updateSplitSum);
        inputBudget.addTextChangedListener(watcher);
        for (Category category : Category.values()) {
            TextInputEditText input = limitInputs.get(category);
            double limit = current.limitFor(category);
            input.setText(limit > 0 ? String.valueOf((long) limit) : "");
            input.addTextChangedListener(watcher);
        }
        updateSplitSum();
    }

    private void updateCurrencyHint(String homeCode) {
        layoutBudget.setHint(getString(R.string.field_budget_hint, homeCode));
        currencyNote.setText(getString(R.string.budget_currency_note, homeCode));
    }

    private double parse(TextInputEditText input) {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        return TextUtils.isEmpty(text) ? 0 : Double.parseDouble(text);
    }

    private double splitTotal() {
        double sum = 0;
        for (TextInputEditText input : limitInputs.values()) {
            sum += parse(input);
        }
        return sum;
    }

    private void updateSplitSum() {
        String home = inputCurrency.getText().toString();
        splitSum.setText(getString(R.string.budget_split_sum,
                CurrencyMock.format(splitTotal(), home),
                CurrencyMock.format(parse(inputBudget), home)));
    }

    private void save() {
        double amount = parse(inputBudget);
        if (amount <= 0) {
            layoutBudget.setError(getString(R.string.error_budget_required));
            return;
        }
        layoutBudget.setError(null);
        boolean perDay = toggleMode.getCheckedButtonId() == R.id.button_mode_daily;
        int days = store.meta(tripId).getDays();
        double tripTotal = perDay ? amount * days : amount;
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        if (switchSplit.isChecked()) {
            if (splitTotal() > tripTotal) {
                toast(R.string.error_split_exceeds);
                return;
            }
            for (Category category : Category.values()) {
                limits.put(category, parse(limitInputs.get(category)));
            }
        }
        store.setBudget(tripId, new BudgetSettings(inputCurrency.getText().toString(),
                current.getLocalCurrency(), amount, perDay, limits));
        finish();
    }

    /** Runs a callback on every text change; keeps the watcher boilerplate in one place. */
    private static final class SimpleWatcher implements TextWatcher {
        private final Runnable onChange;

        SimpleWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChange.run();
        }

        @Override
        public void afterTextChanged(Editable s) { }
    }
}
