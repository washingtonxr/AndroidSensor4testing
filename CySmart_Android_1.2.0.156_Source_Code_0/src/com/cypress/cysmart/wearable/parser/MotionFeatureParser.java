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

public class MotionFeatureParser {

    private static final int NUM_BYTES = 2;

    public static MotionFeature parse(byte[] b) {
        return parse((ByteBuffer) ByteBuffer.allocate(NUM_BYTES).order(Const.BYTE_ORDER).put(b, 0, NUM_BYTES).rewind());
    }

    public static MotionFeature parse(ByteBuffer bb) {
        Utilities.BitField s = new Utilities.BitField(bb.getShort());
        return new MotionFeature(s.isSet(), s.isSet(), s.isSet(), s.isSet(), s.isSet(), s.isSet(), s.isSet(), s.isSet(), s.isSet(), s.isSet(), s.isSet());
    }

    public static class MotionFeature {

        public final boolean mIsAcc;
        public final boolean mIsMag;
        public final boolean mIsGyr;
        public final boolean mIsOrientation;
        public final boolean mIsSteps;
        public final boolean mIsCalories;
        public final boolean mIsSleep;
        public final boolean mIsDuration;
        public final boolean mIsDistance;
        public final boolean mIsSpeed;
        public final boolean mIsFloors;

        public MotionFeature(boolean isAcc, boolean isMag, boolean isGyr, boolean isOrientation, boolean isSteps,
                             boolean isCalories, boolean isSleep, boolean isDuration, boolean isDistance, boolean isSpeed, boolean isFloors) {
            this.mIsAcc = isAcc;
            this.mIsMag = isMag;
            this.mIsGyr = isGyr;
            this.mIsOrientation = isOrientation;
            this.mIsSteps = isSteps;
            this.mIsCalories = isCalories;
            this.mIsSleep = isSleep;
            this.mIsDuration = isDuration;
            this.mIsDistance = isDistance;
            this.mIsSpeed = isSpeed;
            this.mIsFloors = isFloors;
        }
    }
}
