package com.itismob.s01.group01.tripfit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import com.google.android.material.chip.Chip;

import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class CatalogScreenTest {
    @Test
    public void discoverRestoresCityQueryFilterDayAndBudgetToggle() {
        Context context = ApplicationProvider.getApplicationContext();
        TripFitDatabase database = TripFitDatabase.getInstance(context);
        String tripId = UUID.randomUUID().toString();
        database.tripDao().insert(new TripEntity(tripId, null, "Discover test",
                null, null, null, null));
        PrototypeStore.resetForTesting();
        PrototypeStore.get().setMeta(tripId, new TripMeta("Manila", "", 2, "PHP", null));

        Intent intent = new Intent(context, DiscoverActivity.class);
        intent.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
        try (ActivityScenario<DiscoverActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> activity.findViewById(R.id.button_catalog_filter)
                    .performClick());
            selectDialogItem(context.getString(R.string.tab_activities));
            scenario.onActivity(activity -> activity.findViewById(R.id.button_catalog_day)
                    .performClick());
            selectDialogItem(context.getString(R.string.catalog_day, 2));
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.chip_fits_budget).performClick();
                ((EditText) activity.findViewById(R.id.input_catalog_search)).setText("Rizal");
            });

            scenario.recreate();

            scenario.onActivity(activity -> {
                assertEquals("Manila", textOf(activity, R.id.text_city));
                assertEquals("Rizal", textOf(activity, R.id.input_catalog_search));
                assertEquals(context.getString(R.string.tab_activities),
                        textOf(activity, R.id.button_catalog_filter));
                assertEquals(context.getString(R.string.suggestions_target_day, 2),
                        textOf(activity, R.id.button_catalog_day));
                assertTrue(((Chip) activity.findViewById(R.id.chip_fits_budget)).isChecked());
            });
        } finally {
            database.getOpenHelper().getWritableDatabase()
                    .execSQL("DELETE FROM trips WHERE id = ?", new Object[]{tripId});
            PrototypeStore.init(context);
        }
    }

    private static String textOf(DiscoverActivity activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }

    private static void selectDialogItem(String label) {
        onView(withId(androidx.appcompat.R.id.select_dialog_listview)).inRoot(isDialog())
                .perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return org.hamcrest.Matchers.instanceOf(ListView.class);
                    }

                    @Override
                    public String getDescription() {
                        return "Select dialog item " + label;
                    }

                    @Override
                    public void perform(UiController ui, View view) {
                        ListView list = (ListView) view;
                        for (int i = 0; i < list.getCount(); i++) {
                            if (label.equals(list.getItemAtPosition(i))) {
                                list.performItemClick(list.getChildAt(i - list.getFirstVisiblePosition()),
                                        i, list.getItemIdAtPosition(i));
                                return;
                            }
                        }
                        throw new AssertionError("Dialog item missing: " + label);
                    }
                });
    }

}
