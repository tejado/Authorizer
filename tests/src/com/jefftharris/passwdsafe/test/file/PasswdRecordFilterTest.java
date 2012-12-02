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
import com.jefftharris.passwdsafe.util.Utils;

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
        doParcelTest(filter, "foobar");
    }


    /** Test parcelling an expiration filter */
    public void testExpiryParcel()
    {
        PasswdRecordFilter filter =
            new PasswdRecordFilter(PasswdRecordFilter.ExpiryFilter.TODAY, null,
                                   PasswdRecordFilter.OPTS_NO_ALIAS);
        doParcelTest(filter, "Password expires today");
    }


    /** Test parcelling a custom expiration filter */
    public void testExpiryCustomParcel()
    {
        Date now = new Date();
        PasswdRecordFilter filter =
            new PasswdRecordFilter(PasswdRecordFilter.ExpiryFilter.CUSTOM,
                                   now,
                                   PasswdRecordFilter.OPTS_NO_SHORTCUT);
        doParcelTest(filter, "Password expires before " +
                                 Utils.formatDate(now.getTime(), getContext(),
                                                  true, true));
    }


    /** Test a parceled filter */
    private final void doParcelTest(PasswdRecordFilter filter,
                                    String expectedToString)
    {
        PasswdRecordFilter filter2 = recreateFilter(filter);
        assertNotSame(filter, filter2);
        assertEquals(filter, filter2);

        assertEquals(expectedToString, filter.toString(getContext()));
        assertEquals(expectedToString, filter2.toString(getContext()));
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
