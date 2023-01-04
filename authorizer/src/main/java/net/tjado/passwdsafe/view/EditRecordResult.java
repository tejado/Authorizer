/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

/**
 * Results of editing a record
 */
public class EditRecordResult
{
    public final boolean itsIsNewRecord;
    public final boolean itsIsSave;
    public final PasswdLocation itsNewLocation;

    /**
     * Constructor
     */
    public EditRecordResult(boolean newRecord,
                            boolean save,
                            PasswdLocation newLocation)
    {
        itsIsNewRecord = newRecord;
        itsIsSave = save;
        itsNewLocation = newLocation;
    }
}
