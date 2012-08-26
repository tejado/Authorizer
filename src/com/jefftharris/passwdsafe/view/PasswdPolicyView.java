/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.file.PasswdPolicy;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 *  Custom view for showing a password policy
 */
public class PasswdPolicyView extends LinearLayout
{
    /** Constructor */
    public PasswdPolicyView(Context context)
    {
        super(context);
        init(context);
    }

    /** Constructor from layout inflation */
    public PasswdPolicyView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    /** Show a policy */
    public void showPolicy(PasswdPolicy policy)
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

    /** Initialize the view */
    private void init(Context context)
    {
        inflate(context, R.layout.passwd_policy_view, this);
        showPolicy(null);
    }

    /** Set the text on a policy detail string */
    private final void setTextStr(int id, String str)
    {
        Context ctx = getContext();
        TextView tv = (TextView)findViewById(id);
        tv.setText((str != null) ? str : ctx.getString(R.string.policy_no));
    }

    /** Get a string for a particular policy option flag */
    private final String getPolicyOption(PasswdPolicy policy, int flag)
    {
        Context ctx = getContext();
        String str = null;
        if (policy.checkFlags(flag)) {
            switch (flag) {
            case PasswdPolicy.FLAG_USE_LOWERCASE: {
                str = ctx.getString(R.string.policy_yes_len,
                                    policy.getMinLowercase());
                break;
            }
            case PasswdPolicy.FLAG_USE_UPPERCASE: {
                str = ctx.getString(R.string.policy_yes_len,
                                    policy.getMinUppercase());
                break;
            }
            case PasswdPolicy.FLAG_USE_DIGITS: {
                str = ctx.getString(R.string.policy_yes_len,
                                    policy.getMinDigits());
                break;
            }
            case PasswdPolicy.FLAG_USE_SYMBOLS: {
                String symbols = policy.getSpecialSymbols();
                int id;
                if (!TextUtils.isEmpty(symbols)) {
                    id = R.string.policy_yes_sym_policy;
                } else if (policy.checkFlags(
                               PasswdPolicy.FLAG_USE_EASY_VISION)) {
                    id = R.string.policy_yes_sym_easy;
                    symbols = PasswdPolicy.SYMBOLS_EASY;
                } else if (policy.checkFlags(
                               PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE)) {
                    id = R.string.policy_yes_sym_pronounce;
                    symbols = PasswdPolicy.SYMBOLS_PRONOUNCE;
                } else {
                    id = R.string.policy_yes_sym_default;
                    symbols = PasswdPolicy.SYMBOLS_DEFAULT;
                }
                str = ctx.getString(id, policy.getMinSymbols(), symbols);
                break;
            }
            case PasswdPolicy.FLAG_USE_HEX_DIGITS:
            case PasswdPolicy.FLAG_USE_EASY_VISION:
            case PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE: {
                str = ctx.getString(R.string.policy_yes);
                break;
            }
            }
        }
        return str;
    }
}
