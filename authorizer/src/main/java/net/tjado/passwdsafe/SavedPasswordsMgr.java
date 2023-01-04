/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.tjado.passwdsafe.db.PasswdSafeDb;
import net.tjado.passwdsafe.db.SavedPassword;
import net.tjado.passwdsafe.db.SavedPasswordsDao;
import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;

import org.pwsafe.lib.Util;
import org.pwsafe.lib.file.Owner;
import org.pwsafe.lib.file.PwsPassword;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

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

    private final Context itsContext;
    private final SavedPasswordsDao itsDao;
    private @Nullable BiometricPrompt itsBioPrompt;
    private boolean itsHasBioHw;
    private boolean itsHasEnrolledBio;
    private User itsActiveUser;
    private boolean itsHasAuthenticated = false;

    private static final MessageDigest MD_SHA256;
    static {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        MD_SHA256 = md;
    }


    /**
     * User of the saved password manager
     */
    public static abstract class User
            extends BiometricPrompt.AuthenticationCallback
    {
        /**
         * Is the user for encryption or decryption
         */
        protected abstract boolean isEncrypt();
    }

    /**
     * Constructor
     */
    public SavedPasswordsMgr(Context ctx)
    {
        itsContext = ctx.getApplicationContext();
        itsDao = PasswdSafeDb.get(itsContext).accessSavedPasswords();
        itsDao.processDbUpgrade(itsContext);
        itsHasBioHw = false;
        itsHasEnrolledBio = false;
        itsBioPrompt = null;
    }

    /**
     * Attach to its owning fragment
     */
    public void attach(Fragment frag)
    {
        BiometricManager bioMgr = BiometricManager.from(itsContext);
        switch (bioMgr.canAuthenticate()) {
        case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
        case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
        case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
        case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
        case BiometricManager.BIOMETRIC_STATUS_UNKNOWN: {
            break;
        }
        case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED: {
            itsHasBioHw = true;
            break;
        }
        case BiometricManager.BIOMETRIC_SUCCESS: {
            itsHasBioHw = true;
            itsHasEnrolledBio = true;
            break;
        }
        }

        itsBioPrompt = new BiometricPrompt(
                frag, ContextCompat.getMainExecutor(itsContext),
                new BioAuthenticationCallback());
    }

    /**
     * Detach from its owning fragment
     */
    public void detach()
    {
        itsActiveUser = null;
        if ((itsBioPrompt != null) && itsHasAuthenticated) {
            itsBioPrompt.cancelAuthentication();
        }
    }

    /**
     * Are saved passwords available
     */
    public boolean isAvailable()
    {
        return itsHasBioHw;
    }

    /**
     * Is there a saved password for a file
     */
    public synchronized boolean isSaved(PasswdFileUri fileUri)
    {
        try {
            return getSavedPassword(fileUri) != null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking saved for " + fileUri, e);
            return false;
        }
    }

    /**
     * Generate a saved password key for a file
     */
    @TargetApi(Build.VERSION_CODES.M)
    public synchronized void generateKey(PasswdFileUri fileUri)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
                   NoSuchProviderException, IOException
    {
        String keyName = getUriAlias2(fileUri.getUri());
        PasswdSafeUtil.dbginfo(TAG, "generateKey: %s, key: %s",
                               fileUri, keyName);

        if (!itsHasEnrolledBio) {
            throw new IOException(
                    itsContext.getString(R.string.no_biometrics_registered));
        }

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
    public boolean startPasswordAccess(PasswdFileUri fileUri, User user)
    {
        try {
            if (itsBioPrompt == null) {
                throw new IOException("Not attached");
            }

            boolean isEncrypt = user.isEncrypt();
            Cipher cipher = getKeyCipher(fileUri, isEncrypt);

            int descId = isEncrypt ?
                    R.string.touch_sensor_to_save_the_password :
                    R.string.touch_sensor_to_load_saved_password;

            BiometricPrompt.PromptInfo prompt =
                    new BiometricPrompt.PromptInfo.Builder()
                            .setTitle(itsContext.getString(R.string.app_name))
                            .setSubtitle(fileUri.getIdentifier(itsContext,
                                                               true))
                            .setDescription(itsContext.getString(descId))
                            .setNegativeButtonText(
                                    itsContext.getString(R.string.cancel))
                            .setConfirmationRequired(false)
                            .build();
            itsActiveUser = user;
            itsHasAuthenticated = true;
            itsBioPrompt.authenticate(prompt,
                                      new BiometricPrompt.CryptoObject(cipher));
            return true;
        } catch (CertificateException | NoSuchAlgorithmException |
                KeyStoreException | UnrecoverableKeyException |
                NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IOException e) {
            if (e.getClass().getName().equals(
                    "android.security.keystore." +
                    "KeyPermanentlyInvalidatedException")) {
                removeSavedPassword(fileUri);
            }

            String msg = itsContext.getString(
                    R.string.key_error, fileUri.getIdentifier(itsContext, true),
                    e.getLocalizedMessage());
            Log.e(TAG, msg, e);
            user.onAuthenticationError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                                       msg);
            return false;
        }
    }

    /**
     * Load a saved password for a file
     */
    public @CheckResult
    Owner<PwsPassword> loadSavedPassword(PasswdFileUri fileUri, Cipher cipher)
            throws IOException, BadPaddingException, IllegalBlockSizeException
    {
        SavedPassword saved = null;
        Exception exc = null;
        try {
            saved = getSavedPassword(fileUri);
        } catch (Exception e) {
            exc = e;
        }

        if ((saved == null) || TextUtils.isEmpty(saved.encPasswd)) {
            throw new IOException(
                    itsContext.getString(R.string.password_not_found, fileUri),
                    exc);
        }

        byte[] enc = Base64.decode(saved.encPasswd, Base64.NO_WRAP);
        byte[] decPassword = cipher.doFinal(enc);
        try {
            return PwsPassword.create(decPassword, "UTF-8");
        } finally {
            Util.clearArray(decPassword);
            Util.clearArray(enc);
        }
    }

    /**
     * Add a saved password for a file
     */
    public void addSavedPassword(PasswdFileUri fileUri,
                                 Owner<PwsPassword>.Param passwordParam,
                                 Cipher cipher)
            throws Exception
    {
        try (Owner<PwsPassword> password = passwordParam.use()) {
            byte[] enc = cipher.doFinal(password.get().getBytes("UTF-8"));
            String encStr = Base64.encodeToString(enc, Base64.NO_WRAP);
            String ivStr = Base64
                    .encodeToString(cipher.getIV(), Base64.NO_WRAP);

            itsDao.add(fileUri, ivStr, encStr, itsContext);
        }
    }

    /**
     * Removed the saved password and key for a file
     */
    public synchronized void removeSavedPassword(PasswdFileUri fileUri)
    {
        Uri uri = fileUri.getUri();
        try {
            SavedPassword saved = getSavedPassword(fileUri);
            if (saved != null) {
                uri = Uri.parse(saved.uri);
            }
            itsDao.remove(uri);
        } catch (Exception e) {
            Log.e(TAG, "Error removing " + fileUri, e);
        }
        if (isAvailable()) {
            PasswdSafeUtil.dbginfo(TAG, "removeSavedPassword: %s", fileUri);
            try {
                KeyStore keyStore = getKeystore();
                for (String keyName : new String[]
                        { getUriAlias2(uri), getUriAlias1(uri) }) {
                    try {
                        keyStore.deleteEntry(keyName);
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                    }
                }
            } catch (KeyStoreException | CertificateException |
                    IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove all saved passwords and keys
     */
    public synchronized void removeAllSavedPasswords()
    {
        try {
            itsDao.removeAll();
        } catch (Exception e) {
            Log.e(TAG, "Error removing passwords", e);
        }
        if (isAvailable()) {
            try {
                KeyStore keyStore = getKeystore();
                Enumeration<String> aliases = keyStore.aliases();
                if (aliases != null) {
                    while (aliases.hasMoreElements()) {
                        String key = aliases.nextElement();
                        PasswdSafeUtil.dbginfo(
                                TAG, "removeAllSavedPasswords key: %s", key);
                        keyStore.deleteEntry(key);
                    }
                }
            } catch (CertificateException | NoSuchAlgorithmException |
                    IOException | KeyStoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get the cipher for the key protecting the saved password for a file
     */
    @TargetApi(Build.VERSION_CODES.M)
    private Cipher getKeyCipher(PasswdFileUri fileUri, boolean encrypt)
            throws CertificateException, NoSuchAlgorithmException,
                   KeyStoreException, IOException, UnrecoverableKeyException,
                   NoSuchPaddingException, InvalidKeyException,
                   InvalidAlgorithmParameterException
    {
        Uri uri = fileUri.getUri();
        SavedPassword saved = null;
        Exception exc = null;
        if (!encrypt) {
            try {
                saved = getSavedPassword(fileUri);
                if (saved != null) {
                    uri = Uri.parse(saved.uri);
                }
            } catch (Exception e) {
                exc = e;
            }
        }

        KeyStore keystore = getKeystore();
        Key key = null;
        for (String keyName : new String[]
                { getUriAlias2(uri), getUriAlias1(uri) }) {
            key = keystore.getKey(keyName, null);
            if (key != null) {
                PasswdSafeUtil.dbginfo(TAG, "getKeyCipher name %s", keyName);
                break;
            }
        }
        if (key == null) {
            throw new IOException(itsContext.getString(R.string.key_not_found,
                                                       uri));
        }

        Cipher ciph = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/" +
                KeyProperties.BLOCK_MODE_CBC + "/" +
                KeyProperties.ENCRYPTION_PADDING_PKCS7);
        if (encrypt) {
            ciph.init(Cipher.ENCRYPT_MODE, key);
        } else {
            if ((saved == null) || TextUtils.isEmpty(saved.iv)) {
                throw new IOException("Key IV not found for " + fileUri, exc);
            }
            byte[] iv = Base64.decode(saved.iv, Base64.NO_WRAP);
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
     * Get the v2 keystore alias for a URI
     */
    private static String getUriAlias2(Uri uri)
            throws UnsupportedEncodingException
    {
        return "key2_" + getUriKey(uri);
    }

    /**
     * Get the v1 keystore alias for a URI
     */
    private static String getUriAlias1(Uri uri)
    {
        return "key_" + uri.toString();
    }

    /**
     * Get the saved password for a file URI
     */
    private SavedPassword getSavedPassword(PasswdFileUri fileUri)
    {
        return itsDao.get(fileUri, itsContext);
    }

    /**
     * Get a unique key for a URI
     */
    private static String getUriKey(Uri uri)
            throws UnsupportedEncodingException
    {
        String uristr = uri.toString();
        @SuppressWarnings("CharsetObjectCanBeUsed")
        byte[] digest = MD_SHA256.digest(uristr.getBytes("UTF-8"));
        return Base64.encodeToString(digest, Base64.NO_WRAP);
    }

    /**
     * Biometric authentication callback
     */
    private class BioAuthenticationCallback
            extends BiometricPrompt.AuthenticationCallback
    {
        @Override
        public void onAuthenticationError(int errorCode,
                                          @NonNull CharSequence errString)
        {
            super.onAuthenticationError(errorCode, errString);
            if (itsActiveUser != null) {
                itsActiveUser.onAuthenticationError(errorCode, errString);
                itsActiveUser = null;
            }
        }

        @Override
        public void onAuthenticationSucceeded(
                @NonNull BiometricPrompt.AuthenticationResult result)
        {
            super.onAuthenticationSucceeded(result);
            if (itsActiveUser != null) {
                itsActiveUser.onAuthenticationSucceeded(result);
                itsActiveUser = null;
            }
        }

        @Override
        public void onAuthenticationFailed()
        {
            super.onAuthenticationFailed();
            if (itsActiveUser != null) {
                itsActiveUser.onAuthenticationFailed();
            }
        }
    }
}
