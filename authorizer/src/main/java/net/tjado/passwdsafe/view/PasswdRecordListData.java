/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import java.util.Date;

/**
 * Holder class for password record data in a list view
 */
public class PasswdRecordListData
{
    public final String itsTitle;

    public final String itsUser;

    public final String itsUuid;

    public final Date itsCreationTime;

    public final Date itsModTime;

    public final String itsMatch;

    public final String itsRecordIcon;

    public final int itsAppIcon;

    public final boolean itsIsRecord;

    /** Constructor */
    public PasswdRecordListData(String title, String user, String uuid, Date creationTime, Date modTime,
                                String match, String iconRecord, int iconApp, boolean isRecord)
    {
        itsTitle = title;
        itsUser = user;
        itsUuid = uuid;
        itsCreationTime = creationTime;
        itsModTime = modTime;
        itsMatch = match;
        itsRecordIcon = iconRecord;
        itsAppIcon = iconApp;
        itsIsRecord = isRecord;
    }
}
