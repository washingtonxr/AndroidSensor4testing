/*
 * Copyright Cypress Semiconductor Corporation, 2014-2018 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign), United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 *
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 *
 *
 */
package com.cypress.cysmart.wearable.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ProgressBar;

import com.cypress.cysmart.R;

import java.text.NumberFormat;

/**
 * Custom progressBar with text
 */
public class VariableTargetProgressBar extends ProgressBar {

    private static final int PADDING = 30;

    private String mMinValue;
    private String mMaxValue;
    private String mValue;
    private String mUnit;
    private Paint mPaint;

    public VariableTargetProgressBar(Context context) {
        super(context);
        initializeProgressBar();
    }

    public VariableTargetProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeProgressBar();
    }

    public VariableTargetProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeProgressBar();
    }

    public void initializeProgressBar() {
        reset();
        mPaint = new Paint();
        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_TV:
                mPaint.setTextSize(14);
                break;
            case DisplayMetrics.DENSITY_HIGH:
                mPaint.setTextSize(16);
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                mPaint.setTextSize(30);
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                mPaint.setTextSize(40);
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                mPaint.setTextSize(50);
                break;
            default:
                mPaint.setTextSize(50);
                break;
        }
        mPaint.setColor(getResources().getColor(R.color.main_bg_color));
//        this.setBackgroundColor(Color.WHITE);

//        final float[] roundedCorners = new float[]{5, 5, 5, 5, 5, 5, 5, 5};
//        Drawable pgDrawable = new ShapeDrawable(new RoundRectShape(roundedCorners, null, null));
//        ClipDrawable progress = new ClipDrawable(pgDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL);
//        this.setProgressDrawable(progress);
//        this.setBackgroundDrawable(progress);
//        this.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.progress_horizontal));
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Rect minvalBounds = new Rect();
        mPaint.getTextBounds(mMinValue, 0, mMinValue.length(), minvalBounds);
        Rect valBounds = new Rect();
        mPaint.getTextBounds(mValue, 0, mValue.length(), valBounds);
        Rect maxvalBounds = new Rect();
        mPaint.getTextBounds(mMaxValue, 0, mMaxValue.length(), maxvalBounds);
        Rect unitBounds = new Rect();
        mPaint.getTextBounds(mUnit, 0, mUnit.length(), unitBounds);

        int minvalX = PADDING;
        int valX = getWidth() / 2 - valBounds.centerX();
        int unitX = getWidth() - unitBounds.width() - PADDING;

        int maxvalX = Math.max(getWidth() * 3 / 4, valX + valBounds.width() + PADDING);
        maxvalX = Math.min(unitX - maxvalBounds.width() - PADDING, maxvalX);

        unitX = Math.min(maxvalX + maxvalBounds.width() + PADDING, unitX);

        int y = getHeight() / 2 - minvalBounds.centerY();
        canvas.drawText(mMinValue, minvalX, y, mPaint);

        y = getHeight() / 2 - valBounds.centerY();
        canvas.drawText(mValue, valX, y, mPaint);

        y = getHeight() / 2 - maxvalBounds.centerY();
        canvas.drawText(mMaxValue, maxvalX, y, mPaint);

        y = getHeight() / 2 - unitBounds.centerY();
        canvas.drawText(mUnit, unitX, y, mPaint);
    }

    public void reset() {
        mMinValue = mMaxValue = mValue = mUnit = "";
        setMax(1);
        setProgress(0);
    }

    public void setMinValue(String minValue) {
        this.mMinValue = minValue;
    }

    public void setMaxValue(String maxValue) {
        this.mMaxValue = maxValue;
    }

    public void setValue(String value) {
        this.mValue = value;
    }

    public void setUnit(String unit) {
        if (unit != null) {
            this.mUnit = unit;
        }
    }

    public void redraw() {
        // Following is a trick to redraw a progress bar
        final int orig = getProgress();
        if (orig > 0) {
            setProgress(orig - 1);
        } else {
            setProgress(orig + 1);
        }
        setProgress(orig);
    }
}
