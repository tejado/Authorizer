/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import java.io.File;

import org.pwsafe.lib.file.PwsFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;

import net.tjado.passwdsafe.file.PasswdPolicy;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.pref.FileBackupPref;
import net.tjado.passwdsafe.pref.FileTimeoutPref;
import net.tjado.passwdsafe.pref.PasswdExpiryNotifPref;
import net.tjado.passwdsafe.pref.RecordSortOrderPref;

import net.tjado.authorizer.OutputInterface;

/**
 * The Preferences class manages preferences for the application
 *
 * @author Jeff Harris
 */
public class Preferences
{
    public static final String PREF_FILE_DIR = "fileDirPref";
    public static final String PREF_FILE_DIR_DEF =
        Environment.getExternalStorageDirectory().toString();

    public static final String PREF_FILE_CLOSE_TIMEOUT = "fileCloseTimeoutPref";
    private static final FileTimeoutPref PREF_FILE_CLOSE_TIMEOUT_DEF =
        FileTimeoutPref.TO_5_MIN;
    public static final String PREF_FILE_CLOSE_SCREEN_OFF =
                    "fileCloseScreenOffPref";
    public static final boolean PREF_FILE_CLOSE_SCREEN_OFF_DEF = false;

    public static final String PREF_FILE_BACKUP = "fileBackupPref";
    private static final FileBackupPref PREF_FILE_BACKUP_DEF =
        FileBackupPref.BACKUP_1;

    public static final String PREF_FILE_CLOSE_CLEAR_CLIPBOARD =
        "fileCloseClearClipboardPref";
    public static final boolean PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF = true;

    private static final String PREF_FILE_OPEN_READ_ONLY =
        "fileOpenReadOnly";
    private static final boolean PREF_FILE_OPEN_READ_ONLY_DEF = false;

    private static final String PREF_FILE_OPEN_YUBIKEY = "fileOpenYubikey";
    private static final boolean PREF_FILE_OPEN_YUBIKEY_DEF = false;

    private static final String PREF_FILE_SAVED_PASSWORD_CONFIRM =
            "fileSavedPasswordConfirm";
    private static final boolean PREF_FILE_SAVED_PASSWORD_CONFIRM_DEF = false;

    public static final String PREF_DEF_FILE = "defFilePref";
    private static final String PREF_DEF_FILE_DEF = "";

    public static final String PREF_FILE_LEGACY_FILE_CHOOSER =
            "fileLegacyFileChooserPref";
    public static final boolean PREF_FILE_LEGACY_FILE_CHOOSER_DEF = true;

    public static final String PREF_GROUP_RECORDS = "groupRecordsPref";
    public static final boolean PREF_GROUP_RECORDS_DEF = true;

    public static final String PREF_PASSWD_ENC = "passwordEncodingPref";
    public static final String PREF_PASSWD_ENC_DEF =
        PwsFile.DEFAULT_PASSWORD_CHARSET;
    public static final String PREF_PASSWD_EXPIRY_NOTIF =
        "passwordExpiryNotifyPref";
    public static final PasswdExpiryNotifPref PREF_PASSWD_EXPIRY_NOTIF_DEF =
        PasswdExpiryNotifPref.IN_TWO_WEEKS;
    public static final String PREF_PASSWD_DEFAULT_SYMS =
        "passwordDefaultSymbolsPref";
    public static final String PREF_PASSWD_CLEAR_ALL_NOTIFS =
        "passwordClearAllNotifsPref";
    public static final String PREF_PASSWD_CLEAR_ALL_SAVED =
        "passwordClearAllSavedPref";

    public static final String PREF_RECORD_SORT_ORDER = "recordSortOrderPref";
    public static final RecordSortOrderPref PREF_RECORD_SORT_ORDER_DEF =
            RecordSortOrderPref.GROUP_FIRST;

    public static final String PREF_SEARCH_CASE_SENSITIVE =
        "searchCaseSensitivePref";
    public static final boolean PREF_SEARCH_CASE_SENSITIVE_DEF = false;
    public static final String PREF_SEARCH_REGEX = "searchRegexPref";
    public static final boolean PREF_SEARCH_REGEX_DEF = false;

    private static final String PREF_SHOW_HIDDEN_FILES = "showBackupFilesPref";
    private static final boolean PREF_SHOW_HIDDEN_FILES_DEF = false;

    public static final String PREF_FILE_BACKUP_USB_GPG = "fileBackupUsbGpgPref";
    public static final boolean PREF_FILE_BACKUP_USB_GPG_DEF = false;

    private static final String PREF_FILE_BACKUP_USB_GPG_KEY = "fileBackupUsbGpgKeyPref";
    private static final String PREF_FILE_BACKUP_USB_GPG_KEY_DEF = "";

    public static final String PREF_SORT_CASE_SENSITIVE =
        "sortCaseSensitivePref";
    public static final boolean PREF_SORT_CASE_SENSITIVE_DEF = true;

    public static final String PREF_DISPLAY_THEME_LIGHT = "displayThemeLightPref";
    private static final boolean PREF_DISPLAY_THEME_LIGHT_DEF = false;

    public static final String PREF_DISPLAY_LIST_TREEVIEW = "displayListTreeViewPref";
    private static final boolean PREF_DISPLAY_LIST_TREEVIEW_DEF = true;

    private static final String PREF_COPY_PASSWORD_CONFIRM =
            "copyPasswordConfirm";
    private static final boolean PREF_COPY_PASSWORD_CONFIRM_DEF = false;

    private static final String PREF_GEN_LOWER = "passwdGenLower";
    private static final boolean PREF_GEN_LOWER_DEF = true;
    private static final String PREF_GEN_UPPER = "passwdGenUpper";
    private static final boolean PREF_GEN_UPPER_DEF = true;
    private static final String PREF_GEN_DIGITS = "passwdGenDigits";
    private static final boolean PREF_GEN_DIGITS_DEF = true;
    private static final String PREF_GEN_SYMBOLS = "passwdGenSymbols";
    private static final boolean PREF_GEN_SYMBOLS_DEF = false;
    private static final String PREF_GEN_EASY = "passwdGenEasy";
    private static final boolean PREF_GEN_EASY_DEF = false;
    private static final String PREF_GEN_HEX = "passwdGenHex";
    private static final boolean PREF_GEN_HEX_DEF = false;
    private static final String PREF_GEN_LENGTH = "passwdGenLength";
    private static final String PREF_GEN_LENGTH_DEF = "8";
    private static final String PREF_DEF_PASSWD_POLICY = "defaultPasswdPolicy";
    private static final String PREF_DEF_PASSWD_POLICY_DEF = "";

    public static final String PREF_AUTOTYPE_USB_ENABLE = "usbkbdEnablePref";
    private static final boolean PREF_AUTOTYPE_USB_ENABLE_DEF = true;

    public static final String PREF_AUTOTYPE_BT_ENABLE = "bluetoothkbdEnablePref";
    private static final boolean PREF_AUTOTYPE_BT_ENABLE_DEF = true;

    public static final String PREF_AUTOTYPE_LANG = "usbkbdLanguagePref";
    private static final OutputInterface.Language PREF_AUTOTYPE_LANG_DEF = OutputInterface.Language.en_US;

    private static final String TAG = "Preferences";


    /**
     * Get the default shared preferences
     */
    public static SharedPreferences getSharedPrefs(Context ctx)
    {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static FileTimeoutPref getFileCloseTimeoutPref(SharedPreferences prefs)
    {
        try {
            return FileTimeoutPref.prefValueOf(
                prefs.getString(PREF_FILE_CLOSE_TIMEOUT,
                                PREF_FILE_CLOSE_TIMEOUT_DEF.getValue()));
        } catch (IllegalArgumentException e) {
            return PREF_FILE_CLOSE_TIMEOUT_DEF;
        }
    }

    public static boolean getFileCloseScreenOffPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_CLOSE_SCREEN_OFF,
                                PREF_FILE_CLOSE_SCREEN_OFF_DEF);
    }

    public static FileBackupPref getFileBackupPref(SharedPreferences prefs)
    {
        try {
            return FileBackupPref.prefValueOf(
                prefs.getString(PREF_FILE_BACKUP,
                                PREF_FILE_BACKUP_DEF.getValue()));
        } catch (IllegalArgumentException e) {
            return PREF_FILE_BACKUP_DEF;
        }
    }

    public static boolean getFileCloseClearClipboardPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_CLOSE_CLEAR_CLIPBOARD,
                                PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF);
    }

    public static boolean getFileOpenReadOnlyPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_OPEN_READ_ONLY,
                                PREF_FILE_OPEN_READ_ONLY_DEF);
    }

    public static void setFileOpenReadOnlyPref(boolean readonly,
                                               SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putBoolean(PREF_FILE_OPEN_READ_ONLY, readonly);
        prefsEdit.apply();
    }

    /**
     * Get the preference to use YubiKey
     */
    public static boolean getFileOpenYubikeyPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_OPEN_YUBIKEY,
                                PREF_FILE_OPEN_YUBIKEY_DEF);
    }

    /**
     * Set the preference to use YubiKey
     */
    public static void setFileOpenYubikeyPref(boolean yubikey,
                                              SharedPreferences prefs)
    {
        prefs.edit().putBoolean(PREF_FILE_OPEN_YUBIKEY, yubikey).apply();
    }

    /**
     * Get the preference for whether the user has confirmed the saved
     * password warning
     */
    public static boolean isFileSavedPasswordConfirm(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_SAVED_PASSWORD_CONFIRM,
                                PREF_FILE_SAVED_PASSWORD_CONFIRM_DEF);
    }

    /**
     * Set the preference that user has confirmed the saved password warning
     */
    public static void setFileSavedPasswordConfirmed(SharedPreferences prefs)
    {
        prefs.edit().putBoolean(PREF_FILE_SAVED_PASSWORD_CONFIRM, true).apply();
    }

    public static File getFileDirPref(SharedPreferences prefs)
    {
        String prefstr = prefs.getString(PREF_FILE_DIR, PREF_FILE_DIR_DEF);
        return (prefstr != null) ? new File(prefstr) : null;
    }

    public static void setFileDirPref(File dir, SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putString(Preferences.PREF_FILE_DIR, dir.toString());
        prefsEdit.apply();
    }

    public static Uri getDefFilePref(SharedPreferences prefs)
    {
        String defFile = prefs.getString(PREF_DEF_FILE, PREF_DEF_FILE_DEF);
        if (TextUtils.isEmpty(defFile)) {
            return null;
        }
        return Uri.parse(defFile);
    }

    /** Get the preference for use of the legacy file chooser */
    public static boolean getFileLegacyFileChooserPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_LEGACY_FILE_CHOOSER,
                                PREF_FILE_LEGACY_FILE_CHOOSER_DEF);
    }

    public static boolean getGroupRecordsPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GROUP_RECORDS, PREF_GROUP_RECORDS_DEF);
    }

    public static String getPasswordEncodingPref(SharedPreferences prefs)
    {
        return prefs.getString(PREF_PASSWD_ENC, PREF_PASSWD_ENC_DEF);
    }

    /** Get the password expiration notification preference */
    public static PasswdExpiryNotifPref getPasswdExpiryNotifPref
    (
         SharedPreferences prefs
    )
    {
        try {
            return PasswdExpiryNotifPref.prefValueOf(
                prefs.getString(PREF_PASSWD_EXPIRY_NOTIF,
                                PREF_PASSWD_EXPIRY_NOTIF_DEF.getValue()));
        } catch (IllegalArgumentException e) {
            return PREF_PASSWD_EXPIRY_NOTIF_DEF;
        }
    }

    /** Get the symbols used by default in a password policy */
    public static String getPasswdDefaultSymbolsPref(SharedPreferences prefs)
    {
        String val = prefs.getString(PREF_PASSWD_DEFAULT_SYMS, null);
        if (TextUtils.isEmpty(val)) {
            val = PasswdPolicy.SYMBOLS_DEFAULT;
        }
        return val;
    }

    /** Upgrade the default password policy preference if needed */
    public static void upgradePasswdPolicy(SharedPreferences prefs,
                                           Context ctx)
    {
        if (prefs.contains(PREF_DEF_PASSWD_POLICY)) {
            PasswdSafeUtil.dbginfo(TAG, "Have default policy");
            return;
        }

        SharedPreferences.Editor prefsEdit = prefs.edit();
        String policyStr = PREF_DEF_PASSWD_POLICY_DEF;
        if (prefs.contains(PREF_GEN_LOWER) ||
            prefs.contains(PREF_GEN_UPPER) ||
            prefs.contains(PREF_GEN_DIGITS) ||
            prefs.contains(PREF_GEN_SYMBOLS) ||
            prefs.contains(PREF_GEN_EASY) ||
            prefs.contains(PREF_GEN_HEX) ||
            prefs.contains(PREF_GEN_LENGTH)) {
            PasswdSafeUtil.dbginfo(TAG, "Upgrade old prefs");

            int flags = 0;
            if (prefs.getBoolean(PREF_GEN_HEX, PREF_GEN_HEX_DEF)) {
                flags |= PasswdPolicy.FLAG_USE_HEX_DIGITS;
            } else {
                if (prefs.getBoolean(PREF_GEN_EASY, PREF_GEN_EASY_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_EASY_VISION;
                }

                if (prefs.getBoolean(PREF_GEN_LOWER, PREF_GEN_LOWER_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_LOWERCASE;
                }
                if (prefs.getBoolean(PREF_GEN_UPPER, PREF_GEN_UPPER_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_UPPERCASE;
                }
                if (prefs.getBoolean(PREF_GEN_DIGITS, PREF_GEN_DIGITS_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_DIGITS;
                }
                if (prefs.getBoolean(PREF_GEN_SYMBOLS, PREF_GEN_SYMBOLS_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_SYMBOLS;
                }
            }
            int length;
            try {
                length = Integer.parseInt(prefs.getString(PREF_GEN_LENGTH,
                                                          PREF_GEN_LENGTH_DEF));
            } catch (NumberFormatException e) {
                length = Integer.parseInt(PREF_GEN_LENGTH_DEF);
            }
            PasswdPolicy policy = PasswdPolicy.createDefaultPolicy(ctx, flags,
                                                                   length);
            policyStr = policy.toHdrPolicyString();

            prefsEdit.remove(PREF_GEN_LOWER);
            prefsEdit.remove(PREF_GEN_UPPER);
            prefsEdit.remove(PREF_GEN_DIGITS);
            prefsEdit.remove(PREF_GEN_SYMBOLS);
            prefsEdit.remove(PREF_GEN_EASY);
            prefsEdit.remove(PREF_GEN_HEX);
            prefsEdit.remove(PREF_GEN_LENGTH);
        }

        PasswdSafeUtil.dbginfo(TAG, "Save new default policy: %s", policyStr);
        prefsEdit.putString(PREF_DEF_PASSWD_POLICY, policyStr);
        prefsEdit.apply();
    }

    /** Upgrade the default file preference if needed */
    public static void upgradeDefaultFilePref(SharedPreferences prefs)
    {
        Uri defFileUri = getDefFilePref(prefs);
        if ((defFileUri != null) && (defFileUri.getScheme() == null)) {
            File defDir = getFileDirPref(prefs);
            File def = new File(defDir, defFileUri.getPath());
            defFileUri = Uri.fromFile(def);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_DEF_FILE, defFileUri.toString());
            editor.apply();
        }
    }

    /** Get the default password policy preference */
    public static PasswdPolicy getDefPasswdPolicyPref(SharedPreferences prefs,
                                                      Context ctx)
    {
        String policyStr = prefs.getString(PREF_DEF_PASSWD_POLICY,
                                           PREF_DEF_PASSWD_POLICY_DEF);
        PasswdPolicy policy = null;
        if (!TextUtils.isEmpty(policyStr)) {
            try {
                policy = PasswdPolicy.parseHdrPolicy(
                    policyStr, 0, 0, PasswdPolicy.Location.DEFAULT).first;
            } catch (Exception e) {
                // Use default
            }
        }
        if (policy == null) {
            policy = PasswdPolicy.createDefaultPolicy(ctx);
        }
        return policy;
    }

    /** Set the default password policy preference */
    public static void setDefPasswdPolicyPref(PasswdPolicy policy,
                                              SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putString(PREF_DEF_PASSWD_POLICY, policy.toHdrPolicyString());
        prefsEdit.apply();
    }

    public static RecordSortOrderPref getRecordSortOrderPref(
            SharedPreferences prefs)
    {
        try {
            return RecordSortOrderPref.valueOf(
                    prefs.getString(PREF_RECORD_SORT_ORDER,
                                    PREF_RECORD_SORT_ORDER_DEF.toString()));
        } catch (IllegalArgumentException e) {
            return RecordSortOrderPref.GROUP_FIRST;
        }
    }

    public static boolean getSearchCaseSensitivePref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SEARCH_CASE_SENSITIVE,
                                PREF_SEARCH_CASE_SENSITIVE_DEF);
    }

    public static boolean getSearchRegexPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SEARCH_REGEX, PREF_SEARCH_REGEX_DEF);
    }

    public static boolean getShowHiddenFilesPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SHOW_HIDDEN_FILES,
                                PREF_SHOW_HIDDEN_FILES_DEF);
    }

    public static boolean getFileBackupUsbGpg(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_BACKUP_USB_GPG,
                                PREF_FILE_BACKUP_USB_GPG_DEF);

    }

    public static String getFileBackupUsbGpgKey(SharedPreferences prefs)
    {
        return prefs.getString(PREF_FILE_BACKUP_USB_GPG_KEY,
                                PREF_FILE_BACKUP_USB_GPG_KEY_DEF);

    }

    public static void setFileBackupUsbGpgKey(String keyId,
                                              SharedPreferences prefs)
    {
        prefs.edit().putString(PREF_FILE_BACKUP_USB_GPG_KEY, keyId).apply();
    }

    public static boolean getSortCaseSensitivePref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SORT_CASE_SENSITIVE,
                                PREF_SORT_CASE_SENSITIVE_DEF);
    }

    /**
     * Get whether to use the light theme
     */
    public static boolean getDisplayThemeLight(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_DISPLAY_THEME_LIGHT,
                                PREF_DISPLAY_THEME_LIGHT_DEF);
    }

    /**
     * Get whether to use the treeview list
     */
    public static boolean getDisplayListTreeView(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_DISPLAY_LIST_TREEVIEW,
                                PREF_DISPLAY_LIST_TREEVIEW_DEF);
    }

    /**
     * Get whether the user has confirmed the copy password operation
     */
    public static boolean isCopyPasswordConfirm(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_COPY_PASSWORD_CONFIRM,
                                PREF_COPY_PASSWORD_CONFIRM_DEF);
    }

    /**
     * Set whether the user has confirmed the copy password operation
     */
    @SuppressWarnings("SameParameterValue")
    public static void setCopyPasswordConfirm(boolean confirm,
                                              SharedPreferences prefs)
    {
        prefs.edit().putBoolean(PREF_COPY_PASSWORD_CONFIRM, confirm).apply();
    }

    /**
     * Get whether to enable USB Keyboard Output
     */
    public static boolean getAutoTypeUsbEnabled(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_AUTOTYPE_USB_ENABLE,
                                PREF_AUTOTYPE_USB_ENABLE_DEF);
    }

    /**
     * Get whether to enable USB Keyboard Output
     */
    public static boolean getAutoTypeBluetoothEnabled(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_AUTOTYPE_BT_ENABLE,
                                PREF_AUTOTYPE_BT_ENABLE_DEF);
    }

    public static OutputInterface.Language getAutoTypeLanguagePref(SharedPreferences prefs)
    {
        try {
            return OutputInterface.Language.valueOf(
                    prefs.getString(PREF_AUTOTYPE_LANG,
                                    PREF_AUTOTYPE_LANG_DEF.name()));
        } catch (IllegalArgumentException e) {
            return PREF_AUTOTYPE_LANG_DEF;
        }
    }
}
