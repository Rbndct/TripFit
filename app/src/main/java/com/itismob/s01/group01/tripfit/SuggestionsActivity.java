package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.MockData;
import com.itismob.s01.group01.tripfit.prototype.MockPlace;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Budget-aware nearby food and activities. Prices convert to the home currency, and the
 * "Fits my budget" chip hides anything above the category's remaining budget.
 */
public class SuggestionsActivity extends ScreenActivity {

    public static final String EXTRA_DAY = "extra_day";
    private static final int TAB_FOOD = 0;
    private static final String STATE_TAB = "state_tab";
    private static final String STATE_ADDED = "state_added";

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private TripRepository repository;
    private TabLayout tabs;
    private Chip fitsBudget;
    private int targetDay;
    /** Names already added this session, so a second tap cannot create a duplicate stop. */
    private final Set<String> added = new HashSet<>();
    /** Names whose save is in flight; taps on them are ignored until the write returns. */
    private final Set<String> adding = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_suggestions, R.string.suggestions_title);
        repository = ((TripFitApp) getApplication()).getTripRepository();
        targetDay = getIntent().getIntExtra(EXTRA_DAY, 1);

        ((RecyclerView) findViewById(R.id.list_suggestions)).setAdapter(adapter);
        tabs = findViewById(R.id.tabs);
        fitsBudget = findViewById(R.id.chip_fits_budget);
        restoreState(savedInstanceState);
        ((TextView) findViewById(R.id.text_target_day))
                .setText(getString(R.string.suggestions_target_day, targetDay));
        findViewById(R.id.button_show_all).setOnClickListener(v -> fitsBudget.setChecked(false));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                render();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
        fitsBudget.setOnCheckedChangeListener((chip, checked) -> render());
        render();
    }

    private void restoreState(@Nullable Bundle state) {
        if (state == null) {
            return;
        }
        ArrayList<String> saved = state.getStringArrayList(STATE_ADDED);
        if (saved != null) {
            added.addAll(saved);
        }
        TabLayout.Tab tab = tabs.getTabAt(state.getInt(STATE_TAB, TAB_FOOD));
        if (tab != null) {
            tab.select();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, tabs.getSelectedTabPosition());
        outState.putStringArrayList(STATE_ADDED, new ArrayList<>(added));
    }

    private void render() {
        boolean food = tabs.getSelectedTabPosition() == TAB_FOOD;
        Category category = food ? Category.FOOD : Category.ACTIVITIES;
        BudgetSettings budget = store.budget(tripId);
        String home = budget.getHomeCurrency();
        String local = budget.getLocalCurrency();
        double limit = budget.limitFor(category);
        double spent = BudgetMath.spentByCategory(store.expenses(tripId)).get(category);
        double remaining = BudgetMath.remaining(limit, spent);

        TextView hint = findViewById(R.id.text_budget_hint);
        hint.setVisibility(limit > 0 ? View.VISIBLE : View.GONE);
        hint.setText(budgetHint(category, limit, remaining, home));

        List<UiRow> rows = new ArrayList<>();
        for (MockPlace place : food ? MockData.food() : MockData.activities()) {
            double priceHome = CurrencyRates.convert(place.getPriceLocal(), local, home);
            boolean fits = limit <= 0 || priceHome <= remaining;
            if (fitsBudget.isChecked() && !fits) {
                continue;
            }
            rows.add(toRow(place, priceHome, fits, home, local));
        }
        adapter.submit(rows);
        findViewById(R.id.group_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Remaining-budget sentence; the amount is data, so only it is set in monospace. */
    private CharSequence budgetHint(Category category, double limit, double remaining, String home) {
        if (limit <= 0) {
            return "";
        }
        String label = Labels.category(this, category);
        if (remaining <= 0) {
            return getString(R.string.suggestions_budget_used_up, label);
        }
        String amount = CurrencyRates.format(remaining, home);
        SpannableString text = new SpannableString(
                getString(R.string.suggestions_budget_hint, amount, label));
        int start = text.toString().indexOf(amount);
        if (start >= 0) {
            text.setSpan(new TypefaceSpan("monospace"), start, start + amount.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return text;
    }

    private UiRow toRow(MockPlace place, double priceHome, boolean fits, String home, String local) {
        String name = place.getName();
        boolean isAdded = added.contains(name);
        boolean isAdding = adding.contains(name);
        String subtitle = getString(fits ? R.string.suggestion_subtitle
                        : R.string.suggestion_subtitle_over, place.getKind(),
                place.getRating(), place.getDistanceKm(),
                CurrencyRates.format(place.getPriceLocal(), local));
        String label = getString(isAdded ? R.string.action_added
                : isAdding ? R.string.discover_adding : R.string.action_add);
        String actionDesc = isAdded ? getString(R.string.suggestion_added, name, targetDay)
                : getString(R.string.suggestion_add_cd, name, targetDay);
        Runnable onAdd = isAdded || isAdding ? null : () -> addToItinerary(place);
        return UiRow.withAction(name, subtitle, CurrencyRates.format(priceHome, home), label, onAdd)
                .withAccessibility(actionDesc, null)
                .withSelected(isAdded);
    }

    private void addToItinerary(MockPlace place) {
        String name = place.getName();
        if (added.contains(name) || !adding.add(name)) {
            return;
        }
        render();
        repository.addItineraryItem(tripId, name, targetDay, null, null, false, result -> {
            adding.remove(name);
            if (result.isSuccess()) {
                added.add(name);
                toast(getString(R.string.suggestion_added, name, targetDay));
            } else {
                toast(getString(R.string.suggestion_add_failed, name));
            }
            render();
        });
    }
}
