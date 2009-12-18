package com.jefftharris.passwdsafe;

import android.app.Application;
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

    private void closeFileData()
    {
        // TODO: How to close all activities using the file data??
        if (itsFileData != null) {
            itsFileData.close();
            itsFileData = null;
        }
    }
}
