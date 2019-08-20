package com.cypress.cysmart.wearable;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.ProgressIndicatorToggle;
import com.cypress.cysmart.R;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractFragment extends Fragment {

    private Handler mHandler = new Handler();
    protected ExpandableListView mListView;
    protected ProgressDialog mProgressDialog;
    protected int mBackstackCount;
    private boolean mReceiverRegistered;

    protected ProgressIndicatorToggle mProgressIndicatorToggle =
            new ProgressIndicatorToggle(
                    500,
                    new ProgressIndicatorToggle.ReadyTest() {
                        @Override
                        public boolean isReady() {
                            return true; // TODO
                        }
                    });

    private Runnable mDismissProgressDialogRunnable = new Runnable() {
        @Override
        public void run() {
            mProgressDialog.dismiss();
        }
    };

    private Runnable mShowProgressDialogRunnable = new Runnable() {
        @Override
        public void run() {
            mProgressDialog.show();
        }
    };

    protected final Runnable mResponseTimer = new Runnable() {
        @Override
        public void run() {
            BluetoothLeService.disconnect();
            if (getActivity() != null) {
                Intent intent = getActivity().getIntent();
                getActivity().finish();
                getActivity().overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                getActivity().startActivity(intent);
                getActivity().overridePendingTransition(R.anim.slide_right, R.anim.push_right);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.err.println("--BASE: CREATE");
        super.onCreate(savedInstanceState);
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setCancelable(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        System.err.println("--BASE: ACTIVITY CREATED");
        super.onActivityCreated(savedInstanceState);
    }


    protected void toggleProgressOn(String message) {
        mProgressDialog.setMessage(message);
        mProgressIndicatorToggle.on(mShowProgressDialogRunnable);
        mHandler.postDelayed(mResponseTimer,
                Const.WAIT_FOR_BLE_RESPONSE_TIMEOUT_MILLIS);
    }

    protected void toggleProgressOff() {
        mHandler.removeCallbacks(mResponseTimer);
        mProgressIndicatorToggle.off(mDismissProgressDialogRunnable);
    }

    protected void refreshChildView(int groupPosition, int childPosition) {
        for (int start = mListView.getFirstVisiblePosition(), i = start, j = mListView.getLastVisiblePosition(); i <= j; i++) {
            long packedPos = mListView.getExpandableListPosition(i);
            int packedPosType = ExpandableListView.getPackedPositionType(packedPos);
            if (packedPosType != ExpandableListView.PACKED_POSITION_TYPE_NULL) {
                int groupPos = ExpandableListView.getPackedPositionGroup(packedPos);
                if (groupPos == groupPosition
                        && packedPosType == ExpandableListView.PACKED_POSITION_TYPE_CHILD
                        && ExpandableListView.getPackedPositionChild(packedPos) == childPosition) {
                    View view = mListView.getChildAt(i - start);
                    mListView.getAdapter().getView(i, view, mListView);
                    break;
                }
            }
        }
    }

    protected void addFragment(Fragment fragment, String tagName) {
        FragmentManager fm = getFragmentManager();
        mBackstackCount = fm.getBackStackEntryCount();
        fm.beginTransaction()
                .add(R.id.container, fragment, tagName)
                .addToBackStack(null)
                .commit();
    }

    protected void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
        if (mReceiverRegistered) {
            mReceiverRegistered = false;
            System.err.println("--BASE: UNREGISTER RECEIVER");
            BluetoothLeService.unregisterBroadcastReceiver(getActivity(), receiver);
        }
    }

    protected void registerBroadcastReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        if (!mReceiverRegistered) {
            mReceiverRegistered = true;
            System.err.println("--BASE: REGISTER RECEIVER");
            BluetoothLeService.registerBroadcastReceiver(getActivity(), receiver, filter);
        }
    }

    @NonNull
    protected IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter(BluetoothLeService.ACTION_DATA_AVAILABLE);
        filter.addAction(BluetoothLeService.ACTION_WRITE_SUCCESS);
        filter.addAction(BluetoothLeService.ACTION_WRITE_COMPLETED);
        return filter;
    }

    protected void enableNotifications(Collection<BluetoothGattCharacteristic> characteristics) {
        BluetoothLeService.mWearableDemoCharacteristicsToEnable = new ArrayList<>(characteristics);
//        BluetoothLeService.mEnabledCharacteristics = new ArrayList<>(); // TODO
        BluetoothLeService.mDisableNotificationFlag = false;
        if (BluetoothLeService.enableWearableDemoCharacteristics()) {
            toggleProgressOn(getEnablingNotificationsMessage());
        }
    }

    protected void disableNotifications(Collection<BluetoothGattCharacteristic> characts) {
        BluetoothLeService.mWearableDemoCharacteristicsToDisable = new ArrayList<>(characts);
        if (BluetoothLeService.disableWearableDemoCharacteristics()) {
            toggleProgressOn(getDisablingNotificationsMessage());
        }
    }

    protected void disableAllNotifications() {
        disableAllNotifications(true);
    }

    protected void disableAllNotifications(boolean toast) {
        if (BluetoothLeService.disableAllEnabledCharacteristics() && toast) {
            Toast.makeText(getActivity(), getString(R.string.profile_control_stop_both_notify_indicate_toast),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    protected String getEnablingNotificationsMessage() {
        return getString(R.string.message_notifications_enabling)
                + ".\n" + getString(R.string.alert_message_wait);
    }

    @NonNull
    protected String getNotificationsEnabledMessage() {
        return getString(R.string.message_notifications_enabled);
    }

    @NonNull
    protected String getDisablingNotificationsMessage() {
        return getString(R.string.message_notifications_disabling)
                + ".\n" + getString(R.string.alert_message_wait);
    }

    @NonNull
    protected String getNotificationsDisabledMessage() {
        return getString(R.string.message_notifications_disabled);
    }
}
