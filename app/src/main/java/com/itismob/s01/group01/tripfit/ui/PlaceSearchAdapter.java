package com.itismob.s01.group01.tripfit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renders place search results; tapping a row hands the picked {@link PlaceDto} back. */
public class PlaceSearchAdapter extends RecyclerView.Adapter<PlaceSearchAdapter.ViewHolder> {

    /** Receives the place the user tapped. */
    public interface OnPlaceSelectedListener {
        void onPlaceSelected(PlaceDto place);
    }

    private final List<PlaceDto> places = new ArrayList<>();
    private final OnPlaceSelectedListener listener;

    public PlaceSearchAdapter(OnPlaceSelectedListener listener) {
        this.listener = listener;
    }

    public void submitPlaces(List<PlaceDto> newPlaces) {
        places.clear();
        places.addAll(newPlaces);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(places.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView meta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_place_name);
            meta = itemView.findViewById(R.id.text_place_meta);
        }

        void bind(PlaceDto place, OnPlaceSelectedListener listener) {
            name.setText(place.getName());
            meta.setText(resolveMetaText(place));
            itemView.setOnClickListener(v -> listener.onPlaceSelected(place));
        }

        private String resolveMetaText(PlaceDto place) {
            StringBuilder text = new StringBuilder();
            if (place.getCategory() != null) {
                text.append(place.getCategory().replace('_', ' '));
            }
            if (place.isHiddenGem()) {
                text.append(text.length() > 0 ? " · " : "")
                        .append(itemView.getContext().getString(R.string.hidden_gem_label));
            } else if (place.getRating() != null) {
                text.append(text.length() > 0 ? " · " : "")
                        .append(String.format(Locale.getDefault(), "%.1f★", place.getRating()));
            }
            return text.toString();
        }
    }
}
