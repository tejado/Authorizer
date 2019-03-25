/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.os.Bundle;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
        /** Preferences */
        PREFERENCES
    }

    private NavMenuItem itsSelNavItem = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return doCreateView(inflater, container,
                            R.layout.fragment_file_list_nav_drawer);
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
        updateView(Mode.INIT);
    }

    /**
     * Update drawer for the fragments displayed in the activity
     */
    public void updateView(Mode mode)
    {
        boolean openDrawer = false;
        NavMenuItem selNavItem = null;
        switch (mode) {
        case INIT: {
            break;
        }
        case ABOUT: {
            selNavItem = NavMenuItem.ABOUT;
            break;
        }
        case FILES: {
            // If the user hasn't 'learned' about the drawer, open it
            openDrawer = shouldOpenDrawer();
            selNavItem = NavMenuItem.FILES;
            break;
        }
        case PREFERENCES: {
            selNavItem = NavMenuItem.PREFERENCES;
            break;
        }
        }

        updateDrawerToggle(true, 0);

        Menu menu = getNavView().getMenu();
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            int itemId = item.getItemId();
            if (selNavItem == null) {
                item.setChecked(false);
            } else if (selNavItem.itsMenuId == itemId) {
                item.setChecked(true);
            }
        }
        itsSelNavItem = selNavItem;

        openDrawer(openDrawer);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem)
    {
        closeDrawer();

        Listener listener = getListener();
        NavMenuItem navItem = NavMenuItem.fromMenuId(menuItem.getItemId());
        if ((navItem != null) && (itsSelNavItem != navItem)) {
            switch (navItem) {
            case ABOUT: {
                listener.showAbout();
                break;
            }
            case FILES: {
                listener.showFiles();
                break;
            }
            case PREFERENCES: {
                listener.showPreferences();
            }
            }
        }

        return true;
    }

    /**
     * A menu item
     */
    private enum NavMenuItem
    {
        FILES       (R.id.menu_drawer_files),
        PREFERENCES (R.id.menu_drawer_preferences),
        ABOUT       (R.id.menu_drawer_about);

        public final int itsMenuId;

        /**
         * Constructor
         */
        NavMenuItem(int menuId)
        {
            itsMenuId = menuId;
        }

        /**
         * Get the enum from a menu id
         */
        public static NavMenuItem fromMenuId(int menuId)
        {
            for (NavMenuItem item: NavMenuItem.values()) {
                if (item.itsMenuId == menuId) {
                    return item;
                }
            }
            return null;
        }
    }
}
