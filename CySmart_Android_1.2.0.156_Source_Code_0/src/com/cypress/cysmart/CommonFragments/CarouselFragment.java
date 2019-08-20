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
package com.cypress.cysmart.CommonFragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.BLEServiceFragments.BatteryInformationService;
import com.cypress.cysmart.BLEServiceFragments.BloodPressureService;
import com.cypress.cysmart.BLEServiceFragments.CSCService;
import com.cypress.cysmart.BLEServiceFragments.CapsenseService;
import com.cypress.cysmart.BLEServiceFragments.DeviceInformationService;
import com.cypress.cysmart.BLEServiceFragments.FindMeService;
import com.cypress.cysmart.BLEServiceFragments.GlucoseService;
import com.cypress.cysmart.BLEServiceFragments.HealthTemperatureService;
import com.cypress.cysmart.BLEServiceFragments.HeartRateService;
import com.cypress.cysmart.BLEServiceFragments.RGBFragment;
import com.cypress.cysmart.BLEServiceFragments.RSCService;
import com.cypress.cysmart.BLEServiceFragments.SensorHubService;
import com.cypress.cysmart.CommonUtils.CarouselLinearLayout;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.GATTDBFragments.GattServicesFragment;
import com.cypress.cysmart.OTAFirmwareUpdate.OTAFirmwareUpgradeFragment;
import com.cypress.cysmart.R;
import com.cypress.cysmart.RDKEmulatorView.RemoteControlEmulatorFragment;
import com.cypress.cysmart.wearable.demo.DemoFragment;
import com.cypress.cysmart.wearable.location.LocationFragment;
import com.cypress.cysmart.wearable.motion.MotionFragment;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CarouselFragment extends Fragment {

    public final static String EXTRA_FRAG_DEVICE_ADDRESS = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_DEVICE_ADDRESS";
    private final static HashMap<String, BluetoothGattService> mBleHashMap = new HashMap<String, BluetoothGattService>();
    /**
     * Argument keys passed between fragments
     */
    private final static String EXTRA_FRAG_POS = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_POS";
    private final static String EXTRA_FRAG_SCALE = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_SCALE";
    private final static String EXTRA_FRAG_NAME = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_NAME";
    private final static String EXTRA_FRAG_UUID = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_UUID";
    /**
     * BluetoothGattCharacteristic Notify
     */
    public static BluetoothGattCharacteristic mNotifyCharacteristic;
    /**
     * BluetoothGattCharacteristic Read
     */
    public static BluetoothGattCharacteristic mReadCharacteristic;
    /**
     * BluetoothGattService current
     */
    private static BluetoothGattService mService;
    /**
     * Current UUID
     */
    private static UUID mCurrentUUID;
    /**
     * BluetoothGattCharacteristic List length
     */
    int mGattCharacteristicsLength = 0;
    /**
     * BluetoothGattCharacteristic current
     */
    BluetoothGattCharacteristic mCurrentCharacteristic;
    /**
     * CarouselView Image is actually a button
     */
    private Button mCarouselButton;

    /**
     * Fragment new Instance creation with arguments
     *
     * @param pos
     * @param scale
     * @param name
     * @param uuid
     * @param service
     * @return CarouselFragment
     */
    public static Fragment newInstance(int pos,
                                       float scale, String name, String uuid, BluetoothGattService service) {
        CarouselFragment mFragment = new CarouselFragment();
        if (service.getInstanceId() > 0) {
            uuid = uuid + service.getInstanceId();
        }
        mBleHashMap.put(uuid, service);
        Bundle mBundle = new Bundle();
        mBundle.putInt(EXTRA_FRAG_POS, pos);
        mBundle.putFloat(EXTRA_FRAG_SCALE, scale);
        mBundle.putString(EXTRA_FRAG_NAME, name);
        mBundle.putString(EXTRA_FRAG_UUID, uuid);
        mFragment.setArguments(mBundle);
        return mFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.carousel_fragment_item, container, false);

        final int pos = this.getArguments().getInt(EXTRA_FRAG_POS);
        final String mName = this.getArguments().getString(EXTRA_FRAG_NAME);
        final String mUuid = this.getArguments().getString(EXTRA_FRAG_UUID);

        TextView mTv = (TextView) rootView.findViewById(R.id.text);
        mTv.setText(mName);

        if (mName.equalsIgnoreCase(getResources().getString(
                R.string.profile_control_unknown_service))) {
            mService = mBleHashMap.get(mUuid);
            mCurrentUUID = mService.getUuid();

            TextView mTvUUID = (TextView) rootView.findViewById(R.id.text_uuid);
            mTvUUID.setText(Utils.getUuidShort(mCurrentUUID.toString()));
        }

        mCarouselButton = (Button) rootView.findViewById(R.id.content);
        mCarouselButton.setBackgroundResource(pos);
        mCarouselButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Getting the Mapped service from the UUID
                mService = mBleHashMap.get(mUuid);
                mCurrentUUID = mService.getUuid();

                // Heart Rate
                if (mService.getUuid().equals(UUIDDatabase.UUID_HEART_RATE_SERVICE)) {
                    HeartRateService heartRateFragment = HeartRateService.create(mService);
                    displayView(heartRateFragment, getResources().getString(R.string.heart_rate));
                }
                // Device Information
                else if (mService.getUuid().equals(UUIDDatabase.UUID_DEVICE_INFORMATION_SERVICE)) {
                    DeviceInformationService deviceInfoFragment = DeviceInformationService.create(mService);
                    displayView(deviceInfoFragment, getResources().getString(R.string.device_info));
                }
                // Battery
                else if (mService.getUuid().equals(UUIDDatabase.UUID_BATTERY_SERVICE)) {
                    BatteryInformationService batteryInfoFragment = BatteryInformationService.create(mService);
                    displayView(batteryInfoFragment, getResources().getString(R.string.battery_info_fragment));
                }
                // Health Temperature Measurement
                else if (mService.getUuid().equals(UUIDDatabase.UUID_HEALTH_THERMOMETER_SERVICE)) {
                    HealthTemperatureService healthTempFragment = HealthTemperatureService.create(mService);
                    displayView(healthTempFragment, getResources().getString(R.string.health_thermometer_fragment));
                }
                // Find Me
                else if (mService.getUuid().equals(UUIDDatabase.UUID_IMMEDIATE_ALERT_SERVICE)) {
                    FindMeService findMeFragment = FindMeService.create(mService, ServiceDiscoveryFragment.mGattServiceFindMeData, mName);
                    displayView(findMeFragment, getResources().getString(R.string.findme_fragment));
                }
                // Proximity
                else if (mService.getUuid().equals(UUIDDatabase.UUID_LINK_LOSS_SERVICE)
                        || mService.getUuid().equals(UUIDDatabase.UUID_TRANSMISSION_POWER_SERVICE)) {
                    FindMeService findMeFragment = FindMeService.create(mService, ServiceDiscoveryFragment.mGattServiceProximityData, mName);
                    displayView(findMeFragment, getResources().getString(R.string.proximity_fragment));
                }
                // CapSense
                else if (mService.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_SERVICE)
                        || mService.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_SERVICE_CUSTOM)) {
                    List<BluetoothGattCharacteristic> capsenseCharacteristics = mService.getCharacteristics();
                    CapsenseService capsenseFragment = CapsenseService.create(mService, capsenseCharacteristics.size());
                    displayView(capsenseFragment, getResources().getString(R.string.capsense));
                }
                // GattDB
                else if (mService.getUuid().equals(UUIDDatabase.UUID_GENERIC_ATTRIBUTE_SERVICE)
                        || mService.getUuid().equals(UUIDDatabase.UUID_GENERIC_ACCESS_SERVICE)) {
                    GattServicesFragment gattSericesFragment = GattServicesFragment.create();
                    displayView(gattSericesFragment, getResources().getString(R.string.gatt_db));
                }
                // RGB
                else if (mService.getUuid().equals(UUIDDatabase.UUID_RGB_LED_SERVICE)
                        || mService.getUuid().equals(UUIDDatabase.UUID_RGB_LED_SERVICE_CUSTOM)) {
                    RGBFragment rgbfragment = RGBFragment.create(mService);
                    displayView(rgbfragment, getResources().getString(R.string.rgb_led));
                }
                // Glucose Service
                else if (mService.getUuid().equals(UUIDDatabase.UUID_GLUCOSE_SERVICE)) {
                    if (Constants.GMS_ENABLED) {
                        GlucoseService glucoseFragment = GlucoseService.create(mService);
                        displayView(glucoseFragment, getResources().getString(R.string.glucose_fragment));
                    } else {
                        showWarningMessage();
                    }
                }
                // Blood Pressure
                else if (mService.getUuid().equals(UUIDDatabase.UUID_BLOOD_PRESSURE_SERVICE)) {
                    BloodPressureService bloodPressureFragment = BloodPressureService.create(mService);
                    displayView(bloodPressureFragment, getResources().getString(R.string.blood_pressure));
                }
                // Running Speed and Cadence
                else if (mService.getUuid().equals(UUIDDatabase.UUID_RSC_SERVICE)) {
                    RSCService rscFragment = RSCService.create(mService);
                    displayView(rscFragment, getResources().getString(R.string.rsc_fragment));
                }
                // Cycling Speed and Cadence
                else if (mService.getUuid().equals(UUIDDatabase.UUID_CSC_SERVICE)) {
                    CSCService cscFragment = CSCService.create(mService);
                    displayView(cscFragment, getResources().getString(R.string.csc_fragment));
                }
                // Barometer(SensorHub) Service
                else if (mService.getUuid().equals(UUIDDatabase.UUID_BAROMETER_SERVICE)) {
                    SensorHubService sensorHubFragment = SensorHubService.create(mService, ServiceDiscoveryFragment.mGattServiceSensorHubData);
                    displayView(sensorHubFragment, getResources().getString(R.string.sen_hub));
                }
                // HID Remote Control Emulator
                else if (mService.getUuid().equals(UUIDDatabase.UUID_HID_SERVICE)) {
                    String connectedDeviceName = BluetoothLeService.getBluetoothDeviceName();
                    String remoteName = getResources().getString(R.string.rdk_emulator_view);
                    if (connectedDeviceName.indexOf(remoteName) != -1) {
                        if (Constants.RDK_ENABLED) {
                            RemoteControlEmulatorFragment remoteControlEmulatorFragment = RemoteControlEmulatorFragment.create(mService);
                            displayView(remoteControlEmulatorFragment, getResources().getString(R.string.rdk_emulator_view));
                        } else {
                            showWarningMessage();
                        }
                    } else {
                        showWarningMessage();
                    }
                }
                // OTA Firmware Update
                else if (mService.getUuid().equals(UUIDDatabase.UUID_OTA_UPDATE_SERVICE)) {
                    if (Constants.OTA_ENABLED) {
                        OTAFirmwareUpgradeFragment firmwareUpgradeFragment = OTAFirmwareUpgradeFragment.create(mService);
                        displayView(firmwareUpgradeFragment, getResources().getString(R.string.ota_upgrade));
                    } else {
                        showWarningMessage();
                    }
                    // Wearable Solution Demo
                } else if (mService.getUuid().equals(UUIDDatabase.UUID_WEARABLE_DEMO_SERVICE)) {
                    DemoFragment demoFragment = DemoFragment.create();
                    displayView(demoFragment, DemoFragment.TAG);
                } else if (mService.getUuid().equals(UUIDDatabase.UUID_WEARABLE_MOTION_SERVICE)) {
                    MotionFragment motionFragment = new MotionFragment();
                    displayView(motionFragment, MotionFragment.TAG);
                } else if (mService.getUuid().equals(UUIDDatabase.UUID_LOCATION_NAVIGATION_SERVICE)) {
                    LocationFragment locationFragment = LocationFragment.create();
                    displayView(locationFragment, LocationFragment.TAG);
                } else {
                    showWarningMessage();
                }
            }
        });
        float scale = getArguments().getFloat(EXTRA_FRAG_SCALE);
        CarouselLinearLayout root = rootView.findViewById(R.id.root);
        root.setScaleBoth(scale);
        return rootView;
    }

    /**
     * Used for replacing the main content of the view with provided fragments
     *
     * @param fragment
     */
    void displayView(Fragment fragment, String tagName) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.container, fragment, tagName)
                .addToBackStack(null).commit();
    }

    void showWarningMessage() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());
        // set title
        alertDialogBuilder
                .setTitle(R.string.alert_message_unknown_title);
        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.alert_message_unkown)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_message_yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                GattServicesFragment gattServicesFragment = GattServicesFragment.create();
                                displayView(gattServicesFragment, getResources().getString(R.string.gatt_db));
                            }
                        })
                .setNegativeButton(R.string.alert_message_no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
