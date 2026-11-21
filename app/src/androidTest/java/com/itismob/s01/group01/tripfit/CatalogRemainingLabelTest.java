package com.itismob.s01.group01.tripfit;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Discover's remaining figure is the limit minus the larger of planned and logged costs, while the
 * Budget tracker counts logged spending only. The label must say what it counts so the two screens
 * do not read as contradicting each other.
 */
@RunWith(AndroidJUnit4.class)
public class CatalogRemainingLabelTest {

    @Test
    public void discoverRemainingLabelNamesPlannedCosts() {
        Context context = ApplicationProvider.getApplicationContext();
        String label = context.getString(R.string.catalog_remaining, "P3,628.06", "Activities");
        assertTrue("Label should mention planned costs: " + label,
                label.toLowerCase(java.util.Locale.ROOT).contains("planned"));
        assertTrue("Label should keep the amount: " + label, label.contains("P3,628.06"));
        assertTrue("Label should keep the category: " + label, label.contains("Activities"));
    }
}
