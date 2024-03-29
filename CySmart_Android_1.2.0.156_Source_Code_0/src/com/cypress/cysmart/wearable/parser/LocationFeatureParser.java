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
import com.cypress.cysmart.wearable.utils.Utilities;

import java.nio.ByteBuffer;

public class LocationFeatureParser {

    private static final int NUM_BYTES = 4;

    public static LocationFeature parse(byte[] b) {
        return parse((ByteBuffer) ByteBuffer.allocate(NUM_BYTES).order(Const.BYTE_ORDER).put(b, 0, NUM_BYTES).rewind());
    }

    public static LocationFeature parse(ByteBuffer bb) {
        Utilities.BitField bf = new Utilities.BitField(bb.getInt());
        return new LocationFeature(bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet(), bf.isSet());
    }

    public static class LocationFeature {

        public final boolean mIsInstantaneousSpeed;
        public final boolean mIsTotalDistance;
        public final boolean mIsLocation;
        public final boolean mIsElevation;
        public final boolean mIsHeading;
        public final boolean mIsRollingTime;
        public final boolean mIsUtcTime;
        public final boolean mIsRemainingDistance;
        public final boolean mIsRemainingVerticalDistance;
        public final boolean mIsEstimatedTimeOfArrival;
        public final boolean mIsNumberOfBeaconsInSolution;
        public final boolean mIsNumberOfBeaconsInView;
        public final boolean mIsTimeToFirstFix;
        public final boolean mIsEstimatedHorizontalPositionError;
        public final boolean mIsEstimatedVerticalPositionError;
        public final boolean mIsHorizontalDilutionOfPrecision;
        public final boolean mIsVerticalDilutionOfPrecision;
        public final boolean mIsLocationAndSpeedCharacteristicContentMasking;
        public final boolean mIsFixRateSetting;
        public final boolean mIsElevationSetting;
        public final boolean mIsPositionStatus;

        public LocationFeature(boolean isInstantaneousSpeed, boolean isTotalDistance, boolean isLocation,
                               boolean isElevation, boolean isHeading, boolean isRollingTime, boolean isUtcTime,
                               boolean isRemainingDistance, boolean isRemainingVerticalDistance, boolean isEstimatedTimeOfArrival,
                               boolean isNumberOfBeaconsInSolution, boolean isNumberOfBeaconsInView, boolean isTimeToFirstFix,
                               boolean isEstimatedHorizontalPositionError, boolean isEstimatedVerticalPositionError,
                               boolean isHorizontalDilutionOfPrecision, boolean isVerticalDilutionOfPrecision,
                               boolean isLocationAndSpeedCharacteristicContentMasking, boolean isFixRateSetting,
                               boolean isElevationSetting, boolean isPositionStatus) {

            this.mIsInstantaneousSpeed = isInstantaneousSpeed;
            this.mIsTotalDistance = isTotalDistance;
            this.mIsLocation = isLocation;
            this.mIsElevation = isElevation;
            this.mIsHeading = isHeading;
            this.mIsRollingTime = isRollingTime;
            this.mIsUtcTime = isUtcTime;
            this.mIsRemainingDistance = isRemainingDistance;
            this.mIsRemainingVerticalDistance = isRemainingVerticalDistance;
            this.mIsEstimatedTimeOfArrival = isEstimatedTimeOfArrival;
            this.mIsNumberOfBeaconsInSolution = isNumberOfBeaconsInSolution;
            this.mIsNumberOfBeaconsInView = isNumberOfBeaconsInView;
            this.mIsTimeToFirstFix = isTimeToFirstFix;
            this.mIsEstimatedHorizontalPositionError = isEstimatedHorizontalPositionError;
            this.mIsEstimatedVerticalPositionError = isEstimatedVerticalPositionError;
            this.mIsHorizontalDilutionOfPrecision = isHorizontalDilutionOfPrecision;
            this.mIsVerticalDilutionOfPrecision = isVerticalDilutionOfPrecision;
            this.mIsLocationAndSpeedCharacteristicContentMasking = isLocationAndSpeedCharacteristicContentMasking;
            this.mIsFixRateSetting = isFixRateSetting;
            this.mIsElevationSetting = isElevationSetting;
            this.mIsPositionStatus = isPositionStatus;
        }
    }
}
