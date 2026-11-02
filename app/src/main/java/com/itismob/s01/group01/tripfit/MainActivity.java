package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Itinerary screen for a single trip, loaded from Room via {@link ItineraryViewModel}. */
public class MainActivity extends AppCompatActivity {

    private final ItineraryAdapter adapter = new ItineraryAdapter();
    private String tripId;

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

        tripId = getIntent().getStringExtra(HomeActivity.EXTRA_TRIP_ID);

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
        viewModel.getItems().observe(this, items -> adapter.submitRows(buildRows(items)));

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_destination);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddDestinationActivity.class);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        });
    }

    /** Groups flat items by day into header + stop rows, preserving day and item order. */
    private List<ItineraryRow> buildRows(List<ItineraryItemWithPlace> items) {
        Map<Integer, List<ItineraryItemWithPlace>> byDay = new LinkedHashMap<>();
        for (ItineraryItemWithPlace item : items) {
            byDay.computeIfAbsent(item.item.getDay(), day -> new ArrayList<>()).add(item);
        }

        List<ItineraryRow> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<ItineraryItemWithPlace>> entry : byDay.entrySet()) {
            List<ItineraryItemWithPlace> dayItems = entry.getValue();
            rows.add(new ItineraryRow.DayHeader(entry.getKey(), dayItems.size()));
            for (int i = 0; i < dayItems.size(); i++) {
                rows.add(new ItineraryRow.Stop(dayItems.get(i).item, dayItems.get(i).placeName, i + 1));
            }
        }
        return rows;
    }
}
