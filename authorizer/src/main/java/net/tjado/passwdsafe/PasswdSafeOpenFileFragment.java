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
import android.os.Looper;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.ViewTreeObserver;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.CheckResult;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputLayout;

import net.tjado.passwdsafe.PasswdSafeOpenFileViewModel.SavedPasswordState;
import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.ActContext;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.AbstractTextWatcher;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.lib.view.TextInputUtils;
import net.tjado.passwdsafe.lib.view.TypefaceUtils;
import net.tjado.passwdsafe.util.Pair;
import net.tjado.passwdsafe.view.ConfirmPromptDialog;

import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;



/**
 * Fragment for opening a file
 */
public class PasswdSafeOpenFileFragment
        extends AbstractPasswdSafeOpenNewFileFragment
        implements Observer<PasswdSafeOpenFileViewModel.OpenData>,
                   ConfirmPromptDialog.Listener,
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
        CHECKING_YUBIKEY,
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
    private EditText itsPasswordEdit;
    private TextView itsSavedPasswordMsg;
    private int itsSavedPasswordTextColor;
    private TextView itsReadonlyMsg;
    private CheckBox itsReadonlyCb;
    private CheckBox itsSavePasswdCb;
    private CheckBox itsYubikeyCb;
    private Button itsOpenBtn;
    private SavedPasswordsMgr itsSavedPasswordsMgr;
    private SavePasswordChange itsSaveChange = SavePasswordChange.NONE;
    private LoadSavedPasswordUser itsLoadSavedPasswordUser;
    private AddSavedPasswordUser itsAddSavedPasswordUser;
    private YubikeyMgr itsYubiMgr;
    private YubikeyMgr.User itsYubiUser;
    private Phase itsPhase = Phase.INITIAL;
    private TextWatcher itsErrorClearingWatcher;

    private PasswdSafeOpenFileViewModel itsOpenModel;

    private static final String ARG_URI = "uri";
    private static final String ARG_REC_TO_OPEN = "recToOpen";
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
        PasswdSafeUtil.dbginfo(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            setFileUri(args.getParcelable(ARG_URI));
            itsRecToOpen = args.getString(ARG_REC_TO_OPEN);
        }

        setDoResolveOnStart(false);
        itsOpenModel = new ViewModelProvider(requireActivity()).get(
                PasswdSafeOpenFileViewModel.class);
        itsOpenModel.getData().observe(this, this);
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_passwdsafe_open_file,
                                         container, false);
        setupView(rootView);
        Context ctx = requireContext();

        itsTitle = rootView.findViewById(R.id.file);
        itsPasswordInput = rootView.findViewById(R.id.passwd_input);
        itsPasswordEdit = rootView.findViewById(R.id.passwd_edit);
        TypefaceUtils.setMonospace(itsPasswordEdit, ctx);
        itsPasswordEdit.setEnabled(false);

        itsReadonlyMsg = rootView.findViewById(R.id.read_only_msg);
        GuiUtils.setVisible(itsReadonlyMsg, false);
        itsOpenBtn = rootView.findViewById(R.id.open);
        itsOpenBtn.setOnClickListener(this);
        itsOpenBtn.setEnabled(false);

        itsReadonlyCb = (CheckBox)rootView.findViewById(R.id.read_only);

        itsSavedPasswordMsg = rootView.findViewById(R.id.saved_password);
        itsSavedPasswordTextColor = itsSavedPasswordMsg.getCurrentTextColor();
        itsSavePasswdCb = rootView.findViewById(R.id.save_password);
        itsSavePasswdCb.setOnCheckedChangeListener(this);
        boolean saveAvailable = itsSavedPasswordsMgr.isAvailable();
        GuiUtils.setVisible(itsSavePasswdCb, saveAvailable);
        GuiUtils.setVisible(itsSavedPasswordMsg, false);

        itsYubiMgr = new YubikeyMgr();
        itsYubikeyCb = rootView.findViewById(R.id.yubikey);
        itsYubikeyCb.setOnCheckedChangeListener(this);
        setVisibility(R.id.file_open_help_text, false, rootView);

        setVisibility(R.id.yubi_progress_text, false, rootView);

        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
        itsSavedPasswordsMgr = new SavedPasswordsMgr(ctx);
        itsSavedPasswordsMgr.attach(this);
    }

    @Override
    public void onStart()
    {
        PasswdSafeUtil.dbginfo(TAG, "onStart");
        super.onStart();

        // Reset runtime state
        itsSaveChange = SavePasswordChange.NONE;
        itsLoadSavedPasswordUser = null;
        itsAddSavedPasswordUser = null;
        itsYubiUser = null;
        itsPhase = Phase.INITIAL;
        if (itsErrorClearingWatcher != null) {
            itsPasswordEdit.removeTextChangedListener(itsErrorClearingWatcher);
            itsErrorClearingWatcher = null;
        }

        setPhase(Phase.CHECKING_YUBIKEY);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewFileOpen();
    }

    @Override
    public void onPause()
    {
        PasswdSafeUtil.dbginfo(TAG, "onPause");
        super.onPause();
        if (itsYubiMgr != null) {
            itsYubiMgr.onPause();
        }
    }

    @Override
    public void onStop()
    {
        PasswdSafeUtil.dbginfo(TAG, "onStop");
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
        if (itsSavedPasswordsMgr != null) {
            itsSavedPasswordsMgr.detach();
            itsSavedPasswordsMgr = null;
        }
    }

    /** Handle a new intent */
    public void onNewIntent(Intent intent)
    {
        if (itsYubiMgr != null) {
            itsYubiMgr.handleKeyIntent(intent);
        }
    }

    @Override
    public void onChanged(@Nullable final PasswdSafeOpenFileViewModel.OpenData openData)
    {
        if (openData != null) {
            PasswdSafeUtil.dbginfo(TAG, "onChanged phase: %s, data: %s", itsPhase, openData);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu,
                                    @NonNull MenuInflater inflater)
    {
        if ((itsListener != null) && itsListener.isNavDrawerClosed()) {
            inflater.inflate(R.menu.fragment_passwdsafe_open_file, menu);

            var data = itsOpenModel.getDataValue();
            switch (data.getYubiState()) {
                case ENABLED:
                case DISABLED: {
                    break;
                }
                case UNKNOWN:
                case UNAVAILABLE: {
                    menu.setGroupVisible(R.id.menu_group_slots, false);
                    break;
                }
            }

            MenuItem item;
            switch (data.getYubiSlot()) {
                case 2:
                default: {
                    item = menu.findItem(R.id.menu_slot_2);
                    break;
                }
                case 1: {
                    item = menu.findItem(R.id.menu_slot_1);
                    break;
                }
            }
            item.setChecked(true);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_file_open_help) {
            View root = getView();
            if (root != null) {
                View help = root.findViewById(R.id.file_open_help_text);
                help.setVisibility((help.getVisibility() == View.VISIBLE) ?
                                           View.GONE : View.VISIBLE);
                GuiUtils.setKeyboardVisible(itsPasswordEdit, requireContext(),
                                            false);
            }
            return true;
        } else if (itemId == R.id.menu_slot_1) {
            item.setChecked(true);
            itsOpenModel.setYubiSlot(1);
            return true;
        } else if (itemId == R.id.menu_slot_2) {
            item.setChecked(true);
            itsOpenModel.setYubiSlot(2);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view)
    {
        int id = view.getId();
        if (id == R.id.open) {
            if (itsYubikeyCb.isChecked()) {
                setPhase(Phase.YUBIKEY);
            } else {
                setPhase(Phase.OPENING);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked)
    {
        int id = button.getId();
        if (id == R.id.save_password) {
            if (itsSavePasswdCb.isChecked()) {
                Context ctx = getContext();
                SharedPreferences prefs = Preferences.getSharedPrefs(ctx);
                if (!Preferences.isFileSavedPasswordConfirm(prefs)) {
                    FragmentManager fragMgr = getFragmentManager();
                    if (fragMgr == null) {
                        return;
                    }
                    ConfirmPromptDialog dlg = ConfirmPromptDialog.newInstance(
                            getString(R.string.save_password_p),
                            getString(R.string.save_password_warning),
                            getString(R.string.save), null);
                    dlg.setTargetFragment(this, 0);
                    dlg.show(fragMgr, "saveConfirm");
                }
            }
        } else if (id == R.id.yubikey) {
            itsOpenModel.setYubiSelected(isChecked);
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
        boolean savePasswdAllowed = false;
        PasswdFileUri passwdFileUri = getPasswdFileUri();
        if (itsSavedPasswordsMgr.isAvailable() && (passwdFileUri != null)) {
            switch (passwdFileUri.getType()) {
            case FILE:
            case SYNC_PROVIDER:
            case GENERIC_PROVIDER: {
                savePasswdAllowed = true;
                break;
            }
            case EMAIL:
            case BACKUP: {
                break;
            }
            }
        }

        itsOpenModel.provideResolveResults(passwdFileUri, savePasswdAllowed);
        doSetFieldsEnabled(itsOpenBtn.isEnabled());
        setPhase(Phase.WAITING_PASSWORD);
    }

    /**
     *  Derived-class handler when the fragment is canceled
     */
    @Override
    protected final void doCancelFragment(boolean userCancel)
    {
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
        var openData = itsOpenModel.getDataValue();

        itsPasswordEdit.setEnabled(enabled);
        itsOpenBtn.setEnabled(enabled);
        itsSavePasswdCb.setEnabled(enabled && openData.isSaveAllowed());

        switch (openData.getYubiState()) {
        case ENABLED: {
            itsYubikeyCb.setEnabled(enabled);
            break;
        }
        case UNKNOWN:
        case UNAVAILABLE:
        case DISABLED: {
            itsYubikeyCb.setEnabled(false);
            break;
        }
        }
    }

    /**
     * Set the UI phase
     */
    private void setPhase(Phase newPhase)
    {
        if (newPhase == itsPhase) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "setPhase: %s -> %s", itsPhase, newPhase);
        switch (itsPhase) {
        case RESOLVING: {
            exitResolvingPhase();
            break;
        }
        case WAITING_PASSWORD: {
            try (Owner<PwsPassword> password =
                         PwsPassword.create(itsPasswordEdit)) {
                setOpenPassword(password.pass(), false);
            }
            break;
        }
        case YUBIKEY: {
            View root = getView();
            setVisibility(R.id.yubi_progress_text, false,
                          Objects.requireNonNull(root));
            setProgressVisible(false, false);
            setFieldsDisabled(false);
            itsYubiMgr.stop();
            break;
        }
        case SAVING_PASSWORD: {
            setFieldsDisabled(false);
            break;
        }
        case INITIAL:
        case CHECKING_YUBIKEY:
        case OPENING:
        case FINISHED: {
            break;
        }
        }

        itsPhase = newPhase;

        switch (itsPhase) {
            case CHECKING_YUBIKEY: {
                enterCheckingYubikeyPhase();
                break;
            }
            case RESOLVING: {
                enterResolvingPhase();
                break;
            }
            case WAITING_PASSWORD: {
                enterWaitingPasswordPhase();
                break;
            }
            case YUBIKEY: {
                itsYubiUser = new YubikeyUser();
                itsYubiMgr.start(itsYubiUser);
                View root = requireView();
                setVisibility(R.id.yubi_progress_text, true, root);
                setProgressVisible(true, false);
                setFieldsDisabled(true);
                break;
            }
            case OPENING: {
                enterOpeningPhase();
                break;
            }
            case SAVING_PASSWORD: {
                setFieldsDisabled(true);
                break;
            }
            case FINISHED: {
                PasswdSafeIME.resetKeyboard();
                itsOpenModel.resetData();
                break;
            }
            case INITIAL: {
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

        PasswdFileUri uri = getPasswdFileUri();
        if (uri != null) {
            Pair<Boolean, Integer> rc = uri.isWritable();
            if (!rc.first && (rc.second != null)) {
                itsReadonlyMsg.setText(getString(rc.second));
                GuiUtils.setVisible(itsReadonlyMsg, true);
            }
        }
    }

    /**
     * Enter the checking Yubikey phase
     */
    private void enterCheckingYubikeyPhase()
    {
        var openData = itsOpenModel.getDataValue();
        if (!openData.hasYubiInfo()) {
            var state = itsYubiMgr.getState(getActivity());
            var prefs = Preferences.getSharedPrefs(getContext());
            itsOpenModel.provideYubiInfo(state,
                                         Preferences.getFileOpenYubikeyPref(
                                                 prefs));
            openData = itsOpenModel.getDataValue();
        }

        switch (openData.getYubiState()) {
        case UNKNOWN:
        case UNAVAILABLE: {
            GuiUtils.setVisible(itsYubikeyCb, false);
            break;
        }
        case DISABLED: {
            GuiUtils.setVisible(itsYubikeyCb, true);
            itsYubikeyCb.setEnabled(false);
            itsYubikeyCb.setText(R.string.yubikey_disabled);
            itsYubikeyCb.setChecked(false);
            break;
        }
        case ENABLED: {
            GuiUtils.setVisible(itsYubikeyCb, true);
            itsYubikeyCb.setEnabled(true);
            itsYubikeyCb.setText(R.string.yubikey);
            itsYubikeyCb.setChecked(openData.isYubikeySelected());
            break;
        }
        }

        setPhase(Phase.RESOLVING);
    }

    /**
     * Enter the resolving phase
     */
    private void enterResolvingPhase()
    {
        var openData = itsOpenModel.getDataValue();
        if (openData.isResolved()) {
            setPasswdFileUri(openData.getUri());
            doSetFieldsEnabled(true);
            setPhase(Phase.WAITING_PASSWORD);
        } else {
            startResolve();
        }
    }

    /**
     * Enter the waiting password phase
     */
    private void enterWaitingPasswordPhase()
    {
        PasswdFileUri fileUri = getPasswdFileUri();
        if (fileUri == null) {
            cancelFragment(false);
            return;
        }

        var openData = itsOpenModel.getDataValue();

        // Check whether the password can be saved
        var savedState = openData.getSavedPasswordState();
        boolean canSave = false;
        switch (savedState) {
            case UNKNOWN: {
                switch (fileUri.getType()) {
                case FILE:
                case SYNC_PROVIDER:
                case GENERIC_PROVIDER:
                case BACKUP: {
                    canSave = itsSavedPasswordsMgr.isAvailable() &&
                              itsSavedPasswordsMgr.isSaved(getPasswdFileUri());
                    break;
                }
                case EMAIL: {
                    break;
                }
                }

                if (canSave) {
                    itsLoadSavedPasswordUser = new LoadSavedPasswordUser();
                    canSave = itsSavedPasswordsMgr.startPasswordAccess(
                            getPasswdFileUri(), itsLoadSavedPasswordUser);
                }

                itsOpenModel.setSavedPasswordState(
                        (canSave ? SavedPasswordState.AVAILABLE :
                                 SavedPasswordState.NOT_AVAILABLE), null, null);
                openData = itsOpenModel.getDataValue();

                break;
            }
            case NOT_AVAILABLE: {
                break;
            }
            case AVAILABLE:
            case LOADED_SUCCESS:
            case LOADED_FAILURE: {
                canSave = true;
                break;
            }
        }

        // Setup fields
        GuiUtils.setupFormKeyboard(canSave ? null : itsPasswordEdit,
                                   itsPasswordEdit, itsOpenBtn, getContext());
        GuiUtils.setVisible(itsSavedPasswordMsg, canSave);
        itsSavePasswdCb.setChecked(canSave);

        if (!canSave) {
            itsPasswordEdit.requestFocus();
            checkOpenDefaultFile();
        }

        // Restore loaded password
        switch (savedState) {
            case LOADED_SUCCESS:
            case LOADED_FAILURE: {
                var finish = (savedState == SavedPasswordState.LOADED_SUCCESS) ?
                        SavedPasswordFinish.SUCCESS :
                        SavedPasswordFinish.ERROR;
                try (var loadedPassword = openData.getLoadedPassword()) {
                    finishSavedPasswordAccess(true, finish,
                                              openData.getLoadedPasswordMsg(),
                                              ((loadedPassword != null) ?
                                                       loadedPassword.pass() : null), null);
                }
                break;
            }
            case UNKNOWN:
            case NOT_AVAILABLE:
            case AVAILABLE: {
                break;
            }
        }

        // Restore invalid password message after setting password
        if (openData.hasPasswordRetry()) {
            setInvalidPasswordMsg();
        }
    }

    /**
     * Enter the opening phase
     */
    private void enterOpeningPhase()
    {
        setTitle(R.string.loading_file);
        TextInputUtils.setTextInputError(null, itsPasswordInput);

        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        Preferences.setFileOpenYubikeyPref(itsYubikeyCb.isChecked(), prefs);

        var openData = itsOpenModel.getDataValue();

        boolean doSave = itsSavePasswdCb.isChecked();
        boolean readonly = itsReadonlyCb.isChecked();
        boolean passwordSaved = false;
        switch (openData.getSavedPasswordState()) {
        case UNKNOWN:
        case NOT_AVAILABLE: {
            break;
        }
        case AVAILABLE:
        case LOADED_FAILURE:
        case LOADED_SUCCESS: {
            passwordSaved = true;
            break;
        }
        }

        if (passwordSaved && !doSave) {
            itsSaveChange = SavePasswordChange.REMOVE;
        } else if (!passwordSaved && doSave) {
            itsSaveChange = SavePasswordChange.ADD;
        } else {
            itsSaveChange = SavePasswordChange.NONE;
        }

        try (var openPassword = openData.getOpenPassword()) {
            if (openPassword != null) {
                startTask(new OpenTask(openPassword.pass(),
                                       openData.isOpenYubikey(), readonly, this));
            }
        }
    }

    /**
     * Finish accessing saved passwords for loading or saving
     */
    private void finishSavedPasswordAccess(
            boolean isLoad,
            @NonNull SavedPasswordFinish finishMode,
            @Nullable CharSequence finishMsg,
            @Nullable Owner<PwsPassword>.Param loadedPassword,
            @Nullable OpenResult openResult)
    {
        GuiUtils.setVisible(itsSavedPasswordMsg, true);
        itsSavedPasswordMsg.setText(finishMsg);
        int textColor = itsSavedPasswordTextColor;
        if (finishMode == SavedPasswordFinish.SUCCESS) {
            textColor = R.attr.textColorGreen;
        } else if (finishMode == SavedPasswordFinish.ERROR) {
            textColor = R.attr.colorError;
        }

        Context ctx = getContext();
        if (ctx != null) {
            TypedValue value = new TypedValue();
            ctx.getTheme().resolveAttribute(textColor, value, true);
            textColor = value.data;
        }
        itsSavedPasswordMsg.setTextColor(textColor);

        setFieldsDisabled(false);
        if (isLoad) {
            itsLoadSavedPasswordUser = null;

            if (loadedPassword != null) {
                try (var password = loadedPassword.use()) {
                    password.get().setInto(itsPasswordEdit);
                    itsOpenBtn.performClick();
                }
            }

            // Save password state
            switch (itsOpenModel.getDataValue().getSavedPasswordState()) {
                case AVAILABLE: {
                    var newState = SavedPasswordState.AVAILABLE;
                    if (finishMode == SavedPasswordFinish.SUCCESS) {
                        newState = SavedPasswordState.LOADED_SUCCESS;
                    } else if (finishMode == SavedPasswordFinish.ERROR) {
                        newState = SavedPasswordState.LOADED_FAILURE;
                    }
                    itsOpenModel.setSavedPasswordState(newState, finishMsg, loadedPassword);
                    break;
                }
                case UNKNOWN:
                case LOADED_SUCCESS:
                case LOADED_FAILURE:
                case NOT_AVAILABLE: {
                    break;
                }
            }
        } else {
            itsAddSavedPasswordUser = null;
            if (openResult != null) {
                switch (finishMode) {
                    case SUCCESS: {
                        finishFileOpen(openResult.itsFileData);
                        break;
                    }
                    case ERROR: {
                        setPhase(Phase.WAITING_PASSWORD);
                        break;
                    }
                }
            } else {
                setPhase(Phase.WAITING_PASSWORD);
            }
        }
    }

    /**
     * Handle when the open task is finished
     */
    private void openTaskFinished(OpenResult result, Throwable error)
    {
        if (result != null) {
            switch (itsSaveChange) {
            case ADD: {
                if (result.itsKeygenError != null) {
                    String msg = getString(
                            R.string.password_save_canceled_key_error,
                            result.itsKeygenError.toString());
                    PasswdSafeUtil.showErrorMsg(msg,
                                                new ActContext(getContext()));
                    break;
                }

                itsAddSavedPasswordUser = new AddSavedPasswordUser(result);
                itsSavedPasswordsMgr.startPasswordAccess(
                        getPasswdFileUri(), itsAddSavedPasswordUser);
                setPhase(Phase.SAVING_PASSWORD);
                break;
            }
            case REMOVE: {
                itsSavedPasswordsMgr.removeSavedPassword(getPasswdFileUri());
                finishFileOpen(result.itsFileData);
                break;
            }
            case NONE: {
                finishFileOpen(result.itsFileData);
                break;
            }
            }
        } else if (error != null) {
            if (((error instanceof IOException) &&
                 TextUtils.equals(error.getMessage(), "Invalid password")) ||
                (error instanceof InvalidPassphraseException)) {
                if (itsOpenModel.checkOpenRetries()) {
                    setInvalidPasswordMsg();
                } else {
                    PasswdSafeUtil.showFatalMsg(
                            getString(R.string.invalid_password), getActivity(),
                            false);
                }
            } else {
                PasswdSafeUtil.showFatalMsg(error, error.toString(),
                                            getActivity());
            }
            setPhase(Phase.WAITING_PASSWORD);
        } else {
            cancelFragment(false);
        }
    }

    /**
     * Set an error message for an invalid password
     */
    private void setInvalidPasswordMsg()
    {
        TextInputUtils.setTextInputError(getString(R.string.invalid_password),
                                         itsPasswordInput);

        if (itsErrorClearingWatcher == null) {
            itsErrorClearingWatcher = new ErrorClearingWatcher();
            itsPasswordEdit.addTextChangedListener(itsErrorClearingWatcher);
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
        if ((PasswdSafeApp.DEBUG_AUTO_FILE != null) &&
            !PasswdSafeUtil.isTesting()) {
            title += " - AUTOOPEN!!!!!";
        }
        itsTitle.setText(getResources().getString(label, title));
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
            itsYubikeyCb.setChecked(false);
            itsPasswordEdit.setText("test123");
            itsOpenBtn.performClick();
        }
    }

    /**
     * Set the password used to open a file
     */
    private void setOpenPassword(Owner<PwsPassword>.Param password,
                                 boolean fromYubikey)
    {
        itsOpenModel.setOpenPassword(password, fromYubikey);
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
        protected final PasswdFileData itsFileData;
        protected final Exception itsKeygenError;

        /**
         * Constructor
         */
        protected OpenResult(PasswdFileData fileData, Exception keygenError)
        {
            itsFileData = fileData;
            itsKeygenError = keygenError;
        }
    }

    /**
     * Background task for opening the file
     */
    private static final class OpenTask
            extends BackgroundTask<OpenResult, PasswdSafeOpenFileFragment>
    {
        private final PasswdFileUri itsFileUri;
        private final Owner<PwsPassword> itsPassword;
        private final boolean itsIsOpenYubikey;
        private final boolean itsItsIsReadOnly;
        private final SavePasswordChange itsSaveChange;
        private final SavedPasswordsMgr itsSavedPasswordsMgr;

        /**
         * Constructor
         */
        private OpenTask(Owner<PwsPassword>.Param passwd, boolean fromYubikey, boolean readonly,
                        PasswdSafeOpenFileFragment frag)
        {
            super(frag);
            itsFileUri = frag.getPasswdFileUri();
            itsPassword = passwd.use();
            itsIsOpenYubikey = fromYubikey;
            itsItsIsReadOnly = readonly;
            itsSaveChange = frag.itsSaveChange;
            itsSavedPasswordsMgr = frag.itsSavedPasswordsMgr;
        }

        @Override
        protected OpenResult doInBackground() throws Exception
        {
            PasswdFileData fileData = new PasswdFileData(itsFileUri);
            fileData.setYubikey(itsIsOpenYubikey);
            fileData.load(itsPassword.pass(), itsItsIsReadOnly, getContext());

            Exception keygenError = null;
            switch (itsSaveChange) {
            case ADD: {
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }
                try {
                    itsSavedPasswordsMgr.generateKey(itsFileUri);
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

            return new OpenResult(fileData, keygenError);
        }

        @Override
        protected void onTaskFinished(OpenResult result,
                                      Throwable error,
                                      @NonNull PasswdSafeOpenFileFragment frag)
        {
            super.onTaskFinished(result, error, frag);
            try {
                frag.openTaskFinished(result, error);
            } finally {
                itsPassword.close();
            }
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

        @Override @CheckResult @Nullable
        public Owner<PwsPassword> getUserPassword()
        {
            return itsOpenModel.getDataValue().getOpenPassword();
        }

        @Override
        public void finish(Owner<PwsPassword>.Param password, Exception e)
        {
            boolean haveUser = (itsYubiUser != null);
            itsYubiUser = null;
            if (password != null) {
                setOpenPassword(password, true);
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
            return itsOpenModel.getDataValue().getYubiSlot();
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
        private boolean itsIsFinished = false;

        /**
         * Constructor
         */
        protected AbstractSavedPasswordUser(String tag)
        {
            itsTag = tag;
        }

        @Override
        public final void onAuthenticationError(int errorCode,
                                                @NonNull CharSequence errString)
        {
            PasswdSafeUtil.dbginfo(itsTag, "error: %s", errString);
            switch (errorCode) {
            case BiometricPrompt.ERROR_CANCELED:
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON: {
                Context ctx = getContext();
                if (ctx != null) {
                    errString = ctx.getString(R.string.canceled);
                }
                break;
            }
            case BiometricPrompt.ERROR_HW_NOT_PRESENT:
            case BiometricPrompt.ERROR_HW_UNAVAILABLE:
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
            case BiometricPrompt.ERROR_LOCKOUT:
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
            case BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL:
            case BiometricPrompt.ERROR_NO_SPACE:
            case BiometricPrompt.ERROR_TIMEOUT:
            case BiometricPrompt.ERROR_UNABLE_TO_PROCESS:
            case BiometricPrompt.ERROR_VENDOR: {
                break;
            }
            }
            finish(SavedPasswordFinish.ERROR, errString, null);
        }

        @Override
        public final void onAuthenticationFailed()
        {
            PasswdSafeUtil.dbginfo(itsTag, "failed");
        }

        /**
         * Finish access to the saved passwords
         */
        protected final void finish(
                SavedPasswordFinish finishMode,
                @NonNull CharSequence finishMsg,
                @Nullable Owner<PwsPassword>.Param loadedPassword)
        {
            if (itsIsFinished) {
                return;
            }
            itsIsFinished = true;
            handleFinish(finishMode, finishMsg, loadedPassword);
        }

        /**
         * Derived-class callback for finishing access to the saved passwords
         */
        protected abstract void handleFinish(
                SavedPasswordFinish finishMode,
                @NonNull CharSequence finishMsg,
                @Nullable Owner<PwsPassword>.Param loadedPassword);
    }

    /**
     * User for loading a saved password
     */
    private final class LoadSavedPasswordUser extends AbstractSavedPasswordUser
    {
        /**
         * Constructor
         */
        private LoadSavedPasswordUser()
        {
            super("LoadSavedPasswordUser");
            setFieldsDisabled(true);
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result)
        {
            BiometricPrompt.CryptoObject resultCrypto =
                    result.getCryptoObject();
            Cipher cipher =
                    (resultCrypto != null) ? resultCrypto.getCipher() : null;
            if ((itsSavedPasswordsMgr == null) || (cipher == null)) {
                return;
            }
            PasswdSafeUtil.dbginfo(itsTag, "success");

            try (Owner<PwsPassword> password =
                         itsSavedPasswordsMgr.loadSavedPassword(
                                 getPasswdFileUri(), cipher)) {
                finish(SavedPasswordFinish.SUCCESS,
                       getString(R.string.password_loaded), password.pass());
            } catch (IllegalBlockSizeException | BadPaddingException |
                    IOException e) {
                String msg = "Error using cipher: " + e;
                Log.e(itsTag, msg, e);
                onAuthenticationError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, msg);
            }
        }

        @Override
        protected boolean isEncrypt()
        {
            return false;
        }

        @Override
        protected void handleFinish(
                SavedPasswordFinish finishMode,
                @NonNull CharSequence finishMsg,
                @Nullable Owner<PwsPassword>.Param loadedPassword)
        {
            finishSavedPasswordAccess(true, finishMode, finishMsg,
                                      loadedPassword, null);
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
        private AddSavedPasswordUser(OpenResult result)
        {
            super("AddSavedPasswordUser");
            itsOpenResult = result;
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result)
        {
            BiometricPrompt.CryptoObject resultCrypto = result.getCryptoObject();
            Cipher cipher = (resultCrypto != null) ? resultCrypto.getCipher() : null;
            if ((itsSavedPasswordsMgr == null) || (cipher == null)) {
                return;
            }

            PasswdSafeUtil.dbginfo(itsTag, "success");
            try (Owner<PwsPassword> savePassword = PwsPassword.create(itsPasswordEdit)) {
                itsSavedPasswordsMgr.addSavedPassword(
                        getPasswdFileUri(), savePassword.pass(), cipher);
                finish(SavedPasswordFinish.SUCCESS,
                       getString(R.string.password_saved), null);
            } catch (Exception e) {
                String msg = "Error using cipher: " + e;
                Log.e(itsTag, msg, e);
                onAuthenticationError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, msg);
            }
        }

        @Override
        protected boolean isEncrypt()
        {
            return true;
        }

        @Override
        protected void handleFinish(
                SavedPasswordFinish finishMode,
                @NonNull CharSequence finishMsg,
                @Nullable Owner<PwsPassword>.Param loadedPassword)
        {
            finishSavedPasswordAccess(false, finishMode, finishMsg,
                                      loadedPassword, itsOpenResult);
        }
    }

    /**
     * Text watcher to clear the invalid password message
     */
    private final class ErrorClearingWatcher extends AbstractTextWatcher
    {
        @Override
        public void afterTextChanged(Editable s)
        {
            TextInputUtils.setTextInputError(null, itsPasswordInput);
            itsPasswordEdit.removeTextChangedListener(itsErrorClearingWatcher);
            itsErrorClearingWatcher = null;
        }
    }
}
