/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.IBinder;
import android.text.InputType;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.Nullable;

import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdHistory;
import net.tjado.passwdsafe.file.PasswdRecord;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.util.Pair;

import org.pwsafe.lib.file.PwsRecord;

import java.util.List;

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
    public static final int PASSWORD_KEY = -101;
    private static final int URL_KEY = -102;
    private static final int EMAIL_KEY = -103;
    private static final int TITLE_KEY = -104;
    private static final int NOTES_KEY = -105;
    private static final int PREVIOUS_PASSWORD_KEY = -106;

    // Control keys
    public static final int ENTER_KEY = -200;
    public static final int PASSWDSAFE_KEY = -201;
    public static final int KEYBOARD_NEXT_KEY = -202;
    public static final int KEYBOARD_CHOOSE_KEY = -203;
    private static final int DEL_KEY = -204;

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
    private boolean itsIsVibrate = false;

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

        itsKeyboardView = view.findViewById(R.id.keyboard);
        itsKeyboardView.setPreviewEnabled(false);
        itsKeyboardView.setOnKeyboardActionListener(new KeyboardListener());

        itsRecord = view.findViewById(R.id.record);
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

        SharedPreferences prefs = Preferences.getSharedPrefs(this);
        itsIsVibrate = Preferences.getDisplayVibrateKeyboard(prefs);
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
        if (v.getId() == R.id.record) {
            openPasswdSafe();
        }
    }

    /**
     * Open PasswdSafe
     */
    private void openPasswdSafe()
    {
        Pair<Intent, Boolean> rc = refresh((fileData, rec) -> {
            Intent intent;
            if (fileData == null) {
                intent = PasswdSafeUtil.getMainActivityIntent(
                        BuildConfig.APPLICATION_ID, PasswdSafeIME.this);
                if (intent == null) {
                    return null;
                }
                intent.putExtra(PasswdSafe.INTENT_EXTRA_CLOSE_ON_OPEN,
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
            return new Pair<>(intent, (fileData != null));
        });
        if (rc != null) {
            if (rc.second) {
                setKeyboard(itsPasswdSafeKeyboard);
            } else {
                setKeyboard(itsQwertyKeyboard);
            }
            startActivity(rc.first);
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

        if (itsIsVibrate) {
            itsKeyboardView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }

        switch (keycode) {
        case USER_KEY:
        case PASSWORD_KEY:
        case TITLE_KEY:
        case URL_KEY:
        case EMAIL_KEY:
        case NOTES_KEY:
        case PREVIOUS_PASSWORD_KEY: {
            String keyStr = refresh((fileData, rec) -> {
                if ((fileData == null) || (rec == null)) {
                    return null;
                }
                switch (keycode) {
                case USER_KEY: {
                    return fileData.getUsername(rec);
                }
                case PASSWORD_KEY: {
                    showPasswordWarning(!itsAllowPassword);
                    if (itsAllowPassword) {
                        itsAllowPassword = itsIsPasswordField;
                        PasswdRecord pwsrec = fileData.getPasswdRecord(rec);
                        if (pwsrec != null) {
                            return pwsrec.getPassword(fileData);
                        } else {
                            return fileData.getPassword(rec);
                        }
                    } else {
                        itsAllowPassword = true;
                    }
                    break;
                }
                case TITLE_KEY: {
                    return fileData.getTitle(rec);
                }
                case URL_KEY: {
                    return fileData.getURL(
                            rec, PasswdFileData.UrlStyle.URL_ONLY);
                }
                case EMAIL_KEY: {
                    return fileData.getEmail(
                            rec, PasswdFileData.EmailStyle.ADDR_ONLY);
                }
                case NOTES_KEY: {
                    return fileData.getNotes(rec, this).getNotes();
                }
                case PREVIOUS_PASSWORD_KEY: {
                    PasswdHistory hist = fileData.getPasswdHistory(rec);
                    if (hist == null) {
                        return null;
                    }
                    List<PasswdHistory.Entry> entries = hist.getPasswds();
                    if (entries.size() == 0) {
                        return null;
                    }
                    return entries.get(0).getPasswd();
                }
                }
                return null;
            });
            if (keyStr != null) {
                conn.commitText(keyStr, 1);
            }
            break;
        }
        case Keyboard.KEYCODE_DELETE: {
            conn.deleteSurroundingText(1, 0);
            updateShiftKeyState();
            break;
        }
        case DEL_KEY: {
            conn.deleteSurroundingText(0, 1);
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
            if ((inputMgr != null) &&
                (!ApiCompat.shouldOfferSwitchingToNextInputMethod(inputMgr,
                                                                  token) ||
                 !ApiCompat.switchToNextInputMethod(inputMgr, token, false))) {
                inputMgr.showInputMethodPicker();
            }
            break;
        }
        case KEYBOARD_CHOOSE_KEY: {
            InputMethodManager inputMgr =
                    (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (inputMgr != null) {
                inputMgr.showInputMethodPicker();
            }
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
                InputConnection conn = getCurrentInputConnection();
                if (conn != null) {
                    caps = conn.getCursorCapsMode(ei.inputType);
                }
            }
            itsKeyboardView.setShifted(itsCapsLock || (caps != 0));
        }
    }

    /**
     * Refresh the fields from the current password data
     * @param user The user callback to handle the refresh.  Called even if
     *             there is no file data
     */
    private <RetT> RetT refresh(@Nullable final RefreshUser<RetT> user)
    {
        RefreshResult<RetT> rc = PasswdSafeFileDataFragment.useOpenFileData(
                fileData -> {
                    String fileLabel = fileData.getUri().getIdentifier(
                            PasswdSafeIME.this, true);

                    String uuid =
                            PasswdSafeFileDataFragment.getLastViewedRecord();
                    PwsRecord rec =
                            (uuid != null) ? fileData.getRecord(uuid) : null;

                    String recLabel;
                    boolean hasPrevious = false;
                    if (rec != null) {
                        recLabel = fileData.getId(rec);
                        PasswdHistory history = fileData.getPasswdHistory(rec);
                        hasPrevious = (history != null) &&
                                      (history.getMaxSize() > 0);
                    } else {
                        recLabel = getString(R.string.none_selected_open);
                    }

                    RetT ret = (user != null) ?
                            user.refresh(fileData, rec) : null;
                    return new RefreshResult<>(fileLabel, recLabel,
                                               hasPrevious, ret);
                });

        StringBuilder label = new StringBuilder();
        boolean hasPreviousPassword;
        RetT ret;
        if (rc != null) {
            label.append(getString(R.string.record)).append(": ");
            label.append(rc.itsFileLabel);
            label.append(" - ");
            label.append(rc.itsRecordLabel);
            hasPreviousPassword = rc.itsHasPreviousPassword;
            ret = rc.itsResult;
        } else {
            label.append(getString(R.string.file)).append(": ")
                 .append(getString(R.string.none_selected_open));
            hasPreviousPassword = false;
            ret = (user != null) ? user.refresh(null, null) : null;
        }
        itsRecord.setText(label.toString());
        itsPasswdSafeKeyboard.setHasPreviousPassword(hasPreviousPassword);
        return ret;
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
    private interface RefreshUser<RetT>
    {
        /**
         * Callback to refresh with the optional file data and record
         */
        RetT refresh(@Nullable PasswdFileData fileData,
                     @Nullable PwsRecord rec);
    }

    /**
     * Result of a refresh
     */
    private static class RefreshResult<RetT>
    {
        protected final String itsFileLabel;
        protected final String itsRecordLabel;
        protected final boolean itsHasPreviousPassword;
        protected final RetT itsResult;

        /**
         * Constructor
         */
        protected RefreshResult(String fileLabel,
                                String recLabel,
                                boolean hasPreviousPassword,
                                RetT result)
        {
            itsFileLabel = fileLabel;
            itsRecordLabel = recLabel;
            itsHasPreviousPassword = hasPreviousPassword;
            itsResult = result;
        }
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
