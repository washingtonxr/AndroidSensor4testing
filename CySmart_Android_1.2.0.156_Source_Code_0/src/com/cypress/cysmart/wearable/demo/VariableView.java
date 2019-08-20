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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cypress.cysmart.R;
import com.cypress.cysmart.wearable.model.Variable;

public class VariableView extends LinearLayout {

    private TextView mName;
    protected VariableTargetProgressBar mProgress;

    public VariableView(Context context) {
        super(context);
        initializeViews(context);
    }

    public VariableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public VariableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeViews(context);
    }

    public void setVariable(Variable var) {
        mName.setText(var.mName);
        mProgress.reset();
        mProgress.setValue(var.getValueString());
        if (var.mHasTarget) {
            mProgress.setMinValue("" + var.getMinValue());
            mProgress.setMaxValue("" + var.getMaxValue());
            if (var.getMaxValue() == 0) {
                mProgress.setProgress(0);
            } else {
                mProgress.setMax((int) var.getMaxValue()); // TODO
                mProgress.setProgress((int) var.getValue()); // TODO
            }
        }
        mProgress.setUnit(var.getUnit() != null ? var.getUnit().toString() : null);
        mProgress.redraw(); // required for the case when setProgress() is not being called
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mName = (TextView) findViewById(R.id.name);
        mProgress = (VariableTargetProgressBar) findViewById(R.id.progress);
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.wearable_demo_variable_view, this);
    }
}
