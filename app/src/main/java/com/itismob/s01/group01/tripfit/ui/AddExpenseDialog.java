package com.itismob.s01.group01.tripfit.ui;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.CurrencyMock;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;

/** Dialog for logging an expense in the trip's local currency, with a live home-currency preview. */
public final class AddExpenseDialog {

    private AddExpenseDialog() { }

    public static void show(AppCompatActivity activity, String tripId, Runnable onAdded) {
        PrototypeStore store = PrototypeStore.get();
        BudgetSettings budget = store.budget(tripId);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_expense, null);

        TextInputLayout layoutTitle = view.findViewById(R.id.layout_expense_title);
        TextInputLayout layoutAmount = view.findViewById(R.id.layout_expense_amount);
        TextInputEditText inputTitle = view.findViewById(R.id.input_expense_title);
        TextInputEditText inputAmount = view.findViewById(R.id.input_expense_amount);
        TextView preview = view.findViewById(R.id.text_conversion_preview);
        MaterialAutoCompleteTextView inputCategory = view.findViewById(R.id.input_expense_category);

        layoutAmount.setHint(activity.getString(R.string.field_amount, budget.getLocalCurrency()));
        Category[] categories = Category.values();
        String[] labels = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            labels[i] = Labels.category(activity, categories[i]);
        }
        inputCategory.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_list_item_1, labels));
        Category[] chosen = {Category.FOOD};
        inputCategory.setText(labels[0], false);
        inputCategory.setOnItemClickListener((parent, v, position, id) ->
                chosen[0] = categories[position]);

        inputAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Double amount = parseAmount(s.toString());
                if (amount == null) {
                    preview.setVisibility(View.INVISIBLE);
                    return;
                }
                preview.setVisibility(View.VISIBLE);
                preview.setText(activity.getString(R.string.conversion_preview,
                        CurrencyMock.format(CurrencyMock.convert(amount,
                                budget.getLocalCurrency(), budget.getHomeCurrency()),
                                budget.getHomeCurrency())));
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.action_add_expense)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_add, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = inputTitle.getText() == null
                            ? "" : inputTitle.getText().toString().trim();
                    Double amount = parseAmount(inputAmount.getText() == null
                            ? "" : inputAmount.getText().toString());
                    layoutTitle.setError(TextUtils.isEmpty(title)
                            ? activity.getString(R.string.error_title_required) : null);
                    layoutAmount.setError(amount == null
                            ? activity.getString(R.string.error_amount_required) : null);
                    if (TextUtils.isEmpty(title) || amount == null) {
                        return;
                    }
                    store.addExpense(tripId, title, chosen[0], amount, budget.getLocalCurrency());
                    dialog.dismiss();
                    onAdded.run();
                }));
        dialog.show();
    }

    /** Parses a positive amount, returning null for blank, malformed, or non-positive input. */
    static Double parseAmount(String text) {
        try {
            double value = Double.parseDouble(text.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
