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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.cypress.cysmart.R;
import com.cypress.cysmart.wearable.model.ValueWithUnit;
import com.cypress.cysmart.wearable.model.Variable;

public class VariableUnitsDialog extends Dialog implements AdapterView.OnItemClickListener, DialogInterface.OnCancelListener {

    private final Dialog mParent;
    private final Variable mVariable;
    private ArrayAdapter<ValueWithUnit.Unit> mListAdapter;

    public VariableUnitsDialog(Context context, Dialog parent, Variable variable) {
        super(context);
        this.mParent = parent;
        this.mVariable = variable;
        setOnCancelListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        View root = getLayoutInflater().inflate(R.layout.wearable_dialog_list, null, false);
        ListView listView = (ListView) root.findViewById(R.id.listView1);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        mListAdapter = new ArrayAdapter(getContext(),
                android.R.layout.simple_list_item_single_choice, mVariable.getSupportedUnits()) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView root = (CheckedTextView) super.getView(position, convertView, parent);
                return root;
            }
        };
        listView.setAdapter(mListAdapter);
        for (int i = 0; i < mListAdapter.getCount(); i++) {
            ValueWithUnit.Unit item = mListAdapter.getItem(i);
            if (mVariable.getUnit().equals(item)) {
                listView.setItemChecked(i, true);
            }
        }
        listView.setOnItemClickListener(this);
        setContentView(root);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mVariable.setUnit(mListAdapter.getItem(position));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mParent.cancel(); // dismiss parent dialog as well
    }
}
