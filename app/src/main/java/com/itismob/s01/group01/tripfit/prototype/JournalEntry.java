package com.itismob.s01.group01.tripfit.prototype;

import androidx.annotation.Nullable;

import java.util.UUID;

/** A note (and optionally a photo) attached to a day/stop. */
public final class JournalEntry {

    private final String id;
    private final int day;
    private final String stopName;
    @Nullable
    private final String stopId;
    private final String note;
    private final boolean hasPhoto;
    @Nullable
    private final String photoUri;

    /** Legacy form: a photo marker only (no URI), no stop id, fresh id. */
    public JournalEntry(int day, String stopName, String note, boolean hasPhoto) {
        this(UUID.randomUUID().toString(), day, stopName, null, note, hasPhoto, null);
    }

    /** Entry with a real photo URI (or null for none) and an optional stop id. */
    public JournalEntry(String id, int day, String stopName, @Nullable String stopId, String note,
                        @Nullable String photoUri) {
        this(id, day, stopName, stopId, note, photoUri != null, photoUri);
    }

    /** Full form used when reloading from storage. */
    public JournalEntry(String id, int day, String stopName, @Nullable String stopId, String note,
                        boolean hasPhoto, @Nullable String photoUri) {
        this.id = id;
        this.day = day;
        this.stopName = stopName;
        this.stopId = stopId;
        this.note = note;
        this.hasPhoto = hasPhoto;
        this.photoUri = photoUri;
    }

    public String getId() {
        return id;
    }

    public int getDay() {
        return day;
    }

    public String getStopName() {
        return stopName;
    }

    @Nullable
    public String getStopId() {
        return stopId;
    }

    public String getNote() {
        return note;
    }

    public boolean hasPhoto() {
        return hasPhoto;
    }

    @Nullable
    public String getPhotoUri() {
        return photoUri;
    }
}
