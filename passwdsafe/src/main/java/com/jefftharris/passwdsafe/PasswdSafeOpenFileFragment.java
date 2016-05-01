/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.TextInputLayout;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.lib.view.TypefaceUtils;
import com.jefftharris.passwdsafe.util.Pair;
import com.jefftharris.passwdsafe.util.YubiState;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;
import com.jefftharris.passwdsafe.view.TextInputUtils;

import org.pwsafe.lib.exception.InvalidPassphraseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;


/**
 * Fragment for opening a file
 */
public class PasswdSafeOpenFileFragment
        extends AbstractPasswdSafeOpenNewFileFragment
        implements View.OnClickListener
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Handle when the file open is canceled */
        void handleFileOpenCanceled();

        /** Handle when the file was successfully opened */
        void handleFileOpen(PasswdFileData fileData, String recToOpen);

        /** Update the view for opening a file */
        void updateViewFileOpen();
    }

    /**
     * Type of change in the saved password
     */
    private enum SavePasswordChange
    {
        ADD,
        REMOVE,
        NONE
    }

    private Listener itsListener;
    private String itsRecToOpen;
    private TextView itsTitle;
    private TextInputLayout itsPasswordInput;
    private TextView itsPasswordEdit;
    private TextView itsSavedPasswordMsg;
    private CheckBox itsReadonlyCb;
    private CheckBox itsSavePasswdCb;
    private CheckBox itsYubikeyCb;
    private Button itsOkBtn;
    private OpenTask itsOpenTask;
    private SavedPasswordsMgr itsSavedPasswordsMgr;
    private SavePasswordChange itsSaveChange = SavePasswordChange.NONE;
    private AddSavedPasswordUser itsAddSavedPasswordUser;
    private YubikeyMgr itsYubiMgr;
    private YubikeyMgr.User itsYubiUser;
    private YubiState itsYubiState = YubiState.UNAVAILABLE;
    private int itsYubiSlot = 2;
    private boolean itsIsYubikey = false;
    private String itsUserPassword;
    private int itsRetries = 0;
    private TextWatcher itsErrorClearingWatcher;

    private static final String ARG_URI = "uri";
    private static final String ARG_REC_TO_OPEN = "recToOpen";
    private static final String STATE_SLOT = "slot";

    // TODO: Use TextInputEditText everywhere a TextInputLayout is used
    // TODO: translations
    // TODO: Check URI type to see whether save should be enabled
    // TODO: Add warning about no fingerprints available before generating key
    // TODO: Help for saved passwords

    /**
     * Create a new instance
     */
    public static PasswdSafeOpenFileFragment newInstance(Uri fileUri,
                                                         String recToOpen)
    {
        PasswdSafeOpenFileFragment fragment = new PasswdSafeOpenFileFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, fileUri);
        args.putString(ARG_REC_TO_OPEN, recToOpen);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            setFileUri((Uri)args.getParcelable(ARG_URI));
            itsRecToOpen = args.getString(ARG_REC_TO_OPEN);
        }

        if (savedInstanceState == null) {
            itsYubiSlot = 2;
        } else {
            itsYubiSlot = savedInstanceState.getInt(STATE_SLOT, 2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_passwdsafe_open_file,
                                         container, false);
        setupView(rootView);
        Context ctx = getContext();

        itsTitle = (TextView)rootView.findViewById(R.id.file);
        itsPasswordInput =
                (TextInputLayout)rootView.findViewById(R.id.passwd_input);
        itsPasswordEdit = (TextView)rootView.findViewById(R.id.passwd_edit);
        TypefaceUtils.setMonospace(itsPasswordEdit, ctx);
        PasswordVisibilityMenuHandler.set(ctx, itsPasswordEdit);
        itsPasswordEdit.setEnabled(false);

        itsReadonlyCb = (CheckBox)rootView.findViewById(R.id.read_only);
        Button cancelBtn = (Button)rootView.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(this);
        itsOkBtn = (Button)rootView.findViewById(R.id.ok);
        itsOkBtn.setOnClickListener(this);
        itsOkBtn.setEnabled(false);

        itsSavedPasswordMsg =
                (TextView)rootView.findViewById(R.id.saved_password);
        itsSavePasswdCb = (CheckBox)rootView.findViewById(R.id.save_password);
        boolean saveAvailable = itsSavedPasswordsMgr.isAvailable();
        GuiUtils.setVisible(itsSavePasswdCb, saveAvailable);
        GuiUtils.setVisible(itsSavedPasswordMsg, false);

        itsYubiMgr = new YubikeyMgr();
        itsYubiUser = new YubikeyUser();
        itsYubikeyCb = (CheckBox)rootView.findViewById(R.id.yubikey);
        setVisibility(R.id.yubi_help_text, false, rootView);
        itsYubiState = itsYubiMgr.getState(getActivity());
        switch (itsYubiState) {
        case UNAVAILABLE: {
            GuiUtils.setVisible(itsYubikeyCb, false);
            break;
        }
        case DISABLED: {
            itsYubikeyCb.setEnabled(false);
            itsYubikeyCb.setText(R.string.yubikey_disabled);
            break;
        }
        case ENABLED: {
            break;
        }
        }
        setVisibility(R.id.yubi_progress_text, false, rootView);

        // TODO: if saved password, don't show keyboard but keep ok button
        // behavior
        GuiUtils.setupFormKeyboard(itsPasswordEdit, itsPasswordEdit, itsOkBtn,
                                   getActivity());
        itsPasswordEdit.setPrivateImeOptions(PasswdSafeIME.PASSWDSAFE_OPEN);
        return rootView;
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
        itsSavedPasswordsMgr = new SavedPasswordsMgr(ctx);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewFileOpen();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SLOT, itsYubiSlot);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (itsYubiMgr != null) {
            itsYubiMgr.onPause();
        }
        if (itsAddSavedPasswordUser != null) {
            itsAddSavedPasswordUser.cancel();
            itsAddSavedPasswordUser = null;
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (itsYubiMgr != null) {
            itsYubiMgr.stop();
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
        itsSavedPasswordsMgr = null;
    }

    /** Handle a new intent */
    public void onNewIntent(Intent intent)
    {
        if (itsYubiMgr != null) {
            itsYubiMgr.handleKeyIntent(intent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_open_file, menu);

        switch (itsYubiState) {
        case ENABLED:
        case DISABLED: {
            break;
        }
        case UNAVAILABLE: {
            menu.setGroupVisible(R.id.menu_group_slots, false);
            MenuItem item = menu.findItem(R.id.menu_yubi_help);
            item.setVisible(false);
            break;
        }
        }

        MenuItem item;
        switch (itsYubiSlot) {
        case 2:
        default: {
            item = menu.findItem(R.id.menu_slot_2);
            itsYubiSlot = 2;
            break;
        }
        case 1: {
            item = menu.findItem(R.id.menu_slot_1);
            break;
        }
        }
        item.setChecked(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_yubi_help: {
            View root = getView();
            if (root != null) {
                View help = root.findViewById(R.id.yubi_help_text);
                help.setVisibility((help.getVisibility() == View.VISIBLE) ?
                                           View.GONE : View.VISIBLE);
            }
            return true;
        }
        case R.id.menu_slot_1: {
            item.setChecked(true);
            itsYubiSlot = 1;
            return true;
        }
        case R.id.menu_slot_2: {
            item.setChecked(true);
            itsYubiSlot = 2;
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
        case R.id.cancel: {
            Activity act = getActivity();
            GuiUtils.setKeyboardVisible(itsPasswordEdit, act, false);
            cancelFragment(true);
            break;
        }
        case R.id.ok: {
            GuiUtils.setVisible(itsSavedPasswordMsg, false);
            itsUserPassword = itsPasswordEdit.getText().toString();
            if (itsYubikeyCb.isChecked()) {
                itsYubiMgr.start(itsYubiUser);
            } else {
                startFileOpen();
            }
            break;
        }
        }
    }

    /**
     * Derived-class handler for when the resolve task is finished
     */
    @Override
    @SuppressLint("SetTextI18n")
    protected final void doResolveTaskFinished()
    {
        setTitle(R.string.open_file);
        itsPasswordEdit.setEnabled(true);
        itsOkBtn.setEnabled(true);
        //noinspection ConstantConditions
        if ((PasswdSafeApp.DEBUG_AUTO_FILE != null) &&
            (getFileUri().getPath().equals(PasswdSafeApp.DEBUG_AUTO_FILE))) {
            itsReadonlyCb.setChecked(false);
            itsYubikeyCb.setChecked(false);
            itsPasswordEdit.setText("test123");
            itsOkBtn.performClick();
        } else {
            SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
            Pair<Boolean, Integer> rc = getPasswdFileUri().isWritable();
            if (rc.first) {
                itsReadonlyCb.setChecked(
                        Preferences.getFileOpenReadOnlyPref(prefs));
            } else {
                itsReadonlyCb.setChecked(true);
                itsReadonlyCb.setEnabled(false);
                if (rc.second != null) {
                    itsReadonlyCb.setText(String.format(
                            "%s - %s", itsReadonlyCb.getText(),
                            getString(rc.second)));
                }
            }
            itsYubikeyCb.setChecked(Preferences.getFileOpenYubikeyPref(prefs));

            boolean isSaved = itsSavedPasswordsMgr.isAvailable() &&
                              itsSavedPasswordsMgr.isSaved(getFileUri());
            GuiUtils.setVisible(itsSavedPasswordMsg, isSaved);
            if (isSaved) {
                // TODO: i18n
                // TODO: start mgr for load
                itsSavedPasswordMsg.setText("Touch sensor to load saved " +
                                            "password");
            }
            itsSavePasswdCb.setChecked(isSaved);
        }
    }

    /**
     *  Derived-class handler when the fragment is canceled
     */
    @Override
    protected final void doCancelFragment(boolean userCancel)
    {
        if (itsOpenTask != null) {
            OpenTask task = itsOpenTask;
            itsOpenTask = null;
            task.cancel(false);
        }
        GuiUtils.setKeyboardVisible(itsPasswordEdit, getActivity(), false);
        if (userCancel && itsListener != null) {
            itsListener.handleFileOpenCanceled();
        }
    }

    /** Enable/disable field controls during background operations */
    @Override
    protected final void setFieldsEnabled(boolean enabled)
    {
        itsPasswordEdit.setEnabled(enabled);
        itsReadonlyCb.setEnabled(enabled);
        itsSavePasswdCb.setEnabled(enabled);
        switch (itsYubiState) {
        case ENABLED: {
            itsYubikeyCb.setEnabled(enabled);
            break;
        }
        case DISABLED: {
            itsYubikeyCb.setEnabled(false);
            break;
        }
        case UNAVAILABLE: {
            break;
        }
        }
        itsOkBtn.setEnabled(enabled);
    }

    /**
     * Start the task for opening the file
     */
    private void startFileOpen()
    {
        TextInputUtils.setTextInputError(null, itsPasswordInput);

        boolean readonly = itsReadonlyCb.isChecked();
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        Preferences.setFileOpenReadOnlyPref(readonly, prefs);
        Preferences.setFileOpenYubikeyPref(itsYubikeyCb.isChecked(), prefs);

        // TODO: show warning about saving first time
        boolean isSaved = itsSavedPasswordsMgr.isSaved(getFileUri());
        boolean doSave = itsSavePasswdCb.isChecked();
        if (isSaved && !doSave) {
            itsSaveChange = SavePasswordChange.REMOVE;
        } else if (!isSaved && doSave) {
            itsSaveChange = SavePasswordChange.ADD;
        } else {
            itsSaveChange = SavePasswordChange.NONE;
        }

        itsOpenTask = new OpenTask(
                new StringBuilder(itsPasswordEdit.getText()), readonly);
        itsOpenTask.execute();
    }

    /**
     * Handle when the open task is finished
     */
    private void openTaskFinished(OpenResult result)
    {
        if (itsOpenTask == null) {
            return;
        }
        itsOpenTask = null;

        if (itsYubiMgr != null) {
            itsYubiMgr.stop();
        }

        if (result == null) {
            cancelFragment(false);
            return;
        }

        if (result.itsFileData != null) {
            switch (itsSaveChange) {
            case ADD: {
                if (result.itsKeygenError != null) {
                    String msg = getString(
                            R.string.password_save_canceled_key_error,
                            result.itsKeygenError.getLocalizedMessage());
                    PasswdSafeUtil.showErrorMsg(msg, getContext());
                    break;
                }

                if (itsAddSavedPasswordUser != null) {
                    itsAddSavedPasswordUser.cancel();
                }
                itsAddSavedPasswordUser = new AddSavedPasswordUser(result);
                itsSavedPasswordsMgr.startPasswordAccess(
                        getFileUri(), itsAddSavedPasswordUser);
                break;
            }
            case REMOVE: {
                itsSavedPasswordsMgr.removeSavedPassword(getFileUri());
                itsListener.handleFileOpen(result.itsFileData, itsRecToOpen);
                break;
            }
            case NONE: {
                itsListener.handleFileOpen(result.itsFileData, itsRecToOpen);
                break;
            }
            }
        } else {
            Exception e = result.itsError;
            if (((e instanceof IOException) &&
                 TextUtils.equals(e.getMessage(), "Invalid password")) ||
                (e instanceof InvalidPassphraseException)) {
                if (itsRetries++ < 5) {
                    TextInputUtils.setTextInputError(
                            getString(R.string.invalid_password),
                            itsPasswordInput);

                    if (itsErrorClearingWatcher == null) {
                        itsErrorClearingWatcher = new ErrorClearingWatcher();
                        itsPasswordEdit.addTextChangedListener(
                                itsErrorClearingWatcher);
                    }
                } else {
                    PasswdSafeUtil.showFatalMsg(
                            getString(R.string.invalid_password), getActivity(),
                            false);
                }
            } else {
                String msg = e.toString();
                PasswdSafeUtil.showFatalMsg(e, msg, getActivity());
            }
        }
    }

    /**
     * Set the title
     */
    private void setTitle(int label)
    {
        String title;
        PasswdFileUri passwdFileUri = getPasswdFileUri();
        if (passwdFileUri != null) {
            title = passwdFileUri.getIdentifier(getActivity(), true);
        } else {
            title = "";
        }
        //noinspection ConstantConditions
        if (PasswdSafeApp.DEBUG_AUTO_FILE != null) {
            title += " - AUTOOPEN!!!!!";
        }
        itsTitle.setText(getActivity().getString(label, title));
    }

    /**
     * Set visibility of a field
     */
    private static void setVisibility(int id, boolean visible, View parent)
    {
        View v = parent.findViewById(id);
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Result of opening a file
     */
    private static class OpenResult
    {
        public final PasswdFileData itsFileData;
        public final Exception itsKeygenError;
        public final Exception itsError;

        /**
         * Constructor
         */
        public OpenResult(PasswdFileData fileData, Exception keygenError,
                          Exception error)
        {
            itsFileData = fileData;
            itsKeygenError = keygenError;
            itsError = error;
        }
    }

    /**
     * Background task for opening the file
     */
    private class OpenTask extends BackgroundTask<OpenResult>
    {
        private final StringBuilder itsItsPassword;
        private final boolean itsItsIsReadOnly;

        public OpenTask(StringBuilder itsPassword, boolean itsIsReadOnly)
        {
            itsItsPassword = itsPassword;
            itsItsIsReadOnly = itsIsReadOnly;
        }

        @Override
        protected OpenResult doInBackground(Void... voids)
        {
            PasswdFileData fileData =
                    new PasswdFileData(getPasswdFileUri());
            try {
                fileData.setYubikey(itsIsYubikey);
                fileData.load(itsItsPassword, itsItsIsReadOnly, getActivity());
            } catch (Exception e) {
                return new OpenResult(null, null, e);
            }

            Exception keygenError = null;
            switch (itsSaveChange) {
            case ADD: {
                try {
                    itsSavedPasswordsMgr.generateKey(getFileUri());
                } catch (InvalidAlgorithmParameterException |
                        NoSuchAlgorithmException | NoSuchProviderException e) {
                    keygenError = e;
                }
                break;
            }
            case REMOVE:
            case NONE: {
                break;
            }
            }

            return new OpenResult(fileData, keygenError, null);
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            setTitle(R.string.loading_file);
        }

        @Override
        protected void onPostExecute(OpenResult data)
        {
            super.onPostExecute(data);
            openTaskFinished(data);
        }
    }

    /**
     * User of the YubikeyMgr
     */
    private class YubikeyUser implements YubikeyMgr.User
    {
        @Override
        public Activity getActivity()
        {
            return PasswdSafeOpenFileFragment.this.getActivity();
        }

        @Override
        public String getUserPassword()
        {
            return itsUserPassword;
        }

        @Override
        public void setHashedPassword(String password)
        {
            itsIsYubikey = true;
            itsPasswordEdit.setText(password);
            startFileOpen();
        }

        @Override
        public void handleHashException(Exception e)
        {
            Activity act = getActivity();
            PasswdSafeUtil.showFatalMsg(
                    e, act.getString(R.string.yubikey_error), act);
        }

        @Override
        public int getSlotNum()
        {
            return itsYubiSlot;
        }

        @Override
        public void timerTick(int totalTime, int remainingTime)
        {
            ProgressBar progress = getProgress();
            progress.setMax(totalTime);
            progress.setProgress(remainingTime);
        }

        @Override
        public void starting()
        {
            View root = getView();
            setVisibility(R.id.yubi_progress_text, true, root);
            setProgressVisible(true, false);
            setFieldsEnabled(false);
        }

        @Override
        public void stopped()
        {
            View root = getView();
            setVisibility(R.id.yubi_progress_text, false, root);
            setProgressVisible(false, false);
            setFieldsEnabled(true);
        }
    }

    /**
     * User for adding a saved password
     */
    private final class AddSavedPasswordUser extends SavedPasswordsMgr.User
    {
        private final OpenResult itsOpenResult;
        private final int itsTextColor;
        private final CountDownTimer itsCancelTimer;
        private Runnable itsPendingAction;

        private static final String TAG = "AddSavedPasswordUser";

        /**
         * Constructor
         */
        public AddSavedPasswordUser(OpenResult result)
        {
            itsOpenResult = result;
            itsTextColor = itsSavedPasswordMsg.getCurrentHintTextColor();
            itsCancelTimer = new CountDownTimer(30 * 1000, 1 * 1000)
            {
                @Override
                public void onTick(long millisUntilFinished)
                {
                    ProgressBar progress = getProgress();
                    progress.setMax(30);
                    progress.setProgress((int)millisUntilFinished / 1000);
                }

                @Override
                public void onFinish()
                {
                    AddSavedPasswordUser.this.cancel();
                }
            };
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString)
        {
            PasswdSafeUtil.dbginfo(TAG, "error: %s", errString);
            finish(false, errString);
        }

        @Override
        public void onAuthenticationFailed()
        {
            PasswdSafeUtil.dbginfo(TAG, "failed");
            setNotificationMsg(getString(R.string.fingerprint_not_recognized));
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString)
        {
            PasswdSafeUtil.dbginfo(TAG, "help: %s", helpString);
            setNotificationMsg(helpString);
        }

        @Override
        public void onAuthenticationSucceeded(
                FingerprintManagerCompat.AuthenticationResult result)
        {
            PasswdSafeUtil.dbginfo(TAG, "success");
            Cipher cipher = result.getCryptoObject().getCipher();
            try {
                itsSavedPasswordsMgr.addSavedPassword(getFileUri(),
                                                      itsUserPassword, cipher);
                finish(true, getString(R.string.password_saved));
            } catch (IllegalBlockSizeException | BadPaddingException |
                    UnsupportedEncodingException e) {
                String msg = "Error using cipher: " + e.getLocalizedMessage();
                Log.e(TAG, msg, e);
                onAuthenticationError(0, msg);
            }
        }

        @Override
        protected boolean isEncrypt()
        {
            return true;
        }

        @Override
        protected void onStart()
        {
            PasswdSafeUtil.dbginfo(TAG, "onStart");
            itsCancelTimer.start();
            itsSavedPasswordMsg.setTextColor(itsTextColor);
            itsSavedPasswordMsg.setText(
                    R.string.touch_sensor_to_save_the_password);
            GuiUtils.setVisible(itsSavedPasswordMsg, true);
            setFieldsEnabled(false);
            setProgressVisible(true, false);
        }

        @Override
        public void onCancel()
        {
            PasswdSafeUtil.dbginfo(TAG, "onCancel");
            finish(false, getString(R.string.canceled));
        }

        /**
         * Temporarily set a notification message for the user
         */
        private void setNotificationMsg(CharSequence msg)
        {
            itsSavedPasswordMsg.setText(msg);
            doDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    itsSavedPasswordMsg.setText(
                            R.string.touch_sensor_to_save_the_password);
                }
            });
        }

        /**
         * Finish use of the saved password manager
         */
        private void finish(boolean success, CharSequence msg)
        {
            cancelPendingAction();
            setProgressVisible(false, false);
            itsCancelTimer.cancel();
            itsAddSavedPasswordUser = null;

            int textColor;
            if (success) {
                doDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        itsListener.handleFileOpen(itsOpenResult.itsFileData,
                                                   itsRecToOpen);
                    }
                });
                textColor = R.attr.textColorGreen;
            } else {
                setFieldsEnabled(true);
                textColor = R.attr.textColorError;
            }

            TypedValue value = new TypedValue();
            getContext().getTheme().resolveAttribute(textColor, value, true);
            itsSavedPasswordMsg.setTextColor(value.data);
            itsSavedPasswordMsg.setText(msg);
        }

        /**
         * Cancel a pending action
         */
        private void cancelPendingAction()
        {
            if (itsPendingAction != null) {
                itsSavedPasswordMsg.removeCallbacks(itsPendingAction);
            }
            itsPendingAction = null;
        }

        /**
         * Perform a delayed action
         */
        private void doDelayed(final Runnable action)
        {
            cancelPendingAction();
            itsPendingAction = new Runnable()
            {
                @Override
                public void run()
                {
                    action.run();
                    itsPendingAction = null;
                }
            };
            itsSavedPasswordMsg.postDelayed(itsPendingAction, 2000);
        }
    }

    /**
     * Text watcher to clear the invalid password message
     */
    private final class ErrorClearingWatcher implements TextWatcher
    {
        @Override
        public void afterTextChanged(Editable s)
        {
            TextInputUtils.setTextInputError(null, itsPasswordInput);
            itsPasswordEdit.removeTextChangedListener(itsErrorClearingWatcher);
            itsErrorClearingWatcher = null;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after)
        {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count)
        {
        }
    }
}
