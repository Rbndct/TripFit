package com.itismob.s01.group01.tripfit.ui;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;

import java.util.function.Consumer;

/** Shows the shared 24h Material time picker and reports the choice as "HH:mm". */
public final class TimeFields {

    private static final int DEFAULT_HOUR = 9;

    private TimeFields() { }

    /** {@code initial} is an "HH:mm" string or null (defaults to 09:00). */
    public static void pick(AppCompatActivity activity, String initial, Consumer<String> onPicked) {
        int minuteOfDay = initial == null ? DEFAULT_HOUR * 60 : ScheduleMath.parse(initial);
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(minuteOfDay / 60)
                .setMinute(minuteOfDay % 60)
                .setTitleText(R.string.select_time)
                .build();
        picker.addOnPositiveButtonClickListener(v ->
                onPicked.accept(ScheduleMath.format(picker.getHour() * 60 + picker.getMinute())));
        picker.show(activity.getSupportFragmentManager(), "time_picker");
    }
}
