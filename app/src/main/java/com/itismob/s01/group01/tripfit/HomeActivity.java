package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.itismob.s01.group01.tripfit.ui.TripAdapter;
import com.itismob.s01.group01.tripfit.viewmodel.HomeViewModel;

/** Trip dashboard reached after sign-in. Lists every trip in Room; the Tokyo Trip is seeded on first run. */
public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    private HomeViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        applyShellSystemBars();
        View appBar = findViewById(R.id.appbar_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // The navy header runs under the status bar, so it absorbs the top inset itself.
            appBar.setPadding(0, systemBars.top, 0, 0);
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        TripFitApp app = (TripFitApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new HomeViewModel.Factory(app.getTripRepository())).get(HomeViewModel.class);

        TripAdapter adapter = new TripAdapter(this::openTrip);
        RecyclerView listTrips = findViewById(R.id.list_trips);
        listTrips.setLayoutManager(new LinearLayoutManager(this));
        listTrips.setAdapter(adapter);
        viewModel.getTripRows().observe(this, adapter::submitTrips);

        findViewById(R.id.button_profile).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_new_trip);
        fab.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, NewTripActivity.class)));
    }

    /** Keeps the status bar on the fixed navy shell with light icons, in both light and dark themes. */
    @SuppressWarnings("deprecation") // no-op once edge-to-edge is enforced (API 35+); needed below that
    private void applyShellSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.ticket_carrier_navy));
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
    }

    private void openTrip(String tripId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_TRIP_ID, tripId);
        startActivity(intent);
    }
}
