package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.viewmodel.HomeViewModel;

import java.util.HashSet;
import java.util.Set;

/** Trip dashboard reached after sign-in. The Tokyo Trip card is seeded into Room on first run. */
public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    private HomeViewModel viewModel;
    private MaterialCardView cardTrip;
    private String currentTripId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        TripFitApp app = (TripFitApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new HomeViewModel.Factory(app.getTripRepository())).get(HomeViewModel.class);

        cardTrip = findViewById(R.id.card_trip_tokyo);
        TextView title = findViewById(R.id.text_trip_title);
        TextView days = findViewById(R.id.text_trip_days);
        TextView stops = findViewById(R.id.text_trip_stops);

        cardTrip.setVisibility(android.view.View.INVISIBLE);
        viewModel.getFirstTrip().observe(this, trip -> {
            if (trip == null) {
                cardTrip.setVisibility(android.view.View.INVISIBLE);
                return;
            }
            currentTripId = trip.getId();
            title.setText(trip.getName());
            cardTrip.setVisibility(android.view.View.VISIBLE);
        });
        viewModel.getFirstTripItems().observe(this, items -> {
            Set<Integer> uniqueDays = new HashSet<>();
            for (var item : items) {
                uniqueDays.add(item.getDay());
            }
            days.setText(getString(R.string.trip_days_count, uniqueDays.size()));
            stops.setText(getString(R.string.trip_stops_count, items.size()));
        });

        cardTrip.setOnClickListener(v -> {
            if (currentTripId == null) {
                return;
            }
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra(EXTRA_TRIP_ID, currentTripId);
            startActivity(intent);
        });

        findViewById(R.id.button_profile).setOnClickListener(v ->
                showComingSoon(R.string.profile_coming_soon));

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_new_trip);
        fab.setOnClickListener(v -> showComingSoon(R.string.new_trip_coming_soon));
    }

    private void showComingSoon(int messageRes) {
        Snackbar.make(findViewById(R.id.home_root), messageRes, Snackbar.LENGTH_SHORT).show();
    }
}
