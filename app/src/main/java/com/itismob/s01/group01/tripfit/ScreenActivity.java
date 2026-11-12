package com.itismob.s01.group01.tripfit;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

/**
 * Base for the prototype's navy-toolbar screens: edge-to-edge insets, a back arrow, and a
 * title, so each screen only supplies its layout (which must {@code <include>} view_toolbar).
 */
public abstract class ScreenActivity extends AppCompatActivity {

    protected String tripId;

    protected void setScreen(@LayoutRes int layout, @StringRes int titleRes) {
        EdgeToEdge.enable(this);
        setContentView(layout);
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
    }

    protected void toast(@StringRes int messageRes) {
        Snackbar.make(findViewById(android.R.id.content), messageRes, Snackbar.LENGTH_SHORT).show();
    }

    protected void toast(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }
}
