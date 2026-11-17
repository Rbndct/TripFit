package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Holds the "a write is in flight" flag and its one-shot outcome for the optimize screen, so both
 * survive rotation: buttons stay disabled while a write runs and the result still reaches the
 * rotated activity.
 */
public final class OptimizeWriteViewModel extends ViewModel {

    /** How a write ended; {@code successMessage} is the string to toast on success. */
    public static final class Outcome {
        public final boolean success;
        public final int successMessage;

        Outcome(boolean success, int successMessage) {
            this.success = success;
            this.successMessage = successMessage;
        }
    }

    private final MutableLiveData<Event<Outcome>> outcome = new MutableLiveData<>();
    private boolean busy;

    public boolean isBusy() {
        return busy;
    }

    public LiveData<Event<Outcome>> getOutcome() {
        return outcome;
    }

    /** Marks a write as started; false (and no change) if one is already running. */
    public boolean begin() {
        if (busy) {
            return false;
        }
        busy = true;
        return true;
    }

    /** Ends the write. Stays busy on success because the screen closes right after. */
    public void finish(boolean success, int successMessage) {
        busy = success;
        outcome.setValue(new Event<>(new Outcome(success, successMessage)));
    }
}
