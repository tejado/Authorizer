/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import android.content.Context;
import android.text.format.DateUtils;

/**
 * The Utils class provides general utilities
 */
public final class Utils
{
    /** Format a date according to the current locale settings */
    public static String formatDate(Date date, Context ctx)
    {
        return formatDate(date.getTime(), ctx);
    }

    /** Format a date according to the current locale settings */
    public static String formatDate(long date, Context ctx)
    {
        return formatDate(date, ctx, true, true, false);
    }

    /**
     * Format a time and/or date in milliseconds according to the current locale
     * settings
     */
    public static String formatDate(long date, Context ctx,
                                    boolean showTime, boolean showDate,
                                    boolean abbrev)
    {
        int flags = 0;
        if (showTime) {
            flags |= DateUtils.FORMAT_SHOW_TIME;
            if (abbrev) {
                flags |= DateUtils.FORMAT_ABBREV_TIME;
            }
        }
        if (showDate) {
            flags |= DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            if (abbrev) {
                flags |= DateUtils.FORMAT_ABBREV_ALL;
            }
        }
        return DateUtils.formatDateTime(ctx, date, flags);
    }

    /**
     * Format a time and/or date in milliseconds in an uri safe format
     */
    public static String formatDateUriSafe(Date date)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        return dateFormat.format(date);
    }

    /** Copy the input stream to the output */
    public static void copyStream(InputStream is, OutputStream os)
            throws IOException
    {
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
    }


    /** Close the streams */
    public static void closeStreams(InputStream is, OutputStream os)
            throws IOException
    {
        try {
            if (is != null) {
                is.close();
            }
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }
}
