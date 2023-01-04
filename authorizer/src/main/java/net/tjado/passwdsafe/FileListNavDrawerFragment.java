/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Fragment for the navigation drawer of the file list activity
 */
public class FileListNavDrawerFragment
        extends AbstractNavDrawerFragment<FileListNavDrawerFragment.Listener>
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Show the files */
        void showFiles();

        /** Show the backup files */
        void showBackupFiles();

        /** Show the preferences */
        void showPreferences();

        /** Show the about dialog */
        void showAbout();
    }

    /** Mode of the navigation drawer */
    public enum Mode
    {
        /** Initial state */
        INIT,
        /** About */
        ABOUT,
        /** Files */
        FILES,
        /** Backup files */
        BACKUP_FILES,
        /** Preferences */
        PREFERENCES
    }

    private static final String PREF_SHOWN_DRAWER =
            "passwdsafe_navigation_drawer_shown";
    private static final int SHOWN_DRAWER_PROVIDERS = 1;

    private int itsSelNavItem = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        return doCreateView(inflater, container,
                            R.layout.fragment_file_list_nav_drawer);
    }

    @Override
    public void onViewCreated(@NonNull View fragView, Bundle savedInstanceState)
    {
        super.onViewCreated(fragView, savedInstanceState);
    }

    /**
     * Users of this fragment must call this method to set up the navigation
     * drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(DrawerLayout drawerLayout)
    {
        doSetUp(drawerLayout, PREF_SHOWN_DRAWER, SHOWN_DRAWER_PROVIDERS);
        updateView(Mode.INIT);
    }

    /**
     * Update drawer for the fragments displayed in the activity
     */
    public void updateView(Mode mode)
    {
        Menu menu = getNavView().getMenu();
        boolean openDrawer = false;
        int selNavItem = -1;
        switch (mode) {
            case INIT: {
                break;
            }
            case ABOUT: {
                selNavItem = R.id.menu_drawer_about;
                break;
            }
            case FILES: {
                // If the user hasn't 'learned' about the drawer, open it
                openDrawer = shouldOpenDrawer();
                selNavItem = R.id.menu_drawer_files;
                break;
            }
            case BACKUP_FILES: {
                selNavItem = R.id.menu_drawer_backup_files;
                break;
            }
            case PREFERENCES: {
                selNavItem = R.id.menu_drawer_preferences;
                break;
            }
        }

        updateDrawerToggle(true, 0);

        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            int itemId = item.getItemId();
            if (selNavItem == -1) {
                item.setChecked(false);
            } else if (selNavItem == itemId) {
                item.setChecked(true);
            }
        }
        itsSelNavItem = selNavItem;

        openDrawer(openDrawer);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
    {
        closeDrawer();

        Listener listener = getListener();
        int navItem = menuItem.getItemId();
        if (itsSelNavItem != navItem) {
            if (navItem == R.id.menu_drawer_about) {
                listener.showAbout();
            } else if (navItem == R.id.menu_drawer_backup_files) {
                listener.showBackupFiles();
            } else if (navItem == R.id.menu_drawer_files) {
                listener.showFiles();
            } else /*if (navItem == R.id.menu_drawer_preferences)*/ {
                listener.showPreferences();
            }
        }

        return true;
    }
}
