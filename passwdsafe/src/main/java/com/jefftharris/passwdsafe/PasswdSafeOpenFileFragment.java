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
import android.text.TextUtils;
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

import org.pwsafe.lib.exception.InvalidPassphraseException;

import java.io.IOException;


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

    private Listener itsListener;
    private String itsRecToOpen;
    private TextView itsTitle;
    private TextView itsPasswordEdit;
    private CheckBox itsReadonlyCb;
    private Button itsYubiStartBtn;
    private Button itsOkBtn;
    private OpenTask itsOpenTask;
    private YubikeyMgr itsYubiMgr;
    private YubikeyMgr.User itsYubiUser;
    private int itsYubiSlot = 2;
    private boolean itsIsYubikey = false;

    private static final String ARG_URI = "uri";
    private static final String ARG_REC_TO_OPEN = "recToOpen";
    private static final String STATE_SLOT = "slot";


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

        itsYubiMgr = new YubikeyMgr();
        itsYubiUser = new YubikeyUser();
        itsYubiStartBtn = (Button)rootView.findViewById(R.id.yubi_start);
        itsYubiStartBtn.setOnClickListener(this);
        setVisibility(R.id.yubi_help_text, false, rootView);
        YubiState state = YubiState.UNAVAILABLE;
        if (itsYubiMgr != null) {
            state = itsYubiMgr.getState(getActivity());
        }
        boolean showDisabled = false;
        boolean showStart = false;
        switch (state) {
        case UNAVAILABLE: {
            break;
        }
        case DISABLED: {
            showDisabled = true;
            break;
        }
        case ENABLED: {
            showStart = true;
            break;
        }
        }
        setVisibility(R.id.yubi_disabled, showDisabled, rootView);
        setVisibility(R.id.yubi_start, showStart, rootView);
        setVisibility(R.id.yubi_progress_text, false, rootView);

        GuiUtils.setupFormKeyboard(itsPasswordEdit, itsPasswordEdit, itsOkBtn,
                                   getActivity());
        return rootView;
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
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

        View root = getView();
        if (root != null) {
            View yubiStart = root.findViewById(R.id.yubi_start);
            boolean startVisible = (yubiStart.getVisibility() == View.VISIBLE);
            menu.setGroupVisible(R.id.menu_group_slots, startVisible);

            View yubiDisabled = root.findViewById(R.id.yubi_disabled);
            MenuItem item = menu.findItem(R.id.menu_yubi_help);
            item.setVisible((yubiDisabled.getVisibility() == View.VISIBLE) ||
                            startVisible);
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
            boolean readonly = itsReadonlyCb.isChecked();
            SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
            Preferences.setFileOpenReadOnlyPref(readonly, prefs);

            itsOpenTask = new OpenTask(
                    new StringBuilder(itsPasswordEdit.getText()), readonly);
            itsOpenTask.execute();
            break;
        }
        case R.id.yubi_start: {
            itsYubiMgr.start(itsYubiUser);
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
            itsPasswordEdit.setText("test123");
            itsOkBtn.performClick();
        } else {
            Pair<Boolean, Integer> rc = getPasswdFileUri().isWritable();
            if (rc.first) {
                SharedPreferences prefs =
                        Preferences.getSharedPrefs(getContext());
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
        itsYubiStartBtn.setEnabled(enabled);
        itsOkBtn.setEnabled(enabled);
    }

    /**
     * Handle when the open task is finished
     */
    private void openTaskFinished(Object result)
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

        if (result instanceof PasswdFileData) {
            PasswdFileData fileData = (PasswdFileData)result;
            itsListener.handleFileOpen(fileData, itsRecToOpen);
        } else {
            Exception e = (Exception)result;
            if (((e instanceof IOException) &&
                 TextUtils.equals(e.getMessage(), "Invalid password")) ||
                (e instanceof InvalidPassphraseException)) {
                PasswdSafeUtil.showFatalMsg(
                        getString(R.string.invalid_password), getActivity(),
                        false);
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
     * Background task for opening the file
     */
    private class OpenTask extends BackgroundTask<Object>
    {
        private final StringBuilder itsItsPassword;
        private final boolean itsItsIsReadOnly;

        public OpenTask(StringBuilder itsPassword, boolean itsIsReadOnly)
        {
            itsItsPassword = itsPassword;
            itsItsIsReadOnly = itsIsReadOnly;
        }

        @Override
        protected Object doInBackground(Void... voids)
        {
            PasswdFileData fileData =
                    new PasswdFileData(getPasswdFileUri());
            try {
                fileData.setYubikey(itsIsYubikey);
                fileData.load(itsItsPassword, itsItsIsReadOnly, getActivity());
                return fileData;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            setTitle(R.string.loading_file);
        }

        @Override
        protected void onPostExecute(Object data)
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
            return itsPasswordEdit.getText().toString();
        }

        @Override
        public void setHashedPassword(String password)
        {
            itsIsYubikey = true;
            itsPasswordEdit.setText(password);
            itsOkBtn.performClick();
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
}
