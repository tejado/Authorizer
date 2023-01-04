/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.annotation.SuppressLint;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.YubiState;

import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

import java.io.Closeable;
import java.util.Objects;

/**
 * View model for opening a file
 */
public class PasswdSafeOpenFileViewModel extends ViewModel
{
    private static final String TAG = "PasswdSafeOpenFileVM";

    public enum SavedPasswordState
    {
        UNKNOWN,
        NOT_AVAILABLE,
        AVAILABLE,
        LOADED_SUCCESS,
        LOADED_FAILURE
    }

    /**
     * View model data for opening a file
     */
    public static class OpenData implements Closeable
    {
        private boolean itsIsResolved = false;
        private PasswdFileUri itsPasswdFileUri;
        private boolean itsIsSaveAllowed = false;

        private boolean itsHasYubiState = false;
        private YubiState itsYubiState = YubiState.UNKNOWN;
        private int itsYubiSlot = 2;
        private boolean itsIsYubikeySelected = false;

        private int itsRetries = 0;
        private static final int NUM_RETRIES = 5;

        private SavedPasswordState itsSavedPasswordState =
                SavedPasswordState.UNKNOWN;
        private CharSequence itsLoadedPasswordMsg = null;
        private Owner<PwsPassword> itsLoadedPassword;

        private Owner<PwsPassword> itsOpenPassword;
        private boolean itsIsOpenYubikey = false;

        /**
         * Constructor
         */
        private OpenData()
        {
        }

        /**
         * Copy constructor
         */
        private OpenData(@NonNull OpenData data)
        {
            itsIsResolved = data.itsIsResolved;
            itsPasswdFileUri = data.itsPasswdFileUri;
            itsIsSaveAllowed = data.itsIsSaveAllowed;

            itsHasYubiState = data.itsHasYubiState;
            itsYubiState = data.itsYubiState;
            itsYubiSlot = data.itsYubiSlot;
            itsIsYubikeySelected = data.itsIsYubikeySelected;

            itsRetries = data.itsRetries;

            itsSavedPasswordState = data.itsSavedPasswordState;
            itsLoadedPasswordMsg = data.itsLoadedPasswordMsg;
            setLoadedPassword((data.itsLoadedPassword != null) ?
                              data.itsLoadedPassword.pass() : null);

            setOpenPassword((data.itsOpenPassword != null) ?
                            data.itsOpenPassword.pass() : null);
            itsIsOpenYubikey = data.itsIsOpenYubikey;
        }

        /**
         * Finalize the data
         */
        protected void finalize()
        {
            PasswdSafeUtil.dbginfo(TAG, "data finalize");
            close();
        }

        boolean isResolved()
        {
            return itsIsResolved;
        }

        @Nullable
        PasswdFileUri getUri()
        {
            return itsPasswdFileUri;
        }

        boolean isSaveAllowed()
        {
            return itsIsSaveAllowed;
        }

        boolean hasYubiInfo()
        {
            return itsHasYubiState;
        }

        YubiState getYubiState()
        {
            return itsYubiState;
        }

        int getYubiSlot()
        {
            return itsYubiSlot;
        }

        boolean isYubikeySelected()
        {
            return itsIsYubikeySelected;
        }

        boolean hasPasswordRetry()
        {
            return itsRetries > 0;
        }

        SavedPasswordState getSavedPasswordState()
        {
            return itsSavedPasswordState;
        }

        @Nullable
        CharSequence getLoadedPasswordMsg()
        {
            return itsLoadedPasswordMsg;
        }

        @CheckResult @Nullable
        public Owner<PwsPassword> getLoadedPassword()
        {
            return (itsLoadedPassword != null) ?
                   itsLoadedPassword.pass().use() : null;
        }

        @CheckResult @Nullable
        public Owner<PwsPassword> getOpenPassword()
        {
            return (itsOpenPassword != null) ? itsOpenPassword.pass().use() :
                   null;
        }

        public boolean isOpenYubikey()
        {
            return itsIsOpenYubikey;
        }

        @NonNull
        @SuppressLint("DefaultLocale")
        public String toString()
        {
            return String.format(
                    "{\nuri: %s, save allowed: %b, retries: %d" +
                    "\nyubi state: %s, slot: %d, selected: %b" +
                    "\nsaved passwd: %s, loaded passwd: %b, loaded msg: %s"+
                    "\nopen passwd %b, open yubikey %b}",
                    itsPasswdFileUri, itsIsSaveAllowed, itsRetries,
                    itsYubiState, itsYubiSlot, itsIsYubikeySelected,
                    itsSavedPasswordState, (itsLoadedPassword != null),
                    itsLoadedPasswordMsg,
                    (itsOpenPassword != null), itsIsOpenYubikey);
        }

        /**
         * Close the view model data and release resources
         */
        @Override
        public void close()
        {
            setOpenPassword(null);
            setLoadedPassword(null);
        }

        /**
         * Set the open password
         */
        private void setOpenPassword(
                @Nullable Owner<PwsPassword>.Param password)
        {
            if (itsOpenPassword != null) {
                itsOpenPassword.close();
                itsOpenPassword = null;
            }

            if (password != null) {
                itsOpenPassword = password.use();
            }
        }

        /**
         * Set the loaded password
         */
        private void setLoadedPassword(
                @Nullable Owner<PwsPassword>.Param password)
        {
            if (itsLoadedPassword != null) {
                itsLoadedPassword.close();
                itsLoadedPassword = null;
            }

            if (password != null) {
                itsLoadedPassword = password.use();
            }
        }
    }

    private final MutableLiveData<OpenData> itsData;

    /**
     * Constructor
     */
    public PasswdSafeOpenFileViewModel()
    {
        PasswdSafeUtil.dbginfo(TAG, "ctor");
        itsData = new MutableLiveData<>(new OpenData());
    }

    /**
     * Provide the state of the Yubikey
     */
    public void provideYubiInfo(YubiState state, boolean selected)
    {
        OpenData newData = new OpenData(getDataValue());
        newData.itsHasYubiState = true;
        newData.itsYubiState = state;
        newData.itsIsYubikeySelected = selected;
        setDataValue(newData);
    }

    /**
     * Provide the result of resolving the file URI
     */
    public void provideResolveResults(PasswdFileUri uri, boolean saveAllowed)
    {
        OpenData newData = new OpenData(getDataValue());
        newData.itsIsResolved = true;
        newData.itsPasswdFileUri = uri;
        newData.itsIsSaveAllowed = saveAllowed;
        setDataValue(newData);
    }

    /**
     * Set whether the Yubikey is selected
     */
    public void setYubiSelected(boolean selected)
    {
        OpenData newData = new OpenData(getDataValue());
        newData.itsIsYubikeySelected = selected;
        setDataValue(newData);
    }

    /**
     * Set the slot used on the Yubikey
     */
    public void setYubiSlot(int slot)
    {
        OpenData newData = new OpenData(getDataValue());
        newData.itsYubiSlot = slot;
        setDataValue(newData);
    }

    /**
     * Check if there are more retries allowed for opening a file
     */
    public boolean checkOpenRetries()
    {
        OpenData data = getDataValue();
        if (data.itsRetries < OpenData.NUM_RETRIES) {
            OpenData newData = new OpenData(data);
            newData.itsRetries++;
            setDataValue(newData);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set the saved password state
     */
    public void setSavedPasswordState(
            SavedPasswordState state,
            @Nullable CharSequence loadedMsg,
            @Nullable Owner<PwsPassword>.Param loadedPassword)
    {
        var newData = new OpenData(getDataValue());
        newData.itsSavedPasswordState = state;
        newData.itsLoadedPasswordMsg = loadedMsg;
        newData.setLoadedPassword(loadedPassword);
        setDataValue(newData);
    }

    /**
     * Set the password to use during a file open
     */
    public void setOpenPassword(@Nullable Owner<PwsPassword>.Param password,
                                boolean fromYubikey)
    {
        var newData = new OpenData(getDataValue());
        newData.setOpenPassword(password);
        newData.itsIsOpenYubikey = fromYubikey;
        setDataValue(newData);
    }

    /**
     * Get the live open data
     */
    public LiveData<OpenData> getData()
    {
        return itsData;
    }

    /**
     * Get the current value of the open data
     */
    public @NonNull
    OpenData getDataValue()
    {
        return Objects.requireNonNull(itsData.getValue());
    }

    /**
     * Reset the open data
     */
    public void resetData()
    {
        setDataValue(new OpenData());
    }

    /**
     * Set a new value for the open data
     */
    private void setDataValue(@NonNull OpenData newData)
    {
        var data = itsData.getValue();
        if (data != null) {
            data.close();
        }
        itsData.setValue(newData);
    }

    @Override
    protected void onCleared()
    {
        PasswdSafeUtil.dbginfo(TAG, "onCleared");
        getDataValue().close();
    }
}
