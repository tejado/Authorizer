/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.jefftharris.passwdsafe.AbstractDialogClickListener;
import com.jefftharris.passwdsafe.DialogValidator;
import com.jefftharris.passwdsafe.PasswdSafeApp;
import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.file.PasswdPolicy;

/**
 * The PasswdPolicyEditDialog class encapsulates the functionality for the
 * dialog to add or edit a policy.
 */
public class PasswdPolicyEditDialog
{
    public interface Editor
    {
        /** Callback when the policy has finished being edited */
        public void onPolicyEditComplete(PasswdPolicy oldPolicy,
                                         PasswdPolicy newPolicy);

        /** Check whether the policy name already exists */
        public boolean isDuplicatePolicy(String name);
    }

    private final Editor itsEditor;
    private PasswdPolicy itsPolicy;
    private View itsView;
    private DialogValidator itsValidator;
    private PasswdPolicy.Type itsOrigType = PasswdPolicy.Type.NORMAL;
    private PasswdPolicy.Type itsType = PasswdPolicy.Type.NORMAL;
    private boolean itsIsNameEditable;
    private TextView itsNameEdit;
    private TextView itsLengthEdit;
    // Lower, upper, digits, symbols
    private CheckBox[] itsOptions = new CheckBox[4];
    private TextView[] itsOptionLens = new TextView[4];
    private CheckBox itsUseCustomSymbols;
    private TextView itsCustomSymbolsEdit;

    /** Constructor */
    public PasswdPolicyEditDialog(Editor editor)
    {
        itsEditor = editor;
    }

    /** Create a dialog to edit the give policy (null for an add) */
    public Dialog create(PasswdPolicy policy, Activity act)
    {
        itsPolicy = policy;
        LayoutInflater factory = LayoutInflater.from(act);
        itsView = factory.inflate(R.layout.passwd_policy_edit, null);

        itsIsNameEditable = true;
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
        itsUseCustomSymbols =
            (CheckBox)itsView.findViewById(R.id.use_custom_symbols);
        itsCustomSymbolsEdit =
            (TextView)itsView.findViewById(R.id.symbols_custom);

        int titleId;
        String name;
        int len;
        boolean[] useOptions = new boolean[4];
        int[] optionLens = new int[4];
        String customSymbols;
        if (policy != null) {
            switch (policy.getLocation()) {
            case DEFAULT: {
                itsNameEdit.setEnabled(false);
                break;
            }
            case HEADER:
            case RECORD_NAME: {
                break;
            }
            case RECORD: {
                itsIsNameEditable = false;
                break;
            }
            }

            titleId = R.string.edit_policy;
            name = policy.getName();
            len = policy.getLength();
            itsOrigType = policy.getType();
            useOptions[0] =
                policy.checkFlags(PasswdPolicy.FLAG_USE_LOWERCASE);
            useOptions[1] =
                policy.checkFlags(PasswdPolicy.FLAG_USE_UPPERCASE);
            useOptions[2] =
                policy.checkFlags(PasswdPolicy.FLAG_USE_DIGITS);
            useOptions[3] =
                policy.checkFlags(PasswdPolicy.FLAG_USE_SYMBOLS);
            optionLens[0] = policy.getMinLowercase();
            optionLens[1] = policy.getMinUppercase();
            optionLens[2] = policy.getMinDigits();
            optionLens[3] = policy.getMinSymbols();
            customSymbols = policy.getSpecialSymbols();
        } else {
            titleId = R.string.new_policy;
            name = "";
            len = 12;
            itsOrigType = PasswdPolicy.Type.NORMAL;
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
                    dialog.dismiss();
                    itsEditor.onPolicyEditComplete(itsPolicy, createPolicy());
                }
            };

        AlertDialog.Builder alert = new AlertDialog.Builder(act)
            .setTitle(titleId)
            .setView(itsView)
            .setPositiveButton(R.string.ok, dlgClick)
            .setNegativeButton(R.string.cancel, dlgClick)
            .setOnCancelListener(dlgClick);
        AlertDialog dialog = alert.create();
        itsValidator = new Validator(dialog, itsView, act);

        // Must set text before registering view so validation isn't
        // triggered right away
        View v = itsView.findViewById(R.id.name_row);
        v.setVisibility(itsIsNameEditable ? View.VISIBLE: View.GONE);
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

        setCustomSymbolsOption(customSymbols != null, true);
        itsCustomSymbolsEdit.setText(customSymbols);
        itsValidator.registerTextView(itsCustomSymbolsEdit);


        Button btn = (Button)itsView.findViewById(R.id.generate);
        btn.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                generatePasswd();
            }
        });

        return dialog;
    }


    /** Reset the dialog validation */
    public void reset()
    {
        itsValidator.reset();
    }


    /**
     * Create a policy from the dialog fields. It is assumed that the fields
     * are valid
     */
    private PasswdPolicy createPolicy()
    {
        PasswdPolicy policy = new PasswdPolicy(
            itsNameEdit.getText().toString(),
            (itsPolicy != null) ? itsPolicy.getLocation() :
                PasswdPolicy.Location.HEADER);
        int length = getTextViewInt(itsLengthEdit);
        policy.setLength(length);

        int flags = 0;
        int minLower = 1;
        int minUpper = 1;
        int minDigits = 1;
        int minSymbols = 1;
        String customSymbols = null;

        if (itsType != PasswdPolicy.Type.HEXADECIMAL) {
            if (itsOptions[0].isChecked()) {
                flags |= PasswdPolicy.FLAG_USE_LOWERCASE;
            }
            if (itsOptions[1].isChecked()) {
                flags |= PasswdPolicy.FLAG_USE_UPPERCASE;
            }
            if (itsOptions[2].isChecked()) {
                flags |= PasswdPolicy.FLAG_USE_DIGITS;
            }
            if (itsOptions[3].isChecked()) {
                flags |= PasswdPolicy.FLAG_USE_SYMBOLS;
            }
        }

        switch (itsType) {
        case NORMAL:
        case PRONOUNCEABLE: {
            if (itsUseCustomSymbols.isChecked()) {
                customSymbols = itsCustomSymbolsEdit.getText().toString();
            }
            break;
        }
        case EASY_TO_READ:
        case HEXADECIMAL: {
            break;
        }
        }

        switch (itsType) {
        case NORMAL: {
            if ((flags & PasswdPolicy.FLAG_USE_LOWERCASE) != 0) {
                minLower = getTextViewInt(itsOptionLens[0]);
            }
            if ((flags & PasswdPolicy.FLAG_USE_UPPERCASE) != 0) {
                minUpper = getTextViewInt(itsOptionLens[1]);
            }
            if ((flags & PasswdPolicy.FLAG_USE_DIGITS) != 0) {
                minDigits = getTextViewInt(itsOptionLens[2]);
            }
            if ((flags & PasswdPolicy.FLAG_USE_SYMBOLS) != 0) {
                minSymbols = getTextViewInt(itsOptionLens[3]);
            }
            break;
        }
        case EASY_TO_READ: {
            flags |= PasswdPolicy.FLAG_USE_EASY_VISION;
            break;
        }
        case PRONOUNCEABLE: {
            flags |= PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE;
            break;
        }
        case HEXADECIMAL: {
            flags |= PasswdPolicy.FLAG_USE_HEX_DIGITS;
            break;
        }
        }

        policy.setFlags(flags);
        policy.setMinLowercase(minLower);
        policy.setMinUppercase(minUpper);
        policy.setMinDigits(minDigits);
        policy.setMinSymbols(minSymbols);
        policy.setSpecialSymbols(customSymbols);

        return policy;
    }

    /** Generate a password from the policy */
    private void generatePasswd()
    {
        String passwd = null;
        PasswdPolicy policy = createPolicy();
        if (policy != null) {
            try {
                passwd = policy.generate();
            } catch (NoSuchAlgorithmException e) {
                PasswdSafeApp.showErrorMsg(e.toString(),
                                           itsView.getContext());
            }
        }
        TextView tv = (TextView)itsView.findViewById(R.id.generated_passwd);
        tv.setText(passwd);
    }

    /** Set the type of policy and update the UI */
    private final void setType(PasswdPolicy.Type type, boolean init)
    {
        if ((type == itsType) && !init) {
            return;
        }

        itsType = type;
        if (init) {
            Spinner typeSpin = (Spinner)itsView.findViewById(R.id.type);
            typeSpin.setSelection(itsType.itsStrIdx);
            typeSpin.setOnItemSelectedListener(new OnItemSelectedListener()
            {
                public void onItemSelected(AdapterView<?> parent, View arg1,
                                           int position, long id)
                {
                    setType(PasswdPolicy.Type.fromStrIdx(position), false);
                }

                public void onNothingSelected(AdapterView<?> arg0)
                {
                    setType(PasswdPolicy.Type.NORMAL, false);
                }
            });
        }

        boolean optionsVisible = false;
        String defaultSymbols = null;
        switch (itsType) {
        case NORMAL: {
            optionsVisible = true;
            defaultSymbols = PasswdPolicy.SYMBOLS_DEFAULT;
            break;
        }
        case EASY_TO_READ: {
            optionsVisible = true;
            defaultSymbols = PasswdPolicy.SYMBOLS_EASY;
            break;
        }
        case PRONOUNCEABLE: {
            optionsVisible = true;
            defaultSymbols = PasswdPolicy.SYMBOLS_PRONOUNCE;
            break;
        }
        case HEXADECIMAL: {
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


    /** Set the custom symbols option */
    private final void setCustomSymbolsOption(boolean useCustom,
                                              boolean init)
    {
        if (init) {
            itsUseCustomSymbols.setChecked(useCustom);
            itsUseCustomSymbols.setOnCheckedChangeListener(
                new OnCheckedChangeListener()
                {
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked)
                    {
                        setCustomSymbolsOption(isChecked, false);
                    }
                });
        }

        View defView = itsView.findViewById(R.id.symbols_default);
        if (useCustom) {
            defView.setVisibility(View.GONE);
            itsCustomSymbolsEdit.setVisibility(View.VISIBLE);
            itsCustomSymbolsEdit.requestFocus();
        } else {
            defView.setVisibility(View.VISIBLE);
            itsCustomSymbolsEdit.setVisibility(View.GONE);
        }

        if (!init) {
            itsValidator.validate();
        }
    }


    /** Set the visibility of the custom symbols options */
    private final void setCustomSymbolsVisible()
    {
        boolean visible = false;
        switch (itsType) {
        case NORMAL:
        case PRONOUNCEABLE: {
            CheckBox cb = (CheckBox)itsView.findViewById(R.id.symbols);
            visible = cb.isChecked();
            break;
        }
        case EASY_TO_READ:
        case HEXADECIMAL: {
            visible = false;
            break;
        }
        }
        setVisible(R.id.custom_symbols_set, visible);
    }


    /** Set the visibility on an option's length field */
    private final void setOptionLenVisible(CheckBox option)
    {
        boolean visible;
        if (itsType == PasswdPolicy.Type.NORMAL) {
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


    /** Get an integer value from a text view */
    private final int getTextViewInt(TextView tv)
        throws NumberFormatException
    {
        return Integer.valueOf(tv.getText().toString(), 10);
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


    /** Dialog validator */
    private final class Validator extends DialogValidator.AlertValidator
    {
        /** Constructor */
        private Validator(AlertDialog dlg, View view, Activity act)
        {
            super(dlg, view, act, false);
        }

        @Override
        protected String doValidation()
        {
            View generateRow = itsView.findViewById(R.id.generate_row);
            generateRow.setVisibility(View.GONE);

            if (itsIsNameEditable) {
                String name = itsNameEdit.getText().toString();
                if (TextUtils.isEmpty(name)) {
                    return getString(R.string.empty_name);
                }

                if (((itsPolicy == null) ||
                     (!itsPolicy.getName().equals(name))) &&
                    itsEditor.isDuplicatePolicy(name)) {
                    return getString(R.string.duplicate_name);
                }
            }

            int length;
            try {
                length = getTextViewInt(itsLengthEdit);
                if (length < 4) {
                    return getString(R.string.length_min_val, 4);
                } else if (length > 1024) {
                    return getString(R.string.length_max_val, 1024);
                } else if ((itsType == PasswdPolicy.Type.HEXADECIMAL) &&
                           ((length % 2) != 0) ) {
                    return getString(R.string.length_even_hex);
                }

            } catch (NumberFormatException e) {
                return getString(R.string.invalid_length);
            }

            if (itsType != PasswdPolicy.Type.HEXADECIMAL) {
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

            if (itsType == PasswdPolicy.Type.NORMAL) {
                int minOptionsLen = 0;
                for (int i = 0; i < itsOptions.length; ++i) {
                    if (itsOptions[i].isChecked()) {
                        try {
                            int len = getTextViewInt(itsOptionLens[i]);
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

            if (itsUseCustomSymbols.isChecked()) {
                String syms = itsCustomSymbolsEdit.getText().toString();
                if (TextUtils.isEmpty(syms)) {
                    return getString(R.string.empty_custom_symbols);
                }
                for (int i = 0; i < syms.length(); ++i) {
                    char c = syms.charAt(i);
                    if (Character.isLetterOrDigit(c) ||
                        Character.isSpaceChar(c)) {
                        return getString(
                            R.string.custom_symbol_not_alphanum);
                    }
                }
            }

            String str = super.doValidation();
            if (str == null) {
                generateRow.setVisibility(View.VISIBLE);
            }
            return str;
        }
    }
}