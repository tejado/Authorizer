/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.jefftharris.passwdsafe.sync.R;

/**
 * The SyncLogRecord class contains a record in the sync log
 */
public class SyncLogRecord
{
    private String itsAccount;
    private long itsStartTime;
    private List<Exception> itsFailures = new ArrayList<Exception>();
    private long itsEndTime = -1;
    private boolean itsIsFullSync = false;
    private boolean itsIsManualSync;
    private boolean itsIsNotConnected = false;
    private List<String> itsEntries = new ArrayList<String>();

    /** Constructor */
    public SyncLogRecord(String account, String typeName, boolean manual)
    {
        itsAccount = account + " (" + typeName + ")";
        itsStartTime = System.currentTimeMillis();
        itsIsManualSync = manual;
    }

    /** Get the account name */
    public String getAccount()
    {
        return itsAccount;
    }

    /** Get the start time */
    public long getStartTime()
    {
        return itsStartTime;
    }

    /** Add an exception failure */
    public void addFailure(Exception e)
    {
        itsFailures.add(e);
    }

    /** Get the end time for a sync */
    public long getEndTime()
    {
        return itsEndTime;
    }

    /** Set the end time for a sync */
    public void setEndTime()
    {
        itsEndTime = System.currentTimeMillis();
    }

    /** Get whether the sync is full or incremental */
    public boolean isFullSync()
    {
        return itsIsFullSync;
    }

    /** Get whether the sync was started manually */
    public boolean isManualSync()
    {
        return itsIsManualSync;
    }

    /** Set whether the sync is full or incremental */
    public void setFullSync(boolean full)
    {
        itsIsFullSync = full;
    }

    /** Get whether the network was not connected */
    public boolean isNotConnected()
    {
        return itsIsNotConnected;
    }

    /** Set whether the network is not connected */
    public void setNotConnected(boolean notConnected)
    {
        itsIsNotConnected = notConnected;
    }

    /** Add a sync operation entry */
    public void addEntry(String entry)
    {
        itsEntries.add(entry);
    }

    /** Get a string representation of the actions in the record */
    public String getActions()
    {
        StringBuilder actions = new StringBuilder();
        for (String entry: itsEntries) {
            if (actions.length() != 0) {
                actions.append("\n");
            }
            actions.append(entry);
        }
        for (Exception e: itsFailures) {
            if (actions.length() != 0) {
                actions.append("\n");
            }
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            actions.append("FAILURE: ").append(writer.toString());
        }
        return actions.toString();
    }

    /** Get a string representation of the record */
    public String toString(Context ctx)
    {
        StringBuilder str = new StringBuilder();
        str.append(ctx.getString(R.string.sync_log_record, itsAccount,
                                 ctx.getString(itsIsFullSync ?
                                         R.string.full_sync :
                                         R.string.incremental_sync),
                                 ctx.getString(itsIsManualSync ?
                                         R.string.manual :
                                         R.string.automatic),
                                 ctx.getString(itsIsNotConnected ?
                                         R.string.network_not_connected :
                                         R.string.network_connected),
                                 itsStartTime, itsEndTime));
        str.append("\n");
        str.append(getActions());
        return str.toString();
    }
}
