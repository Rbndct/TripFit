package com.itismob.s01.group01.tripfit;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
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

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private List<ItineraryItemWithPlace> stops = new ArrayList<>();
    private ActivityResultLauncher<PickVisualMediaRequest> photoPicker;
    private MaterialButton dialogPhotoButton;
    private boolean photoPicked;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_journal, R.string.journal_title);
        ((RecyclerView) findViewById(R.id.list_journal)).setAdapter(adapter);

        photoPicker = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                (Uri uri) -> {
                    if (uri != null && dialogPhotoButton != null) {
                        photoPicked = true;
                        dialogPhotoButton.setText(R.string.photo_attached);
                    }
                });
        findViewById(R.id.fab_add_entry).setOnClickListener(v -> showAddDialog());

        ((TripFitApp) getApplication()).getTripRepository()
                .observeItineraryItemsWithPlaceNames(tripId)
                .observe(this, items -> stops = items);
        render();
    }

    private void render() {
        List<UiRow> rows = new ArrayList<>();
        for (JournalEntry entry : store.journal(tripId)) {
            String sub = getString(R.string.journal_entry_sub, entry.getDay(), entry.getStopName());
            rows.add(UiRow.plain(entry.getNote(),
                    entry.hasPhoto() ? sub + " · " + getString(R.string.journal_photo_tag) : sub));
        }
        adapter.submit(rows);
        findViewById(R.id.text_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_journal, null);
        MaterialAutoCompleteTextView inputStop = view.findViewById(R.id.input_entry_stop);
        TextInputLayout layoutNote = view.findViewById(R.id.layout_entry_note);
        TextInputEditText inputNote = view.findViewById(R.id.input_entry_note);
        dialogPhotoButton = view.findViewById(R.id.button_entry_photo);
        photoPicked = false;

        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.journal_general));
        for (ItineraryItemWithPlace entry : stops) {
            labels.add(getString(R.string.journal_entry_sub, entry.item.getDay(),
                    entry.item.displayName(entry.placeName)));
        }
        inputStop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        inputStop.setText(labels.get(0), false);
        int[] chosen = {-1};
        inputStop.setOnItemClickListener((parent, v, position, id) -> chosen[0] = position - 1);

        dialogPhotoButton.setOnClickListener(v -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_add_entry)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save_entry, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String note = inputNote.getText() == null
                            ? "" : inputNote.getText().toString().trim();
                    if (TextUtils.isEmpty(note)) {
                        layoutNote.setError(getString(R.string.error_note_required));
                        return;
                    }
                    saveEntry(chosen[0], note);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void saveEntry(int stopIndex, String note) {
        int day = 1;
        String stopName = getString(R.string.journal_general);
        if (stopIndex >= 0 && stopIndex < stops.size()) {
            ItineraryItemWithPlace entry = stops.get(stopIndex);
            day = entry.item.getDay();
            stopName = entry.item.displayName(entry.placeName);
        }
        store.addJournalEntry(tripId, new JournalEntry(day, stopName, note, photoPicked));
        toast(R.string.entry_saved);
        render();
    }
}
