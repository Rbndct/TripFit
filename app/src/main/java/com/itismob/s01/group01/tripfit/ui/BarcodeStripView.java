package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.itismob.s01.group01.tripfit.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The hero trip ticket's closing strip: a barcode unique to the trip, seeded from its id. */
public class BarcodeStripView extends View {

    private static final float MIN_BAR_WIDTH_DP = 2f;
    private static final float MAX_BAR_WIDTH_DP = 6f;
    private static final float GAP_WIDTH_DP = 3f;

    private final Paint barPaint = new Paint();
    private String seed = "";

    public BarcodeStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(getResources().getColor(R.color.ticket_barcode_bar, context.getTheme()));
    }

    /** Seeds the bar pattern so the same trip always draws the same "barcode". */
    public void setSeed(String seed) {
        this.seed = seed != null ? seed : "";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float minWidth = MIN_BAR_WIDTH_DP * density;
        float maxWidth = MAX_BAR_WIDTH_DP * density;
        float gap = GAP_WIDTH_DP * density;

        Random random = new Random(seed.hashCode());
        List<Float> widths = new ArrayList<>();
        float used = 0f;
        while (used < getWidth()) {
            float width = minWidth + random.nextFloat() * (maxWidth - minWidth);
            widths.add(width);
            used += width + gap;
        }

        float x = (getWidth() - (used - gap)) / 2f;
        for (float width : widths) {
            canvas.drawRect(x, 0, x + width, getHeight(), barPaint);
            x += width + gap;
        }
    }
}
