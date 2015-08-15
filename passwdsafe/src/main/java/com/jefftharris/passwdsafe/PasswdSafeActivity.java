/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;

/**
 * The main PasswdSafe activity for showing a password file
 */
public class PasswdSafeActivity extends AppCompatActivity
        implements PasswdSafeNavDrawerFragment.Listener
{
    /** Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer. */
    private PasswdSafeNavDrawerFragment itsNavDrawerFrag;

    /** Used to store the last screen title */
    private CharSequence itsTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passwd_safe);

        itsNavDrawerFrag = (PasswdSafeNavDrawerFragment)
                getSupportFragmentManager().findFragmentById(
                        R.id.navigation_drawer);
        itsTitle = getTitle();

        // Set up the drawer.
        itsNavDrawerFrag.setUp(
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        itsNavDrawerFrag.onPostCreate();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position)
    {
        // TODO: real nav items

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.container,
                                PlaceholderFragment.newInstance(position + 1))
                       .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (!itsNavDrawerFrag.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.activity_passwd_safe, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
        /*
        case R.id.action_settings: {
            return true;
        }
        */
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    private void restoreActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(itsTitle);
        }
    }


    public void onSectionAttached(int number)
    {
        switch (number) {
        case 1:
            itsTitle = "Section 1";
            break;
        case 2:
            itsTitle = "Section 2";
            break;
        case 3:
            itsTitle = "Section 3";
            break;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
    {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber)
        {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment()
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            return inflater
                    .inflate(R.layout.fragment_passwd_safe, container, false);
        }

        @Override
        public void onAttach(Activity activity)
        {
            super.onAttach(activity);
            ((PasswdSafeActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
