package com.itismob.s01.group01.tripfit.prototype;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.itismob.s01.group01.tripfit.HomeActivity;
import com.itismob.s01.group01.tripfit.MapPickerActivity;
import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.ScreenActivity;
import com.itismob.s01.group01.tripfit.TripFitApp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class PrototypeDeviceTest {

    /** Screens close themselves when their trip is missing, so a clean install needs the sample trip. */
    @Before
    public void seedSampleTrip() throws InterruptedException {
        TripFitApp app = ApplicationProvider.getApplicationContext();
        CountDownLatch seeded = new CountDownLatch(1);
        app.getTripRepository().ensureSeedData(result -> seeded.countDown());
        assertTrue("Sample trip was not seeded", seeded.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void screenActivitiesLaunchLocally() {
        Context context = ApplicationProvider.getApplicationContext();
        String tripId = "seed-trip-tokyo";
        Class<?>[] screens = {
            com.itismob.s01.group01.tripfit.DiscoverActivity.class,
            com.itismob.s01.group01.tripfit.BudgetActivity.class,
            com.itismob.s01.group01.tripfit.WeatherActivity.class,
            com.itismob.s01.group01.tripfit.GroupActivity.class,
            com.itismob.s01.group01.tripfit.JournalActivity.class,
            com.itismob.s01.group01.tripfit.SummaryActivity.class,
            com.itismob.s01.group01.tripfit.OptimizeActivity.class,
            com.itismob.s01.group01.tripfit.CalendarSyncActivity.class,
            com.itismob.s01.group01.tripfit.SuggestionsActivity.class,
            com.itismob.s01.group01.tripfit.BudgetSetupActivity.class
        };
        for (Class<?> screen : screens) {
            Intent intent = new Intent(context, screen);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            try (ActivityScenario<?> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> assertTrue(activity instanceof ScreenActivity));
                assertTrue(screen.getSimpleName() + " toolbar is under the status bar",
                        awaitToolbarBelowStatusBar(scenario));
            }
        }
    }

    /**
     * The shell's app bar uses fitsSystemWindows, so the toolbar must sit at or below the
     * status-bar inset (insets arrive after the first layout pass, so wait for them).
     */
    private static boolean awaitToolbarBelowStatusBar(ActivityScenario<?> scenario) {
        long deadline = System.currentTimeMillis() + 3000;
        boolean[] clear = new boolean[1];
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                View toolbar = activity.findViewById(R.id.toolbar);
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(toolbar);
                int statusBar = insets == null ? 0
                        : insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int[] location = new int[2];
                toolbar.getLocationInWindow(location);
                clear[0] = statusBar > 0 && location[1] >= statusBar;
            });
            if (clear[0]) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Test
    public void mapTapReturnsTheNewCoordinatesWithoutGeocoding() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, MapPickerActivity.class);
        intent.putExtra(MapPickerActivity.EXTRA_CITY_LAT, 35.6762);
        intent.putExtra(MapPickerActivity.EXTRA_CITY_LNG, 139.6503);
        try (ActivityScenario<MapPickerActivity> scenario = ActivityScenario.launchActivityForResult(intent)) {
            CountDownLatch pinMoved = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                TextView coordinates = activity.findViewById(R.id.text_coordinates);
                coordinates.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        pinMoved.countDown();
                    }
                    @Override public void afterTextChanged(Editable s) { }
                });
            });
            onView(withId(R.id.map_view)).perform(new androidx.test.espresso.action.GeneralClickAction(
                    androidx.test.espresso.action.Tap.SINGLE, view -> {
                int[] position = new int[2];
                view.getLocationOnScreen(position);
                int x = Math.round(view.getWidth() * .72f);
                int y = Math.round(view.getHeight() * .4f);
                return new float[]{position[0] + x, position[1] + y};
            }, androidx.test.espresso.action.Press.FINGER));
            assertTrue("Map did not update the pin after tapping", pinMoved.await(5, TimeUnit.SECONDS));
            String[] displayedPin = new String[1];
            scenario.onActivity(activity -> {
                TextView coordinates = activity.findViewById(R.id.text_coordinates);
                displayedPin[0] = coordinates.getText().toString();
            });
            onView(withId(R.id.button_use_location)).perform(click());
            Instrumentation.ActivityResult result = scenario.getResult();
            assertEquals(Activity.RESULT_OK, result.getResultCode());
            assertNotNull(result.getResultData());
            double latitude = result.getResultData()
                    .getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LAT, Double.NaN);
            double longitude = result.getResultData()
                    .getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LNG, Double.NaN);
            assertEquals(displayedPin[0], context.getString(R.string.map_picker_coords,
                    latitude, longitude));
            assertTrue(Math.abs(latitude - 35.6762) > 0.0001
                    || Math.abs(longitude - 139.6503) > 0.0001);
        }
    }

    @Test
    public void pinnedStopAndExpenseSurviveReopeningTheLocalStore() {
        Context context = ApplicationProvider.getApplicationContext();
        String suffix = UUID.randomUUID().toString();
        String tripId = "test-store-trip-" + suffix;
        String itemId = "item-" + suffix;
        PrototypeStore first = PrototypeStore.forStorage(new PrefsKeyValueStorage(context));
        first.setItemDetails(itemId, new ItemDetails(500, "PHP", "Activities", true));
        first.addExpense(tripId, "Lunch", Category.FOOD, 150, "PHP");
        try {
            // A second store over the same preferences file must load what the first one saved.
            PrototypeStore reopened = PrototypeStore.forStorage(new PrefsKeyValueStorage(context));
            assertNotNull(reopened.itemDetails(itemId));
            assertEquals(500.0, reopened.itemDetails(itemId).getCost(), 0.001);
            assertEquals(1, reopened.expenses(tripId).size());
            assertEquals("Lunch", reopened.expenses(tripId).get(0).getTitle());
        } finally {
            first.removeItemDetails(itemId);
            for (Expense expense : first.expenses(tripId)) {
                first.removeExpense(tripId, expense.getId());
            }
        }
    }

    @Test
    public void pdfExportProducesRenderablePages() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        TripSnapshot trip = new TripSnapshot();
        trip.name = "Prototype export check";
        for (int i = 0; i < 24; i++) {
            TripSnapshot.Stop stop = DemoCatalog.places("Tokyo").get(i % 6).stop(i % 3 + 1);
            trip.stops.add(stop);
        }
        File pdf = new File(context.getCacheDir(), "prototype-qa.pdf");
        try (FileOutputStream output = new FileOutputStream(pdf)) {
            TripExport.pdf(trip, output);
        }
        try (ParcelFileDescriptor input = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(input)) {
            assertTrue(renderer.getPageCount() >= 2);
            try (PdfRenderer.Page page = renderer.openPage(0)) {
                Bitmap bitmap = Bitmap.createBitmap(1190, 1684, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                try (FileOutputStream image = new FileOutputStream(new File(context.getCacheDir(), "prototype-qa-page-1.png"))) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, image);
                }
                bitmap.recycle();
            }
        }
    }
}
