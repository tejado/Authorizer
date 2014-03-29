/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.pwsafe.lib.file.PwsRecord;

import android.content.Context;
import android.text.TextUtils;

/**
 * The ParsedPasswdFileData contains information from a PasswdFileData parsed
 * for display
 */
public class ParsedPasswdFileData
{
    /** A group node */
    public static final class GroupNode
    {
        private List<MatchPwsRecord> itsRecords = null;
        private TreeMap<String, GroupNode> itsGroups = null;

        /** Constructor */
        public GroupNode()
        {
        }

        /** Add a record */
        public final void addRecord(MatchPwsRecord rec)
        {
            if (itsRecords == null) {
                itsRecords = new ArrayList<MatchPwsRecord>();
            }
            itsRecords.add(rec);
        }

        /** Get the records */
        public final List<MatchPwsRecord> getRecords()
        {
            return itsRecords;
        }

        /** Put a child group */
        public final void putGroup(String name, GroupNode node,
                                   Comparator<String> groupComp)
        {
            if (itsGroups == null) {
                itsGroups = new TreeMap<String, GroupNode>(groupComp);
            }
            itsGroups.put(name, node);
        }

        /** Get a group */
        public final GroupNode getGroup(String name)
        {
            if (itsGroups == null) {
                return null;
            } else {
                return itsGroups.get(name);
            }
        }

        /** Get the groups */
        public final Map<String, GroupNode> getGroups()
        {
            return itsGroups;
        }

        /** Get the number of records */
        public final int getNumRecords()
        {
            int num = 0;
            if (itsRecords != null) {
                num += itsRecords.size();
            }
            if (itsGroups != null) {
                for (GroupNode child: itsGroups.values()) {
                    num += child.getNumRecords();
                }
            }
            return num;
        }
    }

    /** A matched PwsRecord */
    public static final class MatchPwsRecord
    {
        public final PwsRecord itsRecord;
        public final String itsMatch;

        public MatchPwsRecord(PwsRecord rec, String match)
        {
            itsRecord = rec;
            itsMatch = match;
        }
    }

    /** Case-sensitive string comparator */
    public static final class StringComparator implements Comparator<String>
    {
        /** Compare the strings */
        public int compare(String arg0, String arg1)
        {
            return arg0.compareTo(arg1);
        }
    }


    // TODO: password expirations
    // TODO: record grouping pref
    // TODO: case sensitivty pref
    // TODO: search filter

    private final GroupNode itsRootNode;
    //private int itsNumExpired = 0;
    private PasswdRecordFilter itsFilter = null;


    /** Default constructor */
    public ParsedPasswdFileData()
    {
        itsRootNode = new GroupNode();
    }

    /** Constructor with file data */
    public ParsedPasswdFileData(PasswdFileData fileData, boolean groupRecords,
                                boolean sortCaseSensitive, Context ctx)
    {
        //itsNumExpired = 0;
        itsRootNode = new GroupNode();
        if (fileData == null) {
            return;
        }

        ArrayList<PwsRecord> records = fileData.getRecords();
        if (groupRecords) {
            Comparator<String> groupComp;
            if (sortCaseSensitive) {
                groupComp = new StringComparator();
            } else {
                groupComp = String.CASE_INSENSITIVE_ORDER;
            }

            for (PwsRecord rec : records) {
                String match = filterRecord(rec, fileData, ctx);
                if (match == null) {
                    continue;
                }
                String group = fileData.getGroup(rec);
                if (group == null) {
                    group = "";
                }
                String[] groups = TextUtils.split(group, "\\.");
                GroupNode node = itsRootNode;
                for (String g : groups) {
                    GroupNode groupNode = node.getGroup(g);
                    if (groupNode == null) {
                        groupNode = new GroupNode();
                        node.putGroup(g, groupNode, groupComp);
                    }
                    node = groupNode;
                }
                node.addRecord(new MatchPwsRecord(rec, match));
            }
        } else {
            for (PwsRecord rec : records) {
                String match = filterRecord(rec, fileData, ctx);
                if (match == null) {
                    continue;
                }
                itsRootNode.addRecord(new MatchPwsRecord(rec, match));
            }
        }
    }

    /**
     * Filter a record
     * @return A non-null string if the record matches the filter; null if it
     * does not
     */
    private final String filterRecord(PwsRecord rec, PasswdFileData fileData,
                                      Context ctx)
    {
        if (itsFilter == null) {
            return PasswdRecordFilter.QUERY_MATCH;
        }
        return itsFilter.filterRecord(rec, fileData, ctx);
    }
}
