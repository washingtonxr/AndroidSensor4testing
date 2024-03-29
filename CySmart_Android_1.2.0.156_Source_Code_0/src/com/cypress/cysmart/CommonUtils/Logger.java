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

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.cypress.cysmart.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is a custom log class that will manage logs in the project. Using the
 * <b>disableLog()</b> all the logs can be disabled in the project during the
 * production stage <b> enableLog()</b> will allow to enable the logs , by
 * default the logs will be visible.<br>
 * *
 */
public class Logger {

    public static final String DATA_LOGGER_FILE_EXTENSION = ".txt";
    private static String mLogTag = "CySmart Android";
    private static boolean mLogflag = true;
    private static File mDataLoggerDirectory;
    private static File mDataLoggerFile;
    private static Context mContext;

    public static void d(String message) {
        show(Log.DEBUG, mLogTag, message);

    }

    public static void d(String tag, String message) {
        show(Log.DEBUG, tag, message);

    }

    public static void w(String message) {
        show(Log.WARN, mLogTag, message);

    }

    public static void i(String message) {
        show(Log.INFO, mLogTag, message);

    }

    public static void e(String message) {
        show(Log.ERROR, mLogTag, message);

    }

    public static void v(String message) {
        show(Log.VERBOSE, mLogTag, message);

    }

    public static void dataLog(String message) {
        // showToast(Log.INFO, mLogTag, message);
        saveLogData(message);

    }

    /**
     * print log for info/error/debug/warn/verbose
     *
     * @param type : <br>
     *             Log.INFO <br>
     *             Log.ERROR <br>
     *             Log.DEBUG <br>
     *             Log.WARN <br>
     *             Log.VERBOSE Log.
     */
    private static void show(int type, String tag, String msg) {

        if (msg.length() > 4000) {
            Log.i("Length ", msg.length() + "");

            while (msg.length() > 4000) {
                show(type, tag, msg.substring(0, 4000));
                msg = msg.substring(4000, msg.length());

            }
        }
        if (mLogflag)
            switch (type) {
                case Log.INFO:
                    Log.i(tag, msg);
                    break;
                case Log.ERROR:
                    Log.e(tag, msg);
                    break;
                case Log.DEBUG:
                    Log.d(tag, msg);
                    break;
                case Log.WARN:
                    Log.w(tag, msg);
                    break;
                case Log.VERBOSE:
                    Log.v(tag, msg);
                    break;
                case Log.ASSERT:
                    Log.wtf(tag, msg);
                    break;
                default:
                    break;
            }

    }

    public static void createDataLoggerFile(Context context) {
        mContext = context;
        try {
            mDataLoggerDirectory = new File(Environment.getExternalStorageDirectory() + File.separator + context.getResources().getString(R.string.dl_directory));
            if (!mDataLoggerDirectory.exists()) {
                mDataLoggerDirectory.mkdirs();
            }

            mDataLoggerFile = new File(mDataLoggerDirectory.getAbsoluteFile() + File.separator + Utils.GetDate() + DATA_LOGGER_FILE_EXTENSION);
            if (!mDataLoggerFile.exists()) {
                mDataLoggerFile.createNewFile();
            }

            deleteOldFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteOldFiles() {
        File[] files = mDataLoggerDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(DATA_LOGGER_FILE_EXTENSION)
                        && !name.equals(mDataLoggerFile.getName()); // Skip current file
            }
        });
        if (files != null && files.length > 0) { // Can be null on Android M and above if Storage permission is not granted
            Map<Long, File> fileMap = new TreeMap<>(new Comparator<Long>() { // Sort in descending order: from newest to oldest
                @Override
                public int compare(Long arg0, Long arg1) {
                    return arg0 > arg1 ? -1 : arg0 == arg1 ? 0 : 1;
                }
            });
            for (File file : files) {
                fileMap.put(file.lastModified(), file);
            }
            int count = 0;
            for (Map.Entry<Long, File> en : fileMap.entrySet()) {
                if (++count > 6) { // There should be up to 7 file in the history list. 7 = 6 + current file.
                    en.getValue().delete();
                }
            }
        }
    }

    private static void saveLogData(String message) {
        mDataLoggerFile = new File(mDataLoggerDirectory.getAbsoluteFile() + File.separator + Utils.GetDate() + DATA_LOGGER_FILE_EXTENSION);
        if (!mDataLoggerFile.exists()) {
            try {
                mDataLoggerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        message = Utils.GetTimeandDate() + message;
        try {
            OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(mDataLoggerFile, true),
                    "UTF-8");
            BufferedWriter fbw = new BufferedWriter(writer);
            fbw.write(message);
            fbw.newLine();
            fbw.flush();
            fbw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
