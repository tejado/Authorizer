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
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.util.Pair;

/**
 *  Input method for selecting fields from a record
 *
 *  @author Jeff Harris
 */
public class PasswdSafeIME extends InputMethodService
{
    private static final int USER_KEY = -1;
    private static final int URL_KEY = -2;
    private static final int BACK_KEY = -3;
    private static final int PASSWORD_KEY = -11;
    private static final int EMAIL_KEY = -12;
    private static final int ENTER_KEY = -13;
    private static final int TITLE_KEY = -21;
    private static final int NOTES_KEY = -22;
    private static final int PASSWDSAFE_KEY = -24;
    private static final int KEYBOARD_CHOOSE_KEY = -25;

    private View itsView;
    private KeyboardView itsKeyboardView;
    private Keyboard.Key itsEnterKey;
    private boolean itsAllowPassword = false;
    private boolean itsIsPasswordField = false;
    private int itsEnterAction = EditorInfo.IME_ACTION_NONE;

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onCreateInputView()
     */
    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView()
    {
        itsView = getLayoutInflater().inflate(R.layout.input_method, null);
        refresh();

        Keyboard keyboard = new PasswdSafeKeyboard(this, R.xml.keyboard);
        itsKeyboardView = (KeyboardView)itsView.findViewById(R.id.keyboard);
        itsKeyboardView.setPreviewEnabled(false);
        itsKeyboardView.setKeyboard(keyboard);
        itsKeyboardView.setOnKeyboardActionListener(new KeyboardListener());

        View icon = itsView.findViewById(R.id.icon);
        icon.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onKeyPress(PASSWDSAFE_KEY);
            }
        });

        return itsView;
    }

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onStartInputView(android.view.inputmethod.EditorInfo, boolean)
     */
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting)
    {
        super.onStartInputView(info, restarting);
        refresh();

        int enterText = -1;
        int enterIcon = -1;
        itsEnterAction =
                info.imeOptions & (EditorInfo.IME_MASK_ACTION |
                                   EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        switch (itsEnterAction) {
        case EditorInfo.IME_ACTION_DONE: {
            enterText = R.string.done;
            break;
        }
        case EditorInfo.IME_ACTION_GO: {
            enterText = R.string.go;
            break;
        }
        case EditorInfo.IME_ACTION_NEXT: {
            enterText = R.string.next;
            break;
        }
        case EditorInfo.IME_ACTION_SEARCH: {
            enterIcon = R.drawable.sym_keyboard_search;
            break;
        }
        case EditorInfo.IME_ACTION_SEND: {
            enterText = R.string.send;
            break;
        }
        default: {
            enterIcon = R.drawable.sym_keyboard_return;
            itsEnterAction = EditorInfo.IME_ACTION_NONE;
            break;
        }
        }

        if (itsEnterKey != null) {
            itsEnterKey.label = (enterText != -1) ? getString(enterText) : null;
            itsEnterKey.icon = (enterIcon != -1) ?
                    getResources().getDrawable(enterIcon) : null;
        }

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
        itsKeyboardView.invalidateAllKeys();
    }

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onEvaluateFullscreenMode()
     */
    @Override
    public boolean onEvaluateFullscreenMode()
    {
        // Don't want to enter full-screen mode as not a real keyboard
        return false;
    }

    /** Open PasswdSafe */
    private void openPasswdSafe()
    {
        Pair<PasswdFileData, PwsRecord> rc = refresh();
        Intent intent;
        if (rc.first == null) {
            intent = PasswdSafeUtil.getMainActivityIntent(
                    "com.jefftharris.passwdsafe", this);
            if (intent == null) {
                return;
            }
            intent.putExtra(FileListActivity.INTENT_EXTRA_CLOSE_ON_OPEN, true);

            InputMethodManager inputMgr =
                    (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            IBinder token = this.getWindow().getWindow().getAttributes().token;
            GuiUtils.switchToLastInputMethod(inputMgr, token);
        } else {
            String uuid = null;
            if (rc.second != null) {
                uuid = rc.first.getUUID(rc.second);
            }
            intent = PasswdSafeUtil.createOpenIntent(
                    rc.first.getUri().getUri(), uuid);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(ApiCompat.INTENT_FLAG_ACTIVITY_CLEAR_TASK);
        }
        startActivity(intent);
    }

    /** Handle a press of a keyboard key */
    private void onKeyPress(int keycode)
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

        String str = null;
        switch (keycode) {
        case USER_KEY:
        case PASSWORD_KEY:
        case TITLE_KEY:
        case URL_KEY:
        case EMAIL_KEY:
        case NOTES_KEY: {
            Pair<PasswdFileData, PwsRecord> rc = refresh();
            if (rc.second == null) {
                break;
            }
            switch (keycode) {
            case USER_KEY: {
                str = rc.first.getUsername(rc.second);
                break;
            }
            case PASSWORD_KEY: {
                showPasswordWarning(!itsAllowPassword);
                if (itsAllowPassword) {
                    str = rc.first.getPassword(rc.second);
                    itsAllowPassword = itsIsPasswordField;
                } else {
                    itsAllowPassword = true;
                }
                break;
            }
            case TITLE_KEY: {
                str = rc.first.getTitle(rc.second);
                break;
            }
            case URL_KEY: {
                str = rc.first.getURL(rc.second);
                break;
            }
            case EMAIL_KEY: {
                str = rc.first.getEmail(rc.second);
                break;
            }
            case NOTES_KEY: {
                str = rc.first.getNotes(rc.second);
                break;
            }
            }
            break;
        }
        case BACK_KEY: {
            conn.deleteSurroundingText(1, 0);
            break;
        }
        case ENTER_KEY: {
            if (itsEnterAction == EditorInfo.IME_ACTION_NONE) {
                conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                                               KeyEvent.KEYCODE_ENTER));
                conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                                               KeyEvent.KEYCODE_ENTER));
            } else {
                conn.performEditorAction(itsEnterAction);
            }
            break;
        }
        case PASSWDSAFE_KEY: {
            openPasswdSafe();
            break;
        }
        case KEYBOARD_CHOOSE_KEY: {
            InputMethodManager inputMgr =
                    (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            inputMgr.showInputMethodPicker();
            break;
        }
        case 32: {
            str = " ";
            break;
        }
        }
        if (str != null) {
            conn.commitText(str, 1);
        }
    }

    /** Refresh the fields from the current password data */
    private Pair<PasswdFileData, PwsRecord> refresh()
    {
        PasswdSafeApp app = getPasswdSafeApp();
        PasswdFileData fileData = app.accessOpenFileData();
        PwsRecord rec = null;
        TextView filetv = (TextView)itsView.findViewById(R.id.file);
        if (fileData != null) {
            filetv.setText(fileData.getUri().getIdentifier(this, false));

            String uuid = app.getLastViewedRecord();
            if (uuid != null) {
                rec = fileData.getRecord(uuid);
            }
        } else {
            filetv.setText(R.string.none_selected_open);
        }

        View reclabel = itsView.findViewById(R.id.record_label);
        reclabel.setVisibility((fileData == null) ? View.GONE : View.VISIBLE);
        TextView rectv = (TextView)itsView.findViewById(R.id.record);
        rectv.setVisibility((fileData == null) ? View.GONE : View.VISIBLE);
        if (rec != null) {
            rectv.setText(fileData.getId(rec));
        } else if (fileData != null) {
            rectv.setText(R.string.none_selected_open);
        } else {
            rectv.setText(null);
        }

        return new Pair<>(fileData, rec);
    }

    /** Show the password warning */
    private void showPasswordWarning(boolean show)
    {
        View v = itsView.findViewById(R.id.password_warning);
        v.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** Get the PasswdSafeApp */
    private PasswdSafeApp getPasswdSafeApp()
    {
        return (PasswdSafeApp)getApplication();
    }

    /** The PasswdSafeKeyboard class is a keyboard for PasswdSafe */
    private final class PasswdSafeKeyboard extends Keyboard
    {
        /** Constructor */
        public PasswdSafeKeyboard(
                Context context,
                @SuppressWarnings("SameParameterValue") int xmlLayoutResId)
        {
            super(context, xmlLayoutResId);
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.Keyboard#createKeyFromXml(android.content.res.Resources, android.inputmethodservice.Keyboard.Row, int, int, android.content.res.XmlResourceParser)
         */
        @Override
        protected Key createKeyFromXml(@NonNull Resources res,
                                       @NonNull Row parent,
                                       int x, int y, XmlResourceParser parser)
        {
            Key key = super.createKeyFromXml(res, parent, x, y, parser);
            if (key.codes[0] == ENTER_KEY) {
                itsEnterKey = key;
            }
            return key;
        }
    }

    /** The listener for keyboard events */
    private final class KeyboardListener implements OnKeyboardActionListener
    {
        /* (non-Javadoc)
         * @see android.inputmethodservice.KeyboardView.OnKeyboardActionListener#onKey(int, int[])
         */
        @Override
        public void onKey(int primaryCode, int[] keyCodes)
        {
            onKeyPress(primaryCode);
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.KeyboardView.OnKeyboardActionListener#onPress(int)
         */
        @Override
        public void onPress(int primaryCode)
        {
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.KeyboardView.OnKeyboardActionListener#onRelease(int)
         */
        @Override
        public void onRelease(int primaryCode)
        {
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.KeyboardView.OnKeyboardActionListener#onText(java.lang.CharSequence)
         */
        @Override
        public void onText(CharSequence text)
        {
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.KeyboardView.OnKeyboardActionListener#swipeDown()
         */
        @Override
        public void swipeDown()
        {
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.KeyboardView.OnKeyboardActionListener#swipeLeft()
         */
        @Override
        public void swipeLeft()
        {
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.KeyboardView.OnKeyboardActionListener#swipeRight()
         */
        @Override
        public void swipeRight()
        {
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.KeyboardView.OnKeyboardActionListener#swipeUp()
         */
        @Override
        public void swipeUp()
        {
        }
    }
}
