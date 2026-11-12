package com.itismob.s01.group01.tripfit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s01.group01.tripfit.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the Home screen's trip list. The first trip renders as the hero ticket card (navy
 * band, perforation, barcode); the rest render as plainer stub cards. Tapping a card hands
 * back the tapped trip's id.
 */
public class TripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HERO = 0;
    private static final int VIEW_TYPE_STUB = 1;

    /** Receives the id of the trip the user tapped. */
    public interface OnTripSelectedListener {
        void onTripSelected(String tripId);
    }

    private final List<HomeTripRow> trips = new ArrayList<>();
    private final OnTripSelectedListener listener;

    public TripAdapter(OnTripSelectedListener listener) {
        this.listener = listener;
    }

    public void submitTrips(List<HomeTripRow> newTrips) {
        trips.clear();
        trips.addAll(newTrips);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HERO : VIEW_TYPE_STUB;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HERO) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trip_card_hero, parent, false);
            return new HeroViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_card_stub, parent, false);
        return new StubViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HomeTripRow trip = trips.get(position);
        if (holder instanceof HeroViewHolder) {
            ((HeroViewHolder) holder).bind(trip, listener);
        } else {
            ((StubViewHolder) holder).bind(trip, listener);
        }
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class HeroViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView counts;
        private final TextView status;
        private final BarcodeStripView barcode;

        HeroViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_trip_title);
            counts = itemView.findViewById(R.id.text_trip_ticket_counts);
            status = itemView.findViewById(R.id.text_trip_status);
            barcode = itemView.findViewById(R.id.view_trip_barcode);
        }

        void bind(HomeTripRow trip, OnTripSelectedListener listener) {
            title.setText(trip.getName());
            counts.setText(itemView.getContext().getString(
                    R.string.ticket_days_stops, trip.getDayCount(), trip.getStopCount()));
            bindStatus(status, trip);
            barcode.setSeed(trip.getTripId());
            itemView.setOnClickListener(v -> listener.onTripSelected(trip.getTripId()));
        }
    }

    static class StubViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView status;
        private final TextView days;
        private final TextView stops;

        StubViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_trip_title);
            status = itemView.findViewById(R.id.text_trip_status);
            days = itemView.findViewById(R.id.text_trip_days);
            stops = itemView.findViewById(R.id.text_trip_stops);
        }

        void bind(HomeTripRow trip, OnTripSelectedListener listener) {
            title.setText(trip.getName());
            bindStatus(status, trip);
            days.setText(itemView.getContext().getString(
                    R.string.trip_days_count, trip.getDayCount()));
            stops.setText(itemView.getContext().getString(
                    R.string.trip_stops_count, trip.getStopCount()));
            itemView.setOnClickListener(v -> listener.onTripSelected(trip.getTripId()));
        }
    }

    private static void bindStatus(TextView status, HomeTripRow trip) {
        boolean isDraft = trip.getStopCount() == 0;
        status.setText(isDraft ? R.string.trip_status_draft : R.string.trip_status_planned);
        status.setBackgroundResource(isDraft ? R.drawable.bg_badge_draft : R.drawable.bg_badge_planned);
    }
}
