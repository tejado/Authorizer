/*
 * Copyright (©) 2009-2010 Jeff Harris <jeffharris@users.sourceforge.net>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.util.Log;

public class PasswdSafeApp extends Application
{
    private PasswdFileData itsFileData = null;

    private static final String TAG = "PasswdSafeApp";

    public PasswdSafeApp()
    {
    }

    /* (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate()
    {
        closeFileData();
        super.onTerminate();
    }

    public PasswdFileData getFileData(String fileName)
    {
        if ((itsFileData == null) || (itsFileData.itsFileName == null) ||
            (!itsFileData.itsFileName.equals(fileName))) {
            closeFileData();
        }
        Log.i(TAG, "getFileData fileName:" + fileName +
              ", data:" + itsFileData);
        return itsFileData;
    }

    public void setFileData(PasswdFileData fileData)
    {
        closeFileData();
        itsFileData = fileData;
    }

    public static void showFatalMsg(String msg, final Activity activity)
    {
        new AlertDialog.Builder(activity)
        .setMessage(msg)
        .setCancelable(false)
        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        })
        .show();
    }

    private void closeFileData()
    {
        // TODO: How to close all activities using the file data??
        if (itsFileData != null) {
            itsFileData.close();
            itsFileData = null;
        }
    }
}
