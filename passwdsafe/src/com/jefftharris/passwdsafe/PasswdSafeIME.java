/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
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

    private View itsView;
    private KeyboardView itsKeyboardView;
    private Keyboard itsKeyboard;
    private Keyboard.Key itsEnterKey;
    private int itsEnterAction = EditorInfo.IME_ACTION_NONE;

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onCreateInputView()
     */
    @Override
    public View onCreateInputView()
    {
        itsView = getLayoutInflater().inflate(R.layout.input_method, null);
        refresh();

        Button btn;
        btn = (Button)itsView.findViewById(R.id.launch_passwdsafe);
        btn.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                openPasswdSafe();
            }
        });

        itsKeyboard = new PasswdSafeKeyboard(this, R.xml.keyboard);
        itsKeyboardView = (KeyboardView)itsView.findViewById(R.id.keyboard);
        itsKeyboardView.setPreviewEnabled(false);
        itsKeyboardView.setKeyboard(itsKeyboard);
        itsKeyboardView.setOnKeyboardActionListener(new KeyboardListener());

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

        int enterText;
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
            enterText = R.string.search;
            break;
        }
        case EditorInfo.IME_ACTION_SEND: {
            enterText = R.string.send;
            break;
        }
        default: {
            enterText = R.string.execute;
            itsEnterAction = EditorInfo.IME_ACTION_NONE;
            break;
        }
        }

        if (itsEnterKey != null) {
            itsEnterKey.label = getString(enterText);
        }
        // Reset keyboard to reflect key changes
        itsKeyboardView.setKeyboard(itsKeyboard);
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
    private final void openPasswdSafe()
    {
        Pair<PasswdFileData, PwsRecord> rc = refresh();
        if (rc.first == null) {
            PasswdSafeUtil.startMainActivity("com.jefftharris.passwdsafe",
                                             this);
        } else {
            String uuid = null;
            if (rc.second != null) {
                uuid = rc.first.getUUID(rc.second);
            }
            Intent intent = PasswdSafeUtil.createOpenIntent(
                    rc.first.getUri().getUri(), uuid);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    /** Handle a press of a keyboard key */
    private final void onKeyPress(int keycode)
    {
        InputConnection conn = getCurrentInputConnection();
        if (conn == null) {
            return;
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
                str = rc.first.getPassword(rc.second);
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
    private final Pair<PasswdFileData, PwsRecord> refresh()
    {
        // TODO: test file timeouts and file and record deletions
        // TODO: Check field type for password pastes?
        // TODO: show group
        // TODO: disable blank fields?
        // TODO: icons?

        PasswdSafeApp app = getPasswdSafeApp();
        PasswdFileData fileData = app.accessOpenFileData();
        PwsRecord rec = null;
        TextView filetv = (TextView)itsView.findViewById(R.id.file);
        if (fileData != null) {
            filetv.setText(fileData.getUri().toString());

            String uuid = app.getLastViewedRecord();
            if (uuid != null) {
                rec = fileData.getRecord(uuid);
            }
        } else {
            filetv.setText("NO FILE");
        }

        TextView rectv = (TextView)itsView.findViewById(R.id.record);
        if (rec != null) {
            rectv.setText(fileData.getTitle(rec));
        } else {
            rectv.setText("NO RECORD");
        }

        return new Pair<PasswdFileData, PwsRecord>(fileData, rec);
    }

    /** Get the PasswdSafeApp */
    private final PasswdSafeApp getPasswdSafeApp()
    {
        return (PasswdSafeApp)getApplication();
    }

    /** The PasswdSafeKeyboard class is a keyboard for PasswdSafe */
    private final class PasswdSafeKeyboard extends Keyboard
    {
        /** Constructor */
        public PasswdSafeKeyboard(Context context, int xmlLayoutResId)
        {
            super(context, xmlLayoutResId);
        }

        /* (non-Javadoc)
         * @see android.inputmethodservice.Keyboard#createKeyFromXml(android.content.res.Resources, android.inputmethodservice.Keyboard.Row, int, int, android.content.res.XmlResourceParser)
         */
        @Override
        protected Key createKeyFromXml(Resources res,
                                       Row parent,
                                       int x,
                                       int y,
                                       XmlResourceParser parser)
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
