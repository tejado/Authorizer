/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileDataUser;
import com.jefftharris.passwdsafe.file.PasswdFileToken;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.view.PasswdFileDataView;
import com.jefftharris.passwdsafe.view.PasswdLocation;

/**
 * File data fragment for retaining information between runtime configuration
 * changes
 */
public class PasswdSafeFileDataFragment extends Fragment
{
    /** The open password file */
    private static PasswdFileData itsFileData;

    /** The last viewed record UUID */
    private static String itsLastViewedRecord;

    /** The open password file view */
    private final PasswdFileDataView itsFileDataView = new PasswdFileDataView();

    /** One-time check for whether the fragment was newly created */
    private boolean itsIsNew = true;

    private static final String TAG = "PasswdSafeFileDataFragment";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        PasswdSafeUtil.dbginfo(TAG, "onCreate");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // Called when app is being finalized but not when rotated
        PasswdSafeUtil.dbginfo(TAG, "onDestroy");
        setFileData(null, getContext());
    }

    /** One-time check for whether the fragment was created new */
    public boolean checkNew()
    {
        boolean n = itsIsNew;
        itsIsNew = false;
        return n;
    }

    /**
     * Use the password file data.  Only one thread will use the data at a time.
     */
    public void useFileData(PasswdFileDataUser user)
    {
        useOpenFileData(user);
    }

    /** Get the view of the password file data */
    public @NonNull PasswdFileDataView getFileDataView()
    {
        return itsFileDataView;
    }

    /** Set the password file data */
    public void setFileData(PasswdFileData fileData, Context ctx)
    {
        PasswdFileToken token = acquireFileData();
        try {
            if (itsFileData != null) {
                itsFileDataView.clearFileData(ctx);
                itsFileData.close();
            }
            itsFileData = fileData;
            itsLastViewedRecord = null;
            itsFileDataView.setFileData(itsFileData, ctx);
        } finally {
            token.release();
        }
    }

    /** Refresh the password file data */
    public void refreshFileData(Context ctx)
    {
        PasswdFileToken token = acquireFileData();
        try {
            itsFileDataView.setFileData(token.getFileData(), ctx);
        } finally {
            token.release();
        }
    }

    /** Set the location in the file */
    public void setLocation(PasswdLocation location)
    {
        itsFileDataView.setCurrGroups(location.getGroups());
        if (location.isRecord()) {
            itsLastViewedRecord = location.getRecord();
        }
    }

    /**
     * Use the global open password file data
     */
    public static void useOpenFileData(PasswdFileDataUser user)
    {
        PasswdFileToken token = acquireFileData();
        try {
            PasswdFileData fileData = token.getFileData();
            if (fileData != null) {
                user.useFileData(fileData);
            }
        } finally {
            token.release();
        }
    }

    /** Get the last viewed record */
    public static @Nullable String getLastViewedRecord()
    {
        return itsLastViewedRecord;
    }

    /** Acquire the file data token */
    private static @NonNull @CheckResult
    PasswdFileToken acquireFileData()
    {
        return new PasswdFileToken(itsFileData);
    }
}
