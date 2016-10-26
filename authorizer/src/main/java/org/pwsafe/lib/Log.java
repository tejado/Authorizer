/*
 * $Id: Log.java 411 2009-09-25 18:19:34Z roxon $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib;

/**
 * This class provides logging facilities using commons logging.
 *
 * @author Kevin Preece
 */
public class Log
{
    private String itsTag = "Log";

    private Log(String name)
    {
        itsTag = name;
    }

    /**
     * Returns an instance of <code>Log</code> for the logger named
     * <code>name</code>.
     *
     * @param name the logger name.
     * @return An <code>Log</code> instance.
     */
    public static Log getInstance(String name)
    {
        return new Log(name);
    }

    /**
     * Writes a message at error level
     *
     * @param msg the message to issue.
     */
    public final void error(String msg)
    {
        android.util.Log.e(itsTag, msg);
    }

    /**
     * Writes a message at error level along with details of the exception
     *
     * @param msg    the message to issue.
     * @param except the exception to be logged.
     */
    public final void error(String msg, Throwable except)
    {
        android.util.Log.e(itsTag, msg, except);
    }

    /**
     * Logs a message at the warning level.
     *
     * @param msg the message to issue.
     */
    public final void warn(String msg)
    {
        android.util.Log.w(itsTag, msg);
    }

}
