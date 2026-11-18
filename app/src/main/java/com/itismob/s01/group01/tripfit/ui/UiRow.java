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
    private final boolean selected;
    private final String actionDescription;
    private final String clickLabel;
    private final String secondaryLabel;
    private final String secondaryDescription;
    private final Runnable onSecondary;

    public UiRow(String title, String subtitle, String trailing, String actionLabel,
                 Runnable onAction, Runnable onClick) {
        this(title, subtitle, trailing, actionLabel, onAction, onClick, false, null, null, null, null, null);
    }

    private UiRow(String title, String subtitle, String trailing, String actionLabel,
                  Runnable onAction, Runnable onClick, boolean selected,
                  String actionDescription, String clickLabel, String secondaryLabel,
                  String secondaryDescription, Runnable onSecondary) {
        this.title = title;
        this.subtitle = subtitle;
        this.trailing = trailing;
        this.actionLabel = actionLabel;
        this.onAction = onAction;
        this.onClick = onClick;
        this.selected = selected;
        this.actionDescription = actionDescription;
        this.clickLabel = clickLabel;
        this.secondaryLabel = secondaryLabel;
        this.secondaryDescription = secondaryDescription;
        this.onSecondary = onSecondary;
    }

    /** Copy marked as selected: the card gets a heavier primary outline and a TalkBack state. */
    public UiRow withSelected(boolean isSelected) {
        return new UiRow(title, subtitle, trailing, actionLabel, onAction, onClick, isSelected,
                actionDescription, clickLabel, secondaryLabel, secondaryDescription, onSecondary);
    }

    /** Copy whose action button and card tap carry spoken labels naming what they act on. */
    public UiRow withAccessibility(String actionDesc, String rowClickLabel) {
        return new UiRow(title, subtitle, trailing, actionLabel, onAction, onClick, selected,
                actionDesc, rowClickLabel, secondaryLabel, secondaryDescription, onSecondary);
    }

    public UiRow withSecondaryAction(String label, String description, Runnable action) {
        return new UiRow(title, subtitle, trailing, actionLabel, onAction, onClick, selected,
                actionDescription, clickLabel, label, description, action);
    }

    public String getSecondaryLabel() {
        return secondaryLabel;
    }

    public String getSecondaryDescription() {
        return secondaryDescription;
    }

    public Runnable getOnSecondary() {
        return onSecondary;
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

    public boolean isSelected() {
        return selected;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public String getClickLabel() {
        return clickLabel;
    }
}
