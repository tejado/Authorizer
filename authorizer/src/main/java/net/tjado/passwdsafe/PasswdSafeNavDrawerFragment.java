/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import androidx.drawerlayout.widget.DrawerLayout;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.tjado.passwdsafe.lib.view.GuiUtils;

/**
 * Fragment for the navigation drawer of the PasswdSafe activity
 */
public class PasswdSafeNavDrawerFragment
        extends AbstractNavDrawerFragment<PasswdSafeNavDrawerFragment.Listener>
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Show the file records */
        void showFileRecords();

        /** Show the file password policies */
        void showFilePasswordPolicies();

        /** Show the file expired passwords */
        void showFileExpiredPasswords();

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
        /** List of records */
        RECORDS_LIST,
        /** Single record */
        RECORDS_SINGLE,
        /** Action on a record */
        RECORDS_ACTION,
        /** Password policies */
        POLICIES,
        /** Password expirations */
        EXPIRATIONS,
        /** Preferences */
        PREFERENCES,
        /** About */
        ABOUT
    }

    private TextView itsFileName;
    private NavMenuItem itsSelNavItem = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View fragView = doCreateView(inflater, container,
                                     R.layout.fragment_passwdsafe_nav_drawer);
        View header = getNavView().getHeaderView(0);
        itsFileName = (TextView)header.findViewById(R.id.file_name);
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
        updateView(Mode.INIT, "", false);
    }

    /**
     * Update drawer for the fragments displayed in the activity
     * @param fileNameUpdate If non-null, the file name to set in the view
     */
    public void updateView(Mode mode, String fileNameUpdate, boolean fileOpen)
    {
        boolean drawerEnabled = false;
        boolean openDrawer = false;
        int upIndicator = 0;
        NavMenuItem selNavItem = null;

        switch (mode) {
        case INIT: {
            drawerEnabled = true;
            break;
        }
        case RECORDS_LIST: {
            drawerEnabled = true;
            // If the user hasn't 'learned' about the drawer, open it
            openDrawer = shouldOpenDrawer();
            selNavItem = NavMenuItem.RECORDS;
            break;
        }
        case RECORDS_SINGLE: {
            selNavItem = NavMenuItem.RECORDS;
            break;
        }
        case RECORDS_ACTION: {
            upIndicator = R.drawable.ic_action_close_cancel;
            selNavItem = NavMenuItem.RECORDS;
            break;
        }
        case POLICIES: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.PASSWORD_POLICIES;
            break;
        }
        case EXPIRATIONS: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.EXPIRED_PASSWORDS;
            break;
        }
        case PREFERENCES: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.PREFERENCES;
            break;
        }
        case ABOUT: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.ABOUT;
            break;
        }
        }

        updateDrawerToggle(drawerEnabled, upIndicator);

        Menu menu = getNavView().getMenu();
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            int itemId = item.getItemId();
            if (selNavItem == null) {
                item.setChecked(false);
            } else if (selNavItem.itsMenuId == itemId) {
                item.setChecked(true);
            }

            if ((itemId == NavMenuItem.RECORDS.itsMenuId) ||
                (itemId == NavMenuItem.PASSWORD_POLICIES.itsMenuId) ||
                (itemId == NavMenuItem.EXPIRED_PASSWORDS.itsMenuId)) {
                item.setEnabled(fileOpen);
            }
        }
        itsSelNavItem = selNavItem;

        if (fileNameUpdate != null) {
            GuiUtils.setVisible(itsFileName,
                                !TextUtils.isEmpty(fileNameUpdate));
            itsFileName.setText(fileNameUpdate);
        }

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
            case RECORDS: {
                listener.showFileRecords();
                break;
            }
            case PASSWORD_POLICIES: {
                listener.showFilePasswordPolicies();
                break;
            }
            case EXPIRED_PASSWORDS: {
                listener.showFileExpiredPasswords();
                break;
            }
            case PREFERENCES: {
                listener.showPreferences();
                break;
            }
            case ABOUT: {
                listener.showAbout();
                break;
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
        RECORDS              (R.id.menu_drawer_records),
        PASSWORD_POLICIES    (R.id.menu_drawer_passwd_policies),
        EXPIRED_PASSWORDS    (R.id.menu_drawer_expired_passwords),
        PREFERENCES          (R.id.menu_drawer_preferences),
        ABOUT                (R.id.menu_drawer_about);

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
