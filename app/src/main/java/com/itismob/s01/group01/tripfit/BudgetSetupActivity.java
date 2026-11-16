package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
    private final List<TextInputLayout> limitLayouts = new ArrayList<>();
    private int days;
    private boolean isSaving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_budget_setup, R.string.budget_setup_title);
        current = store.budget(tripId);
        days = Math.max(1, store.meta(tripId).getDays());

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
                CurrencyRates.codes()));
        inputCurrency.setText(current.getHomeCurrency(), false);

        boolean hasSplit = !current.getCategoryLimits().isEmpty();
        switchSplit.setChecked(hasSplit);
        findViewById(R.id.split_container).setVisibility(hasSplit ? View.VISIBLE : View.GONE);
        for (Category category : Category.values()) {
            double limit = current.limitFor(category);
            limitInputs.get(category).setText(limit > 0 ? String.valueOf((long) limit) : "");
        }
        useLabelFont(inputBudget);
        useLabelFont(inputCurrency);
        for (TextInputEditText input : limitInputs.values()) {
            limitLayouts.add(useLabelFont(input));
        }
        refreshLabels();
        bindListeners();
    }

    /** Wires change listeners after the initial values are set, so binding never fires them. */
    private void bindListeners() {
        toggleMode.addOnButtonCheckedListener((group, id, isChecked) -> {
            if (isChecked) {
                refreshLabels();
            }
        });
        inputCurrency.addTextChangedListener(new SimpleWatcher(this::refreshLabels));
        inputBudget.addTextChangedListener(new SimpleWatcher(() -> {
            layoutBudget.setError(null);
            refreshLabels();
        }));
        switchSplit.setOnCheckedChangeListener((button, checked) -> {
            findViewById(R.id.split_container).setVisibility(checked ? View.VISIBLE : View.GONE);
            updateSplitSum();
        });
        TextWatcher watcher = new SimpleWatcher(this::updateSplitSum);
        for (TextInputEditText input : limitInputs.values()) {
            input.addTextChangedListener(watcher);
        }
        limitInputs.get(Category.OTHER).setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            save();
            return true;
        });
    }

    /** Labels float in the sans-serif body face; only the typed values stay monospace. */
    private static TextInputLayout useLabelFont(View field) {
        ViewParent parent = field.getParent();
        while (parent != null && !(parent instanceof TextInputLayout)) {
            parent = parent.getParent();
        }
        TextInputLayout layout = (TextInputLayout) parent;
        if (layout != null) {
            layout.setTypeface(Typeface.DEFAULT);
        }
        return layout;
    }

    private String homeCode() {
        return inputCurrency.getText().toString();
    }

    private boolean isPerDay() {
        return toggleMode.getCheckedButtonId() == R.id.button_mode_daily;
    }

    private double tripTotal(double amount) {
        return isPerDay() ? amount * days : amount;
    }

    /** Re-derives everything that depends on the mode, the amount, or the home currency. */
    private void refreshLabels() {
        String home = homeCode();
        double amount = parse(inputBudget);
        layoutBudget.setHint(getString(
                isPerDay() ? R.string.field_budget_hint_daily : R.string.field_budget_hint, home));
        layoutBudget.setHelperText(isPerDay() && amount > 0
                ? getResources().getQuantityString(R.plurals.budget_daily_total, days,
                        CurrencyRates.format(amount, home), days,
                        CurrencyRates.format(tripTotal(amount), home))
                : null);
        currencyNote.setText(getString(R.string.budget_currency_note, home));
        for (TextInputLayout layout : limitLayouts) {
            layout.setPrefixText(home + " ");
        }
        updateSplitSum();
    }

    private double parse(TextInputEditText input) {
        return parse(input.getText());
    }

    private double parse(CharSequence text) {
        String trimmed = text == null ? "" : text.toString().trim();
        return TextUtils.isEmpty(trimmed) ? 0 : Double.parseDouble(trimmed);
    }

    private double splitTotal() {
        double sum = 0;
        for (TextInputEditText input : limitInputs.values()) {
            sum += parse(input);
        }
        return sum;
    }

    /** Shows the split against the trip total; over-limit reads as words and in the error color. */
    private void updateSplitSum() {
        String home = homeCode();
        double split = splitTotal();
        double budget = tripTotal(parse(inputBudget));
        boolean isOver = split > budget;
        splitSum.setText(isOver
                ? getString(R.string.budget_split_over, CurrencyRates.format(split - budget, home),
                        CurrencyRates.format(budget, home))
                : getString(R.string.budget_split_sum, CurrencyRates.format(split, home),
                        CurrencyRates.format(budget, home)));
        splitSum.setTextColor(MaterialColors.getColor(splitSum, isOver
                ? androidx.appcompat.R.attr.colorError
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    private void save() {
        if (isSaving) {
            return;
        }
        double amount = parse(inputBudget);
        if (amount <= 0) {
            layoutBudget.setError(getString(R.string.error_budget_required));
            inputBudget.requestFocus();
            return;
        }
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        if (switchSplit.isChecked()) {
            if (splitTotal() > tripTotal(amount)) {
                toast(R.string.error_split_exceeds);
                limitInputs.get(Category.FOOD).requestFocus();
                return;
            }
            for (Category category : Category.values()) {
                limits.put(category, parse(limitInputs.get(category)));
            }
        }
        isSaving = true;
        store.setBudget(tripId, new BudgetSettings(homeCode(), current.getLocalCurrency(), amount,
                isPerDay(), limits));
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
