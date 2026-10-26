package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.itismob.s01.group01.tripfit.model.ItineraryItem;
import com.itismob.s01.group01.tripfit.ui.ItineraryAdapter;
import com.itismob.s01.group01.tripfit.ui.ItineraryRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final ItineraryAdapter adapter = new ItineraryAdapter();

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

        RecyclerView recyclerView = findViewById(R.id.recycler_itinerary);
        recyclerView.setAdapter(adapter);
        adapter.submitRows(buildRows(sampleItems()));

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_destination);
        fab.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddDestinationActivity.class)));
    }

    private List<ItineraryItem> sampleItems() {
        List<ItineraryItem> items = new ArrayList<>();
        items.add(new ItineraryItem("1", 1, "Senso-ji Temple", "45 min", false, null));
        items.add(new ItineraryItem("2", 1, "Nakamise Shopping Street", "30 min", false, null));
        items.add(new ItineraryItem("3", 1, "Tokyo Skytree", null, true, "18:00"));
        items.add(new ItineraryItem("4", 2, "Meiji Shrine", "40 min", false, null));
        items.add(new ItineraryItem("5", 2, "Shibuya Crossing", "20 min", false, null));
        items.add(new ItineraryItem("6", 2, "teamLab Planets", null, true, "15:30"));
        return items;
    }

    /** Groups flat items by day into header + stop rows, preserving day and item order. */
    private List<ItineraryRow> buildRows(List<ItineraryItem> items) {
        Map<Integer, List<ItineraryItem>> byDay = new LinkedHashMap<>();
        for (ItineraryItem item : items) {
            byDay.computeIfAbsent(item.getDay(), day -> new ArrayList<>()).add(item);
        }

        List<ItineraryRow> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<ItineraryItem>> entry : byDay.entrySet()) {
            List<ItineraryItem> dayItems = entry.getValue();
            rows.add(new ItineraryRow.DayHeader(entry.getKey(), dayItems.size()));
            for (int i = 0; i < dayItems.size(); i++) {
                rows.add(new ItineraryRow.Stop(dayItems.get(i), i + 1));
            }
        }
        return rows;
    }
}
