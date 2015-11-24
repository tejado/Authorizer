/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.lib.view.AbstractTextWatcher;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

/**
 * Fragment for changing a file's password
 */
public class PasswdSafeChangePasswordFragment
        extends AbstractPasswdSafeFileDataFragment
                        <PasswdSafeChangePasswordFragment.Listener>
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Finish changing the password */
        void finishChangePassword();

        /** Update the view for changing a password */
        void updateViewChangingPassword();
    }

    private TextView itsTitle;
    private TextInputLayout itsPasswordInput;
    private TextView itsPassword;
    private TextInputLayout itsPasswordConfirmInput;
    private TextView itsPasswordConfirm;
    private Validator itsValidator = new Validator();

    /**
     * Create a new instance
     */
    public static PasswdSafeChangePasswordFragment newInstance()
    {
        return new PasswdSafeChangePasswordFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(
                R.layout.fragment_passwdsafe_change_password, container, false);

        itsTitle = (TextView)rootView.findViewById(R.id.title);
        itsPasswordInput = (TextInputLayout)
                rootView.findViewById(R.id.password_input);
        itsPassword = (TextView)rootView.findViewById(R.id.password);
        itsValidator.registerTextView(itsPassword);
        itsPasswordInput.setTypeface(Typeface.DEFAULT);

        itsPasswordConfirmInput = (TextInputLayout)
                rootView.findViewById(R.id.password_confirm_input);
        itsPasswordConfirm = (TextView)
                rootView.findViewById(R.id.password_confirm);
        itsValidator.registerTextView(itsPasswordConfirm);
        itsPasswordConfirmInput.setTypeface(Typeface.DEFAULT);
        PasswordVisibilityMenuHandler.set(itsPassword, itsPasswordConfirm);

        GuiUtils.setupFormKeyboard(itsPassword, itsPasswordConfirm,
                                   getContext(), new Runnable()
                {
                    @Override
                    public void run()
                    {
                        save();
                    }
                });

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Listener listener = getListener();
        PasswdFileData fileData = listener.getFileData();
        itsTitle.setText(fileData.getUri().getIdentifier(getContext(), true));
        listener.updateViewChangingPassword();
        itsValidator.validate();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        GuiUtils.setKeyboardVisible(itsPassword, getContext(), false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem item = menu.findItem(R.id.menu_save);
        if (item != null) {
            item.setEnabled(itsValidator.isValid());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_save: {
            save();
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_change_password, menu);
    }

    /**
     * Save the file with the new password
     */
    private void save()
    {
        if (!itsValidator.isValid()) {
            return;
        }

        PasswdFileData fileData = getFileData();
        StringBuilder newPasswd = new StringBuilder(itsPassword.getText());
        fileData.changePasswd(newPasswd);

        getListener().finishChangePassword();
    }

    /**
     * Class to validate fields in the fragment
     */
    private class Validator extends AbstractTextWatcher
    {
        private boolean itsIsValid = false;

        /**
         * Register a text view with the validator
         */
        public void registerTextView(TextView tv)
        {
            tv.addTextChangedListener(this);
        }

        /**
         * Validate the fragment
         */
        public final void validate()
        {
            boolean valid;

            CharSequence passwd = itsPassword.getText();
            valid = !GuiUtils.setTextInputError(
                    (passwd.length() == 0) ?
                            getString(R.string.empty_password) : null,
                    itsPasswordInput);

            CharSequence confirm = itsPasswordConfirm.getText();
            valid &= !GuiUtils.setTextInputError(
                    !TextUtils.equals(passwd, confirm) ?
                            getString(R.string.passwords_do_not_match) : null,
                    itsPasswordConfirmInput);

            if (valid != itsIsValid) {
                itsIsValid = valid;
                GuiUtils.invalidateOptionsMenu(getActivity());
            }
        }

        /**
         * Is valid
         */
        public final boolean isValid()
        {
            return itsIsValid;
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            validate();
        }
    }
}
