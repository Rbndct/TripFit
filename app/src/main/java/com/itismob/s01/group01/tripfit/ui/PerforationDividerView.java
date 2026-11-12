package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.color.MaterialColors;

/** The hero trip ticket's signature divider: a row of punch-hole dots, like a perforated stub. */
public class PerforationDividerView extends View {

    private static final float DOT_RADIUS_DP = 2f;
    private static final float DOT_GAP_DP = 8f;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float dotRadiusPx;
    private float dotGapPx;

    public PerforationDividerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dotPaint.setColor(MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOutline, Color.GRAY));
        dotRadiusPx = dpToPx(DOT_RADIUS_DP);
        dotGapPx = dpToPx(DOT_GAP_DP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerY = getHeight() / 2f;
        float step = dotRadiusPx * 2 + dotGapPx;
        int dotCount = (int) (getWidth() / step);
        float totalWidth = dotCount * step - dotGapPx;
        float startX = (getWidth() - totalWidth) / 2f + dotRadiusPx;
        for (int i = 0; i < dotCount; i++) {
            canvas.drawCircle(startX + i * step, centerY, dotRadiusPx, dotPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
