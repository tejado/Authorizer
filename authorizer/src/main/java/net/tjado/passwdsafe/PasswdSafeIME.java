/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.lib.ObjectHolder;
import net.tjado.passwdsafe.util.Pair;

/**
 *  Input method for selecting fields from a record
 *
 *  @author Jeff Harris
 */
public class PasswdSafeIME extends InputMethodService
        implements View.OnClickListener
{
    // Password fields
    private static final int USER_KEY = -100;
    private static final int PASSWORD_KEY = -101;
    private static final int URL_KEY = -102;
    private static final int EMAIL_KEY = -103;
    private static final int TITLE_KEY = -104;
    private static final int NOTES_KEY = -105;

    // Control keys
    public static final int ENTER_KEY = -200;
    public static final int PASSWDSAFE_KEY = -201;
    public static final int KEYBOARD_NEXT_KEY = -202;
    public static final int KEYBOARD_CHOOSE_KEY = -203;

    private static boolean itsResetKeyboard = false;

    private KeyboardView itsKeyboardView;
    private PasswdSafeIMEKeyboard itsPasswdSafeKeyboard;
    private PasswdSafeIMEKeyboard itsQwertyKeyboard;
    private PasswdSafeIMEKeyboard itsSymbolsKeyboard;
    private PasswdSafeIMEKeyboard itsSymbolsShiftKeyboard;
    private PasswdSafeIMEKeyboard itsCurrKeyboard;
    private TextView itsRecord;
    private View itsPasswordWarning;
    private boolean itsAllowPassword = false;
    private boolean itsIsPasswordField = false;
    private long itsLastShiftTime = Long.MIN_VALUE;
    private boolean itsCapsLock = false;

    /**
     * Reset the keyboard shown when next visible
     */
    public static void resetKeyboard()
    {
        itsResetKeyboard = true;
    }

    @Override
    public void onInitializeInterface()
    {
        itsPasswdSafeKeyboard =
                new PasswdSafeIMEKeyboard(this, R.xml.keyboard_passwdsafe);
        itsQwertyKeyboard =
                new PasswdSafeIMEKeyboard(this, R.xml.keyboard_qwerty);
        itsSymbolsKeyboard =
                new PasswdSafeIMEKeyboard(this, R.xml.keyboard_symbols);
        itsSymbolsShiftKeyboard =
                new PasswdSafeIMEKeyboard(this, R.xml.keyboard_symbols_shift);
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView()
    {
        View view = getLayoutInflater().inflate(R.layout.input_method, null);

        itsKeyboardView = (KeyboardView)view.findViewById(R.id.keyboard);
        itsKeyboardView.setPreviewEnabled(false);
        itsKeyboardView.setOnKeyboardActionListener(new KeyboardListener());

        itsRecord = (TextView)view.findViewById(R.id.record);
        itsRecord.setOnClickListener(this);
        itsPasswordWarning = view.findViewById(R.id.password_warning);

        return view;
    }

    @Override
    public void onStartInput(EditorInfo info, boolean restarting)
    {
        super.onStartInput(info, restarting);

        Resources res = getResources();
        itsPasswdSafeKeyboard.setOptions(info, res);
        itsQwertyKeyboard.setOptions(info, res);
        itsSymbolsKeyboard.setOptions(info, res);
        itsSymbolsShiftKeyboard.setOptions(info, res);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting)
    {
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

        if (itsResetKeyboard) {
            itsResetKeyboard = false;
            itsCurrKeyboard = null;
        }

        PasswdSafeIMEKeyboard keyboard = itsCurrKeyboard;
        if (keyboard == null) {
            keyboard = itsPasswdSafeKeyboard;
        } else if (keyboard != itsPasswdSafeKeyboard) {
            switch (info.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE: {
                keyboard = itsSymbolsKeyboard;
                break;
            }
            default: {
                keyboard = itsQwertyKeyboard;
                break;
            }
            }
        }

        // Reset keyboard to reflect key changes
        setKeyboard(keyboard);
        itsKeyboardView.closing();
        itsKeyboardView.invalidateAllKeys();
    }

    @Override
    public void onFinishInput()
    {
        super.onFinishInput();

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

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.record: {
            openPasswdSafe();
            break;
        }
        }
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
                            BuildConfig.APPLICATION_ID, PasswdSafeIME.this);
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
            if (rc.get().second) {
                setKeyboard(itsPasswdSafeKeyboard);
            } else {
                setKeyboard(itsQwertyKeyboard);
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
        case Keyboard.KEYCODE_DELETE: {
            conn.deleteSurroundingText(1, 0);
            updateShiftKeyState();
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
        case Keyboard.KEYCODE_MODE_CHANGE: {
            Keyboard current = itsKeyboardView.getKeyboard();
            if (current == itsPasswdSafeKeyboard) {
                setKeyboard(itsQwertyKeyboard);
            } else if (current == itsQwertyKeyboard) {
                setKeyboard(itsSymbolsKeyboard);
                itsSymbolsKeyboard.setShifted(false);
            } else if ((current == itsSymbolsKeyboard) ||
                       (current == itsSymbolsShiftKeyboard)) {
                setKeyboard(itsPasswdSafeKeyboard);
            }
            break;
        }
        case Keyboard.KEYCODE_SHIFT: {
            Keyboard current = itsKeyboardView.getKeyboard();
            if (current == itsQwertyKeyboard) {
                long now = System.currentTimeMillis();

                boolean isShifted = itsKeyboardView.isShifted();
                if (itsCapsLock) {
                    itsCapsLock = false;
                } else if (!isShifted) {
                    itsLastShiftTime = now;
                } else if (now < (itsLastShiftTime + 500)) {
                    itsCapsLock = true;
                    itsLastShiftTime = Long.MIN_VALUE;
                }
                itsKeyboardView.setShifted(itsCapsLock || !isShifted);
            } else if (current == itsSymbolsKeyboard) {
                itsSymbolsKeyboard.setShifted(true);
                setKeyboard(itsSymbolsShiftKeyboard);
                itsSymbolsShiftKeyboard.setShifted(true);
            } else if (current == itsSymbolsShiftKeyboard) {
                itsSymbolsShiftKeyboard.setShifted(false);
                setKeyboard(itsSymbolsKeyboard);
                itsSymbolsKeyboard.setShifted(false);
            }
            break;
        }
        default: {
            int code = keycode;
            if (isInputViewShown() && itsKeyboardView.isShifted()) {
                code = Character.toUpperCase(code);
            }
            sendKeyChar((char)code);
            if (Character.isLetter(code) || Character.isWhitespace(code)) {
                updateShiftKeyState();
            }
            break;
        }
        }
    }

    /**
     * Set the current keyboard
     */
    private void setKeyboard(PasswdSafeIMEKeyboard keyboard)
    {
        itsCurrKeyboard = keyboard;
        itsKeyboardView.setKeyboard(itsCurrKeyboard);
        updateShiftKeyState();
    }

    /**
     * Helper to update the shift state of our keyboard based on the editor
     * state
     */
    private void updateShiftKeyState() {
        if (itsCurrKeyboard == itsQwertyKeyboard) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if ((ei != null) && (ei.inputType != InputType.TYPE_NULL)) {
                caps = getCurrentInputConnection().getCursorCapsMode(
                        ei.inputType);
            }
            itsKeyboardView.setShifted(itsCapsLock || (caps != 0));
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
                        PasswdSafeIME.this, true);

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

        StringBuilder label = new StringBuilder();
        if (labels.get() != null) {
            label.append(getString(R.string.record)).append(": ");
            label.append(labels.get().first);
            label.append(" - ");
            label.append(labels.get().second);
        } else {
            label.append(getString(R.string.file)).append(": ")
                    .append(getString(R.string.none_selected_open));
            if (user != null) {
                user.refresh(null, null);
            }
        }
        itsRecord.setText(label.toString());
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
