/*
 * Copyright (©) 2011-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PasswdHistory
{
    public static class Entry implements Comparable<Entry>
    {
        private final Date itsDate;
        private final String itsPasswd;

        public Entry(Date date, String passwd)
        {
            itsDate = date;
            itsPasswd = passwd;
        }

        public Date getDate()
        {
            return itsDate;
        }

        public String getPasswd()
        {
            return itsPasswd;
        }

        public int compareTo(Entry arg0)
        {
            // Sort descending
            return -itsDate.compareTo(arg0.itsDate);
        }

        @Override
        public String toString()
        {
            StringBuilder str = new StringBuilder(itsPasswd);
            str.append(" [").append(itsDate).append("]");
            return str.toString();
        }
    }

    public static final int MAX_SIZE_MIN = 0;
    public static final int MAX_SIZE_MAX = 255;

    private boolean itsIsEnabled;
    private int itsMaxSize;
    // Sorted with newest entry first
    private List<Entry> itsPasswds = new ArrayList<Entry>();

    public PasswdHistory(String historyStr)
        throws IllegalArgumentException
    {
        int historyLen = historyStr.length();
        if (historyLen < 5) {
            throw new IllegalArgumentException(
                "Field length (" + historyLen + ") too short: " + 5);
        }

        itsIsEnabled = historyStr.charAt(0) != '0';
        itsMaxSize = Integer.parseInt(historyStr.substring(1, 3), 16);
        if (itsMaxSize > 255) {
            throw new IllegalArgumentException(
                "Invalid max size: " + itsMaxSize);
        }

        int numEntries = Integer.parseInt(historyStr.substring(3, 5), 16);
        if (numEntries > 255) {
            throw new IllegalArgumentException(
                "Invalid numEntries: " + numEntries);
        }

        int pos = 5;
        while (pos < historyLen) {
            if (pos + 12 >= historyLen) {
                throw new IllegalArgumentException(
                    "Field length (" + historyLen + ") too short: " +
                    (pos + 12));
            }

            long date = Long.parseLong(historyStr.substring(pos, pos + 8), 16);
            int passwdLen =
                Integer.parseInt(historyStr.substring(pos + 8, pos + 12), 16);
            pos += 12;

            if (pos + passwdLen > historyLen) {
                throw new IllegalArgumentException(
                    "Field length (" + historyLen + ") too short: " +
                    (pos + passwdLen));
            }

            String passwd = historyStr.substring(pos, pos + passwdLen);
            itsPasswds.add(new Entry(new Date(date * 1000L), passwd));
            pos += passwdLen;
        }
        Collections.sort(itsPasswds);
    }

    public PasswdHistory()
    {
        itsIsEnabled = true;
        itsMaxSize = 5;
    }

    public boolean isEnabled()
    {
        return itsIsEnabled;
    }

    public void setEnabled(boolean enabled)
    {
        itsIsEnabled = enabled;
    }

    public int getMaxSize()
    {
        return itsMaxSize;
    }

    public void setMaxSize(int maxSize)
    {
        if (maxSize < 0) {
            return;
        }
        itsMaxSize = maxSize;
    }

    public void adjustEntriesToMaxSize()
    {
        while (itsMaxSize < itsPasswds.size()) {
            // Remove oldest
            itsPasswds.remove(itsPasswds.size() - 1);
        }
    }

    public List<Entry> getPasswds()
    {
        return itsPasswds;
    }

    public void addPasswd(String passwd)
    {
        if (itsIsEnabled && (itsMaxSize > 0)) {
            if (itsPasswds.size() == itsMaxSize) {
                // Remove oldest
                itsPasswds.remove(itsPasswds.size() - 1);
            }
            itsPasswds.add(new Entry(new Date(), passwd));
            Collections.sort(itsPasswds);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder strbld = new StringBuilder();
        String str = String.format(Locale.US, "%1d%02x%02x",
                                   isEnabled() ? 1 : 0,
                                   itsMaxSize, itsPasswds.size());
        strbld.append(str);

        for (Entry entry : itsPasswds) {
            String passwd = entry.getPasswd();
            str = String.format(Locale.US, "%08x%04x",
                                (int)(entry.getDate().getTime() / 1000),
                                passwd.length());
            strbld.append(str);
            strbld.append(passwd);
        }

        return strbld.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o)
    {
        if (super.equals(o)) {
            return true;
        } else if (!(o instanceof PasswdHistory)) {
            return false;
        } else {
            PasswdHistory hist = (PasswdHistory)o;
            if ((itsIsEnabled != hist.itsIsEnabled) ||
                (itsMaxSize != hist.itsMaxSize) ||
                (itsPasswds.size() != hist.itsPasswds.size())) {
                return false;
            } else {
                for (int i = 0; i < itsPasswds.size(); ++i) {
                    Entry e1 = itsPasswds.get(i);
                    Entry e2 = hist.itsPasswds.get(i);
                    if (!e1.getPasswd().equals(e2.getPasswd()) ||
                        !e1.getDate().equals(e2.getDate())) {
                        return false;
                    }
                }
                return true;
            }
        }
    }
}
