/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.jefftharris.passwdsafe.lib.AboutDialog;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ReleaseNotesDialog;


/**
 * The FileListActivity is the main PasswdSafe activity for file choosing and
 * top-level options
 */
public class FileListActivity extends AbstractFileListActivity
{
    private static final String TAG = "FileListActivity";


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        ReleaseNotesDialog.checkNotes(this);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_file_list, menu);

        MenuItem item = menu.findItem(R.id.menu_preferences);
        item.setIntent(new Intent(this, Preferences.class));

        return super.onCreateOptionsMenu(menu);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_about: {
            AboutDialog dlg = new AboutDialog();
            dlg.show(getSupportFragmentManager(), "AboutDialog");
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#openFile(android.net.Uri)
     * @see com.jefftharris.passwdsafe.SyncProviderFilesFragment.Listener#openFile(android.net.Uri)
     */
    @Override
    public void openFile(Uri uri, String fileName)
    {
        try {
            Intent intent = PasswdSafeUtil.createOpenIntent(uri, null);
            if (itsIsCloseOnOpen) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(ApiCompat.INTENT_FLAG_ACTIVITY_CLEAR_TASK);
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Can't open uri: " + uri, e);
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#activityHasMenu()
     */
    @Override
    public boolean activityHasMenu()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#activityHasNoneItem()
     */
    @Override
    public boolean activityHasNoneItem()
    {
        return false;
    }
}
