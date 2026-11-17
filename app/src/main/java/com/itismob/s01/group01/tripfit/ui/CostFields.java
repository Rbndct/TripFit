package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.AmountRules;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.ItemDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * The estimated-cost block of a stop form (cost, price currency, budget category), shared by Stop
 * Detail and Add Destination. The dropdown text is the state, so rotation restores it with the
 * views and nothing extra needs saving.
 */
public final class CostFields {

    private final Context context;
    private final TextInputLayout layoutCost;
    private final TextInputEditText inputCost;
    private final MaterialAutoCompleteTextView inputCurrency;
    private final MaterialAutoCompleteTextView inputCategory;
    private final String defaultCurrency;

    private CostFields(View root, String homeCurrency, String defaultCurrency) {
        this.context = root.getContext();
        this.defaultCurrency = defaultCurrency;
        layoutCost = root.findViewById(R.id.layout_cost);
        inputCost = root.findViewById(R.id.input_cost);
        inputCurrency = root.findViewById(R.id.input_cost_currency);
        inputCategory = root.findViewById(R.id.input_cost_category);
        layoutCost.setTypeface(Typeface.DEFAULT);
        ((TextInputLayout) root.findViewById(R.id.layout_cost_currency))
                .setTypeface(Typeface.DEFAULT);
        ((TextView) root.findViewById(R.id.text_cost_helper))
                .setText(context.getString(R.string.cost_helper, homeCurrency));
        inputCost.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layoutCost.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        bindDropdowns();
    }

    /** Binds the block found in {@code root}; the currency defaults to the trip's local one. */
    public static CostFields bind(View root, String homeCurrency, String defaultCurrency) {
        return new CostFields(root, homeCurrency, defaultCurrency);
    }

    private void bindDropdowns() {
        inputCurrency.setAdapter(new NoFilterAdapter(context, CurrencyRates.codes()));
        inputCurrency.setText(defaultCurrency, false);
        List<String> labels = new ArrayList<>();
        for (Category category : Category.values()) {
            labels.add(Labels.category(context, category));
        }
        inputCategory.setAdapter(new NoFilterAdapter(context, labels));
        inputCategory.setText(Labels.category(context, Category.ACTIVITIES), false);
    }

    /** Shows saved details; with none, the cost is blank, the currency local, the category Activities. */
    public void populate(@Nullable ItemDetails details) {
        if (details == null) {
            return;
        }
        inputCost.setText(details.getCost() > 0 ? AmountRules.format(details.getCost()) : "");
        String currency = details.getCostCurrency();
        inputCurrency.setText(currency == null || currency.isEmpty() ? defaultCurrency : currency,
                false);
        inputCategory.setText(Labels.category(context,
                BudgetMath.parseCategory(details.getCategory())), false);
    }

    /** Validates the cost (blank = 0); on failure shows the error and returns false. */
    public boolean validate() {
        boolean ok = AmountRules.parseOptional(text(inputCost)) != null;
        layoutCost.setError(ok ? null : context.getString(R.string.error_optional_amount_range));
        return ok;
    }

    public View costInput() {
        return inputCost;
    }

    /** The validated values merged onto {@code previous} (keeps its outdoor flag). */
    public ItemDetails toDetails(@Nullable ItemDetails previous) {
        Double cost = AmountRules.parseOptional(text(inputCost));
        return ItemDetails.withCost(previous, cost == null ? 0 : cost, currency(),
                BudgetMath.categoryName(category()));
    }

    private String currency() {
        String code = text(inputCurrency);
        return code.isEmpty() ? defaultCurrency : code;
    }

    private Category category() {
        String label = text(inputCategory);
        for (Category category : Category.values()) {
            if (Labels.category(context, category).equals(label)) {
                return category;
            }
        }
        return Category.ACTIVITIES;
    }

    private static String text(android.widget.TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }
}
