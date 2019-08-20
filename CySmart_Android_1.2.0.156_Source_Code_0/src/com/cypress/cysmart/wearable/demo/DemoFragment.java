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

package com.cypress.cysmart.wearable.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;
import com.cypress.cysmart.wearable.model.Category;
import com.cypress.cysmart.wearable.model.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoFragment extends CategoryListFragment {

    public static final String TAG = "Wearable Solution Demo";

    public static DemoFragment create() {
        Map<Category.Id, List<Variable.Id>> cats = new HashMap<>();
        cats.put(Category.Id.ACTIVITY, Arrays.asList(
                Variable.Id.ACT_STEPS, Variable.Id.ACT_DURATION, Variable.Id.ACT_CALORIES,
                Variable.Id.ACT_DISTANCE, Variable.Id.ACT_SPEED, Variable.Id.ACT_FLOORS,
                Variable.Id.ACT_SLEEP));
        cats.put(Category.Id.ENVIRONMENT, Arrays.asList(
                Variable.Id.ENV_TEMPERATURE, Variable.Id.ENV_UV, Variable.Id.ENV_AIR_QUALITY,
                Variable.Id.ENV_PRESSURE, Variable.Id.ENV_ALTITUDE
        ));
        cats.put(Category.Id.LOCATION, Arrays.asList(
                Variable.Id.LOC_POSITION, Variable.Id.LOC_ALTITUDE, Variable.Id.LOC_SPEED));
        DemoFragment f = new DemoFragment();
        f.mCategoriesToBuild = cats;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Utils.setUpActionBar(getActivity(), R.string.wearable_demo_action_bar_title);
        return view;
    }
}
