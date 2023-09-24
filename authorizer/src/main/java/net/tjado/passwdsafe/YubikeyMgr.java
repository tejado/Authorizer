/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.app.Activity;
import android.os.CountDownTimer;
import android.util.Log;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.ClearingByteArrayOutputStream;
import net.tjado.passwdsafe.view.CloseableLiveData;
import com.yubico.yubikit.core.YubiKeyDevice;
import com.yubico.yubikit.yubiotp.Slot;
import com.yubico.yubikit.yubiotp.YubiOtpSession;

import org.pwsafe.lib.Util;
import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;


/**
 * The YubikeyMgr class encapsulates the interaction with a YubiKey
 */
public class YubikeyMgr
{
    private static final int SHA1_MAX_BLOCK_SIZE = 64;

    private static final String TAG = "YubikeyMgr";

    private final YubikeyViewModel itsYubikeyModel;
    private User itsUser = null;
    private CountDownTimer itsTimer = null;
    private final CloseableLiveData<KeyResult> itsResult =
            new CloseableLiveData<>();

    /// Interface for a user of the YubikeyMgr
    public interface User
    {
        /// Get the activity using the key
        @NonNull
        Activity getActivity();

        /// Get the password to be sent to the key
        @CheckResult
        @Nullable
        Owner<PwsPassword> getUserPassword();

        /// Get the slot number to use on the key
        int getSlotNum();

        /// Finish interaction with the key
        void finish(Owner<PwsPassword>.Param password, Exception e);

        /// Handle an update on the timer until the start times out
        void timerTick(@SuppressWarnings("SameParameterValue") int totalTime,
                       int remainingTime);
    }

    /**
     * Constructor
     */
    public YubikeyMgr(@NonNull YubikeyViewModel yubikeyModel,
                      @NonNull Fragment openFrag)
    {
        itsYubikeyModel = yubikeyModel;
        var fragLifecycleOwner = openFrag.getViewLifecycleOwner();
        itsResult.observe(fragLifecycleOwner, this::onYubikeyResultChanged);
        itsYubikeyModel.getDeviceData().observe(fragLifecycleOwner,
                this::onYubikeyDeviceChanged);
    }

    /**
     * Start the interaction with the YubiKey
     */
    public void start(@NonNull User user)
    {
        if (itsUser != null) {
            stop();
        }
        itsUser = user;

        if (YubikeyViewModel.TEST) {
            testYubikey();
        } else {
            startYubikey();
        }

        itsTimer = new CountDownTimer(YubikeyViewModel.KEY_TIMEOUT, 1 * 1000)
        {
            @Override
            public void onFinish()
            {
                stop();
            }

            @Override
            public void onTick(long millisUntilFinished)
            {
                itsUser.timerTick(YubikeyViewModel.KEY_TIMEOUT / 1000,
                        (int)(millisUntilFinished / 1000));
            }
        };
        itsTimer.start();
    }

    /**
     * Stop the interaction with the key
     */
    public void stop()
    {
        onPause();
        stopUser(null, null);
        itsResult.close();
        itsTimer = null;
        itsUser = null;
    }

    /**
     * Handle a pause of the using fragment
     */
    public void onPause()
    {
        if (itsUser != null) {
            itsYubikeyModel.stopNfc(itsUser.getActivity());
        }
    }

    /**
     * Use a discovered YubiKey
     */
    @UiThread
    private void useYubikey(YubiKeyDevice device)
    {
        PasswdSafeUtil.dbginfo(TAG, "Use YubiKey %s, has user: %b", device,
                (itsUser != null));
        if (itsUser == null) {
            return;
        }

        try (var userPassword = itsUser.getUserPassword()) {
            doUseYubikey(device,
                    (userPassword != null) ? userPassword.pass() : null,
                    itsUser.getSlotNum(), itsResult);
        }
    }

    /**
     * Implementation to use a discovered YubiKey
     */
    @UiThread
    private static void doUseYubikey(final YubiKeyDevice device,
                                     @Nullable
                                     final Owner<PwsPassword>.Param password,
                                     final int slotNum,
                                     final CloseableLiveData<KeyResult> result)
    {
        YubiOtpSession.create(device, sessionResult -> {
            try (var userPassword = (password != null) ? password.use() : null;
                 var pwbytes = new ClearingByteArrayOutputStream()) {
                YubiOtpSession otp = sessionResult.getValue();

                if (userPassword == null) {
                    throw new Exception("No password");
                }
                PwsPassword pw = userPassword.get();

                int pwlen = pw.length();
                if (pwlen > 0) {
                    if (pwlen > SHA1_MAX_BLOCK_SIZE / 2) {
                        pwlen = SHA1_MAX_BLOCK_SIZE / 2;
                    }
                    // Chars are encoded as little-endian UTF-16.  A trailing
                    // zero must be skipped as the PC API will skip it.
                    for (int i = 0; i < pwlen - 1; ++i) {
                        char c = pw.charAt(i);
                        pwbytes.write(c & 0xff);
                        pwbytes.write((c >> 8) & 0xff);
                    }

                    char c = pw.charAt(pwlen - 1);
                    pwbytes.write(c & 0xff);
                    int last = (c >> 8) & 0xff;
                    if (last != 0) {
                        pwbytes.write(last);
                    }
                } else {
                    // Empty password needs a single null byte
                    pwbytes.write(0);
                }

                byte[] resp = otp.calculateHmacSha1(
                        slotNum == 1 ? Slot.ONE : Slot.TWO,
                        pwbytes.toByteArray(), null);
                try {
                    // Prune response bytes and convert
                    char[] pwstr = Util.bytesToHexChars(resp, 0, resp.length);
                    try (Owner<PwsPassword> newPassword = PwsPassword.create(
                            pwstr)) {
                        result.postValue(
                                new KeyResult(newPassword.pass(), null));
                    }
                } finally {
                    Util.clearArray(resp);
                }
            } catch (Exception e) {
                PasswdSafeUtil.dbginfo(TAG, e, "Error creating OTP session");
                result.postValue(new KeyResult(null, e));
            }
        });
    }

    /**
     * Start using the YubiKey
     */
    @UiThread
    private void startYubikey()
    {
        var yubikeyDevice = itsYubikeyModel.getDeviceData().getValue();
        if (yubikeyDevice != null) {
            useYubikey(yubikeyDevice);
        } else {
            itsYubikeyModel.startNfc(itsUser.getActivity());
        }
    }

    /**
     * Test using the YubiKey
     */
    @UiThread
    private void testYubikey()
    {
        new CountDownTimer(5000, 5000)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
            }

            @Override
            public void onFinish()
            {
                if (itsUser == null) {
                    return;
                }
                try (var password = itsUser.getUserPassword()) {
                    if (password == null) {
                        itsResult.postValue(new KeyResult(null, null));
                        return;
                    }
                    String utf8 = "UTF-8";
                    byte[] bytes = password.get().getBytes(utf8);
                    String passwordStr = new String(bytes, utf8).toLowerCase();
                    try (Owner<PwsPassword> newPassword = PwsPassword.create(
                            passwordStr)) {
                        itsResult.postValue(
                                new KeyResult(newPassword.pass(), null));
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "encode error", e);
                    itsResult.postValue(new KeyResult(null, e));
                }
            }
        }.start();
    }

    /**
     * Handle a change notification for the YubiKey device
     */
    private void onYubikeyDeviceChanged(YubiKeyDevice device)
    {
        PasswdSafeUtil.dbginfo(TAG, "YubiDevice changed: %s",
                YubikeyViewModel.toString(device));
        if ((itsUser != null) && (device != null)) {
            useYubikey(device);
        }
    }

    /**
     * Handle a change notification for the result of using the YubiKey
     */
    private void onYubikeyResultChanged(KeyResult result)
    {
        try (result) {
            if (result != null) {
                stopUser(
                        ((result.itsPassword != null) ?
                                result.itsPassword.pass() : null), result.itsError);
            }
        }
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

    /**
     * Result of using the YubiKey to calculate the password
     */
    private static class KeyResult implements Closeable
    {
        protected final Owner<PwsPassword> itsPassword;
        protected final Exception itsError;

        /**
         * Constructor
         */
        protected KeyResult(Owner<PwsPassword>.Param password, Exception error)
        {
            itsPassword = (password != null) ? password.use() : null;
            itsError = error;
        }

        @Override
        public void close()
        {
            if (itsPassword != null) {
                itsPassword.close();
            }
        }
    }
}