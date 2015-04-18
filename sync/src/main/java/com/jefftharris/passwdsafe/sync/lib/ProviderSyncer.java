/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.jefftharris.passwdsafe.sync.R;

/**
 *  Base attributes and methods for the sync operation for a provider
 */
public abstract class ProviderSyncer<ProviderClientT>
{
    protected final ProviderClientT itsProviderClient;
    protected final DbProvider itsProvider;
    protected final SQLiteDatabase itsDb;
    protected final SyncLogRecord itsLogrec;
    protected final Context itsContext;
    protected final String itsTag;

    /** Constructor */
    public ProviderSyncer(ProviderClientT providerClient,
                          DbProvider provider, SQLiteDatabase db,
                          SyncLogRecord logrec, Context ctx,
                          String tag)
    {
        itsProviderClient = providerClient;
        itsProvider = provider;
        itsDb = db;
        itsLogrec = logrec;
        itsContext = ctx;
        itsTag = tag;
    }


    /** Update a file to appear as a locally added file with a new name */
    protected final DbFile updateFileAsLocallyAdded(DbFile dbfile,
                                                    String titlePrefix)
            throws SQLException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH-mm-ss", Locale.US);
        String newTitle = String.format(Locale.US,
                "%s (%s) - %s",
                titlePrefix,
                dateFormat.format(System.currentTimeMillis()),
                dbfile.itsLocalTitle);
        SyncDb.updateLocalFile(
                dbfile.itsId, dbfile.itsLocalFile, newTitle,
                null, dbfile.itsLocalModDate, itsDb);
        SyncDb.updateLocalFileChange(
                dbfile.itsId, DbFile.FileChange.ADDED, itsDb);
        return SyncDb.getFile(dbfile.itsId, itsDb);
    }


    /** Split the remote information for the file into a new file.  The
     * remote fields for the existing file are reset. */
    protected final DbFile splitRemoteToNewFile(DbFile dbfile)
            throws SQLException
    {
        long newRemoteId = SyncDb.addRemoteFile(
                itsProvider.itsId, dbfile.itsRemoteId,
                dbfile.itsRemoteTitle, dbfile.itsRemoteFolder,
                dbfile.itsRemoteModDate, dbfile.itsRemoteHash, itsDb);
        DbFile newRemfile = SyncDb.getFile(newRemoteId, itsDb);

        resetRemoteFields(dbfile);

        return newRemfile;
    }


    /** Reset the remote fields for a file to their defaults */
    protected final void resetRemoteFields(DbFile dbfile)
            throws SQLException
    {
        SyncDb.updateRemoteFile(
                dbfile.itsId, null, null, null, -1, null, itsDb);
        SyncDb.updateRemoteFileChange(
                dbfile.itsId, DbFile.FileChange.NO_CHANGE, itsDb);
    }


    /** Log a conflicted file */
    protected final void logConflictFile(DbFile dbfile, boolean localName)
    {
        String filename = localName ? dbfile.getLocalTitleAndFolder() :
            dbfile.getRemoteTitleAndFolder();
        itsLogrec.addConflictFile(filename);

        String log = itsContext.getString(
                R.string.sync_conflict_log,
                filename, dbfile.itsLocalChange, dbfile.itsRemoteChange);
        itsLogrec.addEntry(log);
    }
}
