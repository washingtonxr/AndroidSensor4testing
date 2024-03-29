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

package com.cypress.cysmart.OTAFirmwareUpdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.TextProgressBar;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataModelClasses.PairOnConnect;
import com.cypress.cysmart.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * OTA update fragment
 */
public class OTAFirmwareUpgradeFragment extends Fragment implements View.OnClickListener, OTAFUHandlerCallback {
    //Option Mapping
    public static final int APP_ONLY = 101;
    public static final int APP_AND_STACK_COMBINED = 201;
    public static final int APP_AND_STACK_SEPARATE = 301;
    public static final String REGEX_MATCHES_CYACD2 = "(?i).*\\.cyacd2$";
    public static final String REGEX_ENDS_WITH_CYACD_OR_CYACD2 = "(?i)\\.cyacd2?$";
    public static boolean mFileUpgradeStarted = false;

    private static OTAFUHandler DUMMY_HANDLER = (OTAFUHandler) Proxy.newProxyInstance(OTAFirmwareUpgradeFragment.class.getClassLoader(), new Class<?>[]{OTAFUHandler.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            try {
                new RuntimeException().fillInStackTrace().printStackTrace(pw);
            } finally {
                pw.close();//this will close StringWriter as well
            }
            Logger.e("DUMMY_HANDLER: " + sw);//This is for developer to track the issue
            return null;
        }
    });

    // GATT service and characteristics
    private static BluetoothGattService mOtaService;
    private static BluetoothGattCharacteristic mOtaCharacteristic;

    private OTAFUHandler mOTAFUHandler = DUMMY_HANDLER;//Initializing to DUMMY_HANDLER to avoid NPEs

    private NotificationHandlerBase mNotificationHandler;

    //UI Elements
    private TextView mProgressText;
    private Button mStopUpgradeButton;
    private TextProgressBar mProgressTop;
    private TextProgressBar mProgressBottom;
    private TextView mFileNameTop;
    private TextView mFileNameBottom;
    private Button mAppDownload;
    private Button mAppStackCombinedDownload;
    private Button mAppStackSeparateDownload;
    private RelativeLayout mProgressBarLayoutTop;
    private RelativeLayout mProgressBarLayoutBottom;
    private View mView;
    private ProgressDialog mProgressDialog;

    private BroadcastReceiver mGattOTAStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                processOTAStatus(intent);
            }
        }
    };

    private void processOTAStatus(Intent intent) {
        /**
         * Shared preference to hold the state of the bootloader
         */
        final String bootloaderState = Utils.getStringSharedPreference(getActivity(), Constants.PREF_BOOTLOADER_STATE);
        final String action = intent.getAction();
        Bundle extras = intent.getExtras();
        if (BluetoothLeService.ACTION_OTA_STATUS.equals(action)) {
            mOTAFUHandler.processOTAStatus(bootloaderState, extras);
        } else if (BluetoothLeService.ACTION_OTA_STATUS_V1.equals(action)) {
            mOTAFUHandler.processOTAStatus(bootloaderState, extras);
        } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
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
            } else if (state == BluetoothDevice.BOND_NONE) {
                String dataLog = getResources().getString(R.string.dl_commaseparator)
                        + "[" + BluetoothLeService.getBluetoothDeviceName() + "|"
                        + BluetoothLeService.getBluetoothDeviceAddress() + "]" +
                        getResources().getString(R.string.dl_commaseparator) +
                        getResources().getString(R.string.dl_connection_unpaired);
                Logger.dataLog(dataLog);
            }
        }
    }

    @Override
    public void showErrorDialogMessage(String errorMessage, final boolean stayOnPage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(errorMessage)
                .setTitle(getActivity().getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getResources().getString(
                                R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (OTAFirmwareUpgradeFragment.mOtaCharacteristic != null) {
                                    stopBroadcastDataNotify(OTAFirmwareUpgradeFragment.mOtaCharacteristic);
                                    clearDataAndPreferences();
                                    cancelPendingNotification();
                                    final BluetoothDevice device = BluetoothLeService.getRemoteDevice();
                                    OTAFirmwareUpgradeFragment.mFileUpgradeStarted = false;
                                    if (!stayOnPage) {
                                        BluetoothLeService.disconnect();
                                        BluetoothLeService.unpairDevice(device);
                                        Toast.makeText(getActivity(), getResources().getString(R.string.alert_message_bluetooth_disconnect), Toast.LENGTH_SHORT).show();
                                        Intent intent = getActivity().getIntent();
                                        getActivity().finish();
                                        getActivity().overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                        startActivity(intent);
                                        getActivity().overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                                    } else {
                                        mProgressText.setVisibility(View.INVISIBLE);
                                        mStopUpgradeButton.setVisibility(View.INVISIBLE);
                                        mProgressBarLayoutTop.setVisibility(View.INVISIBLE);
                                        mProgressBarLayoutBottom.setVisibility(View.INVISIBLE);
                                        mAppDownload.setEnabled(true);
                                        mAppDownload.setSelected(false);
                                        mAppStackCombinedDownload.setEnabled(true);
                                        mAppStackCombinedDownload.setSelected(false);
                                        mAppStackSeparateDownload.setEnabled(true);
                                        mAppStackSeparateDownload.setSelected(false);
                                    }
                                }
                            }
                        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        if (!getActivity().isDestroyed())
            alert.show();
    }

    //Factory
    public static OTAFirmwareUpgradeFragment create(BluetoothGattService bluetoothGattService) {
        OTAFirmwareUpgradeFragment.mOtaService = bluetoothGattService;
        return new OTAFirmwareUpgradeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.ota_upgrade_type_selection, container, false);
        initializeGUIElements();
        initializeNotification();
        /**
         * Second file Upgrade
         */
        if (isSecondFileUpdateNeeded()) {
            secondFileUpgrade();
        }
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        BluetoothLeService.registerBroadcastReceiver(getActivity(), mGattOTAStatusReceiver, Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(), R.string.ota_title);
        if (PairOnConnect.isPairOnConnect(getActivity())) { // "Pair on connect" option is set
            initializeBondingIfNotBonded();
        }
    }

    private void initializeBondingIfNotBonded() {
        final BluetoothDevice device = BluetoothLeService.mBluetoothAdapter.getRemoteDevice(BluetoothLeService.getBluetoothDeviceAddress());
        if (!BluetoothLeService.getBondedState()) {
            BluetoothLeService.pairDevice(device);
        }
    }

    @Override
    public void onDestroy() {
        if (mOTAFUHandler != DUMMY_HANDLER) {
            mOTAFUHandler.setPrepareFileWriteEnabled(false);//This is expected case. onDestroy might be invoked before the file to upgrade is selected.
        }
        BluetoothLeService.unregisterBroadcastReceiver(getActivity(), mGattOTAStatusReceiver);
        if (OTAFirmwareUpgradeFragment.mOtaCharacteristic != null) {
            final String sharedPrefStatus = Utils.getStringSharedPreference(getActivity(), Constants.PREF_BOOTLOADER_STATE);
            if (!sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands_v0.EXIT_BOOTLOADER)) {
                cancelPendingNotification();
                clearDataAndPreferences();
            }
            stopBroadcastDataNotify(OTAFirmwareUpgradeFragment.mOtaCharacteristic);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ota_app_download:
                Intent ApplicationUpgrade = new Intent(getActivity(), OTAFilesListingActivity.class);
                ApplicationUpgrade.putExtra(Constants.REQ_FILE_COUNT, APP_ONLY);
                startActivityForResult(ApplicationUpgrade, APP_ONLY);
                break;
            case R.id.ota_app_stack_comb:
                Intent ApplicationAndStackCombined = new Intent(getActivity(), OTAFilesListingActivity.class);
                ApplicationAndStackCombined.putExtra(Constants.REQ_FILE_COUNT, APP_AND_STACK_COMBINED);
                startActivityForResult(ApplicationAndStackCombined, APP_AND_STACK_COMBINED);
                break;
            case R.id.ota_app_stack_seperate:
                Intent ApplicationAndStackSeparate = new Intent(getActivity(), OTAFilesListingActivity.class);
                ApplicationAndStackSeparate.putExtra(Constants.REQ_FILE_COUNT, APP_AND_STACK_SEPARATE);
                startActivityForResult(ApplicationAndStackSeparate, APP_AND_STACK_SEPARATE);
                break;
            case R.id.stop_upgrade_button:
                showOTAStopAlert();
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            ArrayList<String> selectedFiles = data.getStringArrayListExtra(Constants.ARRAYLIST_SELECTED_FILE_NAMES);
            ArrayList<String> selectedFilesPaths = data.getStringArrayListExtra(Constants.ARRAYLIST_SELECTED_FILE_PATHS);
            byte activeApp = data.getByteExtra(Constants.EXTRA_ACTIVE_APP, Constants.ACTIVE_APP_NO_CHANGE);
            long securityKey = data.getLongExtra(Constants.EXTRA_SECURITY_KEY, Constants.NO_SECURITY_KEY);

            if (requestCode == APP_ONLY) {
                //Application upgrade option selected
                String fileOneName = selectedFiles.get(0);
                mFileNameTop.setText(fileOneName.replaceAll(REGEX_ENDS_WITH_CYACD_OR_CYACD2, ""));
                String currentFilePath = selectedFilesPaths.get(0);
                OTAFirmwareUpgradeFragment.mOtaCharacteristic = getGattData();
                mOTAFUHandler = createOTAFUHandler(OTAFirmwareUpgradeFragment.mOtaCharacteristic, activeApp, securityKey, currentFilePath);
                updateGUI(APP_ONLY);
            } else if (requestCode == APP_AND_STACK_COMBINED) {
                //Application and stack upgrade combined option selected
                String fileOneName = selectedFiles.get(0);
                mFileNameTop.setText(fileOneName.replaceAll(REGEX_ENDS_WITH_CYACD_OR_CYACD2, ""));
                String currentFilePath = selectedFilesPaths.get(0);
                OTAFirmwareUpgradeFragment.mOtaCharacteristic = getGattData();
                mOTAFUHandler = createOTAFUHandler(OTAFirmwareUpgradeFragment.mOtaCharacteristic, activeApp, securityKey, currentFilePath);
                updateGUI(APP_AND_STACK_COMBINED);
            } else if (requestCode == APP_AND_STACK_SEPARATE) {
                //Application and stack upgrade separate option selected
                if (selectedFiles.size() == 2) {
                    String fileOneName = selectedFiles.get(0);
                    mFileNameTop.setText(fileOneName.replaceAll(REGEX_ENDS_WITH_CYACD_OR_CYACD2, ""));
                    String fileTwoName = selectedFiles.get(1);
                    mFileNameBottom.setText(fileTwoName.replaceAll(REGEX_ENDS_WITH_CYACD_OR_CYACD2, ""));
                    String currentFilePath = selectedFilesPaths.get(0);
                    OTAFirmwareUpgradeFragment.mOtaCharacteristic = getGattData();
                    mOTAFUHandler = createOTAFUHandler(OTAFirmwareUpgradeFragment.mOtaCharacteristic, activeApp, securityKey, currentFilePath);
                    Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_ONE_NAME, fileOneName);
                    Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_NAME, fileTwoName);
                    Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_PATH, selectedFilesPaths.get(1));
                    Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_ACTIVE_APP_ID, Integer.toHexString(activeApp));
                    Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_SECURITY_KEY, Long.toHexString(securityKey));
                    Logger.e("PREF_OTA_FILE_TWO_PATH-->" + selectedFilesPaths.get(1));
                }
                updateGUI(APP_AND_STACK_SEPARATE);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(getActivity(), getResources().
                    getString(R.string.toast_selection_cancelled), Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private OTAFUHandler createOTAFUHandler(BluetoothGattCharacteristic otaCharacteristic, byte activeApp, long securityKey, String filepath) {
        boolean isCyacd2File = filepath != null && isCyacd2File(filepath);
        Utils.setBooleanSharedPreference(getActivity(), Constants.PREF_IS_CYACD2_FILE, isCyacd2File);

        OTAFUHandler handler = DUMMY_HANDLER;
        if (mNotificationHandler != null && mView != null && otaCharacteristic != null && filepath != null && filepath != "") {
            handler = isCyacd2File
                    ? new OTAFUHandler_v1(this, mNotificationHandler, mView, otaCharacteristic, filepath, this)
                    : new OTAFUHandler_v0(this, mNotificationHandler, mView, otaCharacteristic, activeApp, securityKey, filepath, this);
        }
        return handler;
    }

    private void updateGUI(int updateOption) {
        switch (updateOption) {
            case APP_ONLY:
                /**
                 * Disabling the GUI Option select buttons.
                 * Set the selected position as Application Upgrade
                 */
                mAppDownload.setSelected(true);
                mAppDownload.setPressed(true);
                mAppDownload.setEnabled(false);
                mAppStackCombinedDownload.setEnabled(false);
                mAppStackSeparateDownload.setEnabled(false);
                mProgressText.setVisibility(View.VISIBLE);
                mProgressText.setEnabled(false);
                mStopUpgradeButton.setVisibility(View.VISIBLE);
                mProgressBarLayoutTop.setVisibility(View.VISIBLE);
                mProgressBarLayoutTop.setEnabled(false);
                mProgressBarLayoutBottom.setEnabled(false);
                mProgressText.setText(getActivity().getResources().getText(R.string.ota_file_read));
                mOTAFUHandler.setProgressBarPosition(1);
                try {
                    prepareFileWrite();
                } catch (Exception e) {
                    showErrorDialogMessage(getResources().getString(R.string.ota_alert_invalid_file), true);
                }
                break;
            case APP_AND_STACK_COMBINED:
                /**
                 * Disabling the GUI Option select buttons.
                 * Set the selected position as Application&Stack Upgrade(combined file)
                 */
                mAppStackCombinedDownload.setSelected(true);
                mAppStackCombinedDownload.setPressed(true);
                mAppDownload.setEnabled(false);
                mAppStackCombinedDownload.setEnabled(false);
                mAppStackSeparateDownload.setEnabled(false);
                mProgressText.setVisibility(View.VISIBLE);
                mStopUpgradeButton.setVisibility(View.VISIBLE);
                mProgressBarLayoutTop.setVisibility(View.VISIBLE);
                mProgressBarLayoutBottom.setVisibility(View.INVISIBLE);
                mProgressText.setText(getActivity().getResources().getText(R.string.ota_file_read));
                mOTAFUHandler.setProgressBarPosition(1);
                try {
                    prepareFileWrite();
                } catch (Exception e) {
                    showErrorDialogMessage(getResources().getString(R.string.ota_alert_invalid_file), true);
                }
                break;
            case APP_AND_STACK_SEPARATE:
                /**
                 * Disabling the GUI Option select buttons.
                 * Set the selected position as Application&Stack Upgrade(separate file)
                 */
                mAppStackSeparateDownload.setSelected(true);
                mAppStackSeparateDownload.setPressed(true);
                mAppDownload.setEnabled(false);
                mAppStackCombinedDownload.setEnabled(false);
                mAppStackSeparateDownload.setEnabled(false);
                mProgressText.setVisibility(View.VISIBLE);
                mStopUpgradeButton.setVisibility(View.VISIBLE);
                mProgressBarLayoutTop.setVisibility(View.VISIBLE);
                mProgressBarLayoutBottom.setVisibility(View.VISIBLE);
                mProgressText.setText(getActivity().getResources().getText(R.string.ota_file_read));
                mOTAFUHandler.setProgressBarPosition(1);
                try {
                    prepareFileWrite();
                } catch (Exception e) {
                    showErrorDialogMessage(getResources().getString(R.string.ota_alert_invalid_file), true);
                }
                break;
        }
    }

    private void prepareFileWrite() {
        if (OTAFirmwareUpgradeFragment.mOtaCharacteristic != null) {
            mOTAFUHandler.prepareFileWrite();
        }
    }

    private void initializeGUIElements() {
        LinearLayout parent = (LinearLayout) mView.findViewById(R.id.parent_ota_type);
        Utils.setUpActionBar(getActivity(), getResources().getString(R.string.ota_title));
        setHasOptionsMenu(true);

        mAppDownload = (Button) mView.findViewById(R.id.ota_app_download);
        mAppStackCombinedDownload = (Button) mView.findViewById(R.id.ota_app_stack_comb);
        mAppStackSeparateDownload = (Button) mView.findViewById(R.id.ota_app_stack_seperate);
        mProgressText = (TextView) mView.findViewById(R.id.file_status);
        mProgressTop = (TextProgressBar) mView.findViewById(R.id.upgrade_progress_bar_top);
        mProgressBottom = (TextProgressBar) mView.findViewById(R.id.upgrade_progress_bar_bottom);
        mFileNameTop = (TextView) mView.findViewById(R.id.upgrade_progress_bar_top_filename);
        mFileNameBottom = (TextView) mView.findViewById(R.id.upgrade_progress_bar_bottom_filename);
        mStopUpgradeButton = (Button) mView.findViewById(R.id.stop_upgrade_button);
        mProgressDialog = new ProgressDialog(getActivity());

        mProgressBarLayoutTop = (RelativeLayout) mView.findViewById(R.id.progress_bar_top_rel_lay);
        mProgressBarLayoutBottom = (RelativeLayout) mView.findViewById(R.id.progress_bar_bottom_rel_lay);

        mProgressText.setVisibility(View.INVISIBLE);
        mStopUpgradeButton.setVisibility(View.INVISIBLE);
        mProgressBarLayoutTop.setVisibility(View.INVISIBLE);
        mProgressBarLayoutBottom.setVisibility(View.INVISIBLE);

        mProgressText.setEnabled(false);
        mProgressText.setClickable(false);
        mProgressBarLayoutTop.setEnabled(false);
        mProgressBarLayoutTop.setClickable(false);
        mProgressBarLayoutBottom.setEnabled(false);
        mProgressBarLayoutBottom.setClickable(false);

        parent.setOnClickListener(this);
        /**
         *Application Download
         */
        mAppDownload.setOnClickListener(this);

        /**
         *Application and Stack Combined Option
         */
        mAppStackCombinedDownload.setOnClickListener(this);

        /**
         *Application and Stack Separate Option
         */
        mAppStackSeparateDownload.setOnClickListener(this);

        /**
         *Used to stop on going OTA Update service
         *
         */
        mStopUpgradeButton.setOnClickListener(this);
        setHasOptionsMenu(true);
    }

    private void initializeNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationHandler = new OreoNotificationHandler();
        } else {
            mNotificationHandler = new PreOreoNotificationHandler();
        }
        mNotificationHandler.initializeNotification(getActivity());
    }

    @Override
    public void generatePendingNotification(Context context) {
        mNotificationHandler.generatePendingNotification(context);
    }

    public void cancelPendingNotification() {
        mNotificationHandler.cancelPendingNotification();
    }

    /**
     * Clears all Shared Preference Data & Resets UI
     */
    public void clearDataAndPreferences() {
        //Resetting all preferences on Stop Button
        Logger.e("Data and Prefs cleared>>>>>>>>>");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_ONE_NAME, "Default");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_PATH, "Default");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_NAME, "Default");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_ACTIVE_APP_ID, "Default");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_SECURITY_KEY, "Default");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_BOOTLOADER_STATE, "Default");
        Utils.setIntSharedPreference(getActivity(), Constants.PREF_PROGRAM_ROW_NO, 0);
        Utils.setIntSharedPreference(getActivity(), Constants.PREF_PROGRAM_ROW_START_POS, 0);
        Utils.setIntSharedPreference(getActivity(), Constants.PREF_ARRAY_ID, 0);
    }

    @Override
    public String saveAndReturnDeviceAddress() {
        String deviceAddress = BluetoothLeService.getBluetoothDeviceAddress();
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_DEV_ADDRESS, deviceAddress);
        return Utils.getStringSharedPreference(getActivity(), Constants.PREF_DEV_ADDRESS);
    }

    /**
     * Method to get required characteristics from service
     */
    BluetoothGattCharacteristic getGattData() {
        BluetoothGattCharacteristic characteristic = null;
        List<BluetoothGattCharacteristic> characteristics = OTAFirmwareUpgradeFragment.mOtaService.getCharacteristics();
        for (BluetoothGattCharacteristic c : characteristics) {
            String characteristicUUID = c.getUuid().toString();
            if (characteristicUUID.equalsIgnoreCase(GattAttributes.OTA_CHARACTERISTIC)) {
                characteristic = c;
                prepareBroadcastDataNotify(c);
            }
        }
        return characteristic;
    }

    /**
     * Preparing Broadcast receiver to broadcast notify characteristics
     *
     * @param characteristic
     */
    void prepareBroadcastDataNotify(BluetoothGattCharacteristic characteristic) {
        final int props = characteristic.getProperties();
        if ((props | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BluetoothLeService.setCharacteristicNotification(characteristic, true);
        }
    }

    /**
     * Stopping Broadcast receiver to broadcast notify characteristics
     *
     * @param characteristic
     */
    void stopBroadcastDataNotify(BluetoothGattCharacteristic characteristic) {
        final int props = characteristic.getProperties();
        if ((props | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BluetoothLeService.setCharacteristicNotification(characteristic, false);
        }
    }

    private void showOTAStopAlert() {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getResources().getString(R.string.alert_message_ota_cancel))
                .setTitle(getActivity().getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getResources().getString(R.string.alert_message_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (OTAFirmwareUpgradeFragment.mOtaCharacteristic != null) {
                                    stopBroadcastDataNotify(OTAFirmwareUpgradeFragment.mOtaCharacteristic);
                                    clearDataAndPreferences();
                                    cancelPendingNotification();
                                    final BluetoothDevice device = BluetoothLeService.getRemoteDevice();
                                    OTAFirmwareUpgradeFragment.mFileUpgradeStarted = false;

                                    BluetoothLeService.disconnect();
                                    BluetoothLeService.unpairDevice(device);
                                    Toast.makeText(getActivity(), getResources().getString(R.string.alert_message_bluetooth_disconnect), Toast.LENGTH_SHORT).show();
                                    Intent intent = getActivity().getIntent();
                                    getActivity().finish();
                                    getActivity().overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                    startActivity(intent);
                                    getActivity().overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                                }
                            }
                        })
                .setNegativeButton(getActivity().getResources().getString(R.string.alert_message_no), null);
        alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        if (!getActivity().isDestroyed()) {
            alert.show();
        }
    }

    @Override
    public boolean isSecondFileUpdateNeeded() {
        String secondFilePath = Utils.getStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_PATH);
        Logger.e("secondFilePath-->" + secondFilePath);
        return BluetoothLeService.getBluetoothDeviceAddress().equalsIgnoreCase(saveAndReturnDeviceAddress())
                && (!secondFilePath.equalsIgnoreCase("Default")
                && (!secondFilePath.equalsIgnoreCase("")));
    }

    /**
     * Method to write the second file
     */
    private void secondFileUpgrade() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(
                getActivity().getResources().getString(R.string.alert_message_ota_resume))
                .setTitle(getActivity().getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getResources().getString(R.string.alert_message_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Utils.setStringSharedPreference(getActivity(), Constants.PREF_BOOTLOADER_STATE, null);
                                Utils.setIntSharedPreference(getActivity(), Constants.PREF_PROGRAM_ROW_NO, 0);
                                Utils.setIntSharedPreference(getActivity(), Constants.PREF_PROGRAM_ROW_START_POS, 0);
                                generatePendingNotification(getActivity());

                                OTAFirmwareUpgradeFragment.mOtaCharacteristic = getGattData();
                                //Updating the  file name with progress text
                                if (mProgressTop.getVisibility() != View.VISIBLE) {
                                    mProgressTop.setVisibility(View.VISIBLE);
                                }
                                String fileOneName = Utils.getStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_ONE_NAME);
                                mFileNameTop.setText(fileOneName.replaceAll(REGEX_ENDS_WITH_CYACD_OR_CYACD2, ""));
                                if (mProgressBottom.getVisibility() != View.VISIBLE) {
                                    mProgressBottom.setVisibility(View.VISIBLE);
                                }
                                String fileTwoName = Utils.getStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_NAME);
                                mFileNameBottom.setText(fileTwoName.replaceAll(REGEX_ENDS_WITH_CYACD_OR_CYACD2, ""));
                                mAppStackSeparateDownload.setSelected(true);
                                mAppStackSeparateDownload.setPressed(true);
                                mAppDownload.setEnabled(false);
                                mAppStackCombinedDownload.setEnabled(false);
                                mAppStackSeparateDownload.setEnabled(false);
                                mProgressText.setVisibility(View.VISIBLE);
                                mStopUpgradeButton.setVisibility(View.VISIBLE);
                                mProgressBarLayoutTop.setVisibility(View.VISIBLE);
                                mProgressBarLayoutBottom.setVisibility(View.VISIBLE);
                                mProgressText.setText(getActivity().getResources().getText(R.string.ota_file_read));
                                String currentFilePath = Utils.getStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_PATH);
                                byte activeApp = Constants.ACTIVE_APP_NO_CHANGE;
                                String activeAppString = Utils.getStringSharedPreference(getActivity(), Constants.PREF_OTA_ACTIVE_APP_ID);
                                try {
                                    activeApp = Byte.parseByte(activeAppString, 16);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                                long securityKey = Constants.NO_SECURITY_KEY;
                                String securityKeyString = Utils.getStringSharedPreference(getActivity(), Constants.PREF_OTA_SECURITY_KEY);
                                try {
                                    securityKey = Long.parseLong(securityKeyString, 16);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                                mOTAFUHandler = createOTAFUHandler(OTAFirmwareUpgradeFragment.mOtaCharacteristic, activeApp, securityKey, currentFilePath);
                                mOTAFUHandler.setProgressBarPosition(2);
                                clearDataAndPreferences();
                                prepareFileWrite();
                            }
                        })
                .setNegativeButton(getActivity().getResources().getString(
                        R.string.alert_message_no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                cancelPendingNotification();
                                clearDataAndPreferences();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        if (!getActivity().isDestroyed()) {
            alert.show();
        }
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
    public void setFileUpgradeStarted(boolean status) {
        OTAFirmwareUpgradeFragment.mFileUpgradeStarted = status;
    }

    private boolean isCyacd2File(String file) {
        return file.matches(REGEX_MATCHES_CYACD2);
    }
}
