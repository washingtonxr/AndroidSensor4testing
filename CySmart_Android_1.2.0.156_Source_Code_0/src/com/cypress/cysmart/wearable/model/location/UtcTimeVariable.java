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

import android.content.res.Resources;

import com.cypress.cysmart.wearable.model.ValueWithUnit;
import com.cypress.cysmart.wearable.model.Variable;

import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class UtcTimeVariable extends Variable {

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss a");
    private static final GregorianCalendar mCalendar = new GregorianCalendar();

    private int mYear;
    private short mMonth;
    private short mDay;
    private short mHours;
    private short mMinutes;
    private short mSeconds;

    public UtcTimeVariable(Resources resources, PropertyChangeListener listener) {
        super(Id.LOC_UTC_TIME, "UTC Time", false, null, listener);
    }

    @Override
    public void setUnresolvedValue(double unresolvedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getMinValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getMaxValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMaxValue(double maxValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getValueString() {
        return mDateFormat.format(mCalendar.getTime());
    }

    public ValueWithUnit.Unit[] getSupportedUnits() {
        throw new UnsupportedOperationException();
    }

    public ValueWithUnit.Unit getUnit() {
        return null;
    }

    public void setUnit(ValueWithUnit.Unit unit) {
        throw new UnsupportedOperationException();
    }

    public void setUnresolvedDatetime(int year, short month, short day, short hours, short minutes, short seconds) {
        if (this.mYear != year
                || this.mMonth != month
                || this.mDay != day
                || this.mHours != hours
                || this.mMinutes != minutes
                || this.mSeconds != seconds) {
            this.mYear = year;
            this.mMonth = month;
            this.mDay = day;
            this.mHours = hours;
            this.mMinutes = minutes;
            this.mSeconds = seconds;

            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_YEAR, day);
            mCalendar.set(Calendar.HOUR, hours);
            mCalendar.set(Calendar.MINUTE, minutes);
            mCalendar.set(Calendar.SECOND, seconds);
            mCalendar.set(Calendar.MILLISECOND, 0);

            mPropertyChangeSupport.firePropertyChange("value", null, null); // TODO: optimize
        }
    }
}
