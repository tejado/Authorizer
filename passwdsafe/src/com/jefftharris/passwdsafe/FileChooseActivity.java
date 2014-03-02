/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  File choose activity
 */
public class FileChooseActivity extends AbstractFileListActivity
{
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle(R.string.choose_file);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_file_choose, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
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


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#openFile(android.net.Uri, java.lang.String)
     */
    @Override
    public void openFile(Uri uri, String fileName)
    {
        if (uri != null) {
            Intent result = new Intent(Intent.ACTION_VIEW, uri);
            setResult(RESULT_OK, result);
        }
        finish();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#createNewFile(android.net.Uri)
     * @see com.jefftharris.passwdsafe.SyncProviderFilesFragment.Listener#createNewFile(android.net.Uri)
     */
    @Override
    public void createNewFile(Uri locationUri)
    {
        if (locationUri != null) {
            Intent result = new Intent(PasswdSafeUtil.NEW_INTENT,
                                       locationUri);
            setResult(RESULT_OK, result);
        }
        finish();
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
