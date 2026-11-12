package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Renders plain text lines to a paginated A4 PDF in the app's cache (shared via FileProvider). */
public final class TripPdfWriter {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 48;
    private static final int LINE_HEIGHT = 20;
    private static final float TEXT_SIZE = 12f;
    private static final float TITLE_SIZE = 20f;

    private TripPdfWriter() { }

    /** Writes {@code lines} under a title and returns the file; throws if the cache is unwritable. */
    public static File write(Context context, String title, List<String> lines) throws IOException {
        File dir = new File(context.getCacheDir(), "pdfs");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create " + dir);
        }
        File file = new File(dir, safeName(title) + ".pdf");

        PdfDocument document = new PdfDocument();
        try {
            Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
            body.setTextSize(TEXT_SIZE);
            Paint heading = new Paint(Paint.ANTI_ALIAS_FLAG);
            heading.setTextSize(TITLE_SIZE);
            heading.setFakeBoldText(true);

            int pageNumber = 1;
            PdfDocument.Page page = document.startPage(pageInfo(pageNumber));
            int y = MARGIN;
            page.getCanvas().drawText(title, MARGIN, y, heading);
            y += LINE_HEIGHT * 2;
            for (String line : lines) {
                if (y > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo(++pageNumber));
                    y = MARGIN;
                }
                page.getCanvas().drawText(line, MARGIN, y, body);
                y += LINE_HEIGHT;
            }
            document.finishPage(page);
            try (FileOutputStream out = new FileOutputStream(file)) {
                document.writeTo(out);
            }
        } finally {
            document.close();
        }
        return file;
    }

    private static PdfDocument.PageInfo pageInfo(int number) {
        return new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create();
    }

    static String safeName(String title) {
        String cleaned = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return cleaned.isEmpty() ? "tripfit-itinerary" : "tripfit-" + cleaned;
    }
}
