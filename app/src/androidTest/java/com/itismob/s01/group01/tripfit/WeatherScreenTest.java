package com.itismob.s01.group01.tripfit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.appbar.MaterialToolbar;
import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WeatherScreenTest {
    @Test
    public void itineraryBannerAndOverflowOpenWeatherForTheSelectedDay() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        TripFitDatabase database = TripFitDatabase.getInstance(context);
        String tripId = UUID.randomUUID().toString();
        database.tripDao().insert(new TripEntity(tripId, null, "Weather test",
                null, null, null, null));
        database.itineraryItemDao().insert(new ItineraryItemEntity(UUID.randomUUID().toString(),
                tripId, null, "Meiji Shrine", 2, "15:30", 40, true, null, null, 0));
        PrototypeStore.resetForTesting();
        PrototypeStore.get().setMeta(tripId, new TripMeta("Tokyo", "", 2, "PHP", "2026-12-01"));

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            CountDownLatch bannerVisible = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                LinearLayout tabs = activity.findViewById(R.id.day_tab_container);
                View banner = activity.findViewById(R.id.banner_weather);
                banner.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
                    if (view.isShown()) {
                        bannerVisible.countDown();
                    }
                });
                tabs.getChildAt(1).performClick();
                if (banner.isShown()) {
                    bannerVisible.countDown();
                }
            });
            assertTrue("Weather banner did not show on Day 2", bannerVisible.await(5, TimeUnit.SECONDS));
            onView(withId(R.id.banner_weather)).perform(click());
            onView(withText(R.string.weather_booked_conflict)).check(matches(isDisplayed()));

            pressBack();
            scenario.onActivity(activity -> {
                MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
                assertTrue(toolbar.getMenu().performIdentifierAction(R.id.action_weather, 0));
            });
            onView(withId(R.id.list_weather)).check(matches(isDisplayed()));
        } finally {
            database.getOpenHelper().getWritableDatabase()
                    .execSQL("DELETE FROM trips WHERE id = ?", new Object[]{tripId});
            PrototypeStore.init(context);
        }
    }
}
