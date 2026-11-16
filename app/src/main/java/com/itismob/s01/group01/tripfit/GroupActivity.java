package com.itismob.s01.group01.tripfit;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.MockData;
import com.itismob.s01.group01.tripfit.prototype.MockPlace;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Shared-trip view: invite code, travelers, destination voting, and an even/custom cost split. */
public class GroupActivity extends ScreenActivity {

    private static final String INVITE_CODE = "TRIP-7K2Q";
    private static final int MAX_VOTE_ITEMS = 5;
    private static final String[] MEMBERS = {"Ana R.", "Marco D.", "Lia S."};
    /** Custom split shares (percent) for You + the three travelers; sums to 100. */
    private static final int[] CUSTOM_SHARES = {40, 30, 20, 10};

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter votesAdapter = new RowAdapter();
    private final RowAdapter splitAdapter = new RowAdapter();
    private List<String> destinations = new ArrayList<>();
    private MaterialButtonToggleGroup toggleSplit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_group, R.string.group_title);

        ((TextView) findViewById(R.id.text_invite_code)).setText(INVITE_CODE);
        findViewById(R.id.button_copy_code).setOnClickListener(v -> copyCode());
        findViewById(R.id.button_share_invite).setOnClickListener(v -> shareInvite());

        RowAdapter membersAdapter = new RowAdapter();
        ((RecyclerView) findViewById(R.id.list_members)).setAdapter(membersAdapter);
        membersAdapter.submit(memberRows());
        ((RecyclerView) findViewById(R.id.list_votes)).setAdapter(votesAdapter);
        ((RecyclerView) findViewById(R.id.list_split)).setAdapter(splitAdapter);

        toggleSplit = findViewById(R.id.toggle_split);
        toggleSplit.check(R.id.button_split_even);
        toggleSplit.addOnButtonCheckedListener((group, id, checked) -> {
            if (checked) {
                renderSplit();
            }
        });
        renderSplit();

        ((TripFitApp) getApplication()).getTripRepository()
                .observeItineraryItemsWithPlaceNames(tripId).observe(this, items -> {
                    destinations = destinationNames(items);
                    renderVotes();
                });
    }

    private List<UiRow> memberRows() {
        List<UiRow> rows = new ArrayList<>();
        rows.add(UiRow.plain(getString(R.string.profile_name), getString(R.string.group_you)));
        for (String member : MEMBERS) {
            rows.add(UiRow.plain(member, getString(R.string.group_joined)));
        }
        return rows;
    }

    /** Up to five distinct stop names to vote on, or the city's famous spots when the trip is empty. */
    private List<String> destinationNames(List<ItineraryItemWithPlace> items) {
        Set<String> names = new LinkedHashSet<>();
        for (ItineraryItemWithPlace entry : items) {
            names.add(entry.item.displayName(entry.placeName));
        }
        if (names.isEmpty()) {
            for (MockPlace place : MockData.famous(store.meta(tripId).getCity())) {
                names.add(place.getName());
            }
        }
        List<String> limited = new ArrayList<>(names);
        return limited.subList(0, Math.min(MAX_VOTE_ITEMS, limited.size()));
    }

    private void renderVotes() {
        List<UiRow> rows = new ArrayList<>();
        for (String name : destinations) {
            rows.add(voteRow(name));
        }
        votesAdapter.submit(rows);
    }

    /** Vote row: count (with a text note when it includes yours) under the name, selected when voted. */
    private UiRow voteRow(String name) {
        boolean voted = store.hasVoted(name);
        int count = store.voteCount(name);
        String tally = getResources().getQuantityString(
                voted ? R.plurals.vote_count_yours : R.plurals.vote_count, count, count);
        return UiRow.withAction(name, tally, null,
                getString(voted ? R.string.action_voted : R.string.action_vote), () -> {
                    store.toggleVote(name);
                    renderVotes();
                })
                .withSelected(voted)
                .withAccessibility(getString(
                        voted ? R.string.vote_remove_cd : R.string.vote_cd, name), null);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        renderSplit(); // the toggle may have been restored to Custom after onCreate rendered Even
    }

    private void renderSplit() {
        BudgetSettings budget = store.budget(tripId);
        String home = budget.getHomeCurrency();
        double total = budget.tripTotal(store.meta(tripId).getDays());
        boolean even = toggleSplit.getCheckedButtonId() == R.id.button_split_even;
        int people = MEMBERS.length + 1;

        ((TextView) findViewById(R.id.text_split_total)).setText(
                CurrencyRates.format(total, home));
        List<UiRow> rows = new ArrayList<>();
        for (int i = 0; i < people; i++) {
            int share = even ? 100 / people : CUSTOM_SHARES[i];
            double amount = even ? total / people : total * share / 100.0;
            String name = i == 0 ? getString(R.string.profile_name) : MEMBERS[i - 1];
            rows.add(UiRow.withTrailing(name, splitSubtitle(i == 0, even, share),
                    CurrencyRates.format(amount, home)));
        }
        splitAdapter.submit(rows);
    }

    private String splitSubtitle(boolean isYou, boolean even, int share) {
        if (even) {
            return isYou ? getString(R.string.split_you) : null;
        }
        return isYou ? getString(R.string.split_you_share, share)
                : getString(R.string.split_member_sub, share);
    }

    private void copyCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.invite_code_label),
                INVITE_CODE));
        toast(R.string.code_copied);
    }

    private void shareInvite() {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message, INVITE_CODE));
        startActivity(Intent.createChooser(send, getString(R.string.action_share_invite)));
    }
}
