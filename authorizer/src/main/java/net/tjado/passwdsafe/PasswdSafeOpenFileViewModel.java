/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.SavedPasswordState;
import net.tjado.passwdsafe.util.YubiState;

import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

import java.util.Objects;

/**
 * View model for opening a file
 */
public class PasswdSafeOpenFileViewModel extends ViewModel
{
    private static final String TAG = "PasswdSafeOpenFileVM";

    
    private final MutableLiveData<PasswdSafeOpenFileViewModelData> itsData;

    /**
     * Constructor
     */
    public PasswdSafeOpenFileViewModel()
    {
        PasswdSafeUtil.dbginfo(TAG, "ctor");
        itsData = new MutableLiveData<>(new PasswdSafeOpenFileViewModelData());
    }

    /**
     * Provide the state of the Yubikey
     */
    public void provideYubiInfo(YubiState state, boolean selected)
    {
        setDataValue(getDataValue().cloneWithYubiInfo(state, selected));
    }

    /**
     * Provide the result of resolving the file URI
     */
    public void provideResolveResults(PasswdFileUri uri, boolean saveAllowed)
    {
        setDataValue(getDataValue().cloneWithResolveResults(uri, saveAllowed));
    }

    /**
     * Set whether the Yubikey is selected
     */
    public void setYubiSelected(boolean selected)
    {
        setDataValue(getDataValue().cloneWithYubikeySelection(selected));
    }

    /**
     * Set the slot used on the Yubikey
     */
    public void setYubiSlot(int slot)
    {
        setDataValue(getDataValue().cloneWithYubikeySlot(slot));
    }

    /**
     * Set an error using the YubiKey
     */
    public void setYubikeyError(Throwable error)
    {
        var currData = getDataValue();
        if (!Objects.equals(error, currData.getYubikeyError())) {
            setDataValue(currData.cloneWithYubikeyError(error));
        }
    }

    /**
     * Check if there are more retries allowed for opening a file
     */
    public boolean checkOpenRetries()
    {
        var currData = getDataValue();
        int retries = currData.getPasswordRetries();
        if (retries < PasswdSafeOpenFileViewModelData.NUM_RETRIES) {
            setDataValue(currData.cloneWithPasswordRetries(retries + 1));
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
        setDataValue(getDataValue().cloneWithSavedPasswordState(state, loadedMsg, loadedPassword));
    }

    /**
     * Set the password to use during a file open
     */
    public void setOpenPassword(@Nullable Owner<PwsPassword>.Param password,
                                boolean fromYubikey)
    {
        setDataValue(getDataValue().cloneWithOpenPassword(password, fromYubikey));
    }

    /**
     * Get the live open data
     */
    public LiveData<PasswdSafeOpenFileViewModelData> getData()
    {
        return itsData;
    }

    /**
     * Get the current value of the open data
     */
    public @NonNull PasswdSafeOpenFileViewModelData getDataValue()
    {
        return Objects.requireNonNull(itsData.getValue());
    }

    /**
     * Reset the open data
     */
    public void resetData()
    {
        setDataValue(new PasswdSafeOpenFileViewModelData());
    }

    /**
     * Set a new value for the open data
     */
    private void setDataValue(@NonNull PasswdSafeOpenFileViewModelData newData)
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
