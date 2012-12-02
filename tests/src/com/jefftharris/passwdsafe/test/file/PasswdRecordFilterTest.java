/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.test.file;

import java.util.Date;
import java.util.regex.Pattern;

import com.jefftharris.passwdsafe.file.PasswdRecordFilter;

import android.os.Parcel;
import android.test.AndroidTestCase;

/**
 * Tests for the PasswdRecordFilter class
 */
public class PasswdRecordFilterTest extends AndroidTestCase
{
    /** Test parcelling a query filter */
    public void testQueryParcel()
    {
        Pattern pattern = Pattern.compile("foobar", Pattern.CASE_INSENSITIVE);
        PasswdRecordFilter filter =
            new PasswdRecordFilter(pattern, PasswdRecordFilter.OPTS_DEFAULT);

        PasswdRecordFilter filter2 = recreateFilter(filter);
        assertNotSame(filter, filter2);
        assertEquals(filter, filter2);
    }


    /** Test parcelling an expiration filter */
    public void testExpiryParcel()
    {
        PasswdRecordFilter filter =
            new PasswdRecordFilter(PasswdRecordFilter.ExpiryFilter.TODAY, null,
                                   PasswdRecordFilter.OPTS_NO_ALIAS);

        PasswdRecordFilter filter2 = recreateFilter(filter);
        assertNotSame(filter, filter2);
        assertEquals(filter, filter2);
    }


    /** Test parcelling a custom expiration filter */
    public void testExpiryCustomParcel()
    {
        PasswdRecordFilter filter =
            new PasswdRecordFilter(PasswdRecordFilter.ExpiryFilter.CUSTOM,
                                   new Date(),
                                   PasswdRecordFilter.OPTS_NO_SHORTCUT);

        PasswdRecordFilter filter2 = recreateFilter(filter);
        assertNotSame(filter, filter2);
        assertEquals(filter, filter2);
    }


    /** Recreate the filter from a parcel */
    private final PasswdRecordFilter recreateFilter(PasswdRecordFilter filter)
    {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(filter, 0);
        parcel.setDataPosition(0);
        return parcel.readParcelable(getClass().getClassLoader());
    }
}
