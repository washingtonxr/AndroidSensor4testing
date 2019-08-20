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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cypress.cysmart.R;
import com.cypress.cysmart.wearable.model.Variable;

public class VariableTargetDialogBuilder implements DialogInterface.OnShowListener, View.OnClickListener {

    private Context mContext;
    private Dialog mParent;
    private Variable mVariable;
    private EditText mValue;
    private AlertDialog mDialog;

    public Dialog build(Context context, Dialog parent, Variable variable) {
        this.mContext = context;
        this.mParent = parent;
        this.mVariable = variable;
        mDialog = new AlertDialog.Builder(context)
                .setView(createView(context))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", null) // Set to null. We override the onclick
                .setCancelable(false)
                .create();
        mDialog.setOnShowListener(this);
        return mDialog;
    }

    @NonNull
    private View createView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.wearable_variable_target_dialog, null);
        mValue = (EditText) view.findViewById(R.id.value);
        if (mVariable.getMaxValue() != 0) {
            mValue.setText("" + mVariable.getMaxValue());
        }
        return view;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(this);
        ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE)
                .setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mDialog.getButton(DialogInterface.BUTTON_POSITIVE)) {
            Editable text = mValue.getText();
            try {
                mVariable.setMaxValue(Double.parseDouble(text.toString()));
                mDialog.dismiss();
                mParent.cancel();
            } catch (NumberFormatException e) {
                Toast.makeText(mContext, "Invalid value", Toast.LENGTH_SHORT).show();
            }
        } else {
            mDialog.dismiss();
            mParent.cancel();
        }
    }
}
