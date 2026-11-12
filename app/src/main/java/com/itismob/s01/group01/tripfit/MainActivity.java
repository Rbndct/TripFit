package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.ui.ItineraryAdapter;
import com.itismob.s01.group01.tripfit.ui.ItineraryRow;
import com.itismob.s01.group01.tripfit.viewmodel.ItineraryViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Itinerary screen for a single trip, loaded from Room via {@link ItineraryViewModel}. Days
 * render as a tab strip in the header; the list below shows only the selected day's stops.
 */
public class MainActivity extends AppCompatActivity {

    private final ItineraryAdapter adapter = new ItineraryAdapter();
    private List<ItineraryItemWithPlace> allItems = new ArrayList<>();
    private Integer selectedDay;
    private LinearLayout dayTabContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String tripId = getIntent().getStringExtra(HomeActivity.EXTRA_TRIP_ID);
        dayTabContainer = findViewById(R.id.day_tab_container);

        TripFitApp app = (TripFitApp) getApplication();
        ItineraryViewModel viewModel = new ViewModelProvider(this,
                new ItineraryViewModel.Factory(app.getTripRepository(), tripId))
                .get(ItineraryViewModel.class);

        TextView tripTitle = findViewById(R.id.text_trip_title);
        app.getTripRepository().observeTrip(tripId).observe(this, trip -> {
            if (trip != null) {
                tripTitle.setText(trip.getName());
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recycler_itinerary);
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
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddDestinationActivity.class);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        });
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
            });
            dayTabContainer.addView(tab);
        }
    }

    private void bindTabSelectedState(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected
                ? R.drawable.bg_day_tab_selected : R.drawable.bg_day_tab_unselected);
        tab.setTextColor(getColor(selected
                ? R.color.ticket_carrier_navy : R.color.on_ticket_carrier));
    }

    private void renderSelectedDayStops() {
        List<ItineraryRow> rows = new ArrayList<>();
        int index = 1;
        for (ItineraryItemWithPlace item : allItems) {
            if (item.item.getDay() == selectedDay) {
                rows.add(new ItineraryRow(item.item, item.placeName, index++));
            }
        }
        adapter.submitRows(rows);
    }
}
