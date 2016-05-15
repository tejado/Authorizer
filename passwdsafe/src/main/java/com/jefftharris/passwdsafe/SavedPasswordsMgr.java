/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

/**
 * The SavedPasswordsMgr class encapsulates functionality for saving
 * passwords to files
 */
public final class SavedPasswordsMgr
{
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String TAG = "SavedPasswordsMgr";

    private final FingerprintManagerCompat itsFingerprintMgr;
    private final Context itsContext;

    /**
     * User of the saved password manager
     */
    public static abstract class User
            extends FingerprintManagerCompat.AuthenticationCallback
            implements CancellationSignal.OnCancelListener
    {
        private final CancellationSignal itsCancelSignal;

        /**
         * Constructor
         */
        public User()
        {
            itsCancelSignal = new CancellationSignal();
            itsCancelSignal.setOnCancelListener(this);
        }

        /**
         * Cancel use of the manager
         */
        public void cancel()
        {
            itsCancelSignal.cancel();
        }

        /**
         * Is the user for encryption or decryption
         */
        protected abstract boolean isEncrypt();

        /**
         * Callback when the user has started
         */
        protected abstract void onStart();

        /**
         * Get the cancellation signaler
         */
        private CancellationSignal getCancelSignal()
        {
            return itsCancelSignal;
        }
    }

    /**
     * Constructor
     */
    public SavedPasswordsMgr(Context ctx)
    {
        itsFingerprintMgr = FingerprintManagerCompat.from(ctx);
        itsContext = ctx;
    }

    /**
     * Are saved passwords available
     */
    public boolean isAvailable()
    {
        return itsFingerprintMgr.isHardwareDetected();
    }

    /**
     * Is there a saved password for a file
     */
    public synchronized boolean isSaved(Uri fileUri)
    {
        return getPrefs().contains(getPrefsKey(fileUri));
    }

    /**
     * Generate a saved password key for a file
     */
    // TODO: target api testing
    @TargetApi(23)
    public synchronized void generateKey(Uri fileUri)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException
    {
        PasswdSafeUtil.dbginfo(TAG, "generateKey: %s", fileUri);

        String keyName = getPrefsKey(fileUri);
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
            keyGen.init(
                    new KeyGenParameterSpec.Builder(
                            keyName,
                            KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            .setEncryptionPaddings(
                                    KeyProperties.ENCRYPTION_PADDING_PKCS7)
                            .setKeySize(256)
                            .setUserAuthenticationRequired(true)
                            .build());
            keyGen.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException |
                InvalidAlgorithmParameterException e) {
            Log.e(TAG, "generateKey failure", e);
            removeSavedPassword(fileUri);
            throw e;
        }
    }

    /**
     * Start access to the key protecting the saved password for a file
     */
    public void startPasswordAccess(Uri fileUri, User user)
    {
        try {
            Cipher cipher = getKeyCipher(fileUri, user.isEncrypt());
            FingerprintManagerCompat.CryptoObject cryptoObj =
                    new FingerprintManagerCompat.CryptoObject(cipher);
            itsFingerprintMgr.authenticate(cryptoObj, 0, user.getCancelSignal(),
                                           user, null);
            user.onStart();
        } catch (CertificateException | NoSuchAlgorithmException |
                KeyStoreException | UnrecoverableKeyException |
                NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IOException e) {
            String msg = "Error retrieving key cipher " +
                         e.getLocalizedMessage();
            Log.e(TAG, msg, e);
            user.onAuthenticationError(0, msg);
        }
    }

    /**
     * Load a saved password for a file
     */
    public String loadSavedPassword(Uri fileUri, Cipher cipher)
            throws IOException, BadPaddingException, IllegalBlockSizeException
    {
        String keyName = getPrefsKey(fileUri);
        SharedPreferences prefs = getPrefs();
        String encStr = prefs.getString(keyName, null);
        if (TextUtils.isEmpty(encStr)) {
            // TODO i18n
            throw new IOException("Password not found for " + keyName);
        }

        byte[] enc = Base64.decode(encStr, Base64.NO_WRAP);
        byte[] decPassword = cipher.doFinal(enc);
        return new String(decPassword, "UTF-8");
    }

    /**
     * Add a saved password for a file
     */
    public void addSavedPassword(Uri fileUri, String password, Cipher cipher)
            throws UnsupportedEncodingException, BadPaddingException,
            IllegalBlockSizeException
    {
        byte[] enc = cipher.doFinal(password.getBytes("UTF-8"));
        String encStr = Base64.encodeToString(enc, Base64.NO_WRAP);
        String ivStr = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);

        String keyName = getPrefsKey(fileUri);
        SharedPreferences prefs = getPrefs();
        prefs.edit()
             .putString(keyName, encStr)
             .putString("iv_" + keyName, ivStr)
             .apply();
    }

    /**
     * Removed the saved password and key for a file
     */
    public synchronized void removeSavedPassword(Uri fileUri)
    {
        PasswdSafeUtil.dbginfo(TAG, "removeSavedPassword: %s", fileUri);
        String keyName = getPrefsKey(fileUri);
        try {
            KeyStore keyStore = getKeystore();
            keyStore.deleteEntry(keyName);
        } catch (KeyStoreException | CertificateException |
                IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        getPrefs().edit().remove(keyName).remove("iv_" + keyName).apply();
    }

    /**
     * Get the cipher for the key protecting the saved password for a file
     */
    private Cipher getKeyCipher(Uri fileUri, boolean encrypt)
            throws CertificateException, NoSuchAlgorithmException,
                   KeyStoreException, IOException, UnrecoverableKeyException,
                   NoSuchPaddingException, InvalidKeyException,
                   InvalidAlgorithmParameterException
    {
        String keyName = getPrefsKey(fileUri);
        KeyStore keystore = getKeystore();
        Key key = keystore.getKey(keyName, null);
        if (key == null) {
            // TODO i18n
            throw new IOException("Key not found for " + keyName);
        }

        Cipher ciph = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/" +
                KeyProperties.BLOCK_MODE_CBC + "/" +
                KeyProperties.ENCRYPTION_PADDING_PKCS7);
        if (encrypt) {
            ciph.init(Cipher.ENCRYPT_MODE, key);
        } else {
            SharedPreferences prefs = getPrefs();
            String ivStr = prefs.getString("iv_" + keyName, null);
            if (TextUtils.isEmpty(ivStr)) {
                throw new IOException("Key IV not found for " + keyName);
            }
            byte[] iv = Base64.decode(ivStr, Base64.NO_WRAP);
            ciph.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        }
        return ciph;
    }

    /**
     * Get the Android keystore containing the keys protecting saved passwords
     */
    private KeyStore getKeystore()
            throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException
    {
        KeyStore store = KeyStore.getInstance(KEYSTORE);
        store.load(null);
        return store;
    }

    /**
     * Get the preferences for saved passwords
     */
    private SharedPreferences getPrefs()
    {
        return itsContext.getSharedPreferences("saved", Context.MODE_PRIVATE);
    }

    /**
     * Get the preferences key for a file
     */
    private String getPrefsKey(Uri uri)
    {
        return "SavedPasswordsMgr_" + uri.toString();
    }
}
