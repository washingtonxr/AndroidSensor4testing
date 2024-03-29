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
package com.cypress.cysmart.BLEProfileDataParserClasses;

import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.R;
import com.cypress.cysmart.RDKEmulatorView.ReportAttributes;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Parser class for parsing the descriptor value
 */
public class DescriptorParser {

    //Switch case Constants
    private static final int CASE_NOTIFY_DISABLED_IND_DISABLED = 0;
    private static final int CASE_NOTIFY_ENABLED_IND_DISABLED = 1;
    private static final int CASE_IND_ENABLED_NOTIFY_DISABLED = 2;
    private static final int CASE_IND_ENABLED_NOTIFY_ENABLED = 3;

    public static String getClientCharacteristicConfiguration(BluetoothGattDescriptor descriptor, Context context) {
        String valueConverted = "";
        byte[] array = descriptor.getValue();
        switch (array[0]) {
            case CASE_NOTIFY_DISABLED_IND_DISABLED:
                valueConverted = context.getResources().getString(R.string.descriptor_notification_disabled)
                        + "\n" + context.getResources().getString(R.string.descriptor_indication_disabled);
                break;
            case CASE_NOTIFY_ENABLED_IND_DISABLED:
                valueConverted = context.getResources().getString(R.string.descriptor_notification_enabled)
                        + "\n" + context.getResources().getString(R.string.descriptor_indication_disabled);
                break;
            case CASE_IND_ENABLED_NOTIFY_DISABLED:
                valueConverted = context.getResources().getString(R.string.descriptor_indication_enabled)
                        + "\n" + context.getResources().getString(R.string.descriptor_notification_disabled);
                break;
            case CASE_IND_ENABLED_NOTIFY_ENABLED:
                valueConverted = context.getResources().getString(R.string.descriptor_indication_enabled)
                        + "\n" + context.getResources().getString(R.string.descriptor_notification_enabled);
                break;
        }
        return valueConverted;
    }

    public static HashMap<String, String> getCharacteristicExtendedProperties(BluetoothGattDescriptor descriptor, Context context) {
        HashMap<String, String> valuesMap = new HashMap<String, String>();

        String reliableWriteStatus;
        String writableAuxillaryStatus;
        byte reliableWriteBit = descriptor.getValue()[0];
        byte writableAuxillaryBit = descriptor.getValue()[1];

        if ((reliableWriteBit & 0x01) != 0) {
            reliableWriteStatus = context.getResources().getString(R.string.descriptor_reliablewrite_enabled);
        } else {
            reliableWriteStatus = context.getResources().getString(R.string.descriptor_reliablewrite_disabled);
        }
        if ((writableAuxillaryBit & 0x01) != 0) {
            writableAuxillaryStatus = context.getResources().getString(R.string.descriptor_writableauxillary_enabled);
        } else {
            writableAuxillaryStatus = context.getResources().getString(R.string.descriptor_writableauxillary_disabled);
        }
        valuesMap.put(Constants.FIRST_BIT_KEY_VALUE, reliableWriteStatus);
        valuesMap.put(Constants.SECOND_BIT_KEY_VALUE, writableAuxillaryStatus);
        return valuesMap;
    }

    public static String getCharacteristicUserDescription(BluetoothGattDescriptor descriptor) {
        Charset UTF8_CHARSET = Charset.forName("UTF-8");
        byte[] valueEncoded = descriptor.getValue();
        return new String(valueEncoded, UTF8_CHARSET);
    }

    public static String getServerCharacteristicConfiguration(BluetoothGattDescriptor descriptor, Context context) {
        byte firstBit = descriptor.getValue()[0];
        String broadcastStatus;
        if ((firstBit & 0x01) != 0) {
            broadcastStatus = context.getResources().getString(R.string.descriptor_broadcast_enabled);
        } else {
            broadcastStatus = context.getResources().getString(R.string.descriptor_broadcast_disabled);
        }
        return broadcastStatus;
    }

    public static ArrayList<String> getReportReference(BluetoothGattDescriptor descriptor) {
        ArrayList<String> reportReferencevalues = new ArrayList<String>(2);
        byte[] array = descriptor.getValue();
        String reportReferenceID = ReportAttributes.REPORT_REF_ID;
        String reportType = ReportAttributes.REPORT_TYPE;
        if (array != null && array.length == 2) {
            reportReferenceID = ReportAttributes.lookupReportReferenceID("" + array[0]);
            reportType = ReportAttributes.lookupReportReferenceType("" + array[1]);
            reportReferencevalues.add(reportReferenceID);
            reportReferencevalues.add(reportType);
        } else if (array != null && array.length == 1) {
            reportReferenceID = ReportAttributes.lookupReportReferenceID("" + array[0]);
            reportReferencevalues.add(reportReferenceID);
            reportReferencevalues.add(reportType);
        }
        return reportReferencevalues;
    }

    public static String getCharacteristicPresentationFormat(BluetoothGattDescriptor descriptor, Context context) {
        String value = "";
        String formatKey = String.valueOf(descriptor.getValue()[0]);
        String formatValue = GattAttributes.lookCharacteristicPresentationFormat(formatKey);
        String exponentValue = String.valueOf(descriptor.getValue()[1]);
        byte unit1 = descriptor.getValue()[2];
        byte unit2 = descriptor.getValue()[3];
        String unitValue = String.valueOf(((unit1 & 0xFF) | unit2 << 8));
        String namespaceValue = String.valueOf(descriptor.getValue()[4]);
        if (namespaceValue.equalsIgnoreCase("1")) {
            namespaceValue = context.getResources().getString(R.string.descriptor_bluetoothSIGAssignedNo);
        } else {
            namespaceValue = context.getResources().getString(R.string.descriptor_reservedforFutureUse);;
        }
        String descriptionValue = String.valueOf(descriptor.getValue()[5]);
        value = context.getResources().getString(R.string.descriptor_format) +  "\n" +
                context.getResources().getString(R.string.exponent) + exponentValue + "\n" +
                context.getResources().getString(R.string.unit) + unitValue + "\n" +
                context.getResources().getString(R.string.namespace) + namespaceValue + "\n" +
                context.getResources().getString(R.string.description) + descriptionValue;
        return value;
    }
}
