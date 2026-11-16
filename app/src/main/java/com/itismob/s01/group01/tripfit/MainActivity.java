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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.prototype.MockData;
import com.itismob.s01.group01.tripfit.prototype.MockPlace;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TravelMode;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.ui.ItineraryAdapter;
import com.itismob.s01.group01.tripfit.ui.ItineraryRow;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.viewmodel.ItineraryViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Itinerary hub for a single trip, loaded from Room via {@link ItineraryViewModel}. Days render
 * as a tab strip in the header; the list below shows only the selected day's stops. The toolbar
 * and its overflow menu are the entry points to every other trip feature.
 */
public class MainActivity extends AppCompatActivity implements ItineraryAdapter.Listener {

    private final ItineraryAdapter adapter = new ItineraryAdapter(this);
    private final PrototypeStore store = PrototypeStore.get();
    private List<ItineraryItemWithPlace> allItems = new ArrayList<>();
    private Integer selectedDay;
    private String tripId;
    private TripRepository repository;
    private LinearLayout dayTabContainer;
    private TextView daySummary;
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
            if (selectedDay == null || !distinctDays(items).contains(selectedDay)) {
                selectedDay = distinctDays(items).isEmpty() ? 1 : distinctDays(items).first();
            }
            renderDayTabs();
            renderSelectedDayStops();
        });

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_destination);
        fab.setOnClickListener(v -> open(AddDestinationActivity.class));
        findViewById(R.id.button_empty_discover).setOnClickListener(v -> open(DiscoverActivity.class));
        weatherBanner.setOnClickListener(v -> showWeatherDialog());
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
            open(SuggestionsActivity.class);
        } else if (id == R.id.action_trip_settings) {
            open(NewTripActivity.class);
        } else if (id == R.id.action_discover) {
            open(DiscoverActivity.class);
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
        subtitle.setText(dates.isEmpty() ? meta.getCity() : meta.getCity() + " · " + dates);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            bindSubtitle(); // Trip settings may have changed the city or dates
        }
    }

    // ---- stop / leg taps ----

    @Override
    public void onStopClick(ItineraryRow row) {
        ItineraryItemEntity item = row.getItem();
        Intent intent = new Intent(this, StopDetailActivity.class);
        intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
        intent.putExtra(StopDetailActivity.EXTRA_ITEM_ID, item.getId());
        intent.putExtra(StopDetailActivity.EXTRA_NAME, item.displayName(row.getPlaceName()));
        intent.putExtra(StopDetailActivity.EXTRA_DAY, item.getDay());
        intent.putExtra(StopDetailActivity.EXTRA_POSITION, item.getPosition());
        intent.putExtra(StopDetailActivity.EXTRA_TIME, item.getTime());
        intent.putExtra(StopDetailActivity.EXTRA_DURATION,
                item.getDurationMin() == null ? -1 : item.getDurationMin());
        intent.putExtra(StopDetailActivity.EXTRA_BOOKED, item.isBooked());
        intent.putExtra(StopDetailActivity.EXTRA_MAX_DAY, Math.max(2, maxDay() + 1));
        startActivity(intent);
    }

    @Override
    public void onLegClick(ItineraryRow row) {
        ItineraryItemEntity item = row.getItem();
        double km = PrototypeStore.legKm(item.getId());
        TravelMode[] modes = TravelMode.values();
        String[] labels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            labels[i] = getString(R.string.leg_option, Labels.mode(this, modes[i]),
                    modes[i].minutesFor(km));
        }
        int checked = store.legMode(item.getId(), km).ordinal();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.leg_dialog_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    store.setLegMode(item.getId(), modes[which]);
                    dialog.dismiss();
                    renderSelectedDayStops();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // ---- weather-aware rescheduling ----

    private List<ItineraryItemWithPlace> outdoorFlexibleStops(int day) {
        List<ItineraryItemWithPlace> outdoor = new ArrayList<>();
        for (ItineraryItemWithPlace entry : allItems) {
            String name = entry.item.displayName(entry.placeName);
            if (entry.item.getDay() == day && !entry.item.isBooked() && MockData.isOutdoor(name)) {
                outdoor.add(entry);
            }
        }
        return outdoor;
    }

    private void renderWeatherBanner() {
        int day = selectedDay;
        int count = MockData.isRainy(day) ? outdoorFlexibleStops(day).size() : 0;
        weatherBanner.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        weatherBanner.setText(getResources().getQuantityString(
                R.plurals.weather_banner, count, day, count));
    }

    private void showWeatherDialog() {
        int day = selectedDay;
        List<ItineraryItemWithPlace> outdoor = outdoorFlexibleStops(day);
        StringBuilder names = new StringBuilder();
        for (ItineraryItemWithPlace entry : outdoor) {
            names.append("• ").append(entry.item.displayName(entry.placeName)).append('\n');
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.weather_title, day))
                .setMessage(getString(R.string.weather_message, names.toString().trim()))
                .setNegativeButton(R.string.action_keep_plan, null)
                .setPositiveButton(R.string.action_swap_indoor, (dialog, which) ->
                        swapForIndoor(day, outdoor))
                .show();
    }

    private void swapForIndoor(int day, List<ItineraryItemWithPlace> outdoor) {
        List<MockPlace> indoor = MockData.indoorAlternatives();
        Result.Callback<Void> report = result -> {
            if (!result.isSuccess()) {
                Snackbar.make(findViewById(R.id.main), R.string.error_update_failed,
                        Snackbar.LENGTH_SHORT).show();
            }
        };
        for (int i = 0; i < outdoor.size(); i++) {
            String replacement = indoor.get(i % indoor.size()).getName();
            repository.addItineraryItem(tripId, replacement, day, null, null, false, report);
            repository.deleteItineraryItem(outdoor.get(i).item.getId(), report);
        }
        Snackbar.make(findViewById(R.id.main),
                getResources().getQuantityString(R.plurals.weather_swapped, outdoor.size(),
                        outdoor.size()), Snackbar.LENGTH_SHORT).show();
    }

    // ---- rendering ----

    private int maxDay() {
        TreeSet<Integer> days = distinctDays(allItems);
        return days.isEmpty() ? 1 : days.last();
    }

    private TreeSet<Integer> distinctDays(List<ItineraryItemWithPlace> items) {
        TreeSet<Integer> days = new TreeSet<>();
        for (ItineraryItemWithPlace item : items) {
            days.add(item.item.getDay());
        }
        return days;
    }

    private void renderDayTabs() {
        dayTabContainer.removeAllViews();
        TreeSet<Integer> days = distinctDays(allItems);
        if (days.isEmpty()) {
            days.add(1);
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int day : days) {
            TextView tab = (TextView) inflater.inflate(R.layout.item_day_tab, dayTabContainer, false);
            tab.setText(getString(R.string.day_label, day));
            bindTabSelectedState(tab, day == selectedDay);
            tab.setOnClickListener(v -> {
                selectedDay = day;
                renderDayTabs();
                renderSelectedDayStops();
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

    private void renderSelectedDayStops() {
        List<ItineraryRow> rows = new ArrayList<>();
        int index = 1;
        int travelMin = 0;
        for (ItineraryItemWithPlace item : allItems) {
            if (item.item.getDay() == selectedDay) {
                rows.add(new ItineraryRow(item.item, item.placeName, index++));
            }
        }
        for (int i = 0; i < rows.size() - 1; i++) {
            travelMin += ItineraryAdapter.legMinutes(rows.get(i).getItem());
        }
        adapter.submitRows(rows);
        emptyState.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        daySummary.setText(getResources().getQuantityString(
                R.plurals.day_summary, rows.size(), rows.size(), travelMin));
        renderWeatherBanner();
    }
}
