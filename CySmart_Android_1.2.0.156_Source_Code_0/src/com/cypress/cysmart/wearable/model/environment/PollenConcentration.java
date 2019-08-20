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

package com.cypress.cysmart.wearable.model.environment;

import com.cypress.cysmart.wearable.model.ValueWithUnit;

// Unit is in concentration count per cubic meter with a resolution of 1/m^3
public class PollenConcentration extends ValueWithUnit<PollenConcentration, PollenConcentration.Unit> {

    private static final Unit DEFAULT_UNIT = Unit.COUNT_PER_CUBIC_METER;
    private static final int EXPONENT = 0;
    private static final int CM3_IN_1_M3 = 1000000;

    public PollenConcentration() {
        super(DEFAULT_UNIT);
        mScale = 8; // TODO
    }

    public static class Unit extends ValueWithUnit.Unit {

        public static final Unit COUNT_PER_CUBIC_METER = new Unit("1/\u006D\u00B3");//\u33A5");
        public static final Unit COUNT_PER_CUBIC_CENTIMETER = new Unit("1/\u0063\u006D\u00B3");//\u33A4");
        private static final Unit[] ALL_UNITS = {COUNT_PER_CUBIC_METER, COUNT_PER_CUBIC_CENTIMETER};

        private Unit(String text) {
            super(text);
        }
    }

    @Override
    public Unit[] getSupportedUnits() {
        return Unit.ALL_UNITS;
    }

    @Override
    protected double convert(double value, Unit from, Unit to) {
        if (from == Unit.COUNT_PER_CUBIC_METER) {
            value /= CM3_IN_1_M3;
        } else {
            value *= CM3_IN_1_M3;
        }
        return value;
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
