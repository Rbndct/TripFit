package com.itismob.s01.group01.tripfit.prototype;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Converts between the immutable domain classes and the mutable persisted DTOs. */
final class TripDocMapper {

    private TripDocMapper() { }

    static TripDoc.MetaRec toRec(TripMeta meta) {
        TripDoc.MetaRec rec = new TripDoc.MetaRec();
        rec.city = meta.getCity();
        rec.dates = meta.getDates();
        rec.startDate = meta.getStartDateIso();
        rec.days = meta.getDays();
        rec.homeCurrency = meta.getHomeCurrency();
        return rec;
    }

    static TripMeta fromRec(TripDoc.MetaRec rec, String fallbackCity, int fallbackDays,
                            String fallbackHome) {
        return new TripMeta(
                rec.city != null ? rec.city : fallbackCity,
                rec.dates != null ? rec.dates : "",
                rec.days > 0 ? rec.days : fallbackDays,
                rec.homeCurrency != null ? rec.homeCurrency : fallbackHome,
                rec.startDate);
    }

    static TripDoc.BudgetRec toRec(BudgetSettings budget) {
        TripDoc.BudgetRec rec = new TripDoc.BudgetRec();
        rec.homeCurrency = budget.getHomeCurrency();
        rec.localCurrency = budget.getLocalCurrency();
        rec.amount = budget.getAmount();
        rec.perDay = budget.isPerDay();
        rec.limits = new HashMap<>();
        for (Map.Entry<Category, Double> entry : budget.getCategoryLimits().entrySet()) {
            rec.limits.put(entry.getKey().name(), entry.getValue());
        }
        return rec;
    }

    /** Returns null when the record lacks the currencies, so the caller falls back to defaults. */
    static BudgetSettings fromRec(TripDoc.BudgetRec rec) {
        if (rec.homeCurrency == null || rec.localCurrency == null) {
            return null;
        }
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        if (rec.limits != null) {
            for (Map.Entry<String, Double> entry : rec.limits.entrySet()) {
                Category category = parseCategoryOrNull(entry.getKey());
                if (category != null && entry.getValue() != null) {
                    limits.put(category, entry.getValue());
                }
            }
        }
        return new BudgetSettings(rec.homeCurrency, rec.localCurrency, rec.amount, rec.perDay, limits);
    }

    static TripDoc.ExpenseRec toRec(Expense expense) {
        TripDoc.ExpenseRec rec = new TripDoc.ExpenseRec();
        rec.id = expense.getId();
        rec.title = expense.getTitle();
        rec.category = expense.getCategory().name();
        rec.localCurrency = expense.getLocalCurrency();
        rec.amountLocal = expense.getAmountLocal();
        return rec;
    }

    static TripDoc.JournalRec toRec(JournalEntry entry) {
        TripDoc.JournalRec rec = new TripDoc.JournalRec();
        rec.id = entry.getId();
        rec.day = entry.getDay();
        rec.stopName = entry.getStopName();
        rec.stopId = entry.getStopId();
        rec.note = entry.getNote();
        rec.hasPhoto = entry.hasPhoto();
        rec.photoUri = entry.getPhotoUri();
        return rec;
    }

    static JournalEntry fromRec(TripDoc.JournalRec rec) {
        return new JournalEntry(rec.id, rec.day, rec.stopName, rec.stopId, rec.note,
                rec.hasPhoto, rec.photoUri);
    }

    /** Unknown or missing names map to {@link Category#OTHER}. */
    static Category parseCategory(String name) {
        Category category = parseCategoryOrNull(name);
        return category != null ? category : Category.OTHER;
    }

    /** Unknown or missing names map to null. */
    static Category parseCategoryOrNull(String name) {
        try {
            return name == null ? null : Category.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Unknown or missing names map to null so the caller can fall back to the distance default. */
    static TravelMode parseMode(String name) {
        try {
            return name == null ? null : TravelMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
