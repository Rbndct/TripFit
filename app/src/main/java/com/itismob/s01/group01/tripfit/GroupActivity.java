package com.itismob.s01.group01.tripfit;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.CatalogSuggestions;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshot;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.List;

/** Local companions, catalog votes, and an equal split preview for the current trip. */
public class GroupActivity extends ScreenActivity {
    private static final String STATE_INPUT = "group_input";
    private static final String STATE_JOIN = "group_join";
    private static final String STATE_REMOVE = "group_remove";
    private static final String INVITE_CODE = "TRIP-DEMO";
    private static final String SAMPLE_COMPANION = "Sam (sample)";

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter membersAdapter = new RowAdapter();
    private final RowAdapter votesAdapter = new RowAdapter();
    private final RowAdapter splitAdapter = new RowAdapter();
    private List<ItineraryItemWithPlace> items;
    private AlertDialog dialog;
    private TextInputEditText dialogInput;
    private boolean joining;
    private String removing;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!setScreen(R.layout.activity_group, R.string.group_title)) {
            return;
        }
        ((TextView) findViewById(R.id.text_invite_code)).setText(INVITE_CODE);
        ((RecyclerView) findViewById(R.id.list_members)).setAdapter(membersAdapter);
        ((RecyclerView) findViewById(R.id.list_votes)).setAdapter(votesAdapter);
        ((RecyclerView) findViewById(R.id.list_split)).setAdapter(splitAdapter);

        findViewById(R.id.button_copy_code).setOnClickListener(v -> copyCode());
        findViewById(R.id.button_share_invite).setOnClickListener(v -> shareCode());
        findViewById(R.id.button_group_add).setOnClickListener(v -> openInput(false, ""));
        findViewById(R.id.button_group_join).setOnClickListener(v -> openInput(true, ""));
        findViewById(R.id.button_group_discover).setOnClickListener(v -> {
            Intent discover = new Intent(this, DiscoverActivity.class);
            discover.putExtra(HomeActivity.EXTRA_TRIP_ID, tripId);
            startActivity(discover);
        });
        renderMembers();
        renderVotes();
        ((TripFitApp) getApplication()).getTripRepository().observeItineraryItemsWithPlaceNames(tripId)
                .observe(this, rows -> {
                    items = rows;
                    renderSplit();
                });
        if (state != null) {
            if (state.containsKey(STATE_INPUT)) {
                openInput(state.getBoolean(STATE_JOIN), state.getString(STATE_INPUT, ""));
            } else if (state.containsKey(STATE_REMOVE)) {
                confirmRemove(state.getString(STATE_REMOVE));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (dialog != null && dialog.isShowing()) {
            if (dialogInput != null) {
                state.putString(STATE_INPUT, String.valueOf(dialogInput.getText()));
                state.putBoolean(STATE_JOIN, joining);
            } else if (removing != null) {
                state.putString(STATE_REMOVE, removing);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
        }
        super.onDestroy();
    }

    private void copyCode() {
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.invite_code_label), INVITE_CODE));
            toast(R.string.code_copied);
        }
    }

    private void shareCode() {
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message, INVITE_CODE));
        startActivity(Intent.createChooser(share, getString(R.string.action_share_invite)));
    }

    private void renderMembers() {
        List<String> members = store.members(tripId);
        List<UiRow> rows = new ArrayList<>();
        rows.add(UiRow.plain(members.get(0), getString(R.string.group_organizer)));
        for (int i = 1; i < members.size(); i++) {
            String name = members.get(i);
            rows.add(UiRow.withAction(name, null, null, getString(R.string.action_remove),
                            () -> confirmRemove(name))
                    .withAccessibility(getString(R.string.group_remove_cd, name), null));
        }
        membersAdapter.submit(rows);
        renderSplit();
    }

    private void renderVotes() {
        List<UiRow> rows = new ArrayList<>();
        for (DemoCatalog.Place place : DemoCatalog.places(store.meta(tripId).getCity())) {
            if (CatalogSuggestions.Filter.ACTIVITIES.matches(place)) {
                String name = place.name;
                boolean voted = store.hasVoted(tripId, name);
                rows.add(UiRow.withAction(name,
                                getString(voted ? R.string.group_one_vote : R.string.group_no_votes),
                                null, getString(voted ? R.string.action_voted : R.string.action_vote),
                                () -> {
                                    store.toggleVote(tripId, name);
                                    renderVotes();
                                })
                        .withSelected(voted)
                        .withAccessibility(getString(voted ? R.string.vote_remove_cd
                                : R.string.vote_cd, name), null));
            }
        }
        votesAdapter.submit(rows);
        findViewById(R.id.text_group_votes_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderSplit() {
        if (items == null) {
            return;
        }
        TripSnapshot trip = TripSnapshots.from(tripId, store.meta(tripId), items, store);
        List<String> members = store.members(tripId);
        String home = store.budget(tripId).getHomeCurrency();
        double planned = BudgetMath.plannedTotal(trip);
        double logged = BudgetMath.totalSpent(store.expenses(tripId));
        List<UiRow> rows = new ArrayList<>();
        rows.add(UiRow.plain(getString(R.string.group_split_totals),
                splitLine(planned, logged, home)));
        String share = splitLine(planned / members.size(), logged / members.size(), home);
        for (String name : members) {
            rows.add(UiRow.plain(name, share));
        }
        splitAdapter.submit(rows);
    }

    private String splitLine(double planned, double logged, String home) {
        return getString(R.string.group_split_line, CurrencyRates.format(planned, home),
                CurrencyRates.format(logged, home));
    }

    private void openInput(boolean join, String draft) {
        joining = join;
        removing = null;
        TextInputLayout layout = (TextInputLayout) getLayoutInflater().inflate(R.layout.dialog_group_input, null);
        layout.setHint(getString(join ? R.string.group_code_hint : R.string.group_name_hint));
        dialogInput = layout.findViewById(R.id.input_group_value);
        dialogInput.setText(draft);
        int title = join ? R.string.group_join : R.string.group_add;
        dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(layout)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(join ? R.string.group_join_action : R.string.action_add, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> submitCompanion(layout, join)));
        dialog.show();
    }

    private void submitCompanion(TextInputLayout layout, boolean join) {
        String value = String.valueOf(dialogInput.getText()).trim();
        if (join && !INVITE_CODE.equalsIgnoreCase(value)) {
            layout.setError(getString(R.string.group_invalid_code, INVITE_CODE));
            return;
        }
        if (value.isEmpty()) {
            layout.setError(getString(R.string.group_name_required));
            return;
        }
        if (!store.addCompanion(tripId, join ? SAMPLE_COMPANION : value)) {
            layout.setError(getString(R.string.group_duplicate));
            return;
        }
        dialog.dismiss();
        renderMembers();
    }

    private void confirmRemove(String name) {
        removing = name;
        dialogInput = null;
        dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.group_remove_title, name))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_remove, (picker, which) -> {
                    store.removeCompanion(tripId, name);
                    renderMembers();
                })
                .show();
    }
}
