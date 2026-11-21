package com.itismob.s01.group01.tripfit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.itismob.s01.group01.tripfit.data.TripRepositoryImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** At 1.3x font the Discover filter and day buttons must each stay on one line. */
@RunWith(AndroidJUnit4.class)
public class DiscoverFontScaleTest {
    private static final String LARGE_FONT = "1.3";
    private static final String NORMAL_FONT = "1.0";
    private static final long LAYOUT_WAIT_MS = 4000;

    @Before
    public void seedSampleTrip() throws InterruptedException {
        TripFitApp app = ApplicationProvider.getApplicationContext();
        CountDownLatch seeded = new CountDownLatch(1);
        app.getTripRepository().ensureSeedData(result -> seeded.countDown());
        assertTrue("Sample trip was not seeded", seeded.await(10, TimeUnit.SECONDS));
    }

    @After
    public void restoreFontScale() throws IOException {
        setFontScale(NORMAL_FONT);
    }

    @Test
    public void filterAndDayButtonsStayOnOneLineAtLargeFont() throws Exception {
        setFontScale(LARGE_FONT);
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, DiscoverActivity.class)
                .putExtra(HomeActivity.EXTRA_TRIP_ID, TripRepositoryImpl.SEED_TRIP_ID);
        try (ActivityScenario<DiscoverActivity> scenario = ActivityScenario.launch(intent)) {
            int[] lines = new int[2];
            long deadline = System.currentTimeMillis() + LAYOUT_WAIT_MS;
            while (System.currentTimeMillis() < deadline && (lines[0] == 0 || lines[1] == 0)) {
                Thread.sleep(200);
                scenario.onActivity(activity -> {
                    lines[0] = lineCount(activity.findViewById(R.id.button_catalog_filter));
                    lines[1] = lineCount(activity.findViewById(R.id.button_catalog_day));
                });
            }
            assertTrue("Discover buttons were never laid out", lines[0] > 0 && lines[1] > 0);
            assertEquals("Filter button wraps at 1.3x font", 1, lines[0]);
            assertEquals("Day button wraps at 1.3x font", 1, lines[1]);
        }
    }

    private static int lineCount(android.view.View button) {
        return ((TextView) button).getLineCount();
    }

    private static void setFontScale(String scale) throws IOException {
        ParcelFileDescriptor output = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("settings put system font_scale " + scale);
        try (FileInputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(output)) {
            while (stream.read() != -1) {
                // drain so the command has finished before the screen is launched
            }
        }
        try {
            Thread.sleep(1500); // let the new configuration reach the app
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
