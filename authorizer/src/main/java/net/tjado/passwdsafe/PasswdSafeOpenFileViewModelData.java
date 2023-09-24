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

import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.SavedPasswordState;
import net.tjado.passwdsafe.util.YubiState;

import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

import java.io.Closeable;

/**
 * View model data for opening a file
 */
public class PasswdSafeOpenFileViewModelData implements Closeable
{
    public static final int NUM_RETRIES = 5;

    private boolean itsIsResolved = false;
    private PasswdFileUri itsPasswdFileUri;
    private boolean itsIsSaveAllowed = false;

    private boolean itsHasYubiState = false;
    private YubiState itsYubiState = YubiState.UNKNOWN;
    private int itsYubiSlot = 2;
    private boolean itsIsYubikeySelected = false;
    private Throwable itsYubikeyError = null;

    private int itsRetries = 0;

    private SavedPasswordState itsSavedPasswordState =
            SavedPasswordState.UNKNOWN;
    private CharSequence itsLoadedPasswordMsg = null;
    private Owner<PwsPassword> itsLoadedPassword;

    private Owner<PwsPassword> itsOpenPassword;
    private boolean itsIsOpenYubikey = false;

    private static final String TAG = "PasswdSafeOpenFileVMData";

    /**
     * Constructor
     */
    public PasswdSafeOpenFileViewModelData()
    {
    }

    /**
     * Copy constructor
     */
    private PasswdSafeOpenFileViewModelData(
            @NonNull PasswdSafeOpenFileViewModelData data)
    {
        itsIsResolved = data.itsIsResolved;
        itsPasswdFileUri = data.itsPasswdFileUri;
        itsIsSaveAllowed = data.itsIsSaveAllowed;

        itsHasYubiState = data.itsHasYubiState;
        itsYubiState = data.itsYubiState;
        itsYubiSlot = data.itsYubiSlot;
        itsIsYubikeySelected = data.itsIsYubikeySelected;
        itsYubikeyError = data.itsYubikeyError;

        itsRetries = data.itsRetries;

        itsSavedPasswordState = data.itsSavedPasswordState;
        itsLoadedPasswordMsg = data.itsLoadedPasswordMsg;
        setLoadedPassword((data.itsLoadedPassword != null) ?
                data.itsLoadedPassword.pass() : null);

        setOpenPassword(
                (data.itsOpenPassword != null) ? data.itsOpenPassword.pass() :
                        null);
        itsIsOpenYubikey = data.itsIsOpenYubikey;
    }

    /**
     * Clone with Yubikey state
     */
    public PasswdSafeOpenFileViewModelData cloneWithYubiInfo(
            YubiState yubiState,
            boolean yubikeySelected)
    {
        var newData = new PasswdSafeOpenFileViewModelData(this);
        newData.itsHasYubiState = true;
        newData.itsYubiState = yubiState;
        newData.itsIsYubikeySelected = yubikeySelected;
        return newData;
    }

    /**
     * Clone with file URI resolving info
     */
    public PasswdSafeOpenFileViewModelData cloneWithResolveResults(
            PasswdFileUri uri,
            boolean saveAllowed)
    {
        var newData = new PasswdSafeOpenFileViewModelData(this);
        newData.itsIsResolved = true;
        newData.itsPasswdFileUri = uri;
        newData.itsIsSaveAllowed = saveAllowed;
        return newData;
    }

    /**
     * Clone with Yubikey selection
     */
    public PasswdSafeOpenFileViewModelData cloneWithYubikeySelection(
            boolean selected)
    {
        var newData = new PasswdSafeOpenFileViewModelData(this);
        newData.itsIsYubikeySelected = selected;
        return newData;
    }

    /**
     * Clone with Yubikey slot
     */
    public PasswdSafeOpenFileViewModelData cloneWithYubikeySlot(int slot)
    {
        var newData = new PasswdSafeOpenFileViewModelData(this);
        newData.itsYubiSlot = slot;
        return newData;
    }

    /**
     * Clone with Yubikey error
     */
    public PasswdSafeOpenFileViewModelData cloneWithYubikeyError(
            Throwable error)
    {
        var newData = new PasswdSafeOpenFileViewModelData(this);
        newData.itsYubikeyError = error;
        return newData;
    }

    /**
     * Clone with password retries
     */
    public PasswdSafeOpenFileViewModelData cloneWithPasswordRetries(int retries)
    {
        var newData = new PasswdSafeOpenFileViewModelData(this);
        newData.itsRetries = retries;
        return newData;
    }

    /**
     * Clone with saved password state
     */
    public PasswdSafeOpenFileViewModelData cloneWithSavedPasswordState(
            SavedPasswordState state,
            @Nullable CharSequence loadedMsg,
            @Nullable Owner<PwsPassword>.Param loadedPassword)
    {
        var newData = new PasswdSafeOpenFileViewModelData(this);
        newData.itsSavedPasswordState = state;
        newData.itsLoadedPasswordMsg = loadedMsg;
        newData.setLoadedPassword(loadedPassword);
        return newData;
    }

    /**
     * Clone with the password to use during a file open
     */
    public PasswdSafeOpenFileViewModelData cloneWithOpenPassword(
            @Nullable Owner<PwsPassword>.Param password,
            boolean fromYubikey)
    {
        var newData = new PasswdSafeOpenFileViewModelData(this);
        newData.setOpenPassword(password);
        newData.itsIsOpenYubikey = fromYubikey;
        return newData;
    }

    public boolean isResolved()
    {
        return itsIsResolved;
    }

    @Nullable
    public PasswdFileUri getUri()
    {
        return itsPasswdFileUri;
    }

    public boolean isSaveAllowed()
    {
        return itsIsSaveAllowed;
    }

    public boolean hasYubiInfo()
    {
        return itsHasYubiState;
    }

    @NonNull
    public YubiState getYubiState()
    {
        return itsYubiState;
    }

    public int getYubiSlot()
    {
        return itsYubiSlot;
    }

    public boolean isYubikeySelected()
    {
        return itsIsYubikeySelected;
    }

    public Throwable getYubikeyError()
    {
        return itsYubikeyError;
    }

    public boolean hasPasswordRetry()
    {
        return itsRetries > 0;
    }

    public int getPasswordRetries()
    {
        return itsRetries;
    }

    public SavedPasswordState getSavedPasswordState()
    {
        return itsSavedPasswordState;
    }

    @Nullable
    public CharSequence getLoadedPasswordMsg()
    {
        return itsLoadedPasswordMsg;
    }

    @CheckResult
    @Nullable
    public Owner<PwsPassword> getLoadedPassword()
    {
        return (itsLoadedPassword != null) ? itsLoadedPassword.pass().use() :
                null;
    }

    @CheckResult
    @Nullable
    public Owner<PwsPassword> getOpenPassword()
    {
        return (itsOpenPassword != null) ? itsOpenPassword.pass().use() : null;
    }

    public boolean isOpenYubikey()
    {
        return itsIsOpenYubikey;
    }

    @NonNull
    @SuppressLint("DefaultLocale")
    public String toString()
    {
        return String.format("{\nuri: %s, save allowed: %b, retries: %d" +
                        "\nyubi state: %s, slot: %d, selected: %b, " +
                        "error: %s" +
                        "\nsaved passwd: %s, loaded passwd: %b, loaded " +
                        "msg: %s" +
                        "\nopen passwd %b, open yubikey %b}",
                itsPasswdFileUri, itsIsSaveAllowed, itsRetries,
                itsYubiState, itsYubiSlot, itsIsYubikeySelected,
                itsYubikeyError, itsSavedPasswordState,
                (itsLoadedPassword != null), itsLoadedPasswordMsg,
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
     * Finalize the data
     */
    protected void finalize()
    {
        PasswdSafeUtil.dbginfo(TAG, "data finalize");
        close();
    }

    /**
     * Set the open password
     */
    private void setOpenPassword(@Nullable Owner<PwsPassword>.Param password)
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
    private void setLoadedPassword(@Nullable Owner<PwsPassword>.Param password)
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
