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

package com.cypress.cysmart.wearable.model.location;

import com.cypress.cysmart.wearable.model.ValueWithUnit;

// Unit is in degrees with a resolution of 1/(10^7)
public abstract class AbstractPosition extends ValueWithUnit<AbstractPosition, AbstractPosition.Unit> {

    private static final Unit DEFAULT_UNIT = Unit.DEG;
    private static final double EXPONENT = -7;
    private static final double MIN_IN_1_DEG = 60;
    private static final double SEC_IN_1_MIN = 60;

    protected AbstractPosition() {
        super(DEFAULT_UNIT);
        // we want display 4 decimal places for seconds
        mNumberFormat.setMinimumFractionDigits(4);
        mNumberFormat.setMaximumFractionDigits(4);
    }

    public static class Unit extends ValueWithUnit.Unit {

        public static final Unit DEG = new Unit("deg");
        public static final Unit DEG_MIN_SEC = new Unit("deg min sec");
        private static final Unit[] ALL_UNITS = {DEG, DEG_MIN_SEC};

        private Unit(String text) {
            super(text);
        }
    }

    @Override
    public String getValueString() {
        String s;
        if (getUnit() == Unit.DEG) {
            s = "" + getValue() + "\u00B0 ";
        } else {
            int ideg = (int) getValue();
            double min = (getValue() - ideg) * MIN_IN_1_DEG;
            int imin = (int) min;
            double sec = (min - imin) * SEC_IN_1_MIN;
            s = "" + ideg + "\u00B0" + imin + "\u2032" + mNumberFormat.format(sec) + "\u2033";
        }
        return s;
    }

    @Override
    public Unit[] getSupportedUnits() {
        return Unit.ALL_UNITS;
    }

    @Override
    protected double convert(double value, Unit from, Unit to) {
        return value; // no conversion required
    }

    @Override
    protected Unit getDefaultUnit() {
        return DEFAULT_UNIT;
    }

    @Override
    protected double getExponent() {
        return EXPONENT;
    }
}
