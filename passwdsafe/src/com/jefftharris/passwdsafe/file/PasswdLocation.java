/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.util.ArrayList;

import android.text.TextUtils;

/**
 * The PasswdLocation class encapsulates a location within a password file
 */
public class PasswdLocation
{
    private final ArrayList<String> itsGroups = new ArrayList<String>();
    private final String itsRecord;

    /** Default constructor */
    public PasswdLocation()
    {
        this(null, null);
    }

    /** Constructor with groups */
    public PasswdLocation(ArrayList<String> groups)
    {
        this(groups, null);
    }

    /** Constructor with groups and a specific record */
    public PasswdLocation(ArrayList<String> groups, String record)
    {
        if (groups != null) {
            itsGroups.addAll(groups);
        }
        itsRecord = record;
    }

    /** Get the location's groups */
    public ArrayList<String> getGroups()
    {
        return itsGroups;
    }

    /** Get the path string for the groups */
    public String getGroupPath()
    {
        return TextUtils.join(" / ", itsGroups);
    }

    /** Get the location's specific record; null for no record */
    public String getRecord()
    {
        return itsRecord;
    }

    /** Get a new location with a selected record */
    public PasswdLocation selectRecord(String record)
    {
        return new PasswdLocation(itsGroups, record);
    }

    /** Get a new location with a child group selected */
    public PasswdLocation selectGroup(String group)
    {
        PasswdLocation loc = new PasswdLocation(itsGroups, null);
        loc.itsGroups.add(group);
        return loc;
    }

    /** Get a new location with one group popped from the list */
    public PasswdLocation popGroup()
    {
        PasswdLocation loc = new PasswdLocation(itsGroups, null);
        if (!loc.itsGroups.isEmpty()) {
            loc.itsGroups.remove(loc.itsGroups.size() - 1);
        }
        return loc;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof PasswdLocation)) {
            return false;
        }
        PasswdLocation location = (PasswdLocation)o;
        if (!itsGroups.equals(location.itsGroups)) {
            return false;
        }
        return TextUtils.equals(itsRecord, location.itsRecord);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return String.format("{rec: %s, groups: %s}",
                             itsRecord, getGroupPath());
    }
}
