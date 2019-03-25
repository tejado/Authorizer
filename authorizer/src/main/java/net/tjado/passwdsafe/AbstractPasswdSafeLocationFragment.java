/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.file.PasswdRecord;
import net.tjado.passwdsafe.view.PasswdLocation;

import org.pwsafe.lib.file.PwsRecord;

/**
 * Base fragment for accessing password file data at a location
 */
public abstract class AbstractPasswdSafeLocationFragment
        <ListenerT extends AbstractPasswdSafeFileDataFragment.Listener>
        extends AbstractPasswdSafeFileDataFragment<ListenerT>
{
    /**
     * Wrapper class for record information
     */
    protected static class RecordInfo
    {
        public final PwsRecord itsRec;
        public final PasswdRecord itsPasswdRec;
        public final PasswdFileData itsFileData;

        /**
         * Constructor
         */
        public RecordInfo(@NonNull PwsRecord rec,
                          @NonNull PasswdRecord passwdRec,
                          @NonNull PasswdFileData fileData)
        {
            itsRec = rec;
            itsPasswdRec = passwdRec;
            itsFileData = fileData;
        }
    }

    /**
     * Interface for users of a file data record
     */
    protected interface RecordInfoUser
    {
        /**
         * Callback to use the file data record
         */
        void useRecordInfo(@NonNull RecordInfo info);
    }

    /**
     * Interfaces for users of file data with an optional record
     */
    protected interface RecordFileUser
    {
        /**
         * Callback to use the file data and record
         */
        void useFile(@Nullable RecordInfo info,
                     @NonNull PasswdFileData fileData);
    }

    private PasswdLocation itsLocation;

    /**
     * Create arguments for new instance
     */
    protected static Bundle createArgs(PasswdLocation location)
    {
        Bundle args = new Bundle();
        args.putParcelable("location", location);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            itsLocation = args.getParcelable("location");
        } else {
            itsLocation = new PasswdLocation();
        }
    }

    /**
     * Get the password location
     */
    protected final PasswdLocation getLocation()
    {
        return itsLocation;
    }

    /**
     * Use the file data record at the current location
     */
    protected final void useRecordInfo(final RecordInfoUser user)
    {
        useRecordFile(new RecordFileUser()
        {
            @Override
            public void useFile(@Nullable RecordInfo info,
                                @NonNull PasswdFileData fileData)
            {
                if (info != null) {
                    user.useRecordInfo(info);
                }
            }
        });
    }

    /**
     * Use the file data with an optional record at the current location
     */
    protected final void useRecordFile(final RecordFileUser user)
    {
        useFileData(new PasswdFileDataUser()
        {
            @Override
            public void useFileData(@NonNull PasswdFileData fileData)
            {
                PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
                if (rec == null) {
                    user.useFile(null, fileData);
                    return;
                }
                PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
                if (passwdRec == null) {
                    user.useFile(null, fileData);
                    return;
                }

                user.useFile(new RecordInfo(rec, passwdRec, fileData),
                             fileData);
            }
        });
    }
}
