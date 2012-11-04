/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.util;

import java.util.Date;

import android.content.Context;
import android.text.format.DateUtils;

/**
 * The Utils class provides general text manipulation utilities
 */
public final class Utils
{
    /// Format a date according to the current locale settings
    public static String formatDate(Date date, Context ctx)
    {
        return DateUtils.formatDateTime(ctx, date.getTime(),
                                        DateUtils.FORMAT_SHOW_TIME |
                                        DateUtils.FORMAT_SHOW_DATE |
                                        DateUtils.FORMAT_SHOW_YEAR);
    }
}
