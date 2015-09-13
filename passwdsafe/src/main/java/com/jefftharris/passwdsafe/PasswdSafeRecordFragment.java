/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdLocation;

import org.pwsafe.lib.file.PwsRecord;

import java.util.List;


/**
 * Fragment showing a password record
 */
public class PasswdSafeRecordFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Get the file data */
        PasswdFileData getFileData();

        /** Update the view for the location in the password file */
        void updateLocationView(PasswdLocation location);

        /** Edit the record */
        void editRecord(PasswdLocation location);

        /** Delete the record */
        void deleteRecord(PasswdLocation location);

        /** Is the navigation drawer open */
        boolean isNavDrawerOpen();
    }

    private PasswdLocation itsLocation;
    private boolean itsCanEdit = false;
    private boolean itsCanDelete = false;
    private boolean itsHasNotes = false;
    private TabLayout itsTabs;
    private TextView itsTitle;
    private Listener itsListener;

    /** Last selected tab to restore across records */
    private static int itsLastSelectedTab = 0;

    /**
     * Create a new instance
     */
    public static PasswdSafeRecordFragment newInstance(PasswdLocation location)
    {
        PasswdSafeRecordFragment frag = new PasswdSafeRecordFragment();
        Bundle args = new Bundle();
        args.putParcelable("location", location);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        PasswdLocation location;
        if (args != null) {
            location = args.getParcelable("location");
        } else {
            location = new PasswdLocation();
        }
        itsLocation = location;
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record,
                                     container, false);
        itsTitle = (TextView)root.findViewById(R.id.title);

        final ViewPager viewPager = (ViewPager)root.findViewById(R.id.viewpager);
        viewPager.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener()
                {
                    @Override
                    public void onPageSelected(int position)
                    {
                        itsLastSelectedTab = position;
                    }
                });
        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager())
        {
            @Override
            public Fragment getItem(int position)
            {
                switch (position) {
                case 0: {
                    return PasswdSafeRecordBasicFragment.newInstance(
                            itsLocation.getRecord());
                }
                case 1: {
                    return PasswdSafeRecordPasswordFragment.newInstance(
                            itsLocation.getRecord());
                }
                case 2: {
                    return PasswdSafeRecordNotesFragment.newInstance(
                            itsLocation.getRecord());
                }
                }
                return null;
            }

            @Override
            public int getCount()
            {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position)
            {
                switch (position) {
                case 0: {
                    return getString(R.string.basic);
                }
                case 1: {
                    return getString(R.string.password);
                }
                case 2: {
                    return getString(R.string.notes);
                }
                }
                return null;
            }
        });
        viewPager.setCurrentItem(itsLastSelectedTab);

        // TODO: how to distinguish close of file vs. close of record in single pane view

        itsTabs = (TabLayout)root.findViewById(R.id.tabs);
        itsTabs.post(new Runnable()
        {
            @Override
            public void run()
            {
                itsTabs.setupWithViewPager(viewPager);
                updateNotesTab();
            }
        });

        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateLocationView(itsLocation);
        refresh();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        if ((itsListener != null) && !itsListener.isNavDrawerOpen()) {
            inflater.inflate(R.menu.fragment_passwdsafe_record, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.menu_edit);
        if (item != null) {
            item.setVisible(itsCanEdit);
        }

        item = menu.findItem(R.id.menu_delete);
        if (item != null) {
            item.setVisible(itsCanDelete);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_edit: {
            if (itsListener != null) {
                itsListener.editRecord(itsLocation);
            }
            return true;
        }
        case R.id.menu_delete: {
            if (itsListener != null) {
                itsListener.deleteRecord(itsLocation);
            }
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /**
     * Refresh the view
     */
    public void refresh()
    {
        if (!isAdded() || (itsListener == null)) {
            return;
        }

        PasswdFileData fileData = itsListener.getFileData();
        if (fileData == null) {
            return;
        }

        PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
        if (rec == null) {
            return;
        }

        PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
        if (passwdRec == null) {
            return;
        }

        itsTitle.setText(fileData.getTitle(rec));

        itsCanEdit = fileData.canEdit();
        boolean isProtected = fileData.isProtected(rec);
        List<PwsRecord> references = passwdRec.getRefsToRecord();
        boolean hasReferences = (references != null) && !references.isEmpty();
        itsCanDelete = itsCanEdit && !hasReferences && !isProtected;

        switch (passwdRec.getType()) {
        case NORMAL:
        case ALIAS: {
            String notes = fileData.getNotes(rec);
            itsHasNotes = !TextUtils.isEmpty(notes);
            break;
        }
        case SHORTCUT: {
            itsHasNotes = false;
            break;
        }
        }
        updateNotesTab();

        GuiUtils.invalidateOptionsMenu(getActivity());
    }

    /**
     * Update the notes tab
     */
    private void updateNotesTab()
    {
        TabLayout.Tab tab =
                (itsTabs.getTabCount() >= 3) ? itsTabs.getTabAt(2) : null;
        if (tab != null) {
            if (itsHasNotes) {
                tab.setIcon(R.drawable.ic_action_file_attachment);
            } else {
                tab.setIcon(null);
            }
        }
    }
}
