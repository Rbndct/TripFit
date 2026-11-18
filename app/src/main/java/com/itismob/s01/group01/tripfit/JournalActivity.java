package com.itismob.s01.group01.tripfit;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.JournalEntry;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.ui.JournalAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local memories linked to the trip, a day, or a stable itinerary stop ID. */
public class JournalActivity extends ScreenActivity {
    public static final String EXTRA_JOURNAL_STOP = "journal_stop_id";
    private static final String STATE_DRAFT = "entry_draft";
    private static final String STATE_REMOVE = "entry_removal";
    private static final String KEY_ID = "entry_id";
    private static final String KEY_NOTE = "entry_note";
    private static final String KEY_DAY = "entry_day";
    private static final String KEY_STOP = "entry_stop_id";
    private static final String KEY_PHOTO = "entry_photo_uri";
    private static final int DAY_GENERAL = 0;

    private final PrototypeStore store = PrototypeStore.get();
    private final JournalAdapter adapter = new JournalAdapter(this::editEntry, this::confirmRemove);
    private List<ItineraryItemWithPlace> stops;
    private int tripDays;
    private Bundle pendingDraft;
    private String pendingRemoval;
    private ActivityResultLauncher<String[]> photoPicker;
    private RecyclerView list;
    private AlertDialog entryDialog;
    private AlertDialog removeDialog;
    private TextInputEditText noteField;
    private MaterialAutoCompleteTextView dayField;
    private MaterialAutoCompleteTextView stopField;
    private TextView photoStatus;
    private View clearPhotoButton;
    private String editingId;
    private int chosenDay;
    private String chosenStopId;
    private String photoUri;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!setScreen(R.layout.activity_journal, R.string.journal_title)) {
            return;
        }
        list = findViewById(R.id.list_journal);
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        list.setAdapter(adapter);
        View add = findViewById(R.id.fab_add_entry);
        add.setEnabled(false);
        add.setOnClickListener(v -> openEntry(new Bundle()));
        if (state != null) {
            pendingDraft = state.getBundle(STATE_DRAFT);
            pendingRemoval = state.getString(STATE_REMOVE);
        } else {
            String stopId = getIntent().getStringExtra(EXTRA_JOURNAL_STOP);
            if (stopId != null) {
                pendingDraft = new Bundle();
                pendingDraft.putString(KEY_STOP, stopId);
            }
        }
        photoPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPhotoPicked);
        ((TripFitApp) getApplication()).getTripRepository().observeItineraryItemsWithPlaceNames(tripId)
                .observe(this, items -> {
                    stops = items;
                    tripDays = store.meta(tripId).getDays();
                    for (ItineraryItemWithPlace stop : stops) {
                        tripDays = Math.max(tripDays, stop.item.getDay());
                    }
                    add.setEnabled(true);
                    render();
                    if (pendingDraft != null) {
                        Bundle draft = pendingDraft;
                        pendingDraft = null;
                        openEntry(draft);
                    } else if (pendingRemoval != null && removeDialog == null) {
                        for (JournalEntry entry : store.journal(tripId)) {
                            if (pendingRemoval.equals(entry.getId())) {
                                confirmRemove(entry);
                                break;
                            }
                        }
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        Bundle draft = pendingDraft;
        if (entryDialog != null && entryDialog.isShowing()) {
            draft = new Bundle();
            draft.putString(KEY_ID, editingId);
            draft.putString(KEY_NOTE, String.valueOf(noteField.getText()));
            draft.putString(KEY_STOP, chosenStopId);
            draft.putInt(KEY_DAY, chosenDay);
            draft.putString(KEY_PHOTO, photoUri);
        }
        if (draft != null) {
            state.putBundle(STATE_DRAFT, draft);
        }
        if (pendingRemoval != null) {
            state.putString(STATE_REMOVE, pendingRemoval);
        }
    }

    @Override
    protected void onDestroy() {
        if (entryDialog != null) {
            entryDialog.dismiss();
        }
        if (removeDialog != null) {
            removeDialog.dismiss();
        }
        super.onDestroy();
    }

    private void onPhotoPicked(@Nullable Uri uri) {
        if (uri == null || entryDialog == null && pendingDraft == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException error) {
            Toast.makeText(this, R.string.journal_photo_permission, Toast.LENGTH_LONG).show();
            return;
        }
        if (entryDialog == null) {
            pendingDraft.putString(KEY_PHOTO, uri.toString());
        } else {
            photoUri = uri.toString();
            renderDraftPhoto();
        }
    }

    private void render() {
        List<JournalAdapter.Card> cards = new ArrayList<>();
        for (JournalEntry entry : store.journal(tripId)) {
            ItineraryItemWithPlace stop = linkedStop(entry);
            String label;
            if (stop != null) {
                label = stopLabel(stop);
            } else if (entry.getDay() == DAY_GENERAL) {
                label = getString(R.string.journal_general);
            } else {
                label = getString(R.string.day_label, entry.getDay());
            }
            cards.add(new JournalAdapter.Card(label, entry));
        }
        adapter.submit(cards);
        findViewById(R.id.text_empty).setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void editEntry(JournalEntry entry) {
        Bundle draft = new Bundle();
        draft.putString(KEY_ID, entry.getId());
        draft.putString(KEY_NOTE, entry.getNote());
        draft.putString(KEY_PHOTO, entry.getPhotoUri());
        draft.putInt(KEY_DAY, entry.getDay());
        ItineraryItemWithPlace stop = linkedStop(entry);
        if (stop != null) {
            draft.putString(KEY_STOP, stop.item.getId());
        }
        openEntry(draft);
    }

    @Nullable
    private ItineraryItemWithPlace linkedStop(JournalEntry entry) {
        if (entry.getStopId() != null) {
            return findStop(entry.getStopId());
        }
        if (entry.getDay() <= 0 || entry.getStopName() == null || entry.getStopName().isEmpty()) {
            return null;
        }
        // Legacy entries had only a day and name. Link only when there is a unique match.
        ItineraryItemWithPlace match = null;
        for (ItineraryItemWithPlace stop : stops) {
            if (stop.item.getDay() == entry.getDay() && entry.getStopName().equals(displayName(stop))) {
                if (match != null) {
                    return null;
                }
                match = stop;
            }
        }
        return match;
    }

    @Nullable
    private ItineraryItemWithPlace findStop(@Nullable String id) {
        for (ItineraryItemWithPlace stop : stops) {
            if (stop.item.getId().equals(id)) {
                return stop;
            }
        }
        return null;
    }

    private void openEntry(Bundle draft) {
        if (entryDialog != null || stops == null) {
            return;
        }
        editingId = draft.getString(KEY_ID);
        photoUri = draft.getString(KEY_PHOTO);
        chosenDay = Math.max(DAY_GENERAL, Math.min(tripDays, draft.getInt(KEY_DAY, DAY_GENERAL)));
        ItineraryItemWithPlace linked = findStop(draft.getString(KEY_STOP));
        chosenStopId = linked == null ? null : linked.item.getId();
        if (linked != null) {
            chosenDay = linked.item.getDay();
        }
        View view = getLayoutInflater().inflate(R.layout.dialog_add_journal, null);
        noteField = view.findViewById(R.id.input_entry_note);
        noteField.setText(draft.getString(KEY_NOTE, ""));
        dayField = view.findViewById(R.id.input_entry_day);
        stopField = view.findViewById(R.id.input_entry_stop);
        bindAttachments();
        photoStatus = view.findViewById(R.id.text_entry_photo);
        clearPhotoButton = view.findViewById(R.id.button_clear_entry_photo);
        clearPhotoButton.setOnClickListener(v -> {
            photoUri = null;
            renderDraftPhoto();
        });
        view.findViewById(R.id.button_entry_photo).setOnClickListener(v -> {
            try {
                photoPicker.launch(new String[]{"image/*"});
            } catch (ActivityNotFoundException error) {
                photoStatus.setText(R.string.journal_photo_picker_unavailable);
            }
        });
        renderDraftPhoto();
        TextInputLayout layout = view.findViewById(R.id.layout_entry_note);
        entryDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(editingId == null ? R.string.action_add_entry : R.string.journal_edit)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save_entry, null)
                .create();
        entryDialog.setOnDismissListener(dialog -> entryDialog = null);
        entryDialog.setOnShowListener(dialog -> entryDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> submit(layout)));
        entryDialog.show();
    }

    private void bindAttachments() {
        List<String> days = new ArrayList<>();
        days.add(getString(R.string.journal_general));
        for (int day = 1; day <= tripDays; day++) {
            days.add(getString(R.string.day_label, day));
        }
        dayField.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, days));
        dayField.setText(days.get(chosenDay), false);
        dayField.setOnItemClickListener((parent, view, position, id) -> {
            chosenDay = position;
            ItineraryItemWithPlace selected = findStop(chosenStopId);
            if (selected != null && selected.item.getDay() != chosenDay) {
                chosenStopId = null;
                stopField.setText(getString(R.string.journal_whole_day), false);
            }
        });
        List<String> labels = new ArrayList<>();
        List<ItineraryItemWithPlace> options = new ArrayList<>(stops);
        labels.add(getString(R.string.journal_whole_day));
        for (ItineraryItemWithPlace stop : options) {
            labels.add(stopLabel(stop));
        }
        stopField.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        ItineraryItemWithPlace selected = findStop(chosenStopId);
        stopField.setText(selected == null ? labels.get(0) : stopLabel(selected), false);
        stopField.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                chosenStopId = null;
                return;
            }
            ItineraryItemWithPlace stop = options.get(position - 1);
            chosenStopId = stop.item.getId();
            chosenDay = Math.min(stop.item.getDay(), days.size() - 1);
            dayField.setText(days.get(chosenDay), false);
        });
    }

    private void renderDraftPhoto() {
        photoStatus.setText(photoUri == null ? R.string.journal_no_photo : R.string.journal_photo_local);
        clearPhotoButton.setVisibility(photoUri == null ? View.GONE : View.VISIBLE);
    }

    private void submit(TextInputLayout layout) {
        String note = String.valueOf(noteField.getText()).trim();
        if (note.isEmpty() && photoUri == null) {
            layout.setError(getString(R.string.journal_note_or_photo));
            noteField.requestFocus();
            return;
        }
        ItineraryItemWithPlace stop = findStop(chosenStopId);
        int day = stop == null ? Math.min(chosenDay, tripDays) : stop.item.getDay();
        String name = stop == null ? "" : displayName(stop);
        String id = editingId == null ? UUID.randomUUID().toString() : editingId;
        JournalEntry entry = new JournalEntry(id, day, name,
                stop == null ? null : stop.item.getId(), note, photoUri);
        if (editingId == null) {
            store.addJournalEntry(tripId, entry);
            list.scrollToPosition(0);
        } else if (!store.updateJournalEntry(tripId, entry)) {
            Toast.makeText(this, R.string.journal_entry_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        entryDialog.dismiss();
        render();
        String where = stop != null ? stopLabel(stop) : day == DAY_GENERAL
                ? getString(R.string.journal_general) : getString(R.string.day_label, day);
        Snackbar.make(findViewById(R.id.journal_root), getString(R.string.journal_saved_to, where),
                Snackbar.LENGTH_SHORT).setAnchorView(R.id.fab_add_entry).show();
    }

    private void confirmRemove(JournalEntry entry) {
        pendingRemoval = entry.getId();
        removeDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.journal_remove_title)
                .setMessage(R.string.journal_remove_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_remove, (dialog, which) -> {
                    store.removeJournalEntry(tripId, entry.getId());
                    render();
                })
                .show();
        removeDialog.setOnDismissListener(dialog -> {
            pendingRemoval = null;
            removeDialog = null;
        });
    }

    private String stopLabel(ItineraryItemWithPlace stop) {
        return getString(R.string.journal_entry_sub, stop.item.getDay(), displayName(stop));
    }

    private static String displayName(ItineraryItemWithPlace stop) {
        return stop.item.displayName(stop.placeName);
    }
}
