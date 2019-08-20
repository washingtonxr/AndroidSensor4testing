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

package com.cypress.cysmart.wearable.model.motion;

import com.cypress.cysmart.wearable.model.ValueWithUnit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

// TODO: default unit?
public class Duration extends ValueWithUnit<Duration, Duration.Unit> {

    private static final Unit DEFAULT_UNIT = Unit.SECONDS;
    private static final int EXPONENT = 0;
    private static final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm:ss");
    private static final Calendar mCalendar = new GregorianCalendar();

    public Duration() {
        super(DEFAULT_UNIT);
    }

    public static class Unit extends ValueWithUnit.Unit {

        public static final Unit SECONDS = new Unit("ss");
        public static final Unit MINUTES = new Unit("mm");
        public static final Unit HOURS = new Unit("hh");
        public static final Unit TIME = new Unit("hh:mm:ss");
        private static final Unit[] ALL_UNITS = {SECONDS, MINUTES, HOURS, TIME};

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
        if (from == Unit.SECONDS) {
            if (to == Unit.MINUTES) {
                value /= 60;
            } else if (to == Unit.HOURS) {
                value /= 3600;
            } else if (to == Unit.TIME) {
                // leave value in seconds
            }
        } else {
            if (to != Unit.SECONDS) {
                // first convert to seconds...
                double seconds = convert(value, from, Unit.SECONDS);
                // ... then convert to final unit
                return convert(seconds, Unit.SECONDS, to);
            }
            if (to != Unit.SECONDS) {
                throw new IllegalArgumentException();
            }
            if (from == Unit.MINUTES) {
                value *= 60;
            } else if (from == Unit.HOURS) {
                value *= 3600;
            } else if (from == Unit.TIME) {
                // value already in seconds
            }
        }
        return value;
    }

    @Override
    public String getValueString() {
        if (getUnit() == Unit.TIME) {
            mCalendar.set(Calendar.HOUR_OF_DAY, 0);
            mCalendar.set(Calendar.MINUTE, 0);
            mCalendar.set(Calendar.SECOND, (int) convert(getValue(), Unit.TIME, Unit.SECONDS));
            mCalendar.set(Calendar.MILLISECOND, 0);
            return mFormat.format(mCalendar.getTime());
        }
        return super.getValueString();
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
