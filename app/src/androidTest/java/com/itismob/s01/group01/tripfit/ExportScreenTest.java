package com.itismob.s01.group01.tripfit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.FileObserver;
import android.os.ParcelFileDescriptor;
import android.view.View;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ExportScreenTest {
    private Context context;
    private TripFitDatabase database;
    private String tripId;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        database = TripFitDatabase.getInstance(context);
        tripId = UUID.randomUUID().toString();
        database.tripDao().insert(new TripEntity(tripId, null, "Export fixture", null, null, null, null));
        database.itineraryItemDao().insert(new ItineraryItemEntity(UUID.randomUUID().toString(),
                tripId, null, "Museum, gallery", 1, null, 60, false, null, null, 0));
        PrototypeStore.resetForTesting();
        PrototypeStore.get().setMeta(tripId, new TripMeta("Manila", "", 2, "PHP", "2026-12-01"));
    }

    @After
    public void tearDown() {
        database.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM trips WHERE id = ?", new Object[]{tripId});
        PrototypeStore.init(context);
    }

    @Test
    public void calendarScreenWritesImportableEventsToThePickedDocument() throws Exception {
        File file = pickFile(CalendarSyncActivity.class, R.id.button_save_calendar, "text/calendar", ".ics");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            assertEquals("BEGIN:VCALENDAR", reader.readLine());
            boolean foundTime = false;
            boolean foundTitle = false;
            for (String line; (line = reader.readLine()) != null; ) {
                foundTime |= "DTSTART:20261201T010000Z".equals(line);
                foundTitle |= "SUMMARY:Museum\\, gallery".equals(line);
            }
            assertTrue("Saved Manila event should start at 09:00 local time", foundTime);
            assertTrue("Saved event title should escape the comma", foundTitle);
        } finally {
            file.delete();
        }
    }

    @Test
    public void summaryScreenWritesRenderablePdfToThePickedDocument() throws Exception {
        File file = pickFile(SummaryActivity.class, R.id.button_export_pdf, "application/pdf", ".pdf");
        try (ParcelFileDescriptor input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer pdf = new PdfRenderer(input);
             PdfRenderer.Page page = pdf.openPage(0)) {
            assertTrue(pdf.getPageCount() > 0);
            Bitmap bitmap = Bitmap.createBitmap(1190, 1684, Bitmap.Config.ARGB_8888);
            try {
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                String artifacts = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
                File directory = artifacts == null ? context.getCacheDir() : new File(artifacts);
                assertTrue(directory.isDirectory() || directory.mkdirs());
                try (FileOutputStream image = new FileOutputStream(
                        new File(directory, "step8-summary-qa.png"))) {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, image));
                }
            } finally {
                bitmap.recycle();
            }
        } finally {
            file.delete();
        }
    }

    private File pickFile(Class<? extends Activity> screen, int buttonId, String mimeType,
                          String extension) throws Exception {
        File directory = new File(context.getCacheDir(), "pdfs");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        File file = File.createTempFile("export-fixture-", extension, directory);
        Intent result = new Intent().setData(FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file));
        IntentFilter filter = new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);
        filter.addCategory(Intent.CATEGORY_OPENABLE);
        filter.addDataType(mimeType);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor picker = instrumentation.addMonitor(filter,
                new Instrumentation.ActivityResult(Activity.RESULT_OK, result), true);
        CountDownLatch written = new CountDownLatch(1);
        FileObserver observer = new FileObserver(file.getPath(), FileObserver.CLOSE_WRITE) {
            @Override
            public void onEvent(int event, String path) {
                written.countDown();
            }
        };
        observer.startWatching();
        Intent launch = new Intent(context, screen).putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
        try (ActivityScenario<Activity> scenario = ActivityScenario.launch(launch)) {
            CountDownLatch ready = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                View button = activity.findViewById(buttonId);
                button.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    if (button.isEnabled()) {
                        ready.countDown();
                    }
                });
                if (button.isEnabled()) {
                    ready.countDown();
                }
            });
            assertTrue("Export button did not become ready", ready.await(5, TimeUnit.SECONDS));
            scenario.onActivity(activity -> activity.findViewById(buttonId).performClick());
            assertTrue("Export did not finish writing " + mimeType, written.await(8, TimeUnit.SECONDS));
            assertEquals(1, picker.getHits());
        } finally {
            observer.stopWatching();
            instrumentation.removeMonitor(picker);
        }
        return file;
    }
}
