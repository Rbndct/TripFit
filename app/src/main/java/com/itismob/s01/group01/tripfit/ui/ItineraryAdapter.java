package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TravelMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders the active day's stops as numbered cards (day grouping lives in the day tab strip).
 * Every stop except the last carries a chip for the transport leg to the next stop.
 */
public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.StopViewHolder> {

    /** Callbacks for tapping a stop card or its transport-leg chip. */
    public interface Listener {
        void onStopClick(ItineraryRow row);

        void onLegClick(ItineraryRow row);
    }

    private final List<ItineraryRow> rows = new ArrayList<>();
    private final Listener listener;

    public ItineraryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitRows(List<ItineraryRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    /** Minutes of travel for one leg, using the user's chosen (or distance-suggested) mode. */
    public static int legMinutes(ItineraryItemEntity item) {
        double km = PrototypeStore.legKm(item.getId());
        return PrototypeStore.get().legMode(item.getId(), km).minutesFor(km);
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
        holder.bind(rows.get(position), position < rows.size() - 1, listener);
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
        private final Chip legChip;

        StopViewHolder(@NonNull View itemView) {
            super(itemView);
            indexBadge = itemView.findViewById(R.id.text_index_badge);
            name = itemView.findViewById(R.id.text_stop_name);
            meta = itemView.findViewById(R.id.text_stop_meta);
            bookedBadge = itemView.findViewById(R.id.badge_booked);
            legChip = itemView.findViewById(R.id.chip_leg);
        }

        void bind(ItineraryRow stop, boolean hasNextLeg, Listener listener) {
            indexBadge.setText(String.valueOf(stop.getIndexInDay()));
            name.setText(stop.getItem().displayName(stop.getPlaceName()));
            meta.setText(resolveMetaText(stop.getItem()));
            bookedBadge.setVisibility(stop.getItem().isBooked() ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(v -> listener.onStopClick(stop));
            bindLeg(stop, hasNextLeg, listener);
        }

        private void bindLeg(ItineraryRow stop, boolean hasNextLeg, Listener listener) {
            legChip.setVisibility(hasNextLeg ? View.VISIBLE : View.GONE);
            if (!hasNextLeg) {
                return;
            }
            Context context = legChip.getContext();
            ItineraryItemEntity item = stop.getItem();
            double km = PrototypeStore.legKm(item.getId());
            TravelMode mode = PrototypeStore.get().legMode(item.getId(), km);
            legChip.setText(context.getString(R.string.leg_label,
                    Labels.mode(context, mode), mode.minutesFor(km)));
            legChip.setOnClickListener(v -> listener.onLegClick(stop));
        }

        private String resolveMetaText(ItineraryItemEntity item) {
            if (item.isBooked() && item.getTime() != null) {
                return meta.getContext().getString(R.string.stop_booked_at, item.getTime());
            }
            if (item.getDurationMin() != null) {
                String label = String.format(Locale.getDefault(), "%d min", item.getDurationMin());
                return meta.getContext().getString(R.string.stop_duration, label);
            }
            return meta.getContext().getString(R.string.no_duration);
        }
    }
}
