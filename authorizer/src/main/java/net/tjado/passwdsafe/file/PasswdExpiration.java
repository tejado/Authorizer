/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import java.util.Date;


/**
 * The PasswdExpiration class contains the password expiration options for a
 * record
 */
public class PasswdExpiration
{
    /** Expiration type */
    public enum Type
    {
        NEVER       (0),
        DATE        (1),
        INTERVAL    (2);

        /** Constructor */
        Type(int strIdx)
        {
            itsStrIdx = strIdx;
        }

        /** Index of the type in the string array of choices */
        public final int itsStrIdx;

        /** Get the type from the string index */
        public static Type fromStrIdx(int idx)
        {
            for (PasswdExpiration.Type t: values()) {
                if (idx == t.itsStrIdx) {
                    return t;
                }
            }
            return NEVER;
        }
    }


    public static final int VALID_INTERVAL_MIN = 1;
    public static final int VALID_INTERVAL_MAX = 3650;
    public static final int INTERVAL_DEFAULT = 30;

    public final Date itsExpiration;
    public final int itsInterval;
    public final boolean itsIsRecurring;

    /** Constructor */
    public PasswdExpiration(Date date, int interval, boolean recurring)
    {
        itsExpiration = date;
        itsInterval = interval;
        itsIsRecurring = recurring;
    }

    /**
     * Are two expirations equal
     */
    public static boolean isEqual(PasswdExpiration exp1, PasswdExpiration exp2)
    {
        if (((exp1 == null) && (exp2 != null)) ||
            ((exp1 != null) && (exp2 == null))) {
            return false;
        }

        if (exp1 == null) {
            return true;
        }

        //noinspection SimplifiableIfStatement
        if ((exp1.itsExpiration == null) || (exp2.itsExpiration == null)) {
            return false;
        }

        return exp1.itsExpiration.equals(exp2.itsExpiration) &&
               (exp1.itsInterval == exp2.itsInterval) &&
               (exp1.itsIsRecurring == exp2.itsIsRecurring);

    }
}
