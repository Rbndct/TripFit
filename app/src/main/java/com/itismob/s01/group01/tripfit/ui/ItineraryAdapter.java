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
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.ScheduleMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders the active day's stops as numbered cards in schedule order (day grouping lives in the
 * day tab strip). A chip under card i describes the leg into card i+1 and edits that stop's mode
 * and buffer (the incoming-leg rule).
 */
public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.StopViewHolder> {

    /** Callbacks for tapping a stop card or a leg chip. */
    public interface Listener {
        void onStopClick(ItineraryRow row);

        /** {@code destination} is the stop the leg arrives at; its mode and buffer are edited. */
        void onLegClick(ItineraryRow destination);
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

    @NonNull
    @Override
    public StopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stop, parent, false);
        return new StopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StopViewHolder holder, int position) {
        ItineraryRow next = position < rows.size() - 1 ? rows.get(position + 1) : null;
        holder.bind(rows.get(position), next, listener);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class StopViewHolder extends RecyclerView.ViewHolder {
        private final TextView indexBadge;
        private final TextView name;
        private final TextView meta;
        private final TextView coords;
        private final TextView estimate;
        private final View conflictRow;
        private final View bookedBadge;
        private final Chip legChip;

        StopViewHolder(@NonNull View itemView) {
            super(itemView);
            indexBadge = itemView.findViewById(R.id.text_index_badge);
            name = itemView.findViewById(R.id.text_stop_name);
            meta = itemView.findViewById(R.id.text_stop_meta);
            coords = itemView.findViewById(R.id.text_stop_coords);
            estimate = itemView.findViewById(R.id.text_stop_estimate);
            conflictRow = itemView.findViewById(R.id.row_conflict);
            bookedBadge = itemView.findViewById(R.id.badge_booked);
            legChip = itemView.findViewById(R.id.chip_leg);
        }

        void bind(ItineraryRow stop, ItineraryRow next, Listener listener) {
            Context context = itemView.getContext();
            indexBadge.setText(String.valueOf(stop.getIndexInDay()));
            name.setText(stop.getName());
            meta.setText(context.getString(R.string.stop_time_range,
                    ScheduleMath.format(stop.getStart()), ScheduleMath.format(stop.getEnd()),
                    context.getString(stop.isBooked()
                            ? R.string.stop_status_fixed : R.string.stop_status_flexible)));
            bookedBadge.setVisibility(stop.isBooked() ? View.VISIBLE : View.GONE);
            conflictRow.setVisibility(stop.isConflict() ? View.VISIBLE : View.GONE);
            estimate.setVisibility(stop.getEstimateHome() > 0 ? View.VISIBLE : View.GONE);
            if (stop.getEstimateHome() > 0) {
                estimate.setText(context.getString(R.string.stop_estimate,
                        CurrencyRates.format(stop.getEstimateHome(), stop.getHomeCurrency())));
            }
            coords.setVisibility(stop.hasLocation() ? View.VISIBLE : View.GONE);
            if (stop.hasLocation()) {
                coords.setText(context.getString(R.string.stop_coords,
                        String.format(Locale.US, "%.5f", stop.getLat()),
                        String.format(Locale.US, "%.5f", stop.getLng())));
            }
            itemView.setOnClickListener(v -> listener.onStopClick(stop));
            bindLeg(next, listener);
        }

        private void bindLeg(ItineraryRow next, Listener listener) {
            legChip.setVisibility(next == null ? View.GONE : View.VISIBLE);
            if (next == null) {
                return;
            }
            Context context = legChip.getContext();
            legChip.setText(next.isPlaceholderLeg()
                    ? context.getString(R.string.leg_placeholder)
                    : context.getString(R.string.leg_label,
                            Labels.mode(context, next.getLegMode()),
                            next.getIncomingTravel(), next.getBuffer()));
            legChip.setOnClickListener(v -> listener.onLegClick(next));
        }
    }
}
