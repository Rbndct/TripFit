package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Dropdown adapter that always lists every option. A read-only exposed dropdown otherwise
 * filters by its current text, so after a rotation restores the text only that one option shows.
 */
public final class NoFilterAdapter extends ArrayAdapter<String> {

    private final List<String> options;
    private final Filter everything = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            results.values = options;
            results.count = options.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    };

    public NoFilterAdapter(Context context, List<String> options) {
        super(context, android.R.layout.simple_list_item_1, new ArrayList<>(options));
        this.options = new ArrayList<>(options);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return everything;
    }
}
