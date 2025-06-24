/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.Utils;
import net.tjado.passwdsafe.util.ClearingByteArrayOutputStream;
import net.tjado.passwdsafe.util.YubiState;

import org.pwsafe.lib.Util;
import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

import java.io.UnsupportedEncodingException;


/**
 * The YubikeyMgr class encapsulates the interaction with a YubiKey
 */
public class YubikeyMgr
{
    /// Command to select the app running on the key
    private static final byte[] SELECT_CMD =
            {0x00, (byte) 0xA4, 0x04, 0x00, 0x07,
             (byte) 0xA0, 0x00, 0x00, 0x05, 0x27, 0x20, 0x01, 0x00};
    /// Command to perform a hash operation
    private static final byte[] HASH_CMD = {0x00, 0x01, 0x00, 0x00 };

    private static final byte SLOT_CHAL_HMAC1 = 0x30;
    private static final byte SLOT_CHAL_HMAC2 = 0x38;

    private static final int SHA1_MAX_BLOCK_SIZE = 64;

    private static final boolean TEST = false;//PasswdSafeUtil.DEBUG;

    private static final String TAG = "YubikeyMgr";

    private User itsUser = null;
    private boolean itsIsRegistered = false;
    private PendingIntent itsTagIntent = null;
    private CountDownTimer itsTimer = null;

    /// Interface for a user of the YubikeyMgr
    public interface User
    {
        /// Get the activity using the key
        Activity getActivity();

        /// Get the password to be sent to the key
        @CheckResult @Nullable
        Owner<PwsPassword> getUserPassword();

        /// Get the slot number to use on the key
        int getSlotNum();

        /// Finish interaction with the key
        void finish(Owner<PwsPassword>.Param password, Exception e);

        /// Handle an update on the timer until the start times out
        void timerTick(@SuppressWarnings("SameParameterValue") int totalTime,
                       int remainingTime);
    }

    /** Get the state of support for the Yubikey */
    public YubiState getState(Activity act)
    {
        if (TEST) {
            return YubiState.ENABLED;
        }

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(act);
        if (adapter == null) {
            return YubiState.UNAVAILABLE;
        } else if (!adapter.isEnabled()) {
            return YubiState.DISABLED;
        }
        return YubiState.ENABLED;
    }

    /// Start the interaction with the YubiKey
    @SuppressLint("UnspecifiedImmutableFlag")
    public void start(User user)
    {
        if (itsUser != null) {
            stop();
        }

        itsUser = user;
        Activity act = itsUser.getActivity();
        if (itsTagIntent == null) {
            Intent intent = new Intent(act, act.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            itsTagIntent = PendingIntent.getActivity(
                    act, 0, intent, ApiCompat.getPendingIntentMutableFlag());
        }

        if (TEST) {
            new CountDownTimer(5000, 5000) {
                @Override
                public void onTick(long millisUntilFinished)
                {
                }

                @Override
                public void onFinish()
                {
                    if (itsUser != null) {
                        try (Owner<PwsPassword> password =
                                     itsUser.getUserPassword()) {
                            if (password == null) {
                                stopUser(null, null);
                                return;
                            }
                            String utf8 = "UTF-8";
                            byte[] bytes = password.get().getBytes(utf8);
                            String passwordStr =
                                    new String(bytes, utf8).toLowerCase();
                            try (Owner<PwsPassword> newPassword =
                                         PwsPassword.create(passwordStr)) {
                                stopUser(newPassword.pass(), null);
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.e(TAG, "encode error", e);
                        }
                    }
                }
            }.start();
        } else if (!itsIsRegistered) {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(act);
            if (adapter == null) {
                Toast.makeText(act, "NO NFC", Toast.LENGTH_LONG).show();
                return;
            }

            if (!adapter.isEnabled()) {
                Toast.makeText(act, "NFC DISABLED", Toast.LENGTH_LONG).show();
                return;
            }

            IntentFilter iso =
                    new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
            adapter.enableForegroundDispatch(
                    act, itsTagIntent, new IntentFilter[] { iso },
                    new String[][]
                            { new String[] { IsoDep.class.getName() } });
            itsIsRegistered = true;
        }

        itsTimer = new CountDownTimer(30 * 1000, 1 * 1000) {
            @Override
            public void onFinish()
            {
                stop();
            }

            @Override
            public void onTick(long millisUntilFinished)
            {
                itsUser.timerTick(30, (int)(millisUntilFinished / 1000));
            }
        };
        itsTimer.start();
    }

    /** Handle a pause of the activity */
    public void onPause()
    {
        if (itsUser == null) {
            return;
        }
        Activity act = itsUser.getActivity();

        if (itsIsRegistered) {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(act);
            if ((adapter == null) || !adapter.isEnabled()) {
                return;
            }

            adapter.disableForegroundDispatch(act);
            itsIsRegistered = false;
        }

        if (itsTagIntent != null) {
            itsTagIntent.cancel();
            itsTagIntent = null;
        }
    }

    /// Stop the interaction with the key
    public void stop()
    {
        onPause();
        stopUser(null, null);
        itsTimer = null;
        itsUser = null;
    }

    /// Handle the intent for when the key is discovered
    public void handleKeyIntent(Intent intent)
    {
        if (!NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "calculate");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if ((tag == null) || (itsUser == null)) {
            return;
        }

        IsoDep isotag = IsoDep.get(tag);
        try {
            isotag.connect();

            ClearingByteArrayOutputStream cmd =
                    new ClearingByteArrayOutputStream();
            byte[] resp = null;
            try (Owner<PwsPassword> userPassword = itsUser.getUserPassword()) {
                if (userPassword == null) {
                    throw new Exception("No password");
                }

                resp = isotag.transceive(SELECT_CMD);
                checkResponse(resp);
                Util.clearArray(resp);

                PwsPassword pw = userPassword.get();
                cmd.write(HASH_CMD);

                // Placeholder for length
                byte datalen;
                cmd.write(0);

                int pwlen = pw.length();
                if (pwlen > 0) {
                    if (pwlen > SHA1_MAX_BLOCK_SIZE / 2) {
                        pwlen = SHA1_MAX_BLOCK_SIZE / 2;
                    }
                    // Chars are encoded as little-endian UTF-16.  A trailing
                    // zero must be skipped as the PC API will skip it.
                    datalen = 0;
                    for (int i = 0; i < pwlen - 1; ++i) {
                        datalen += 2;
                        char c = pw.charAt(i);
                        cmd.write(c & 0xff);
                        cmd.write((c >> 8) & 0xff);
                    }

                    char c = pw.charAt(pwlen - 1);
                    cmd.write(c & 0xff);
                    ++datalen;
                    int last = (c >> 8) & 0xff;
                    if (last != 0) {
                        cmd.write(last);
                        ++datalen;
                    }
                } else {
                    // Empty password needs a single null byte
                    datalen = 1;
                    cmd.write(0);
                }

                byte[] cmdbytes = cmd.toByteArray();
                try {
                    int slot = itsUser.getSlotNum();
                    if (slot == 1) {
                        cmdbytes[2] = SLOT_CHAL_HMAC1;
                    } else {
                        cmdbytes[2] = SLOT_CHAL_HMAC2;
                    }
                    cmdbytes[HASH_CMD.length] = datalen;
                    resp = isotag.transceive(cmdbytes);
                    checkResponse(resp);
                } finally {
                    Util.clearArray(cmdbytes);
                }

                // Prune response bytes and convert

                char[] pwstr = Util.bytesToHexChars(resp, 0, resp.length - 2);
                try (Owner<PwsPassword> newPassword =
                             PwsPassword.create(pwstr)) {
                    stopUser(newPassword.pass(), null);
                } finally {
                    Util.clearArray(resp);
                }
            } finally {
                if (resp != null) {
                    Util.clearArray(resp);
                }
                Utils.closeStreams(cmd, isotag);
                Runtime.getRuntime().gc();
            }
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, e, "handleKeyIntent");
            stopUser(null, e);
        }

    }

    /// Check for a valid response
    private static void checkResponse(byte[] resp) throws Exception
    {
        if ((resp.length >= 2) &&
            (resp[resp.length - 2] == (byte)0x90) &&
            (resp[resp.length - 1] == 0x00)) {
            return;
        }

        throw new Exception("Invalid response: " +
                            Util.bytesToHex(resp));
    }

    /**
     * Stop interaction with the user
     */
    private void stopUser(Owner<PwsPassword>.Param password, Exception e)
    {
        if (itsTimer != null) {
            itsTimer.cancel();
        }
        if (itsUser != null) {
            itsUser.finish(password, e);
        }
    }
}
