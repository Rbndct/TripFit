package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.annotation.Nullable;

/** LiveData payload that is delivered once, so a rotation does not replay it. */
public final class Event<T> {

    private final T content;
    private boolean consumed;

    public Event(T content) {
        this.content = content;
    }

    /** The content the first time, null afterwards. */
    @Nullable
    public synchronized T consume() {
        if (consumed) {
            return null;
        }
        consumed = true;
        return content;
    }

    public T peek() {
        return content;
    }
}
