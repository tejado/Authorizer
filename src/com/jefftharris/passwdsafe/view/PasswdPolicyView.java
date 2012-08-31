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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 *  Custom view for showing a password policy
 */
public class PasswdPolicyView extends LinearLayout
{
    private boolean itsIsVariableHeight = false;

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

    /** Set whether the height of the view changes based on type */
    public void setVariableHeight(boolean var)
    {
        itsIsVariableHeight = var;
    }

    /** Show the policy location */
    public void showLocation(String location)
    {
        View row = findViewById(R.id.location_row);
        row.setVisibility((location == null) ? View.GONE : View.VISIBLE);

        if (location != null) {
            TextView tv = (TextView)findViewById(R.id.location);
            tv.setText(location);
        }
    }

    /** Show a policy */
    public void showPolicy(PasswdPolicy policy)
    {
        if (policy == null) {
            policy = PasswdPolicy.createDefaultPolicy(getContext());
            policy.setFlags(0);
        }

        int length = policy.getLength();
        PasswdPolicy.Type type = policy.getType();
        String lowercase = getPolicyOption(policy,
                                           PasswdPolicy.FLAG_USE_LOWERCASE);
        String uppercase = getPolicyOption(policy,
                                           PasswdPolicy.FLAG_USE_UPPERCASE);
        String digits = getPolicyOption(policy,
                                        PasswdPolicy.FLAG_USE_DIGITS);
        String symbols = getPolicyOption(policy,
                                         PasswdPolicy.FLAG_USE_SYMBOLS);

        boolean optionsVisible = (type != PasswdPolicy.Type.HEXADECIMAL);
        setTextStr(R.id.length, R.id.length_label,
                   Integer.toString(length), true);
        setTextStr(R.id.type, R.id.type_label,
                   PasswdPolicy.getTypeStr(type, getContext()), true);
        setTextStr(R.id.lowercase, R.id.lowercase_label,
                   lowercase, optionsVisible);
        setTextStr(R.id.uppercase, R.id.uppercase_label,
                   uppercase, optionsVisible);
        setTextStr(R.id.digits, R.id.digits_label, digits, optionsVisible);
        setTextStr(R.id.symbols, R.id.symbols_label, symbols, optionsVisible);
    }

    /** Initialize the view */
    private void init(Context context)
    {
        inflate(context, R.layout.passwd_policy_view, this);
        showLocation(null);
        showPolicy(null);
    }

    /** Set the text on a policy detail string */
    private final void setTextStr(int id, int labelId,
                                  String str, boolean visible)
    {
        View label = findViewById(labelId);
        TextView tv = (TextView)findViewById(id);
        if (visible) {
            label.setVisibility(View.VISIBLE);
            tv.setVisibility(View.VISIBLE);
            Context ctx = getContext();
            tv.setText((str != null) ? str : ctx.getString(R.string.policy_no));
        } else {
            int vis = itsIsVariableHeight ? View.GONE : View.INVISIBLE;
            label.setVisibility(vis);
            tv.setVisibility(vis);
        }
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
            }
        }
        return str;
    }
}
