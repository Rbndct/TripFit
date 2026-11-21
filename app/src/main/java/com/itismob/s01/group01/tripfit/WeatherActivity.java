package com.itismob.s01.group01.tripfit;

import android.os.Bundle;

import androidx.annotation.Nullable;
import com.itismob.s01.group01.tripfit.ui.Keyboard;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.CatalogSuggestions;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripDates;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.prototype.WeatherPlanning;
import com.itismob.s01.group01.tripfit.prototype.WeatherPlanning.Condition;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WeatherActivity extends ScreenActivity {
    private static final String STATE_FORECAST_DAY = "forecast_day";
    private static final String STATE_ALTERNATIVES_STOP = "alternatives_stop";

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private TripRepository repository;
    private RecyclerView list;
    private List<ItineraryItemWithPlace> itinerary;
    private int initialDay;
    private boolean saving;
    private AlertDialog dialog;
    private int forecastDay;
    private String alternativesStopId;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!setScreen(R.layout.activity_weather, R.string.weather_screen_title)) {
            return;
        }
        repository = ((TripFitApp) getApplication()).getTripRepository();
        initialDay = state == null ? getIntent().getIntExtra(AddDestinationActivity.EXTRA_DAY, 1) : 0;
        list = findViewById(R.id.list_weather);
        adapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        list.setAdapter(adapter);
        repository.observeItineraryItemsWithPlaceNames(tripId).observe(this, rows -> {
            boolean loaded = itinerary != null;
            itinerary = rows;
            render();
            if (!loaded && state != null) {
                restoreDialog(state);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (dialog != null && dialog.isShowing()) {
            state.putInt(STATE_FORECAST_DAY, forecastDay);
            state.putString(STATE_ALTERNATIVES_STOP, alternativesStopId);
        }
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
        }
        super.onDestroy();
    }

    private void restoreDialog(Bundle state) {
        int day = state.getInt(STATE_FORECAST_DAY);
        if (day > 0) {
            chooseForecast(day);
            return;
        }
        String stopId = state.getString(STATE_ALTERNATIVES_STOP);
        for (ItineraryItemWithPlace stop : itinerary) {
            if (stop.item.getId().equals(stopId) && canReschedule(stop)) {
                showAlternatives(stop);
                return;
            }
        }
    }

    private void render() {
        TripMeta meta = store.meta(tripId);
        int days = meta.getDays();
        for (ItineraryItemWithPlace stop : itinerary) {
            days = Math.max(days, stop.item.getDay());
        }
        List<String> forecast = store.weather(tripId);
        List<UiRow> rows = new ArrayList<>();
        int initialPosition = 0;
        for (int day = 1; day <= days; day++) {
            if (day == Math.min(initialDay, days)) {
                initialPosition = rows.size();
            }
            Condition condition = WeatherPlanning.conditionAt(forecast, day);
            List<ItineraryItemWithPlace> conflicts =
                    WeatherPlanning.conflicts(itinerary, day, condition, store);
            rows.add(forecastRow(day, meta, condition, conflicts.isEmpty()));
            for (ItineraryItemWithPlace stop : conflicts) {
                rows.add(conflictRow(stop));
            }
        }
        adapter.submit(rows);
        if (initialDay > 0) {
            ((LinearLayoutManager) list.getLayoutManager()).scrollToPositionWithOffset(initialPosition, 0);
            initialDay = 0;
        }
    }

    private UiRow forecastRow(int day, TripMeta meta, Condition condition, boolean empty) {
        String date = TripDates.dayDateLabel(meta.getStartDateIso(), day);
        String title = date.isEmpty() ? getString(R.string.day_label, day)
                : getString(R.string.day_heading_dated, day, date);
        String subtitle = getString(R.string.weather_sample_reading, conditionLabel(condition),
                condition.temperatureC, condition.precipitationChance);
        if (empty) {
            subtitle += "\n" + getString(condition == Condition.CLEAR
                    ? R.string.weather_no_conflicts : R.string.weather_no_outdoor_stops);
        }
        return saving ? UiRow.plain(title, subtitle)
                : UiRow.withAction(title, subtitle, null, getString(R.string.weather_change_forecast),
                        () -> chooseForecast(day))
                .withAccessibility(getString(R.string.weather_change_forecast_cd, day), null);
    }

    private UiRow conflictRow(ItineraryItemWithPlace stop) {
        String name = displayName(stop);
        if (stop.item.isBooked()) {
            return UiRow.plain(name, getString(R.string.weather_booked_conflict));
        }
        String warning = getString(R.string.weather_flexible_conflict);
        return saving ? UiRow.plain(name, warning)
                : UiRow.withAction(name, warning, null, getString(R.string.weather_indoor_action),
                        () -> showAlternatives(stop))
                .withAccessibility(getString(R.string.weather_indoor_action_cd, name), null);
    }

    private String conditionLabel(Condition condition) {
        switch (condition) {
            case STORM: return getString(R.string.weather_storm);
            case RAIN: return getString(R.string.weather_rain);
            default: return getString(R.string.weather_clear);
        }
    }

    private void chooseForecast(int day) {
        Condition[] choices = Condition.values();
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            labels[i] = conditionLabel(choices[i]);
        }
        forecastDay = day;
        alternativesStopId = null;
        dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.weather_change_forecast_cd, day))
                .setSingleChoiceItems(labels, WeatherPlanning.conditionAt(store.weather(tripId), day)
                        .ordinal(), (picker, choice) -> {
                            Keyboard.hide(findViewById(android.R.id.content));
                            store.setForecastCondition(tripId, day, choices[choice]);
                            picker.dismiss();
                            render();
                        })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAlternatives(ItineraryItemWithPlace outdoor) {
        if (saving) {
            return;
        }
        if (!canReschedule(outdoor)) {
            toast(R.string.catalog_swap_unavailable);
            return;
        }
        String outdoorId = outdoor.item.getId();
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (ItineraryItemWithPlace indoor : itinerary) {
            if (WeatherPlanning.canTradeDays(outdoor, indoor, store)) {
                labels.add(getString(R.string.weather_trade_days, displayName(indoor), indoor.item.getDay()));
                actions.add(() -> repository.swapWeatherStops(tripId, outdoorId,
                        indoor.item.getId(), this::onSwapResult));
            }
        }
        Set<String> planned = CatalogSuggestions.plannedNames(itinerary);
        for (DemoCatalog.Place place : DemoCatalog.places(store.meta(tripId).getCity())) {
            if (!place.outdoor && CatalogSuggestions.Filter.ACTIVITIES.matches(place)
                    && !planned.contains(DemoCatalog.nameKey(place.name))) {
                labels.add(getString(R.string.weather_replace_stop, place.name));
                actions.add(() -> repository.replaceWithCatalogPlace(tripId, outdoorId,
                        place, this::onSwapResult));
            }
        }
        if (actions.isEmpty()) {
            toast(R.string.weather_no_alternatives);
            return;
        }
        forecastDay = 0;
        alternativesStopId = outdoorId;
        dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.weather_indoor_action_cd, displayName(outdoor)))
                .setItems(labels.toArray(new String[0]), (picker, choice) -> {
                    saving = true;
                    render();
                    actions.get(choice).run();
                })
                .setNegativeButton(R.string.action_keep_plan, null)
                .show();
    }

    private boolean canReschedule(ItineraryItemWithPlace stop) {
        return !stop.item.isBooked() && WeatherPlanning.isOutdoor(stop, store)
                && WeatherPlanning.conditionAt(store.weather(tripId), stop.item.getDay())
                != Condition.CLEAR;
    }

    private void onSwapResult(Result<Void> result) {
        if (isDestroyed()) {
            return;
        }
        saving = false;
        render();
        if (result.isSuccess()) {
            toast(R.string.weather_itinerary_updated);
        } else if (TripRepository.CATALOG_ALREADY_PLANNED.equals(result.getErrorMessage())) {
            toast(R.string.catalog_already_planned);
        } else if (TripRepository.CATALOG_STOP_UNAVAILABLE.equals(result.getErrorMessage())) {
            toast(R.string.catalog_swap_unavailable);
        } else {
            toast(R.string.error_update_failed);
        }
    }

    private static String displayName(ItineraryItemWithPlace stop) {
        return stop.item.displayName(stop.placeName);
    }
}
