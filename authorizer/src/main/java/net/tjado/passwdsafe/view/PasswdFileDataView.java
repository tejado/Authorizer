/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import net.tjado.passwdsafe.Preferences;
import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.file.PasswdExpiration;
import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdRecord;
import net.tjado.passwdsafe.file.PasswdRecordFilter;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.pref.PasswdExpiryNotifPref;
import net.tjado.passwdsafe.pref.RecordSortOrderPref;

import org.pwsafe.lib.file.PwsRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The PasswdFileDataView contains state for viewing a password file
 */
public class PasswdFileDataView
{
    /**
     * Visitor interface for iterating records
     */
    public interface RecordVisitor
    {
        /** Visit a record */
        void visitRecord(String recordUuid);
    }

    private GroupNode itsRootNode;
    private GroupNode itsCurrGroupNode;
    private final ArrayList<String> itsCurrGroups = new ArrayList<>();
    private PasswdRecordFilter itsFilter;
    private int itsNumExpired = 0;
    private boolean itsIsExpiryChanged = true;
    private boolean itsIsGroupRecords =
            Preferences.PREF_GROUP_RECORDS_DEF;
    private boolean itsIsSortCaseSensitive =
            Preferences.PREF_SORT_CASE_SENSITIVE_DEF;
    private boolean itsIsSearchCaseSensitive =
            Preferences.PREF_SEARCH_CASE_SENSITIVE_DEF;
    private boolean itsIsSearchRegex =
            Preferences.PREF_SEARCH_REGEX_DEF;
    private RecordSortOrderPref itsRecordSortOrder =
            Preferences.PREF_RECORD_SORT_ORDER_DEF;
    private PasswdExpiryNotifPref itsExpiryNotifPref =
            Preferences.PREF_PASSWD_EXPIRY_NOTIF_DEF;
    private Context itsContext;
    private int itsFolderIcon;
    private int itsRecordIcon;

    private static final String TAG = "PasswdFileDataView";

    /**
     * Constructor
     */
    public PasswdFileDataView()
    {
        itsRootNode = new GroupNode();
    }

    /**
     * Handle when the owning fragment is attached to the context
     */
    public void onAttach(Context ctx, SharedPreferences prefs)
    {
        itsContext = ctx.getApplicationContext();
        itsIsGroupRecords = Preferences.getGroupRecordsPref(prefs);
        itsIsSortCaseSensitive = Preferences.getSortCaseSensitivePref(prefs);
        itsIsSearchCaseSensitive =
                Preferences.getSearchCaseSensitivePref(prefs);
        itsIsSearchRegex = Preferences.getSearchRegexPref(prefs);
        itsRecordSortOrder = Preferences.getRecordSortOrderPref(prefs);
        itsExpiryNotifPref = Preferences.getPasswdExpiryNotifPref(prefs);

        Resources.Theme theme = ctx.getTheme();
        TypedValue attr = new TypedValue();
        theme.resolveAttribute(R.attr.drawableFolder, attr, true);
        itsFolderIcon = attr.resourceId;
        theme.resolveAttribute(R.attr.drawablePersonOutline, attr, true);
        itsRecordIcon = attr.resourceId;
    }

    /**
     * Handle when the owning fragment is detached from its fragment
     */
    public void onDetach()
    {
        itsContext = null;
    }

    /**
     * Handle a shared preference change
     * @return Whether the file data should be refreshed
     */
    public boolean handleSharedPreferenceChanged(SharedPreferences prefs,
                                                 String key)
    {
        boolean rebuild = false;
        boolean rebuildSearch = false;
        switch (key) {
        case Preferences.PREF_GROUP_RECORDS: {
            itsIsGroupRecords = Preferences.getGroupRecordsPref(prefs);
            rebuild = true;
            break;
        }
        case Preferences.PREF_SORT_CASE_SENSITIVE: {
            itsIsSortCaseSensitive =
                    Preferences.getSortCaseSensitivePref(prefs);
            rebuild = true;
            break;
        }
        case Preferences.PREF_SEARCH_CASE_SENSITIVE: {
            itsIsSearchCaseSensitive =
                    Preferences.getSearchCaseSensitivePref(prefs);
            rebuildSearch = true;
            break;
        }
        case Preferences.PREF_SEARCH_REGEX: {
            itsIsSearchRegex = Preferences.getSearchRegexPref(prefs);
            rebuildSearch = true;
            break;
        }
        case Preferences.PREF_RECORD_SORT_ORDER: {
            itsRecordSortOrder = Preferences.getRecordSortOrderPref(prefs);
            rebuild = true;
            break;
        }
        case Preferences.PREF_PASSWD_EXPIRY_NOTIF: {
            itsExpiryNotifPref = Preferences.getPasswdExpiryNotifPref(prefs);
            rebuild = true;
            itsIsExpiryChanged = true;
            break;
        }
        }

        if (rebuildSearch &&
            (itsFilter != null) && itsFilter.isQueryType()) {
            try {
                PasswdRecordFilter filter =
                        createRecordFilter(itsFilter.toString(itsContext));
                setRecordFilter(filter);
            } catch (Exception e) {
                String msg = e.getMessage();
                Log.e(TAG, msg, e);
                PasswdSafeUtil.showErrorMsg(msg, itsContext);
            }
        }

        return rebuild || rebuildSearch;
    }

    /**
     * Clear the file data
     */
    public synchronized void clearFileData()
    {
        itsCurrGroups.clear();
        itsIsExpiryChanged = true;
        rebuildView(null);
    }

    /**
     * Set the file data
     */
    public synchronized void setFileData(PasswdFileData fileData)
    {
        itsCurrGroups.clear();
        itsIsExpiryChanged = true;
        rebuildView(fileData);
    }

    /**
     * Refresh the file data
     */
    public synchronized void refreshFileData(PasswdFileData fileData)
    {
        itsCurrGroups.clear();
        rebuildView(fileData);
    }

    /**
     * Get records
     */
    public synchronized List<PasswdRecordListData> getRecords(
            boolean incRecords,
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
                            null, "", itsFolderIcon, false));
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

        PasswdRecordListDataComparator comp =
                new PasswdRecordListDataComparator(itsIsSortCaseSensitive,
                                                   itsRecordSortOrder);
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
     * Does the data view show the given group
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGroup(String group)
    {
        if (TextUtils.isEmpty(group)) {
            return true;
        }

        ArrayList<String> groups = new ArrayList<>();
        PasswdFileData.splitGroup(group, groups);

        GroupNode node = itsRootNode;
        for (String checkGroup: groups) {
            GroupNode childNode = node.getGroup(checkGroup);
            if (childNode == null) {
                return false;
            }
            node = childNode;
        }
        return true;
    }

    /**
     * Get the record filter
     */
    public synchronized PasswdRecordFilter getRecordFilter()
    {
        return itsFilter;
    }

    /**
     * Create a record filter from a query string
     */
    public PasswdRecordFilter createRecordFilter(String query)
            throws Exception
    {
        PasswdRecordFilter filter = null;
        Pattern queryPattern = null;
        if (!TextUtils.isEmpty(query)) {
            try {
                int flags = 0;
                if (!itsIsSearchCaseSensitive) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                if (!itsIsSearchRegex) {
                    flags |= Pattern.LITERAL;
                }
                queryPattern = Pattern.compile(query, flags);
            } catch(PatternSyntaxException e) {
                throw new Exception("Invalid query regex", e);
            }
        }
        if (queryPattern != null) {
            filter = new PasswdRecordFilter(queryPattern,
                                            PasswdRecordFilter.OPTS_DEFAULT);
        }

        return filter;
    }

    public synchronized void setRecordFilter(PasswdRecordFilter filter)
    {
        itsFilter = filter;
    }

    /**
     * Visit all records under the current group
     */
    public synchronized void walkGroupRecords(RecordVisitor visitor)
    {
        walkGroupRecords(itsCurrGroupNode, visitor);
    }

    /**
     * Check whether the expiration options have changed
     */
    public boolean checkExpiryChanged()
    {
        boolean changed = itsIsExpiryChanged;
        itsIsExpiryChanged = false;
        return changed;
    }

    /**
     * Reset whether the expiration options have changed
     */
    public void resetExpiryChanged()
    {
        itsIsExpiryChanged = true;
    }

    /**
     * Get whether there are expired records
     */
    public boolean hasExpiredRecords()
    {
        return itsNumExpired > 0;
    }

    /**
     * Get the description of the expired records
     */
    public String getExpiredRecordsStr(Context ctx)
    {
        String str = null;
        PasswdRecordFilter.ExpiryFilter filter = itsExpiryNotifPref.getFilter();
        if (filter != null) {
            str = filter.getRecordsExpireStr(itsNumExpired, ctx.getResources());
        }
        return str;
    }

    /**
     * Get the filter for expired records
     */
    public PasswdRecordFilter.ExpiryFilter getExpiredRecordsFilter()
    {
        return itsExpiryNotifPref.getFilter();
    }

    /**
     * Rebuild the view information
     */
    private synchronized void rebuildView(PasswdFileData fileData)
    {
        itsRootNode = new GroupNode();
        itsNumExpired = 0;
        if (fileData == null) {
            updateCurrentGroup();
            return;
        }

        List<PwsRecord> records = fileData.getRecords();
        if (itsIsGroupRecords) {
            Comparator<String> groupComp = itsIsSortCaseSensitive ?
                    new StringComparator() : String.CASE_INSENSITIVE_ORDER;

            for (PwsRecord rec: records) {
                String match = filterRecord(rec, fileData);
                if (match == null) {
                    continue;
                }

                String group = fileData.getGroup(rec);
                if (group == null) {
                    group = "";
                }
                ArrayList<String> groups = new ArrayList<>();
                PasswdFileData.splitGroup(group, groups);
                GroupNode node = itsRootNode;
                for (String g : groups) {
                    GroupNode groupNode = node.getGroup(g);
                    if (groupNode == null) {
                        groupNode = new GroupNode();
                        node.putGroup(g, groupNode, groupComp);
                    }
                    node = groupNode;
                }
                node.addRecord(new MatchPwsRecord(rec, fileData, match));
             }
        } else {
            for (PwsRecord rec: records) {
                String match = filterRecord(rec, fileData);
                if (match != null) {
                    itsRootNode.addRecord(
                            new MatchPwsRecord(rec, fileData, match));
                }
            }
        }
        updateCurrentGroup();

        PasswdRecordFilter.ExpiryFilter filter = itsExpiryNotifPref.getFilter();
        if (filter != null) {
            long expiration = filter.getExpiryFromNow(null);
            for (PasswdRecord rec : fileData.getPasswdRecords()) {
                PasswdExpiration expiry = rec.getPasswdExpiry();
                if ((expiry != null) &&
                    (expiry.itsExpiration.getTime() <= expiration)) {
                    ++itsNumExpired;
                }
            }
        }
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
    private String filterRecord(PwsRecord rec, PasswdFileData fileData)
    {
        if (itsFilter == null) {
            return PasswdRecordFilter.QUERY_MATCH;
        }
        return itsFilter.filterRecord(rec, fileData, itsContext);
    }

    /**
     * Recursively visit all records under a group.  Must be called while
     * synchronized.
     */
    private void walkGroupRecords(GroupNode node, RecordVisitor visitor)
    {
        if (node == null) {
            return;
        }

        Map<String, GroupNode> childGroups = node.getGroups();
        if (childGroups != null) {
            for (GroupNode child : childGroups.values()) {
                walkGroupRecords(child, visitor);
            }
        }

        List<MatchPwsRecord> childRecords = node.getRecords();
        if (childRecords != null) {
            for (MatchPwsRecord matchRec : childRecords) {
                visitor.visitRecord(matchRec.itsUuid);
            }
        }
    }

    /**
     * Create a record item for a password record
     */
    private PasswdRecordListData createListData(MatchPwsRecord rec)
    {
        String title = rec.itsTitle;
        if (title == null) {
            title = "Untitled";
        }
        String user = rec.itsUsername;
        if (!TextUtils.isEmpty(user)) {
            user = "[" + user + "]";
        }

        return new PasswdRecordListData(title, user, rec.itsUuid, rec.itsMatch,
                                        rec.itsIcon, itsRecordIcon, true);
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
        public final String itsTitle;
        public final String itsUsername;
        public final String itsUuid;
        public final String itsMatch;
        public final String itsIcon;

        public MatchPwsRecord(PwsRecord rec,
                              PasswdFileData fileData,
                              String match)
        {
            itsTitle = fileData.getTitle(rec);
            itsUsername = fileData.getUsername(rec);
            itsUuid = fileData.getUUID(rec);
            itsMatch = match;
            itsIcon = fileData.getIcon(rec);
        }
    }


    /**
     * Case-sensitive string comparator
     */
    private static final class StringComparator implements Comparator<String>
    {
        /** Compare the strings */
        public final int compare(String arg0, String arg1)
        {
            return arg0.compareTo(arg1);
        }
    }
}
