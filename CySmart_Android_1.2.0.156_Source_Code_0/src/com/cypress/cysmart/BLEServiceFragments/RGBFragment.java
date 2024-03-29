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

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.BLEProfileDataParserClasses.RGBParser;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.util.List;

/**
 * Fragment to display the RGB service
 */
public class RGBFragment extends Fragment {

    // GATT service and characteristics
    private static BluetoothGattService mRgbService;
    private static BluetoothGattCharacteristic mRgbCharacteristic;

    // Data view variables
    private ImageView mRgbCanvas;
    private ImageView mColorPicker;
    private ViewGroup mViewContainer;
    private TextView mTextRed;
    private TextView mTextGreen;
    private TextView mTextBlue;
    private TextView mTextIntensity;
    private ImageView mColorIndicator;
    private SeekBar mIntensityBar;
    private RelativeLayout mParentRelLayout;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    // Data variables
    private float mWidth;
    private float mHeight;
    private View mRootView;

    private Bitmap mBitmap;
    private int mRed, mGreen, mBlue, mIntensity;

    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    Logger.i("Bonding is in process....");
                    Utils.showBondingProgressDialog(getActivity(), mProgressDialog);
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + BluetoothLeService.getBluetoothDeviceName() + "|"
                            + BluetoothLeService.getBluetoothDeviceAddress() + "]" +
                            getResources().getString(R.string.dl_commaseparator) +
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.dataLog(dataLog);
                    Utils.hideBondingProgressDialog(mProgressDialog);
                    getGattData();
                } else if (state == BluetoothDevice.BOND_NONE) {
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + BluetoothLeService.getBluetoothDeviceName() + "|"
                            + BluetoothLeService.getBluetoothDeviceAddress() + "]" +
                            getResources().getString(R.string.dl_commaseparator) +
                            getResources().getString(R.string.dl_connection_unpaired);
                    Logger.dataLog(dataLog);
                    Utils.hideBondingProgressDialog(mProgressDialog);
                }
            } else if (action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)) {
                Bundle extras = intent.getExtras();
                if (extras.containsKey(Constants.EXTRA_RGB_VALUE)) {
                    String strRGBA = extras.getString(Constants.EXTRA_RGB_VALUE);
                    int rgba = RGBParser.parseRGBAString(strRGBA);
                    mRed = RGBParser.red(rgba);
                    mGreen = RGBParser.green(rgba);
                    mBlue = RGBParser.blue(rgba);
                    mIntensity = RGBParser.alpha(rgba);
                    updateUI(false);
                }
            }
        }
    };

    public static RGBFragment create(BluetoothGattService rgbService) {
        mRgbService = rgbService;
        return new RGBFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mRootView = inflater.inflate(R.layout.rgb_view_landscape, container, false);
        } else {
            mRootView = inflater.inflate(R.layout.rgb_view_portrait, container, false);
        }
        getActivity().getActionBar().setTitle(R.string.rgb_led);
        setupControls();
//        getColorPickerInitialPosition();
        setHasOptionsMenu(true);
        return mRootView;
    }

//    private void getColorPickerInitialPosition() {
//        ViewTreeObserver observer = mColorPicker.getViewTreeObserver();
//        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                mColorPicker.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                int[] locations = new int[2];
//                mColorPicker.getLocationOnScreen(locations);
//                int x = locations[0];
//                int y = locations[1];
//                if (x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
//                    int p = mBitmap.getPixel(x, y);
//                    int rgb = getRGB(p);
//                    if (rgb != 0) {
//                        mRed = Color.red(p);
//                        mGreen = Color.green(p);
//                        mBlue = Color.blue(p);
//                        Logger.i(Utils.formatForRootLocale("RGB: 0x%02x%02x%02x", mRed, mGreen, mBlue));
//                        updateUI();
//                    }
//                }
//            }
//        });
//    }

    private void updateColorPickerPosition() {
        float x = getWidth() * mRgbCanvas.getMeasuredWidth();
        float y = (1.f - getHeight()) * mRgbCanvas.getMeasuredHeight();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mColorPicker.getLayoutParams();
        layoutParams.leftMargin = (int) (mRgbCanvas.getLeft() + x - Math.floor(mColorPicker.getMeasuredWidth() / 2) - mViewContainer.getPaddingLeft());
        layoutParams.topMargin = (int) (mRgbCanvas.getTop() + y - Math.floor(mColorPicker.getMeasuredHeight() / 2) - mViewContainer.getPaddingTop());
        mColorPicker.setLayoutParams(layoutParams);
    }

    /**
     * Method to set up the gamut view
     */
    void setupControls() {
        mParentRelLayout = (RelativeLayout) mRootView.findViewById(R.id.parent);
        mParentRelLayout.setClickable(true);
        mRgbCanvas = (ImageView) mRootView.findViewById(R.id.img_rgb_canvas);
        mColorPicker = (ImageView) mRootView.findViewById(R.id.img_color_picker);
        mColorPicker.setVisibility(View.INVISIBLE);

        mTextIntensity = (TextView) mRootView.findViewById(R.id.txt_intensity);
        mTextRed = (TextView) mRootView.findViewById(R.id.txt_red);
        mTextGreen = (TextView) mRootView.findViewById(R.id.txt_green);
        mTextBlue = (TextView) mRootView.findViewById(R.id.txt_blue);
        mColorIndicator = (ImageView) mRootView.findViewById(R.id.txt_color_indicator);
        mViewContainer = (ViewGroup) mRootView.findViewById(R.id.view_group);
        mIntensityBar = (SeekBar) mRootView.findViewById(R.id.intensity_changer);
        mProgressDialog = new ProgressDialog(getActivity());
        BitmapDrawable bitmapDrawable = (BitmapDrawable) mRgbCanvas.getDrawable();
        mBitmap = bitmapDrawable.getBitmap();
        mRgbCanvas.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_UP) {

                    float x = event.getX();
                    float y = event.getY();

                    if (x >= 0 && y >= 0 && x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
                        int p = mBitmap.getPixel((int) x, (int) y);
                        int rgb = getRGB(p);
                        if (rgb != 0) {
                            if (x > mRgbCanvas.getMeasuredWidth()) {
                                x = mRgbCanvas.getMeasuredWidth();
                            }
                            if (y > mRgbCanvas.getMeasuredHeight()) {
                                y = mRgbCanvas.getMeasuredHeight();
                            }
                            setWidth(1.f / mRgbCanvas.getMeasuredWidth() * x);
                            setHeight(1.f - (1.f / mRgbCanvas.getMeasuredHeight() * y));
                            mRed = Color.red(p);
                            mGreen = Color.green(p);
                            mBlue = Color.blue(p);
                            updateUI();
                            mColorPicker.setVisibility(View.VISIBLE);
                            updateColorPickerPosition();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        mIntensity = mIntensityBar.getProgress();
        mTextIntensity.setText(Utils.formatForRootLocale("0x%02x", mIntensity));
        // Seek bar progress change listener
        mIntensityBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mIntensity = progress;
                updateUI();
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                BluetoothLeService.writeCharacteristicRGB(mRgbCharacteristic,
                        mRed, mGreen, mBlue, mIntensity);
            }
        });
    }

    private int getRGB(int color) {
        return ((Color.red(color) << 16) | (Color.green(color) << 8) | Color.blue(color)) & 0x00FFFFFF;
    }

    @Override
    public void onResume() {
        BluetoothLeService.registerBroadcastReceiver(getActivity(), mGattUpdateReceiver, Utils.makeGattUpdateIntentFilter());
        getGattData();
        Utils.setUpActionBar(getActivity(), R.string.rgb_led);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        BluetoothLeService.unregisterBroadcastReceiver(getActivity(), mGattUpdateReceiver);
        super.onDestroy();
    }

    private void updateUI() {
        updateUI(true);
    }

    private void updateUI(boolean writeCharacteristic) {
        String hexColor = Utils.formatForRootLocale("#%02x%02x%02x%02x", mIntensity, mRed, mGreen, mBlue);
        mColorIndicator.setBackgroundColor(Color.parseColor(hexColor));
        mTextIntensity.setText(Utils.formatForRootLocale("0x%02x", mIntensity));
        mIntensityBar.setProgress(mIntensity);
        mTextRed.setText(Utils.formatForRootLocale("0x%02x", mRed));
        mTextBlue.setText(Utils.formatForRootLocale("0x%02x", mBlue));
        mTextGreen.setText(Utils.formatForRootLocale("0x%02x", mGreen));
        if (writeCharacteristic) {
            try {
                Logger.i(Utils.formatForRootLocale("Writing ARGB: 0x%02x%02x%02x%02x", mIntensity, mRed, mGreen, mBlue));
                BluetoothLeService.writeCharacteristicRGB(mRgbCharacteristic, mRed, mGreen, mBlue, mIntensity);
            } catch (Exception e) {
                Logger.e(Utils.formatForRootLocale("Failed to write ARGB: 0x%02x%02x%02x%02x", mIntensity, mRed, mGreen, mBlue));
            }
        }
    }

    /**
     * Method to get required characteristics from service
     */
    private void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mRgbService.getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String characteristicUUID = gattCharacteristic.getUuid().toString();
            if (characteristicUUID.equalsIgnoreCase(GattAttributes.RGB_LED) || characteristicUUID.equalsIgnoreCase(GattAttributes.RGB_LED_CUSTOM)) {
                mRgbCharacteristic = gattCharacteristic;
                //Read the characteristic to set initial cursor position
                BluetoothLeService.readCharacteristic(mRgbCharacteristic);
                break;
            }
        }
    }

    private float getWidth() {
        return mWidth;
    }

    private void setWidth(float width) {
        mWidth = width;
    }

    private float getHeight() {
        return mHeight;
    }

    private void setHeight(float height) {
        mHeight = height;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.global, menu);
        MenuItem graph = menu.findItem(R.id.graph);
        MenuItem log = menu.findItem(R.id.log);
        MenuItem search = menu.findItem(R.id.search);
        search.setVisible(false);
        graph.setVisible(false);
        log.setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mRootView = inflater.inflate(R.layout.rgb_view_landscape, null);
            ViewGroup rootViewGroup = (ViewGroup) getView();
            // Remove all the existing views from the root view.
            rootViewGroup.removeAllViews();
            rootViewGroup.addView(mRootView);
            setupControls();
//            getColorPickerInitialPosition();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mRootView = inflater.inflate(R.layout.rgb_view_portrait, null);
            ViewGroup rootViewGroup = (ViewGroup) getView();
            // Remove all the existing views from the root view.
            rootViewGroup.removeAllViews();
            rootViewGroup.addView(mRootView);
            setupControls();
//            getColorPickerInitialPosition();
        }
    }
}
