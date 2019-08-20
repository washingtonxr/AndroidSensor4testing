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

package com.cypress.cysmart.wearable.utils;

import android.content.Context;
import android.content.res.Configuration;

import com.cypress.cysmart.wearable.Const;

import java.nio.ByteOrder;

public class Utilities {

    public static long getLong(byte[] b, int n) {
        return getLong(b, n, Const.BYTE_ORDER);
    }

    private static long getLong(byte[] b, int n, ByteOrder order) {
        long v = 0;
        for (int i = 0; i < n; i++) {
            int x = order == ByteOrder.LITTLE_ENDIAN ? i : n - 1 - i;
            v |= ((long) b[x] << (i * 8) & (0xFFl << (i * 8)));
        }
        return v;
    }

    public static final class BitField {

        private long mBits;
        private int mPos;

        public BitField(long bits) {
            this.mBits = bits;
        }

        public boolean isSet() {
            return ((mBits >> mPos++) & 1) != 0;
        }

        public long getNumber(int numBits) {
            int mask = (int) (Math.pow(2, numBits) - 1);
            long res = (mBits >> mPos) & mask;
            mPos += numBits;
            return res;
        }
    }

    public static boolean isHardwareKeyboardAvailable(Context context) {
        return context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
    }
}
