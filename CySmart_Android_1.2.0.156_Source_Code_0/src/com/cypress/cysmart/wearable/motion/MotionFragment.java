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

package com.cypress.cysmart.wearable.motion;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;
import com.cypress.cysmart.wearable.AbstractFragment;
import com.cypress.cysmart.wearable.parser.MotionDataParser;
import com.cypress.cysmart.wearable.parser.MotionFeatureParser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// TODO:
// bulk update?
// collapsed by default? all collapsed = disabled
// variables enable/disable (Control charact)
public class MotionFragment extends AbstractFragment implements MotionListAdapter.Listener {

    public static final String TAG = "Motion Sensor Fragment";
    private static final String[] CHARACTERISTICS = {
            GattAttributes.WEARABLE_MOTION_FEATURE_CHARACTERISTIC,
            GattAttributes.WEARABLE_MOTION_DATA_CHARACTERISTIC,
            GattAttributes.WEARABLE_MOTION_CONTROL_CHARACTERISTIC,
            GattAttributes.HEIGHT,
            GattAttributes.WEIGHT,
            GattAttributes.AGE
    };

    private BluetoothGattService mService;
    private Map<String, BluetoothGattCharacteristic> mCharacteristics = new HashMap<>();

    private MotionListAdapter mListAdapter;
    private boolean mDisablingAllEnabledCharacteristics;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                if (extras.containsKey(Constants.EXTRA_BYTE_SERVICE_UUID_VALUE)
                        && extras.containsKey(Constants.EXTRA_BYTE_UUID_VALUE)) {

                    String serviceUuid = extras.getString(Constants.EXTRA_BYTE_SERVICE_UUID_VALUE);
                    String charactUuid = extras.getString(Constants.EXTRA_BYTE_UUID_VALUE);
                    byte[] b = extras.getByteArray(Constants.EXTRA_BYTE_VALUE);
                    if (GattAttributes.WEARABLE_MOTION_SERVICE.equalsIgnoreCase(serviceUuid)) {

                        if (GattAttributes.WEARABLE_MOTION_FEATURE_CHARACTERISTIC.equalsIgnoreCase(charactUuid)) {

                            toggleProgressOff();
                            MotionFeatureParser.MotionFeature f = MotionFeatureParser.parse(b);
                            setAdapter(f);
                            enableNotifications(getNotifiableCharacteristics());
                        } else if (GattAttributes.WEARABLE_MOTION_DATA_CHARACTERISTIC.equalsIgnoreCase(charactUuid)) {

                            MotionFeatureParser.MotionFeature f = MotionFeatureParser.parse(b);
                            MotionDataParser.MotionData d = MotionDataParser.parse(b);
                            updateMotionData(f, d);
                        } else if (GattAttributes.WEARABLE_MOTION_CONTROL_CHARACTERISTIC.equalsIgnoreCase(charactUuid)) {

                            // TODO
                        } else if (GattAttributes.HEIGHT.equalsIgnoreCase(charactUuid)) {

                            mListAdapter.processHeightData(b);
                        } else if (GattAttributes.WEIGHT.equalsIgnoreCase(charactUuid)) {

                            mListAdapter.processWeightData(b);
                        } else if (GattAttributes.AGE.equalsIgnoreCase(charactUuid)) {

                            mListAdapter.processAgeData(b);
                        }
                    }
                }
            } else if (BluetoothLeService.ACTION_WRITE_SUCCESS.equals(action)) {

                if (mDisablingAllEnabledCharacteristics) {

                    if (BluetoothLeService.mEnabledCharacteristics.size() == 0) {

                        mDisablingAllEnabledCharacteristics = false;
                        toggleProgressOff();
                        Toast.makeText(getActivity(), getResources().getString(R.string.profile_control_stop_both_notify_indicate_toast),
                                Toast.LENGTH_SHORT).show();
                        getFragmentManager().popBackStackImmediate();
                    }
                }
            } else if (BluetoothLeService.ACTION_WRITE_COMPLETED.equals(action)) {

                toggleProgressOff();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.err.println("--MOT: CREATE");
        super.onCreate(savedInstanceState);
        initGatt();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.err.println("--MOT: CREATE_VIEW");
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.wearable_motion_fragment, container, false);
        mListView = (ExpandableListView) root.findViewById(R.id.list);
        mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true; // disable group click, instead use group indicator to expand/collapse
            }
        });

        registerBroadcastReceiver(mReceiver, getIntentFilter());

        // read features
        toggleProgressOn("Reading Motion Features,\nPlease wait..."); // TODO: externalize
        BluetoothLeService.readCharacteristic(
                mCharacteristics.get(GattAttributes.WEARABLE_MOTION_FEATURE_CHARACTERISTIC));

        Utils.setUpActionBar(getActivity(), R.string.wearable_motion_action_bar_title);

        return root;
    }

    @Override
    public void onResume() {
        System.err.println("--MOT: RESUME");
        super.onResume();
        registerBroadcastReceiver(mReceiver, getIntentFilter());
    }

    @Override
    public void onPause() {
        System.err.println("--MOT: PAUSE");
        unregisterBroadcastReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        System.err.println("--MOT: DESTROY");
        disableAllNotifications();
        super.onDestroy();
    }

    public void handleBackPressed() {
        if (BluetoothLeService.mEnabledCharacteristics.size() == 0) {
            getFragmentManager().popBackStackImmediate();
        } else if (!mDisablingAllEnabledCharacteristics) {
            mDisablingAllEnabledCharacteristics = true;
            toggleProgressOn("Disabling notifications,\nPlease wait..."); // TODO: externalize
            disableAllNotifications(false);
        }
    }

    @Override
    public void onRefreshChildView(int groupPosition, int childPosition) {
        refreshChildView(groupPosition, childPosition);
    }

    @Override
    public void onCollapseGroup(int groupPosition) {
        mListView.collapseGroup(groupPosition);
    }

    @Override
    public void onExpandGroup(int groupPosition) {
        mListView.expandGroup(groupPosition);
    }

    @Override
    public void onApplyHeight(byte[] b) {
        BluetoothGattCharacteristic c = mCharacteristics.get(GattAttributes.HEIGHT);
        c.setValue(b);
        BluetoothLeService.writeCharacteristic(c);
    }

    @Override
    public void onApplyWeight(byte[] b) {
        BluetoothGattCharacteristic c = mCharacteristics.get(GattAttributes.WEIGHT);
        c.setValue(b);
        BluetoothLeService.writeCharacteristic(c);
    }

    @Override
    public void onApplyAge(byte[] b) {
        BluetoothGattCharacteristic c = mCharacteristics.get(GattAttributes.AGE);
        c.setValue(b);
        BluetoothLeService.writeCharacteristic(c);
    }

    private void setAdapter(MotionFeatureParser.MotionFeature f) {
        boolean isAnyFeature = f.mIsAcc || f.mIsGyr || f.mIsMag || f.mIsOrientation || f.mIsSteps || f.mIsCalories;
        if (isAnyFeature) {
            // TODO: make adapter bulletproof
            mListAdapter = new MotionListAdapter(getActivity(), this,
                    f.mIsAcc, f.mIsGyr, f.mIsMag, f.mIsOrientation, f.mIsSteps, f.mIsCalories);
            mListView.setAdapter(mListAdapter);
            for (int i = 0; i < mListAdapter.getGroupCount(); i++) {
                mListView.expandGroup(i);
            }
        }
    }

    private void initGatt() {
        mService = null;
        for (BluetoothGattService s : BluetoothLeService.getSupportedGattServices()) {
            if (UUIDDatabase.UUID_WEARABLE_MOTION_SERVICE.equals(s.getUuid())) {
                mService = s;
                for (BluetoothGattCharacteristic c : mService.getCharacteristics()) {
                    for (String k : CHARACTERISTICS) {
                        if (k.equalsIgnoreCase(c.getUuid().toString())) {
                            //-- TODO: bug in BLE project - 2 characteristics (data and control) share same UUID
                            if (!mCharacteristics.containsKey(k)) {
                                mCharacteristics.put(k, c);
                            }
                            //--
                            break;
                        }
                    }
                }
                break;
            }
        }
        // TODO: check all characteristics found
    }

    private void updateMotionData(MotionFeatureParser.MotionFeature f, MotionDataParser.MotionData d) {
        if (f.mIsAcc) {
            mListAdapter.processAccelerometerData(d);
        }
        if (f.mIsGyr) {
            mListAdapter.processGyroscopeData(d);
        }
        if (f.mIsMag) {
            mListAdapter.processMagnetometerData(d);
        }
        if (f.mIsOrientation) {
            mListAdapter.processOrientationData(d);
        }
        if (f.mIsSteps) {
            mListAdapter.processStepsData(d);
        }
        if (f.mIsCalories) {
            mListAdapter.processCaloriesData(d);
        }
    }

    @NonNull
    private List<BluetoothGattCharacteristic> getNotifiableCharacteristics() {
        List<BluetoothGattCharacteristic> list = new LinkedList<>();
        if (mCharacteristics.containsKey(GattAttributes.WEARABLE_MOTION_DATA_CHARACTERISTIC)) {
            list.add(mCharacteristics.get(GattAttributes.WEARABLE_MOTION_DATA_CHARACTERISTIC));
        }
        return list;
    }
}
