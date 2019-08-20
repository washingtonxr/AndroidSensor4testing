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

package com.cypress.cysmart.OTAFirmwareUpdate;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;

import com.cypress.cysmart.HomePageActivity;
import com.cypress.cysmart.R;

public abstract class NotificationHandlerBase {

    protected final int mNotificationId = 1;
    protected NotificationManager mNotificationManager;
    protected Notification.Builder mBuilder;

    public void initializeNotification(Context context) {
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        createChannel(); // Android O

        String text = String.format("%s %d%%", context.getResources().getString(R.string.ota_notification_ongoing), 0);
        mBuilder = new Notification.Builder(context)
                .setContentTitle(context.getResources().getString(R.string.ota_notification_title))
                .setContentText(text)
                .setSmallIcon(R.drawable.appicon_monochrome)
                .setAutoCancel(false);

        setChannelId(); // Android O
    }

    protected void createChannel() {

    }

    protected void setChannelId() {

    }

    public void generatePendingNotification(Context context) {
        // Displays the progress bar for the first time.
        mBuilder.setProgress(100, 0, false);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, HomePageActivity.class);

        // This somehow makes sure there is only one CountDownTimer going if the notification is pressed
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(HomePageActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        // Make this unique ID to make sure there is not generated just a brand new intent with new extra values:
        int requestID = (int) System.currentTimeMillis();

        // Pass the unique ID to the resultPendingIntent:
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, requestID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);

        // notificationId allows you to update the notification later on.
        mNotificationManager.notify(mNotificationId, mBuilder.build());
    }

    public void cancelPendingNotification() {
        mNotificationManager.cancel(mNotificationId);
    }

    public void completeProgress(Context context, int contentTextResourceId) {
        mBuilder
                .setContentText(context.getResources().getText(contentTextResourceId))
                .setProgress(0, 0, false);
        mNotificationManager.notify(mNotificationId, mBuilder.build());
    }

    public void updateProgress(Context context, int limit, int updateLimit, boolean flag) {
        String text = String.format("%s %d%%", context.getResources().getString(R.string.ota_notification_ongoing), updateLimit);
        mBuilder.setContentText(text)
                .setProgress(limit, updateLimit, flag);
        mNotificationManager.notify(mNotificationId, mBuilder.build());
    }
}
