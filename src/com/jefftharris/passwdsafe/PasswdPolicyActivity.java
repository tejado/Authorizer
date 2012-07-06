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
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity for managing password policies for a file
 */
public class PasswdPolicyActivity extends AbstractPasswdFileListActivity
{
    private List<PasswdPolicy> itsPolicies;

    // TODO: app default policy


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
        showPolicy(null);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    public void saveFinished(boolean success)
    {
        setResult(PasswdSafeApp.RESULT_MODIFIED);
        showPolicies();
    }


    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        PasswdPolicy policy = null;
        if (position < itsPolicies.size()) {
            policy = itsPolicies.get(position);
        }
        showPolicy(policy);
    }


    /** Show the password policies */
    private final void showPolicies()
    {
        GuiUtils.invalidateOptionsMenu(this);

        PasswdFileData fileData = getPasswdFileData();
        if (fileData != null) {
            itsPolicies = fileData.getHdrPasswdPolicies();
        } else {
            itsPolicies = Collections.emptyList();
        }

        setListAdapter(new ArrayAdapter<PasswdPolicy>(
            this, android.R.layout.simple_list_item_single_choice,
            itsPolicies));
    }


    /** Show the details of a policy */
    private final void showPolicy(PasswdPolicy policy)
    {
        int length = 0;
        String lowercase = null;
        String uppercase = null;
        String digits = null;
        String symbols = null;
        String easyvision = null;
        String pronounceable = null;
        String hexadecimal = null;

        if (policy != null) {
            length = policy.getLength();
            lowercase = getPolicyOption(policy,
                                        PasswdPolicy.FLAG_USE_LOWERCASE);
            uppercase = getPolicyOption(policy,
                                        PasswdPolicy.FLAG_USE_UPPERCASE);
            digits = getPolicyOption(policy,
                                     PasswdPolicy.FLAG_USE_DIGITS);
            symbols = getPolicyOption(policy,
                                        PasswdPolicy.FLAG_USE_SYMBOLS);
            easyvision = getPolicyOption(policy,
                                        PasswdPolicy.FLAG_USE_EASY_VISION);
            pronounceable = getPolicyOption(policy,
                                            PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE);
            hexadecimal = getPolicyOption(policy,
                                          PasswdPolicy.FLAG_USE_HEX_DIGITS);
        }

        setTextStr(R.id.length, Integer.toString(length));
        setTextStr(R.id.lowercase, lowercase);
        setTextStr(R.id.uppercase, uppercase);
        setTextStr(R.id.digits, digits);
        setTextStr(R.id.symbols, symbols);
        setTextStr(R.id.easyvision, easyvision);
        setTextStr(R.id.pronounceable, pronounceable);
        setTextStr(R.id.hexadecimal, hexadecimal);
    }


    /** Set the text on a policy detail string */
    private final void setTextStr(int id, String str)
    {
        TextView tv = (TextView)findViewById(id);
        tv.setText((str != null) ? str : getString(R.string.policy_no));
    }


    /** Get a string for a particular policy option flag */
    private final String getPolicyOption(PasswdPolicy policy, int flag)
    {
        String str = null;
        if ((policy.getFlags() & flag) != 0) {
            switch (flag) {
            case PasswdPolicy.FLAG_USE_LOWERCASE: {
                str = getString(R.string.policy_yes_len,
                                policy.getMinLowercase());
                break;
            }
            case PasswdPolicy.FLAG_USE_UPPERCASE: {
                str = getString(R.string.policy_yes_len,
                                policy.getMinUppercase());
                break;
            }
            case PasswdPolicy.FLAG_USE_DIGITS: {
                str = getString(R.string.policy_yes_len,
                                policy.getMinDigits());
                break;
            }
            case PasswdPolicy.FLAG_USE_SYMBOLS: {
                String symbols = policy.getSpecialSymbols();
                int id;
                if (!TextUtils.isEmpty(symbols)) {
                    id = R.string.policy_yes_sym_policy;
                } else if ((policy.getFlags() &
                            PasswdPolicy.FLAG_USE_EASY_VISION) != 0) {
                    id = R.string.policy_yes_sym_easy;
                    symbols = PasswdPolicy.SYMBOLS_EASY;
                } else if ((policy.getFlags() &
                            PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE) != 0) {
                    id = R.string.policy_yes_sym_pronounce;
                    symbols = PasswdPolicy.SYMBOLS_PRONOUNCE;
                } else {
                    id = R.string.policy_yes_sym_default;
                    symbols = PasswdPolicy.SYMBOLS_DEFAULT;
                }
                str = getString(id, policy.getMinSymbols(), symbols);
                break;
            }
            case PasswdPolicy.FLAG_USE_HEX_DIGITS:
            case PasswdPolicy.FLAG_USE_EASY_VISION:
            case PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE: {
                str = getString(R.string.policy_yes);
                break;
            }
            }
        }
        return str;
    }
}
