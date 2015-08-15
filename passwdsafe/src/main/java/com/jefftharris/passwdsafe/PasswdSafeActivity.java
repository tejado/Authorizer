/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

/**
 * The main PasswdSafe activity for showing a password file
 */
public class PasswdSafeActivity extends AppCompatActivity
        implements PasswdSafeNavDrawerFragment.Listener
{
    // TODO: file open
    // TODO: new files
    // TODO: search
    // TODO: 3rdparty file open
    // TODO: record view
    // TODO: policies
    // TODO: expired passwords
    // TODO: preferences
    // TODO: about
    // TODO: add record
    // TODO: edit record
    // TODO: expiry notifications
    // TODO: details
    // TODO: file operations
    // TODO: modern theme
    // TODO: file close/lock timeout

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
        itsNavDrawerFrag.setUp((DrawerLayout)findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        itsNavDrawerFrag.onPostCreate();
        super.onPostCreate(savedInstanceState);
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
        case R.id.menu_close: {
            finish();
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /**
     * Show the file records
     */
    @Override
    public void showFileRecords()
    {
        Toast.makeText(this, "showFileRecords", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show the file password policies
     */
    @Override
    public void showFilePasswordPolicies()
    {
        Toast.makeText(this, "showFilePasswordPolicies", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show the file expired passwords
     */
    @Override
    public void showFileExpiredPasswords()
    {
        Toast.makeText(this, "showFileExpiredPasswords", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show the preferences
     */
    @Override
    public void showPreferences()
    {
        Toast.makeText(this, "showPreferences", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show the about dialog
     */
    @Override
    public void showAbout()
    {
        Toast.makeText(this, "showAbout", Toast.LENGTH_SHORT).show();
    }

    /**
     * Restore the action bar from the nav drawer
     */
    private void restoreActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(itsTitle);
        }
    }
}
