/*
 * Copyright (©) 2011-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.file.PasswdExpiration;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.pref.FontSizePref;
import com.jefftharris.passwdsafe.pref.PasswdExpiryNotifPref;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public abstract class AbstractPasswdSafeActivity extends AbstractPasswdFileListActivity
    implements PasswdFileActivity
{
    protected static final String TAG = "PasswdSafe";

    private static final int MENU_PARENT = 1;
    private static final int MENU_SEARCH = 2;
    private static final int MENU_CLOSE = 3;
    protected static final int ABS_MENU_MAX = 3;

    protected static final String RECORD = "record";
    protected static final String TITLE = "title";
    protected static final String MATCH = "match";
    protected static final String USERNAME = "username";
    protected static final String ICON = "icon";

    private static final String BUNDLE_FILTER_QUERY =
        "passwdsafe.filterQuery";
    private static final String BUNDLE_CURR_GROUPS =
        "passwdsafe.currGroups";

    /** File data may have been modified */
    protected static final int MOD_DATA           = 1 << 0;
    /** Group may have been modified */
    protected static final int MOD_GROUP          = 1 << 1;
    /** Search may have been modified */
    protected static final int MOD_SEARCH         = 1 << 2;

    private GroupNode itsRootNode = null;
    private GroupNode itsCurrGroupNode = null;
    private boolean itsGroupRecords = true;
    private boolean itsIsSortCaseSensitive = true;
    private boolean itsIsSearchCaseSensitive = false;
    private boolean itsIsSearchRegex = false;
    private FontSizePref itsFontSize = Preferences.PREF_FONT_SIZE_DEF;
    protected PasswdExpiryNotifPref itsExpiryNotifPref =
        Preferences.PREF_PASSWD_EXPIRY_NOTIF_DEF;
    protected int itsNumExpired = 0;

    protected final ArrayList<HashMap<String, Object>> itsListData =
        new ArrayList<HashMap<String, Object>>();

    private PasswdRecordFilter itsFilter = null;

    private ArrayList<String> itsCurrGroups = new ArrayList<String>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passwd_safe);

        View v = findViewById(R.id.query_clear_btn);
        v.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                setPasswdRecordFilter(null);
            }
        });

        GuiUtils.removeUnsupportedCenterVertical(findViewById(R.id.query));

        v = findViewById(R.id.current_group_panel);
        v.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doBackPressed();
            }
        });

        PasswdRecordFilter filter = null;
        if (savedInstanceState != null) {
            filter = savedInstanceState.getParcelable(BUNDLE_FILTER_QUERY);
            ArrayList<String> currGroups =
                savedInstanceState.getStringArrayList(BUNDLE_CURR_GROUPS);
            if (currGroups != null) {
                itsCurrGroups = new ArrayList<String>(currGroups);
            }
        }
        setPasswdRecordFilter(filter);

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        itsGroupRecords = Preferences.getGroupRecordsPref(prefs);
        itsIsSortCaseSensitive = Preferences.getSortCaseSensitivePref(prefs);
        itsIsSearchCaseSensitive =
            Preferences.getSearchCaseSensitivePref(prefs);
        itsIsSearchRegex = Preferences.getSearchRegexPref(prefs);
        itsFontSize = Preferences.getFontSizePref(prefs);
        itsExpiryNotifPref = Preferences.getPasswdExpiryNotifPref(prefs);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    public void saveFinished(boolean success)
    {
        showFileData(MOD_DATA);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        String query = null;
        if ((intent != null) &&
            intent.getAction().equals(Intent.ACTION_SEARCH)) {
            query = intent.getStringExtra(SearchManager.QUERY);
        }
        setRecordFilter(query);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_FILTER_QUERY,
                               (itsFilter != null) ? itsFilter : null);
        outState.putStringArrayList(BUNDLE_CURR_GROUPS, itsCurrGroups);
    }


    protected void addSearchMenuItem(Menu menu)
    {
        MenuItem mi = menu.add(0, MENU_SEARCH, 0, R.string.search);
        mi.setIcon(android.R.drawable.ic_menu_search);
    }


    protected void addParentMenuItem(Menu menu)
    {
        MenuItem mi = menu.add(0, MENU_PARENT, 0, R.string.parent_group);
        mi.setIcon(R.drawable.arrow_up);
    }


    protected void addCloseMenuItem(Menu menu)
    {
        MenuItem mi = menu.add(0, MENU_CLOSE, 0, R.string.close);
        mi.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        addSearchMenuItem(menu);
        addCloseMenuItem(menu);
        addParentMenuItem(menu);
        return true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem mi = menu.findItem(MENU_PARENT);
        if (mi != null) {
            mi.setEnabled(!isRootGroup());
        }
        return super.onPrepareOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean rc = true;
        switch (item.getItemId()) {
        case MENU_PARENT:
        {
            doBackPressed();
            break;
        }
        case MENU_SEARCH:
        {
            onSearchRequested();
            break;
        }
        case MENU_CLOSE:
        {
            ActivityPasswdFile passwdFile = getPasswdFile();
            if (passwdFile != null) {
                passwdFile.close();
            }
            break;
        }
        default:
        {
            rc = super.onOptionsItemSelected(item);
            break;
        }
        }
        return rc;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (GuiUtils.isBackKeyDown(keyCode, event)) {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            if (doBackPressed()) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        if (!doBackPressed()) {
            finish();
        }
    }


    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        HashMap<String, Object> item = itsListData.get(position);
        PwsRecord rec = (PwsRecord)item.get(RECORD);
        if (rec != null) {
            onRecordClick(rec);
        } else {
            String childTitle = (String)item.get(TITLE);
            itsCurrGroups.add(childTitle);
            showFileData(MOD_GROUP);
        }
    }


    protected abstract void onRecordClick(PwsRecord rec);


    protected void showFileData(int mod)
    {
        GuiUtils.invalidateOptionsMenu(this);
        populateFileData(mod);

        View panel = findViewById(R.id.current_group_panel);
        if (isRootGroup()) {
            panel.setVisibility(View.GONE);
        } else {
            panel.setVisibility(View.VISIBLE);
            TextView tv = (TextView)findViewById(R.id.current_group_label);
            tv.setText(TextUtils.join(" / ", itsCurrGroups));
        }

        int layout = R.layout.passwdsafe_list_item;
        switch (itsFontSize) {
        case NORMAL:
        {
            // Default already set
            break;
        }
        case SMALL:
        {
            layout = R.layout.passwdsafe_list_item_small;
            break;
        }
        }

        String[] from;
        int[] to;
        if (!hasFilterSearchQuery()) {
            from = new String[] { TITLE, USERNAME, ICON };
            to = new int[] { android.R.id.text1, android.R.id.text2,
                             R.id.icon };
        } else {
            from = new String[] { TITLE, USERNAME, ICON, MATCH };
            to = new int[] { android.R.id.text1, android.R.id.text2,
                             R.id.icon, R.id.match };
        }

        ListAdapter adapter = new SectionListAdapter(this, itsListData, layout,
                                                     from, to,
                                                     itsIsSortCaseSensitive);
        setListAdapter(adapter);
    }


    /**
     * Is the root group selected
     */
    protected boolean isRootGroup()
    {
        return itsCurrGroups.isEmpty();
    }


    /** Get the current group node */
    protected GroupNode getCurrGroupNode()
    {
        return itsCurrGroupNode;
    }


    private final void populateFileData(int mod)
    {
        itsListData.clear();
        PasswdFileData fileData = getPasswdFileData();
        if (fileData == null) {
            return;
        }

        if ((mod & (MOD_DATA | MOD_SEARCH)) != 0) {
            populateRootNode(fileData);
        }

        // find right group
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

        // Build the list data
        Map<String, GroupNode> entryGroups = itsCurrGroupNode.getGroups();
        if (entryGroups != null) {
            for(Map.Entry<String, GroupNode> entry : entryGroups.entrySet()) {
                HashMap<String, Object> recInfo = new HashMap<String, Object>();
                recInfo.put(TITLE, entry.getKey());
                recInfo.put(ICON,R.drawable.folder_rev);

                int items = entry.getValue().getNumRecords();
                String str =
                    getResources().getQuantityString(R.plurals.group_items,
                                                     items, items);
                recInfo.put(USERNAME, str);
                itsListData.add(recInfo);
            }
        }

        List<MatchPwsRecord> entryRecs = itsCurrGroupNode.getRecords();
        if (entryRecs != null) {
            for (MatchPwsRecord rec : entryRecs) {
                itsListData.add(createRecInfo(rec, fileData));
            }
        }

        RecordMapComparator comp =
            new RecordMapComparator(itsIsSortCaseSensitive);
        Collections.sort(itsListData, comp);
    }


    /** Populate the contents of the root node from the file data */
    private final void populateRootNode(PasswdFileData fileData)
    {
        PasswdSafeApp.dbginfo(TAG, "populateRootNode");
        ArrayList<PwsRecord> records = fileData.getRecords();
        itsRootNode = new GroupNode();

        if (itsGroupRecords) {
            Comparator<String> groupComp;
            if (itsIsSortCaseSensitive) {
                groupComp = new StringComparator();
            } else {
                groupComp = String.CASE_INSENSITIVE_ORDER;
            }

            for (PwsRecord rec : records) {
                String match = filterRecord(rec, fileData);
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
                String match = filterRecord(rec, fileData);
                if (match == null) {
                    continue;
                }
                itsRootNode.addRecord(new MatchPwsRecord(rec, match));
            }
        }

        itsNumExpired = 0;
        PasswdRecordFilter.ExpiryFilter filter = itsExpiryNotifPref.getFilter();
        if (filter != null) {
            long expiration = filter.getExpiryFromNow(null);
            for (PasswdRecord rec : fileData.getPasswdRecords()) {
                PasswdExpiration expiry = rec.getPasswdExpiry();
                if (expiry == null) {
                    continue;
                }

                if (expiry.itsExpiration.getTime() <= expiration) {
                    ++itsNumExpired;
                }
            }
        }
    }


    private static final HashMap<String, Object>
    createRecInfo(MatchPwsRecord rec, PasswdFileData fileData)
    {
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


    /** Set the filter to match on various fields */
    protected final void setRecordFilter(String query)
    {
        setRecordFilter(query, PasswdRecordFilter.OPTS_DEFAULT);
    }


    /** Set the record filter options */
    protected final void setRecordFilter(String query, int options)
    {
        PasswdRecordFilter filter = null;
        Pattern queryPattern = null;
        if ((query != null) && (query.length() != 0)) {
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
            }
        }
        if ((queryPattern != null) ||
            (options != PasswdRecordFilter.OPTS_DEFAULT)) {
            filter = new PasswdRecordFilter(queryPattern, options);
        }

        setPasswdRecordFilter(filter);
    }

    /** Set the record filter for an expiration */
    protected final void setRecordExpiryFilter
    (
        PasswdRecordFilter.ExpiryFilter filter,
        Date customDate
    )
    {
        setPasswdRecordFilter(
            new PasswdRecordFilter(filter, customDate,
                                   PasswdRecordFilter.OPTS_DEFAULT));
    }

    /** Set the record filter */
    private final void setPasswdRecordFilter(PasswdRecordFilter filter)
    {
        itsFilter = filter;
        View panel = findViewById(R.id.query_panel);
        if (hasFilterSearchQuery()) {
            panel.setVisibility(View.VISIBLE);
            TextView tv = (TextView)findViewById(R.id.query);
            tv.setText(getString(R.string.query_label,
                                 itsFilter.toString(this)));
        } else {
            panel.setVisibility(View.GONE);
        }

        showFileData(MOD_SEARCH);
    }


    /** Does the record filter have a search query */
    private final boolean hasFilterSearchQuery()
    {
        if (itsFilter == null) {
            return false;
        }
        return itsFilter.hasSearchQuery();
    }


    /**
     * Filter a record
     * @return A non-null string if the record matches the filter; null if it
     * does not
     */
    private final String filterRecord(PwsRecord rec, PasswdFileData fileData)
    {
        if (itsFilter == null) {
            return PasswdRecordFilter.QUERY_MATCH;
        }
        return itsFilter.filterRecord(rec, fileData, this);
    }


    /**
     * @return true if a group was popped or filter removed, false to use
     *         default behavior
     */
    private final boolean doBackPressed()
    {
        PasswdSafeApp.dbginfo(TAG, "doBackPressed");
        int size = itsCurrGroups.size();
        if (size != 0) {
            itsCurrGroups.remove(size - 1);
            showFileData(MOD_GROUP);
            return true;
        } else if (itsFilter != null) {
            setPasswdRecordFilter(null);
            return true;
        } else {
            return false;
        }
    }


    private static final class RecordMapComparator implements
                    Comparator<HashMap<String, Object>>
    {
        private boolean itsIsSortCaseSensitive;

        public RecordMapComparator(boolean sortCaseSensitive)
        {
            itsIsSortCaseSensitive = sortCaseSensitive;
        }

        public int compare(HashMap<String, Object> arg0,
                           HashMap<String, Object> arg1)
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

        private final int compareField(HashMap<String, Object> arg0,
                                       HashMap<String, Object> arg1,
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


    private static final class StringComparator implements Comparator<String>
    {
        public int compare(String arg0, String arg1)
        {
            return arg0.compareTo(arg1);
        }
    }


    protected static final class MatchPwsRecord
    {
        public final PwsRecord itsRecord;
        public final String itsMatch;

        public MatchPwsRecord(PwsRecord rec, String match)
        {
            itsRecord = rec;
            itsMatch = match;
        }
    }


    protected static final class GroupNode
    {
        private List<MatchPwsRecord> itsRecords = null;
        private TreeMap<String, GroupNode> itsGroups = null;

        public GroupNode()
        {
        }

        public final void addRecord(MatchPwsRecord rec)
        {
            if (itsRecords == null) {
                itsRecords = new ArrayList<MatchPwsRecord>();
            }
            itsRecords.add(rec);
        }

        public final List<MatchPwsRecord> getRecords()
        {
            return itsRecords;
        }

        public final void putGroup(String name, GroupNode node,
                                   Comparator<String> groupComp)
        {
            if (itsGroups == null) {
                itsGroups = new TreeMap<String, GroupNode>(groupComp);
            }
            itsGroups.put(name, node);
        }

        public final GroupNode getGroup(String name)
        {
            if (itsGroups == null) {
                return null;
            } else {
                return itsGroups.get(name);
            }
        }

        public final Map<String, GroupNode> getGroups()
        {
            return itsGroups;
        }

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


    private static final class SectionListAdapter
        extends SimpleAdapter implements SectionIndexer
    {
        private static final class Section
        {
            public final String itsName;
            public final int itsPos;
            public Section(String name, int pos)
            {
                itsName = name;
                itsPos = pos;
            }

            @Override
            public final String toString()
            {
                return itsName;
            }
        }

        private Section[] itsSections;

        public SectionListAdapter(Context context,
                                  List<? extends Map<String, ?>> data,
                                  int resource, String[] from, int[] to,
                                  boolean caseSensitive)
        {
            super(context, data, resource, from, to);
            ArrayList<Section> sections = new ArrayList<Section>();
            char compChar = '\0';
            char first;
            char compFirst;
            for (int i = 0; i < data.size(); ++i) {
                String title = (String) data.get(i).get(TITLE);
                if (TextUtils.isEmpty(title)) {
                    first = ' ';
                } else {
                    first = title.charAt(0);
                }

                if (!caseSensitive) {
                    compFirst = Character.toLowerCase(first);
                } else {
                    compFirst = first;
                }
                if (compChar != compFirst) {
                    Section s = new Section(Character.toString(first), i);
                    sections.add(s);
                    compChar = compFirst;
                }
            }

            itsSections = sections.toArray(new Section[sections.size()]);
        }

        public int getPositionForSection(int section)
        {
            if (section < itsSections.length) {
                return itsSections[section].itsPos;
            } else {
                return 0;
            }
        }

        public int getSectionForPosition(int position)
        {
            // Section positions in increasing order
            for (int i = 0; i < itsSections.length; ++i) {
                Section s = itsSections[i];
                if (position <= s.itsPos) {
                    return i;
                }
            }
            return 0;
        }

        public Object[] getSections()
        {
            return itsSections;
        }
    }
}