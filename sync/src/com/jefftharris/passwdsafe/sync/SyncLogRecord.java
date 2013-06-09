/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

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
    private List<String> itsEntries = new ArrayList<String>();

    /** Constructor */
    public SyncLogRecord(String account)
    {
        itsAccount = account;
        itsStartTime = System.currentTimeMillis();
    }

    /** Add an exception failure */
    public void addFailure(Exception e)
    {
        itsFailures.add(e);
    }

    /** Set the end time for a sync */
    public void setEndTime()
    {
        itsEndTime = System.currentTimeMillis();
    }

    /** Set whether the sync is full or incremental */
    public void setFullSync(boolean full)
    {
        itsIsFullSync = full;
    }

    /** Add a sync operation entry */
    public void addEntry(String entry)
    {
        itsEntries.add(entry);
    }

    /** Get a string representation of the record */
    public String toString(Context ctx)
    {
        StringBuilder str = new StringBuilder();
        str.append(ctx.getString(R.string.sync_log_record, itsAccount,
                                 ctx.getString(itsIsFullSync ?
                                         R.string.full : R.string.incremental),
                                 itsStartTime, itsEndTime));
        for (String entry: itsEntries) {
            str.append("\n").append(entry);
        }
        for (Exception e: itsFailures) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            str.append("\nFAILURE: ").append(writer.toString());
        }
        return str.toString();
    }
}
