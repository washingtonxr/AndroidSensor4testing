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

package com.cypress.cysmart;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

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
import com.cypress.cysmart.CommonFragments.AboutFragment;
import com.cypress.cysmart.CommonFragments.NavigationDrawerFragment;
import com.cypress.cysmart.CommonFragments.ProfileControlFragment;
import com.cypress.cysmart.CommonFragments.ProfileScanningFragment;
import com.cypress.cysmart.CommonFragments.ServiceDiscoveryFragment;
import com.cypress.cysmart.CommonFragments.SettingsFragment;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataLoggerFragments.DataLoggerFragment;
import com.cypress.cysmart.DataModelClasses.PairOnConnect;
import com.cypress.cysmart.GATTDBFragments.GattDescriptorFragment;
import com.cypress.cysmart.GATTDBFragments.GattServicesFragment;
import com.cypress.cysmart.OTAFirmwareUpdate.OTAFirmwareUpgradeFragment;
import com.cypress.cysmart.RDKEmulatorView.RemoteControlEmulatorFragment;
import com.cypress.cysmart.wearable.demo.CategoryListFragment;
import com.cypress.cysmart.wearable.motion.MotionFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Base activity to hold all fragments
 */
public class HomePageActivity extends FragmentActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static final String ROOT_DIR = "/storage/emulated/0/CySmart";
    public static FrameLayout mContainerView;
    public static Boolean mApplicationInBackground = false;

    /**
     * Used to manage connections of the Bluetooth LE Device
     */
    private static BluetoothLeService mBluetoothLeService;
    private static DrawerLayout mParentView;
    private String mAttachmentFileName = "attachment.cyacd";
    private boolean BLUETOOTH_STATUS_FLAG = true;
    private String mPairedString;
    private String mNotPairedString;
    // progress dialog variable
    public ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;
    //Upgrade file catch
    private InputStream mAttachment = null;
    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    public static boolean mPairingStarted;
    public static boolean mAuthenticatedPairing; // Pairing dialog with user input is shown

    /**
     * Broadcast receiver for getting the bonding information
     */
    private BroadcastReceiver mBondStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Received when the bond state is changed
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDING) {
                    Logger.d("HPA: pair: BluetoothDevice.BOND_BONDING");
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_pairing_request);
                    Logger.dataLog(dataLog);
                    Utils.showBondingProgressDialog(HomePageActivity.this, mProgressDialog);
                    // Pairing
                    mPairingStarted = true;
                    // Getting the current active fragment
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
                    if (currentFragment instanceof ServiceDiscoveryFragment) {
                        ServiceDiscoveryFragment sdf = (ServiceDiscoveryFragment) currentFragment;
                        sdf.onBondStateChanged(bondState, previousBondState);
                    }
                } else if (bondState == BluetoothDevice.BOND_BONDED) {
                    Logger.d("HPA: pair: BluetoothDevice.BOND_BONDED");
                    if (ProfileScanningFragment.mPairButton != null) {
                        ProfileScanningFragment.mPairButton.setText(mPairedString);
                        if (previousBondState == BluetoothDevice.BOND_BONDING) {
                            Toast.makeText(HomePageActivity.this, getResources().getString(R.string.toast_paired), Toast.LENGTH_SHORT).show();
                        }
                    }
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.dataLog(dataLog);
                    Utils.hideBondingProgressDialog(mProgressDialog);
                    // Pairing
                    mPairingStarted = false;
                    // Getting the current active fragment
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
                    if (currentFragment instanceof ServiceDiscoveryFragment) {
                        ServiceDiscoveryFragment sdf = (ServiceDiscoveryFragment) currentFragment;
                        sdf.onBondStateChanged(bondState, previousBondState);
                    }
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    Logger.d("HPA: pair: BluetoothDevice.BOND_NONE");
                    if (ProfileScanningFragment.mPairButton != null) {
                        ProfileScanningFragment.mPairButton.setText(mNotPairedString);
                        if (previousBondState == BluetoothDevice.BOND_BONDED) {
                            Toast.makeText(HomePageActivity.this, getResources().getString(R.string.toast_unpaired), Toast.LENGTH_SHORT).show();
                        }
                    }
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_pairing_unsupported);
                    Logger.dataLog(dataLog);
                    Utils.hideBondingProgressDialog(mProgressDialog);
                    // Pairing
                    mPairingStarted = false;
                    // Getting the current active fragment
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
                    if (currentFragment instanceof ServiceDiscoveryFragment) {
                        ServiceDiscoveryFragment sdf = (ServiceDiscoveryFragment) currentFragment;
                        sdf.onBondStateChanged(bondState, previousBondState);
                    }
                } else {
                    Logger.e("HPA: pair: unknown bond state: " + bondState);
                }
            } else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                Logger.d("HPA: pair: BluetoothDevice.ACTION_PAIRING_REQUEST");
                // Pairing
                mAuthenticatedPairing = true; // Pairing Dialog is shown
            } else if (BluetoothLeService.ACTION_PAIRING_CANCEL.equals(action)) {
                Logger.d("HPA: pair: BluetoothDevice.ACTION_PAIRING_CANCEL");
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Logger.d("HPA: BluetoothAdapter.ACTION_STATE_CHANGED");
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    Logger.d("HPA: BluetoothAdapter.STATE_OFF");
                    if (BLUETOOTH_STATUS_FLAG) {
                        connectionLostAlertBox(true);
                    }
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    Logger.d("HPA: BluetoothAdapter.STATE_ON");
                    if (BLUETOOTH_STATUS_FLAG) {
                        connectionLostAlertBox(false);
                    }
                }
            } else if (action.equals(BluetoothLeService.ACTION_GATT_INSUFFICIENT_ENCRYPTION)) {
                Logger.d("HPA: pair: BluetoothLeService.ACTION_GATT_INSUFFICIENT_ENCRYPTION");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d("HPA: lifecycle: onCreate " + this);

        Constants.PACKAGE_NAME = getApplicationContext().getPackageName();
        Logger.d("HPA: package: " + Constants.PACKAGE_NAME);

        if (Utils.isTablet(this)) {
            Logger.d("Tablet");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            Logger.d("Phone");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_main);

        mPairedString = getResources().getString(R.string.bluetooth_pair);
        mNotPairedString = getResources().getString(R.string.bluetooth_unpair);
        mParentView = findViewById(R.id.drawer_layout);
        mContainerView = findViewById(R.id.container);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setMessage(getResources().getString(
                R.string.alert_message_bluetooth_reconnect));
        mAlertDialog.setCancelable(false);
        mAlertDialog.setTitle(getResources().getString(R.string.app_name));
        mAlertDialog.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(
                R.string.alert_message_exit_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intentActivity = getIntent();
                finish();
                overridePendingTransition(
                        R.anim.slide_left, R.anim.push_left);
                startActivity(intentActivity);
                overridePendingTransition(
                        R.anim.slide_right, R.anim.push_right);
            }
        });
        mAlertDialog.setCanceledOnTouchOutside(false);

        // Getting the id of the navigation fragment from the attached xml
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Set the Clear Cache on Disconnect as true by default
        if (!Utils.containsSharedPreference(this, Constants.PREF_CLEAR_CACHE_ON_DISCONNECT)) {
            Utils.setBooleanSharedPreference(this, Constants.PREF_CLEAR_CACHE_ON_DISCONNECT, true);
        }
        // Set the Delete Bond on Disconnect as false by default
        if (!Utils.containsSharedPreference(this, Constants.PREF_UNPAIR_ON_DISCONNECT)) {
            Utils.setBooleanSharedPreference(this, Constants.PREF_UNPAIR_ON_DISCONNECT, Constants.PREF_DEFAULT_UNPAIR_ON_DISCONNECT);
        }
        // Set the Initiate Pairing on Connection as true by default
        if (!PairOnConnect.isPairOnConnectOptionSet(this)) {
            PairOnConnect.setPairOnConnect(this, true);
        }
        // Set the Wait for Pairing Request From the Device as 1 second by default
        if (!PairOnConnect.isWaitForPairingRequestFromPeripheralOptionSet(this)) {
            PairOnConnect.setWaitForPairingRequestFromPeripheralSeconds(this, 1);
        }

        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        startService(gattServiceIntent);
        updateWithNewFragment();
    }

    private void updateWithNewFragment() {
        /**
         * Attaching the profileScanning fragment to start scanning for nearby
         * devices
         */
        Utils.replaceFragment(this, new ProfileScanningFragment(), Constants.PROFILE_SCANNING_FRAGMENT_TAG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (currentFragment instanceof ProfileScanningFragment) {
            ProfileScanningFragment psf = (ProfileScanningFragment) currentFragment;
            psf.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void connectionLostAlertBox(Boolean show) {
        //Disconnected
        if (show) {
            mAlertDialog.show();
        } else {
            if (mAlertDialog != null && mAlertDialog.isShowing())
                mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.d("HPA: lifecycle: onStart " + this);
        Logger.d("HPA: pair: registering mBondStateReceiver");
        BluetoothLeService.registerBroadcastReceiver(this, mBondStateReceiver, Utils.makeGattUpdateIntentFilter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d("HPA: lifecycle: onResume " + this);
        try {
            catchUpgradeFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mApplicationInBackground = false;
        BLUETOOTH_STATUS_FLAG = true;
    }

    @Override
    protected void onPause() {
        Logger.d("HPA: lifecycle: onPause " + this);
        getIntent().setData(null);
        // Getting the current active fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (currentFragment instanceof ProfileScanningFragment || currentFragment instanceof AboutFragment) {
            Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
            stopService(gattServiceIntent);
        }
        mApplicationInBackground = true;
        BLUETOOTH_STATUS_FLAG = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        Logger.d("HPA: lifecycle: onStop " + this);
        Logger.d("HPA: pair: unregistering mBondStateReceiver");
        BluetoothLeService.unregisterBroadcastReceiver(this, mBondStateReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Logger.d("HPA: lifecycle: onDestroy " + this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Logger.e("HPA: lifecycle: onNewIntent " + this);
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /**
     * Handling the back pressed actions
     */
    @Override
    public void onBackPressed() {

        // Getting the current active fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);

        if (mParentView.isDrawerOpen(Gravity.START)) {
            mParentView.closeDrawer(Gravity.START);
        } else {
            if (currentFragment instanceof HeartRateService
                    || currentFragment instanceof HealthTemperatureService
                    || currentFragment instanceof DeviceInformationService
                    || currentFragment instanceof BatteryInformationService
                    || currentFragment instanceof BloodPressureService
                    || currentFragment instanceof CapsenseService
                    || currentFragment instanceof CSCService
                    || currentFragment instanceof FindMeService
                    || currentFragment instanceof GlucoseService
                    || currentFragment instanceof RGBFragment
                    || currentFragment instanceof RSCService
                    || currentFragment instanceof SensorHubService
                    || currentFragment instanceof RemoteControlEmulatorFragment
                    || currentFragment instanceof GattServicesFragment
                    || currentFragment instanceof CategoryListFragment
                    || currentFragment instanceof MotionFragment) {
                Utils.setUpActionBar(this, R.string.profile_control_fragment);
            }

            if (currentFragment instanceof MotionFragment) {
                ((MotionFragment) currentFragment).handleBackPressed();
            } else if (currentFragment instanceof ProfileScanningFragment) {
                alertbox();
            } else if (currentFragment instanceof AboutFragment) {
                if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTED ||
                        BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTING ||
                        BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTING) {
                    BluetoothLeService.disconnect();
                    Toast.makeText(this,
                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                            Toast.LENGTH_SHORT).show();
                }

                // Guiding the user back to profile scanning fragment
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
            } else if (currentFragment instanceof ProfileControlFragment
                    || currentFragment instanceof ServiceDiscoveryFragment) {
                // Guiding the user back to profile scanning fragment
                //  Logger.i("BLE DISCONNECT---->"+BluetoothLeService.getConnectionState());
                if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTED ||
                        BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTING ||
                        BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTING) {

                    BluetoothLeService.disconnect();
                    Toast.makeText(this,
                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                super.onBackPressed();
            } else if (currentFragment instanceof GattDescriptorFragment) {
                CySmartApplication application = (CySmartApplication) getApplication();
                BluetoothGattDescriptor descriptor = application.getBluetoothGattDescriptor();
                if (descriptor != null) {
                    BluetoothLeService.readDescriptor(descriptor);
                }
                super.onBackPressed();
            } else if (currentFragment instanceof OTAFirmwareUpgradeFragment) {
                if (OTAFirmwareUpgradeFragment.mFileUpgradeStarted) {
                    AlertDialog alert;
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(
                            this.getResources().getString(
                                    R.string.alert_message_ota_pending))
                            .setTitle(this.getResources().getString(R.string.app_name))
                            .setCancelable(false)
                            .setPositiveButton(
                                    this.getResources().getString(
                                            R.string.alert_message_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTED ||
                                                    BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTING ||
                                                    BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTING) {
                                                final BluetoothDevice device = BluetoothLeService.getRemoteDevice();
                                                OTAFirmwareUpgradeFragment.mFileUpgradeStarted = false;
                                                BluetoothLeService.unpairDevice(device);
                                                BluetoothLeService.disconnect();
                                                Toast.makeText(HomePageActivity.this,
                                                        getResources().getString(R.string.alert_message_bluetooth_disconnect),
                                                        Toast.LENGTH_SHORT).show();
                                                Intent intent = getIntent();
                                                finish();
                                                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                                startActivity(intent);
                                                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                                            }
                                        }
                                    })
                            .setNegativeButton(this.getResources().getString(
                                    R.string.alert_message_no), null);
                    alert = builder.create();
                    alert.setCanceledOnTouchOutside(true);
                    if (!this.isDestroyed()) {
                        alert.show();
                    }
                } else {
                    Utils.setUpActionBar(this, R.string.profile_control_fragment);
                    super.onBackPressed();
                }
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Logger.e("onNavigationDrawerItemSelected " + position);
        /**
         * Update the main content by replacing fragments with user selected
         * option
         */
        switch (position) {
            case NavigationDrawerFragment.ItemPosition.BLE:
                /**
                 * BLE Devices
                 */
                if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTED ||
                        BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTING ||
                        BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTING) {
                    BluetoothLeService.disconnect();
                }
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                break;
            case NavigationDrawerFragment.ItemPosition.ABOUT:
                /**
                 * About
                 */
                Utils.replaceFragment(this, new AboutFragment(), Constants.ABOUT_FRAGMENT_TAG);
                break;
            case NavigationDrawerFragment.ItemPosition.SETTINGS:
                /**
                 * Settings
                 */
                FragmentManager fragmentManager = getSupportFragmentManager();
                // User might select "Settings" menu item multiple times
                // Handle only the very first selection
                if (fragmentManager.findFragmentByTag(Constants.FRAGMENT_TAG_SETTINGS) == null) {
                    fragmentManager
                            .beginTransaction()
                            .add(R.id.container, new SettingsFragment(), Constants.FRAGMENT_TAG_SETTINGS)
                            .addToBackStack(null)
                            .commit();
                }
                break;
            default:
                break;
        }

    }

    // Get intent, action and MIME type
    private void catchUpgradeFile() throws IOException, NullPointerException {
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        File rootDir = new File(ROOT_DIR);

        if (Intent.ACTION_VIEW.equalsIgnoreCase(action) && data != null) {
            if (intent.getScheme().compareTo("content") == 0) {
                try {
                    Cursor c = getContentResolver().query(
                            intent.getData(), null, null, null, null);
                    c.moveToFirst();
                    final int fileNameColumnId = c.getColumnIndex(
                            MediaStore.MediaColumns.DISPLAY_NAME);
                    if (fileNameColumnId >= 0)
                        mAttachmentFileName = c.getString(fileNameColumnId);
                    Logger.e("Filename>>>" + mAttachmentFileName);
                    // Fetch the attachment
                    mAttachment = getContentResolver().openInputStream(data);
                    if (mAttachment == null) {
                        Logger.e("onCreate" + "cannot access mail attachment");
                    } else {
                        if (fileExists(mAttachmentFileName, rootDir)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(HomePageActivity.this);
                            builder.setMessage(getResources().getString(R.string.alert_message_file_copy))
                                    .setCancelable(false)
                                    .setTitle(getResources().getString(R.string.app_name))
                                    .setPositiveButton(
                                            getResources()
                                                    .getString(R.string.alert_message_yes),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    try {
                                                        FileOutputStream tmp = new FileOutputStream(ROOT_DIR + File.separator + mAttachmentFileName);
                                                        byte[] buffer = new byte[1024];
                                                        int bytes = 0;
                                                        while ((bytes = mAttachment.read(buffer)) > 0)
                                                            tmp.write(buffer, 0, bytes);
                                                        tmp.close();
                                                        mAttachment.close();
                                                        getIntent().setData(null);
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            })
                                    .setNegativeButton(
                                            getResources().getString(
                                                    R.string.alert_message_no),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    // Cancel the dialog box
                                                    dialog.cancel();
                                                    getIntent().setData(null);
                                                }
                                            });
                            AlertDialog alert = builder.create();
                            alert.show();
                        } else {
                            try {
                                FileOutputStream tmp = new FileOutputStream(ROOT_DIR + File.separator + mAttachmentFileName);
                                byte[] buffer = new byte[1024];
                                int bytes = 0;
                                while ((bytes = mAttachment.read(buffer)) > 0)
                                    tmp.write(buffer, 0, bytes);
                                tmp.close();
                                mAttachment.close();
                                Toast.makeText(this, getResources().getString(R.string.toast_file_copied), Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                String sourcePath = data.getPath();
                Logger.e("Action>>>" + action + "Uri" + data.toString() + "Source path>>" + sourcePath);

                final File sourceLocation = new File(sourcePath);
                String sourceFileName = sourceLocation.getName();

                final File targetLocation = new File(ROOT_DIR + File.separator + sourceFileName);

                if (fileExists(sourceFileName, rootDir)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            HomePageActivity.this);
                    builder.setMessage(getResources().getString(R.string.alert_message_file_copy))
                            .setCancelable(false)
                            .setTitle(getResources().getString(R.string.app_name))
                            .setPositiveButton(
                                    getResources()
                                            .getString(R.string.alert_message_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            try {
                                                copyDirectory(sourceLocation, targetLocation);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                            .setNegativeButton(
                                    getResources().getString(
                                            R.string.alert_message_no),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // Cancel the dialog box
                                            dialog.cancel();
                                            getIntent().setData(null);
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    try {
                        copyDirectory(sourceLocation, targetLocation);
                        Toast.makeText(this, getResources().getString(R.string.toast_file_copied), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /*
     * Checks whether a file exists in the folder specified
     */
    public boolean fileExists(String name, File file) {
        File[] list = file.listFiles();
        if (list != null) { // Might be null on Android M and above when not granted Storage permission
            for (File fil : list) {
                if (fil.isDirectory()) {
                    fileExists(name, fil);
                } else if (name.equalsIgnoreCase(fil.getName())) {
                    Logger.e("File>>" + fil.getName());
                    return true;
                }
            }
        }
        return false;
    }

    // If targetLocation does not exist, it will be created.
    public void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            InputStream in = new FileInputStream(sourceLocation.getAbsolutePath());
            OutputStream out = new FileOutputStream(targetLocation.getAbsolutePath());
            // Copy bits from in-stream to out-stream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            getIntent().setData(null);
        }
    }

    /**
     * Method to create an alert before user exit from the application
     */
    void alertbox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                HomePageActivity.this);
        builder.setMessage(
                getResources().getString(R.string.alert_message_exit))
                .setCancelable(false)
                .setTitle(getResources().getString(R.string.app_name))
                .setPositiveButton(
                        getResources()
                                .getString(R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Finish the current activity
                                HomePageActivity.this.finish();
                                Intent gattServiceIntent = new Intent(getApplicationContext(),
                                        BluetoothLeService.class);
                                stopService(gattServiceIntent);

                            }
                        })
                .setNegativeButton(
                        getResources().getString(
                                R.string.alert_message_exit_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.global, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        switch (item.getItemId()) {
            case R.id.share:
                // Share
                HomePageActivity.mContainerView.invalidate();
                View rootView = getWindow().getDecorView().getRootView();

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("image/jpg");

                final String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "CySmart" + File.separator + "file.jpg";
                final File shareFile = new File(filepath);
                if (shareFile.exists()) {
                    shareFile.delete();
                }

                try {
                    shareFile.createNewFile();
                    Utils.takeScreenshotAndSaveToFile(rootView, shareFile);
                    Logger.i("temporaryPath>" + filepath);

                    Uri contentUri = FileProvider.getUriForFile(this, getResources().getString(R.string.authority_fileprovider), shareFile);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.clearcache:
                showWarningInfo();
                return true;
            case R.id.log:
                // DataLogger
                File file = new File(Environment.getExternalStorageDirectory()
                        + File.separator + "CySmart" + File.separator
                        + Utils.GetDate() + ".txt");
                String path = file.getAbsolutePath();
                Bundle bundle = new Bundle();
                bundle.putString(Constants.DATA_LOGGER_FILE_NAME, path);
                bundle.putBoolean(Constants.DATA_LOGGER_FLAG, false);
                /**
                 * Adding new fragment DataLoggerFragment to the view
                 */
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
                DataLoggerFragment dataloggerfragment = new DataLoggerFragment()
                        .create(currentFragment.getTag());
                dataloggerfragment.setArguments(bundle);
                fragmentManager.beginTransaction()
                        .add(R.id.container, dataloggerfragment)
                        .addToBackStack(null).commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showWarningInfo() {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.alert_message_clear_cache))
                .setTitle(getString(R.string.alert_title_clear_cache))
                .setCancelable(false)
                .setPositiveButton(getString(
                        R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                if (BluetoothLeService.mBluetoothGatt != null) {
                                    BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
                                }
                                BluetoothLeService.disconnect();
                                Toast.makeText(getBaseContext(),
                                        getString(R.string.alert_message_bluetooth_disconnect),
                                        Toast.LENGTH_SHORT).show();
                                Intent homePage = getIntent();
                                finish();
                                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                startActivity(homePage);
                                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                            }
                        })
                .setNegativeButton(getString(
                        R.string.alert_message_exit_cancel), null);
        alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
}
