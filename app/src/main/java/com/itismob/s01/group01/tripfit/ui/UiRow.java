package com.itismob.s01.group01.tripfit.ui;

/**
 * One generic list row (title, subtitle, optional monospace trailing value, optional action
 * button). Lets simple list screens share {@link RowAdapter} instead of each owning an adapter.
 */
public final class UiRow {

    private final String title;
    private final String subtitle;
    private final String trailing;
    private final String actionLabel;
    private final Runnable onAction;
    private final Runnable onClick;

    public UiRow(String title, String subtitle, String trailing, String actionLabel,
                 Runnable onAction, Runnable onClick) {
        this.title = title;
        this.subtitle = subtitle;
        this.trailing = trailing;
        this.actionLabel = actionLabel;
        this.onAction = onAction;
        this.onClick = onClick;
    }

    /** Row with no trailing value and no action. */
    public static UiRow plain(String title, String subtitle) {
        return new UiRow(title, subtitle, null, null, null, null);
    }

    /** Row with a trailing value only (e.g. an amount). */
    public static UiRow withTrailing(String title, String subtitle, String trailing) {
        return new UiRow(title, subtitle, trailing, null, null, null);
    }

    /** Row with a tonal action button (e.g. "Add", "Vote"). */
    public static UiRow withAction(String title, String subtitle, String trailing,
                                   String actionLabel, Runnable onAction) {
        return new UiRow(title, subtitle, trailing, actionLabel, onAction, null);
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getTrailing() {
        return trailing;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public Runnable getOnAction() {
        return onAction;
    }

    public Runnable getOnClick() {
        return onClick;
    }
}
