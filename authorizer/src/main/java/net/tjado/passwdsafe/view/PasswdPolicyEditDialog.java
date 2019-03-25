/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import net.tjado.passwdsafe.Preferences;
import net.tjado.passwdsafe.file.PasswdPolicy;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.AbstractDialogClickListener;
import net.tjado.passwdsafe.lib.view.TypefaceUtils;

/**
 * The PasswdPolicyEditDialog class encapsulates the functionality for the
 * dialog to add or edit a policy.
 */
public class PasswdPolicyEditDialog extends AppCompatDialogFragment
{
    /**
     * Listener interface for the owning fragment
     */
    public interface Listener
    {
        /** Callback when the policy has finished being edited */
        void handlePolicyEditComplete(PasswdPolicy oldPolicy,
                                      PasswdPolicy newPolicy);

        /** Check whether the policy name already exists */
        boolean isDuplicatePolicy(String name);
    }

    private PasswdPolicy itsPolicy;
    private View itsView;
    private DialogValidator itsValidator;
    private PasswdPolicy.Type itsType = PasswdPolicy.Type.NORMAL;
    private boolean itsIsNameEditable;
    private TextView itsNameEdit;
    private TextView itsLengthEdit;
    // Lower, upper, digits, symbols
    private final CheckBox[] itsOptions = new CheckBox[4];
    private final TextView[] itsOptionLens = new TextView[4];
    private CheckBox itsUseCustomSymbols;
    private TextView itsCustomSymbolsEdit;
    private TextView itsGeneratedPasswd;
    private String itsDefaultSymbols;

    /**
     * Create a new instance
     * @param policy The policy to edit; null for an add
     */
    public static PasswdPolicyEditDialog newInstance(PasswdPolicy policy)
    {
        PasswdPolicyEditDialog dlg = new PasswdPolicyEditDialog();
        Bundle args = new Bundle();
        args.putParcelable("policy", policy);
        dlg.setArguments(args);
        return dlg;
    }

    @SuppressLint("InflateParams")
    public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = getArguments();
        if (args != null) {
            itsPolicy = args.getParcelable("policy");
        } else {
            itsPolicy = null;
        }

        Context ctx = getContext();
        LayoutInflater factory = LayoutInflater.from(ctx);
        itsView = factory.inflate(net.tjado.passwdsafe.R.layout.passwd_policy_edit, null);

        SharedPreferences prefs = Preferences.getSharedPrefs(ctx);

        itsIsNameEditable = true;
        itsNameEdit = (TextView)itsView.findViewById(net.tjado.passwdsafe.R.id.name);
        itsLengthEdit = (TextView)itsView.findViewById(net.tjado.passwdsafe.R.id.length);
        itsOptions[0] = (CheckBox)itsView.findViewById(net.tjado.passwdsafe.R.id.lowercase);
        itsOptions[1] = (CheckBox)itsView.findViewById(net.tjado.passwdsafe.R.id.uppercase);
        itsOptions[2] = (CheckBox)itsView.findViewById(net.tjado.passwdsafe.R.id.digits);
        itsOptions[3] = (CheckBox)itsView.findViewById(net.tjado.passwdsafe.R.id.symbols);
        itsOptionLens[0] = (TextView)itsView.findViewById(
                net.tjado.passwdsafe.R.id.lowercase_len);
        itsOptionLens[1] = (TextView)itsView.findViewById(
                net.tjado.passwdsafe.R.id.uppercase_len);
        itsOptionLens[2] = (TextView)itsView.findViewById(
                net.tjado.passwdsafe.R.id.digits_len);
        itsOptionLens[3] = (TextView)itsView.findViewById(
                net.tjado.passwdsafe.R.id.symbols_len);
        itsUseCustomSymbols =
            (CheckBox)itsView.findViewById(net.tjado.passwdsafe.R.id.use_custom_symbols);
        itsCustomSymbolsEdit =
            (TextView)itsView.findViewById(net.tjado.passwdsafe.R.id.symbols_custom);
        TypefaceUtils.setMonospace(itsCustomSymbolsEdit, ctx);
        itsGeneratedPasswd =
                (TextView)itsView.findViewById(net.tjado.passwdsafe.R.id.generated_passwd);
        TypefaceUtils.setMonospace(itsGeneratedPasswd, ctx);
        itsDefaultSymbols = Preferences.getPasswdDefaultSymbolsPref(prefs);

        int titleId;
        String name;
        int len;
        boolean[] useOptions = new boolean[4];
        int[] optionLens = new int[4];
        String customSymbols;
        PasswdPolicy.Type origType;
        if (itsPolicy != null) {
            switch (itsPolicy.getLocation()) {
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

            titleId = net.tjado.passwdsafe.R.string.edit_policy;
            name = itsPolicy.getName();
            len = itsPolicy.getLength();
            origType = itsPolicy.getType();
            useOptions[0] =
                itsPolicy.checkFlags(PasswdPolicy.FLAG_USE_LOWERCASE);
            useOptions[1] =
                itsPolicy.checkFlags(PasswdPolicy.FLAG_USE_UPPERCASE);
            useOptions[2] =
                itsPolicy.checkFlags(PasswdPolicy.FLAG_USE_DIGITS);
            useOptions[3] =
                itsPolicy.checkFlags(PasswdPolicy.FLAG_USE_SYMBOLS);
            optionLens[0] = itsPolicy.getMinLowercase();
            optionLens[1] = itsPolicy.getMinUppercase();
            optionLens[2] = itsPolicy.getMinDigits();
            optionLens[3] = itsPolicy.getMinSymbols();
            customSymbols = itsPolicy.getSpecialSymbols();
        } else {
            titleId = net.tjado.passwdsafe.R.string.new_policy;
            name = "";
            len = 12;
            origType = PasswdPolicy.Type.NORMAL;
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
                    Listener listener = (Listener)getTargetFragment();
                    listener.handlePolicyEditComplete(itsPolicy,
                                                      createPolicy());
                }
            };

        AlertDialog.Builder alert = new AlertDialog.Builder(ctx)
            .setTitle(titleId)
            .setView(itsView)
            .setPositiveButton(net.tjado.passwdsafe.R.string.ok, dlgClick)
            .setNegativeButton(net.tjado.passwdsafe.R.string.cancel, dlgClick)
            .setOnCancelListener(dlgClick);
        AlertDialog dialog = alert.create();
        itsValidator = new Validator(dialog, itsView, ctx);

        // Must set text before registering view so validation isn't
        // triggered right away
        View v = itsView.findViewById(net.tjado.passwdsafe.R.id.name_row);
        v.setVisibility(itsIsNameEditable ? View.VISIBLE: View.GONE);
        itsNameEdit.setText(name);
        itsValidator.registerTextView(itsNameEdit);
        setTextView(itsLengthEdit, len);
        itsValidator.registerTextView(itsLengthEdit);

        setType(origType, true);
        for (int i = 0; i < itsOptions.length; ++i) {
            setOption(itsOptions[i], useOptions[i], true);
            setTextView(itsOptionLens[i], optionLens[i]);
            itsValidator.registerTextView(itsOptionLens[i]);
        }

        setCustomSymbolsOption(customSymbols != null, true);
        itsCustomSymbolsEdit.setText(customSymbols);
        itsValidator.registerTextView(itsCustomSymbolsEdit);

        Button btn = (Button)itsView.findViewById(net.tjado.passwdsafe.R.id.generate);
        btn.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                generatePasswd();
            }
        });

        return dialog;
    }


    @Override
    public void onResume()
    {
        super.onResume();
        itsValidator.reset();
    }


    /**
     * Create a policy from the dialog fields. It is assumed that the fields
     * are valid
     */
    private PasswdPolicy createPolicy()
    {
        int length = getTextViewInt(itsLengthEdit);
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
        case NORMAL: {
            if (itsUseCustomSymbols.isChecked()) {
                customSymbols = itsCustomSymbolsEdit.getText().toString();
            }
            break;
        }
        case EASY_TO_READ:
        case PRONOUNCEABLE:
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

        return new PasswdPolicy(
            itsNameEdit.getText().toString(),
            (itsPolicy != null) ? itsPolicy.getLocation() :
                PasswdPolicy.Location.HEADER,
            flags, length,
            minLower, minUpper, minDigits, minSymbols, customSymbols);
    }

    /** Generate a password from the policy */
    private void generatePasswd()
    {
        String passwd = null;
        PasswdPolicy policy = createPolicy();
        if (policy != null) {
            try {
                passwd = policy.generate();
            } catch (Exception e) {
                PasswdSafeUtil.showErrorMsg(e.toString(),
                                            itsView.getContext());
            }
        }
        itsGeneratedPasswd.setText(passwd);
    }

    /** Set the type of policy and update the UI */
    private void setType(PasswdPolicy.Type type, boolean init)
    {
        if ((type == itsType) && !init) {
            return;
        }

        itsType = type;
        if (init) {
            Spinner typeSpin = (Spinner)itsView.findViewById(
                    net.tjado.passwdsafe.R.id.type);
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
            defaultSymbols = itsDefaultSymbols;
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

        setVisible(net.tjado.passwdsafe.R.id.lowercase_row, optionsVisible);
        setVisible(net.tjado.passwdsafe.R.id.uppercase_row, optionsVisible);
        setVisible(net.tjado.passwdsafe.R.id.digits_row, optionsVisible);
        setVisible(net.tjado.passwdsafe.R.id.symbols_row, optionsVisible);
        for (CheckBox option: itsOptions) {
            setOptionLenVisible(option);
        }
        setCustomSymbolsVisible();
        setTextView(net.tjado.passwdsafe.R.id.symbols_default, defaultSymbols);

        if (!init) {
            itsValidator.validate();
        }
    }


    /** Set whether an option is used */
    private void setOption(CheckBox option, boolean use, boolean init)
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
        if (option.getId() == net.tjado.passwdsafe.R.id.symbols) {
            setCustomSymbolsVisible();
        }

        if (!init) {
            itsValidator.validate();
        }
    }


    /** Set the custom symbols option */
    private void setCustomSymbolsOption(boolean useCustom, boolean init)
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

        View defView = itsView.findViewById(net.tjado.passwdsafe.R.id.symbols_default);
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
    private void setCustomSymbolsVisible()
    {
        boolean visible = false;
        switch (itsType) {
        case NORMAL: {
            CheckBox cb = (CheckBox)itsView.findViewById(net.tjado.passwdsafe.R.id.symbols);
            visible = cb.isChecked();
            break;
        }
        case EASY_TO_READ:
        case PRONOUNCEABLE:
        case HEXADECIMAL: {
            visible = false;
            break;
        }
        }
        setVisible(net.tjado.passwdsafe.R.id.custom_symbols_set, visible);
    }


    /** Set the visibility on an option's length field */
    private void setOptionLenVisible(CheckBox option)
    {
        boolean visible =
                (itsType == PasswdPolicy.Type.NORMAL) && option.isChecked();

        int labelId = 0;
        int lengthId = 0;
        switch (option.getId()) {
        case net.tjado.passwdsafe.R.id.lowercase: {
            labelId = net.tjado.passwdsafe.R.id.lowercase_label;
            lengthId = net.tjado.passwdsafe.R.id.lowercase_len;
            break;
        }
        case net.tjado.passwdsafe.R.id.uppercase: {
            labelId = net.tjado.passwdsafe.R.id.uppercase_label;
            lengthId = net.tjado.passwdsafe.R.id.uppercase_len;
            break;
        }
        case net.tjado.passwdsafe.R.id.digits: {
            labelId = net.tjado.passwdsafe.R.id.digits_label;
            lengthId = net.tjado.passwdsafe.R.id.digits_len;
            break;
        }
        case net.tjado.passwdsafe.R.id.symbols: {
            labelId = net.tjado.passwdsafe.R.id.symbols_label;
            lengthId = net.tjado.passwdsafe.R.id.symbols_len;
            break;
        }
        }

        if (labelId != 0) {
            setVisible(labelId, visible);
            setVisible(lengthId, visible);
        }
    }


    /** Set the visibility of a view */
    private void setVisible(int id, boolean visible)
    {
        View v = itsView.findViewById(id);
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }


    /** Get an integer value from a text view */
    private int getTextViewInt(TextView tv)
        throws NumberFormatException
    {
        return Integer.valueOf(tv.getText().toString(), 10);
    }


    /** Set a text view to an integer value */
    @SuppressLint("SetTextI18n")
    private void setTextView(TextView tv, int value)
    {
        tv.setText(Integer.toString(value));
    }


    /** Set a text view to a value */
    private void setTextView(@SuppressWarnings("SameParameterValue") int id,
                             String value)
    {
        TextView tv = (TextView)itsView.findViewById(id);
        tv.setText(value);
    }


    /** Dialog validator */
    private final class Validator extends DialogValidator.AlertCompatValidator
    {
        /** Constructor */
        private Validator(AlertDialog dlg, View view, Context ctx)
        {
            super(dlg, view, ctx);
        }

        @Override
        protected String doValidation()
        {
            View generateRow = itsView.findViewById(net.tjado.passwdsafe.R.id.generate_row);
            generateRow.setVisibility(View.GONE);

            if (itsIsNameEditable) {
                String name = itsNameEdit.getText().toString();
                if (TextUtils.isEmpty(name)) {
                    return getString(net.tjado.passwdsafe.R.string.empty_name);
                }

                if ((itsPolicy == null) ||
                    !itsPolicy.getName().equals(name)) {
                    Listener listener = (Listener)getTargetFragment();
                    if (listener.isDuplicatePolicy(name)) {
                        return getString(net.tjado.passwdsafe.R.string.duplicate_name);
                    }
                }
            }

            int length;
            try {
                length = getTextViewInt(itsLengthEdit);
                if (length < 4) {
                    return getString(net.tjado.passwdsafe.R.string.length_min_val, 4);
                } else if (length > 1024) {
                    return getString(net.tjado.passwdsafe.R.string.length_max_val, 1024);
                } else if ((itsType == PasswdPolicy.Type.HEXADECIMAL) &&
                           ((length % 2) != 0) ) {
                    return getString(net.tjado.passwdsafe.R.string.length_even_hex);
                }

            } catch (NumberFormatException e) {
                return getString(net.tjado.passwdsafe.R.string.invalid_length);
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
                    return getString(net.tjado.passwdsafe.R.string.option_not_selected);
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
                                    net.tjado.passwdsafe.R.string.invalid_option_length);
                        }
                    }
                }
                if (minOptionsLen > length) {
                    return getString(net.tjado.passwdsafe.R.string.password_len_short_opt);
                }
            }

            if (itsUseCustomSymbols.isChecked()) {
                String syms = itsCustomSymbolsEdit.getText().toString();
                if (TextUtils.isEmpty(syms)) {
                    return getString(net.tjado.passwdsafe.R.string.empty_custom_symbols);
                }
                for (int i = 0; i < syms.length(); ++i) {
                    char c = syms.charAt(i);
                    if (Character.isLetterOrDigit(c) ||
                        Character.isSpaceChar(c)) {
                        return getString(
                                net.tjado.passwdsafe.R.string.custom_symbol_not_alphanum);
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
