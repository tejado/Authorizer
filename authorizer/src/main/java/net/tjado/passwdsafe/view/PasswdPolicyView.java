/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.file.PasswdPolicy;
import net.tjado.passwdsafe.lib.ActContext;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.TypefaceUtils;

/**
 *  Custom view for showing a password policy
 */
public class PasswdPolicyView extends LinearLayout
    implements OnCreateContextMenuListener, OnMenuItemClickListener
{
    private static final int MENU_COPY = 0;

    private PasswdPolicy itsPolicy = null;
    private TextView itsGeneratedPasswd = null;

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

    /** Show the policy location */
    public void showLocation(String location)
    {
        View row = findViewById(R.id.policy_view_location_row);
        row.setVisibility((location == null) ? View.GONE : View.VISIBLE);

        if (location != null) {
            TextView tv = findViewById(R.id.policy_view_location);
            tv.setText(location);
        }
    }

    /**
     * Show a policy
     * @param policy The policy to show
     * @param useCount The policy's use count (<0 to not show)
     */
    public void showPolicy(PasswdPolicy policy, int useCount)
    {
        if (policy == null) {
            policy = PasswdPolicy.createDefaultPolicy(getContext());
        }

        itsPolicy = policy;
        int length = itsPolicy.getLength();
        PasswdPolicy.Type type = itsPolicy.getType();
        String lowercase = getPolicyOption(itsPolicy, PasswdPolicy.FLAG_USE_LOWERCASE);
        String uppercase = getPolicyOption(itsPolicy, PasswdPolicy.FLAG_USE_UPPERCASE);
        String digits = getPolicyOption(itsPolicy, PasswdPolicy.FLAG_USE_DIGITS);
        String symbols = getPolicyOption(itsPolicy, PasswdPolicy.FLAG_USE_SYMBOLS);

        boolean optionsVisible = (type != PasswdPolicy.Type.HEXADECIMAL);
        setTextStr(R.id.policy_view_length, R.id.policy_view_length_label, Integer.toString(length), true);
        setTextStr(R.id.policy_view_type, R.id.policy_view_type_label, PasswdPolicy.getTypeStr(type, getContext()), true);
        setTextStr(R.id.policy_view_lowercase, R.id.policy_view_lowercase_label, lowercase, optionsVisible);
        setTextStr(R.id.policy_view_uppercase, R.id.policy_view_uppercase_label, uppercase, optionsVisible);
        setTextStr(R.id.policy_view_digits, R.id.policy_view_digits_label, digits, optionsVisible);
        setTextStr(R.id.policy_view_symbols, R.id.policy_view_symbols_label, symbols, optionsVisible);

        setTextStr(R.id.policy_view_use_count, R.id.policy_view_use_count_label,
                   Integer.toString(useCount), (useCount >= 0));
    }

    /** Set whether generation is enabled */
    public void setGenerateEnabled(
            @SuppressWarnings("SameParameterValue") boolean enabled)
    {
        View row = findViewById(R.id.policy_view_generate_row);
        row.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    /* (non-Javadoc)
     * @see android.view.MenuItem.OnMenuItemClickListener#onMenuItemClick(android.view.MenuItem)
     */
    public boolean onMenuItemClick(MenuItem item)
    {
        if (item.getItemId() == MENU_COPY) {
            PasswdSafeUtil.copyToClipboard(
                    itsGeneratedPasswd.getText().toString(), true, getContext());
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see android.view.View.OnCreateContextMenuListener#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    public void onCreateContextMenu(ContextMenu menu,
                                    View v,
                                    ContextMenuInfo menuInfo)
    {
        if (v == itsGeneratedPasswd) {
            menu.setHeaderTitle(R.string.password);
            menu.add(0, MENU_COPY, 0, R.string.copy_clipboard).
                setOnMenuItemClickListener(this);
        }
    }

    /** Initialize the view */
    private void init(Context context)
    {
        inflate(context, R.layout.passwd_policy_view, this);
        if (isInEditMode()) {
            return;
        }

        showLocation(null);
        showPolicy(null, -1);

        Button btn = findViewById(R.id.policy_view_generate);
        btn.setOnClickListener(v -> generatePasswd());

        itsGeneratedPasswd = findViewById(R.id.policy_view_generated_passwd);
        itsGeneratedPasswd.setOnCreateContextMenuListener(this);
        TypefaceUtils.setMonospace(itsGeneratedPasswd, context);
    }

    /** Generate a password from the current policy */
    private void generatePasswd()
    {
        String passwd = null;
        if (itsPolicy != null) {
            try {
                passwd = itsPolicy.generate();
            } catch (Exception e) {
                PasswdSafeUtil.showErrorMsg(e.toString(), new ActContext(getContext()));
            }
        }
        itsGeneratedPasswd.setText(passwd);
    }

    /** Set the text on a policy detail string */
    private void setTextStr(int id, int labelId, String str, boolean visible)
    {
        View label = findViewById(labelId);
        TextView tv = findViewById(id);
        if (visible) {
            label.setVisibility(View.VISIBLE);
            tv.setVisibility(View.VISIBLE);
            Context ctx = getContext();
            tv.setText((str != null) ? str : ctx.getString(
                    R.string.policy_no));
        } else {
            label.setVisibility(View.GONE);
            tv.setVisibility(View.GONE);
        }
    }

    /** Get a string for a particular policy option flag */
    private String getPolicyOption(PasswdPolicy policy, int flag)
    {
        Context ctx = getContext();
        String str = null;
        if (policy.checkFlags(flag)) {
            int strId = (policy.getType() == PasswdPolicy.Type.PRONOUNCEABLE)
                        ? R.string.yes : R.string.policy_yes_len;
            switch (flag) {
            case PasswdPolicy.FLAG_USE_LOWERCASE: {
                str = ctx.getString(strId, policy.getMinLowercase());
                break;
            }
            case PasswdPolicy.FLAG_USE_UPPERCASE: {
                str = ctx.getString(strId, policy.getMinUppercase());
                break;
            }
            case PasswdPolicy.FLAG_USE_DIGITS: {
                str = ctx.getString(strId, policy.getMinDigits());
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
