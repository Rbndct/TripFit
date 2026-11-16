package com.itismob.s01.group01.tripfit;

import android.view.View;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

/**
 * Base for the prototype's navy-toolbar screens: edge-to-edge insets, a back arrow, and a
 * title, so each screen only supplies its layout (which must {@code <include>} view_toolbar).
 */
public abstract class ScreenActivity extends AppCompatActivity {

    protected String tripId;

    /**
     * Sets up the shared shell. Returns false when this screen needs a trip but got a missing
     * or blank id; it has then already finished itself and the caller must return from
     * {@code onCreate}. An id that matches no trip closes the screen as soon as Room answers.
     */
    protected boolean setScreen(@LayoutRes int layout, @StringRes int titleRes) {
        EdgeToEdge.enable(this);
        setContentView(layout);
        applyShellSystemBars();
        View content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });
        tripId = getIntent().getStringExtra(HomeActivity.EXTRA_TRIP_ID);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(titleRes);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        return guardTrip();
    }

    /** Whether this screen shows one trip's data; false for Profile, Map Picker and New Trip. */
    protected boolean requiresTrip() {
        return true;
    }

    private boolean guardTrip() {
        boolean hasId = tripId != null && !tripId.trim().isEmpty();
        if (!hasId) {
            if (requiresTrip()) {
                finish();
                return false;
            }
            return true;
        }
        ((TripFitApp) getApplication()).getTripRepository().observeTrip(tripId)
                .observe(this, trip -> {
                    if (trip == null) {
                        finish();
                    }
                });
        return true;
    }

    /** Keeps the status bar on the fixed navy shell with light icons, in both light and dark themes. */
    @SuppressWarnings("deprecation") // no-op once edge-to-edge is enforced (API 35+); needed below that
    private void applyShellSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.ticket_carrier_navy));
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
    }

    protected void toast(@StringRes int messageRes) {
        Snackbar.make(findViewById(android.R.id.content), messageRes, Snackbar.LENGTH_SHORT).show();
    }

    protected void toast(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }
}
