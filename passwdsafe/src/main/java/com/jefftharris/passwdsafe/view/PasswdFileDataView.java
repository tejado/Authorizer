/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import android.content.Context;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.pref.RecordSortOrderPref;

import org.pwsafe.lib.file.PwsRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The PasswdFileDataView contains state for viewing a password file
 */
public class PasswdFileDataView
{
    private PasswdFileData itsFileData;

    private GroupNode itsRootNode;

    private GroupNode itsCurrGroupNode;

    private final ArrayList<String> itsCurrGroups = new ArrayList<>();

    private PasswdRecordFilter itsFilter;

    // TODO: group records pref
    private boolean itsIsGroupRecords = true;

    // TODO: sort case pref
    private boolean itsIsSortCaseSensitive = false;

    // TODO: sort order pref
    private RecordSortOrderPref itsRecordSortOrder =
            RecordSortOrderPref.GROUP_LAST;

    private final Context itsContext;

    /**
     * Constructor
     */
    public PasswdFileDataView(Context ctx)
    {
        itsRootNode = new GroupNode();
        itsContext = ctx;
    }

    /**
     * Clear the file data
     */
    public synchronized void clearFileData()
    {
        itsFileData = null;
        itsCurrGroups.clear();
        rebuildView();
    }

    /**
     * Set the file data
     */
    public synchronized void setFileData(PasswdFileData fileData)
    {
        itsFileData = fileData;
        itsCurrGroups.clear();
        rebuildView();
    }

    /**
     * Get records
     */
    public synchronized List<PasswdRecordListData> getRecords(boolean incRecords,
                                                              boolean incGroups)
    {
        List<PasswdRecordListData> records = new ArrayList<>();
        if (itsCurrGroupNode == null) {
            return records;
        }

        if (incGroups) {
            Map<String, GroupNode> entryGroups = itsCurrGroupNode.getGroups();
            if (entryGroups != null) {
                for (Map.Entry<String, GroupNode> entry:
                        entryGroups.entrySet()) {
                    int items = entry.getValue().getNumRecords();
                    String str = itsContext.getResources().getQuantityString(
                            R.plurals.group_items, items, items);

                    records.add(new PasswdRecordListData(
                            entry.getKey(), str, null,
                            null, R.drawable.folder_rev, null));
                }
            }
        }

        if (incRecords) {
            List<MatchPwsRecord> entryRecs = itsCurrGroupNode.getRecords();
            if (entryRecs != null) {
                for (MatchPwsRecord rec: entryRecs) {
                    records.add(createListData(rec));
                }
            }
        }

        PasswdRecordListDataComparator comp = new PasswdRecordListDataComparator(
                itsIsSortCaseSensitive, itsRecordSortOrder);
        Collections.sort(records, comp);
        return records;
    }

    /**
     * Set the current groups
     */
    public synchronized void setCurrGroups(List<String> groups)
    {
        itsCurrGroups.clear();
        if (groups != null) {
            itsCurrGroups.addAll(groups);
        }
        updateCurrentGroup();
    }

    /**
     * Rebuild the view information
     */
    private void rebuildView()
    {
        // TODO: rebuild in background?

        itsRootNode = new GroupNode();
        if (itsFileData == null) {
            updateCurrentGroup();
            return;
        }

        List<PwsRecord> records = itsFileData.getRecords();
        if (itsIsGroupRecords) {
            Comparator<String> groupComp = itsIsSortCaseSensitive ?
                    new StringComparator() : String.CASE_INSENSITIVE_ORDER;

            for (PwsRecord rec: records) {
                String match = filterRecord(rec);
                if (match == null) {
                    continue;
                }

                String group = itsFileData.getGroup(rec);
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
            for (PwsRecord rec: records) {
                String match = filterRecord(rec);
                if (match != null) {
                    itsRootNode.addRecord(new MatchPwsRecord(rec, match));
                }
            }
        }
        updateCurrentGroup();
    }

    /** Update the current group */
    private void updateCurrentGroup()
    {
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

    /**
     * Filter a record
     * @return A non-null string if the record matches the filter; null if it
     * does not
     */
    private String filterRecord(PwsRecord rec)
    {
        if (itsFilter == null) {
            return PasswdRecordFilter.QUERY_MATCH;
        }
        return itsFilter.filterRecord(rec, itsFileData, itsContext);
    }

    /**
     * Create a record item for a password record
     */
    private PasswdRecordListData createListData(MatchPwsRecord rec)
    {
        String title = itsFileData.getTitle(rec.itsRecord);
        if (title == null) {
            title = "Untitled";
        }
        String user = itsFileData.getUsername(rec.itsRecord);
        if (!TextUtils.isEmpty(user)) {
            user = "[" + user + "]";
        }
        String uuid = itsFileData.getUUID(rec.itsRecord);

        return new PasswdRecordListData(title, user, uuid, rec.itsMatch,
                                        R.drawable.contact_rev, rec.itsRecord);
    }


    /**
     * A group node
     */
    private static final class GroupNode
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
                itsRecords = new ArrayList<>();
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
                itsGroups = new TreeMap<>(groupComp);
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


    /**
     * A matched PwsRecord
     */
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


    /**
     * Case-sensitive string comparator
     */
    public static final class StringComparator implements Comparator<String>
    {
        /** Compare the strings */
        public final int compare(String arg0, String arg1)
        {
            return arg0.compareTo(arg1);
        }
    }
}
