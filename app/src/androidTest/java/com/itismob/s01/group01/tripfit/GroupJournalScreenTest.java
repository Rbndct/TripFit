package com.itismob.s01.group01.tripfit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.any;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.prototype.JournalEntry;
import com.itismob.s01.group01.tripfit.prototype.PrefsKeyValueStorage;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class GroupJournalScreenTest {
    private Context context;
    private Context isolated;
    private TripFitDatabase database;
    private String tripId;
    private String stopId;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        isolated = new ContextWrapper(context) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("step9_test_" + name, mode);
            }
        };
        database = TripFitDatabase.getInstance(context);
        tripId = UUID.randomUUID().toString();
        stopId = UUID.randomUUID().toString();
        database.tripDao().insert(new TripEntity(tripId, null, "Step 9 fixture", null, null, null, null));
        database.itineraryItemDao().insert(new ItineraryItemEntity(stopId, tripId, null,
                "Museum", 2, null, 60, false, null, null, 0));
        reloadStore();
        PrototypeStore.get().setMeta(tripId, new TripMeta("Tokyo", "", 2, "PHP", "2026-12-01"));
    }

    @After
    public void tearDown() {
        database.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM trips WHERE id = ?", new Object[]{tripId});
        isolated.getSharedPreferences(PrefsKeyValueStorage.FILE_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();
        PrototypeStore.resetForTesting();
        PrototypeStore.init(context);
    }

    @Test
    public void sampleCodeAddsOneLocalCompanionAndSurvivesReload() {
        try (ActivityScenario<Activity> scenario = ActivityScenario.launch(intent(GroupActivity.class))) {
            scenario.onActivity(activity -> activity.findViewById(R.id.button_group_join).performClick());
            onView(withId(R.id.input_group_value)).inRoot(isDialog()).perform(replaceText("trip-demo"));
            clickDialogButton(android.R.id.button1);
        }
        reloadStore();
        assertEquals(Arrays.asList("You", "Sam (sample)"), PrototypeStore.get().members(tripId));
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void photoOnlyEntrySurvivesStoreReloadAndFollowsItsStop() throws Exception {
        String filename = "Trippings-journal-" + tripId.substring(0, 8) + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TrippingsStep9/");
        Uri image = context.getContentResolver().insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
        assertNotNull(image);
        Uri grantedPhoto = null;
        try {
            Bitmap fixture = Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888);
            try (OutputStream stream = context.getContentResolver().openOutputStream(image)) {
                assertNotNull(stream);
                assertTrue(fixture.compress(Bitmap.CompressFormat.JPEG, 85, stream));
            } finally {
                fixture.recycle();
            }
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            context.getContentResolver().update(image, values, null, null);

            Intent launch = intent(JournalActivity.class)
                    .putExtra(JournalActivity.EXTRA_JOURNAL_STOP, stopId);
            try (ActivityScenario<Activity> scenario = ActivityScenario.launch(launch)) {
                onView(withId(R.id.input_entry_stop)).inRoot(isDialog())
                        .check(matches(withText("Day 2 · Museum")));
                Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
                Instrumentation.ActivityMonitor initialFolder = new Instrumentation.ActivityMonitor() {
                    @Override
                    public Instrumentation.ActivityResult onStartActivity(Intent picker) {
                        if (Intent.ACTION_OPEN_DOCUMENT.equals(picker.getAction())) {
                            picker.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                                    DocumentsContract.buildDocumentUri(
                                            "com.android.externalstorage.documents",
                                            "primary:Pictures/TrippingsStep9"));
                        }
                        return null;
                    }
                };
                instrumentation.addMonitor(initialFolder);
                try {
                    clickDialogButton(R.id.button_entry_photo);
                    chooseDocument(filename);
                } finally {
                    instrumentation.removeMonitor(initialFolder);
                }
                onView(withId(R.id.text_entry_photo)).inRoot(isDialog())
                        .check(matches(withText(R.string.journal_photo_local)));
                scenario.recreate();
                onView(withId(R.id.text_entry_photo)).inRoot(isDialog())
                        .check(matches(withText(R.string.journal_photo_local)));
                clickDialogButton(android.R.id.button1);
            }

            JournalEntry saved = PrototypeStore.get().journal(tripId).get(0);
            assertEquals("", saved.getNote());
            assertEquals(stopId, saved.getStopId());
            assertNotNull(saved.getPhotoUri());
            grantedPhoto = Uri.parse(saved.getPhotoUri());
            assertTrue(context.getContentResolver().getPersistedUriPermissions().stream()
                    .anyMatch(grant -> grant.isReadPermission() && grant.getUri().toString().equals(saved.getPhotoUri())));

            database.itineraryItemDao().updateDayAndPosition(stopId, 1, 0);
            reloadStore();
            try (ActivityScenario<Activity> scenario = ActivityScenario.launch(intent(JournalActivity.class))) {
                awaitCard(scenario, "Day 1 · Museum", true);
                database.itineraryItemDao().deleteById(stopId);
                awaitCard(scenario, "Day 2", true);
            }
        } finally {
            if (grantedPhoto != null) {
                context.getContentResolver().releasePersistableUriPermission(
                        grantedPhoto, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            context.getContentResolver().delete(image, null, null);
        }
    }

    private void reloadStore() {
        PrototypeStore.resetForTesting();
        PrototypeStore.init(isolated);
    }

    private Intent intent(Class<? extends Activity> screen) {
        return new Intent(context, screen).putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
    }

    private static void chooseDocument(String filename) {
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        boolean[] clicked = {false};
        boolean returned = await(() -> {
            AccessibilityNodeInfo root = automation.getRootInActiveWindow();
            if (root == null) {
                return false;
            }
            String pkg = String.valueOf(root.getPackageName());
            if (pkg.equals("com.itismob.s01.group01.tripfit")) {
                return clicked[0];
            }
            if (!pkg.contains("documentsui")) {
                return false;
            }
            // List view exposes the filename itself. Grid view exposes the preview affordance.
            for (AccessibilityNodeInfo name : root.findAccessibilityNodeInfosByText(filename)) {
                if (filename.contentEquals(String.valueOf(name.getText()))) {
                    for (AccessibilityNodeInfo row = name; row != null; row = row.getParent()) {
                        if (row.isClickable() && row.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            clicked[0] = true;
                            return false;
                        }
                    }
                }
            }
            for (AccessibilityNodeInfo control : root.findAccessibilityNodeInfosByText("List view")) {
                if (control.isClickable()) {
                    control.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                }
            }
            return false;
        });
        if (!returned) {
            capturePicker(automation);
        }
        assertTrue("The document picker did not return the selected photo", returned);
    }

    private static void capturePicker(UiAutomation automation) {
        String artifacts = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        if (artifacts == null) {
            return;
        }
        Bitmap bitmap = automation.takeScreenshot();
        if (bitmap != null) {
            try (OutputStream out = new FileOutputStream(new File(artifacts, "step9-picker.png"))) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException ignored) {
                // Diagnostics must not replace the picker assertion.
            } finally {
                bitmap.recycle();
            }
        }
    }

    private static void awaitCard(ActivityScenario<Activity> scenario, String title, boolean withPhoto) {
        assertTrue("Journal entry did not render " + title, await(() -> {
            boolean[] ready = {false};
            scenario.onActivity(activity -> {
                TextView heading = activity.findViewById(R.id.text_journal_title);
                ImageView photo = activity.findViewById(R.id.image_journal_photo);
                Bitmap bitmap = photo != null && photo.getDrawable() instanceof BitmapDrawable
                        ? ((BitmapDrawable) photo.getDrawable()).getBitmap() : null;
                ready[0] = heading != null && title.contentEquals(heading.getText())
                        && (!withPhoto || bitmap != null
                        && bitmap.getWidth() <= 1200 && bitmap.getHeight() <= 1200);
            });
            return ready[0];
        }));
    }

    private static boolean await(java.util.function.BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + 6000;
        do {
            if (condition.getAsBoolean()) {
                return true;
            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        return false;
    }

    private static void clickDialogButton(int id) {
        onView(withId(id)).inRoot(isDialog()).perform(new ViewAction() {
            @Override public Matcher<View> getConstraints() { return any(View.class); }
            @Override public String getDescription() { return "Click the dialog control"; }
            @Override public void perform(UiController ui, View view) { assertTrue(view.performClick()); }
        });
    }
}
