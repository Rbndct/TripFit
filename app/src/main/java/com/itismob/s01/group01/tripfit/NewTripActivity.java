package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.viewmodel.NewTripViewModel;

/** Creates a new trip, then hands off straight into its (empty) itinerary. */
public class NewTripActivity extends AppCompatActivity {

    private TextInputLayout layoutName;
    private TextInputEditText inputName;
    private NewTripViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_trip);

        TripFitApp app = (TripFitApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new NewTripViewModel.Factory(app.getTripRepository())).get(NewTripViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        layoutName = findViewById(R.id.layout_trip_name);
        inputName = findViewById(R.id.input_trip_name);

        findViewById(R.id.button_create_trip).setOnClickListener(v -> attemptCreate());

        viewModel.getCreateResult().observe(this, result -> {
            if (result == null) {
                return;
            }
            if (result.isSuccess()) {
                Intent intent = new Intent(NewTripActivity.this, MainActivity.class);
                intent.putExtra(HomeActivity.EXTRA_TRIP_ID, result.getData());
                startActivity(intent);
                finish();
            } else {
                Snackbar.make(findViewById(R.id.new_trip_root), R.string.error_create_trip_failed,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptCreate() {
        String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            layoutName.setError(getString(R.string.error_trip_name_required));
            return;
        }
        layoutName.setError(null);
        viewModel.createTrip(name);
    }
}
