package com.itismob.s01.group01.tripfit.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.TripFitApp;
import com.itismob.s01.group01.tripfit.prototype.StopFormRules;
import com.itismob.s01.group01.tripfit.prototype.TravelMode;

/**
 * Edits one leg (mode and buffer of the stop it arrives at). A DialogFragment so the choice and
 * any half-typed buffer survive rotation; the buffer error shows in the field and clears on edit.
 */
public final class LegSettingsDialog extends DialogFragment {

    private static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_MODE = "mode";
    private static final String ARG_BUFFER = "buffer";
    private static final String ARG_DEFAULT_BUFFER = "default_buffer";

    private TextInputLayout layoutBuffer;
    private TextInputEditText inputBuffer;
    private RadioGroup groupMode;
    private boolean saving;

    /**
     * {@code explicitMode} null = Suggested; {@code explicitBuffer} null = follows the profile
     * default ({@code defaultBuffer}, shown as a hint), so saving it untouched keeps null.
     */
    public static LegSettingsDialog newInstance(String itemId, String stopName,
                                                @Nullable TravelMode explicitMode,
                                                @Nullable Integer explicitBuffer,
                                                int defaultBuffer) {
        LegSettingsDialog dialog = new LegSettingsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ITEM_ID, itemId);
        args.putString(ARG_NAME, stopName);
        args.putString(ARG_MODE, explicitMode == null ? null : explicitMode.name());
        if (explicitBuffer != null) {
            args.putInt(ARG_BUFFER, explicitBuffer);
        }
        args.putInt(ARG_DEFAULT_BUFFER, defaultBuffer);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        View view = getLayoutInflater().inflate(R.layout.dialog_leg_settings, null);
        layoutBuffer = view.findViewById(R.id.layout_buffer);
        inputBuffer = view.findViewById(R.id.input_buffer);
        groupMode = view.findViewById(R.id.group_mode);
        if (savedInstanceState == null) {
            groupMode.check(radioFor(modeNamed(args.getString(ARG_MODE))));
            inputBuffer.setText(StopFormRules.bufferFieldText(args.containsKey(ARG_BUFFER)
                    ? Integer.valueOf(args.getInt(ARG_BUFFER)) : null));
        }
        layoutBuffer.setPlaceholderText(String.valueOf(args.getInt(ARG_DEFAULT_BUFFER)));
        inputBuffer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                layoutBuffer.setError(null);
            }

            @Override public void afterTextChanged(Editable s) { }
        });
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.leg_dialog_title, args.getString(ARG_NAME)))
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();
        // Overridden after show() so an invalid buffer keeps the dialog open.
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> save(dialog)));
        return dialog;
    }

    private void save(AlertDialog dialog) {
        if (saving) {
            return;
        }
        String text = inputBuffer.getText() == null ? "" : inputBuffer.getText().toString();
        if (!StopFormRules.isBufferValid(text)) {
            layoutBuffer.setError(getString(R.string.error_buffer_range));
            inputBuffer.requestFocus();
            return;
        }
        saving = true;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        TravelMode mode = modeFor(groupMode.getCheckedRadioButtonId());
        ((TripFitApp) requireActivity().getApplication()).getTripRepository().updateLegSettings(
                requireArguments().getString(ARG_ITEM_ID), StopFormRules.bufferOrNull(text),
                mode == null ? null : mode.name(), result -> {
                    saving = false;
                    if (!isAdded()) {
                        return; // rotated or closed meanwhile; the write already landed or failed
                    }
                    if (result.isSuccess()) {
                        dismissAllowingStateLoss();
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        layoutBuffer.setError(getString(R.string.error_update_failed));
                    }
                });
    }

    /** Parses the stored mode name; unknown or null means Suggested. */
    @Nullable
    private static TravelMode modeNamed(@Nullable String name) {
        try {
            return name == null ? null : TravelMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int radioFor(@Nullable TravelMode mode) {
        if (mode == null) {
            return R.id.radio_suggested;
        }
        switch (mode) {
            case WALK: return R.id.radio_walk;
            case CYCLE: return R.id.radio_cycle;
            case DRIVE: return R.id.radio_drive;
            default: return R.id.radio_transit;
        }
    }

    @Nullable
    private static TravelMode modeFor(int radioId) {
        if (radioId == R.id.radio_walk) {
            return TravelMode.WALK;
        } else if (radioId == R.id.radio_cycle) {
            return TravelMode.CYCLE;
        } else if (radioId == R.id.radio_drive) {
            return TravelMode.DRIVE;
        } else if (radioId == R.id.radio_transit) {
            return TravelMode.TRANSIT;
        }
        return null;
    }
}
