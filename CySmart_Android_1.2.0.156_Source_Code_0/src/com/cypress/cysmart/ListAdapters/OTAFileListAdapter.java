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


package com.cypress.cysmart.ListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cypress.cysmart.DataModelClasses.OTAFileModel;
import com.cypress.cysmart.R;

import java.util.ArrayList;

public class OTAFileListAdapter extends BaseAdapter {

    ArrayList<OTAFileModel> mFileList = new ArrayList<OTAFileModel>();
    LayoutInflater mInflater;
    int mRequiredFilesCount;
    Context mContext;

    public OTAFileListAdapter(Context context, ArrayList<OTAFileModel> fileList,
                              int requiredFilesCount) {
        this.mFileList = fileList;
        this.mContext = context;
        this.mRequiredFilesCount = requiredFilesCount;
        mInflater = LayoutInflater.from(this.mContext);        // only context can also be used
    }


    @Override
    public int getCount() {
        return mFileList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MyViewHolder mViewHolder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.listitem_firmware, null);
            mViewHolder = new MyViewHolder();
            mViewHolder.fileName = (TextView) convertView.findViewById(R.id.file_name);
            mViewHolder.layout =  (LinearLayout) convertView.findViewById(R.id.itemParent);
            mViewHolder.fileSelect = (CheckBox) convertView.findViewById(R.id.file_checkbox);
            convertView.setTag(mViewHolder);
        }
        mViewHolder = (MyViewHolder) convertView.getTag();
        OTAFileModel file = mFileList.get(position);
        mViewHolder.fileName.setText(file.getFileName());
        mViewHolder.fileSelect.setChecked(file.isSelected());

        return convertView;
    }

    public void addFiles(ArrayList<OTAFileModel> fileModels) {
        this.mFileList = fileModels;
    }

    private class MyViewHolder {
        TextView fileName;
        CheckBox fileSelect;
        LinearLayout layout;
    }

}
