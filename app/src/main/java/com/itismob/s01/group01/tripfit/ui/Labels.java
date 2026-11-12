package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;

import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.TravelMode;

/** Maps prototype enums to their localized display strings. */
public final class Labels {

    private Labels() { }

    public static String mode(Context context, TravelMode mode) {
        switch (mode) {
            case WALK:
                return context.getString(R.string.mode_walk);
            case CYCLE:
                return context.getString(R.string.mode_cycle);
            case DRIVE:
                return context.getString(R.string.mode_drive);
            default:
                return context.getString(R.string.mode_transit);
        }
    }

    public static String category(Context context, Category category) {
        switch (category) {
            case FOOD:
                return context.getString(R.string.cat_food);
            case ACTIVITIES:
                return context.getString(R.string.cat_activities);
            case TRANSPORT:
                return context.getString(R.string.cat_transport);
            default:
                return context.getString(R.string.cat_other);
        }
    }
}
