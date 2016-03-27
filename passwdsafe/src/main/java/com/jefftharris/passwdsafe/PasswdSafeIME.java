/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileDataUser;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.lib.ObjectHolder;
import com.jefftharris.passwdsafe.util.Pair;

/**
 *  Input method for selecting fields from a record
 *
 *  @author Jeff Harris
 */
public class PasswdSafeIME extends InputMethodService
{
    public static final int ENTER_KEY = -13;
    public static final int KEYBOARD_NEXT_KEY = -25;
    public static final int KEYBOARD_CHOOSE_KEY = -26;

    private static final int USER_KEY = -1;
    private static final int URL_KEY = -2;
    private static final int BACK_KEY = -3;
    private static final int PASSWORD_KEY = -11;
    private static final int EMAIL_KEY = -12;
    private static final int TITLE_KEY = -21;
    private static final int NOTES_KEY = -22;
    private static final int PASSWDSAFE_KEY = -24;

    private KeyboardView itsKeyboardView;
    private PasswdSafeIMEKeyboard itsPasswdKeyboard;
    private PasswdSafeIMEKeyboard itsCurrKeyboard;
    private TextView itsFile;
    private View itsRecordLabel;
    private TextView itsRecord;
    private View itsPasswordWarning;
    private boolean itsAllowPassword = false;
    private boolean itsIsPasswordField = false;

    // TODO: when launching passwdsafe to get file/record, close passwdsafe once
    // one is chosen to get back to previous activity

    @Override
    public void onInitializeInterface()
    {
        PasswdSafeUtil.dbginfo("foo", "onInitializeInterface");
        itsPasswdKeyboard = new PasswdSafeIMEKeyboard(this, R.xml.keyboard);
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView()
    {
        PasswdSafeUtil.dbginfo("foo", "onCreateInputView");
        View view = getLayoutInflater().inflate(R.layout.input_method, null);

        itsKeyboardView = (KeyboardView)view.findViewById(R.id.keyboard);
        itsKeyboardView.setPreviewEnabled(false);
        itsKeyboardView.setKeyboard(itsPasswdKeyboard);
        itsKeyboardView.setOnKeyboardActionListener(new KeyboardListener());

        itsFile = (TextView)view.findViewById(R.id.file);
        itsRecordLabel = view.findViewById(R.id.record_label);
        itsRecord = (TextView)view.findViewById(R.id.record);
        itsPasswordWarning = view.findViewById(R.id.password_warning);

        return view;
    }

    @Override
    public void onStartInput(EditorInfo info, boolean restarting)
    {
        PasswdSafeUtil.dbginfo("foo", "onStartInput");
        super.onStartInput(info, restarting);

        // TODO: choose right starting keyboard...
        itsCurrKeyboard = itsPasswdKeyboard;
        itsCurrKeyboard.setOptions(info, getResources());
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting)
    {
        PasswdSafeUtil.dbginfo("foo", "onStartInputView");
        super.onStartInputView(info, restarting);
        refresh(null);

        itsIsPasswordField = false;
        switch (info.inputType & InputType.TYPE_MASK_CLASS) {
        case InputType.TYPE_CLASS_NUMBER: {
            switch (info.inputType & InputType.TYPE_MASK_VARIATION) {
            case 0x10 /* TYPE_NUMBER_VARIATION_PASSWORD in API 11 */: {
                itsIsPasswordField = true;
                break;
            }
            }
            break;
        }
        case InputType.TYPE_CLASS_TEXT: {
            switch (info.inputType & InputType.TYPE_MASK_VARIATION) {
            case InputType.TYPE_TEXT_VARIATION_PASSWORD:
            case 0xE0 /* TYPE_TEXT_VARIATION_WEB_PASSWORD in API 11 */: {
                itsIsPasswordField = true;
                break;
            }
            }
            break;
        }
        }
        itsAllowPassword = itsIsPasswordField;
        showPasswordWarning(false);

        // Reset keyboard to reflect key changes
        itsKeyboardView.setKeyboard(itsCurrKeyboard);
        itsKeyboardView.closing();
        itsKeyboardView.invalidateAllKeys();
    }

    @Override
    public void onFinishInput()
    {
        PasswdSafeUtil.dbginfo("foo", "onFinishInput");
        super.onFinishInput();

        itsCurrKeyboard = itsPasswdKeyboard;
        if (itsKeyboardView != null) {
            itsKeyboardView.closing();
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode()
    {
        // Don't want to enter full-screen mode as not a real keyboard
        return false;
    }

    /**
     * Open PasswdSafe
     */
    private void openPasswdSafe()
    {
        final ObjectHolder<Pair<Intent, Boolean>> rc = new ObjectHolder<>();
        refresh(new RefreshUser()
        {
            @Override
            public void refresh(@Nullable PasswdFileData fileData,
                                @Nullable PwsRecord rec)
            {
                Intent intent;
                if (fileData == null) {
                    intent = PasswdSafeUtil.getMainActivityIntent(
                            "com.jefftharris.passwdsafe", PasswdSafeIME.this);
                    if (intent == null) {
                        return;
                    }
                    intent.putExtra(FileListActivity.INTENT_EXTRA_CLOSE_ON_OPEN,
                                    true);
                } else {
                    String uuid = null;
                    if (rec != null) {
                        uuid = fileData.getUUID(rec);
                    }
                    intent = PasswdSafeUtil.createOpenIntent(
                            fileData.getUri().getUri(), uuid);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                }
                rc.set(new Pair<>(intent, (fileData != null)));
            }
        });
        if (rc.get() != null) {
            if (!rc.get().second) {
                InputMethodManager inputMgr = (InputMethodManager)
                        getSystemService(INPUT_METHOD_SERVICE);
                GuiUtils.switchToLastInputMethod(inputMgr, getToken());
            }
            startActivity(rc.get().first);
        }
    }

    /**
     * Handle a press of a keyboard key
     */
    private void onKeyPress(final int keycode)
    {
        InputConnection conn = getCurrentInputConnection();
        if (conn == null) {
            return;
        }

        switch (keycode) {
        case PASSWORD_KEY: {
            break;
        }
        default: {
            itsAllowPassword = itsIsPasswordField;
            showPasswordWarning(false);
            break;
        }
        }

        switch (keycode) {
        case USER_KEY:
        case PASSWORD_KEY:
        case TITLE_KEY:
        case URL_KEY:
        case EMAIL_KEY:
        case NOTES_KEY: {
            final ObjectHolder<String> keyStr = new ObjectHolder<>();
            refresh(new RefreshUser()
            {
                @Override
                public void refresh(@Nullable PasswdFileData fileData,
                                    @Nullable PwsRecord rec)
                {
                    if ((fileData == null) || (rec == null)) {
                        return;
                    }
                    switch (keycode) {
                    case USER_KEY: {
                        keyStr.set(fileData.getUsername(rec));
                        break;
                    }
                    case PASSWORD_KEY: {
                        showPasswordWarning(!itsAllowPassword);
                        if (itsAllowPassword) {
                            keyStr.set(fileData.getPassword(rec));
                            itsAllowPassword = itsIsPasswordField;
                        } else {
                            itsAllowPassword = true;
                        }
                        break;
                    }
                    case TITLE_KEY: {
                        keyStr.set(fileData.getTitle(rec));
                        break;
                    }
                    case URL_KEY: {
                        keyStr.set(fileData.getURL(rec));
                        break;
                    }
                    case EMAIL_KEY: {
                        keyStr.set(fileData.getEmail(rec));
                        break;
                    }
                    case NOTES_KEY: {
                        keyStr.set(fileData.getNotes(rec));
                        break;
                    }
                    }
                }
            });
            String str = keyStr.get();
            if (str != null) {
                conn.commitText(str, 1);
            }
            break;
        }
        case BACK_KEY: {
            conn.deleteSurroundingText(1, 0);
            break;
        }
        case ENTER_KEY: {
            sendKeyChar('\n');
            break;
        }
        case PASSWDSAFE_KEY: {
            openPasswdSafe();
            break;
        }
        case KEYBOARD_NEXT_KEY: {
            InputMethodManager inputMgr =
                    (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            IBinder token = getToken();
            if (!ApiCompat.shouldOfferSwitchingToNextInputMethod(inputMgr,
                                                                 token) ||
                !ApiCompat.switchToNextInputMethod(inputMgr, token, false)) {
                inputMgr.showInputMethodPicker();
            }
            break;
        }
        case KEYBOARD_CHOOSE_KEY: {
            InputMethodManager inputMgr =
                    (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            inputMgr.showInputMethodPicker();
            break;
        }
        default: {
            sendKeyChar((char)keycode);
            break;
        }
        }
    }

    /**
     * Refresh the fields from the current password data
     * @param user The user callback to handle the refresh.  Called even if
     *             there is no file data
     */
    private void refresh(@Nullable final RefreshUser user)
    {
        final ObjectHolder<Pair<String, String>> labels = new ObjectHolder<>();
        PasswdSafeFileDataFragment.useOpenFileData(new PasswdFileDataUser()
        {
            @Override
            public void useFileData(@NonNull PasswdFileData fileData)
            {
                String fileLabel = fileData.getUri().getIdentifier(
                        PasswdSafeIME.this, false);

                PwsRecord rec = null;
                String uuid = PasswdSafeFileDataFragment.getLastViewedRecord();
                if (uuid != null) {
                    rec = fileData.getRecord(uuid);
                }

                String recLabel;
                if (rec != null) {
                    recLabel = fileData.getId(rec);
                } else {
                    recLabel = getString(R.string.none_selected_open);
                }

                labels.set(new Pair<>(fileLabel, recLabel));
                if (user != null) {
                    user.refresh(fileData, rec);
                }
            }
        });

        boolean haveFile = (labels.get() != null);
        GuiUtils.setVisible(itsRecordLabel, haveFile);
        GuiUtils.setVisible(itsRecord, haveFile);
        if (haveFile) {
            itsFile.setText(labels.get().first);
            itsRecord.setText(labels.get().second);
        } else {
            itsFile.setText(R.string.none_selected_open);
            itsRecord.setText(null);
            if (user != null) {
                user.refresh(null, null);
            }
        }
    }

    /**
     * Show the password warning
     */
    private void showPasswordWarning(boolean show)
    {
        GuiUtils.setVisible(itsPasswordWarning, show);
    }

    /**
     * Get the IME token
     */
    private IBinder getToken()
    {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    /**
     * User for the refresh call
     */
    private interface RefreshUser
    {
        /**
         * Callback to refresh with the optional file data and record
         */
        void refresh(@Nullable PasswdFileData fileData,
                     @Nullable PwsRecord rec);
    }

    /**
     * The listener for keyboard events
     */
    private final class KeyboardListener implements OnKeyboardActionListener
    {
        @Override
        public void onKey(int primaryCode, int[] keyCodes)
        {
            onKeyPress(primaryCode);
        }

        @Override
        public void onPress(int primaryCode)
        {
        }

        @Override
        public void onRelease(int primaryCode)
        {
        }

        @Override
        public void onText(CharSequence text)
        {
        }

        @Override
        public void swipeDown()
        {
        }

        @Override
        public void swipeLeft()
        {
        }

        @Override
        public void swipeRight()
        {
        }

        @Override
        public void swipeUp()
        {
        }
    }
}
