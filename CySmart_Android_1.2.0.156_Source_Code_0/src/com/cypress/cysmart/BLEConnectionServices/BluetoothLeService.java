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

package com.cypress.cysmart.BLEConnectionServices;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.cypress.cysmart.BLEProfileDataParserClasses.BloodPressureParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.CSCParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.CapSenseParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.DescriptorParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.GlucoseParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.HRMParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.HTMParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.RGBParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.RSCParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.SensorHubParser;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    /**
     * GATT Status constants
     */
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_DISCONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTING";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_OTA_DATA_AVAILABLE =
            "com.cysmart.bluetooth.le.ACTION_OTA_DATA_AVAILABLE";
    public final static String ACTION_OTA_DATA_AVAILABLE_V1 =
            "com.cysmart.bluetooth.le.ACTION_OTA_DATA_AVAILABLE_V1";
    public final static String ACTION_GATT_CHARACTERISTIC_ERROR =
            "com.example.bluetooth.le.ACTION_GATT_CHARACTERISTIC_ERROR";
    public final static String ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL =
            "com.example.bluetooth.le.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL";
    public final static String ACTION_WRITE_COMPLETED =
            "android.bluetooth.device.action.ACTION_WRITE_COMPLETED";
    public final static String ACTION_WRITE_FAILED =
            "android.bluetooth.device.action.ACTION_WRITE_FAILED";
    public final static String ACTION_WRITE_SUCCESS =
            "android.bluetooth.device.action.ACTION_WRITE_SUCCESS";
    public final static String ACTION_GATT_INSUFFICIENT_ENCRYPTION =
            "com.example.bluetooth.le.ACTION_GATT_INSUFFICIENT_ENCRYPTION";
    public static final String ACTION_PAIRING_CANCEL =
            "android.bluetooth.device.action.PAIRING_CANCEL";

    public final static String ACTION_OTA_STATUS = "com.example.bluetooth.le.ACTION_OTA_STATUS";
    public final static String ACTION_OTA_STATUS_V1 = "com.example.bluetooth.le.ACTION_OTA_STATUS_V1";

    public static final int GATT_CONNECTION_TIMEOUT = 8;

    /**
     * Connection status constants
     */
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 4;

    public static final boolean MTU_USE_NEGOTIATED = true;//Use negotiated MTU vs MTU_DEFAULT(20)
    public static final int MTU_DEFAULT = 20;//MIN_MTU(23) - 3
    public static final int MTU_NUM_BYTES_TO_SUBTRACT = 3;//3 bytes need to be subtracted
    public static Semaphore writeSemaphore = new Semaphore(1);

    /**
     * BluetoothAdapter for handling connections
     */
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothGatt mBluetoothGatt;
    /**
     * Disable/enable notification
     */
    public static ArrayList<BluetoothGattCharacteristic> mEnabledCharacteristics = new ArrayList<>();
    public static ArrayList<BluetoothGattCharacteristic> mRDKCharacteristics = new ArrayList<>();
    public static ArrayList<BluetoothGattCharacteristic> mGlucoseCharacteristics = new ArrayList<>();
    public static ArrayList<BluetoothGattCharacteristic> mWearableDemoCharacteristicsToEnable = new ArrayList<>();
    public static ArrayList<BluetoothGattCharacteristic> mWearableDemoCharacteristicsToDisable = new ArrayList<>();

    public static boolean mDisableNotificationFlag = false;
    public static boolean mEnableRDKNotificationFlag = false;
    public static boolean mEnableGlucoseFlag = false;
    public static boolean mEnableWearableDemoCharacteristicsFlag = false;
    public static boolean mDisableWearableDemoCharacteristicsFlag = false;

    private static int mConnectionState = STATE_DISCONNECTED;
    private static boolean mOtaExitBootloaderCmdInProgress = false;

    /**
     * Device address
     */
    private static String mBluetoothDeviceAddress;
    private static String mBluetoothDeviceName;
    private static Context mContext;

    /**
     * Implements callback methods for GATT events that the app cares about. For
     * example,connection change and services discovered.
     */
    private final static BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Logger.i("onConnectionStateChange: status: " + status + ", newState: " + newState);
            String intentAction;
            // GATT Server connected
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTED;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_established);
                Logger.dataLog(dataLog);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_DISCONNECTED;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_disconnected);
                Logger.dataLog(dataLog);
            }
            // GATT Server Connecting
            else if (newState == BluetoothProfile.STATE_CONNECTING) {
                intentAction = ACTION_GATT_CONNECTING;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTING;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_establishing);
                Logger.dataLog(dataLog);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                intentAction = ACTION_GATT_DISCONNECTING;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_DISCONNECTING;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_disconnecting);
                Logger.dataLog(dataLog);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // GATT Services discovered
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_service_discovery_status) +
                        mContext.getResources().getString(R.string.dl_status_success);
                Logger.dataLog(dataLog);
                broadcastConnectionUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_service_discovery_status) +
                        mContext.getResources().getString(R.string.dl_status_failure) + status;
                Logger.dataLog(dataLog);
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION ||
                        status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
//                    pairDevice();
                }
                broadcastConnectionUpdate(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getService().getUuid(), serviceUUID);

            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getUuid(), characteristicUUID);

            String descriptorUUID = descriptor.getUuid().toString();
            String descriptorName = GattAttributes.lookupUUID(descriptor.getUuid(), descriptorUUID);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[00]";
                Logger.dataLog(dataLog);
                if (descriptor.getValue() != null) {
                    addRemoveData(descriptor);
                }

                if (mDisableNotificationFlag) {
                    disableAllEnabledCharacteristics();
                } else if (mDisableWearableDemoCharacteristicsFlag) {
                    disableWearableDemoCharacteristics();
                } else if (mEnableRDKNotificationFlag) {
                    if (mRDKCharacteristics.size() > 0) {
                        mRDKCharacteristics.remove(0);
                        enableAllRDKCharacteristics();
                    }
                } else if (mEnableGlucoseFlag) {
                    if (mGlucoseCharacteristics.size() > 0) {
                        mGlucoseCharacteristics.remove(0);
                        enableAllGlucoseCharacteristics();
                    }
                } else if (mEnableWearableDemoCharacteristicsFlag) {
                    if (mWearableDemoCharacteristicsToEnable.size() > 0) {
                        mWearableDemoCharacteristicsToEnable.remove(0);
                        enableWearableDemoCharacteristics();
                    }
                }
                Intent intent = new Intent(ACTION_WRITE_SUCCESS);
                sendExplicitBroadcastIntent(mContext, intent);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_status_failure) +
                        +status;
                Logger.dataLog(dataLog);
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
//                    pairDevice(); // TODO: Android automatically pairs in this case
                    sendExplicitBroadcastIntent(mContext, new Intent(ACTION_GATT_INSUFFICIENT_ENCRYPTION));
                } else {
                    mDisableNotificationFlag = false;
                    mEnableRDKNotificationFlag = false;
                    mEnableGlucoseFlag = false;
                    sendExplicitBroadcastIntent(mContext, new Intent(ACTION_WRITE_FAILED));
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getService().getUuid(), serviceUUID);

            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getUuid(), characteristicUUID);

            String descriptorUUIDText = descriptor.getUuid().toString();
            String descriptorName = GattAttributes.lookupUUID(descriptor.getUuid(), descriptorUUIDText);

            String descriptorValue = " " + Utils.byteArrayToHex(descriptor.getValue()) + " ";

            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_response) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + descriptorValue + "]";
                Logger.dataLog(dataLog);

                UUID descriptorUUID = descriptor.getUuid();
                final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                Bundle bundle = new Bundle();

                // Putting the byte value read for GATT DB
                bundle.putByteArray(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE, descriptor.getValue());
                bundle.putInt(Constants.EXTRA_BYTE_DESCRIPTOR_INSTANCE_VALUE, descriptor.getCharacteristic().getInstanceId());
                bundle.putString(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_UUID, descriptor.getUuid().toString());
                bundle.putString(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_CHARACTERISTIC_UUID, descriptor.getCharacteristic().getUuid().toString());

                if (descriptorUUID.equals(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG)) {

                    String valueReceived = DescriptorParser.getClientCharacteristicConfiguration(descriptor, mContext);
                    bundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, valueReceived);

                } else if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_EXTENDED_PROPERTIES)) {

                    HashMap<String, String> receivedValuesMap = DescriptorParser.getCharacteristicExtendedProperties(descriptor, mContext);
                    String reliableWriteStatus = receivedValuesMap.get(Constants.FIRST_BIT_KEY_VALUE);
                    String writeAuxiliaryStatus = receivedValuesMap.get(Constants.SECOND_BIT_KEY_VALUE);
                    bundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, reliableWriteStatus + "\n" + writeAuxiliaryStatus);

                } else if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_USER_DESCRIPTION)) {

                    String description = DescriptorParser.getCharacteristicUserDescription(descriptor);
                    bundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, description);

                } else if (descriptorUUID.equals(UUIDDatabase.UUID_SERVER_CHARACTERISTIC_CONFIGURATION)) {

                    String broadcastStatus = DescriptorParser.getServerCharacteristicConfiguration(descriptor, mContext);
                    bundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, broadcastStatus);

                } else if (descriptorUUID.equals(UUIDDatabase.UUID_REPORT_REFERENCE)) {

                    ArrayList<String> reportReferenceValues = DescriptorParser.getReportReference(descriptor);
                    String reportReference;
                    String reportReferenceType;
                    if (reportReferenceValues.size() == 2) {
                        reportReference = reportReferenceValues.get(0);
                        reportReferenceType = reportReferenceValues.get(1);
                        bundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID, reportReference);
                        bundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_TYPE, reportReferenceType);
                        bundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, reportReference + "\n" + reportReferenceType);
                    }

                } else if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_PRESENTATION_FORMAT)) {

                    String value = DescriptorParser.getCharacteristicPresentationFormat(descriptor, mContext);
                    bundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, value);

                }
                intent.putExtras(bundle);
                /**
                 * Sending the broadcast so that it can be received by
                 * registered receivers
                 */
                sendExplicitBroadcastIntent(mContext, intent);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_request_status) +
                        mContext.getResources().getString(R.string.dl_status_failure) + status;
                Logger.dataLog(dataLog);

                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                    sendExplicitBroadcastIntent(mContext, new Intent(ACTION_GATT_INSUFFICIENT_ENCRYPTION));
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String dataLog = "";
            if (status == BluetoothGatt.GATT_SUCCESS) {
                dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_status_success);
                Logger.dataLog(dataLog);
            } else {
                dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status) +
                        mContext.getResources().getString(R.string.dl_status_failure) + status;
                Logger.dataLog(dataLog);

                Intent intent = new Intent(ACTION_GATT_CHARACTERISTIC_ERROR);
                intent.putExtra(Constants.EXTRA_CHARACTERISTIC_ERROR_MESSAGE, "" + status);
                sendExplicitBroadcastIntent(mContext, intent);

                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                    sendExplicitBroadcastIntent(mContext, new Intent(ACTION_GATT_INSUFFICIENT_ENCRYPTION));
                }
            }

            Logger.d("CYSMART", dataLog);
            boolean isExitBootloaderCmd = false;
            synchronized (mGattCallback) {
                isExitBootloaderCmd = mOtaExitBootloaderCmdInProgress;
                if (mOtaExitBootloaderCmdInProgress) {
                    mOtaExitBootloaderCmdInProgress = false;
                }
            }
            if (isExitBootloaderCmd) {
                onOtaExitBootloaderComplete(status);
            }

            if (characteristic.getUuid().toString().equalsIgnoreCase(GattAttributes.OTA_CHARACTERISTIC)) {
                Logger.v("Release semaphore");
                writeSemaphore.release();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String characteristicValue = " " + Utils.byteArrayToHex(characteristic.getValue()) + " ";

            // GATT Characteristic read
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_response) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + characteristicValue + "]";
                Logger.dataLog(dataLog);

                broadcastNotifyUpdate(characteristic);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_request_status) +
                        mContext.getResources().getString(R.string.dl_status_failure) + status;
                Logger.dataLog(dataLog);

                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
//                    pairDevice();
                    sendExplicitBroadcastIntent(mContext, new Intent(ACTION_GATT_INSUFFICIENT_ENCRYPTION));
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String characteristicValue = Utils.byteArrayToHex(characteristic.getValue());

            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_notification_response) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.dataLog(dataLog);

            broadcastNotifyUpdate(characteristic);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            String dataLog = Utils.formatForRootLocale(
                    mContext.getResources().getString(R.string.exchange_mtu_rsp),
                    mBluetoothDeviceName,
                    mBluetoothDeviceAddress,
                    mContext.getResources().getString(R.string.exchange_mtu),
                    mtu,
                    status);
            Logger.dataLog(dataLog);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Utils.setIntSharedPreference(mContext, Constants.PREF_MTU_NEGOTIATED, mtu);
            }
        }
    };

    // Android 8 (Oreo) bans implicit broadcasts (where the intent doesn't specify the receiver's package and/or Java class)
    // FIX: use explicit broadcasts
    public static void sendExplicitBroadcastIntent(Context context, Intent intent) {
        // Make intent explicit by specifying the package name
        intent.setPackage(Constants.PACKAGE_NAME);
        context.sendBroadcast(intent);
        // Broadcasts are not being received by the receivers registered in the AndroidManifest.xml
//        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void registerBroadcastReceiver(Context context, BroadcastReceiver receiver, IntentFilter filter) {
        context.registerReceiver(receiver, filter);
//        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
    }

    public static void unregisterBroadcastReceiver(Context context, BroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
//        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    public static void exchangeGattMtu(int mtu) {
        int retry = 5;
        boolean status = false;
        while ((false == status) && retry > 0) {
            status = mBluetoothGatt.requestMtu(mtu);
            retry--;
        }

        Resources res = mContext.getResources();
        String dataLog = Utils.formatForRootLocale(
                res.getString(R.string.exchange_mtu_request),
                mBluetoothDeviceName,
                mBluetoothDeviceAddress,
                res.getString(R.string.exchange_mtu),
                mtu,
                status ? 0x00 : 0x01);

        Logger.dataLog(dataLog);
    }

    private final IBinder mBinder = new LocalBinder();
    /**
     * Flag to check the mBound status
     */
    public boolean mBound;
    /**
     * BlueTooth manager for handling connections
     */
    private BluetoothManager mBluetoothManager;

    public static String getBluetoothDeviceAddress() {
        return mBluetoothDeviceAddress;
    }

    public static String getBluetoothDeviceName() {
        return mBluetoothDeviceName;
    }

    private static void broadcastConnectionUpdate(String action) {
        Logger.i("BluetoothLeService: action: " + action);
        Intent intent = new Intent(action);
        sendExplicitBroadcastIntent(mContext, intent);
    }

    private static void broadcastWriteStatusUpdate(final String action) {
        final Intent intent = new Intent((action));
        sendExplicitBroadcastIntent(mContext, intent);
    }

    private static void broadcastNotifyUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle bundle = new Bundle();
        // Putting the byte value read for GATT Db
        bundle.putByteArray(Constants.EXTRA_BYTE_VALUE,
                characteristic.getValue());
        bundle.putString(Constants.EXTRA_BYTE_UUID_VALUE,
                characteristic.getUuid().toString());
        bundle.putInt(Constants.EXTRA_BYTE_INSTANCE_VALUE,
                characteristic.getInstanceId());
        bundle.putString(Constants.EXTRA_BYTE_SERVICE_UUID_VALUE,
                characteristic.getService().getUuid().toString());
        bundle.putInt(Constants.EXTRA_BYTE_SERVICE_INSTANCE_VALUE,
                characteristic.getService().getInstanceId());
        // Heart rate Measurement notify value
        if (characteristic.getUuid().equals(UUIDDatabase.UUID_HEART_RATE_MEASUREMENT)) {
            if (HRMParser.isValidValue(characteristic)) {
                String heartRate = HRMParser.getHeartRate(characteristic);
                String sensorContact = HRMParser.getSensorContactStatus(characteristic);
                String energyExpended = HRMParser.getEnergyExpended(characteristic);
                ArrayList<Integer> rrInterval = HRMParser.getRRInterval(characteristic);
                bundle.putString(Constants.EXTRA_HRM_HEART_RATE_VALUE, heartRate);
                bundle.putString(Constants.EXTRA_HRM_SENSOR_CONTACT_VALUE, sensorContact);
                bundle.putString(Constants.EXTRA_HRM_ENERGY_EXPENDED_VALUE, energyExpended);
                bundle.putIntegerArrayList(Constants.EXTRA_HRM_RR_INTERVAL_VALUE, rrInterval);
            }
        }
        // Health thermometer notify value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_TEMPERATURE_MEASUREMENT)) {
            ArrayList<String> htmData = HTMParser.parseTemperatureMeasurement(characteristic, mContext);
            bundle.putStringArrayList(Constants.EXTRA_HTM_TEMPERATURE_MEASUREMENT_VALUE, htmData);
        }
        // Blood pressure measurement notify value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_BLOOD_PRESSURE_MEASUREMENT)) {
            String bloodPressureSystolic = BloodPressureParser
                    .getSystolicBloodPressure(characteristic);
            String bloodPressureDiastolic = BloodPressureParser
                    .getDiastolicBloodPressure(characteristic);
            String bloodPressureSystolicUnit = BloodPressureParser
                    .getSystolicBloodPressureUnit(characteristic, mContext);
            String bloodPressureDiastolicUnit = BloodPressureParser
                    .getDiaStolicBloodPressureUnit(characteristic, mContext);
            bundle.putString(
                    Constants.EXTRA_PRESURE_SYSTOLIC_UNIT_VALUE,
                    bloodPressureSystolicUnit);
            bundle.putString(
                    Constants.EXTRA_PRESURE_DIASTOLIC_UNIT_VALUE,
                    bloodPressureDiastolicUnit);
            bundle.putString(
                    Constants.EXTRA_PRESURE_SYSTOLIC_VALUE,
                    bloodPressureSystolic);
            bundle.putString(
                    Constants.EXTRA_PRESURE_DIASTOLIC_VALUE,
                    bloodPressureDiastolic);
        }
        // Cycling speed Measurement notify value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_CSC_MEASURE)) {
            ArrayList<String> cscValues = CSCParser.getCyclingSpeedCadence(characteristic);
            bundle.putStringArrayList(Constants.EXTRA_CSC_VALUE, cscValues);
        }
        //RDK characteristic
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_REPORT)) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor
                    (UUIDDatabase.UUID_REPORT_REFERENCE);
            if (descriptor != null) {
                BluetoothLeService.readDescriptor(characteristic.getDescriptor(
                        UUIDDatabase.UUID_REPORT_REFERENCE));
                ArrayList<String> reportReferenceValues = DescriptorParser.getReportReference(characteristic.
                        getDescriptor(UUIDDatabase.UUID_REPORT_REFERENCE));
                if (reportReferenceValues.size() == 2) {
                    bundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID,
                            reportReferenceValues.get(0));
                    bundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_TYPE,
                            reportReferenceValues.get(1));
                }
            }
        }
        //case for OTA characteristic received
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_OTA_UPDATE_CHARACTERISTIC)) {
            boolean isCyacd2File = Utils.getBooleanSharedPreference(mContext, Constants.PREF_IS_CYACD2_FILE);
            String intentAction = isCyacd2File
                    ? BluetoothLeService.ACTION_OTA_DATA_AVAILABLE_V1
                    : BluetoothLeService.ACTION_OTA_DATA_AVAILABLE;
            Intent intentOTA = new Intent(intentAction);
            intentOTA.putExtras(bundle);
            sendExplicitBroadcastIntent(mContext, intentOTA);
        }
        // Body sensor location read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_BODY_SENSOR_LOCATION)) {
            bundle.putString(Constants.EXTRA_HRM_BODY_SENSOR_LOCATION_VALUE,
                    HRMParser.getBodySensorLocation(characteristic, mContext));
        }
        // Manufacturer Name read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_MANUFACTURER_NAME)) {
            bundle.putString(Constants.EXTRA_MANUFACTURER_NAME,
                    Utils.getManufacturerName(characteristic));
        }
        // Model Number read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_MODEL_NUMBER)) {
            bundle.putString(Constants.EXTRA_MODEL_NUMBER,
                    Utils.getModelNumber(characteristic));
        }
        // Serial Number read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_SERIAL_NUMBER)) {
            bundle.putString(Constants.EXTRA_SERIAL_NUMBER,
                    Utils.getSerialNumber(characteristic));
        }
        // Hardware Revision read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_HARDWARE_REVISION)) {
            bundle.putString(Constants.EXTRA_HARDWARE_REVISION,
                    Utils.getHardwareRevision(characteristic));
        }
        // Firmware Revision read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_FIRMWARE_REVISION)) {
            bundle.putString(Constants.EXTRA_FIRMWARE_REVISION,
                    Utils.getFirmwareRevision(characteristic));
        }
        // Software Revision read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_SOFTWARE_REVISION)) {
            bundle.putString(Constants.EXTRA_SOFTWARE_REVISION,
                    Utils.getSoftwareRevision(characteristic));
        }
        // System ID read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_SYSTEM_ID)) {
            bundle.putString(Constants.EXTRA_SYSTEM_ID,
                    Utils.getSystemId(characteristic));
        }
        // Regulatory Certification Data List read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_REGULATORY_CERTIFICATION_DATA_LIST)) {
            bundle.putString(Constants.EXTRA_REGULATORY_CERTIFICATION_DATA_LIST,
                    Utils.byteArrayToHex(characteristic.getValue()));
        }
        // PnP ID read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_PNP_ID)) {
            bundle.putString(Constants.EXTRA_PNP_ID,
                    Utils.getPnPId(characteristic));
        }
        // Battery level read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_BATTERY_LEVEL)) {
            bundle.putString(Constants.EXTRA_BTL_VALUE,
                    Utils.getBatteryLevel(characteristic));
        }
        // Health thermometer sensor location read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_TEMPERATURE_TYPE)) {
            bundle.putString(Constants.EXTRA_HTM_TEMPERATURE_TYPE_VALUE, HTMParser
                    .parseTemperatureType(characteristic, mContext));
        }
        // CapSense proximity read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_PROXIMITY) ||
                characteristic.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_PROXIMITY_CUSTOM)) {
            bundle.putInt(Constants.EXTRA_CAPPROX_VALUE,
                    CapSenseParser.getCapSenseProximity(characteristic));
        }
        // CapSense slider read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_SLIDER) ||
                characteristic.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_SLIDER_CUSTOM)) {
            bundle.putInt(Constants.EXTRA_CAPSLIDER_VALUE,
                    CapSenseParser.getCapSenseSlider(characteristic));
        }
        // CapSense buttons read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_BUTTONS) ||
                characteristic.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_BUTTONS_CUSTOM)) {
            bundle.putIntegerArrayList(
                    Constants.EXTRA_CAPBUTTONS_VALUE,
                    CapSenseParser.getCapSenseButtons(characteristic));
        }
        // Alert level read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_ALERT_LEVEL)) {
            bundle.putString(Constants.EXTRA_ALERT_VALUE,
                    Utils.getAlertLevel(characteristic));
        }
        // Transmission power level read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_TRANSMISSION_POWER_LEVEL)) {
            bundle.putInt(Constants.EXTRA_POWER_VALUE,
                    Utils.getTransmissionPower(characteristic));
        }
        // RGB LED read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_RGB_LED) ||
                characteristic.getUuid().equals(UUIDDatabase.UUID_RGB_LED_CUSTOM)) {
            bundle.putString(Constants.EXTRA_RGB_VALUE,
                    RGBParser.getRGBAString(characteristic));
        }
        // Glucose Measurement value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_GLUCOSE_MEASUREMENT)
                || characteristic.getUuid().equals(UUIDDatabase.UUID_GLUCOSE_MEASUREMENT_CONTEXT)) {
            bundle.putSparseParcelableArray(Constants.EXTRA_GLUCOSE_MEASUREMENT,
                    GlucoseParser.getGlucoseMeasurement(characteristic));
            //Logger.e("ON glucose Measurement received..." + Utils.ByteArraytoHex(characteristic.getValue()));
        }
//        // Glucose Measurement Context value
//        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_GLUCOSE_MEASUREMENT_CONTEXT)) {
//            mBundle.putSparseParcelableArray(Constants.EXTRA_GLUCOSE_MEASUREMENT_CONTEXT,
//                    GlucoseParser.getGlucoseMeasurement(characteristic));
//            Logger.e("ON glucose Measurement context received..." + Utils.ByteArraytoHex(characteristic.getValue()));
//        }
        // Glucose RACP
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_RECORD_ACCESS_CONTROL_POINT)) {
            GlucoseParser.onCharacteristicIndicated(characteristic);
            Logger.e("ON RACP received..." + Utils.byteArrayToHex(characteristic.getValue()));
        }
        // Running speed read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_RSC_MEASURE)) {
            bundle.putStringArrayList(Constants.EXTRA_RSC_VALUE,
                    RSCParser.getRunningSpeedAndCadence(characteristic));
        }
        // Accelerometer X read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_ACCELEROMETER_READING_X)) {
            bundle.putInt(Constants.EXTRA_ACCX_VALUE, SensorHubParser
                    .getAcceleroMeterXYZReading(characteristic));
        }
        // Accelerometer Y read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_ACCELEROMETER_READING_Y)) {
            bundle.putInt(Constants.EXTRA_ACCY_VALUE, SensorHubParser
                    .getAcceleroMeterXYZReading(characteristic));
        }
        // Accelerometer Z read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_ACCELEROMETER_READING_Z)) {
            bundle.putInt(Constants.EXTRA_ACCZ_VALUE, SensorHubParser
                    .getAcceleroMeterXYZReading(characteristic));
        }
        // Temperature read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_TEMPERATURE_READING)) {
            bundle.putFloat(Constants.EXTRA_STEMP_VALUE,
                    SensorHubParser
                            .getThermometerReading(characteristic));
        }
        // Barometer read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_BAROMETER_READING)) {
            bundle.putInt(Constants.EXTRA_SPRESSURE_VALUE,
                    SensorHubParser.getBarometerReading(characteristic));
        }
        // Accelerometer scan interval read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_ACCELEROMETER_SENSOR_SCAN_INTERVAL)) {
            bundle.putInt(
                    Constants.EXTRA_ACC_SENSOR_SCAN_VALUE,
                    SensorHubParser
                            .getSensorScanIntervalReading(characteristic));
        }
        // Accelerometer analog sensor read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_ACCELEROMETER_ANALOG_SENSOR)) {
            bundle.putInt(Constants.EXTRA_ACC_SENSOR_TYPE_VALUE,
                    SensorHubParser
                            .getSensorTypeReading(characteristic));
        }
        // Accelerometer data accumulation read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_ACCELEROMETER_DATA_ACCUMULATION)) {
            bundle.putInt(Constants.EXTRA_ACC_FILTER_VALUE,
                    SensorHubParser
                            .getFilterConfiguration(characteristic));
        }
        // Temperature sensor scan read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_TEMPERATURE_SENSOR_SCAN_INTERVAL)) {
            bundle.putInt(
                    Constants.EXTRA_STEMP_SENSOR_SCAN_VALUE,
                    SensorHubParser
                            .getSensorScanIntervalReading(characteristic));
        }
        // Temperature analog sensor read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_TEMPERATURE_ANALOG_SENSOR)) {
            bundle.putInt(Constants.EXTRA_STEMP_SENSOR_TYPE_VALUE,
                    SensorHubParser
                            .getSensorTypeReading(characteristic));
        }
        // Barometer sensor scan interval read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_BAROMETER_SENSOR_SCAN_INTERVAL)) {
            bundle.putInt(
                    Constants.EXTRA_SPRESSURE_SENSOR_SCAN_VALUE,
                    SensorHubParser
                            .getSensorScanIntervalReading(characteristic));
        }
        // Barometer digital sensor
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_BAROMETER_DIGITAL_SENSOR)) {
            bundle.putInt(Constants.EXTRA_SPRESSURE_SENSOR_TYPE_VALUE,
                    SensorHubParser
                            .getSensorTypeReading(characteristic));
        }
        // Barometer threshold for indication read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_BAROMETER_THRESHOLD_FOR_INDICATION)) {
            bundle.putInt(Constants.EXTRA_SPRESSURE_THRESHOLD_VALUE,
                    SensorHubParser.getThresholdValue(characteristic));
        }

        intent.putExtras(bundle);
        /**
         * Sending the broad cast so that it can be received on registered
         * receivers
         */
        sendExplicitBroadcastIntent(mContext, intent);
    }

    private static void onOtaExitBootloaderComplete(int status) {
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.EXTRA_BYTE_VALUE, new byte[]{(byte) status});
        Intent intentOTA = new Intent(BluetoothLeService.ACTION_OTA_DATA_AVAILABLE);
        intentOTA.putExtras(bundle);
        sendExplicitBroadcastIntent(mContext, intentOTA);
    }

    /**
     * Connects to the GATT server hosted on the BlueTooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void connect(final String address, final String deviceName, Context context) {
        mContext = context;
        Utils.setIntSharedPreference(mContext, Constants.PREF_MTU_NEGOTIATED, 0);//The actual value will be set in hte onMtuChanged callback method

        if (mBluetoothAdapter == null || address == null) {
            return;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceName = deviceName;
        /**
         * Adding data to the data logger
         */
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                + "[" + deviceName + "|" + address + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Logger.dataLog(dataLog);
    }

    /**
     * Reconnect method to connect to already connected device
     */
    public static void reconnect() {
        Logger.e("<--Reconnecting device-->");
        BluetoothDevice device = getRemoteDevice();
        if (device == null) {
            return;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close(); // Disposing off previous connection resources
        }
        mBluetoothGatt = null;//Creating a new instance of GATT before connect
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        /**
         * Adding data to the data logger
         */
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Logger.dataLog(dataLog);
    }

    public static BluetoothDevice getRemoteDevice() {
        return mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress);
    }

    /**
     * Method to clear the device cache
     *
     * @param gatt
     * @return boolean
     */
    public static boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method refresh = gatt.getClass().getMethod("refresh");
            if (refresh != null) {
                return (Boolean) refresh.invoke(gatt);
            }
        } catch (Exception ex) {
            Logger.i("An exception occurred while refreshing device");
        }
        return false;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void disconnect() {
        Logger.i("disconnect called");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            // Clearing Bluetooth cache before disconnecting from the device
            if (Utils.getBooleanSharedPreference(mContext, Constants.PREF_CLEAR_CACHE_ON_DISCONNECT)) {
                BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
            }
            // Deleting bond before disconnecting from the device
            if (Utils.getBooleanSharedPreference(mContext, Constants.PREF_UNPAIR_ON_DISCONNECT)) {
                Logger.v("Deleting bond on disconnect");
                BluetoothLeService.unpairDevice();
            }
            mBluetoothGatt.disconnect();
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                    + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    mContext.getResources().getString(R.string.dl_disconnection_request);
            Logger.dataLog(dataLog);
            close();
        }
    }

    public static boolean discoverServices() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return false;
        } else {
            boolean result = mBluetoothGatt.discoverServices();
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                    + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    mContext.getResources().getString(R.string.dl_service_discovery_request);
            Logger.dataLog(dataLog);
            return result;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public static void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null
                || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            return;
        }
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);
        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);
        mBluetoothGatt.readCharacteristic(characteristic);
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                "[" + serviceName + "|" + characteristicName + "] " +
                mContext.getResources().getString(R.string.dl_characteristic_read_request);
        Logger.dataLog(dataLog);
    }

    /**
     * Request a read on a given {@code BluetoothGattDescriptor }.
     *
     * @param descriptor The descriptor to read from.
     */
    public static void readDescriptor(BluetoothGattDescriptor descriptor) {
        String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getService().getUuid(), serviceUUID);

        String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getUuid(), characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        //Logger.datalog(mContext.getResources().getString(R.string.dl_descriptor_read_request));
        mBluetoothGatt.readDescriptor(descriptor);
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                "[" + serviceName + "|" + characteristicName + "] " +
                mContext.getResources().getString(R.string.dl_characteristic_read_request);
        Logger.dataLog(dataLog);
    }

    /**
     * Request a write with no response on a given
     * {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray      to write
     */
    public static void writeCharacteristicNoResponse(BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.byteArrayToHex(byteArray);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            characteristic.setValue(byteArray);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.dataLog(dataLog);
        }
    }

    public static void writeOTABootLoaderCommand(BluetoothGattCharacteristic characteristic, byte[] value, boolean isExitBootloaderCmd) {
        synchronized (mGattCallback) {
            writeOTABootLoaderCommand(characteristic, value);
            if (isExitBootloaderCmd) {
                mOtaExitBootloaderCmdInProgress = true;
            }
        }
    }

    public static void writeOTABootLoaderCommand(BluetoothGattCharacteristic characteristic, byte[] value) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            writeOTABootLoaderCommandNoResponse(characteristic, value);
        } else {
            writeOTABootLoaderCommandWithResponse(characteristic, value);
        }
    }

    private static void writeOTABootLoaderCommandNoResponse(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.byteArrayToHex(value);

        final int mtuValue;
        if (MTU_USE_NEGOTIATED) {
            int negotiatedMtu = Utils.getIntSharedPreference(mContext, Constants.PREF_MTU_NEGOTIATED);
            mtuValue = Math.max(MTU_DEFAULT, (negotiatedMtu - MTU_NUM_BYTES_TO_SUBTRACT));
        } else {
            mtuValue = MTU_DEFAULT;
        }

        int totalLength = value.length;
        int localLength = 0;
        byte[] localValue = new byte[mtuValue];

        do {
            try {
                Logger.v("Acquire semaphore");
                writeSemaphore.acquire();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            if (totalLength >= mtuValue) {
                for (int i = 0; i < mtuValue; i++) {
                    localValue[i] = value[localLength + i];
                }
                characteristic.setValue(localValue);
                totalLength -= mtuValue;
                localLength += mtuValue;
            } else {
                byte[] lastValue = new byte[totalLength];
                for (int i = 0; i < totalLength; i++) {
                    lastValue[i] = value[localLength + i];
                }
                characteristic.setValue(lastValue);
                totalLength = 0;
            }

            int counter = 20;
            boolean status;

            do {
                int i = 0;
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                status = mBluetoothGatt.writeCharacteristic(characteristic);
                if (false == status) {
                    Logger.v("writeCharacteristic() status: False");
                    try {
                        i++;
                        Thread.sleep(100, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while ((false == status) && (counter-- > 0));

            if (status) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[ " + characteristicValue + " ]";
                Logger.dataLog(dataLog);
                Logger.v(dataLog);
            } else {
                Logger.v("Release semaphore");
                writeSemaphore.release();
                Logger.v("writeOTABootLoaderCommand failed!");
            }
        } while (totalLength > 0);
    }

    private static void writeOTABootLoaderCommandWithResponse(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.byteArrayToHex(value);
        characteristic.setValue(value);
        int counter = 20;
        boolean status;
        do {
            int i = 0;
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            status = mBluetoothGatt.writeCharacteristic(characteristic);
            if (false == status) {
                Logger.v("writeCharacteristic() status: False");
                try {
                    i++;
                    Thread.sleep(100, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while ((false == status) && (counter-- > 0));

        if (status) {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.dataLog(dataLog);
            Logger.v(dataLog);
        } else {
            Logger.v("writeOTABootLoaderCommand failed!");
        }
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray
     */
    public static void writeCharacteristicGattDb(
            BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.byteArrayToHex(byteArray);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            characteristic.setValue(byteArray);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.dataLog(dataLog);
        }
    }

    /**
     * Writes the characteristic value to the given characteristic.
     *
     * @param characteristic the characteristic to write to
     * @return true if request has been sent
     */
    public static final boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
            return false;

        Logger.v("Writing characteristic " + characteristic.getUuid());
        Logger.d("gatt.writeCharacteristic(" + characteristic.getUuid() + ")");
        return gatt.writeCharacteristic(characteristic);
    }


    /**
     * Request a write on a given {@code BluetoothGattCharacteristic} for RGB.
     *
     * @param characteristic
     * @param red
     * @param green
     * @param blue
     * @param intensity
     */
    public static void writeCharacteristicRGB(
            BluetoothGattCharacteristic characteristic, int red, int green,
            int blue, int intensity) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = new byte[4];
            valueByte[0] = (byte) red;
            valueByte[1] = (byte) green;
            valueByte[2] = (byte) blue;
            valueByte[3] = (byte) intensity;
            characteristic.setValue(valueByte);
            String characteristicValue = Utils.byteArrayToHex(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.dataLog(dataLog);
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public static void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null
                || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            return;
        }

        // Setting default write type according to CDT 222486
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookupUUID(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG, descriptorUUID);

        if (characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);

                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.byteArrayToHex(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) + "]";
                Logger.dataLog(dataLog);
            } else {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);

                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.byteArrayToHex(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) + "]";
                Logger.dataLog(dataLog);
            }
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_start_notification);
            Logger.dataLog(dataLog);
        } else {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_stop_notification);
            Logger.dataLog(dataLog);
        }
    }

    /**
     * Enables or disables indications on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable indications. False otherwise.
     */
    public static void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null
                || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            return;
        }

        // Setting default write type according to CDT 222486
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookupUUID(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG, descriptorUUID);

        if (characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);

                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" +
                        descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.byteArrayToHex(BluetoothGattDescriptor.
                        ENABLE_INDICATION_VALUE) + "]";
                Logger.dataLog(dataLog);
            } else {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);

                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.byteArrayToHex(BluetoothGattDescriptor.
                        DISABLE_NOTIFICATION_VALUE) + "]";
                Logger.dataLog(dataLog);
            }
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_start_indication);
            Logger.dataLog(dataLog);
        } else {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_stop_indication);
            Logger.dataLog(dataLog);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public static List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    public static int getConnectionState() {
        synchronized (mGattCallback) {
            return mConnectionState;
        }
    }

    public static boolean getBondedState() {
        BluetoothDevice device = getRemoteDevice();
        return device.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    public static boolean pairDevice() {
        return pairDevice(mBluetoothGatt.getDevice());
    }

    public static boolean pairDevice(BluetoothDevice device) {
        try {
            // TODO: use BluetoothDevice.createBond() public method
            Boolean rv = (Boolean) invokeBluetoothDeviceMethod(device, "createBond");
            Logger.i("Pair status: " + rv);
            return rv;
        } catch (Exception e) {
            Logger.e("Pair: exception: " + e.getMessage());
            return false;
        }
    }

    public static boolean unpairDevice() {
        return unpairDevice(mBluetoothGatt.getDevice());
    }

    public static boolean unpairDevice(BluetoothDevice device) {
        try {
            Boolean rv = (Boolean) invokeBluetoothDeviceMethod(device, "removeBond");
            Logger.i("Un-Pair status: " + rv);
            return rv;
        } catch (Exception e) {
            Logger.e("Un-Pair: exception: " + e.getMessage());
            return false;
        }
    }

    private static Object invokeBluetoothDeviceMethod(BluetoothDevice dev, String methodName, Object... args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class c = dev.getClass();
        Method m = c.getMethod(methodName);
        m.setAccessible(true);
        return m.invoke(dev, args);
    }

    private static void addRemoveData(BluetoothGattDescriptor descriptor) {
        switch (descriptor.getValue()[0]) {
            case 0:
                //Disabled notification and indication
                removeEnabledCharacteristic(descriptor.getCharacteristic());
                Logger.e("removed characteristic, size: " + mEnabledCharacteristics.size());
                break;
            case 1:
                //Enabled notification
                addEnabledCharacteristic(descriptor.getCharacteristic());
                Logger.e("added notify characteristic, size: " + mEnabledCharacteristics.size());
                break;
            case 2:
                //Enabled indication
                addEnabledCharacteristic(descriptor.getCharacteristic());
                Logger.e("added indicate characteristic, size: " + mEnabledCharacteristics.size());
                break;
        }
    }

    private static void addEnabledCharacteristic(BluetoothGattCharacteristic
                                                         bluetoothGattCharacteristic) {
        if (false == mEnabledCharacteristics.contains(bluetoothGattCharacteristic))
            mEnabledCharacteristics.add(bluetoothGattCharacteristic);
    }

    private static void removeEnabledCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (mEnabledCharacteristics.contains(bluetoothGattCharacteristic))
            mEnabledCharacteristics.remove(bluetoothGattCharacteristic);
    }

    public static boolean disableAllEnabledCharacteristics() {
        if (mEnabledCharacteristics.size() > 0) {
            mDisableNotificationFlag = true;
            BluetoothGattCharacteristic characteristic = mEnabledCharacteristics.get(0);
            Logger.e("disabling characteristic " + characteristic.getUuid());
            setCharacteristicNotification(characteristic, false);
        } else {
            mDisableNotificationFlag = false;
        }
        return mDisableNotificationFlag;
    }

    public static void enableAllRDKCharacteristics() {
        if (mRDKCharacteristics.size() > 0) {
            mEnableRDKNotificationFlag = true;
            BluetoothGattCharacteristic characteristic = mRDKCharacteristics.get(0);
            Logger.e("enabling characteristic--" + characteristic.getInstanceId());
            setCharacteristicNotification(characteristic, true);
        } else {
            Logger.e("All RDK characteristics enabled");
            mEnableRDKNotificationFlag = false;
            broadcastWriteStatusUpdate(ACTION_WRITE_COMPLETED);
        }
    }

    public static void enableAllGlucoseCharacteristics() {
        if (mGlucoseCharacteristics.size() > 0) {
            mEnableGlucoseFlag = true;
            BluetoothGattCharacteristic characteristic = mGlucoseCharacteristics.get(0);
            Logger.e("enabling characteristic--" + characteristic);
            if (characteristic.getUuid().equals(UUIDDatabase.UUID_RECORD_ACCESS_CONTROL_POINT)) {
                setCharacteristicIndication(characteristic, true);
                Logger.e("RACP Indicate");
            } else {
                setCharacteristicNotification(characteristic, true);
            }
        } else {
            Logger.e("All Glucose characteristics enabled");
            mEnableGlucoseFlag = false;
            broadcastWriteStatusUpdate(ACTION_WRITE_COMPLETED);
        }
    }

    public static boolean enableWearableDemoCharacteristics() {
        if (mWearableDemoCharacteristicsToEnable.size() > 0) {
            mEnableWearableDemoCharacteristicsFlag = true;
            BluetoothGattCharacteristic c = mWearableDemoCharacteristicsToEnable.get(0);
            Logger.e("Wearable Demo: enabling characteristic " + c);
            setCharacteristicNotification(c, true);
        } else {
            Logger.e("Wearable Demo: characteristics enabled");
            mEnableWearableDemoCharacteristicsFlag = false;
            broadcastWriteStatusUpdate(ACTION_WRITE_COMPLETED);
        }
        return mEnableWearableDemoCharacteristicsFlag;
    }

    public static boolean disableWearableDemoCharacteristics() {
        // remove characteristics which (either/or)
        // - were never enabled
        // - have been disabled as a result of previous invocation of this method
        while (mWearableDemoCharacteristicsToDisable.size() > 0) {
            if (mEnabledCharacteristics.contains(mWearableDemoCharacteristicsToDisable.get(0))) {
                break;
            }
            mWearableDemoCharacteristicsToDisable.remove(0);
        }
        if (mWearableDemoCharacteristicsToDisable.size() > 0) {
            mDisableWearableDemoCharacteristicsFlag = true;
            BluetoothGattCharacteristic c = mWearableDemoCharacteristicsToDisable.get(0);
            Logger.e("Wearable Demo: disabling characteristic " + c.getUuid());
            setCharacteristicNotification(c, false);
        } else {
            Logger.e("Wearable Demo: characteristics disabled");
            mDisableWearableDemoCharacteristicsFlag = false;
        }
        return mDisableWearableDemoCharacteristicsFlag;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public static void close() {
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local BlueTooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        return mBluetoothAdapter != null;
    }

    @Override
    public void onCreate() {
        // Initializing the service
        if (false == initialize()) {
            Logger.d("Service not initialized");
        }
    }

    /**
     * Local binder class
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}
