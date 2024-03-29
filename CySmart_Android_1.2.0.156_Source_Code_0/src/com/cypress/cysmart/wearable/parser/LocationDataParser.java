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

package com.cypress.cysmart.wearable.parser;

import com.cypress.cysmart.wearable.Const;
import com.cypress.cysmart.wearable.parser.LocationDataFlagsParser.LocationDataFlags;
import com.cypress.cysmart.wearable.utils.Utilities;

import java.nio.ByteBuffer;

public class LocationDataParser {

    private static final int NUM_BYTES = 28;

    public static LocationData parse(byte[] b) {
        ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(NUM_BYTES).order(Const.BYTE_ORDER).put(b).rewind();
        LocationDataFlags f = LocationDataFlagsParser.parse(bb);

        //uint16
        int instantaneousSpeed = f.mIsInstantaneousSpeed ? bb.getShort() : Integer.MIN_VALUE;

        int totalDistance = Integer.MIN_VALUE;
        if (f.mIsTotalDistance) {
            //uint24
            byte[] x = new byte[3];
            bb.get(x);
            totalDistance = (int) Utilities.getLong(x, 3);
        }

        //sint32
        int locationLatitude = f.mIsLocation ? bb.getInt() : Integer.MIN_VALUE;

        //sint32
        int locationLongitude = f.mIsLocation ? bb.getInt() : Integer.MIN_VALUE;

        int elevation = Integer.MIN_VALUE;
        if (f.mIsElevation) {
            //sint24
            byte[] x = new byte[3];
            bb.get(x);
            elevation = (int) Utilities.getLong(x, 3);
        }

        //uint16
        int heading = f.mIsHeading ? bb.getShort() : Short.MIN_VALUE;

        //uint8
        short rollingTime = f.mIsRollingTime ? bb.get() : Short.MIN_VALUE;

        int year = Integer.MIN_VALUE;
        short month = Short.MIN_VALUE;
        short day = Short.MIN_VALUE;
        short hours = Short.MIN_VALUE;
        short minutes = Short.MIN_VALUE;
        short seconds = Short.MIN_VALUE;
        if (f.mIsUtcTime) {
            //uint16
            year = bb.getShort();
            //uint8
            month = bb.get();
            //uint8
            day = bb.get();
            //uint8
            hours = bb.get();
            //uint8
            minutes = bb.get();
            //uint8
            seconds = bb.get();
        }

        return new LocationData(instantaneousSpeed, totalDistance, locationLatitude, locationLongitude, elevation, heading, rollingTime,
                year, month, day, hours, minutes, seconds);
    }

    public static class LocationData {

        public final int mInstantaneousSpeed;
        public final int mTotalDistance;
        public final int mLocationLatitude;
        public final int mLocationLongitude;
        public final int mElevation;
        public final int mHeading;
        public final short mRollingTime;
        public final int mYear;
        public final short mMonth;
        public final short mDay;
        public final short mHours;
        public final short mMinutes;
        public final short mSeconds;

        public LocationData(int instantaneousSpeed, int totalDistance, int locationLatitude, int locationLongitude, int elevation, int heading, short rollingTime, int year, short month, short day, short hours, short minutes, short seconds) {
            this.mInstantaneousSpeed = instantaneousSpeed;
            this.mTotalDistance = totalDistance;
            this.mLocationLatitude = locationLatitude;
            this.mLocationLongitude = locationLongitude;
            this.mElevation = elevation;
            this.mHeading = heading;
            this.mRollingTime = rollingTime;
            this.mYear = year;
            this.mMonth = month;
            this.mDay = day;
            this.mHours = hours;
            this.mMinutes = minutes;
            this.mSeconds = seconds;
        }
    }
}
