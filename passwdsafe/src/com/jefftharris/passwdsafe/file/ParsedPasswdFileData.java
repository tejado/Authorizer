/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.pwsafe.lib.file.PwsRecord;

import android.content.Context;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.R;

/**
 * The ParsedPasswdFileData contains information from a PasswdFileData parsed
 * for display
 */
public class ParsedPasswdFileData
{
    public static final String RECORD = "record";
    public static final String TITLE = "title";
    public static final String MATCH = "match";
    public static final String USERNAME = "username";
    public static final String ICON = "icon";

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
    // TODO: search filter

    /** Comparator for the collections used to display records in a password
     *  file */
    public static final class RecordMapComparator implements
                    Comparator<Map<String, Object>>
    {
        private boolean itsIsSortCaseSensitive;

        /** Constructor */
        public RecordMapComparator(boolean sortCaseSensitive)
        {
            itsIsSortCaseSensitive = sortCaseSensitive;
        }

        /** Compare two record items */
        public int compare(Map<String, Object> arg0,
                           Map<String, Object> arg1)
        {
            // Sort groups first
            Object rec0 = arg0.get(RECORD);
            Object rec1 = arg1.get(RECORD);
            if ((rec0 == null) && (rec1 != null)) {
                return -1;
            } else if ((rec0 != null) && (rec1 == null)) {
                return 1;
            }

            int rc = compareField(arg0, arg1, TITLE);
            if (rc == 0) {
                rc = compareField(arg0, arg1, USERNAME);
            }
            return rc;
        }

        /** Compare a field in two record items */
        private final int compareField(Map<String, Object> arg0,
                                       Map<String, Object> arg1,
                                       String field)
        {
            Object obj0 = arg0.get(field);
            Object obj1 = arg1.get(field);

            if ((obj0 == null) && (obj1 == null)) {
                return 0;
            } else if (obj0 == null) {
                return -1;
            } else if (obj1 == null) {
                return 1;
            } else {
                String str0 = obj0.toString();
                String str1 = obj1.toString();

                if (itsIsSortCaseSensitive) {
                    return str0.compareTo(str1);
                } else {
                    return str0.compareToIgnoreCase(str1);
                }
            }
        }
    }


    private final PasswdFileData itsFileData;
    private final GroupNode itsRootNode;
    private final ArrayList<String> itsCurrGroups = new ArrayList<String>();
    private GroupNode itsCurrGroupNode;
    //private int itsNumExpired = 0;
    private PasswdRecordFilter itsFilter = null;


    /** Default constructor */
    public ParsedPasswdFileData()
    {
        itsFileData = null;
        itsRootNode = new GroupNode();
        itsCurrGroupNode = itsRootNode;
    }

    /** Constructor with file data */
    public ParsedPasswdFileData(PasswdFileData fileData, boolean groupRecords,
                                boolean sortCaseSensitive,
                                List<String> currGroups,
                                Context ctx)
    {
        //itsNumExpired = 0;
        itsFileData = fileData;
        itsRootNode = new GroupNode();
        itsCurrGroupNode = itsRootNode;
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
        setCurrGroups(currGroups);
    }

    /** Add the current groups to the list of record items */
    public void addGroups(List<Map<String, Object>> recItems, Context ctx)
    {
        Map<String, ParsedPasswdFileData.GroupNode> entryGroups =
                itsCurrGroupNode.getGroups();
        if (entryGroups != null) {
            for(Map.Entry<String, GroupNode> entry : entryGroups.entrySet()) {
                HashMap<String, Object> recInfo = new HashMap<String, Object>();
                recInfo.put(TITLE, entry.getKey());
                recInfo.put(ICON,R.drawable.folder_rev);

                int items = entry.getValue().getNumRecords();
                String str = ctx.getResources().getQuantityString(
                        R.plurals.group_items, items, items);
                recInfo.put(USERNAME, str);
                recItems.add(recInfo);
            }
        }
    }

    /** Add the records in the current groups to the list */
    public void addRecords(List<Map<String, Object>> recItems, Context ctx)
    {
        List<MatchPwsRecord> entryRecs = itsCurrGroupNode.getRecords();
        if (entryRecs != null) {
            for (MatchPwsRecord rec : entryRecs) {
                recItems.add(createRecInfo(rec, itsFileData));
            }
        }
    }

    /** Set the current groups */
    public void setCurrGroups(List<String> groups)
    {
        itsCurrGroups.clear();
        if (groups != null) {
            itsCurrGroups.addAll(groups);
        }
        itsCurrGroupNode = itsRootNode;
        for (int i = 0; i < itsCurrGroups.size(); ++i) {
            String group = itsCurrGroups.get(i);
            GroupNode childNode = itsCurrGroupNode.getGroup(group);
            if (childNode == null) {
                // Prune groups from current item in the stack on down
                for (int j = itsCurrGroups.size() - 1; j >= i; --j) {
                    itsCurrGroups.remove(j);
                }
                break;
            }
            itsCurrGroupNode = childNode;
        }
    }

    /** Get the current groups */
    public ArrayList<String> getCurrGroups()
    {
        return itsCurrGroups;
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

    /** Create a record item for a password record */
    public static final HashMap<String, Object>
    createRecInfo(MatchPwsRecord rec, PasswdFileData fileData)
    {
        // TODO: private
        HashMap<String, Object> recInfo = new HashMap<String, Object>();
        String title = fileData.getTitle(rec.itsRecord);
        if (title == null) {
            title = "Untitled";
        }
        String user = fileData.getUsername(rec.itsRecord);
        if (!TextUtils.isEmpty(user)) {
            user = "[" + user + "]";
        }
        recInfo.put(TITLE, title);
        recInfo.put(RECORD, rec.itsRecord);
        recInfo.put(MATCH, rec.itsMatch);
        recInfo.put(USERNAME, user);
        recInfo.put(ICON, R.drawable.contact_rev);
        return recInfo;
    }
}
