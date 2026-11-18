package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s01.group01.tripfit.R;

import java.util.function.IntConsumer;

public final class CatalogDayPicker {
    public static AlertDialog show(Context context, int days, int selected, IntConsumer onSelected) {
        String[] labels = new String[days];
        for (int i = 0; i < days; i++) {
            labels[i] = context.getString(R.string.catalog_day, i + 1);
        }
        return new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.catalog_choose_day)
                .setSingleChoiceItems(labels, selected - 1, (dialog, index) -> {
                    dialog.dismiss();
                    onSelected.accept(index + 1);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private CatalogDayPicker() { }
}
