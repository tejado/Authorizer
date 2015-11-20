/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


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

import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdLocation;

import org.pwsafe.lib.file.PwsRecord;

import java.util.List;


/**
 * Fragment showing a password record
 */
public class PasswdSafeRecordFragment
        extends AbstractPasswdSafeFileDataFragment
                        <PasswdSafeRecordFragment.Listener>
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Edit the record */
        void editRecord(PasswdLocation location);

        /** Delete the record */
        void deleteRecord(PasswdLocation location);

        /** Update the view for a record */
        void updateViewRecord(PasswdLocation location);
    }

    private boolean itsCanEdit = false;
    private boolean itsCanDelete = false;
    private boolean itsHasNotes = false;
    private TabLayout itsTabs;

    /** Last selected tab to restore across records */
    private static int itsLastSelectedTab = 0;

    /**
     * Create a new instance
     */
    public static PasswdSafeRecordFragment newInstance(PasswdLocation location)
    {
        PasswdSafeRecordFragment frag = new PasswdSafeRecordFragment();
        frag.setArguments(createArgs(location));
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record,
                                     container, false);

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
                            getLocation());
                }
                case 1: {
                    return PasswdSafeRecordPasswordFragment.newInstance(
                            getLocation());
                }
                case 2: {
                    return PasswdSafeRecordNotesFragment.newInstance(
                            getLocation());
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

        itsTabs = (TabLayout)root.findViewById(R.id.tabs);
        itsTabs.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (!isAdded()) {
                    return;
                }
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
        itsListener.updateViewRecord(getLocation());
        refresh();
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
                itsListener.editRecord(getLocation());
            }
            return true;
        }
        case R.id.menu_delete: {
            if (itsListener != null) {
                itsListener.deleteRecord(getLocation());
            }
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_record, menu);
    }

    /**
     * Refresh the view
     */
    private void refresh()
    {
        RecordInfo info = getRecordInfo();
        if (info == null) {
            return;
        }

        itsCanEdit = info.itsFileData.canEdit();
        boolean isProtected = info.itsFileData.isProtected(info.itsRec);
        List<PwsRecord> references = info.itsPasswdRec.getRefsToRecord();
        boolean hasReferences = (references != null) && !references.isEmpty();
        itsCanDelete = itsCanEdit && !hasReferences && !isProtected;

        switch (info.itsPasswdRec.getType()) {
        case NORMAL:
        case ALIAS: {
            String notes = info.itsFileData.getNotes(info.itsRec);
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
