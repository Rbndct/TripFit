package com.itismob.s01.group01.tripfit.ui;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.itismob.s01.group01.tripfit.R;

import java.util.ArrayList;
import java.util.List;

/** Renders {@link UiRow}s as outlined cards; shared by every simple list screen. */
public class RowAdapter extends RecyclerView.Adapter<RowAdapter.RowViewHolder> {

    private final List<UiRow> rows = new ArrayList<>();

    public void submit(List<UiRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RowViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView subtitle;
        private final TextView trailing;
        private final MaterialButton action;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_row_title);
            subtitle = itemView.findViewById(R.id.text_row_subtitle);
            trailing = itemView.findViewById(R.id.text_row_trailing);
            action = itemView.findViewById(R.id.button_row_action);
        }

        void bind(UiRow row) {
            title.setText(row.getTitle());
            bindOptional(subtitle, row.getSubtitle());
            bindOptional(trailing, row.getTrailing());
            boolean hasAction = !TextUtils.isEmpty(row.getActionLabel());
            action.setVisibility(hasAction ? View.VISIBLE : View.GONE);
            action.setText(row.getActionLabel());
            action.setOnClickListener(hasAction && row.getOnAction() != null
                    ? v -> row.getOnAction().run() : null);
            itemView.setOnClickListener(row.getOnClick() != null
                    ? v -> row.getOnClick().run() : null);
            itemView.setClickable(row.getOnClick() != null);
            bindAccessibility(row);
            bindSelected(row);
        }

        private void bindAccessibility(UiRow row) {
            action.setContentDescription(row.getActionDescription());
            String label = row.getClickLabel();
            ViewCompat.setAccessibilityDelegate(itemView, label == null ? null
                    : new AccessibilityDelegateCompat() {
                        @Override
                        public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                @NonNull AccessibilityNodeInfoCompat info) {
                            super.onInitializeAccessibilityNodeInfo(host, info);
                            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                    AccessibilityNodeInfoCompat.ACTION_CLICK, label));
                        }
                    });
        }

        /** Selected rows get a 2dp primary outline (plus a spoken state) instead of the 1dp one. */
        private void bindSelected(UiRow row) {
            if (!(itemView instanceof MaterialCardView)) {
                return;
            }
            MaterialCardView card = (MaterialCardView) itemView;
            int strokeAttr = row.isSelected()
                    ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOutline;
            card.setStrokeColor(MaterialColors.getColor(card, strokeAttr));
            // Math.round matches how the XML strokeWidth="1dp" resolves; a truncating cast
            // would thin every shared row's outline by a pixel on 2.75x displays.
            card.setStrokeWidth(Math.round(card.getResources().getDisplayMetrics().density
                    * (row.isSelected() ? 2 : 1)));
            ViewCompat.setStateDescription(card, row.isSelected()
                    ? card.getContext().getString(R.string.state_selected) : null);
        }

        private void bindOptional(TextView view, String text) {
            view.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
            view.setText(text);
        }
    }
}
