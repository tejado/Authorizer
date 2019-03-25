/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.CountDownTimer;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
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
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageView;
import android.view.ViewTreeObserver;
import android.graphics.drawable.Drawable;

import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.lib.view.TypefaceUtils;
import net.tjado.passwdsafe.util.Pair;
import net.tjado.passwdsafe.util.YubiState;
import net.tjado.passwdsafe.view.ConfirmPromptDialog;
import net.tjado.passwdsafe.view.TextInputUtils;

import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.util.ArrayList;


/**
 * Fragment for opening a file
 */
public class PasswdSafeOpenFileFragment
        extends AbstractPasswdSafeOpenNewFileFragment
        implements ConfirmPromptDialog.Listener,
                   View.OnClickListener, CompoundButton.OnCheckedChangeListener
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

        /** Is the navigation drawer closed */
        boolean isNavDrawerClosed();
    }

    /**
     * Phase of the UI
     */
    private enum Phase
    {
        INITIAL,
        RESOLVING,
        WAITING_PASSWORD,
        YUBIKEY,
        OPENING,
        SAVING_PASSWORD,
        FINISHED
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
    private Menu itsMenu;
    private Drawable itsOriginalDrawable;
    private String itsRecToOpen;
    private TextView itsTitle;
    private TextInputLayout itsPasswordInput;
    private TextView itsPasswordEdit;
    private TextView itsSavedPasswordMsg;
    private int itsSavedPasswordTextColor;
    private CheckBox itsReadonlyCb;
    private CheckBox itsSavePasswdCb;
    //private CheckBox itsYubikeyCb;
    private Button itsOkBtn;
    private OpenTask itsOpenTask;
    private SavedPasswordsMgr itsSavedPasswordsMgr;
    private SavePasswordChange itsSaveChange = SavePasswordChange.NONE;
    private LoadSavedPasswordUser itsLoadSavedPasswordUser;
    private AddSavedPasswordUser itsAddSavedPasswordUser;
    private YubikeyMgr itsYubiMgr;
    private YubikeyMgr.User itsYubiUser;
    private YubiState itsYubiState = YubiState.UNAVAILABLE;
    private int itsYubiSlot = 0;
    private int itsYubiSlotDefault = 2;
    private boolean itsIsYubikey = false;
    private String itsUserPassword;
    private int itsRetries = 0;
    private Phase itsPhase = Phase.INITIAL;
    private TextWatcher itsErrorClearingWatcher;

    private static final String ARG_URI = "uri";
    private static final String ARG_REC_TO_OPEN = "recToOpen";
    private static final String STATE_SLOT = "slot";
    private static final String TAG = "AuthorizerOpenFileFragment";

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

    /* http://stackoverflow.com/a/27672844
     * thanks to michalbrz <http://stackoverflow.com/users/2707179/michalbrz> */
    public void setOverflowButton(final Activity activity) {
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<View>();
                decorView.findViewsWithText(outViews, overflowDescription,
                                            View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);

                itsOriginalDrawable = overflow.getDrawable();

                overflow.setImageDrawable(PasswdSafeUtil.scaleImage(activity.getResources().getDrawable(R.drawable.icon_yubico), 0.09f, getResources()));
                overflow.setColorFilter(activity.getResources().getColor(R.color.menu_icon_color));
                removeOnGlobalLayoutListener(decorView,this);
            }
        });
    }

    public void resetOverflowButton(final Activity activity) {
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<View>();
                decorView.findViewsWithText(outViews, overflowDescription,
                                            View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);

                overflow.setImageDrawable(itsOriginalDrawable);
                removeOnGlobalLayoutListener(decorView,this);
            }
        });
    }

    public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        }
        else {
            v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
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
        itsPasswordEdit.setEnabled(false);

        itsReadonlyCb = (CheckBox)rootView.findViewById(R.id.read_only);
        itsOkBtn = (Button)rootView.findViewById(R.id.ok);
        itsOkBtn.setOnClickListener(this);
        itsOkBtn.setEnabled(false);

        itsSavedPasswordMsg =
                (TextView)rootView.findViewById(R.id.saved_password);
        itsSavedPasswordTextColor = itsSavedPasswordMsg.getCurrentTextColor();
        itsSavePasswdCb = (CheckBox)rootView.findViewById(R.id.save_password);
        itsSavePasswdCb.setOnCheckedChangeListener(this);
        boolean saveAvailable = itsSavedPasswordsMgr.isAvailable();
        GuiUtils.setVisible(itsSavePasswdCb, saveAvailable);
        GuiUtils.setVisible(itsSavedPasswordMsg, false);

        itsYubiMgr = new YubikeyMgr();
        //itsYubikeyCb = (CheckBox)rootView.findViewById(R.id.yubikey);
        itsYubiState = itsYubiMgr.getState(getActivity());

        setVisibility(R.id.yubi_progress_text, false, rootView);

        return rootView;
    }

    private void setYubikeyState(boolean state, int slot) {
        itsMenu.setGroupEnabled(R.id.menu_group_slots, state);

        itsYubiSlot = slot;

        MenuItem item;
        switch (itsYubiSlot) {
            case 1: {
                item = itsMenu.findItem(R.id.menu_slot_1);
                break;
            }
            case 2: {
                item = itsMenu.findItem(R.id.menu_slot_2);
                break;
            }
            default: {
                item = itsMenu.findItem(R.id.menu_slot_none);
            }
        }
        item.setChecked(true);
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
        itsSavedPasswordsMgr = new SavedPasswordsMgr(ctx);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        setPhase(Phase.RESOLVING);
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
        cancelSavedPasswordUsers();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (itsYubiMgr != null) {
            itsYubiMgr.stop();
        }
        setPhase(Phase.INITIAL);

        resetOverflowButton(getActivity());
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
        itsMenu = menu;

        if ((itsListener != null) && itsListener.isNavDrawerClosed()) {
            inflater.inflate(R.menu.fragment_passwdsafe_open_file, menu);

            switch (itsYubiState) {
                case ENABLED:
                    setYubikeyState(true, itsYubiSlot);
                    PasswdSafeUtil.dbginfo(TAG, "YUBIKEY ENABLED");
                    //setOverflowButton(getActivity());
                    break;
                case DISABLED: {
                    setYubikeyState(false, 0);
                    PasswdSafeUtil.dbginfo(TAG, "YUBIKEY DISABLED");
                    //setOverflowButton(getActivity());
                    break;
                }
                case UNAVAILABLE: {
                    setYubikeyState(false, -1);
                    //menu.setGroupVisible(R.id.menu_group_slots, false);
                    PasswdSafeUtil.dbginfo(TAG, "YUBIKEY UNAVAILABLE");
                    break;
                }
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.menu_file_open_help: {
                View root = getView();
                if (root != null) {
                    GuiUtils.setKeyboardVisible(itsPasswordEdit, getContext(), false);
                    PasswdSafeUtil.showInfoMsg(getResources().getString(R.string.file_open_help), getContext());
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
            case R.id.menu_slot_none: {
                item.setChecked(true);
                itsYubiSlot = 0;
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
            case R.id.ok: {
                if (itsYubiSlot > 0) {
                    setPhase(Phase.YUBIKEY);
                } else {
                    setPhase(Phase.OPENING);
                }
                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked)
    {
        switch (button.getId()) {
        case R.id.save_password: {
            if (itsSavePasswdCb.isChecked()) {
                Context ctx = getContext();
                SharedPreferences prefs = Preferences.getSharedPrefs(ctx);
                if (!Preferences.isFileSavedPasswordConfirm(prefs)) {
                    ConfirmPromptDialog dlg = ConfirmPromptDialog.newInstance(
                            getString(R.string.save_password_p),
                            getString(R.string.save_password_warning),
                            getString(R.string.save), null);
                    dlg.setTargetFragment(this, 0);
                    dlg.show(getFragmentManager(), "saveConfirm");
                }
            }
            break;
        }
        }
    }

    @Override
    public void promptCanceled()
    {
        itsSavePasswdCb.setChecked(false);
    }

    @Override
    public void promptConfirmed(Bundle confirmArgs)
    {
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        Preferences.setFileSavedPasswordConfirmed(prefs);
    }

    @Override
    protected final void doResolveTaskFinished()
    {
        setPhase(Phase.WAITING_PASSWORD);
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
        Context ctx = getContext();
        if (ctx != null) {
            GuiUtils.setKeyboardVisible(itsPasswordEdit, ctx, false);
        }
        if (userCancel && itsListener != null) {
            itsListener.handleFileOpenCanceled();
        }
    }

    /**
     * Derived-class handler to enable/disable field controls during
     * background operations
     */
    @Override
    protected final void doSetFieldsEnabled(boolean enabled)
    {
        itsPasswordEdit.setEnabled(enabled);
        itsOkBtn.setEnabled(enabled);

        boolean readonlyEnabled = enabled;
        boolean savePasswdEnabled = enabled;
        PasswdFileUri passwdFileUri = getPasswdFileUri();
        if (enabled && (passwdFileUri != null)) {
            Pair<Boolean, Integer> rc = getPasswdFileUri().isWritable();
            readonlyEnabled = rc.first;

            switch (passwdFileUri.getType()) {
                case EMAIL:
                case GENERIC_PROVIDER: {
                    savePasswdEnabled = false;
                    break;
                }
                case FILE:
                case SYNC_PROVIDER: {
                    break;
                }
            }
        }
        itsReadonlyCb.setEnabled(readonlyEnabled);
        itsSavePasswdCb.setEnabled(savePasswdEnabled);

        /*
        switch (itsYubiState) {
            case ENABLED: {
                itsMenu.setGroupEnabled(R.id.menu_group_slots, enabled);
                break;
            }
            case DISABLED: {
                itsMenu.setGroupEnabled(R.id.menu_group_slots, false);
                break;
            }
            case UNAVAILABLE: {
                break;
            }
        }
        */
    }

    /**
     * Set the UI phase
     */
    private void setPhase(Phase newPhase)
    {
        if (newPhase == itsPhase) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "setPhase: %s", newPhase);
        switch (itsPhase) {
            case RESOLVING: {
                exitResolvingPhase();
                break;
            }
            case WAITING_PASSWORD: {
                itsUserPassword = itsPasswordEdit.getText().toString();
                setProgressVisible(false, false);
                cancelSavedPasswordUsers();
                break;
            }
            case YUBIKEY: {
                View root = getView();
                setVisibility(R.id.yubi_progress_text, false, root);
                setProgressVisible(false, false);
                setFieldsDisabled(true);
                itsYubiMgr.stop();
                break;
            }
            case SAVING_PASSWORD: {
                setProgressVisible(false, false);
                setFieldsDisabled(true);
                cancelSavedPasswordUsers();
                break;
            }
            case INITIAL:
            case OPENING:
            case FINISHED: {
                break;
            }
        }

        itsPhase = newPhase;

        switch (itsPhase) {
            case WAITING_PASSWORD: {
                enterWaitingPasswordPhase();
                break;
            }
            case YUBIKEY: {
                itsYubiUser = new YubikeyUser();
                itsYubiMgr.start(itsYubiUser);
                View root = getView();
                setVisibility(R.id.yubi_progress_text, true, root);
                setProgressVisible(true, false);
                setFieldsDisabled(false);
                break;
            }
            case OPENING: {
                enterOpeningPhase();
                break;
            }
            case SAVING_PASSWORD: {
                setFieldsDisabled(false);
                setProgressVisible(true, false);
                break;
            }
            case FINISHED: {
                PasswdSafeIME.resetKeyboard();
                break;
            }
            case INITIAL:
            case RESOLVING: {
                break;
            }
        }
    }

    /**
     * Exit the resolving phase
     */
    private void exitResolvingPhase()
    {
        setTitle(R.string.open_file);

        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        PasswdFileUri uri = getPasswdFileUri();
        if (uri != null) {
            Pair<Boolean, Integer> rc = uri.isWritable();
            if (rc.first) {
                itsReadonlyCb.setChecked(
                        Preferences.getFileOpenReadOnlyPref(prefs));
            } else {
                itsReadonlyCb.setChecked(true);
                if (rc.second != null) {
                    itsReadonlyCb.setText(String.format(
                            "%s - %s", itsReadonlyCb.getText(),
                            getString(rc.second)));
                }
            }
        }

        switch (itsYubiState) {
            case ENABLED: {
                itsYubiSlot = Preferences.getFileOpenYubikeyPref(prefs) ? itsYubiSlotDefault : 0;
                break;
            }
            case DISABLED:
            case UNAVAILABLE: {
                itsYubiSlot = -1;
                break;
            }
        }
    }

    /**
     * Enter the waiting password phase
     */
    private void enterWaitingPasswordPhase()
    {
        boolean isSaved = false;
        switch (getPasswdFileUri().getType()) {
        case FILE:
        case SYNC_PROVIDER: {
            isSaved = itsSavedPasswordsMgr.isAvailable() &&
                      itsSavedPasswordsMgr.isSaved(getFileUri());
        }
        case EMAIL:
        case GENERIC_PROVIDER: {
            break;
        }
        }

        GuiUtils.setupFormKeyboard(isSaved ? null : itsPasswordEdit,
                                   itsPasswordEdit, itsOkBtn, getContext());
        GuiUtils.setVisible(itsSavedPasswordMsg, isSaved);
        if (isSaved) {
            cancelSavedPasswordUsers();
            setProgressVisible(true, false);
            itsLoadSavedPasswordUser = new LoadSavedPasswordUser();
            itsSavedPasswordsMgr.startPasswordAccess(
                    getFileUri(), itsLoadSavedPasswordUser);
        } else {
            itsPasswordEdit.requestFocus();
            checkOpenDefaultFile();
        }
        itsSavePasswdCb.setChecked(isSaved);
    }

    /**
     * Enter the opening phase
     */
    private void enterOpeningPhase()
    {
        setTitle(R.string.loading_file);
        TextInputUtils.setTextInputError(null, itsPasswordInput);

        boolean readonly = itsReadonlyCb.isChecked();
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        Preferences.setFileOpenReadOnlyPref(readonly, prefs);
        Preferences.setFileOpenYubikeyPref((itsYubiSlot > 0), prefs);

        boolean isSaved = itsSavedPasswordsMgr.isSaved(getFileUri());
        boolean doSave = itsSavePasswdCb.isChecked();
        if (isSaved && !doSave) {
            itsSaveChange = SavePasswordChange.REMOVE;
        } else if (!isSaved && doSave) {
            itsSaveChange = SavePasswordChange.ADD;
        } else {
            itsSaveChange = SavePasswordChange.NONE;
        }

        Owner<PwsPassword> passwd =
                new Owner<>(new PwsPassword(itsPasswordEdit.getText()));
        try {
            itsOpenTask = new OpenTask(passwd.pass(), readonly);
            itsOpenTask.execute();
        } finally {
            passwd.close();
        }
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

                cancelSavedPasswordUsers();
                itsAddSavedPasswordUser = new AddSavedPasswordUser(result);
                itsSavedPasswordsMgr.startPasswordAccess(
                        getFileUri(), itsAddSavedPasswordUser);
                setPhase(Phase.SAVING_PASSWORD);
                break;
            }
            case REMOVE: {
                itsSavedPasswordsMgr.removeSavedPassword(getFileUri());
                finishFileOpen(result.itsFileData);
                break;
            }
            case NONE: {
                finishFileOpen(result.itsFileData);
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
            setPhase(Phase.WAITING_PASSWORD);
        }
    }

    /**
     *  Finish the file open fragment
     */
    private void finishFileOpen(PasswdFileData fileData)
    {
        setPhase(Phase.FINISHED);
        itsListener.handleFileOpen(fileData, itsRecToOpen);
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
     * Cancel saved password operations
     */
    private void cancelSavedPasswordUsers()
    {
        if (itsLoadSavedPasswordUser != null) {
            itsLoadSavedPasswordUser.cancel();
            itsLoadSavedPasswordUser = null;
        }

        if (itsAddSavedPasswordUser != null) {
            itsAddSavedPasswordUser.cancel();
            itsAddSavedPasswordUser = null;
        }
    }

    /**
     * Check for opening default file
     */
    @SuppressLint("SetTextI18n")
    private void checkOpenDefaultFile()
    {
        //noinspection ConstantConditions
        if ((PasswdSafeApp.DEBUG_AUTO_FILE != null) &&
            (getFileUri().getPath().equals(PasswdSafeApp.DEBUG_AUTO_FILE))) {
            itsReadonlyCb.setChecked(false);
            setYubikeyState(false, 0);
            itsPasswordEdit.setText("test123");
            itsOkBtn.performClick();
        }
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
        private final Owner<PwsPassword> itsPassword;
        private final boolean itsItsIsReadOnly;

        public OpenTask(Owner<PwsPassword>.Param passwd,
                        boolean itsIsReadOnly)
        {
            itsPassword = passwd.use();
            itsItsIsReadOnly = itsIsReadOnly;
        }

        @Override
        protected OpenResult doInBackground(Void... voids)
        {
            PasswdFileData fileData =
                    new PasswdFileData(getPasswdFileUri());
            try {
                fileData.setYubikey(itsIsYubikey);
                fileData.load(itsPassword.pass(), itsItsIsReadOnly,
                              getActivity());
            } catch (Exception e) {
                return new OpenResult(null, null, e);
            }

            Exception keygenError = null;
            switch (itsSaveChange) {
            case ADD: {
                try {
                    itsSavedPasswordsMgr.generateKey(getFileUri());
                } catch (InvalidAlgorithmParameterException |
                        NoSuchAlgorithmException | NoSuchProviderException |
                        IOException e) {
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
        protected void onPostExecute(OpenResult data)
        {
            super.onPostExecute(data);
            openTaskFinished(data);
            itsPassword.close();
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
        public void finish(String password, Exception e)
        {
            boolean haveUser = (itsYubiUser != null);
            itsYubiUser = null;
            if (password != null) {
                itsIsYubikey = true;
                itsPasswordEdit.setText(password);
                setPhase(Phase.OPENING);
            } else if (e != null) {
                Activity act = getActivity();
                PasswdSafeUtil.showFatalMsg(
                        e, act.getString(R.string.yubikey_error), act);
            } else if (haveUser) {
                setPhase(Phase.WAITING_PASSWORD);
            }
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
    }

    /**
     * How a saved password access is finished
     */
    private enum SavedPasswordFinish
    {
        /** Success */
        SUCCESS,
        /** Fragment canceled, no delayed operations should be added */
        FRAGMENT_CANCEL,
        /** Timeout */
        TIMEOUT,
        /** Error */
        ERROR
    }

    /**
     * Base user for accessing a saved password
     */
    private abstract class AbstractSavedPasswordUser
            extends SavedPasswordsMgr.User
    {
        protected final String itsTag;
        private final int itsStartMsgId;
        private final CountDownTimer itsCancelTimer;
        private Runnable itsPendingAction;
        private boolean itsIsCancelTimeout = false;
        private boolean itsIsFinished = false;

        /**
         * Constructor
         */
        protected AbstractSavedPasswordUser(int startMsgId, String tag)
        {
            itsTag = tag;
            itsStartMsgId = startMsgId;
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
                    getProgress().setProgress(0);
                    itsIsCancelTimeout = true;
                    AbstractSavedPasswordUser.this.cancel();
                }
            };
        }

        @Override
        public final void onAuthenticationError(int errMsgId,
                                                CharSequence errString)
        {
            PasswdSafeUtil.dbginfo(itsTag, "error: %s", errString);
            finish(SavedPasswordFinish.ERROR, errString);
        }

        @Override
        public final void onAuthenticationFailed()
        {
            PasswdSafeUtil.dbginfo(itsTag, "failed");
            setNotificationMsg(getString(R.string.fingerprint_not_recognized),
                               itsStartMsgId);
        }

        @Override
        public final void onAuthenticationHelp(int helpMsgId,
                                               CharSequence helpString)
        {
            PasswdSafeUtil.dbginfo(itsTag, "help: %s", helpString);
            setNotificationMsg(helpString, itsStartMsgId);
        }

        @Override
        protected final void onStart()
        {
            PasswdSafeUtil.dbginfo(itsTag, "onStart");
            itsCancelTimer.start();
            itsSavedPasswordMsg.setTextColor(itsSavedPasswordTextColor);
            itsSavedPasswordMsg.setText(itsStartMsgId);
            GuiUtils.setVisible(itsSavedPasswordMsg, true);
        }

        @Override
        public final void onCancel()
        {
            PasswdSafeUtil.dbginfo(itsTag, "onCancel");
            finish(itsIsCancelTimeout ? SavedPasswordFinish.TIMEOUT :
                                        SavedPasswordFinish.FRAGMENT_CANCEL,
                   getString(R.string.canceled));
        }

        /**
         * Finish access to the saved passwords
         */
        protected final void finish(SavedPasswordFinish finishMode,
                                    CharSequence msg)
        {
            cancelPendingAction();
            if (itsIsFinished) {
                return;
            }
            itsIsFinished = true;
            itsCancelTimer.cancel();

            GuiUtils.setVisible(itsSavedPasswordMsg, true);
            itsSavedPasswordMsg.setText(msg);
            int textColor = itsSavedPasswordTextColor;
            boolean resolve = false;
            switch (finishMode) {
            case SUCCESS: {
                textColor = R.attr.textColorGreen;
                resolve = true;
                break;
            }
            case ERROR: {
                textColor = R.attr.colorError;
                resolve = true;
                break;
            }
            case FRAGMENT_CANCEL:
            case TIMEOUT: {
                break;
            }
            }

            if (resolve) {
                TypedValue value = new TypedValue();
                getContext().getTheme().resolveAttribute(textColor, value,
                                                         true);
                textColor = value.data;
            }
            itsSavedPasswordMsg.setTextColor(textColor);
            handleFinish(finishMode);

            switch (finishMode) {
            case FRAGMENT_CANCEL: {
                cancelPendingAction();
                break;
            }
            case SUCCESS:
            case ERROR:
            case TIMEOUT: {
                break;
            }
            }
        }

        /**
         * Derived-class callback for finishing access to the saved passwords
         */
        protected abstract void handleFinish(SavedPasswordFinish finishMode);

        /**
         * Perform a delayed action
         */
        protected final void doDelayed(final Runnable action)
        {
            cancelPendingAction();
            itsPendingAction = new Runnable()
            {
                @Override
                public void run()
                {
                    action.run();
                    itsPendingAction = null;
                    itsSavedPasswordMsg.setTextColor(itsSavedPasswordTextColor);
                }
            };
            itsSavedPasswordMsg.postDelayed(itsPendingAction, 2000);
        }

        /**
         * Temporarily set a notification message for the user
         */
        private void setNotificationMsg(CharSequence msg, final int baseMsgId)
        {
            itsSavedPasswordMsg.setText(msg);
            doDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    itsSavedPasswordMsg.setText(baseMsgId);
                }
            });
        }

        /**
         * Cancel a pending action
         */
        private void cancelPendingAction()
        {
            if (itsPendingAction != null) {
                itsSavedPasswordMsg.removeCallbacks(itsPendingAction);
                itsPendingAction = null;
            }
        }
    }

    /**
     * User for loading a saved password
     */
    private final class LoadSavedPasswordUser extends AbstractSavedPasswordUser
    {
        /**
         * Constructor
         */
        public LoadSavedPasswordUser()
        {
            super(R.string.touch_sensor_to_load_saved_password,
                  "LoadSavedPasswordUser");
        }

        @Override
        public void onAuthenticationSucceeded(
                FingerprintManagerCompat.AuthenticationResult result)
        {
            PasswdSafeUtil.dbginfo(itsTag, "success");
            Cipher cipher = result.getCryptoObject().getCipher();
            try {
                String password = itsSavedPasswordsMgr.loadSavedPassword(
                        getFileUri(), cipher);
                itsPasswordEdit.setText(password);
                finish(SavedPasswordFinish.SUCCESS,
                       getString(R.string.password_loaded));
            } catch (IllegalBlockSizeException | BadPaddingException |
                    IOException e) {
                String msg = "Error using cipher: " + e.getLocalizedMessage();
                Log.e(itsTag, msg, e);
                onAuthenticationError(0, msg);
            }
        }

        @Override
        protected boolean isEncrypt()
        {
            return false;
        }

        @Override
        protected void handleFinish(SavedPasswordFinish finishMode)
        {
            switch (finishMode) {
            case SUCCESS: {
                doDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        itsLoadSavedPasswordUser = null;
                        if (itsYubiSlot > 0) {
                            setPhase(Phase.YUBIKEY);
                        } else {
                            setPhase(Phase.OPENING);
                        }
                    }
                });
                break;
            }
            case ERROR:
            case FRAGMENT_CANCEL:
            case TIMEOUT: {
                itsLoadSavedPasswordUser = null;
                setProgressVisible(false, false);
                break;
            }
            }
        }
    }

    /**
     * User for adding a saved password
     */
    private final class AddSavedPasswordUser extends AbstractSavedPasswordUser
    {
        private final OpenResult itsOpenResult;

        /**
         * Constructor
         */
        public AddSavedPasswordUser(OpenResult result)
        {
            super(R.string.touch_sensor_to_save_the_password,
                  "AddSavedPasswordUser");
            itsOpenResult = result;
        }

        @Override
        public void onAuthenticationSucceeded(
                FingerprintManagerCompat.AuthenticationResult result)
        {
            PasswdSafeUtil.dbginfo(itsTag, "success");
            Cipher cipher = result.getCryptoObject().getCipher();
            try {
                itsSavedPasswordsMgr.addSavedPassword(getFileUri(),
                                                      itsUserPassword, cipher);
                finish(SavedPasswordFinish.SUCCESS,
                       getString(R.string.password_saved));
            } catch (IllegalBlockSizeException | BadPaddingException |
                    UnsupportedEncodingException e) {
                String msg = "Error using cipher: " + e.getLocalizedMessage();
                Log.e(itsTag, msg, e);
                onAuthenticationError(0, msg);
            }
        }

        @Override
        protected boolean isEncrypt()
        {
            return true;
        }

        @Override
        protected void handleFinish(SavedPasswordFinish finishMode)
        {
            switch (finishMode) {
            case SUCCESS: {
                doDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        itsAddSavedPasswordUser = null;
                        finishFileOpen(itsOpenResult.itsFileData);
                    }
                });
                break;
            }
            case ERROR:
            case TIMEOUT: {
                doDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        itsAddSavedPasswordUser = null;
                        setPhase(Phase.WAITING_PASSWORD);
                    }
                });
                break;
            }
            case FRAGMENT_CANCEL: {
                itsAddSavedPasswordUser = null;
                break;
            }
            }
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
