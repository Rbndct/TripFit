package com.itismob.s01.group01.tripfit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renders the active day's stops as numbered cards (day grouping lives in the day tab strip). */
public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.StopViewHolder> {

    private final List<ItineraryRow> rows = new ArrayList<>();

    public void submitRows(List<ItineraryRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stop, parent, false);
        return new StopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StopViewHolder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class StopViewHolder extends RecyclerView.ViewHolder {
        private final TextView indexBadge;
        private final TextView name;
        private final TextView meta;
        private final View bookedBadge;

        StopViewHolder(@NonNull View itemView) {
            super(itemView);
            indexBadge = itemView.findViewById(R.id.text_index_badge);
            name = itemView.findViewById(R.id.text_stop_name);
            meta = itemView.findViewById(R.id.text_stop_meta);
            bookedBadge = itemView.findViewById(R.id.badge_booked);
        }

        void bind(ItineraryRow stop) {
            indexBadge.setText(String.valueOf(stop.getIndexInDay()));
            name.setText(stop.getItem().displayName(stop.getPlaceName()));
            meta.setText(resolveMetaText(stop.getItem()));
            bookedBadge.setVisibility(stop.getItem().isBooked() ? View.VISIBLE : View.GONE);
        }

        private String resolveMetaText(ItineraryItemEntity item) {
            if (item.isBooked() && item.getTime() != null) {
                return meta.getContext().getString(R.string.stop_duration, item.getTime());
            }
            if (item.getDurationMin() != null) {
                String label = String.format(Locale.getDefault(), "%d min", item.getDurationMin());
                return meta.getContext().getString(R.string.stop_duration, label);
            }
            return meta.getContext().getString(R.string.no_duration);
        }
    }
}
