/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.view.DialogUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Activity for managing password policies for a file
 */
public class PasswdPolicyActivity extends AbstractPasswdFileListActivity
{
    private static final String TAG = "PasswdPolicyActivity";

    private static final int MENU_ADD =         0;
    private static final int MENU_EDIT =        1;
    private static final int MENU_DELETE =      2;

    private static final int DIALOG_ADD =       MAX_DIALOG + 1;
    private static final int DIALOG_EDIT =      MAX_DIALOG + 2;
    private static final int DIALOG_DELETE =    MAX_DIALOG + 3;

    // Constants must match policy_type strings
    private static final int TYPE_NORMAL =              0;
    private static final int TYPE_EASY_TO_READ =        1;
    private static final int TYPE_PRONOUNCEABLE =       2;
    private static final int TYPE_HEXADECIMAL =         3;

    private List<PasswdPolicy> itsPolicies;
    private Set<String> itsPolicyNames;
    private DialogValidator itsDeleteValidator;
    private EditDialog itsEditDialog;

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
        // Programmatic setting for Android 1.5
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        int selectedPos = getListView().getCheckedItemPosition();
        PasswdSafeApp.dbginfo(TAG, "onPrepareOptionsMenu pos " + selectedPos);
        boolean editDelete = (getSelectedPolicy() != null);

        MenuItem mi;
        mi = menu.findItem(MENU_EDIT);
        mi.setEnabled(editDelete);
        mi = menu.findItem(MENU_DELETE);
        mi.setEnabled(editDelete);

        return super.onPrepareOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_ADD, 0, R.string.add_policy);
        menu.add(0, MENU_EDIT, 0, R.string.edit_policy);
        menu.add(0, MENU_DELETE, 0, R.string.delete_policy);
        return true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean rc = true;
        switch (item.getItemId()) {
        case MENU_ADD: {
            removeDialog(DIALOG_ADD);
            showDialog(DIALOG_ADD);
            break;
        }
        case MENU_EDIT: {
            removeDialog(DIALOG_EDIT);
            showDialog(DIALOG_EDIT);
            break;
        }
        case MENU_DELETE: {
            removeDialog(DIALOG_DELETE);
            showDialog(DIALOG_DELETE);
            break;
        }
        default: {
            rc = super.onOptionsItemSelected(item);
            break;
        }
        }
        return rc;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_ADD: {
            itsEditDialog = new EditDialog();
            dialog = itsEditDialog.create(null);
            break;
        }
        case DIALOG_EDIT: {
            itsEditDialog = new EditDialog();
            dialog = itsEditDialog.create(getSelectedPolicy());
            break;
        }
        case DIALOG_DELETE: {
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
            {
                @Override
                public final void onOkClicked(DialogInterface dialog)
                {
                    deletePolicy();
                }
            };

            PasswdPolicy policy = getSelectedPolicy();
            String prompt = getString(R.string.delete_policy_msg,
                                      policy.getName());
            String title = getString(R.string.delete_policy_title);
            DialogUtils.DialogData data =
                DialogUtils.createDeletePrompt(this, dlgClick, title, prompt);
            dialog = data.itsDialog;
            itsDeleteValidator = data.itsValidator;
            break;

        }
        default: {
            dialog = super.onCreateDialog(id);
            break;
        }
        }
        return dialog;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id) {
        case DIALOG_ADD:
        case DIALOG_EDIT: {
            itsEditDialog.reset();
            break;
        }
        case DIALOG_DELETE: {
            itsDeleteValidator.reset();
            break;
        }
        default: {
            super.onPrepareDialog(id, dialog);
            break;
        }
        }
    }


    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        GuiUtils.invalidateOptionsMenu(this);

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
        itsPolicyNames = new HashSet<String>(itsPolicies.size());
        for (PasswdPolicy policy: itsPolicies) {
            itsPolicyNames.add(policy.getName());
        }

        setListAdapter(new ArrayAdapter<PasswdPolicy>(
            this, android.R.layout.simple_list_item_single_choice,
            itsPolicies));
        showPolicy(null);
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


    /** Delete the currently selected policy */
    private final void deletePolicy()
    {
        itsPolicies.remove(getSelectedPolicy());
        PasswdFileData fileData = getPasswdFileData();
        if (fileData != null) {
            fileData.setHdrPasswdPolicies(
                (itsPolicies.size() > 0) ? itsPolicies : null);
            getPasswdFile().save();
        }
        showPolicies();
    }


    /** Get the currently selected policy */
    private final PasswdPolicy getSelectedPolicy()
    {
        PasswdPolicy policy = null;
        int selectedPos = getListView().getCheckedItemPosition();
        if ((itsPolicies != null) &&
            (selectedPos >= 0) &&
            (selectedPos < itsPolicies.size())) {
            policy = itsPolicies.get(selectedPos);
        }
        return policy;
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


    /**
     * The EditDialog class encapsulates the functionality for the dialog to
     * add or edit a policy.
     */
    private class EditDialog
    {
        private PasswdPolicy itsPolicy;
        private View itsView;
        private DialogValidator itsValidator;
        private int itsOrigType = TYPE_NORMAL;
        private int itsType = TYPE_NORMAL;
        private TextView itsNameEdit;
        private TextView itsLengthEdit;
        // Lower, upper, digits, symbols
        private CheckBox[] itsOptions = new CheckBox[4];
        private TextView[] itsOptionLens = new TextView[4];

        /** Create a dialog to edit the give policy (null for an add) */
        public Dialog create(PasswdPolicy policy)
        {
            itsPolicy = policy;
            Activity act = PasswdPolicyActivity.this;
            LayoutInflater factory = LayoutInflater.from(act);
            itsView = factory.inflate(R.layout.passwd_policy_edit, null);

            itsNameEdit = (TextView)itsView.findViewById(R.id.name);
            itsLengthEdit = (TextView)itsView.findViewById(R.id.length);
            itsOptions[0] = (CheckBox)itsView.findViewById(R.id.lowercase);
            itsOptions[1] = (CheckBox)itsView.findViewById(R.id.uppercase);
            itsOptions[2] = (CheckBox)itsView.findViewById(R.id.digits);
            itsOptions[3] = (CheckBox)itsView.findViewById(R.id.symbols);
            itsOptionLens[0] = (TextView)itsView.findViewById(R.id.lowercase_len);
            itsOptionLens[1] = (TextView)itsView.findViewById(R.id.uppercase_len);
            itsOptionLens[2] = (TextView)itsView.findViewById(R.id.digits_len);
            itsOptionLens[3] = (TextView)itsView.findViewById(R.id.symbols_len);

            int titleId;
            String name;
            int len;
            boolean[] useOptions = new boolean[4];
            int[] optionLens = new int[4];
            String customSymbols;
            if (policy != null) {
                titleId = R.string.edit_policy;
                name = policy.getName();
                len = policy.getLength();
                int flags = policy.getFlags();
                if ((flags & PasswdPolicy.FLAG_USE_EASY_VISION) != 0) {
                    itsOrigType = TYPE_EASY_TO_READ;
                } else if ((flags & PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE) != 0) {
                    itsOrigType = TYPE_PRONOUNCEABLE;
                } else if ((flags & PasswdPolicy.FLAG_USE_HEX_DIGITS) != 0) {
                    itsOrigType = TYPE_HEXADECIMAL;
                } else {
                    itsOrigType = TYPE_NORMAL;
                }
                useOptions[0] = ((flags & PasswdPolicy.FLAG_USE_LOWERCASE) != 0);
                useOptions[1] = ((flags & PasswdPolicy.FLAG_USE_UPPERCASE) != 0);
                useOptions[2] = ((flags & PasswdPolicy.FLAG_USE_DIGITS) != 0);
                useOptions[3] = ((flags & PasswdPolicy.FLAG_USE_SYMBOLS) != 0);
                optionLens[0] = policy.getMinLowercase();
                optionLens[1] = policy.getMinUppercase();
                optionLens[2] = policy.getMinDigits();
                optionLens[3] = policy.getMinSymbols();
                customSymbols = policy.getSpecialSymbols();
            } else {
                titleId = R.string.new_policy;
                name = "";
                len = 12;
                itsOrigType = TYPE_NORMAL;
                for (int i = 0; i < useOptions.length; ++i) {
                    useOptions[i] = true;
                    optionLens[i] = 1;
                }
                customSymbols = null;
            }

            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
                {
                    @Override
                    public void onOkClicked(DialogInterface dialog)
                    {
                    }
                };

            AlertDialog.Builder alert = new AlertDialog.Builder(act)
                .setTitle(titleId)
                .setView(itsView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            AlertDialog dialog = alert.create();

            itsValidator = new DialogValidator.AlertValidator(dialog, itsView,
                                                              act, false)
            {
                @Override
                protected String doValidation()
                {
                    String name = itsNameEdit.getText().toString();
                    if (TextUtils.isEmpty(name)) {
                        return getString(R.string.empty_name);
                    }

                    if (((itsPolicy == null) ||
                         (!itsPolicy.getName().equals(name))) &&
                        itsPolicyNames.contains(name)) {
                        return getString(R.string.duplicate_name);
                    }

                    String lenStr = itsLengthEdit.getText().toString();
                    int length;
                    try {
                        length = Integer.valueOf(lenStr, 10);
                        if (length < 4) {
                            return getString(R.string.length_min_val, 4);
                        } else if (length > 1024) {
                            return getString(R.string.length_max_val, 1024);
                        } else if ((itsType == TYPE_HEXADECIMAL) &&
                                   ((length % 2) != 0) ) {
                            return getString(R.string.length_even_hex);
                        }

                    } catch (NumberFormatException e) {
                        return getString(R.string.invalid_length);
                    }

                    if (itsType != TYPE_HEXADECIMAL) {
                        boolean oneSelected = false;
                        for (CheckBox option: itsOptions) {
                            if (option.isChecked()) {
                                oneSelected = true;
                                break;
                            }
                        }
                        if (!oneSelected) {
                            return getString(R.string.option_not_selected);
                        }
                    }

                    if (itsType == TYPE_NORMAL) {
                        int minOptionsLen = 0;
                        for (int i = 0; i < itsOptions.length; ++i) {
                            if (itsOptions[i].isChecked()) {
                                try {
                                    int len = Integer.valueOf(
                                        itsOptionLens[i].getText().toString(),
                                        10);
                                    minOptionsLen += len;
                                } catch (NumberFormatException e) {
                                    return getString(
                                        R.string.invalid_option_length);
                                }
                            }
                        }
                        if (minOptionsLen > length) {
                            return getString(R.string.password_len_short_opt);
                        }
                    }

                    // TODO validate custom symbol chars
                    return super.doValidation();
                }

            };

            // Must set text before registering view so validation isn't
            // triggered right away

            // TODO: show/hide symbol options based on radio buttons

            itsNameEdit.setText(name);
            itsValidator.registerTextView(itsNameEdit);
            setTextView(itsLengthEdit, len);
            itsValidator.registerTextView(itsLengthEdit);

            setType(itsOrigType, true);
            for (int i = 0; i < itsOptions.length; ++i) {
                setOption(itsOptions[i], useOptions[i], true);
                setTextView(itsOptionLens[i], optionLens[i]);
                itsValidator.registerTextView(itsOptionLens[i]);
            }
            setTextView(R.id.symbols_custom, customSymbols);

            return dialog;
        }


        /** Reset the dialog validation */
        public void reset()
        {
            itsValidator.reset();
        }


        /** Set the type of policy and update the UI */
        private final void setType(int type, boolean init)
        {
            if ((type == itsType) && !init) {
                return;
            }

            itsType = type;
            if (init) {
                Spinner typeSpin = (Spinner)itsView.findViewById(R.id.type);
                typeSpin.setSelection(itsType);
                typeSpin.setOnItemSelectedListener(new OnItemSelectedListener()
                {
                    public void onItemSelected(AdapterView<?> parent, View arg1,
                                               int position, long id)
                    {
                        setType(position, false);
                    }

                    public void onNothingSelected(AdapterView<?> arg0)
                    {
                        setType(TYPE_NORMAL, false);
                    }
                });
            }

            boolean optionsVisible = false;
            String defaultSymbols = null;
            switch (itsType) {
            case TYPE_NORMAL: {
                optionsVisible = true;
                defaultSymbols = PasswdPolicy.SYMBOLS_DEFAULT;
                break;
            }
            case TYPE_EASY_TO_READ: {
                optionsVisible = true;
                defaultSymbols = PasswdPolicy.SYMBOLS_EASY;
                break;
            }
            case TYPE_PRONOUNCEABLE: {
                optionsVisible = true;
                defaultSymbols = PasswdPolicy.SYMBOLS_PRONOUNCE;
                break;
            }
            case TYPE_HEXADECIMAL: {
                optionsVisible = false;
                break;
            }
            }

            setVisible(R.id.lowercase_row, optionsVisible);
            setVisible(R.id.uppercase_row, optionsVisible);
            setVisible(R.id.digits_row, optionsVisible);
            setVisible(R.id.symbols_row, optionsVisible);
            for (CheckBox option: itsOptions) {
                setOptionLenVisible(option);
            }
            setCustomSymbolsVisible();
            setTextView(R.id.symbols_default, defaultSymbols);

            if (!init) {
                itsValidator.validate();
            }
        }


        /** Set whether an option is used */
        private final void setOption(CheckBox option, boolean use, boolean init)
        {
            if (init) {
                option.setChecked(use);
                option.setOnCheckedChangeListener(new OnCheckedChangeListener()
                {
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked)
                    {
                        setOption((CheckBox)buttonView, isChecked, false);
                    }
                });
            }

            setOptionLenVisible(option);
            if (option.getId() == R.id.symbols) {
                setCustomSymbolsVisible();
            }

            if (!init) {
                itsValidator.validate();
            }
        }


        /** Set the visibility of the custom symbols options */
        private final void setCustomSymbolsVisible()
        {
            CheckBox cb = (CheckBox)itsView.findViewById(R.id.symbols);
            boolean visible = (itsType != TYPE_HEXADECIMAL) && cb.isChecked();
            setVisible(R.id.custom_symbols_set, visible);
        }


        /** Set the visibility on an option's length field */
        private final void setOptionLenVisible(CheckBox option)
        {
            boolean visible;
            if (itsType == TYPE_NORMAL) {
                visible = option.isChecked();
            } else {
                visible = false;
            }

            int labelId = 0;
            int lengthId = 0;
            switch (option.getId()) {
            case R.id.lowercase: {
                labelId = R.id.lowercase_label;
                lengthId = R.id.lowercase_len;
                break;
            }
            case R.id.uppercase: {
                labelId = R.id.uppercase_label;
                lengthId = R.id.uppercase_len;
                break;
            }
            case R.id.digits: {
                labelId = R.id.digits_label;
                lengthId = R.id.digits_len;
                break;
            }
            case R.id.symbols: {
                labelId = R.id.symbols_label;
                lengthId = R.id.symbols_len;
                break;
            }
            }

            if (labelId != 0) {
                setVisible(labelId, visible);
                setVisible(lengthId, visible);
            }
        }


        /** Set the visibility of a view */
        private final void setVisible(int id, boolean visible)
        {
            View v = itsView.findViewById(id);
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }


        /** Set a text view to an integer value */
        private final void setTextView(TextView tv, int value)
        {
            tv.setText(Integer.toString(value));
        }


        /** Set a text view to a value */
        private final void setTextView(int id, String value)
        {
            TextView tv = (TextView)itsView.findViewById(id);
            tv.setText(value);
        }
    }
}
