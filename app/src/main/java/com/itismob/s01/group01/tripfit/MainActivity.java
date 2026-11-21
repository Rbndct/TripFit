package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshot;
import com.itismob.s01.group01.tripfit.prototype.TripDates;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.prototype.WeatherPlanning;
import com.itismob.s01.group01.tripfit.prototype.WeatherPlanning.Condition;
import com.itismob.s01.group01.tripfit.ui.ItineraryAdapter;
import com.itismob.s01.group01.tripfit.ui.ItineraryRow;
import com.itismob.s01.group01.tripfit.ui.LegSettingsDialog;
import com.itismob.s01.group01.tripfit.viewmodel.ItineraryViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Itinerary hub for a single trip, loaded from Room via {@link ItineraryViewModel}. Days render
 * as a tab strip in the header; the list below shows only the selected day's stops. The toolbar
 * and its overflow menu are the entry points to every other trip feature.
 */
public class MainActivity extends AppCompatActivity implements ItineraryAdapter.Listener {

    private static final String KEY_SELECTED_DAY = "selected_day";
    private static final String LEG_DIALOG_TAG = "leg_settings";

    private final ItineraryAdapter adapter = new ItineraryAdapter(this);
    private final PrototypeStore store = PrototypeStore.get();
    private List<ItineraryItemWithPlace> allItems = new ArrayList<>();
    private int selectedDay = 1;
    private int tripDays = 1;
    private String tripId;
    private TripRepository repository;
    private LinearLayout dayTabContainer;
    private TextView daySummary;
    private TextView dayHeading;
    private TextView weatherBanner;
    private View emptyState;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        applyShellSystemBars();
        View appBar = findViewById(R.id.appbar_itinerary);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // The navy header runs under the status bar, so it absorbs the top inset itself.
            appBar.setPadding(0, systemBars.top, 0, 0);
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        tripId = getIntent().getStringExtra(HomeActivity.EXTRA_TRIP_ID);
        if (tripId == null || tripId.trim().isEmpty()) {
            finish(); // an itinerary needs a trip: close instead of rendering defaults
            return;
        }
        dayTabContainer = findViewById(R.id.day_tab_container);
        daySummary = findViewById(R.id.text_day_summary);
        dayHeading = findViewById(R.id.text_day_heading);
        if (savedInstanceState != null) {
            selectedDay = savedInstanceState.getInt(KEY_SELECTED_DAY, 1);
        }
        weatherBanner = findViewById(R.id.banner_weather);
        emptyState = findViewById(R.id.empty_state);

        TripFitApp app = (TripFitApp) getApplication();
        repository = app.getTripRepository();
        ItineraryViewModel viewModel = new ViewModelProvider(this,
                new ItineraryViewModel.Factory(repository, tripId))
                .get(ItineraryViewModel.class);

        setUpToolbar();

        TextView tripTitle = findViewById(R.id.text_trip_title);
        repository.observeTrip(tripId).observe(this, trip -> {
            if (trip == null) {
                finish(); // unknown trip id
            } else {
                tripTitle.setText(trip.getName());
            }
        });
        bindSubtitle();

        recyclerView = findViewById(R.id.recycler_itinerary);
        recyclerView.setAdapter(adapter);
        viewModel.getItems().observe(this, items -> {
            allItems = items;
            render();
        });

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_destination);
        fab.setOnClickListener(v -> openWithDay(AddDestinationActivity.class));
        findViewById(R.id.button_empty_discover).setOnClickListener(v -> openWithDay(DiscoverActivity.class));
        weatherBanner.setOnClickListener(v -> openWithDay(WeatherActivity.class));
    }

    /** Navy status bar with light icons, matching Home and every ScreenActivity. */
    private void applyShellSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.ticket_carrier_navy));
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(false);
    }

    private void setUpToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_itinerary);
        toolbar.setOverflowIcon(AppCompatResources.getDrawable(this, R.drawable.ic_more_vert_24));
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        toolbar.setOnMenuItemClickListener(this::onMenuItem);
    }

    private boolean onMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_optimize) {
            open(OptimizeActivity.class);
        } else if (id == R.id.action_budget) {
            open(BudgetActivity.class);
        } else if (id == R.id.action_suggestions) {
            openWithDay(SuggestionsActivity.class);
        } else if (id == R.id.action_trip_settings) {
            open(NewTripActivity.class);
        } else if (id == R.id.action_discover) {
            openWithDay(DiscoverActivity.class);
        } else if (id == R.id.action_weather) {
            openWithDay(WeatherActivity.class);
        } else if (id == R.id.action_calendar) {
            open(CalendarSyncActivity.class);
        } else if (id == R.id.action_summary) {
            open(SummaryActivity.class);
        } else if (id == R.id.action_group) {
            open(GroupActivity.class);
        } else if (id == R.id.action_journal) {
            open(JournalActivity.class);
        } else {
            return false;
        }
        return true;
    }

    private void open(Class<?> screen) {
        Intent intent = new Intent(this, screen);
        intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
        startActivity(intent);
    }

    private void bindSubtitle() {
        TripMeta meta = store.meta(tripId);
        TextView subtitle = findViewById(R.id.text_trip_subtitle);
        String dates = meta.getDates();
        BudgetSettings budget = store.budget(tripId);
        double total = budget.tripTotal(meta.getDays());
        String budgetText = CurrencyRates.format(total, budget.getHomeCurrency());
        String city = meta.getCity();
        if (total <= 0) {
            subtitle.setText(dates.isEmpty() ? city : city + " · " + dates);
        } else if (dates.isEmpty()) {
            subtitle.setText(getString(R.string.itinerary_subtitle_budget, city, budgetText));
        } else {
            subtitle.setText(getString(R.string.itinerary_subtitle_full, city, dates, budgetText));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            bindSubtitle(); // Trip settings may have changed the city or dates
            render();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_DAY, selectedDay);
    }

    private void openWithDay(Class<?> screen) {
        Intent intent = new Intent(this, screen);
        intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
        intent.putExtra(AddDestinationActivity.EXTRA_DAY, selectedDay);
        startActivity(intent);
    }

    // ---- stop / leg taps ----

    @Override
    public void onStopClick(ItineraryRow row) {
        Intent intent = new Intent(this, StopDetailActivity.class);
        intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
        intent.putExtra(StopDetailActivity.EXTRA_ITEM_ID, row.getId());
        startActivity(intent);
    }

    @Override
    public void onLegClick(ItineraryRow destination) {
        if (getSupportFragmentManager().findFragmentByTag(LEG_DIALOG_TAG) != null) {
            return; // a rotation-restored dialog is already showing
        }
        LegSettingsDialog.newInstance(destination.getId(), destination.getName(),
                destination.getExplicitMode(), destination.getExplicitBuffer(),
                PrototypeStore.get().getBufferMin())
                .show(getSupportFragmentManager(), LEG_DIALOG_TAG);
    }

    // ---- weather-aware rescheduling ----

    private void renderWeatherBanner() {
        Condition condition = WeatherPlanning.conditionAt(store.weather(tripId), selectedDay);
        int count = WeatherPlanning.conflicts(allItems, selectedDay, condition, store).size();
        weatherBanner.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) {
            int banner = condition == Condition.STORM ? R.plurals.weather_banner_storm
                    : R.plurals.weather_banner_rain;
            weatherBanner.setText(getResources().getQuantityString(banner, count, selectedDay, count));
        }
    }

    // ---- rendering ----

    /** Rebuilds the tabs, heading, summary and cards from the latest rows and trip details. */
    private void render() {
        TripMeta meta = store.meta(tripId);
        TripSnapshot trip = TripSnapshots.from(tripId, meta, allItems, store);
        tripDays = trip.days;
        selectedDay = TripSnapshots.clampDay(selectedDay, tripDays);
        renderDayTabs();
        renderSelectedDayStops(trip, meta);
    }

    private void renderDayTabs() {
        dayTabContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int day = 1; day <= tripDays; day++) {
            final int tabDay = day;
            TextView tab = (TextView) inflater.inflate(R.layout.item_day_tab, dayTabContainer, false);
            tab.setText(getString(R.string.day_label, day));
            bindTabSelectedState(tab, day == selectedDay);
            tab.setOnClickListener(v -> {
                selectedDay = tabDay;
                render();
                recyclerView.scrollToPosition(0);
            });
            dayTabContainer.addView(tab);
        }
    }

    private void bindTabSelectedState(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected
                ? R.drawable.bg_day_tab_selected : R.drawable.bg_day_tab_unselected);
        tab.setTextColor(getColor(selected
                ? R.color.ticket_carrier_navy : R.color.on_ticket_carrier));
        ViewCompat.setStateDescription(tab, selected ? getString(R.string.state_selected) : null);
    }

    private void renderSelectedDayStops(TripSnapshot trip, TripMeta meta) {
        List<ItineraryRow> rows = ItineraryRow.build(trip, selectedDay);
        adapter.submitRows(rows);
        emptyState.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        String date = TripDates.dayDateLabel(meta.getStartDateIso(), selectedDay);
        dayHeading.setText(date.isEmpty() ? getString(R.string.day_label, selectedDay)
                : getString(R.string.day_heading_dated, selectedDay, date));
        daySummary.setText(getResources().getQuantityString(R.plurals.day_summary, rows.size(),
                rows.size(), ItineraryRow.stayMinutes(rows), ItineraryRow.travelMinutes(rows)));
        renderWeatherBanner();
    }
}
