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
import android.widget.Toast;

/**
 * Fragment for the navigation drawer of the PasswdSafe activity
 */
public class PasswdSafeNavDrawerFragment extends Fragment
    implements NavigationView.OnNavigationItemSelectedListener
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Called when an item in the navigation drawer is selected */
        void onNavigationDrawerItemSelected(int pos);
    }

    /** Remember the position of the selected item. */
    private static final String STATE_SELECTED_POSITION =
            "passwdsafe_selected_navigation_drawer_position";

    /** Per the design guidelines, you should show the drawer on launch until
     * the user manually expands it. This shared preference tracks this. */
    private static final String PREF_USER_LEARNED_DRAWER =
            "passwdsafe_navigation_drawer_learned";


    /** Helper component that ties the action bar to the navigation drawer. */
    private ActionBarDrawerToggle itsDrawerToggle;

    private DrawerLayout itsDrawerLayout;
    private NavigationView itsNavView;
    private View itsFragmentContainerView;
    private Listener itsListener;

    private int itsCurrentSelectedPosition = 0;
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
            itsCurrentSelectedPosition =
                    savedInstanceState.getInt(STATE_SELECTED_POSITION);
            itsFromSavedInstanceState = true;
        }

        // TODO: need selected item??

        // Select either the default item (0) or the last selected item.
        selectItem(itsCurrentSelectedPosition);
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
                R.layout.fragment_passwd_safe_nav_drawer, container, false);
        itsNavView = (NavigationView)fragView;
        itsNavView.setNavigationItemSelectedListener(this);
        selectNavViewItem(itsCurrentSelectedPosition);
        return fragView;
    }

    /** Is the drawer open */
    public boolean isDrawerOpen()
    {
        return itsDrawerLayout != null &&
               itsDrawerLayout.isDrawerOpen(itsFragmentContainerView);
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
    }

    /** Call from activity's onPostCreate callback */
    public void onPostCreate()
    {
        itsDrawerToggle.syncState();
    }

    private void selectItem(int position)
    {
        itsCurrentSelectedPosition = position;
        if (itsDrawerLayout != null) {
            itsDrawerLayout.closeDrawer(itsFragmentContainerView);
        }
        if (itsListener != null) {
            itsListener.onNavigationDrawerItemSelected(position);
        }
    }

    /** Select an item in the nav view */
    private void selectNavViewItem(int position)
    {
        if (itsNavView != null) {
            Menu menu = itsNavView.getMenu();
            int numItems = menu.size();
            for (int i = 0; i < numItems; ++i) {
                MenuItem item = menu.getItem(i);
                if (item.getOrder() == position) {
                    item.setChecked(true);
                    break;
                }
            }
        }
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
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, itsCurrentSelectedPosition);
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
        if (itsDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // TODO: needed?
        if (item.getItemId() == R.id.action_example) {
            Toast.makeText(getActivity(), "Example action.", Toast.LENGTH_SHORT)
                 .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem)
    {
        switch(menuItem.getItemId()) {
        case R.id.menu_drawer_records: {
            // TODO: records
        }
        case R.id.menu_drawer_passwd_policies: {
            // TODO: policies
        }
        case R.id.menu_drawer_preferences: {
            // TODO: preferences
        }
        case R.id.menu_drawer_about: {
            // TODO: about
        }
        }
        return true;
    }


    private ActionBar getActionBar()
    {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }
}
