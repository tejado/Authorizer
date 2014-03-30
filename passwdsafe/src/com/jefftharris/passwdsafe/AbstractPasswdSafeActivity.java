/*
 * Copyright (©) 2011-2012, 2014 Jeff Harris <jefftharris@gmail.com>
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.file.ParsedPasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdExpiration;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.pref.FontSizePref;
import com.jefftharris.passwdsafe.pref.PasswdExpiryNotifPref;
import com.jefftharris.passwdsafe.view.GuiUtils;

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
    /** Initialize file data */
    protected static final int MOD_INIT           = 1 << 3;
    /** First change after a file has been opened from a new intent */
    protected static final int MOD_OPEN_NEW       = 1 << 4;

    private ParsedPasswdFileData.GroupNode itsRootNode = null;
    private ParsedPasswdFileData.GroupNode itsCurrGroupNode = null;
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

    private final ArrayList<String> itsCurrGroups = new ArrayList<String>();

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
                setPasswdRecordFilter(null, false);
            }
        });

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
                itsCurrGroups.clear();
                itsCurrGroups.addAll(currGroups);
            }
        }
        setPasswdRecordFilter(filter, false);

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
        if ((intent != null) &&
            intent.getAction().equals(Intent.ACTION_SEARCH)) {
            setRecordFilter(intent.getStringExtra(SearchManager.QUERY));
        }
    }


    /** Initialize the activity for a new view intent */
    protected void initNewViewIntent()
    {
        itsRootNode = null;
        itsCurrGroupNode = null;
        itsNumExpired = 0;
        itsCurrGroups.clear();
        setPasswdRecordFilter(null, true);
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
            } else {
                finish();
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


    /** Get a string of the currently selected group */
    protected String getCurrGroupStr()
    {
        return TextUtils.join(".", itsCurrGroups);
    }


    /** Get the current group node */
    protected ParsedPasswdFileData.GroupNode getCurrGroupNode()
    {
        return itsCurrGroupNode;
    }


    private final void populateFileData(int mod)
    {
        itsListData.clear();
        PasswdFileData fileData;
        if ((mod & MOD_INIT) == 0) {
            fileData = getPasswdFileData();
            if (fileData == null) {
                return;
            }
        } else {
            fileData = null;
        }

        if ((mod & (MOD_DATA | MOD_SEARCH | MOD_INIT)) != 0) {
            populateRootNode(fileData);
        }

        // find right group
        itsCurrGroupNode = itsRootNode;
        for (int i = 0; i < itsCurrGroups.size(); ++i) {
            String group = itsCurrGroups.get(i);
            ParsedPasswdFileData.GroupNode childNode = itsCurrGroupNode.getGroup(group);
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
        Map<String, ParsedPasswdFileData.GroupNode> entryGroups = itsCurrGroupNode.getGroups();
        if (entryGroups != null) {
            for(Map.Entry<String, ParsedPasswdFileData.GroupNode> entry : entryGroups.entrySet()) {
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

        List<ParsedPasswdFileData.MatchPwsRecord> entryRecs = itsCurrGroupNode.getRecords();
        if (entryRecs != null) {
            for (ParsedPasswdFileData.MatchPwsRecord rec : entryRecs) {
                itsListData.add(ParsedPasswdFileData.createRecInfo(rec, fileData));
            }
        }

        ParsedPasswdFileData.RecordMapComparator comp =
            new ParsedPasswdFileData.RecordMapComparator(itsIsSortCaseSensitive);
        Collections.sort(itsListData, comp);
    }


    /** Populate the contents of the root node from the file data */
    private final void populateRootNode(PasswdFileData fileData)
    {
        PasswdSafeUtil.dbginfo(TAG, "populateRootNode");
        itsRootNode = new ParsedPasswdFileData.GroupNode();
        itsNumExpired = 0;
        if (fileData == null) {
            return;
        }

        ArrayList<PwsRecord> records = fileData.getRecords();

        if (itsGroupRecords) {
            Comparator<String> groupComp;
            if (itsIsSortCaseSensitive) {
                groupComp = new ParsedPasswdFileData.StringComparator();
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
                ParsedPasswdFileData.GroupNode node = itsRootNode;
                for (String g : groups) {
                    ParsedPasswdFileData.GroupNode groupNode = node.getGroup(g);
                    if (groupNode == null) {
                        groupNode = new ParsedPasswdFileData.GroupNode();
                        node.putGroup(g, groupNode, groupComp);
                    }
                    node = groupNode;
                }
                node.addRecord(new ParsedPasswdFileData.MatchPwsRecord(rec, match));
            }
        } else {
            for (PwsRecord rec : records) {
                String match = filterRecord(rec, fileData);
                if (match == null) {
                    continue;
                }
                itsRootNode.addRecord(new ParsedPasswdFileData.MatchPwsRecord(rec, match));
            }
        }

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

        setPasswdRecordFilter(filter, false);
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
                                   PasswdRecordFilter.OPTS_DEFAULT),
            false);
    }

    /** Set the record filter */
    private final void setPasswdRecordFilter(PasswdRecordFilter filter,
                                             boolean init)
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

        if (!init) {
            showFileData(MOD_SEARCH);
        }
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
        PasswdSafeUtil.dbginfo(TAG, "doBackPressed");
        int size = itsCurrGroups.size();
        if (size != 0) {
            itsCurrGroups.remove(size - 1);
            showFileData(MOD_GROUP);
            return true;
        } else if (itsFilter != null) {
            setPasswdRecordFilter(null, false);
            return true;
        } else {
            return false;
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
