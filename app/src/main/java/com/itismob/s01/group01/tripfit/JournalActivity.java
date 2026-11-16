package com.itismob.s01.group01.tripfit;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.JournalEntry;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.List;

/** Lightweight trip journal: notes and photo markers attached to a stop or the trip generally. */
public class JournalActivity extends ScreenActivity {

    private static final String STATE_DRAFT = "entry_draft";
    private static final String KEY_NOTE = "entry_note";
    private static final String KEY_STOP = "entry_stop";
    private static final String KEY_PHOTO = "entry_photo";
    /** Day value stored for entries that belong to the trip rather than a stop. */
    private static final int DAY_GENERAL = 0;
    private static final int STOP_GENERAL = -1;

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private List<ItineraryItemWithPlace> stops = new ArrayList<>();
    private ActivityResultLauncher<PickVisualMediaRequest> photoPicker;
    private RecyclerView list;
    private AlertDialog entryDialog;
    private MaterialButton dialogPhotoButton;
    private Bundle pendingDraft;
    private boolean photoPicked;
    private int chosenStop = STOP_GENERAL;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_journal, R.string.journal_title);
        list = findViewById(R.id.list_journal);
        list.setAdapter(adapter);

        photoPicker = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                (Uri uri) -> {
                    if (uri != null) {
                        photoPicked = true;
                        showPhotoState();
                    }
                });
        findViewById(R.id.fab_add_entry).setOnClickListener(v -> showAddDialog(null));
        if (savedInstanceState != null) {
            pendingDraft = savedInstanceState.getBundle(STATE_DRAFT);
        }

        ((TripFitApp) getApplication()).getTripRepository()
                .observeItineraryItemsWithPlaceNames(tripId)
                .observe(this, items -> {
                    stops = items;
                    reopenPendingDraft();
                });
        render();
    }

    /** Stops load asynchronously; a rotated draft reopens once its stop list is available. */
    private void reopenPendingDraft() {
        if (pendingDraft != null) {
            Bundle draft = pendingDraft;
            pendingDraft = null;
            showAddDialog(draft);
        }
    }

    private void render() {
        List<UiRow> rows = new ArrayList<>();
        for (JournalEntry entry : store.journal(tripId)) {
            String sub = entry.getDay() == DAY_GENERAL ? entry.getStopName()
                    : getString(R.string.journal_entry_sub, entry.getDay(), entry.getStopName());
            rows.add(UiRow.plain(entry.getNote(),
                    entry.hasPhoto() ? sub + " · " + getString(R.string.journal_photo_tag) : sub));
        }
        adapter.submit(rows);
        findViewById(R.id.text_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Opens Add entry (once at a time), optionally restoring a draft after rotation. */
    private void showAddDialog(@Nullable Bundle draft) {
        if (entryDialog != null) {
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_journal, null);
        TextInputLayout layoutNote = view.findViewById(R.id.layout_entry_note);
        TextInputEditText inputNote = view.findViewById(R.id.input_entry_note);
        chosenStop = STOP_GENERAL;
        photoPicked = false;
        if (draft != null) {
            int stop = draft.getInt(KEY_STOP, STOP_GENERAL);
            chosenStop = stop < stops.size() ? stop : STOP_GENERAL;
            photoPicked = draft.getBoolean(KEY_PHOTO, false);
            inputNote.setText(draft.getString(KEY_NOTE, ""));
        }
        bindStopField(view.findViewById(R.id.input_entry_stop));
        bindPhotoButton(view.findViewById(R.id.button_entry_photo));
        inputNote.addTextChangedListener(new ErrorClearingWatcher(layoutNote));
        inputNote.requestFocus();

        entryDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_add_entry)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save_entry, null)
                .create();
        entryDialog.setOnDismissListener(d -> {
            entryDialog = null;
            dialogPhotoButton = null;
        });
        entryDialog.setOnShowListener(d -> entryDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> submit(layoutNote, inputNote)));
        if (entryDialog.getWindow() != null) {
            entryDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        entryDialog.show();
    }

    private void bindStopField(MaterialAutoCompleteTextView inputStop) {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.journal_general));
        for (ItineraryItemWithPlace entry : stops) {
            labels.add(getString(R.string.journal_entry_sub, entry.item.getDay(),
                    entry.item.displayName(entry.placeName)));
        }
        inputStop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        inputStop.setText(labels.get(chosenStop + 1), false);
        inputStop.setOnItemClickListener((parent, v, position, id) -> chosenStop = position - 1);
    }

    private void bindPhotoButton(MaterialButton button) {
        dialogPhotoButton = button;
        showPhotoState();
        button.setOnClickListener(v -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
    }

    /** Text plus a check icon, so "attached" never relies on colour alone. */
    private void showPhotoState() {
        if (dialogPhotoButton == null) {
            return;
        }
        dialogPhotoButton.setText(photoPicked ? R.string.photo_attached_change
                : R.string.action_pick_photo);
        dialogPhotoButton.setIconResource(photoPicked ? R.drawable.ic_check_24 : 0);
    }

    /** Validates the note; on failure shows the error and keeps focus on the field. */
    private void submit(TextInputLayout layoutNote, TextInputEditText inputNote) {
        if (entryDialog == null || !entryDialog.isShowing()) {
            return;
        }
        String note = inputNote.getText() == null ? "" : inputNote.getText().toString().trim();
        if (TextUtils.isEmpty(note)) {
            layoutNote.setError(getString(R.string.error_note_required));
            inputNote.requestFocus();
            return;
        }
        saveEntry(note);
        entryDialog.dismiss();
    }

    private void saveEntry(String note) {
        int day = DAY_GENERAL;
        String stopName = getString(R.string.journal_general);
        if (chosenStop >= 0 && chosenStop < stops.size()) {
            ItineraryItemWithPlace entry = stops.get(chosenStop);
            day = entry.item.getDay();
            stopName = entry.item.displayName(entry.placeName);
        }
        store.addJournalEntry(tripId, new JournalEntry(day, stopName, note, photoPicked));
        render();
        list.scrollToPosition(0);
        String where = day == DAY_GENERAL ? stopName
                : getString(R.string.journal_entry_sub, day, stopName);
        // Attach to the CoordinatorLayout root (not the window content) so the FAB anchor stays correct.
        Snackbar.make(findViewById(R.id.journal_root),
                getString(R.string.journal_saved_to, where), Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.fab_add_entry).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle draft = null;
        if (entryDialog != null && entryDialog.isShowing()) {
            draft = draftOf(entryDialog);
        } else if (pendingDraft != null) {
            draft = pendingDraft;
        }
        if (draft != null) {
            outState.putBundle(STATE_DRAFT, draft);
        }
    }

    @Nullable
    private Bundle draftOf(AlertDialog dialog) {
        TextInputEditText note = dialog.findViewById(R.id.input_entry_note);
        if (note == null) {
            return null;
        }
        Bundle draft = new Bundle();
        draft.putString(KEY_NOTE, String.valueOf(note.getText()));
        draft.putInt(KEY_STOP, chosenStop);
        draft.putBoolean(KEY_PHOTO, photoPicked);
        return draft;
    }

    @Override
    protected void onDestroy() {
        if (entryDialog != null) {
            entryDialog.dismiss();
        }
        super.onDestroy();
    }

    /** Clears a field's error as soon as the user edits it. */
    private static final class ErrorClearingWatcher implements TextWatcher {
        private final TextInputLayout layout;

        ErrorClearingWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            layout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) { }
    }
}
