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

package com.cypress.cysmart.CommonUtils;

import android.os.Handler;
import android.os.Looper;

public class ProgressIndicatorToggle {

    public interface ReadyTest {
        boolean isReady();
    }

    private long mDelay;
    private ReadyTest mTest;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mOnAction;

    public ProgressIndicatorToggle(ReadyTest test) {
        this(100, test);
    }

    public ProgressIndicatorToggle(long delay, ReadyTest test) {
        this.mDelay = delay;
        this.mTest = test;
    }

    public void toggle(boolean on, final Runnable action) {
        if (on)
            on(action);
        else
            off(action);
    }

    // wait N milliseconds and only then showToast progress indicator
    public void on(final Runnable action) {
        // discard previous task
        if (mOnAction != null)
            mHandler.removeCallbacks(mOnAction);
        // submit new task
        mOnAction = new Runnable() {
            @Override
            public void run() {
                if (mTest.isReady())
                    action.run();
            }
        };
        mHandler.postDelayed(mOnAction, mDelay);
    }

    // hide progress indicator
    public void off(Runnable action) {
        // discard active task
        if (mOnAction != null) {
            mHandler.removeCallbacks(mOnAction);
            mOnAction = null;
        }
        action.run();
    }
}