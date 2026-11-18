package com.itismob.s01.group01.tripfit.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.data.AppExecutors;
import com.itismob.s01.group01.tripfit.prototype.JournalEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.EntryHolder> {
    private static final Executor PHOTO_READS = Executors.newFixedThreadPool(2);

    public static final class Card {
        final String title;
        final JournalEntry entry;

        public Card(String title, JournalEntry entry) {
            this.title = title;
            this.entry = entry;
        }
    }

    private final List<Card> cards = new ArrayList<>();
    private final Consumer<JournalEntry> edit;
    private final Consumer<JournalEntry> remove;

    public JournalAdapter(Consumer<JournalEntry> edit, Consumer<JournalEntry> remove) {
        this.edit = edit;
        this.remove = remove;
    }

    public void submit(List<Card> entries) {
        cards.clear();
        cards.addAll(entries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new EntryHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_journal_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EntryHolder holder, int position) {
        holder.bind(cards.get(position), edit, remove);
    }

    @Override
    public void onViewRecycled(@NonNull EntryHolder holder) {
        holder.clearPhoto();
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static final class EntryHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView note;
        private final ImageView photo;
        private final TextView photoError;
        private int photoVersion;

        EntryHolder(View view) {
            super(view);
            title = view.findViewById(R.id.text_journal_title);
            note = view.findViewById(R.id.text_journal_note);
            photo = view.findViewById(R.id.image_journal_photo);
            photoError = view.findViewById(R.id.text_journal_photo_error);
            ViewCompat.setAccessibilityHeading(title, true);
        }

        void bind(Card card, Consumer<JournalEntry> edit, Consumer<JournalEntry> remove) {
            JournalEntry entry = card.entry;
            title.setText(card.title);
            note.setText(entry.getNote());
            note.setVisibility(entry.getNote() == null || entry.getNote().isEmpty() ? View.GONE : View.VISIBLE);
            View editButton = itemView.findViewById(R.id.button_edit_memory);
            editButton.setContentDescription(itemView.getContext().getString(R.string.journal_edit_cd, card.title));
            editButton.setOnClickListener(v -> edit.accept(entry));
            View removeButton = itemView.findViewById(R.id.button_remove_memory);
            removeButton.setContentDescription(itemView.getContext().getString(R.string.journal_remove_cd, card.title));
            removeButton.setOnClickListener(v -> remove.accept(entry));
            clearPhoto();
            if (entry.getPhotoUri() != null) {
                loadPhoto(Uri.parse(entry.getPhotoUri()), photoVersion);
            } else if (entry.hasPhoto()) {
                showPhotoError(R.string.journal_photo_unavailable);
            }
        }

        void clearPhoto() {
            photoVersion++;
            photo.setImageDrawable(null);
            photo.setVisibility(View.GONE);
            photoError.setVisibility(View.GONE);
        }

        private void loadPhoto(Uri uri, int version) {
            AppExecutors executors = AppExecutors.getInstance();
            PHOTO_READS.execute(() -> {
                try {
                    Bitmap bitmap = JournalPhotos.decode(itemView.getContext().getContentResolver(), uri);
                    executors.mainThread().execute(() -> {
                        if (photoVersion != version) {
                            return;
                        }
                        if (bitmap == null) {
                            showPhotoError(R.string.journal_photo_invalid);
                        } else {
                            photo.setImageBitmap(bitmap);
                            photo.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (IOException | SecurityException | IllegalArgumentException error) {
                    executors.mainThread().execute(() -> {
                        if (photoVersion == version) {
                            showPhotoError(R.string.journal_photo_unavailable);
                        }
                    });
                }
            });
        }

        private void showPhotoError(int text) {
            photoError.setText(text);
            photoError.setVisibility(View.VISIBLE);
        }
    }
}
