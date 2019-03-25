/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.file.PasswdFileToken;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.view.PasswdFileDataView;
import net.tjado.passwdsafe.view.PasswdLocation;

/**
 * File data fragment for retaining information between runtime configuration
 * changes
 */
public class PasswdSafeFileDataFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    /** The open password file */
    private static PasswdFileData itsFileData;

    /** The last viewed record UUID */
    private static String itsLastViewedRecord;

    /** The open password file view */
    private final PasswdFileDataView itsFileDataView = new PasswdFileDataView();

    /** One-time check for whether the fragment was newly created */
    private boolean itsIsNew = true;

    private boolean itsIsCloseClearClipboard =
            Preferences.PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF;

    private static final String TAG = "AuthorizerFileDataFragment";

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        SharedPreferences prefs = Preferences.getSharedPrefs(ctx);
        prefs.registerOnSharedPreferenceChangeListener(this);
        itsIsCloseClearClipboard =
                Preferences.getFileCloseClearClipboardPref(prefs);
        itsFileDataView.onAttach(ctx, prefs);
    }

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
        setFileData(null);
    }

    @Override
    public void onDetach()
    {
        itsFileDataView.onDetach();
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDetach();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        switch (key) {
        case Preferences.PREF_FILE_CLOSE_CLEAR_CLIPBOARD: {
            itsIsCloseClearClipboard =
                    Preferences.getFileCloseClearClipboardPref(prefs);
            break;
        }
        }
        if (itsFileDataView.handleSharedPreferenceChanged(prefs, key)) {
            refreshFileData();
        }
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
    public void setFileData(PasswdFileData fileData)
    {
        PasswdFileToken token = acquireFileData();
        try {
            if (itsFileData != null) {
                itsFileDataView.clearFileData();
                itsFileData.close();
                if (itsIsCloseClearClipboard) {
                    PasswdSafeUtil.copyToClipboard("", getContext());
                }
            }
            itsFileData = fileData;
            itsLastViewedRecord = null;
            itsFileDataView.setFileData(itsFileData);
        } finally {
            token.release();
        }
    }

    /** Refresh the password file data */
    public void refreshFileData()
    {
        PasswdFileToken token = acquireFileData();
        try {
            itsFileDataView.refreshFileData(token.getFileData());
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
