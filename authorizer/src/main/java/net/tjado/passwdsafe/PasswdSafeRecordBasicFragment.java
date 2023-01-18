/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */

package net.tjado.passwdsafe;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.NumberKeyListener;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.DialogInterface;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.android.material.textfield.TextInputLayout;

import net.tjado.authorizer.OutputUsbKeyboard;
import net.tjado.authorizer.Utilities;
import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdHistory;
import net.tjado.passwdsafe.lib.ActContext;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.ObjectHolder;
import net.tjado.passwdsafe.lib.view.AbstractTextWatcher;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.lib.view.TextInputUtils;
import net.tjado.passwdsafe.lib.view.TypefaceUtils;
import net.tjado.passwdsafe.otp.AddActivity;
import net.tjado.passwdsafe.otp.ScanActivity;
import net.tjado.passwdsafe.otp.Token;
import net.tjado.passwdsafe.otp.TokenCode;
import net.tjado.passwdsafe.pref.PasswdTimeoutPref;
import net.tjado.passwdsafe.util.Pair;
import net.tjado.passwdsafe.view.CopyField;
import net.tjado.passwdsafe.view.PasswdLocation;

import net.tjado.authorizer.OutputInterface;
import net.tjado.authorizer.OutputUsbKeyboardAsRoot;
import net.tjado.authorizer.OutputBluetoothKeyboard;

import org.pwsafe.lib.file.PwsRecord;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.Set;

import static android.app.Activity.RESULT_OK;


/**
 * Fragment for showing basic fields of a password record
 */
public class PasswdSafeRecordBasicFragment
        extends AbstractPasswdSafeRecordFragment
        implements View.OnClickListener,
                   View.OnLongClickListener,
                   CompoundButton.OnCheckedChangeListener
{
    private static final String TAG = "PasswdSafeRecordBasicFragment";

    /**
     * Password visibility option change
     */
    private enum PasswordVisibilityChange
    {
        INITIAL,
        TOGGLE,
        SEEK,
        SHOW_SUBSET
    }

    private static final Pattern SUBSET_SPLIT = Pattern.compile("[ ,;]+");
    private static final char[] SUBSET_CHARS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
             '-', ' ', ',', ';', '?'};

    private boolean itsIsPasswordShown = false;
    private boolean itsShowOtp = false;
    private String itsHiddenPasswordStr;
    private String itsSubsetErrorStr;
    private String itsTitle;
    private View itsBaseRow;
    private TextView itsBaseLabel;
    private TextView itsBase;
    private View itsGroupRow;
    private TextView itsGroup;
    private View itsUserRow;
    private TextView itsUser;
    private View itsPasswordRow;
    private TextView itsPassword;
    private Runnable itsPasswordHideRun;
    private SeekBar itsPasswordSeek;
    private CompoundButton itsPasswordSubsetBtn;
    private TextInputLayout itsPasswordSubsetInput;
    private TextView itsPasswordSubset;
    private Button itsAutoTypeUsbUsername;
    private Button itsAutoTypeUsbPassword;
    private Button itsAutoTypeUsbOtp;
    private Button itsAutoTypeUsbCredentials;
    private Button itsAutoTypeBluetoothUsername;
    private Button itsAutoTypeBluetoothPassword;
    private Button itsAutoTypeBluetoothOtp;
    private Button itsAutoTypeBluetoothCredential;
    private CheckBox itsAutoTypeReturnSuffix;
    private RadioGroup itsAutoTypeDelimiter;
    private View itsUrlRow;
    private TextView itsUrl;
    private View itsEmailRow;
    private TextView itsEmail;
    private View itsTimesRow;
    private View itsCreationTimeRow;
    private TextView itsCreationTime;
    private View itsLastModTimeRow;
    private TextView itsLastModTime;
    private View itsProtectedRow;
    private View itsReferencesRow;
    private ListView itsReferences;
    private Button itsOtpAdd;
    private Button itsOtpAddCamera;
    private TokenCode itsOtp;
    private TextView itsOtpCode;
    private ProgressBar itsOtpTimer;
    private View itsOtpTokenRow;

    private String SUB_OTP;
    private String SUB_TAB;
    private String SUB_RETURN;

    private final int PERMISSIONS_REQUEST_CAMERA = 1;
    private final int REQUEST_SAVE_OTP_MANUAL = 1;
    private final int REQUEST_SAVE_OTP_CAMERA = 2;
    private final int REQUEST_ENABLE_BT = 3;

    private OutputBluetoothKeyboard itsOutputBluetoothKeyboard = null;

    /**
     * Create a new instance of the fragment
     */
    public static PasswdSafeRecordBasicFragment newInstance(
            PasswdLocation location)
    {
        PasswdSafeRecordBasicFragment frag = new PasswdSafeRecordBasicFragment();
        frag.setArguments(createArgs(location));
        return frag;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record_basic,
                                     container, false);
        itsBaseRow = root.findViewById(R.id.base_row);
        itsBaseRow.setOnClickListener(this);
        itsBaseLabel = root.findViewById(R.id.base_label);
        itsBase = root.findViewById(R.id.base);
        View baseBtn = root.findViewById(R.id.base_btn);
        baseBtn.setOnClickListener(this);
        itsGroupRow = root.findViewById(R.id.group_row);
        itsGroup = root.findViewById(R.id.group);
        itsUserRow = root.findViewById(R.id.user_row);
        itsUser = root.findViewById(R.id.user);
        itsPasswordRow = root.findViewById(R.id.password_row);
        itsPasswordRow.setOnClickListener(this);
        itsPassword = root.findViewById(R.id.password);
        itsPasswordSeek = root.findViewById(R.id.password_seek);
        itsPasswordSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser)
                    {
                        if (fromUser) {
                            updatePasswordShown(PasswordVisibilityChange.SEEK,
                                                progress, false);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar)
                    {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar)
                    {
                    }
                });

        itsPasswordSubsetBtn = root.findViewById(R.id.password_subset_btn);
        itsPasswordSubsetBtn.setOnCheckedChangeListener(this);
        itsPasswordSubsetBtn.setOnLongClickListener(this);
        itsPasswordSubsetInput = root.findViewById(R.id.password_subset_input);
        itsPasswordSubset = root.findViewById(R.id.password_subset);
        itsPasswordSubset.addTextChangedListener(new AbstractTextWatcher()
        {
            @Override
            public void afterTextChanged(Editable editable)
            {
                passwordSubsetChanged();
            }
        });

        itsPasswordSubset.setKeyListener(new NumberKeyListener()
        {
            @NonNull
            @Override
            protected char[] getAcceptedChars()
            {
                return SUBSET_CHARS;
            }

            @Override
            public int getInputType()
            {
                return InputType.TYPE_CLASS_NUMBER |
                       InputType.TYPE_NUMBER_FLAG_SIGNED;
            }
        });

        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        final OutputInterface.Language lang = Preferences.getAutoTypeLanguagePref(
                prefs);

        // Auto-Type USB username
        itsAutoTypeUsbUsername = root.findViewById(R.id.autotype_usb_username);
        itsAutoTypeUsbUsername.setOnClickListener(
                view -> autotypeUsb(lang, true, false, false));
        itsAutoTypeUsbUsername.setOnLongClickListener(view -> {
            autotypeUsbCustomLang(true, false, false);
            return true;
        });

        // Auto-Type USB password
        itsAutoTypeUsbPassword = root.findViewById(R.id.autotype_usb_password);
        itsAutoTypeUsbPassword.setOnClickListener(
                view -> autotypeUsb(lang, false, true, false));
        itsAutoTypeUsbPassword.setOnLongClickListener(view -> {
            autotypeUsbCustomLang(false, true, false);
            return true;
        });

        // Auto-Type USB OTP
        itsAutoTypeUsbOtp = root.findViewById(R.id.autotype_usb_otp);
        itsAutoTypeUsbOtp.setOnClickListener(
                view -> autotypeUsb(lang, false, false, true));
        itsAutoTypeUsbOtp.setOnLongClickListener(view -> {
            autotypeUsbCustomLang(true, false, false);
            return true;
        });

        // Auto-Type USB credentials
        itsAutoTypeUsbCredentials = root.findViewById(
                R.id.autotype_usb_credentials);
        itsAutoTypeUsbCredentials.setOnClickListener(
                view -> autotypeUsb(lang, true, true, false));
        itsAutoTypeUsbCredentials.setOnLongClickListener(view -> {
            autotypeUsbCustomLang(true, true, false);
            return true;
        });


        LinearLayout autotypeUsbRow = root.findViewById(R.id.autotype_usb_row);
        LinearLayout autotypeBluetoothRow = root.findViewById(
                R.id.autotype_bt_row);
        LinearLayout autotypeSettingsRow = root.findViewById(
                R.id.autotype_settings_row);

        itsAutoTypeBluetoothUsername = root.findViewById(
                R.id.autotype_bt_username);
        itsAutoTypeBluetoothPassword = root.findViewById(
                R.id.autotype_bt_password);
        itsAutoTypeBluetoothOtp = root.findViewById(R.id.autotype_bt_otp);
        itsAutoTypeBluetoothCredential = root.findViewById(
                R.id.autotype_bt_credentials);

        if (Preferences.getAutoTypeBluetoothEnabled(prefs) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Auto-Type Bluetooth username
            itsAutoTypeBluetoothUsername.setOnClickListener(
                    view -> autotypeBluetooth(lang, true, false, false));
            itsAutoTypeBluetoothUsername.setOnLongClickListener(view -> {
                autotypeBluetoothCustomLang(true, false, false);
                return true;
            });

            // Auto-Type Bluetooth password
            itsAutoTypeBluetoothPassword.setOnClickListener(
                    view -> autotypeBluetooth(lang, false, true, false));
            itsAutoTypeBluetoothPassword.setOnLongClickListener(view -> {
                autotypeBluetoothCustomLang(false, true, false);
                return true;
            });

            // Auto-Type Bluetooth OTP
            itsAutoTypeBluetoothOtp.setOnClickListener(
                    view -> autotypeBluetooth(lang, false, false, true));
            itsAutoTypeBluetoothOtp.setOnLongClickListener(view -> {
                autotypeBluetoothCustomLang(true, false, false);
                return true;
            });

            // Auto-Type Bluetooth credentials
            itsAutoTypeBluetoothCredential.setOnClickListener(
                    view -> autotypeBluetooth(lang, true, true, false));
            itsAutoTypeBluetoothCredential.setOnLongClickListener(view -> {
                autotypeBluetoothCustomLang(true, true, false);
                return true;
            });

        } else {
            autotypeBluetoothRow.setVisibility(View.GONE);
        }

        if (!Preferences.getAutoTypeUsbEnabled(prefs)) {
            autotypeUsbRow.setVisibility(View.GONE);
        }

        if (autotypeUsbRow.getVisibility() == View.GONE &&
            autotypeBluetoothRow.getVisibility() == View.GONE) {
            autotypeSettingsRow.setVisibility(View.GONE);
        }

        itsAutoTypeReturnSuffix = root.findViewById(
                R.id.autotype_return_suffix);
        itsAutoTypeReturnSuffix.setOnClickListener(view -> {
            Integer ival = 0;
            if (itsAutoTypeReturnSuffix.isChecked()) {
                ival = 1;
            }
            saveAutotypeReturnSuffix(ival);
        });

        itsAutoTypeDelimiter = root.findViewById(R.id.autotype_delimiter);

        View.OnClickListener autotypeDelimiterOnClickListener = view -> {
            Integer ival = 2;
            if (itsAutoTypeDelimiter.getCheckedRadioButtonId() ==
                R.id.autotype_delimiter_return) {
                ival = 1;
            }
            saveAutotypeDelimiterChange(ival);
        };
        root.findViewById(R.id.autotype_delimiter_return)
            .setOnClickListener(autotypeDelimiterOnClickListener);
        root.findViewById(R.id.autotype_delimiter_tab)
            .setOnClickListener(autotypeDelimiterOnClickListener);

        itsPasswordHideRun = () ->
                updatePasswordShown(PasswordVisibilityChange.INITIAL, 0, false);
        itsUrlRow = root.findViewById(R.id.url_row);
        itsUrl = root.findViewById(R.id.url);
        itsEmailRow = root.findViewById(R.id.email_row);
        itsEmail = root.findViewById(R.id.email);
        itsTimesRow = root.findViewById(R.id.times_row);
        itsCreationTimeRow = root.findViewById(R.id.creation_time_row);
        itsCreationTime = root.findViewById(R.id.creation_time);
        itsLastModTimeRow = root.findViewById(R.id.last_mod_time_row);
        itsLastModTime = root.findViewById(R.id.last_mod_time);
        itsProtectedRow = root.findViewById(R.id.protected_row);
        itsReferencesRow = root.findViewById(R.id.references_row);
        itsReferences = root.findViewById(R.id.references);
        itsReferences.setOnItemClickListener(
                (parent, view, position, id) -> showRefRec(false, position));

        registerForContextMenu(itsUserRow);
        registerForContextMenu(itsPasswordRow);
        updatePasswordShown(PasswordVisibilityChange.INITIAL, 0, false);


        itsOtpAdd = root.findViewById(R.id.otp_new);
        itsOtpAdd.setOnClickListener(view -> startOtpAddActivity(true));

        itsOtpAddCamera = root.findViewById(R.id.otp_new_camera);
        itsOtpAddCamera.setOnClickListener(view -> startOtpAddActivity(false));


        itsOtpCode = root.findViewById(R.id.otp_code);

        itsOtpTimer = root.findViewById(R.id.otp_time);
        itsOtpTokenRow = root.findViewById(R.id.otp_token_row);

        itsOtpCode.setOnClickListener(view -> {
            itsShowOtp = !itsShowOtp;
            generateOtpToken();
        });

        SUB_OTP = getResources().getString(R.string.SUB_OTP);
        SUB_TAB = getResources().getString(R.string.SUB_TAB);
        SUB_RETURN = getResources().getString(R.string.SUB_RETURN);

        return root;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Utilities.dbginfo(TAG, "onPause");
        if(itsOutputBluetoothKeyboard != null) {
            itsOutputBluetoothKeyboard.deinitializeBluetoothHidDevice();
            itsOutputBluetoothKeyboard = null;
        }

        itsPassword.removeCallbacks(itsPasswordHideRun);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // Check which request we're responding to
        if (requestCode == REQUEST_SAVE_OTP_MANUAL) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                saveOtpChange(data.getExtras().getString("uri"), false);
                PasswdSafeUtil.dbginfo("OTP",
                                       String.format("Store manual otp uri: %s",
                                                     data.getExtras()
                                                         .getString("uri")));
            }
        } else if (requestCode == REQUEST_SAVE_OTP_CAMERA) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                saveOtpChange(data.getExtras().getString("uri"), false);
                PasswdSafeUtil.dbginfo("OTP",
                                       String.format("Store camera otp uri: %s",
                                                     data.getExtras()
                                                         .getString("uri")));
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getActivity(), R.string.bluetooth_enabled,
                               Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), R.string.bluetooth_enable_error,
                               Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults)
    {
        switch (requestCode) {
        case PERMISSIONS_REQUEST_CAMERA: {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startOtpCameraAddActivity();
            } else {
                Toast.makeText(getActivity(),
                               R.string.error_permission_camera_open,
                               Toast.LENGTH_LONG).show();
            }
            return;
        }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.menu_toggle_password);
        if (item != null) {
            item.setTitle(
                    itsIsPasswordShown ?
                            R.string.hide_password : R.string.show_password);
            item.setEnabled(itsPasswordRow.getVisibility() == View.VISIBLE);
        }

        item = menu.findItem(R.id.menu_copy_url);
        if (item != null) {
            item.setVisible(itsUrlRow.getVisibility() == View.VISIBLE);
        }

        item = menu.findItem(R.id.menu_copy_email);
        if (item != null) {
            item.setVisible(itsEmailRow.getVisibility() == View.VISIBLE);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_copy_user) {
            copyUser();
            return true;
        } else if (itemId == R.id.menu_copy_password) {
            copyPassword();
            return true;
        } else if (itemId == R.id.menu_copy_url) {
            copyUrl();
            return true;
        } else if (itemId == R.id.menu_copy_email) {
            copyEmail();
            return true;
        } else if (itemId == R.id.menu_toggle_password) {
            updatePasswordShown(PasswordVisibilityChange.TOGGLE, 0, false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu,
                                    @NonNull View view,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.setHeaderTitle(itsTitle);
        int id = view.getId();
        if (id == R.id.user_row) {
            menu.add(PasswdSafe.CONTEXT_GROUP_RECORD_BASIC, R.id.menu_copy_user,
                     0, R.string.copy_user);
        } else if (id == R.id.password_row) {
            menu.add(PasswdSafe.CONTEXT_GROUP_RECORD_BASIC,
                     R.id.menu_copy_password, 0, R.string.copy_password);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        if (item.getGroupId() != PasswdSafe.CONTEXT_GROUP_RECORD_BASIC) {
            return super.onContextItemSelected(item);
        }

        int itemId = item.getItemId();
        if (itemId == R.id.menu_copy_password) {
            copyPassword();
            return true;
        } else if (itemId == R.id.menu_copy_user) {
            copyUser();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View view)
    {
        int id = view.getId();
        if ((id == R.id.base_row) || (id == R.id.base_btn)) {
            showRefRec(true, 0);
        } else if (id == R.id.password_row) {
            updatePasswordShown(PasswordVisibilityChange.TOGGLE, 0, false);
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        if (v.getId() == R.id.password_subset_btn) {
            Toast.makeText(getContext(), R.string.password_subset,
                           Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton btn, boolean checked)
    {
        if (btn.getId() == R.id.password_subset_btn) {
            updatePasswordShown(PasswordVisibilityChange.SHOW_SUBSET, 0,
                                checked);
        }
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_record_basic, menu);
    }

    @Override
    protected void doRefresh(@NonNull RecordInfo info)
    {
        PwsRecord ref = info.itsPasswdRec.getRef();
        PwsRecord recForPassword = info.itsRec;
        int hiddenId = R.string.hidden_password_normal;
        String url = null;
        String email = null;
        String otp = null;
        Date creationTime = null;
        Date lastModTime = null;
        Integer autotypeDelimiter = null;
        Integer autotypeReturnSuffix = null;
        switch (info.itsPasswdRec.getType()) {
        case NORMAL: {
            itsBaseRow.setVisibility(View.GONE);
            url = info.itsFileData.getURL(info.itsRec,
                                          PasswdFileData.UrlStyle.FULL);
            email = info.itsFileData.getEmail(info.itsRec,
                                              PasswdFileData.EmailStyle.FULL);
            otp = info.itsFileData.getOtp(info.itsRec);
            creationTime = info.itsFileData.getCreationTime(info.itsRec);
            lastModTime = info.itsFileData.getLastModTime(info.itsRec);

            autotypeDelimiter = info.itsFileData.getAutotypeDelimiter(
                    info.itsRec);
            autotypeReturnSuffix = info.itsFileData.getAutotypeReturnSuffix(
                    info.itsRec);
            break;
        }
        case ALIAS: {
            itsBaseRow.setVisibility(View.VISIBLE);
            itsBaseLabel.setText(R.string.alias_base_record_header);
            itsBase.setText(info.itsFileData.getId(ref));
            hiddenId = R.string.hidden_password_alias;
            recForPassword = ref;
            url = info.itsFileData.getURL(info.itsRec,
                                          PasswdFileData.UrlStyle.FULL);
            email = info.itsFileData.getEmail(info.itsRec,
                                              PasswdFileData.EmailStyle.FULL);
            otp = info.itsFileData.getOtp(info.itsRec);
            creationTime = info.itsFileData.getCreationTime(recForPassword);
            lastModTime = info.itsFileData.getLastModTime(recForPassword);

            autotypeDelimiter = info.itsFileData.getAutotypeDelimiter(
                    info.itsRec);
            autotypeReturnSuffix = info.itsFileData.getAutotypeReturnSuffix(
                    info.itsRec);
            break;
        }
        case SHORTCUT: {
            itsBaseRow.setVisibility(View.VISIBLE);
            itsBaseLabel.setText(R.string.shortcut_base_record_header);
            itsBase.setText(info.itsFileData.getId(ref));
            hiddenId = R.string.hidden_password_shortcut;
            recForPassword = ref;
            creationTime = info.itsFileData.getCreationTime(recForPassword);
            lastModTime = info.itsFileData.getLastModTime(recForPassword);
            break;
        }
        }

        itsTitle = info.itsFileData.getTitle(info.itsRec);
        setFieldText(itsGroup, itsGroupRow,
                     info.itsFileData.getGroup(info.itsRec));
        setFieldText(itsUser, itsUserRow,
                     info.itsFileData.getUsername(info.itsRec));

        itsIsPasswordShown = false;
        itsHiddenPasswordStr = getString(hiddenId);
        String password = info.itsFileData.getPassword(recForPassword);
        setFieldText(itsPassword, itsPasswordRow,
                     ((password != null) ? itsHiddenPasswordStr : null));
        int passwordLen = (password != null) ? password.length() : 0;
        itsPasswordSeek.setMax(passwordLen);
        itsSubsetErrorStr = getString(R.string.password_subset_error,
                                      passwordLen);
        updatePasswordShown(PasswordVisibilityChange.INITIAL, 0, false);

        setFieldText(itsUrl, itsUrlRow, url);
        setFieldText(itsEmail, itsEmailRow, email);

        if (otp != null && !otp.equals("")) {
            itsOtpTokenRow.setVisibility(View.VISIBLE);
            itsAutoTypeUsbOtp.setEnabled(true);
            itsAutoTypeBluetoothOtp.setEnabled(true);
        }

        if (itsUserRow.getVisibility() == View.GONE) {
            itsAutoTypeUsbUsername.setEnabled(false);
            itsAutoTypeBluetoothUsername.setEnabled(false);
        }

        if (autotypeDelimiter != null && autotypeDelimiter == 1) {
            itsAutoTypeDelimiter.check(R.id.autotype_delimiter_return);
        }

        if (autotypeReturnSuffix != null && autotypeReturnSuffix == 1) {
            itsAutoTypeReturnSuffix.setChecked(true);
        }

        GuiUtils.setVisible(itsTimesRow,
                            (creationTime != null) || (lastModTime != null));
        setFieldDate(itsCreationTime, itsCreationTimeRow, creationTime);
        setFieldDate(itsLastModTime, itsLastModTimeRow, lastModTime);
        GuiUtils.setVisible(itsProtectedRow,
                            info.itsFileData.isProtected(info.itsRec));

        List<PwsRecord> references = info.itsPasswdRec.getRefsToRecord();
        boolean hasReferences = (references != null) && !references.isEmpty();
        if (hasReferences) {
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(requireActivity(),
                                       android.R.layout.simple_list_item_1);
            for (PwsRecord refRec : references) {
                adapter.add(info.itsFileData.getId(refRec));
            }
            itsReferences.setAdapter(adapter);
        } else {
            itsReferences.setAdapter(null);
        }
        GuiUtils.setListViewHeightBasedOnChildren(itsReferences);
        GuiUtils.setVisible(itsReferencesRow, hasReferences);

        requireActivity().invalidateOptionsMenu();
    }

    private void generateOtpToken()
    {
        String otp = getOtp();
        Token token = null;
        try {
            PasswdSafeUtil.dbginfo("OTP", String.format("LOAD OTP: %s", otp));
            token = new Token(otp, false);
            itsOtp = token.generateCodes();

            if (token.getType() == Token.TokenType.HOTP) {
                PasswdSafeUtil.dbginfo("OTP", String.format(
                        "Saving incremented HOTP counter: %d",
                        token.getCounter()));
                saveOtpChange(token.toString(), true);
            }
            setFieldText(itsOtpCode, null, itsShowOtp ? itsOtp.getCurrentCode() : "XXXXXX");

            itsOtpTimer.setProgress(itsOtp.getCurrentProgress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        CountDownTimer otpTimeCountDown;
        otpTimeCountDown = new CountDownTimer(30000, 500)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                String currentCode = itsOtp.getCurrentCode();
                int currentProgress = itsOtp.getCurrentProgress();

                if (currentCode == null) {
                    setFieldText(itsOtpCode, null, "------");
                    itsOtpTimer.setProgress(0);
                    cancel();

                    return;
                }

                itsOtpTimer.setProgress(currentProgress);
                setFieldText(itsOtpCode, null, itsShowOtp ? itsOtp.getCurrentCode() : "XXXXXX");
            }

            @Override
            public void onFinish()
            {
                start();
            }
        };
        otpTimeCountDown.start();
    }

    /**
     * Show a referenced record
     */
    private void showRefRec(final boolean baseRef, final int referencingPos)
    {
        PasswdLocation location = useRecordInfo(info -> {
            PwsRecord refRec = null;
            if (baseRef) {
                refRec = info.itsPasswdRec.getRef();
            } else {
                List<PwsRecord> refs = info.itsPasswdRec.getRefsToRecord();
                if ((referencingPos >= 0) && (referencingPos < refs.size())) {
                    refRec = refs.get(referencingPos);
                }
            }
            if (refRec == null) {
                return null;
            }

            return new PasswdLocation(refRec, info.itsFileData);
        });

        if (location != null) {
            getListener().changeLocation(location);
        }
    }

    /**
     * Update whether the password is shown
     */
    private void updatePasswordShown(PasswordVisibilityChange change,
                                     int progress,
                                     boolean showSubset)
    {
        String password = null;
        boolean seekShown = true;
        boolean subsetShown = false;

        switch (change) {
        case INITIAL: {
            itsIsPasswordShown = false;
            itsPasswordSeek.setProgress(0);
            break;
        }
        case TOGGLE: {
            itsIsPasswordShown = !itsIsPasswordShown;
            if (itsIsPasswordShown) {
                password = getPassword();
            }
            itsPasswordSeek.setProgress(
                    itsIsPasswordShown ? itsPasswordSeek.getMax() : 0);
            break;
        }
        case SEEK: {
            if (progress == 0) {
                itsIsPasswordShown = false;
                password = itsHiddenPasswordStr;
            } else {
                itsIsPasswordShown = true;
                password = getPassword();
                if ((password != null) && (progress < password.length())) {
                    password = password.substring(0, progress) + "…";
                }
            }
            break;
        }
        case SHOW_SUBSET: {
            itsPasswordSeek.setProgress(0);
            if (showSubset) {
                seekShown = false;
                subsetShown = true;
                itsIsPasswordShown = true;
                password = "";
                itsPasswordSubset.setText(null);
            } else {
                itsIsPasswordShown = false;
            }
            break;
        }
        }

        itsPasswordSubsetBtn.setChecked(subsetShown);
        GuiUtils.setVisible(itsPasswordSeek, seekShown);
        GuiUtils.setTextInputVisible(itsPasswordSubsetInput, subsetShown);
        if (subsetShown) {
            itsPasswordSubset.requestFocus();
        }
        Activity act = requireActivity();
        GuiUtils.setKeyboardVisible(itsPasswordSubset, act, subsetShown);
        itsPassword.setText(
                (password != null) ? password : itsHiddenPasswordStr);
        TypefaceUtils.enableMonospace(itsPassword, itsIsPasswordShown, act);
        itsPassword.removeCallbacks(itsPasswordHideRun);
        if (itsIsPasswordShown) {
            SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
            PasswdTimeoutPref timeout =
                    Preferences.getPasswdVisibleTimeoutPref(prefs);
            switch (timeout) {
            case TO_15_SEC:
            case TO_30_SEC:
            case TO_1_MIN:
            case TO_5_MIN: {
                itsPassword.postDelayed(itsPasswordHideRun,
                                        timeout.getTimeout());
                break;
            }
            case TO_NONE: {
                break;
            }
            }
        }
        act.invalidateOptionsMenu();
    }


    /**
     * Handle a change in the password subset to show
     */
    private void passwordSubsetChanged()
    {
        String subset = itsPasswordSubset.getText().toString();
        String password = getPassword();
        if (password == null) {
            itsPassword.setText("");
            return;
        }
        int passwordLen = password.length();
        StringBuilder passwordSubset = new StringBuilder();
        boolean error = false;
        String[] tokens = TextUtils.split(subset, SUBSET_SPLIT);
        for (int i = 0; i < tokens.length; ++i) {
            String trimToken = tokens[i].trim();
            if ((trimToken.length() == 0) ||
                ((i == (tokens.length - 1)) && trimToken.equals("-"))) {
                continue;
            }
            int idx;
            try {
                idx = Integer.parseInt(trimToken);
            } catch (Exception e) {
                error = true;
                break;
            }
            char c;
            if ((idx > 0) && (idx <= passwordLen)) {
                c = password.charAt(idx - 1);
            } else if ((idx < 0) && (-idx <= passwordLen)) {
                c = password.charAt(passwordLen + idx);
            } else {
                error = true;
                break;
            }
            if (passwordSubset.length() > 0) {
                passwordSubset.append(" ");
            }
            passwordSubset.append(c);
        }
        String errorStr = "Positions are separated by spaces, commas, or " +
                          "semicolons. They are numbered from 1 to the " +
                          String.format("password length, %d. ", passwordLen) +
                          "Negative values count from the end.";
        TextInputUtils.setTextInputError(
                error ? errorStr : null, itsPasswordSubsetInput);
        itsPassword.setText(passwordSubset.toString());
    }


    public void enableBluetooth()
    {
        Intent enableBtIntent = new Intent(
                BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }


    /**
     * Auto-Type over Bluetooth HID
     */
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.P)
    private void autotypeBluetooth(OutputInterface.Language lang,
                                   Boolean sendUsername, Boolean sendPassword,
                                   Boolean sendOTP)
    {
        itsOutputBluetoothKeyboard = new OutputBluetoothKeyboard(lang, getContext());
        if (!itsOutputBluetoothKeyboard.checkBluetoothStatus()) {
            Toast.makeText(getActivity(), "Bluetooth is disabled",
                           Toast.LENGTH_LONG).show();
            return;
        }

        String username = getUsername();
        String password = getPassword();
        String otp = getOtp();
        String quoteSubReturn = Pattern.quote(SUB_RETURN);
        String quoteSubTab = Pattern.quote(SUB_TAB);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            boolean otpTokenGenerated = false;

            if (sendOTP && otp != null) {
                generateOtpToken();
                otpTokenGenerated = true;

                try {
                    outputStream.write(itsOutputBluetoothKeyboard.convertTextToScancode(
                                    itsOtp.getCurrentCode()));
                } catch (Exception e) {
                    PasswdSafeUtil.showErrorMsg(
                            "Invalid OTP token generated! - " +
                            e.getLocalizedMessage(),
                            new ActContext(getContext()));
                }
            }

            if (sendUsername && username != null) {
                if (username.contains(SUB_OTP)) {
                    generateOtpToken();
                    otpTokenGenerated = true;

                    username = username.replace(SUB_OTP, itsOtp.getCurrentCode());
                }

                String[] usernameArray = username.split(
                        String.format("((?<=(%1$s|%2$s))|(?=(%1$s|%2$s)))",
                                      quoteSubReturn, quoteSubTab));
                PasswdSafeUtil.dbginfo(TAG, String.format(
                        "Username Substitution Array: %s",
                        Arrays.toString(usernameArray)));

                for (String str : usernameArray) {

                    if (str.equals(SUB_RETURN)) {
                        outputStream.write(itsOutputBluetoothKeyboard.getReturn());
                    } else if (str.equals(SUB_TAB)) {
                        outputStream.write(itsOutputBluetoothKeyboard.getTabulator());
                    } else {
                        outputStream.write(itsOutputBluetoothKeyboard.convertTextToScancode(str));
                    }
                }
            }

            if (sendUsername && sendPassword) {
                int checkedId = itsAutoTypeDelimiter.getCheckedRadioButtonId();
                if (checkedId == R.id.autotype_delimiter_return) {
                    outputStream.write(itsOutputBluetoothKeyboard.getReturn());
                } else if (checkedId == R.id.autotype_delimiter_tab) {
                    outputStream.write(itsOutputBluetoothKeyboard.getTabulator());
                }
            }

            if (sendPassword && password != null) {
                if (password.contains(SUB_OTP)) {
                    if (!otpTokenGenerated) {
                        generateOtpToken();
                    }

                    password = password.replace(SUB_OTP, itsOtp.getCurrentCode());
                }

                String[] passwordArray = password.split(
                        String.format("((?<=(%1$s|%2$s))|(?=(%1$s|%2$s)))",
                                      quoteSubReturn, quoteSubTab));
                PasswdSafeUtil.dbginfo(TAG, String.format(
                        "Password Substitution Array: %s",
                        Arrays.toString(passwordArray)));

                for (String str : passwordArray) {

                    if (str.equals(SUB_RETURN)) {
                        outputStream.write(
                                itsOutputBluetoothKeyboard.getReturn());
                    } else if (str.equals(SUB_TAB)) {
                        outputStream.write(
                                itsOutputBluetoothKeyboard.getTabulator());
                    } else {
                        outputStream.write(
                                itsOutputBluetoothKeyboard.convertTextToScancode(
                                        str));
                    }
                }

                if (itsAutoTypeReturnSuffix.isChecked()) {
                    outputStream.write(itsOutputBluetoothKeyboard.getReturn());
                }
            }

        } catch (Exception e) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", e,
                                   e.getLocalizedMessage());
        }

        Set<BluetoothDevice> bondedDevices = itsOutputBluetoothKeyboard.getBondedDevices();

        SortedMap<String, BluetoothDevice> deviceList = new TreeMap<>();
        bondedDevices.forEach(device -> deviceList.put(device.getName(), device));
        CharSequence[] cs = deviceList.keySet().toArray(new CharSequence[deviceList.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.autotype_bluetooth_devices)
               .setItems(cs, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       itsOutputBluetoothKeyboard.connectDeviceAndSend( deviceList.get(cs[which]), outputStream.toByteArray() );
                   }
               });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    /**
     * Auto-Type over Bluetooth HID
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private void autotypeBluetooth(String sLang, Boolean sendUsername, Boolean sendPassword, Boolean sendOtp)
    {

        if( sLang.equals("null") ) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "Getting systems language default");
            sLang = Locale.getDefault().toString();
        }

        PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "Identified language: " + sLang);

        OutputInterface.Language lang;
        try {
            lang = OutputInterface.Language.valueOf(sLang);
        } catch (IllegalArgumentException e) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "No scancode mapping for '" + sLang +"' - using en_US!");
            lang = OutputInterface.Language.en_US;
        }

        autotypeBluetooth(lang, sendUsername, sendPassword, sendOtp);
    }

    @RequiresApi(Build.VERSION_CODES.P)
    public void autotypeBluetoothCustomLang(final Boolean sendUsername,
                                            final Boolean sendPassword,
                                            final Boolean sendOTP)
    {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.autotype_lang)
               .setItems(R.array.autotype_lang_titles, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       autotypeBluetooth(getResources().getStringArray(R.array.autotype_lang_values)[which], sendUsername, sendPassword, sendOTP );
                   }
               });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }



    /**
     * Auto-Type credential over USB HID
     */
    private void autotypeUsb(OutputInterface.Language lang,
                             Boolean sendUsername, Boolean sendPassword, Boolean sendOTP)
    {
        String username = getUsername();
        String password = getPassword();
        String otp = getOtp();
        String quoteSubReturn = Pattern.quote(SUB_RETURN);
        String quoteSubTab = Pattern.quote(SUB_TAB);

        try {
            boolean nativeMode = Preferences.getUsbNativeEnabled(Preferences.getSharedPrefs(getContext()));
            OutputInterface ct = nativeMode ? new OutputUsbKeyboard(lang) : new OutputUsbKeyboardAsRoot(lang);
            boolean otpTokenGenerated = false;

            if(sendOTP && otp != null ) {
                generateOtpToken();
                otpTokenGenerated = true;

                int ret = 0;
                ret = ct.sendText(itsOtp.getCurrentCode());

                if (ret == 1) {
                    PasswdSafeUtil.showErrorMsg(
                            "Unvalid OTP token generated!",
                            new ActContext(getContext()));
                }
            }

            if(sendUsername && username != null ) {
                if (username.contains(SUB_OTP)){
                    generateOtpToken();
                    otpTokenGenerated = true;

                    username = username.replace(SUB_OTP, itsOtp.getCurrentCode());
                }

                String[] usernameArray = username.split(String.format("((?<=(%1$s|%2$s))|(?=(%1$s|%2$s)))", quoteSubReturn, quoteSubTab));
                PasswdSafeUtil.dbginfo(TAG, String.format("Username Substitution Array: %s", Arrays.toString(usernameArray)));

                int ret = 0;
                for (String str : usernameArray){

                    if (str.equals(SUB_RETURN)) {
                        ct.sendReturn();
                    } else if (str.equals(SUB_TAB)) {
                        ct.sendTabulator();
                    } else {
                        ret = ct.sendText(str);
                    }

                    if (ret == 1) {
                        PasswdSafeUtil.showErrorMsg(
                                "Lost characters in output due to missing mapping!",
                                new ActContext(getContext()));
                    }
                }
            }

            if( sendUsername && sendPassword )
            {
                int checkedId = itsAutoTypeDelimiter.getCheckedRadioButtonId();
                if (checkedId == R.id.autotype_delimiter_return) {
                    ct.sendReturn();
                } else if (checkedId == R.id.autotype_delimiter_tab) {
                    ct.sendTabulator();
                }
            }

            if( sendPassword && password != null ) {
                if (password.contains(SUB_OTP)){
                    if (!otpTokenGenerated) {
                        generateOtpToken();
                    }

                    password = password.replace(SUB_OTP, itsOtp.getCurrentCode());
                }

                String[] passwordArray = password.split(String.format("((?<=(%1$s|%2$s))|(?=(%1$s|%2$s)))", quoteSubReturn, quoteSubTab));
                PasswdSafeUtil.dbginfo(TAG, String.format("Password Substitution Array: %s", Arrays.toString(passwordArray)));

                int ret = 0;
                for (String str : passwordArray){

                    if (str.equals(SUB_RETURN)) {
                        ct.sendReturn();
                    } else if (str.equals(SUB_TAB)) {
                        ct.sendTabulator();
                    } else {
                        ret = ct.sendText(str);
                    }

                    if (ret == 1) {
                        PasswdSafeUtil.showErrorMsg(
                                "Lost characters in output due to missing mapping!",
                                new ActContext(getContext()));
                    }
                }

                if( itsAutoTypeReturnSuffix.isChecked() ) {
                    ct.sendReturn();
                }
            }

            ct.destruct();

        } catch (SecurityException e) {
            PasswdSafeUtil.showErrorMsg(getResources().getString(R.string.autotype_usb_root_denied), new ActContext(requireContext()));
        } catch (FileNotFoundException e) {
            PasswdSafeUtil.showErrorMsg(getResources().getString(R.string.autotype_usb_hidg_not_found), new ActContext(requireContext()));
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", e, e.getLocalizedMessage());
            PasswdSafeUtil.showErrorMsg(String.format("PasswdSafeRecordBasicFragment Exception: %s", e.getLocalizedMessage()), new ActContext(requireContext()));
        }

    }

    /**
     * Auto-Type over USB HID
     */
    private void autotypeUsb(String sLang, Boolean sendUsername, Boolean sendPassword, Boolean sendOtp)
    {

        if( sLang.equals("null") ) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "Getting systems language default");
            sLang = Locale.getDefault().toString();
        }

        PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "Identified language: " + sLang);

        OutputInterface.Language lang;
        try {
            lang = OutputInterface.Language.valueOf(sLang);
        } catch (IllegalArgumentException e) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "No scancode mapping for '" + sLang +"' - using en_US!");
            lang = OutputInterface.Language.en_US;
        }

        autotypeUsb(lang, sendUsername, sendPassword, sendOtp);
    }

    public void autotypeUsbCustomLang(final Boolean sendUsername,
                                      final Boolean sendPassword,
                                      final Boolean sendOTP)
    {

        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.autotype_lang)
               .setItems(R.array.autotype_lang_titles,
                         (dialog, which) -> autotypeUsb(getResources().getStringArray(R.array.autotype_lang_values)[which], sendUsername, sendPassword, sendOTP ));

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    private void startOtpAddActivity(final boolean manual) {
        if (hasOtp()) {
            Context ctx = getContext();
            LayoutInflater factory = LayoutInflater.from(ctx);
            View dlgView = factory.inflate(R.layout.confirm_prompt, null);

            final CheckBox itsConfirmCb = (CheckBox)dlgView.findViewById(R.id.confirm);
            AlertDialog.Builder alert = new AlertDialog.Builder(ctx)
                    .setTitle(getString(R.string.otp_overwrite))
                    .setView(dlgView)
                    .setPositiveButton(R.string.replace,
                                       (dialog, whichButton) -> {
                                           if (itsConfirmCb.isChecked()) {
                                               if(manual) {
                                                   startOtpManualAddActivity();
                                               } else {
                                                   checkAndStartOtpCameraAddActivity();
                                               }
                                           }
                                       })
                    .setNegativeButton(R.string.cancel, null);
            alert.show();
        } else if (manual) {
            startOtpManualAddActivity();
        } else {
            checkAndStartOtpCameraAddActivity();
        }
    }

    private void startOtpManualAddActivity() {
        Intent intent = new Intent(getActivity(), AddActivity.class);
        startActivityForResult(intent, REQUEST_SAVE_OTP_MANUAL);
    }

    private void startOtpCameraAddActivity() {
        Intent intent = new Intent(getActivity(), ScanActivity.class);
        startActivityForResult(intent, REQUEST_SAVE_OTP_CAMERA);

        getActivity().overridePendingTransition(R.xml.fadein, R.xml.fadeout);
    }

    private void checkAndStartOtpCameraAddActivity() {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        }
        else {
            // permission is already granted
            startOtpCameraAddActivity();
        }
    }

    private void saveOtpChange(final String otpUri, final boolean counterUpdate) {
        final ObjectHolder<Pair<Boolean, PasswdLocation>> rc = new ObjectHolder<>();
        useRecordFile((RecordFileUser)(info, fileData) -> {

            PwsRecord record;
            boolean newRecord;
            if (info != null) {
                record = info.itsRec;
                newRecord = false;
            } else {
                record = fileData.createRecord();
                record.setLoaded();
                newRecord = true;
            }

            if(fileData.isProtected(record)) {
                return null;
            }

            /*if(!fileData.canEdit()) {
                return null;
            }*/

            String oldOtp = fileData.getOtp(record);
            if (!otpUri.equals(oldOtp)) {
                fileData.setOtp(otpUri, record);

                PasswdHistory hist = fileData.getPasswdHistory(record);
                if (oldOtp != null && !oldOtp.equals("") && hist != null && !counterUpdate) {
                    Date passwdDate = fileData.getPasswdLastModTime(record);
                    if (passwdDate == null) {
                        passwdDate = fileData.getCreationTime(record);
                    }

                    hist.addPasswd(oldOtp, passwdDate);
                    fileData.setPasswdHistory(hist, record, true);
                }
            }

            if (newRecord) {
                fileData.addRecord(record);
            }

            rc.set(new Pair<>((newRecord || record.isModified()), new PasswdLocation(record, fileData)));

            return null;
        });

        if (rc == null || rc.get() == null) {
            return;
        }
        getListener().finishEditRecord(rc.get().first, rc.get().second, false);
    }

    private void saveAutotypeDelimiterChange(final Integer itemValue) {
        final ObjectHolder<Pair<Boolean, PasswdLocation>> rc = new ObjectHolder<>();
        useRecordFile((RecordFileUser)(info, fileData) -> {

            PwsRecord record;
            boolean newRecord;
            if (info != null) {
                record = info.itsRec;
                newRecord = false;
            } else {
                record = fileData.createRecord();
                record.setLoaded();
                newRecord = true;
            }

            if( fileData.isProtected(record)) {
                return null;
            }

            if (fileData.getAutotypeDelimiter(record) != itemValue) {
                fileData.setAutotypeDelimiter(itemValue, record);
            }

            if (newRecord) {
                fileData.addRecord(record);
            }

            rc.set(new Pair<>((newRecord || record.isModified()), new PasswdLocation(record, fileData)));

            return null;
        });

        if (rc == null || rc.get() == null) {
            return;
        }
        getListener().finishEditRecord(rc.get().first, rc.get().second, false);
    }

    private void saveAutotypeReturnSuffix(final Integer itemValue) {
        final ObjectHolder<Pair<Boolean, PasswdLocation>> rc = new ObjectHolder<>();
        useRecordFile((RecordFileUser)(info, fileData) -> {

            PwsRecord record;
            boolean newRecord;
            if (info != null) {
                record = info.itsRec;
                newRecord = false;
            } else {
                record = fileData.createRecord();
                record.setLoaded();
                newRecord = true;
            }

            if( fileData.isProtected(record)) {
                return null;
            }

            if (fileData.getAutotypeReturnSuffix(record) != itemValue) {
                fileData.setAutotypeReturnSuffix(itemValue, record);
            }

            if (newRecord) {
                fileData.addRecord(record);
            }

            rc.set(new Pair<>((newRecord || record.isModified()), new PasswdLocation(record, fileData)));

            return null;
        });

        if (rc == null || rc.get() == null) {
            return;
        }
        getListener().finishEditRecord(rc.get().first, rc.get().second, false);
    }

    /**
     * Copy the user name to the clipboard
     */
    private void copyUser()
    {
        getListener().copyField(CopyField.USER_NAME, getLocation().getRecord());
    }

    /**
     * Copy the password to the clipboard
     */
    private void copyPassword()
    {
        getListener().copyField(CopyField.PASSWORD, getLocation().getRecord());
    }

    /**
     * Copy the URL to the clipboard
     */
    private void copyUrl()
    {
        getListener().copyField(CopyField.URL, getLocation().getRecord());
    }

    /**
     * Copy the email to the clipboard
     */
    private void copyEmail()
    {
        getListener().copyField(CopyField.EMAIL, getLocation().getRecord());
    }

    /**
     * Get the username
     */
    private String getUsername()
    {
        return useRecordInfo(info -> info.itsFileData.getUsername(info.itsRec));
    }

    /**
     * Get the password
     */
    private @Nullable String getPassword()
    {
        return useRecordInfo(info -> info.itsPasswdRec.getPassword(info.itsFileData));
    }

    /**
     * Get the otp
     */
    private String getOtp()
    {
        return useRecordInfo(info -> info.itsFileData.getOtp(info.itsRec));
    }

    /**
     * Check if otp is set
     */
    private boolean hasOtp()
    {
        String otpUri = getOtp();
        return (otpUri != null && !otpUri.equals(""));
    }
}
