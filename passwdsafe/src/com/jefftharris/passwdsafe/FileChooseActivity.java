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

        // TODO: new file support
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
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#activityHasMenu()
     */
    @Override
    public boolean activityHasMenu()
    {
        return false;
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
