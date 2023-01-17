/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputLayout;

import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.lib.view.AbstractTextWatcher;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.lib.view.TextInputUtils;
import net.tjado.passwdsafe.lib.view.TypefaceUtils;
import net.tjado.passwdsafe.view.PasswordVisibilityMenuHandler;

import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

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
    private EditText itsPassword;
    private TextInputLayout itsPasswordConfirmInput;
    private EditText itsPasswordConfirm;
    private final Validator itsValidator = new Validator();

    /**
     * Create a new instance
     */
    public static PasswdSafeChangePasswordFragment newInstance()
    {
        return new PasswdSafeChangePasswordFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(
                R.layout.fragment_passwdsafe_change_password, container, false);
        Context ctx = getContext();

        itsTitle = rootView.findViewById(R.id.title);
        itsPasswordInput = rootView.findViewById(R.id.password_input);
        itsPassword = rootView.findViewById(R.id.password);

        TypefaceUtils.setMonospace(itsPassword, ctx);
        itsValidator.registerTextView(itsPassword);
        itsPasswordInput.setTypeface(Typeface.DEFAULT);

        itsPasswordConfirmInput = rootView.findViewById(R.id.password_confirm_input);
        itsPasswordConfirm = rootView.findViewById(R.id.password_confirm);
        TypefaceUtils.setMonospace(itsPasswordConfirm, ctx);
        itsValidator.registerTextView(itsPasswordConfirm);
        itsPasswordConfirmInput.setTypeface(Typeface.DEFAULT);
        PasswordVisibilityMenuHandler.set(ctx, itsPassword, itsPasswordConfirm);

        GuiUtils.setupFormKeyboard(itsPassword, itsPasswordConfirm,
                                   getContext(), this::save);

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        useFileData((PasswdFileDataUser<Void>)fileData -> {
            itsTitle.setText(fileData.getUri().getIdentifier(getContext(),
                                                             true));
            return null;
        });
        getListener().updateViewChangingPassword();
        itsValidator.validate();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        GuiUtils.setKeyboardVisible(itsPassword, requireContext(), false);
        GuiUtils.clearEditText(itsPassword);
        GuiUtils.clearEditText(itsPasswordConfirm);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsValidator.unregisterTextView(itsPassword);
        itsValidator.unregisterTextView(itsPasswordConfirm);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu)
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
        if (item.getItemId() == R.id.menu_save) {
            save();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

        try (Owner<PwsPassword> passwd = PwsPassword.create(itsPassword.getText()))
        {
            useFileData((PasswdFileDataUser<Void>)fileData -> {
                fileData.changePasswd(passwd.pass());
                return null;
            });
        }
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
        protected void registerTextView(TextView tv)
        {
            tv.addTextChangedListener(this);
        }

        /**
         * Unregister a text view
         */
        protected void unregisterTextView(TextView tv)
        {
            tv.removeTextChangedListener(this);
        }

        /**
         * Validate the fragment
         */
        protected final void validate()
        {
            boolean valid;

            CharSequence passwd = itsPassword.getText();
            valid = !TextInputUtils.setTextInputError(
                    (passwd.length() == 0) ?
                            getString(R.string.empty_password) : null,
                    itsPasswordInput);

            CharSequence confirm = itsPasswordConfirm.getText();
            valid &= !TextInputUtils.setTextInputError(
                    !TextUtils.equals(passwd, confirm) ?
                            getString(R.string.passwords_do_not_match) : null,
                    itsPasswordConfirmInput);

            if (valid != itsIsValid) {
                itsIsValid = valid;
                requireActivity().invalidateOptionsMenu();
            }
        }

        /**
         * Is valid
         */
        protected final boolean isValid()
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
