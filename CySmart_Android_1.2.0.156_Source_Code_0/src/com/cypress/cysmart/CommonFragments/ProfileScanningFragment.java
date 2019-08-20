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

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.HomePageActivity;
import com.cypress.cysmart.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileScanningFragment extends Fragment implements View.OnClickListener {

    //Delay Time out
    private static final long DELAY_MILLIS = 500;

    // Stops scanning after 2 seconds.
    private static final long SCAN_TIMEOUT = 2000;
    private boolean mScanning;
    private Handler mScanTimer = new Handler(Looper.getMainLooper());
    private Runnable mScanTimerTask = new Runnable() {
        @Override
        public void run() {
            BluetoothLeScanner scanner = getScanner();
            if (scanner != null && mBluetoothAdapter.isEnabled()) {
                mScanning = false;
                stopScan();
                mSwipeLayout.setRefreshing(false);
                mRefreshText.setText(getResources().getString(R.string.profile_control_no_device_message));
            }
        }
    };

    // Connection time out after 10 seconds.
    private static final long CONNECTION_TIMEOUT = 10000;
    private Handler mConnectTimer = new Handler(Looper.getMainLooper());
    private Runnable mConnectTimerTask = new Runnable() {
        @Override
        public void run() {
            Logger.e("PSF: connect: connection time out");
            mConnectTimerON = false;
            BluetoothLeService.disconnect();
            dismissProgressDialog();
            if (getActivity() != null) {
                Toast.makeText(getActivity(), R.string.profile_cannot_connect_message, Toast.LENGTH_SHORT).show();
                clearDeviceList();
                scanLeDevice(true);
            }
        }
    };
    private boolean mConnectTimerON;
    private ProgressDialog mProgressDialog;

    // Activity request constant
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_COARSE_LOCATION = 2;
    private static final int REQUEST_PERMISSION_EXTERNAL_STORAGE = 3;

    // device details
    public static String mDeviceName = "name";
    public static String mDeviceAddress = "address";

    //Pair status button and variables
    public static Button mPairButton;

    //Bluetooth adapter
    private static BluetoothAdapter mBluetoothAdapter;

    // Devices list variables
    private static ArrayList<BluetoothDevice> mLeDevices;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private SwipeRefreshLayout mSwipeLayout;
    private Map<String, Integer> mDevRssiValues;

    //GUI elements
    private ListView mProfileListView;
    private TextView mRefreshText;
    private EditText mSearchEditText;

    //  Flags
    public static boolean mIsInFragment = false;

    private boolean mCheckLocationPermission = true;
    private View mLocationDisabledAlertView;
    private Button mLocationEnableButton;
    private Button mLocationMoreButton;
    private boolean mCheckStoragePermission = true;

    /**
     * Call back for BLE Scan
     * This call back is called when a BLE device is found near by.
     */
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                // Should not happen.
                return;
            }
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                return;
            }
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(result.getDevice(), result.getRssi());
                        notifyDeviceListUpdated();
                    }
                });
            }
        }
    };

    /**
     * BroadcastReceiver for receiving the GATT communication status
     */
    private final BroadcastReceiver mGattConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // Status received when connected to GATT Server
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Logger.d("PSF: connect: BluetoothLeService.ACTION_GATT_CONNECTED");
                showConnectionEstablishedInfo();
                if (mScanning) {
                    stopScan();
                    mScanning = false;
                }
                dismissProgressDialog();
                cancelConnectTimer();
                clearDeviceList();
                updateWithNewFragment();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Logger.d("PSF: connect: BluetoothLeService.ACTION_GATT_DISCONNECTED");
                /**
                 * Disconnect event.When the connect timer is ON, reconnect the device
                 * else showToast disconnect message
                 */
                if (mConnectTimerON) {
                    BluetoothLeService.reconnect();
                } else {
                    Toast.makeText(getActivity(), R.string.profile_cannot_connect_message, Toast.LENGTH_SHORT).show();
                }
            } else if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
                Logger.d("PSF: location: LocationManager.PROVIDERS_CHANGED_ACTION");
                checkLocationEnabled();
            }
        }
    };

    /**
     * TextWatcher for filtering the list devices
     */
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mLeDeviceListAdapter.notifyDataSetInvalidated();
            mLeDeviceListAdapter.getFilter().filter(s.toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.d("PSF: lifecycle: onCreateView " + this + ", " + getActivity());
        View rootView = inflater.inflate(R.layout.fragment_profile_scan, container, false);
        mLocationDisabledAlertView = rootView.findViewById(R.id.location_disabled_alert);
        mLocationEnableButton = rootView.findViewById(R.id.location_enable);
        mLocationEnableButton.setOnClickListener(this);
        mLocationMoreButton = rootView.findViewById(R.id.location_more);
        mLocationMoreButton.setOnClickListener(this);
        mDevRssiValues = new HashMap<>();
        mSwipeLayout = rootView.findViewById(R.id.swipe_container);
        mSwipeLayout.setColorScheme(R.color.dark_blue, R.color.medium_blue, R.color.light_blue, R.color.faint_blue);
        mProfileListView = rootView.findViewById(R.id.listView_profiles);
        mRefreshText = rootView.findViewById(R.id.no_dev);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mProfileListView.setAdapter(mLeDeviceListAdapter);
        mProfileListView.setTextFilterEnabled(true);
        setHasOptionsMenu(true);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle(getResources().getString(R.string.alert_message_connect_title));
        mProgressDialog.setCancelable(false);

        checkBleSupportAndInitialize();
        prepareList();

        /**
         * Swipe listener,initiate a new scan on refresh. Stop the swipe refresh
         * after 5 seconds
         */
        mSwipeLayout
                .setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // Clear filter text on refresh
                        mSearchEditText.removeTextChangedListener(mTextWatcher);
                        mSearchEditText.setText("");
                        mSearchEditText.addTextChangedListener(mTextWatcher);

                        if (false == mScanning) {
                            // Prepare list view and initiate scanning
                            clearDeviceList();
                            mCheckLocationPermission = true;
                            mCheckStoragePermission = true;
                            scanLeDevice(true);
                        }
                    }
                });

        /**
         * Creating the dataLogger file and
         * updating the dataLogger history
         */
        Logger.createDataLoggerFile(getActivity());
        mProfileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mLeDeviceListAdapter.getCount() > 0) {
                    final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                    if (device != null) {
                        scanLeDevice(false);
                        connectDevice(device);
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Logger.d("PSF: lifecycle: onStart " + this + ", " + getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.d("PSF: lifecycle: onResume " + this + ", " + getActivity());
        mIsInFragment = true;
        if (checkBluetoothStatus()) {
            prepareList();
        }
        checkLocationEnabled();
        Logger.d("PSF: connect: registering mGattConnectReceiver");
        BluetoothLeService.registerBroadcastReceiver(getActivity(), mGattConnectReceiver, Utils.makeGattUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        Logger.d("PSF: lifecycle: onPause " + this + ", " + getActivity());
        mIsInFragment = false;
        dismissProgressDialog();
        Logger.d("PSF: connect: unregistering mGattConnectReceiver");
        BluetoothLeService.unregisterBroadcastReceiver(getActivity(), mGattConnectReceiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        Logger.d("PSF: lifecycle: onStop " + this + ", " + getActivity());
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d("PSF: lifecycle: onDestroy " + this + ", " + getActivity());
        scanLeDevice(false);
        clearDeviceList();
        mSwipeLayout.setRefreshing(false);

        // Cancel tasks
        mScanTimer.removeCallbacks(mScanTimerTask);
        mConnectTimer.removeCallbacks(mConnectTimerTask);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.device_bluetooth_on),
                        Toast.LENGTH_SHORT).show();
                if (Utils.getBooleanSharedPreference(getActivity(), Constants.PREF_UNPAIR_ON_DISCONNECT)) {
                    boolean unpaired = false;
                    try {
                        unpaired = BluetoothLeService.unpairDevice(BluetoothLeService.getRemoteDevice());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String status = "PSF: pair: unpair status for device " + BluetoothLeService.getBluetoothDeviceAddress() + " after BT OFF-ON cycle: " + unpaired;
                    if (unpaired) {
                        Logger.v(status);
                    } else {
                        Logger.e(status);
                    }
                }
                mLeDeviceListAdapter = new LeDeviceListAdapter();
                mProfileListView.setAdapter(mLeDeviceListAdapter);
                scanLeDevice(true);
            }
            // User chose not to enable Bluetooth.
            else {
                Logger.e("User chose not to enable Bluetooth");
                getActivity().finish();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Logger.d("HPA: permission: ACCESS_COARSE_LOCATION: granted");
                } else {
                    Logger.d("HPA: permission: ACCESS_COARSE_LOCATION: denied");
                    getActivity().finish();
                }
                break;
            }
            case REQUEST_PERMISSION_EXTERNAL_STORAGE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Logger.d("HPA: permission: READ/WRITE_EXTERNAL_STORAGE: granted");
                } else {
                    Logger.d("HPA: permission: READ/WRITE_EXTERNAL_STORAGE: denied");
                    getActivity().finish();
                }
                break;
            }
            default:
                Logger.e("HPA: permission: unknown requestCode: " + requestCode);
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.global, menu);
        mSearchEditText = (EditText) menu.findItem(R.id.search).getActionView();
        mSearchEditText.addTextChangedListener(mTextWatcher);

        MenuItem searchMenuItem = menu.findItem(R.id.search);
        searchMenuItem.setVisible(true);
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when collapsed
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                Logger.e("Collapsed");
                return true; // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Logger.e("Expanded");
                mSearchEditText.requestFocus();
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
                return true; // Return true to expand action view
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.location_enable:
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                break;
            case R.id.location_more:
                final AlertDialog alert[] = {null};
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.alert_message_location_required_title)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                CheckBox checkBox = alert[0].findViewById(R.id.dont_ask_again);
                                if (checkBox.isChecked()) {
                                    Utils.setBooleanSharedPreference(getActivity(), Constants.PREF_LOCATION_REQUIRED_DONT_ASK_AGAIN, true);
                                    mLocationDisabledAlertView.setVisibility(View.GONE);
                                }
                            }
                        })
                        .setView(R.layout.dialog_location_required)
                        .setCancelable(false);
                alert[0] = builder.show();
                break;
            default:
                Logger.e("Unhandled click event");
                break;
        }
    }

    /**
     * Setting up the ActionBar
     */
    private void setUpActionBar() {
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            actionBar.setTitle(R.string.profile_scan_fragment);
        }
    }

    private void updateWithNewFragment() {
        clearDeviceList();
        Utils.replaceFragment(getActivity(), new ServiceDiscoveryFragment(), Constants.SERVICE_DISCOVERY_FRAGMENT_TAG);
    }

    private void checkBleSupportAndInitialize() {
        // Use this check to determine whether BLE is supported on the device.
        if (false == getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.device_ble_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        // Initializes a Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getActivity(), R.string.device_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    /**
     * Method to connect to the device selected. The time allotted for having a
     * connection is 10 seconds. After 10 seconds it will disconnect if not
     * connected and initiate scan once more
     *
     * @param device
     */
    private void connectDevice(BluetoothDevice device) {
        mDeviceAddress = device.getAddress();
        mDeviceName = device.getName();
        connectDevice();
    }

    private void connectDevice() {
        HomePageActivity.mPairingStarted = false;
        HomePageActivity.mAuthenticatedPairing = false;
        // Get the connection status of the device
        if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED) {
            Logger.d("PSF: connectDevice: BluetoothLeService.STATE_DISCONNECTED");
            // Disconnected, so connect
            BluetoothLeService.connect(mDeviceAddress, mDeviceName, getActivity());
            showConnectionInProgressInfo(mDeviceName, mDeviceAddress);
        } else {
            Logger.d("PSF: connectDevice: BLE OTHER STATE: " + BluetoothLeService.getConnectionState());
            // Connecting to some devices, so disconnect and then connect
            BluetoothLeService.disconnect();
            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        BluetoothLeService.connect(mDeviceAddress, mDeviceName, getActivity());
                        showConnectionInProgressInfo(mDeviceName, mDeviceAddress);
                    }
                }
            }, DELAY_MILLIS);
        }
        startConnectTimer();
    }

    private void showConnectionInProgressInfo(String deviceName, String deviceAddress) {
        mProgressDialog.setMessage(getResources().getString(
                R.string.alert_message_connect)
                + "\n"
                + deviceName
                + "\n"
                + deviceAddress
                + "\n"
                + getResources().getString(R.string.alert_message_wait));
        showProgressDialog();
    }

    private void showConnectionEstablishedInfo() {
        mProgressDialog.setMessage(getString(R.string.alert_message_bluetooth_connect));
        showProgressDialog();
    }

    private void showProgressDialog() {
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        mProgressDialog.dismiss();
    }

    /**
     * Method to scan BLE Devices. The status of the scan will be detected in
     * the BluetoothAdapter.LeScanCallback
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {
        BluetoothLeScanner scanner = getScanner();
        if (scanner != null && mBluetoothAdapter.isEnabled()) {
            if (checkStoragePermission() && checkLocationPermission()) {
                if (enable) {
                    if (false == mScanning) {
                        startScanTimer();
                        mScanning = true;
                        mSwipeLayout.setRefreshing(true);
                        startScan();
                        mRefreshText.setText(getResources().getString(R.string.profile_control_device_scanning));
                    }
                } else {
                    cancelScanTimer();
                    mScanning = false;
                    mSwipeLayout.setRefreshing(false);
                    stopScan();
                    mRefreshText.setText(getResources().getString(R.string.profile_control_no_device_message));
                }
            }
        }
    }

    private BluetoothLeScanner getScanner() {
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            Logger.e("PSF: getScanner: cannot get BluetoothLeScanner");
        }
        return scanner;
    }

    private void startScan() {
        BluetoothLeScanner scanner = getScanner();
        if (scanner != null) {
            ScanSettings settings = new ScanSettings.Builder()
//                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Scan using highest duty cycle
                    .build();

            scanner.startScan(null, settings, mLeScanCallback);
        }
    }

    private void stopScan() {
        BluetoothLeScanner scanner = getScanner();
        if (scanner != null) {
            scanner.stopScan(mLeScanCallback);
        }
    }


    /**
     * Swipe refresh timer
     */
    public void startScanTimer() {
        cancelScanTimer();
        mScanTimer.postDelayed(mScanTimerTask, SCAN_TIMEOUT);
    }

    private void cancelScanTimer() {
        mScanTimer.removeCallbacks(mScanTimerTask);
    }

    private void startConnectTimer() {
        cancelConnectTimer();
        mConnectTimer.postDelayed(mConnectTimerTask, CONNECTION_TIMEOUT);
        mConnectTimerON = true;
    }

    private void cancelConnectTimer() {
        mConnectTimer.removeCallbacks(mConnectTimerTask);
        mConnectTimerON = false;
    }

    /**
     * Preparing the BLE device list
     */
    public void prepareList() {
        // Initializes ActionBar as required
        setUpActionBar();
        // Prepare list view and initiate scanning
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mProfileListView.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    private void pairDevice(BluetoothDevice device) {
        boolean success = BluetoothLeService.pairDevice(device);
        if (false == success) {
            dismissProgressDialog();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        boolean success = BluetoothLeService.unpairDevice(device);
        if (false == success) {
            dismissProgressDialog();
        }
    }

    private boolean checkBluetoothStatus() {
        /**
         * Ensures Bluetooth is enabled on the device. If Bluetooth is not
         * currently enabled, fire an intent to display a dialog asking the user
         * to grant permission to enable it.
         */
        if (false == mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
    }

    private boolean checkLocationPermission() {
        // Since Marshmallow either ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission is required for BLE scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Grant permission to CySmart to access Location Service
            if (mCheckLocationPermission
                    && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mCheckLocationPermission = false;
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.alert_message_permission_required_title)
                        .setMessage(R.string.alert_message_location_permission_required_message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (getActivity() != null) {
                                        getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_COARSE_LOCATION);
                                    }
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });
                builder.show();
                return false;
            }
        }
        return true;
    }

    private void checkLocationEnabled() {
        // Since Marshmallow access to Location Service is required for BLE scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean dontAskAgain = Utils.getBooleanSharedPreference(getActivity(), Constants.PREF_LOCATION_REQUIRED_DONT_ASK_AGAIN);
            if (false == dontAskAgain) {
                LocationManager mgr = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                if ((false == mgr.isProviderEnabled(LocationManager.GPS_PROVIDER))
                        && (false == mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
                    mLocationDisabledAlertView.setVisibility(View.VISIBLE);
                } else {
                    mLocationDisabledAlertView.setVisibility(View.GONE);
                }
            }
        }
    }

    private boolean checkStoragePermission() {
        // Since Marshmallow Read/Write access to Storage need to be requested
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Grant permission to CySmart to access External Storage
                if (mCheckStoragePermission
                        && (getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        || getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                    mCheckStoragePermission = false;
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.alert_message_permission_required_title)
                            .setMessage(R.string.alert_message_storage_permission_required_message)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (getActivity() != null) {
                                            getActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_EXTERNAL_STORAGE);
                                        }
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    dialog.cancel();
                                }
                            });
                    builder.show();
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Holder class for the list view view widgets
     */
    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        Button pairStatus;
    }

    private void clearDeviceList() {
        if (mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.clear();
            notifyDeviceListUpdated();
        }
    }

    private void notifyDeviceListUpdated() {
        try {
            mLeDeviceListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * List Adapter for holding devices found through scanning.
     */
    private class LeDeviceListAdapter extends BaseAdapter implements Filterable {

        private LayoutInflater mInflator;
        private int mRssiValue;
        private ItemFilter mFilter = new ItemFilter();

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = getActivity().getLayoutInflater();
        }

        private void addDevice(BluetoothDevice device, int rssi) {
            this.mRssiValue = rssi;
            // New device found
            if (false == mLeDevices.contains(device)) {
                mDevRssiValues.put(device.getAddress(), rssi);
                mLeDevices.add(device);
            } else {
                mDevRssiValues.put(device.getAddress(), rssi);
            }
        }

        public int getRssiValue() {
            return mRssiValue;
        }

        /**
         * Getter method to get the Bluetooth device
         *
         * @param position
         * @return BluetoothDevice
         */
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        /**
         * Clearing all values in the device array list
         */
        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }


        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int position, View view, ViewGroup viewGroup) {
            final ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = view.findViewById(R.id.device_rssi);
                viewHolder.pairStatus = view.findViewById(R.id.btn_pair);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            /**
             * Setting the name and the RSSI of the BluetoothDevice. provided it
             * is a valid one
             */
            final BluetoothDevice device = mLeDevices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                try {
                    viewHolder.deviceName.setText(deviceName);
                    viewHolder.deviceAddress.setText(device.getAddress());
                    byte rssi = (byte) mDevRssiValues.get(device.getAddress()).intValue();
                    if (rssi != 0) {
                        viewHolder.deviceRssi.setText(String.valueOf(rssi));
                    }
                    String pairStatus = (device.getBondState() == BluetoothDevice.BOND_BONDED) ? getActivity().getResources().getString(R.string.bluetooth_pair) : getActivity().getResources().getString(R.string.bluetooth_unpair);
                    viewHolder.pairStatus.setText(pairStatus);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                viewHolder.deviceName.setText(R.string.device_unknown);
                viewHolder.deviceName.setSelected(true);
                viewHolder.deviceAddress.setText(device.getAddress());
            }
            viewHolder.pairStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPairButton = (Button) view;
                    mDeviceAddress = device.getAddress();
                    mDeviceName = device.getName();
                    String status = mPairButton.getText().toString();
                    if (status.equalsIgnoreCase(getResources().getString(R.string.bluetooth_pair))) {
                        unpairDevice(device);
                    } else {
                        pairDevice(device);
                    }
                }
            });
            return view;
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        private class ItemFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                String filterString = constraint.toString().toLowerCase();
                FilterResults results = new FilterResults();
                final ArrayList<BluetoothDevice> list = mLeDevices;

                int count = list.size();
                final ArrayList<BluetoothDevice> newList = new ArrayList<>(count);

                for (int i = 0; i < count; i++) {
                    if (list.get(i).getName() != null && list.get(i).getName().toLowerCase().contains(filterString)) {
                        newList.add(list.get(i));
                    }
                }

                results.values = newList;
                results.count = newList.size();
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                ArrayList<BluetoothDevice> filteredDevices = (ArrayList<BluetoothDevice>) results.values;
                clear();
                int count = filteredDevices.size();
                for (int i = 0; i < count; i++) {
                    BluetoothDevice device = filteredDevices.get(i);
                    mLeDeviceListAdapter.addDevice(device, mLeDeviceListAdapter.getRssiValue());
                }
                notifyDataSetChanged(); // notifies the data with new filtered values
            }
        }
    }
}