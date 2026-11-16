package com.itismob.s01.group01.tripfit;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;

import java.util.List;

/** Account card plus app-wide preferences; Sign out returns to Login with a cleared back stack. */
public class ProfileActivity extends ScreenActivity {

    private static final int[] BUFFER_OPTIONS_MIN = {0, 10, 15, 20, 30};
    private static final String STATE_DIALOG = "open_dialog";
    private static final int DIALOG_NONE = 0;
    private static final int DIALOG_CURRENCY = 1;
    private static final int DIALOG_BUFFER = 2;
    private static final int DIALOG_ABOUT = 3;
    private static final int DIALOG_SIGN_OUT = 4;

    private final PrototypeStore store = PrototypeStore.get();
    private View rowCurrency;
    private View rowBuffer;
    private Dialog shownDialog;
    private int openDialog = DIALOG_NONE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_profile, R.string.profile_title);

        rowCurrency = findViewById(R.id.row_currency);
        rowBuffer = findViewById(R.id.row_buffer);
        bindRow(rowCurrency, R.string.setting_home_currency, R.drawable.ic_expand_more_24,
                R.string.profile_action_choose, v -> pickCurrency());
        bindRow(rowBuffer, R.string.setting_buffer, R.drawable.ic_expand_more_24,
                R.string.profile_action_choose, v -> pickBuffer());
        bindRow(findViewById(R.id.row_about), R.string.setting_about, R.drawable.ic_chevron_right_24,
                R.string.profile_action_open, v -> showAbout());
        renderCurrency();
        renderBuffer();

        MaterialSwitch dark = findViewById(R.id.switch_dark_mode);
        dark.setChecked(isDarkNow());
        dark.setOnCheckedChangeListener((button, checked) -> AppCompatDelegate.setDefaultNightMode(
                checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO));

        MaterialSwitch reminders = findViewById(R.id.switch_reminders);
        reminders.setOnCheckedChangeListener((button, checked) ->
                toast(checked ? R.string.reminders_on : R.string.reminders_off));

        findViewById(R.id.button_sign_out).setOnClickListener(v -> confirmSignOut());

        if (savedInstanceState != null) {
            reopenDialog(savedInstanceState.getInt(STATE_DIALOG, DIALOG_NONE));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_DIALOG, openDialog);
    }

    @Override
    protected void onDestroy() {
        if (shownDialog != null) {
            shownDialog.dismiss();
        }
        super.onDestroy();
    }

    /** True when the screen is actually drawn dark, including when the system theme drives it. */
    private boolean isDarkNow() {
        int mask = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mask == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Titles the row, adds a trailing affordance and names the tap action for TalkBack. */
    private void bindRow(View row, @StringRes int title, @DrawableRes int trailingIcon,
                         @StringRes int actionLabel, View.OnClickListener click) {
        ((TextView) row.findViewById(R.id.text_setting_title)).setText(title);
        TextView value = row.findViewById(R.id.text_setting_value);
        value.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, trailingIcon, 0);
        value.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.space_xs));
        ViewCompat.replaceAccessibilityAction(row,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getString(actionLabel), null);
        row.setOnClickListener(click);
    }

    private void renderCurrency() {
        String code = store.getDefaultHomeCurrency();
        ((TextView) rowCurrency.findViewById(R.id.text_setting_value)).setText(code);
        rowCurrency.setContentDescription(
                getString(R.string.profile_row_cd, getString(R.string.setting_home_currency), code));
    }

    private void renderBuffer() {
        int minutes = store.getBufferMin();
        ((TextView) rowBuffer.findViewById(R.id.text_setting_value)).setText(bufferLabel(minutes));
        String spoken = minutes == 0 ? getString(R.string.buffer_none)
                : getResources().getQuantityString(R.plurals.buffer_minutes_spoken, minutes, minutes);
        rowBuffer.setContentDescription(
                getString(R.string.profile_row_cd, getString(R.string.setting_buffer), spoken));
    }

    /** Shows a dialog and remembers which one, so it can be reopened after rotation. */
    private void present(int kind, MaterialAlertDialogBuilder builder) {
        openDialog = kind;
        shownDialog = builder.setOnDismissListener(d -> {
            if (shownDialog == d) {
                shownDialog = null;
                openDialog = DIALOG_NONE;
            }
        }).show();
    }

    private void reopenDialog(int kind) {
        if (kind == DIALOG_CURRENCY) {
            pickCurrency();
        } else if (kind == DIALOG_BUFFER) {
            pickBuffer();
        } else if (kind == DIALOG_ABOUT) {
            showAbout();
        } else if (kind == DIALOG_SIGN_OUT) {
            confirmSignOut();
        }
    }

    private void pickCurrency() {
        List<String> codes = CurrencyRates.codes();
        int checked = codes.indexOf(store.getDefaultHomeCurrency());
        String[] labels = new String[codes.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = currencyLabel(codes.get(i));
        }
        present(DIALOG_CURRENCY, new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_home_currency)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    store.setDefaultHomeCurrency(codes.get(which));
                    renderCurrency();
                    toast(getString(R.string.profile_currency_set, codes.get(which)));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null));
    }

    /** "JPY · Japanese Yen"; just the code when no name is known (offline fallback table). */
    private String currencyLabel(String code) {
        String name = CurrencyRates.nameOf(code);
        return name.equals(code) ? code : getString(R.string.currency_option, code, name);
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
        present(DIALOG_BUFFER, new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_buffer)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    store.setBufferMin(BUFFER_OPTIONS_MIN[which]);
                    renderBuffer();
                    toast(bufferSetMessage(BUFFER_OPTIONS_MIN[which]));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null));
    }

    private String bufferSetMessage(int minutes) {
        return minutes == 0 ? getString(R.string.profile_buffer_none_set)
                : getResources().getQuantityString(R.plurals.profile_buffer_set, minutes, minutes);
    }

    private String bufferLabel(int minutes) {
        return minutes == 0 ? getString(R.string.buffer_none)
                : getString(R.string.buffer_minutes, minutes);
    }

    private void showAbout() {
        present(DIALOG_ABOUT, new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_about)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.action_close, null));
    }

    private void confirmSignOut() {
        present(DIALOG_SIGN_OUT, new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_sign_out, (dialog, which) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }));
    }
}
