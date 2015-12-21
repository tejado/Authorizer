/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Context;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment for the navigation drawer of the PasswdSafe activity
 */
public class PasswdSafeNavDrawerFragment extends Fragment
    implements NavigationView.OnNavigationItemSelectedListener
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

    /** Per the design guidelines, you should show the drawer on launch until
     * the user manually expands it. This shared preference tracks this. */
    private static final String PREF_USER_LEARNED_DRAWER =
            "passwdsafe_navigation_drawer_learned";

    /** Helper component that ties the action bar to the navigation drawer. */
    private ActionBarDrawerToggle itsDrawerToggle;

    private DrawerLayout itsDrawerLayout;
    private NavigationView itsNavView;
    private View itsFragmentContainerView;
    private NavMenuItem itsSelNavItem = null;
    private Listener itsListener;

    private boolean itsFromSavedInstanceState;
    private boolean itsUserLearnedDrawer;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        itsUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            itsFromSavedInstanceState = true;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View fragView = inflater.inflate(
                R.layout.fragment_passwdsafe_nav_drawer, container, false);
        itsNavView = (NavigationView)fragView;
        itsNavView.setNavigationItemSelectedListener(this);
        return fragView;
    }

    /** Is the drawer open */
    public boolean isDrawerOpen()
    {
        return itsDrawerLayout != null &&
               itsDrawerLayout.isDrawerOpen(itsFragmentContainerView);
    }

    /** Is the drawer enabled */
    public boolean isDrawerEnabled()
    {
        return itsDrawerToggle.isDrawerIndicatorEnabled();
    }

    /**
     * Users of this fragment must call this method to set up the navigation
     * drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(DrawerLayout drawerLayout)
    {
        itsFragmentContainerView =
                getActivity().findViewById(R.id.navigation_drawer);
        itsDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer
        // opens
        itsDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                                        GravityCompat.START);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        itsDrawerToggle = new ActionBarDrawerToggle(
                getActivity(), itsDrawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        {
            @Override
            public void onDrawerClosed(View drawerView)
            {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!itsUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to
                    // prevent auto-showing the navigation drawer automatically
                    // in the future.
                    itsUserLearnedDrawer = true;
                    SharedPreferences sp =
                            PreferenceManager.getDefaultSharedPreferences(
                                    getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true)
                      .apply();
                }

                getActivity().supportInvalidateOptionsMenu();
            }
        };

        itsDrawerLayout.setDrawerListener(itsDrawerToggle);
        setMode(Mode.INIT, false);
    }

    /** Update drawer for the fragments displayed in the activity */
    public void setMode(Mode mode, boolean fileOpen)
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
            openDrawer = !itsUserLearnedDrawer && !itsFromSavedInstanceState;
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

        itsDrawerToggle.setDrawerIndicatorEnabled(drawerEnabled);
        if (upIndicator == 0) {
            itsDrawerToggle.setHomeAsUpIndicator(null);
        } else {
            itsDrawerToggle.setHomeAsUpIndicator(upIndicator);
        }

        Menu menu = itsNavView.getMenu();
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

        if (openDrawer) {
            itsDrawerLayout.openDrawer(itsFragmentContainerView);
        }
    }

    /** Call from activity's onPostCreate callback */
    public void onPostCreate()
    {
        itsDrawerToggle.syncState();
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        itsDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        // If the drawer is open, show the global app actions in the action bar
        if (itsDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.app_name);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return itsDrawerToggle.onOptionsItemSelected(item) ||
               super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem)
    {
        itsDrawerLayout.closeDrawers();

        NavMenuItem navItem = NavMenuItem.fromMenuId(menuItem.getItemId());
        if ((navItem != null) && (itsSelNavItem != navItem)) {
            switch (navItem) {
            case RECORDS: {
                itsListener.showFileRecords();
                break;
            }
            case PASSWORD_POLICIES: {
                itsListener.showFilePasswordPolicies();
                break;
            }
            case EXPIRED_PASSWORDS: {
                itsListener.showFileExpiredPasswords();
                break;
            }
            case PREFERENCES: {
                itsListener.showPreferences();
                break;
            }
            case ABOUT: {
                itsListener.showAbout();
                break;
            }
            }
        }

        return true;
    }

    /** Get the action bar */
    private ActionBar getActionBar()
    {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
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
