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

package com.cypress.cysmart.BLEServiceFragments;

import android.app.ActionBar;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.DepthPageTransformer;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.PagerFooterview;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fragment to display the CapSenseService
 */
public class CapsenseService extends Fragment {

    // Service and characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mNotifyCharacteristicProximity;
    private static BluetoothGattCharacteristic mNotifyCharacteristicSlider;
    private static BluetoothGattCharacteristic mNotifyCharacteristicButtons;

    private int mPositionCapsenseProximity = -1;
    private int mPositionCapsenseSlider = -1;
    private int mPositionCapsenseButtons = -1;

    // Flag for notify
    private boolean mNotifySet = false;

    // Separate fragments for each capsense service
    private CapsenseServiceProximity mCapsenseProximity;
    private CapsenseServiceSlider mCapsenseSlider;
    private CapsenseServiceButtons mCapsenseButtons;

    // ViewPager variables
    private static int mViewpagerCount;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private PagerFooterview mPagerView;
    private LinearLayout mPagerLayout;

    // Fragment list
    private ArrayList<Fragment> fragmentsList = new ArrayList<Fragment>();

    public static CapsenseService create(BluetoothGattService service, int pageCount) {
        mService = service;
        mViewpagerCount = pageCount;
        return new CapsenseService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.capsense_main, container,
                false);

        mCapsenseProximity = new CapsenseServiceProximity();
        mCapsenseSlider = new CapsenseServiceSlider();
        mCapsenseButtons = new CapsenseServiceButtons();

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) rootView.findViewById(R.id.capsenseViewpager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getActivity()
                .getSupportFragmentManager());

        mPagerLayout = (LinearLayout) rootView
                .findViewById(R.id.capsense_page_indicator);
        mPagerView = new PagerFooterview(getActivity(), mViewpagerCount,
                mPagerLayout.getWidth());
        mPagerLayout.addView(mPagerView);

        if (mViewpagerCount == 1) {
            mPagerLayout.setVisibility(View.INVISIBLE);
        }

        /**
         * get required characteristics from service
         */
        int count = 0;
        List<BluetoothGattCharacteristic> gattCharacteristics = mService.getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            UUID uuidchara = gattCharacteristic.getUuid();
            if (uuidchara.equals(UUIDDatabase.UUID_CAPSENSE_PROXIMITY) || uuidchara.equals(UUIDDatabase.UUID_CAPSENSE_PROXIMITY_CUSTOM)) {
                Logger.i("UUID Characteristic Proximity"
                        + gattCharacteristic.getUuid().toString());
                mNotifyCharacteristicProximity = gattCharacteristic;
                if (!mNotifySet) {
                    mNotifySet = true;
                    prepareBroadcastDataNotify(mNotifyCharacteristicProximity);
                }
                fragmentsList.add(mCapsenseProximity.create(mService));
                mPositionCapsenseProximity = count++;
            } else if (uuidchara.equals(UUIDDatabase.UUID_CAPSENSE_SLIDER) || uuidchara.equals(UUIDDatabase.UUID_CAPSENSE_SLIDER_CUSTOM)) {
                Logger.i("UUID Characteristic Slider"
                        + gattCharacteristic.getUuid().toString());
                mNotifyCharacteristicSlider = gattCharacteristic;
                if (!mNotifySet) {
                    mNotifySet = true;
                    prepareBroadcastDataNotify(mNotifyCharacteristicSlider);
                }
                fragmentsList.add(mCapsenseSlider.create(mService));
                mPositionCapsenseSlider = count++;
            } else if (uuidchara.equals(UUIDDatabase.UUID_CAPSENSE_BUTTONS) || uuidchara.equals(UUIDDatabase.UUID_CAPSENSE_BUTTONS_CUSTOM)) {
                Logger.i("UUID Characteristic Buttons"
                        + gattCharacteristic.getUuid().toString());
                mNotifyCharacteristicButtons = gattCharacteristic;
                if (!mNotifySet) {
                    mNotifySet = true;
                    prepareBroadcastDataNotify(mNotifyCharacteristicButtons);
                }
                fragmentsList.add(mCapsenseButtons.create(mService));
                mPositionCapsenseButtons = count++;
            }
        }
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageTransformer(true, new DepthPageTransformer());
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                mPagerView.Update(position);
                if (position == mPositionCapsenseProximity) {
                    prepareBroadcastDataNotify(mNotifyCharacteristicProximity);
                    if (mNotifyCharacteristicSlider != null) {
                        stopBroadcastDataNotify(mNotifyCharacteristicSlider);
                    }
                    if (mNotifyCharacteristicButtons != null) {
                        stopBroadcastDataNotify(mNotifyCharacteristicButtons);
                    }
                } else if (position == mPositionCapsenseSlider) {
                    prepareBroadcastDataNotify(mNotifyCharacteristicSlider);
                    if (mNotifyCharacteristicProximity != null) {
                        stopBroadcastDataNotify(mNotifyCharacteristicProximity);
                    }
                    if (mNotifyCharacteristicButtons != null) {
                        stopBroadcastDataNotify(mNotifyCharacteristicButtons);
                    }
                } else if (position == mPositionCapsenseButtons) {
                    prepareBroadcastDataNotify(mNotifyCharacteristicButtons);
                    if (mNotifyCharacteristicSlider != null) {
                        stopBroadcastDataNotify(mNotifyCharacteristicSlider);
                    }
                    if (mNotifyCharacteristicProximity != null) {
                        stopBroadcastDataNotify(mNotifyCharacteristicProximity);
                    }
                } else {
                    Logger.e("Unknown position: " + position);
                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                //Not needed
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                //Not needed
            }
        });
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mNotifySet = false;
        BluetoothLeService.registerBroadcastReceiver(getActivity(), mGattUpdateReceiver, Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(), R.string.capsense);
    }

    @Override
    public void onDestroy() {
        BluetoothLeService.unregisterBroadcastReceiver(getActivity(), mGattUpdateReceiver);
        if (mNotifyCharacteristicSlider != null) {
            stopBroadcastDataNotify(mNotifyCharacteristicSlider);
        }
        if (mNotifyCharacteristicProximity != null) {
            stopBroadcastDataNotify(mNotifyCharacteristicProximity);
        }
        if (mNotifyCharacteristicButtons != null) {
            stopBroadcastDataNotify(mNotifyCharacteristicButtons);
        }
        super.onDestroy();
    }

    /**
     * A simple pager adapter that represents CapsenseFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentsList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentsList.size();
        }
    }

    /**
     * Preparing Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    true);
        }
    }

    /**
     * Stopping Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    private static void stopBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            if (gattCharacteristic != null) {
                Logger.d("Stopped notification");
                BluetoothLeService.setCharacteristicNotification(
                        gattCharacteristic, false);
            }
        }
    }

    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // GATT Data Available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras.containsKey(Constants.EXTRA_CAPPROX_VALUE)) {
                    int received_proximity_rate = extras
                            .getInt(Constants.EXTRA_CAPPROX_VALUE);
                    CapsenseServiceProximity.displayLiveData(context, received_proximity_rate);
                }
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.global, menu);
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setIcon(new ColorDrawable(getResources().getColor(
                    android.R.color.transparent)));
        }
        MenuItem graph = menu.findItem(R.id.graph);
        MenuItem log = menu.findItem(R.id.log);
        MenuItem search = menu.findItem(R.id.search);
//        MenuItem pairCache = menu.findItem(R.id.pairing);
//        if (Utils.getBooleanSharedPreference(getActivity(), Constants.PREF_PAIR_CACHE_STATUS)) {
//            pairCache.setChecked(true);
//        } else {
//            pairCache.setChecked(false);
//        }
        search.setVisible(false);
        graph.setVisible(false);
        log.setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
