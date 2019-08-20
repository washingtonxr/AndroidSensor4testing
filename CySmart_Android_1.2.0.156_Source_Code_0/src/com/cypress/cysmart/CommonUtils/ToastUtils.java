package com.cypress.cysmart.CommonUtils;

import android.content.Context;
import android.widget.Toast;

import com.cypress.cysmart.CySmartApplication;

public class ToastUtils {

    private static Toast mToast;

    public static void showToast(int resId, int duration) {
        if (mToast != null) {
            mToast.cancel();
        }
        Context context = CySmartApplication.mApplication.getApplicationContext();
        mToast = Toast.makeText(context, resId, duration);
        mToast.show();
    }
}
