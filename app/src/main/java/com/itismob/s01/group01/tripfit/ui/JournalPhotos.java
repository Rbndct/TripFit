package com.itismob.s01.group01.tripfit.ui;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/** Loads a bounded preview from a journal photo's document URI. Call off the main thread. */
public final class JournalPhotos {
    private static final int MAX_EDGE = 1200;

    private JournalPhotos() { }

    @Nullable
    public static Bitmap decode(ContentResolver resolver, Uri photo) throws IOException {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, photo),
                        (decoder, info, source) -> {
                            int width = info.getSize().getWidth();
                            int height = info.getSize().getHeight();
                            int edge = Math.max(width, height);
                            if (edge > MAX_EDGE) {
                                double scale = (double) MAX_EDGE / edge;
                                decoder.setTargetSize(Math.max(1, (int) Math.round(width * scale)),
                                        Math.max(1, (int) Math.round(height * scale)));
                            }
                        });
            } catch (ImageDecoder.DecodeException error) {
                if (error.getError() == ImageDecoder.DecodeException.SOURCE_EXCEPTION) {
                    throw error;
                }
                return null;
            }
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = open(resolver, photo)) {
            BitmapFactory.decodeStream(input, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null;
        }
        int edge = Math.max(options.outWidth, options.outHeight);
        options.inSampleSize = 1;
        while (Math.ceil((double) edge / options.inSampleSize) > MAX_EDGE) {
            options.inSampleSize *= 2;
        }
        options.inJustDecodeBounds = false;
        try (InputStream input = open(resolver, photo)) {
            return BitmapFactory.decodeStream(input, null, options);
        }
    }

    private static InputStream open(ContentResolver resolver, Uri photo) throws IOException {
        InputStream input = resolver.openInputStream(photo);
        if (input == null) {
            throw new FileNotFoundException("Photo document is unavailable");
        }
        return input;
    }
}
