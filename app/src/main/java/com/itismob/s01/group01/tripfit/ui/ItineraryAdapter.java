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

/** Renders a flat list of {@link ItineraryRow}s as day headers and stop cards. */
public class ItineraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ItineraryRow> rows = new ArrayList<>();

    public void submitRows(List<ItineraryRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ItineraryRow.TYPE_DAY_HEADER) {
            return new DayHeaderViewHolder(inflater.inflate(R.layout.item_day_header, parent, false));
        }
        return new StopViewHolder(inflater.inflate(R.layout.item_stop, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItineraryRow row = rows.get(position);
        if (holder instanceof DayHeaderViewHolder) {
            ((DayHeaderViewHolder) holder).bind((ItineraryRow.DayHeader) row);
        } else if (holder instanceof StopViewHolder) {
            ((StopViewHolder) holder).bind((ItineraryRow.Stop) row);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class DayHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayLabel;
        private final TextView stopsCount;

        DayHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dayLabel = itemView.findViewById(R.id.text_day_label);
            stopsCount = itemView.findViewById(R.id.text_stops_count);
        }

        void bind(ItineraryRow.DayHeader header) {
            dayLabel.setText(dayLabel.getContext().getString(R.string.day_label, header.getDay()));
            stopsCount.setText(stopsCount.getContext()
                    .getString(R.string.stops_count, header.getStopCount()));
        }
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

        void bind(ItineraryRow.Stop stop) {
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
