package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/** The one place that hides the soft keyboard after a form's primary action. */
public final class Keyboard {

    private Keyboard() { }

    /** Hides the soft keyboard for the window containing {@code view}; no-op for null. */
    public static void hide(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
