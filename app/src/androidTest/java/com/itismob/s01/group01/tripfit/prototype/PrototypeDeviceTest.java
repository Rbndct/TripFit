package com.itismob.s01.group01.tripfit.prototype;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.itismob.s01.group01.tripfit.HomeActivity;
import com.itismob.s01.group01.tripfit.MapPickerActivity;
import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.ScreenActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class PrototypeDeviceTest {

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
            com.itismob.s01.group01.tripfit.SummaryActivity.class
        };
        for (Class<?> screen : screens) {
            Intent intent = new Intent(context, screen);
            intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            try (ActivityScenario<?> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> assertTrue(activity instanceof ScreenActivity));
            }
        }
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
        Context isolated = new ContextWrapper(ApplicationProvider.getApplicationContext()) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("device_test_" + name, mode);
            }
        };
        PrototypeStore.init(isolated);
        PrototypeStore store = PrototypeStore.get();
        String tripId = "test-store-trip";
        store.setItemDetails("item-1", new ItemDetails(500, "PHP", "Activities", true));
        store.addExpense(tripId, "Lunch", Category.FOOD, 150, "PHP");

        assertEquals(500.0, store.itemDetails("item-1").getCost(), 0.001);
        assertEquals(1, store.expenses(tripId).size());
        assertEquals("Lunch", store.expenses(tripId).get(0).getTitle());
    }

    @Test
    public void pdfExportProducesRenderablePages() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        DraftTrip trip = new DraftTrip();
        trip.name = "Prototype export check";
        for (int i = 0; i < 24; i++) {
            DraftTrip.Stop stop = DemoCatalog.places("Tokyo").get(i % 6).stop(i % 3 + 1);
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
