/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment for the navigation drawer of the file list activity
 */
public class FileListNavDrawerFragment
        extends AbstractNavDrawerFragment<FileListNavDrawerFragment.Listener>
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Show the preferences */
        //void showPreferences();

        /** Show the about dialog */
        //void showAbout();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View fragView = doCreateView(inflater, container,
                                     R.layout.fragment_file_list_nav_drawer);
        return fragView;
    }

    /**
     * Users of this fragment must call this method to set up the navigation
     * drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(DrawerLayout drawerLayout)
    {
        super.setUp(drawerLayout);
        //updateView(Mode.INIT, "", false);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem)
    {
        closeDrawer();
        return true;
    }
}
