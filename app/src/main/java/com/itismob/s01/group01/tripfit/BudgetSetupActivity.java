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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.itismob.s01.group01.tripfit.prototype.AmountRules;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
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
    private final Map<Category, TextInputLayout> limitLayouts = new EnumMap<>(Category.class);
    private MaterialButton saveButton;
    private double lastAmount;
    private int days;
    private boolean isSaving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setScreen(R.layout.activity_budget_setup, R.string.budget_setup_title)) {
            return;
        }
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

        saveButton = findViewById(R.id.button_save_budget);
        bindCurrent();
        saveButton.setOnClickListener(v -> save());
    }

    private void bindCurrent() {
        toggleMode.check(current.isPerDay() ? R.id.button_mode_daily : R.id.button_mode_overall);
        inputBudget.setText(AmountRules.format(current.getAmount()));
        lastAmount = current.getAmount();
        inputCurrency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                CurrencyRates.codes()));
        inputCurrency.setText(current.getHomeCurrency(), false);

        boolean hasSplit = !current.getCategoryLimits().isEmpty();
        switchSplit.setChecked(hasSplit);
        findViewById(R.id.split_container).setVisibility(hasSplit ? View.VISIBLE : View.GONE);
        for (Category category : Category.values()) {
            double limit = current.limitFor(category);
            limitInputs.get(category).setText(limit > 0 ? AmountRules.format(limit) : "");
        }
        useLabelFont(inputBudget);
        useLabelFont(inputCurrency);
        for (Category category : Category.values()) {
            limitLayouts.put(category, useLabelFont(limitInputs.get(category)));
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
            followDefaultSplit();
            refreshLabels();
        }));
        switchSplit.setOnCheckedChangeListener((button, checked) -> {
            findViewById(R.id.split_container).setVisibility(checked ? View.VISIBLE : View.GONE);
            // Blank fields get the default split; untouched defaults already follow the amount.
            if (checked && splitIsBlank()) {
                showLimits(BudgetMath.defaultLimits(parse(inputBudget)));
            }
            updateSplitSum();
        });
        for (Category category : Category.values()) {
            limitInputs.get(category).addTextChangedListener(new SimpleWatcher(() -> {
                limitLayouts.get(category).setError(null);
                updateSplitSum();
            }));
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
        findViewById(R.id.text_split_note).setVisibility(isPerDay() ? View.VISIBLE : View.GONE);
        for (TextInputLayout layout : limitLayouts.values()) {
            layout.setPrefixText(home + " ");
        }
        updateSplitSum();
    }

    /** Parsed amount for live feedback; blank or invalid text counts as 0 until Save validates it. */
    private double parse(TextInputEditText input) {
        Double value = AmountRules.parseOptional(String.valueOf(input.getText()));
        return value == null ? 0 : value;
    }

    private double splitTotal() {
        double sum = 0;
        for (TextInputEditText input : limitInputs.values()) {
            sum += parse(input);
        }
        return sum;
    }

    private boolean splitIsBlank() {
        return splitTotal() == 0;
    }

    private Map<Category, Double> typedLimits() {
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            limits.put(category, parse(limitInputs.get(category)));
        }
        return limits;
    }

    private void showLimits(Map<Category, Double> limits) {
        for (Category category : Category.values()) {
            double limit = limits.get(category);
            limitInputs.get(category).setText(limit > 0 ? AmountRules.format(limit) : "");
        }
    }

    /**
     * Keeps the 30/40/20/10 default split in step with the budget, even while the split section
     * is hidden: limits that still equal the default split of the previous amount follow the new
     * amount; numbers the user changed are left alone.
     */
    private void followDefaultSplit() {
        double amount = parse(inputBudget);
        Map<Category, Double> typed = typedLimits();
        Map<Category, Double> next = BudgetMath.followDefaultSplit(typed, lastAmount, amount);
        if (next != typed) {
            showLimits(next);
        }
        lastAmount = amount;
    }

    /**
     * Shows the split against the budget in the unit being edited (trip or per day), because in
     * daily mode the limits are per day too. Over-limit reads as words and in the error color.
     */
    private void updateSplitSum() {
        String home = homeCode();
        double split = splitTotal();
        double budget = parse(inputBudget);
        boolean isOver = !AmountRules.fitsWithin(split, budget);
        boolean daily = isPerDay();
        int overRes = daily ? R.string.budget_split_over_daily : R.string.budget_split_over;
        int sumRes = daily ? R.string.budget_split_sum_daily : R.string.budget_split_sum;
        splitSum.setText(isOver
                ? getString(overRes, CurrencyRates.format(split - budget, home),
                        CurrencyRates.format(budget, home))
                : getString(sumRes, CurrencyRates.format(split, home),
                        CurrencyRates.format(budget, home)));
        splitSum.setTextColor(MaterialColors.getColor(splitSum, isOver
                ? androidx.appcompat.R.attr.colorError
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    private void save() {
        if (isSaving) {
            return;
        }
        String amountText = String.valueOf(inputBudget.getText()).trim();
        Double amount = AmountRules.parseBudget(amountText);
        if (amount == null) {
            layoutBudget.setError(getString(amountText.isEmpty()
                    ? R.string.error_budget_required : R.string.error_amount_range));
            inputBudget.requestFocus();
            return;
        }
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        if (switchSplit.isChecked() && !collectLimits(amount, limits)) {
            return;
        }
        isSaving = true;
        saveButton.setEnabled(false);
        store.setBudget(tripId, new BudgetSettings(homeCode(), current.getLocalCurrency(), amount,
                isPerDay(), limits));
        setResult(RESULT_OK);
        finish();
    }

    /** Validates each limit (0..1e8) and that they fit the budget; fills {@code out} when valid. */
    private boolean collectLimits(double amount, Map<Category, Double> out) {
        for (Category category : Category.values()) {
            Double limit = AmountRules.parseOptional(
                    String.valueOf(limitInputs.get(category).getText()));
            if (limit == null) {
                limitLayouts.get(category).setError(getString(R.string.error_limit_range));
                limitInputs.get(category).requestFocus();
                return false;
            }
            out.put(category, limit);
        }
        if (!AmountRules.fitsWithin(splitTotal(), amount)) {
            toast(R.string.error_split_exceeds);
            limitInputs.get(Category.FOOD).requestFocus();
            return false;
        }
        return true;
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
