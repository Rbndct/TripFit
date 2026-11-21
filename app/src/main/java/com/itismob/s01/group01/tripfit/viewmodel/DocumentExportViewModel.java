package com.itismob.s01.group01.tripfit.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.data.AppExecutors;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshot;
import com.itismob.s01.group01.tripfit.prototype.TripExport;
import com.itismob.s01.group01.tripfit.ui.ExportNames;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Retains the selected export and its result while the picker is open or a file is being written. */
public final class DocumentExportViewModel extends AndroidViewModel {
    public enum Format { CALENDAR, PDF }

    public static final class Outcome {
        public final int message;
        public final Uri sharedPdf;
        public final String tripName;

        private Outcome(int message, Uri sharedPdf, String tripName) {
            this.message = message;
            this.sharedPdf = sharedPdf;
            this.tripName = tripName;
        }
    }

    private final AppExecutors executors = AppExecutors.getInstance();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Outcome> outcome = new MutableLiveData<>();
    private TripSnapshot pickedTrip;
    private Uri destination;

    public DocumentExportViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> busy() { return busy; }
    public LiveData<Outcome> outcome() { return outcome; }
    public boolean isBusy() { return Boolean.TRUE.equals(busy.getValue()); }

    public void consumeOutcome() { outcome.setValue(null); }

    public void beginPicker(TripSnapshot snapshot) {
        pickedTrip = snapshot;
        busy.setValue(true);
    }

    public void onDocumentPicked(@Nullable Uri uri) {
        destination = uri;
        if (uri == null) {
            pickedTrip = null;
        }
        busy.setValue(uri != null);
    }

    public void writePickedDocument(@Nullable TripSnapshot current, Format format) {
        TripSnapshot snapshot = pickedTrip == null ? current : pickedTrip;
        if (destination == null || snapshot == null) {
            return;
        }
        Uri target = destination;
        destination = null;
        pickedTrip = null;
        executors.diskIO().execute(() -> {
            try {
                writeDocument(target, snapshot, format);
                finish(new Outcome(format == Format.CALENDAR ? R.string.calendar_file_saved
                        : R.string.summary_file_saved, null, null));
            } catch (IOException | RuntimeException error) {
                reportFailure(error);
            }
        });
    }

    private void writeDocument(Uri target, TripSnapshot snapshot, Format format) throws IOException {
        byte[] calendar = null;
        if (format == Format.CALENDAR) {
            if (snapshot.stops.isEmpty()) {
                throw new IOException("Calendar itinerary is empty");
            }
            calendar = TripExport.calendar(snapshot).getBytes(StandardCharsets.UTF_8);
        }
        try (OutputStream output = getApplication().getContentResolver().openOutputStream(target, "wt")) {
            if (output == null) {
                throw new IOException("Document provider returned no stream");
            }
            if (calendar != null) {
                output.write(calendar);
            } else {
                TripExport.pdf(snapshot, output);
            }
        }
    }

    public void sharePdf(TripSnapshot snapshot) {
        busy.setValue(true);
        executors.diskIO().execute(() -> {
            try {
                Application app = getApplication();
                File directory = new File(app.getCacheDir(), "pdfs");
                if (!directory.isDirectory() && !directory.mkdirs()) {
                    throw new IOException("Could not create the PDF cache");
                }
                File file = File.createTempFile(ExportNames.safeName(snapshot.name) + "-", ".pdf", directory);
                try (FileOutputStream output = new FileOutputStream(file)) {
                    TripExport.pdf(snapshot, output);
                }
                Uri uri = FileProvider.getUriForFile(app, app.getPackageName() + ".fileprovider", file);
                finish(new Outcome(0, uri, snapshot.name));
            } catch (IOException | RuntimeException error) {
                reportFailure(error);
            }
        });
    }

    private void reportFailure(Exception error) {
        Log.w("DocumentExport", "Could not write trip export", error);
        int message = error instanceof IllegalArgumentException
                && "Invalid trip date".equals(error.getMessage()) ? R.string.calendar_invalid_date
                : R.string.export_save_failed;
        finish(new Outcome(message, null, null));
    }

    private void finish(Outcome value) {
        executors.mainThread().execute(() -> {
            busy.setValue(false);
            outcome.setValue(value);
        });
    }
}
