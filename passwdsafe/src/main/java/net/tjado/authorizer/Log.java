/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.authorizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Log {

    // instance object (singleton)
    private static final Log INSTANCE = new Log();

    // private constructor -> singleton
    private Log() {/************** nothing in constructor **************/}

    public static Log getInstance() {
        return INSTANCE;
    }

    public class LogEntry {
        public String time;
        public Level level;
        public String message;
    }

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    public enum Level { Debug };

    private List<LogEntry> logEntries = new ArrayList<LogEntry>();


    public List<LogEntry> getLogs() {
        return logEntries;
    }

    public String getLogsString() {
        return getLogsString(false);
    }

    public String getLogsString(boolean trim) {

        StringBuilder builder = new StringBuilder("");
        for (LogEntry entry : logEntries) {

            builder.append(String.format("%s [%s] %s%n", (trim) ? entry.time.substring(10) : entry.time, String
                    .valueOf(entry.level).toUpperCase(), entry.message) );
        }

        return builder.toString();
    }

    // debug
    public void debug(String msg) {

        this._(Level.Debug, msg);
    }

    public void _(Level level, String msg) {

        LogEntry entry = new LogEntry();
        entry.time = dateFormat.format(new Date());
        entry.level = level;
        entry.message = ToolBox.formatString(msg);

        logEntries.add( entry );
    }

}
