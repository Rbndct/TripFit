package com.itismob.s01.group01.tripfit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.itismob.s01.group01.tripfit.data.TripRepositoryImpl;

/**
 * Debug-only entry point for screenshot capture: {@code adb shell am start -n
 * <pkg>/.DebugLauncherActivity --es screen BudgetActivity}. Seeds the sample trip, then opens the
 * named activity with every extra forwarded and the seed trip id as the default trip. Lives in
 * src/debug, so release builds never contain it.
 */
public class DebugLauncherActivity extends AppCompatActivity {

    private static final String TAG = "DebugLauncher";
    private static final String EXTRA_SCREEN = "screen";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Class<? extends Activity> target = resolve(getIntent().getStringExtra(EXTRA_SCREEN));
        if (target == null) {
            finish();
            return;
        }
        ((TripFitApp) getApplication()).getTripRepository().ensureSeedData(result -> {
            Intent intent = new Intent(this, target);
            if (getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
            if (!intent.hasExtra(HomeActivity.EXTRA_TRIP_ID)) {
                intent.putExtra(HomeActivity.EXTRA_TRIP_ID, TripRepositoryImpl.SEED_TRIP_ID);
            }
            startActivity(intent);
            finish();
        });
    }

    /** Only activities inside this app's own package can be launched. */
    @Nullable
    private Class<? extends Activity> resolve(@Nullable String simpleName) {
        if (simpleName == null || !simpleName.matches("[A-Za-z]+")) {
            Log.w(TAG, "Missing or invalid screen name: " + simpleName);
            return null;
        }
        try {
            Class<?> cls = Class.forName(getClass().getPackage().getName() + "." + simpleName);
            if (Activity.class.isAssignableFrom(cls)) {
                return cls.asSubclass(Activity.class);
            }
            Log.w(TAG, simpleName + " is not an Activity");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Unknown screen: " + simpleName);
        }
        return null;
    }
}
