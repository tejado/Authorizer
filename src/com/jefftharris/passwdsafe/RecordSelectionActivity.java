/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.file.PasswdFileData;

import android.content.Intent;
import android.os.Bundle;

public class RecordSelectionActivity extends AbstractPasswdSafeActivity
{
    /**
     * Intent flag to apply a filter to show records that have no aliases
     * referencing them
     */
    public static final String FILTER_NO_ALIAS = "filterNoAlias";

    /**
     * Intent flag to apply a filter to show records that have no shortcuts
     * referencing them
     */
    public static final String FILTER_NO_SHORTCUT = "filterNoShortcut";


    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle(R.string.choose_record);

        Intent intent = getIntent();

        int options = RecordFilter.OPTS_DEFAULT;
        if (intent.getBooleanExtra(FILTER_NO_ALIAS, false)) {
            options |= RecordFilter.OPTS_NO_ALIAS;
        }
        if (intent.getBooleanExtra(FILTER_NO_SHORTCUT, false)) {
            options |= RecordFilter.OPTS_NO_SHORTCUT;
        }

        if (accessOpenFile()) {
            setRecordFilter(null, options);
            showFileData(MOD_DATA);
        } else {
            finish();
        }
    }


    @Override
    protected void onRecordClick(PwsRecord rec)
    {
        PasswdFileData fileData = getPasswdFileData();
        String uuid = fileData.getUUID(rec);

        Intent intent = new Intent();
        intent.putExtra(PasswdSafeApp.RESULT_DATA_UUID, uuid);
        setResult(RESULT_OK, intent);
        finish();
    }
}
