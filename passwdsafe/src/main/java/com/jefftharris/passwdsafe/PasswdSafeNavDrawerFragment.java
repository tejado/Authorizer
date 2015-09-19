/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
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

    public enum NavMode
    {
        /** Initial state */
        INIT,
        /** File open */
        FILE_OPEN,
        /** Record open in a single-pane view */
        SINGLE_RECORD
    }

    /** Per the design guidelines, you should show the drawer on launch until
     * the user manually expands it. This shared preference tracks this. */
    private static final String PREF_USER_LEARNED_DRAWER =
            "passwdsafe_navigation_drawer_learned";

    // TODO: remember selected item? (or init from activity state)

    // TODO: test initial state of autoshow drawer.  Does it conflict with file open?

    /** Helper component that ties the action bar to the navigation drawer. */
    private ActionBarDrawerToggle itsDrawerToggle;

    private DrawerLayout itsDrawerLayout;
    private NavigationView itsNavView;
    private View itsFragmentContainerView;
    private Listener itsListener;

    private boolean itsFromSavedInstanceState;
    private boolean itsUserLearnedDrawer;

    /** Constructor */
    public PasswdSafeNavDrawerFragment()
    {
    }

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

        // If the user hasn't 'learned' about the drawer, open it
        if (!itsUserLearnedDrawer && !itsFromSavedInstanceState) {
            itsDrawerLayout.openDrawer(itsFragmentContainerView);
        }

        itsDrawerLayout.setDrawerListener(itsDrawerToggle);
        setMode(NavMode.INIT);
    }

    /** Update drawer for whether a file is open */
    public void setMode(NavMode mode)
    {
        boolean fileOpen = false;
        boolean drawerEnabled = false;
        switch (mode) {
        case INIT: {
            drawerEnabled = true;
            break;
        }
        case FILE_OPEN: {
            fileOpen = true;
            drawerEnabled = true;
            break;
        }
        case SINGLE_RECORD: {
            fileOpen = true;
            break;
        }
        }

        itsDrawerToggle.setDrawerIndicatorEnabled(drawerEnabled);

        Menu menu = itsNavView.getMenu();
        menu.setGroupEnabled(R.id.menu_drawer_file_group, fileOpen);
    }

    /** Call from activity's onPostCreate callback */
    public void onPostCreate()
    {
        itsDrawerToggle.syncState();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
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

        switch(menuItem.getItemId()) {
        case R.id.menu_drawer_records: {
            menuItem.setChecked(true);
            itsListener.showFileRecords();
            break;
        }
        case R.id.menu_drawer_passwd_policies: {
            menuItem.setChecked(true);
            itsListener.showFilePasswordPolicies();
            break;
        }
        case R.id.menu_drawer_expired_passwords: {
            menuItem.setChecked(true);
            itsListener.showFileExpiredPasswords();
            break;
        }
        case R.id.menu_drawer_preferences: {
            itsListener.showPreferences();
            break;
        }
        case R.id.menu_drawer_about: {
            itsListener.showAbout();
            break;
        }
        }

        return true;
    }

    /** Get the action bar */
    private ActionBar getActionBar()
    {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }
}
