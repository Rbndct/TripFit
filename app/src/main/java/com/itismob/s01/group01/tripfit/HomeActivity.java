package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

/**
 * Sprint 1 UI for the trip dashboard reached after sign-in. Trip cards, the
 * profile button, and "New Trip" are UI only; the Tokyo Trip card is the one
 * live link, into the existing itinerary screen.
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.card_trip_tokyo).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MainActivity.class)));

        findViewById(R.id.button_profile).setOnClickListener(v ->
                showComingSoon(R.string.profile_coming_soon));

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_new_trip);
        fab.setOnClickListener(v -> showComingSoon(R.string.new_trip_coming_soon));
    }

    private void showComingSoon(int messageRes) {
        Snackbar.make(findViewById(R.id.home_root), messageRes, Snackbar.LENGTH_SHORT).show();
    }
}
