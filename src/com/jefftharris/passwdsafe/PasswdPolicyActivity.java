/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.Collections;
import java.util.List;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdPolicy;

import android.os.Bundle;
import android.widget.ArrayAdapter;

/**
 * Activity for managing password policies for a file
 */
public class PasswdPolicyActivity extends AbstractPasswdFileListActivity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passwd_policy);

        if (!accessOpenFile()) {
            finish();
            return;
        }

        setTitle(PasswdSafeApp.getAppFileTitle(getUri(), this));
        showPolicies();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    public void saveFinished(boolean success)
    {
        setResult(PasswdSafeApp.RESULT_MODIFIED);
        showPolicies();
    }


    /** Show the password policies */
    private final void showPolicies()
    {
        GuiUtils.invalidateOptionsMenu(this);

        List<PasswdPolicy> policies = null;
        PasswdFileData fileData = getPasswdFileData();
        if (fileData != null) {
            policies = fileData.getHdrPasswdPolicies();
        } else {
            policies = Collections.emptyList();
        }

        setListAdapter(new ArrayAdapter<PasswdPolicy>(
            this, android.R.layout.simple_list_item_1, policies));
    }
}
