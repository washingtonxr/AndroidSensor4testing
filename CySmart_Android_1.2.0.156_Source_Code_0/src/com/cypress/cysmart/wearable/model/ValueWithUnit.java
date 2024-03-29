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

package com.cypress.cysmart.wearable.model;

import com.cypress.cysmart.wearable.Const;

import java.text.NumberFormat;

public abstract class ValueWithUnit<V extends ValueWithUnit<V, U>, U extends ValueWithUnit.Unit> {

    public static abstract class Unit {

        private final String mText;

        protected Unit(String text) {
            this.mText = text;
        }

        @Override
        public String toString() {
            return mText;
        }
    }

    protected final NumberFormat mNumberFormat = NumberFormat.getNumberInstance();

    private double mValue;
    private U mUnit;
    protected int mScale = Const.SCALE;

    protected ValueWithUnit(U unit) {
        setUnit(unit);
    }

    // unresolved value in default unit
    public double getUnresolvedValueInDefaultUnit() {
        double value = this.mValue;
        if (getDefaultUnit() != null && mUnit != getDefaultUnit()) {
            value = convert(value, mUnit, getDefaultUnit()); // convert
        }
        value /= Math.pow(10, getExponent()); // resolve
        return value;
    }

    // unresolved value in default unit
    public void setUnresolvedValueInDefaultUnit(double value) {
        value *= Math.pow(10, getExponent()); // resolve
        if (getDefaultUnit() != null && mUnit != getDefaultUnit()) {
            value = convert(value, getDefaultUnit(), mUnit); // convert
        }
        this.mValue = value;
    }

    public double getValue() {
        return mValue;
    }

    public String getValueString() {
        applyScale(mScale);
        return mNumberFormat.format(getValue());
    }

    public U getUnit() {
        return mUnit;
    }

    public void setUnit(U unit) {
        if (unit == null)
            throw new NullPointerException();

        if (this.mUnit != null && this.mUnit != unit) {
            mValue = convert(mValue, this.mUnit, unit);
        }
        this.mUnit = unit;
    }

    public abstract U[] getSupportedUnits();

    /**
     * It is guaranteed that "from" and "to" are different
     */
    protected abstract double convert(double value, U from, U to);

    protected abstract U getDefaultUnit();

    protected abstract double getExponent();

    private void applyScale(int scale) {
        double d = getValue();
        if (scale > Const.SCALE && d == (long) d) { // no reason to show extra 0s for integers
            scale = Const.SCALE;
        }
        mNumberFormat.setMinimumFractionDigits(scale);
        mNumberFormat.setMaximumFractionDigits(scale);
    }
}
