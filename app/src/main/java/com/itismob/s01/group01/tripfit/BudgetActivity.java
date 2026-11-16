package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.CurrencyMock;
import com.itismob.s01.group01.tripfit.prototype.Expense;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.ui.AddExpenseDialog;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Running budget tracker: overall spend vs budget, per-category progress, and logged expenses. */
public class BudgetActivity extends ScreenActivity {

    private final PrototypeStore store = PrototypeStore.get();
    private static final String STATE_EXPENSE_DRAFT = "expense_draft";

    private final RowAdapter expenseAdapter = new RowAdapter();
    @Nullable
    private AlertDialog expenseDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_budget, R.string.budget_title);
        ((RecyclerView) findViewById(R.id.list_expenses)).setAdapter(expenseAdapter);

        findViewById(R.id.button_edit_budget).setOnClickListener(v -> {
            Intent intent = new Intent(this, BudgetSetupActivity.class);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        });
        findViewById(R.id.fab_add_expense).setOnClickListener(v -> showExpenseDialog(null));
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_EXPENSE_DRAFT)) {
            showExpenseDialog(savedInstanceState.getBundle(STATE_EXPENSE_DRAFT));
        }
    }

    /** Opens Add Expense (once at a time), optionally restoring a draft after rotation. */
    private void showExpenseDialog(@Nullable Bundle draft) {
        if (expenseDialog != null) {
            return;
        }
        expenseDialog = AddExpenseDialog.show(this, tripId, draft, () -> {
            // Anchor above the FAB so the confirmation never covers the primary action.
            Snackbar.make(findViewById(android.R.id.content), R.string.expense_added,
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
    }

    @Override
    protected void onDestroy() {
        if (expenseDialog != null) {
            expenseDialog.dismiss();
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
        double total = budget.tripTotal(store.meta(tripId).getDays());
        double spent = BudgetMath.totalSpent(expenses);

        ((TextView) findViewById(R.id.text_spent)).setText(CurrencyMock.format(spent, home));
        ((TextView) findViewById(R.id.text_of_total)).setText(total > 0
                ? getString(R.string.budget_of_total, CurrencyMock.format(total, home))
                : getString(R.string.budget_no_total));
        bindProgress(findViewById(R.id.progress_total), spent, total);
        renderRemaining(total, spent, home);

        renderCategories(budget, expenses);
        renderExpenses(expenses, home);
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
                ? getString(R.string.budget_over, CurrencyMock.format(-left, home))
                : getString(R.string.budget_left, CurrencyMock.format(left, home)));
        remaining.setTextColor(over ? getColor(R.color.status_over)
                : getColor(R.color.md_on_surface));
    }

    private void bindProgress(LinearProgressIndicator bar, double spent, double limit) {
        bar.setProgress(BudgetMath.percent(spent, limit));
        bar.setIndicatorColor(BudgetMath.isOver(spent, limit)
                ? getColor(R.color.status_over)
                : MaterialColors.getColor(bar, androidx.appcompat.R.attr.colorPrimary));
    }

    private void renderCategories(BudgetSettings budget, List<Expense> expenses) {
        LinearLayout container = findViewById(R.id.category_container);
        container.removeAllViews();
        Map<Category, Double> byCategory = BudgetMath.spentByCategory(expenses);
        String home = budget.getHomeCurrency();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Category category : Category.values()) {
            double spent = byCategory.get(category);
            double limit = budget.limitFor(category);
            boolean over = BudgetMath.isOver(spent, limit);
            View row = inflater.inflate(R.layout.item_category, container, false);
            ((TextView) row.findViewById(R.id.text_category_name))
                    .setText(Labels.category(this, category));
            row.findViewById(R.id.badge_over).setVisibility(over ? View.VISIBLE : View.GONE);
            String amountText = limit > 0
                    ? getString(R.string.category_progress, CurrencyMock.format(spent, home),
                            CurrencyMock.format(limit, home))
                    : getString(R.string.category_no_limit, CurrencyMock.format(spent, home));
            ((TextView) row.findViewById(R.id.text_category_amount)).setText(amountText);
            // One TalkBack stop per category: name, over-budget state, and amounts.
            ViewCompat.setScreenReaderFocusable(row, true);
            row.setContentDescription(getString(over ? R.string.category_row_cd_over
                    : R.string.category_row_cd, Labels.category(this, category), amountText));
            bindProgress(row.findViewById(R.id.progress_category), spent, limit);
            container.addView(row);
        }
    }

    private void renderExpenses(List<Expense> expenses, String home) {
        List<UiRow> rows = new ArrayList<>();
        for (Expense expense : expenses) {
            rows.add(UiRow.withTrailing(expense.getTitle(),
                    getString(R.string.expense_subtitle,
                            Labels.category(this, expense.getCategory()),
                            CurrencyMock.format(expense.getAmountLocal(),
                                    expense.getLocalCurrency())),
                    CurrencyMock.format(expense.getAmountHome(), home)));
        }
        expenseAdapter.submit(rows);
        findViewById(R.id.text_no_expenses).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
