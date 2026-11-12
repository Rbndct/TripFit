package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.itismob.s01.group01.tripfit.prototype.CurrencyMock;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;

import java.util.List;

/** Account card plus app-wide preferences; Sign out returns to Login with a cleared back stack. */
public class ProfileActivity extends ScreenActivity {

    private static final int[] BUFFER_OPTIONS_MIN = {0, 10, 15, 20, 30};

    private final PrototypeStore store = PrototypeStore.get();
    private View rowCurrency;
    private View rowBuffer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_profile, R.string.profile_title);

        rowCurrency = findViewById(R.id.row_currency);
        rowBuffer = findViewById(R.id.row_buffer);
        bindRow(rowCurrency, R.string.setting_home_currency, store.getDefaultHomeCurrency(),
                v -> pickCurrency());
        bindRow(rowBuffer, R.string.setting_buffer, bufferLabel(store.getBufferMin()),
                v -> pickBuffer());
        bindRow(findViewById(R.id.row_about), R.string.setting_about, "", v -> showAbout());

        MaterialSwitch dark = findViewById(R.id.switch_dark_mode);
        dark.setChecked(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        dark.setOnCheckedChangeListener((button, checked) -> AppCompatDelegate.setDefaultNightMode(
                checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO));

        MaterialSwitch reminders = findViewById(R.id.switch_reminders);
        reminders.setOnCheckedChangeListener((button, checked) ->
                toast(checked ? R.string.reminders_on : R.string.reminders_off));

        findViewById(R.id.button_sign_out).setOnClickListener(v -> confirmSignOut());
    }

    private void bindRow(View row, @StringRes int title, String value, View.OnClickListener click) {
        ((TextView) row.findViewById(R.id.text_setting_title)).setText(title);
        ((TextView) row.findViewById(R.id.text_setting_value)).setText(value);
        row.setOnClickListener(click);
    }

    private void pickCurrency() {
        List<String> codes = CurrencyMock.codes();
        int checked = codes.indexOf(store.getDefaultHomeCurrency());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_home_currency)
                .setSingleChoiceItems(codes.toArray(new String[0]), checked, (dialog, which) -> {
                    store.setDefaultHomeCurrency(codes.get(which));
                    ((TextView) rowCurrency.findViewById(R.id.text_setting_value))
                            .setText(codes.get(which));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void pickBuffer() {
        String[] labels = new String[BUFFER_OPTIONS_MIN.length];
        int checked = 0;
        for (int i = 0; i < BUFFER_OPTIONS_MIN.length; i++) {
            labels[i] = bufferLabel(BUFFER_OPTIONS_MIN[i]);
            if (BUFFER_OPTIONS_MIN[i] == store.getBufferMin()) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_buffer)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    store.setBufferMin(BUFFER_OPTIONS_MIN[which]);
                    ((TextView) rowBuffer.findViewById(R.id.text_setting_value))
                            .setText(labels[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private String bufferLabel(int minutes) {
        return minutes == 0 ? getString(R.string.buffer_none)
                : getString(R.string.buffer_minutes, minutes);
    }

    private void showAbout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_about)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.action_close, null)
                .show();
    }

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_sign_out, (dialog, which) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }
}
