package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.AmountRules;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshot;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.Expense;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.AddExpenseDialog;
import com.itismob.s01.group01.tripfit.ui.BudgetLines;
import com.itismob.s01.group01.tripfit.ui.Keyboard;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.ui.NoFilterAdapter;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Running budget tracker. Logged expenses and planned stop estimates are shown side by side,
 * overall and per category, and are never added together. Also hosts the currency calculator.
 */
public class BudgetActivity extends ScreenActivity {

    private final PrototypeStore store = PrototypeStore.get();
    private static final String STATE_EXPENSE_DRAFT = "expense_draft";
    private static final String STATE_REMOVE_ID = "remove_expense_id";
    private static final String DEFAULT_CALC_AMOUNT = "1000";

    private final RowAdapter expenseAdapter = new RowAdapter();
    @Nullable
    private AlertDialog expenseDialog;
    @Nullable
    private AlertDialog removeDialog;
    @Nullable
    private String removeExpenseId;
    private List<ItineraryItemWithPlace> stops = new ArrayList<>();
    private boolean stopsLoaded;
    private TextInputLayout layoutCalcAmount;
    private TextInputEditText inputCalcAmount;
    private MaterialAutoCompleteTextView inputCalcCurrency;
    private final ActivityResultLauncher<Intent> budgetSetup = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    showBudgetSaved();
                }
            });

    /** Package-private so the screen test can trigger it without driving Budget setup. */
    void showBudgetSaved() {
        Snackbar.make(findViewById(R.id.budget_root), R.string.budget_saved,
                Snackbar.LENGTH_SHORT).setAnchorView(R.id.fab_add_expense).show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!setScreen(R.layout.activity_budget, R.string.budget_title)) {
            return;
        }
        ((RecyclerView) findViewById(R.id.list_expenses)).setAdapter(expenseAdapter);

        findViewById(R.id.button_edit_budget).setOnClickListener(v -> {
            Intent intent = new Intent(this, BudgetSetupActivity.class);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            budgetSetup.launch(intent);
        });
        findViewById(R.id.fab_add_expense).setOnClickListener(v -> showExpenseDialog(null));
        findViewById(R.id.button_find_cheap).setOnClickListener(v -> {
            Intent intent = new Intent(this, SuggestionsActivity.class);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        });
        bindCalculator();
        LiveData<List<ItineraryItemWithPlace>> items = ((TripFitApp) getApplication())
                .getTripRepository().observeItineraryItemsWithPlaceNames(tripId);
        items.observe(this, list -> {
            stops = list;
            stopsLoaded = true;
            render();
        });
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_EXPENSE_DRAFT)) {
            showExpenseDialog(savedInstanceState.getBundle(STATE_EXPENSE_DRAFT));
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_REMOVE_ID)) {
            confirmRemove(savedInstanceState.getString(STATE_REMOVE_ID));
        }
    }

    // ---- currency calculator ----

    private void bindCalculator() {
        layoutCalcAmount = findViewById(R.id.layout_calc_amount);
        inputCalcAmount = findViewById(R.id.input_calc_amount);
        inputCalcCurrency = findViewById(R.id.input_calc_currency);
        layoutCalcAmount.setTypeface(Typeface.DEFAULT);
        ((TextInputLayout) findViewById(R.id.layout_calc_currency)).setTypeface(Typeface.DEFAULT);
        inputCalcCurrency.setAdapter(new NoFilterAdapter(this, CurrencyRates.codes()));
        inputCalcCurrency.setText(store.budget(tripId).getLocalCurrency(), false);
        inputCalcAmount.setText(DEFAULT_CALC_AMOUNT);
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCalculator();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };
        inputCalcAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            Keyboard.hide(v);
            return true;
        });
        inputCalcAmount.addTextChangedListener(watcher);
        inputCalcCurrency.addTextChangedListener(watcher);
    }

    /** Any currency to the trip's home currency, labelled by where the rate comes from. */
    private void renderCalculator() {
        TextView result = findViewById(R.id.text_calc_result);
        String home = store.budget(tripId).getHomeCurrency();
        String from = String.valueOf(inputCalcCurrency.getText()).trim();
        Double amount = AmountRules.parseOptional(String.valueOf(inputCalcAmount.getText()));
        layoutCalcAmount.setError(amount == null ? getString(R.string.calc_error) : null);
        if (amount == null || from.isEmpty()) {
            result.setText("");
            return;
        }
        String original = CurrencyRates.format(amount, from);
        String converted = CurrencyRates.format(BudgetMath.convert(amount, from, home), home);
        result.setText(CurrencyRates.isLive()
                ? getString(R.string.calc_result_live, original, converted,
                        CurrencyRates.asOfDate())
                : getString(R.string.calc_result_sample, original, converted));
    }

    /** Names the rate source (sample/reference or live as of a date) and the unpriced-leg caveat. */
    private void renderRatesNotice() {
        ((TextView) findViewById(R.id.text_rates_notice)).setText(CurrencyRates.isLive()
                ? getString(R.string.budget_rates_notice_live, CurrencyRates.asOfDate())
                : getString(R.string.budget_rates_notice_sample));
    }

    /** Opens Add Expense (once at a time), optionally restoring a draft after rotation. */
    private void showExpenseDialog(@Nullable Bundle draft) {
        if (expenseDialog != null) {
            return;
        }
        expenseDialog = AddExpenseDialog.show(this, tripId, draft, () -> {
            // The root is a CoordinatorLayout, so the anchor is re-resolved on every layout pass and
            // the snackbar stays above the FAB even while the keyboard is still hiding. Anchored to
            // the window content view it kept a stale margin and floated mid-screen.
            Snackbar.make(findViewById(R.id.budget_root), R.string.expense_added,
                    Snackbar.LENGTH_SHORT).setAnchorView(R.id.fab_add_expense).show();
            render();
        });
        expenseDialog.setOnDismissListener(d -> expenseDialog = null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (expenseDialog != null && expenseDialog.isShowing()) {
            outState.putBundle(STATE_EXPENSE_DRAFT, AddExpenseDialog.draftOf(expenseDialog));
        }
        if (removeDialog != null && removeDialog.isShowing() && removeExpenseId != null) {
            outState.putString(STATE_REMOVE_ID, removeExpenseId);
        }
    }

    @Override
    protected void onDestroy() {
        if (expenseDialog != null) {
            expenseDialog.dismiss();
        }
        if (removeDialog != null) {
            removeDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        BudgetSettings budget = store.budget(tripId);
        List<Expense> expenses = store.expenses(tripId);
        String home = budget.getHomeCurrency();
        int days = store.meta(tripId).getDays();
        TripSnapshot trip = TripSnapshots.from(tripId, store.meta(tripId), stops, store);
        double total = budget.tripTotal(days);
        double spent = BudgetMath.totalSpent(expenses);

        ((TextView) findViewById(R.id.text_spent)).setText(CurrencyRates.format(spent, home));
        ((TextView) findViewById(R.id.text_of_total)).setText(total > 0
                ? getString(R.string.budget_of_total, CurrencyRates.format(total, home))
                : getString(R.string.budget_no_total));
        bindProgress(findViewById(R.id.progress_total), spent, total);
        renderRemaining(total, spent, home);
        findViewById(R.id.card_planned).setVisibility(stopsLoaded ? View.VISIBLE : View.GONE);
        if (stopsLoaded) {
            renderPlanned(trip, total, home);
        }
        renderUnknownCurrencies(trip, expenses);

        renderCategories(budget, days, expenses, trip);
        renderExpenses(expenses, home);
        renderCalculator();
        renderRatesNotice();
    }

    /** Flags stops and expenses whose currency is not in the rates table (counted 1:1). */
    private void renderUnknownCurrencies(TripSnapshot trip, List<Expense> expenses) {
        java.util.Set<String> unknown = BudgetMath.unknownCurrencies(trip, expenses);
        TextView note = findViewById(R.id.text_unknown_currency);
        note.setVisibility(unknown.isEmpty() ? View.GONE : View.VISIBLE);
        if (!unknown.isEmpty()) {
            note.setText(getString(R.string.budget_unknown_currency,
                    android.text.TextUtils.join(", ", unknown)));
        }
    }

    /** Planned stop estimates, kept apart from the logged total, with an over-budget warning. */
    private void renderPlanned(TripSnapshot trip, double total, String home) {
        double planned = BudgetMath.plannedTotal(trip);
        boolean over = BudgetMath.isOver(planned, total);
        ((TextView) findViewById(R.id.text_planned)).setText(CurrencyRates.format(planned, home));
        TextView of = findViewById(R.id.text_planned_of);
        of.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        of.setText(getString(R.string.budget_planned_of, CurrencyRates.format(total, home)));
        bindProgress(findViewById(R.id.progress_planned), planned, total);
        findViewById(R.id.row_planned_warning).setVisibility(over ? View.VISIBLE : View.GONE);
        if (over) {
            ((TextView) findViewById(R.id.text_planned_warning)).setText(
                    getString(R.string.budget_planned_over_warning,
                            CurrencyRates.format(planned, home),
                            CurrencyRates.format(planned - total, home),
                            CurrencyRates.format(total, home)));
        }
        ((TextView) findViewById(R.id.text_planned_note)).setText(planned > 0
                ? R.string.budget_planned_note : R.string.budget_planned_none);
    }

    /** "₱X left" / "₱X over budget"; hidden when no budget is set, so it never reads "₱-X left". */
    private void renderRemaining(double total, double spent, String home) {
        TextView remaining = findViewById(R.id.text_remaining);
        if (total <= 0) {
            remaining.setVisibility(View.GONE);
            return;
        }
        remaining.setVisibility(View.VISIBLE);
        boolean over = BudgetMath.isOver(spent, total);
        double left = BudgetMath.remaining(total, spent);
        remaining.setText(over
                ? getString(R.string.budget_over, CurrencyRates.format(-left, home))
                : getString(R.string.budget_left, CurrencyRates.format(left, home)));
        remaining.setTextColor(over ? getColor(R.color.status_over)
                : getColor(R.color.md_on_surface));
    }

    private void bindProgress(LinearProgressIndicator bar, double spent, double limit) {
        bar.setProgress(BudgetMath.percent(spent, limit));
        bar.setIndicatorColor(BudgetMath.isOver(spent, limit)
                ? getColor(R.color.status_over)
                : MaterialColors.getColor(bar, androidx.appcompat.R.attr.colorPrimary));
    }

    private void renderCategories(BudgetSettings budget, int days, List<Expense> expenses,
                                  TripSnapshot trip) {
        LinearLayout container = findViewById(R.id.category_container);
        container.removeAllViews();
        Map<Category, Double> byCategory = BudgetMath.spentByCategory(expenses);
        Map<Category, Double> plannedByCategory = BudgetMath.plannedByCategory(trip);
        String home = budget.getHomeCurrency();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Category category : Category.values()) {
            double spent = byCategory.get(category);
            double limit = budget.limitFor(category, days);
            double planned = plannedByCategory.get(category);
            boolean over = BudgetMath.isOver(spent, limit);
            View row = inflater.inflate(R.layout.item_category, container, false);
            ((TextView) row.findViewById(R.id.text_category_name))
                    .setText(Labels.category(this, category));
            row.findViewById(R.id.badge_over).setVisibility(over ? View.VISIBLE : View.GONE);
            String amountText = BudgetLines.categoryLine(spent, limit, home,
                    getString(R.string.category_remaining), getString(R.string.category_over),
                    getString(R.string.category_no_limit));
            ((TextView) row.findViewById(R.id.text_category_amount)).setText(amountText);
            String plannedText = getString(R.string.category_planned,
                    CurrencyRates.format(planned, home));
            TextView plannedView = row.findViewById(R.id.text_category_planned);
            plannedView.setVisibility(planned > 0 ? View.VISIBLE : View.GONE);
            plannedView.setText(plannedText);
            // One TalkBack stop per category: name, over-budget state, and amounts.
            ViewCompat.setScreenReaderFocusable(row, true);
            String spoken = planned > 0
                    ? getString(R.string.category_planned_cd, amountText, plannedText) : amountText;
            row.setContentDescription(getString(R.string.category_row_cd,
                    Labels.category(this, category), spoken));
            bindProgress(row.findViewById(R.id.progress_category), spent, limit);
            container.addView(row);
        }
    }

    private void renderExpenses(List<Expense> expenses, String home) {
        List<UiRow> rows = new ArrayList<>();
        for (Expense expense : expenses) {
            UiRow row = UiRow.withAction(expense.getTitle(),
                    getString(R.string.expense_subtitle,
                            Labels.category(this, expense.getCategory()),
                            CurrencyRates.format(expense.getAmountLocal(),
                                    expense.getLocalCurrency())),
                    CurrencyRates.format(expense.getAmountHome(), home),
                    getString(R.string.expense_remove), () -> confirmRemove(expense.getId()));
            rows.add(row.withAccessibility(
                    getString(R.string.expense_remove_cd, expense.getTitle()), null));
        }
        expenseAdapter.submit(rows);
        findViewById(R.id.text_no_expenses).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---- remove expense (confirmation survives rotation) ----

    private void confirmRemove(@Nullable String expenseId) {
        Expense target = findExpense(expenseId);
        if (target == null || removeDialog != null) {
            return;
        }
        removeExpenseId = expenseId;
        removeDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.expense_remove_title)
                .setMessage(getString(R.string.expense_remove_message, target.getTitle(),
                        CurrencyRates.format(target.getAmountLocal(), target.getLocalCurrency())))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.expense_remove, (dialog, which) -> removeExpense())
                .show();
        removeDialog.setOnDismissListener(d -> removeDialog = null);
    }

    @Nullable
    private Expense findExpense(@Nullable String expenseId) {
        for (Expense expense : store.expenses(tripId)) {
            if (expense.getId().equals(expenseId)) {
                return expense;
            }
        }
        return null;
    }

    private void removeExpense() {
        if (removeExpenseId != null && store.removeExpense(tripId, removeExpenseId)) {
            Snackbar.make(findViewById(R.id.budget_root), R.string.expense_removed,
                    Snackbar.LENGTH_SHORT).setAnchorView(R.id.fab_add_expense).show();
            render();
        }
        removeExpenseId = null;
    }
}
