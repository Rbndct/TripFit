package com.itismob.s01.group01.tripfit.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
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

    private static final String KEY_TITLE = "expense_title";
    private static final String KEY_AMOUNT = "expense_amount";
    private static final String KEY_CATEGORY = "expense_category";

    private AddExpenseDialog() { }

    public static AlertDialog show(AppCompatActivity activity, String tripId, Runnable onAdded) {
        return show(activity, tripId, null, onAdded);
    }

    /**
     * Shows the dialog, optionally pre-filled from a draft produced by {@link #draftOf}, so a
     * half-typed expense survives a rotation.
     */
    public static AlertDialog show(AppCompatActivity activity, String tripId,
                                   @Nullable Bundle draft, Runnable onAdded) {
        Form form = new Form(activity, tripId);
        if (draft != null) {
            form.restore(draft);
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.action_add_expense)
                .setView(form.view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_add, null)
                .create();
        Runnable submit = () -> {
            if (form.submit()) {
                dialog.dismiss();
                onAdded.run();
            }
        };
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> submit.run());
            form.inputAmount.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId != EditorInfo.IME_ACTION_DONE) {
                    return false;
                }
                submit.run();
                return true;
            });
        });
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
        return dialog;
    }

    /** Captures what has been typed into a shown dialog, or null if it is not one of ours. */
    @Nullable
    public static Bundle draftOf(AlertDialog dialog) {
        TextInputEditText title = dialog.findViewById(R.id.input_expense_title);
        TextInputEditText amount = dialog.findViewById(R.id.input_expense_amount);
        MaterialAutoCompleteTextView category = dialog.findViewById(R.id.input_expense_category);
        if (title == null || amount == null || category == null) {
            return null;
        }
        Bundle draft = new Bundle();
        draft.putString(KEY_TITLE, String.valueOf(title.getText()));
        draft.putString(KEY_AMOUNT, String.valueOf(amount.getText()));
        draft.putInt(KEY_CATEGORY, Form.indexOfLabel(dialog.getContext(),
                String.valueOf(category.getText())));
        return draft;
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

    /** The dialog's views plus the state needed to validate and save. */
    private static final class Form {
        final AppCompatActivity activity;
        final PrototypeStore store = PrototypeStore.get();
        final String tripId;
        final BudgetSettings budget;
        final View view;
        final TextInputLayout layoutTitle;
        final TextInputLayout layoutAmount;
        final TextInputEditText inputTitle;
        final TextInputEditText inputAmount;
        final TextView preview;
        final MaterialAutoCompleteTextView inputCategory;
        final Category[] categories = Category.values();
        final String[] labels = new String[categories.length];
        int chosen = 0;

        Form(AppCompatActivity activity, String tripId) {
            this.activity = activity;
            this.tripId = tripId;
            this.budget = store.budget(tripId);
            view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_expense, null);
            layoutTitle = view.findViewById(R.id.layout_expense_title);
            layoutAmount = view.findViewById(R.id.layout_expense_amount);
            inputTitle = view.findViewById(R.id.input_expense_title);
            inputAmount = view.findViewById(R.id.input_expense_amount);
            preview = view.findViewById(R.id.text_conversion_preview);
            inputCategory = view.findViewById(R.id.input_expense_category);
            layoutAmount.setHint(activity.getString(R.string.field_amount,
                    budget.getLocalCurrency()));
            bindCategory();
            inputTitle.addTextChangedListener(new SimpleWatcher(s -> layoutTitle.setError(null)));
            inputAmount.addTextChangedListener(new SimpleWatcher(s -> {
                layoutAmount.setError(null);
                updatePreview(s);
            }));
        }

        private void bindCategory() {
            for (int i = 0; i < categories.length; i++) {
                labels[i] = Labels.category(activity, categories[i]);
            }
            inputCategory.setAdapter(new ArrayAdapter<>(activity,
                    android.R.layout.simple_list_item_1, labels));
            inputCategory.setText(labels[chosen], false);
            inputCategory.setOnItemClickListener((parent, v, position, id) -> chosen = position);
        }

        void restore(Bundle draft) {
            chosen = Math.max(0, Math.min(categories.length - 1, draft.getInt(KEY_CATEGORY, 0)));
            inputCategory.setText(labels[chosen], false);
            inputTitle.setText(draft.getString(KEY_TITLE, ""));
            inputAmount.setText(draft.getString(KEY_AMOUNT, ""));
        }

        static int indexOfLabel(android.content.Context context, String label) {
            Category[] all = Category.values();
            for (int i = 0; i < all.length; i++) {
                if (Labels.category(context, all[i]).equals(label)) {
                    return i;
                }
            }
            return 0;
        }

        private void updatePreview(CharSequence text) {
            Double amount = parseAmount(text.toString());
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

        /** Validates and saves; on failure shows errors and focuses the first invalid field. */
        boolean submit() {
            String title = inputTitle.getText() == null
                    ? "" : inputTitle.getText().toString().trim();
            Double amount = parseAmount(inputAmount.getText() == null
                    ? "" : inputAmount.getText().toString());
            boolean titleMissing = TextUtils.isEmpty(title);
            layoutTitle.setError(titleMissing
                    ? activity.getString(R.string.error_title_required) : null);
            layoutAmount.setError(amount == null
                    ? activity.getString(R.string.error_amount_required) : null);
            if (titleMissing) {
                inputTitle.requestFocus();
                return false;
            }
            if (amount == null) {
                inputAmount.requestFocus();
                return false;
            }
            store.addExpense(tripId, title, categories[chosen], amount, budget.getLocalCurrency());
            return true;
        }
    }

    /** TextWatcher that only cares about the new text. */
    private static final class SimpleWatcher implements TextWatcher {
        private final java.util.function.Consumer<CharSequence> onChanged;

        SimpleWatcher(java.util.function.Consumer<CharSequence> onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChanged.accept(s);
        }

        @Override
        public void afterTextChanged(Editable s) { }
    }
}
