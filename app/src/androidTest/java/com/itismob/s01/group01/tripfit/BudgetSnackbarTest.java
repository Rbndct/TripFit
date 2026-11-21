package com.itismob.s01.group01.tripfit;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.itismob.s01.group01.tripfit.data.TripRepositoryImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** The "Budget saved" confirmation must not cover the Add expense button (critique fix 4547a15). */
@RunWith(AndroidJUnit4.class)
public class BudgetSnackbarTest {
    private static final long WAIT_MS = 4000;
    private static final long SETTLE_MS = 700;

    @Before
    public void seedSampleTrip() throws InterruptedException {
        TripFitApp app = ApplicationProvider.getApplicationContext();
        CountDownLatch seeded = new CountDownLatch(1);
        app.getTripRepository().ensureSeedData(result -> seeded.countDown());
        assertTrue("Sample trip was not seeded", seeded.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void budgetSavedSnackbarSitsAboveTheAddExpenseButton() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, BudgetActivity.class)
                .putExtra(HomeActivity.EXTRA_TRIP_ID, TripRepositoryImpl.SEED_TRIP_ID);
        try (ActivityScenario<BudgetActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(BudgetActivity::showBudgetSaved);
            int[] bounds = new int[2]; // snackbar bottom, fab top
            long deadline = System.currentTimeMillis() + WAIT_MS;
            boolean found = false;
            while (!found && System.currentTimeMillis() < deadline) {
                Thread.sleep(SETTLE_MS);
                found = measure(scenario, bounds);
            }
            assertTrue("Budget saved snackbar did not appear", found);
            assertTrue("Snackbar bottom " + bounds[0] + " covers the FAB top " + bounds[1],
                    bounds[0] <= bounds[1] + 2);
        }
    }

    private static boolean measure(ActivityScenario<BudgetActivity> scenario, int[] bounds) {
        boolean[] found = new boolean[1];
        scenario.onActivity(activity -> {
            View text = activity.findViewById(com.google.android.material.R.id.snackbar_text);
            View fab = activity.findViewById(R.id.fab_add_expense);
            if (text == null || fab == null) {
                return;
            }
            View snackbar = (View) text.getParent().getParent();
            int[] snackbarAt = new int[2];
            int[] fabAt = new int[2];
            snackbar.getLocationOnScreen(snackbarAt);
            fab.getLocationOnScreen(fabAt);
            bounds[0] = snackbarAt[1] + snackbar.getHeight();
            bounds[1] = fabAt[1];
            found[0] = true;
        });
        return found[0];
    }
}
