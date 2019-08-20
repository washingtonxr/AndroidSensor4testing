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
import android.graphics.LightingColorFilter;
import android.util.AttributeSet;

import com.cypress.cysmart.wearable.model.Variable;

public class UvVariableView extends VariableView {

    private static final int GREEN = 0x00FF00;
    private static final int YELLOW = 0xFFFF00;
    private static final int ORANGE = 0xFFA500;
    private static final int RED = 0xFF0000;
    private static final int VIOLET = 0x8A2BE2;
    private static final int LOW = 3;
    private static final int MODERATE = 6;
    private static final int HIGH = 8;
    public static final int VERY_HIGH = 11;

    public UvVariableView(Context context) {
        super(context);
    }

    public UvVariableView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UvVariableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setVariable(Variable var) {
        super.setVariable(var);
        mProgress.setMinValue("" + var.getMinValue());
        mProgress.setMaxValue("" + var.getMaxValue() + "+");
        mProgress.setMax(1);
        mProgress.setProgress(1);
        mProgress.getProgressDrawable().setColorFilter(new LightingColorFilter(0xFF000000, getColor(var.getValue())));
    }

    private int getColor(double value) {
        int color;
        // low
        if (value < LOW) {
            color = GREEN;
        }
        // moderate
        else if (value < MODERATE) {
            color = YELLOW;
        }
        // high
        else if (value < HIGH) {
            color = ORANGE;
        }
        // very high
        else if (value < VERY_HIGH) {
            return RED;
        }
        // extreme
        else {
            color = VIOLET;
        }
        return color;
    }
}
