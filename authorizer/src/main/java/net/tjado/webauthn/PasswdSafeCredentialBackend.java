package net.tjado.webauthn;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import kotlin.NotImplementedError;

import net.tjado.authorizer.Utilities;
import net.tjado.passwdsafe.AbstractPasswdSafeLocationFragment;
import net.tjado.passwdsafe.PasswdSafe;
import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.view.EditRecordResult;
import net.tjado.passwdsafe.view.PasswdLocation;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.models.ICredentialSafe;
import net.tjado.webauthn.models.PublicKeyCredentialSource;

import org.pwsafe.lib.file.PwsRecord;


/**
 * CredentialBackend uses the PasswdSafe store.
 */
@SuppressLint("NewApi")
public class PasswdSafeCredentialBackend implements ICredentialSafe {
    private static final String SEC_PROVDER_LCOAL = "AndroidOpenSSL";
    private static final String CURVE_NAME = "secp256r1";
    private boolean strongboxRequired;

    public boolean biometricSigningSupported;

    // PasswdSafe Group to store all FIDO credentials
    private String PWSAFE_GROUP = "_FIDO";

    private final static String TAG = "CredentialSafe";

    private PasswdSafe activity = null;

    /**
     * Construct a CredentialSafe that requires user authentication and strongbox backing.
     *
     * @param ctx The application context
     * @throws VirgilException
     */
    public PasswdSafeCredentialBackend(Context ctx) throws VirgilException {
        this(ctx, true, null);
    }

    /**
     * Construct a CredentialSafe with configurable user authentication / strongbox choices.
     *
     * @param ctx                    The application context
     * @param strongboxRequired      Require keys to be backed by the "Strongbox Keymaster" HSM.
     *                               Requires hardware support.
     * @throws VirgilException
     */
    public PasswdSafeCredentialBackend(Context ctx, boolean strongboxRequired, PasswdSafe activity) throws VirgilException {

        this.activity = activity;
        this.biometricSigningSupported = false;

        if(strongboxRequired) {
            throw new NotImplementedError();
        }
    }

    /**
     * Generate a new ES256 keypair (COSE algorithm -7, ECDSA + SHA-256 over the NIST P-256 curve).
     * WARNING: this generates the key locally and not in Android Keystore.
     *
     * @return The KeyPair object representing the newly generated keypair.
     * @throws VirgilException
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public static KeyPair generateNewES256KeyPairLocal() throws VirgilException {
        Log.d(TAG, "generateNewES256KeyPairLocal");

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, SEC_PROVDER_LCOAL);
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new VirgilException("couldn't generate key pair: " + e.toString());
        }
    }

    /**
     * Generate and save new credential with an ES256 keypair.
     *
     * @param rpEntityId      The relying party's identifier
     * @param rpDisplayName         A human-readable display name for the user
     * @param u2fRpId               Native U2F domain
     * @return A PublicKeyCredentialSource object corresponding to the new keypair and its associated
     * rpId, credentialId, etc.
     * @throws VirgilException
     */
    public PublicKeyCredentialSource generateCredential(@NonNull String rpEntityId, String rpDisplayName,
                                                        @NonNull String u2fRpId) throws VirgilException {
        return generateCredential(rpEntityId, rpDisplayName, new byte[4], null, null,false, u2fRpId);
    }

    /**
     * Generate and save new credential with an ES256 keypair.
     *
     * @param rpEntityId            The relying party's identifier
     * @param rpDisplayName         A human-readable display name for the user
     * @param userHandle            A unique ID for the user
     * @param userDisplayName       A human-readable username for the user
     * @param generateHmacSecret    Wether an HMAC symmetric key should be genereated
     * @return A PublicKeyCredentialSource object corresponding to the new keypair and its associated
     * rpId, credentialId, etc.
     * @throws VirgilException
     */
    public PublicKeyCredentialSource generateCredential(@NonNull String rpEntityId, String rpDisplayName,
                                                        byte[] userHandle, String userName,
                                                        String userDisplayName, boolean generateHmacSecret) throws VirgilException {
        return generateCredential(rpEntityId, rpDisplayName, userHandle, userName, userDisplayName, generateHmacSecret, null);
    }

    /**
     * Generate and save new credential with an ES256 keypair.
     *
     * @param rpEntityId            The relying party's identifier
     * @param rpDisplayName         A human-readable display name for the user
     * @param userHandle            A unique ID for the user
     * @param userDisplayName       A human-readable username for the user
     * @param generateHmacSecret    Wether an HMAC symmetric key should be genereated
     * @param u2fRpId               Native U2F domain
     * @return A PublicKeyCredentialSource object corresponding to the new keypair and its associated
     * rpId, credentialId, etc.
     * @throws VirgilException
     */
    public PublicKeyCredentialSource generateCredential(@NonNull String rpEntityId, String rpDisplayName,
                                                        byte[] userHandle, String userName,
                                                        String userDisplayName, boolean generateHmacSecret,
                                                        String u2fRpId) throws VirgilException {
        Log.w(TAG, "generateCredential");
        if(!activity.isFileWritable()) {
            Log.w(TAG, "PasswdSafe File is not writeable... exit");
            return null;
        }

        KeyPair keyPair = generateNewES256KeyPairLocal();

        SecretKey symmetricKey = generateSymmetricKey();
        String publicKey = keyToString(keyPair.getPublic());
        String privateKey = keyToString(keyPair.getPrivate());
        String key = String.format("%s:%s", publicKey, privateKey);

        final byte[][] id = {null};
        EditRecordResult rc = useRecordFile((info, fileData) -> {
            PwsRecord record = fileData.createRecord();
            record.setLoaded();

            id[0] = fileData.getUUID(record).getBytes(StandardCharsets.UTF_8);

            fileData.setTitle(String.format("%s - %s", rpEntityId, userName), record);
            fileData.setGroup(PWSAFE_GROUP, record);
            fileData.setUsername(userName, record);

            fileData.setFidoRpId(rpEntityId, record);
            fileData.setFidoRpName(rpDisplayName, record);
            fileData.setFidoUserHandle(Utilities.bytesToHex(userHandle), record);
            fileData.setFidoUserName(userName, record);
            fileData.setFidoUserDisplayName(userDisplayName, record);
            fileData.setFidoU2fRpId(u2fRpId, record);
            fileData.setFidoKeyPair(key, record);
            fileData.setFidoKeyUseCounter(0, record);

            fileData.setFidoHmacSecret(keyToString(symmetricKey), record);

            fileData.addRecord(record);

            return new EditRecordResult(true, true, new PasswdLocation(record, fileData));
        });

        if (rc != null) {
            activity.finishEditRecord(rc);
        }

        PublicKeyCredentialSource credentialSource;
        credentialSource = new PublicKeyCredentialSource(id[0], rpEntityId, rpDisplayName,
                userHandle, userName, userDisplayName,
                generateHmacSecret, u2fRpId, keyPair, null);

        if (generateHmacSecret && (symmetricKey == null)) {
            deleteCredential(credentialSource);
            return null;
        }

        return credentialSource;
    }

    /**
     * Use the file data with an optional record at the current location
     */
    protected final <RetT> RetT useRecordFile(final AbstractPasswdSafeLocationFragment.RecordFileUser<RetT> user)
    {
        return activity.useFileData(fileData -> user.useFile(null, fileData));
    }

    public String keyToString(Key key) {
        return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
    }

    public PublicKey base64ToPublicKey(String base64Key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] bytesKey = Base64.decode(base64Key, Base64.DEFAULT);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytesKey);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(keySpec);
    }

    public PrivateKey base64ToPrivateKey(String base64Key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] bytesKey = Base64.decode(base64Key, Base64.DEFAULT);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytesKey);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(keySpec);
    }

    public SecretKey base64ToSecretKey(String base64Key) {
        byte[] bytesKey = Base64.decode(base64Key, Base64.DEFAULT);

        return new SecretKeySpec(bytesKey, "HmacSHA256");
    }

    public PublicKeyCredentialSource recordToCredential(PasswdFileData fileData, PwsRecord rec) {
        if(rec == null) {
            return null;
        }

        String fileKeyPair = fileData.getFidoKeyPair(rec);
        if(fileKeyPair == null) {
            return null;
        }

        String[] keys = fileKeyPair.split(":");

        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        try {
            publicKey = base64ToPublicKey(keys[0]);
            privateKey = base64ToPrivateKey(keys[1]);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        return new PublicKeyCredentialSource(
                fileData.getUUID(rec).getBytes(StandardCharsets.UTF_8),
                fileData.getFidoRpId(rec),
                fileData.getFidoRpName(rec),
                Utilities.hexStringToByteArray(fileData.getFidoUserHandle(rec)),
                fileData.getFidoUserName(rec),
                fileData.getFidoUserDisplayName(rec),
                false,
                fileData.getFidoU2fRpId(rec),
                keyPair,
                base64ToSecretKey(fileData.getFidoHmacSecret(rec))
        );
    }

    public SecretKey generateSymmetricKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            return keyGen.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate symmetric key:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes a single credential
     *
     * @param credentialSource  Credential to be deleted
     */
    public void deleteCredential(PublicKeyCredentialSource credentialSource) {
        throw new NotImplementedError();
    }

    /**
     * Wipes out all credentials in credentialList
     *
     * @param credentialList Credentials to be deleted
     */
    public void deleteAllCredentials(List<PublicKeyCredentialSource> credentialList) {
        throw new NotImplementedError();
    }

    /**
     * Wipes out ALL credentials in the storage
     */
    public void deleteAllCredentials() {
        throw new NotImplementedError();
    }

    /**
     *
     * @return All credentials registered
     */
    public List<PublicKeyCredentialSource> getAllCredentials() {
        Log.d(TAG, "getAllCredentials");

        List<PublicKeyCredentialSource> credentialSources = new ArrayList<>();
        activity.useFileData(fileData -> {
            for (PwsRecord rec: fileData.getRecords()) {
                String rpId = fileData.getFidoRpId(rec);
                if (rpId != null) {
                    credentialSources.add(recordToCredential(fileData, rec));
                }
            }

            return null;
        });

        return credentialSources;
    }

    /**
     * Get keys belonging to this RP ID.
     *
     * @param rpEntityId rpEntity.id from WebAuthn spec.
     * @return The set of associated PublicKeyCredentialSources.
     */
    public List<PublicKeyCredentialSource> getKeysForEntity(@NonNull String rpEntityId) {
        Log.d(TAG, "getKeysForEntity: " + rpEntityId);

        List<PublicKeyCredentialSource> credentialSources = new ArrayList<>();
        activity.useFileData(fileData -> {
            for (PwsRecord rec: fileData.getRecords()) {
                String rpId = fileData.getFidoRpId(rec);
                if (rpId != null && rpId.equals(rpEntityId)) {
                    credentialSources.add(recordToCredential(fileData, rec));
                }
            }
            return null;
        });

        return credentialSources;
    }

    /**
     * Get the credential matching the specified id, if it exists
     *
     * @param id byte[] credential id
     * @return PublicKeyCredentialSource that matches the id, or null
     */
    public PublicKeyCredentialSource getCredentialSourceById(@NonNull byte[] id) {

        final PublicKeyCredentialSource[] credentialSource = {null};
        activity.useFileData(fileData -> {
            PwsRecord rec = fileData.getRecord(new String(id, StandardCharsets.UTF_8));
            credentialSource[0] = recordToCredential(fileData, rec);

            return null;
        });

        return credentialSource[0];
    }

    /**
     * Checks whether all certificates are stored in hardware
     * @return true iff all credentials are in hardware
     *         false if at least one of the credentials is not in hardware
     *         null if there are no credentials or the operation failed
     */
    public Boolean credentialsInHardware() {
        return false;
    }

    /**
     * Fix the length of a byte array such that:
     * 1) If the desired length is less than the length of `arr`, the left-most source bytes are
     * truncated.
     * 2) If the desired length is more than the length of `arr`, the left-most destination bytes
     * are set to 0x00.
     *
     * @param arr         The source byte array.
     * @param fixedLength The desired length of the resulting array.
     * @return A new array of length fixedLength.
     */
    private static byte[] toUnsignedFixedLength(byte[] arr, int fixedLength) {
        byte[] fixed = new byte[fixedLength];
        int offset = fixedLength - arr.length;
        int srcPos = Math.max(-offset, 0);
        int dstPos = Math.max(offset, 0);
        int copyLength = Math.min(arr.length, fixedLength);
        System.arraycopy(arr, srcPos, fixed, dstPos, copyLength);
        return fixed;
    }

    /**
     * Encode an EC public key in the COSE/CBOR format.
     *
     * @param publicKey The public key.
     * @return A COSE_Key-encoded public key as byte array.
     * @throws VirgilException
     */
    public static byte[] coseEncodePublicKey(PublicKey publicKey) throws VirgilException {
        Pair<byte[], byte[]> point = cosePointEncode(publicKey);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(1, 2)              // kty: EC2 key type
                    .put(3, -7)             // alg: ES256 sig algorithm
                    .put(-1, 1)             // crv: P-256 curve
                    .put(-2, point.first)   // x-coord
                    .put(-3, point.second)  // y-coord
                    .end()
                    .build()
            );
        } catch (CborException e) {
            throw new VirgilException("couldn't serialize to cbor", e);
        }
        return baos.toByteArray();
    }

    /**
     * Perform first part of cose encoding and validation
     *
     * @param publicKey The public key.
     * @return byte array "byte[]" pair where x is first and y is second
     */
   public static Pair<byte[], byte[]> cosePointEncode(PublicKey publicKey) {
       ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
       ECPoint point = ecPublicKey.getW();
       // ECPoint coordinates are *unsigned* values that span the range [0, 2**32). The getAffine
       // methods return BigInteger objects, which are signed. toByteArray will output a byte array
       // containing the two's complement representation of the value, outputting only as many
       // bytes as necessary to do so. We want an unsigned byte array of length 32, but when we
       // call toByteArray, we could get:
       // 1) A 33-byte array, if the point's unsigned representation has a high 1 bit.
       //    toByteArray will prepend a zero byte to keep the value positive.
       // 2) A <32-byte array, if the point's unsigned representation has 9 or more high zero
       //    bits.
       // Due to this, we need to either chop off the high zero byte or prepend zero bytes
       // until we have a 32-length byte array.
       byte[] xVariableLength = point.getAffineX().toByteArray();
       byte[] yVariableLength = point.getAffineY().toByteArray();

       byte[] x = toUnsignedFixedLength(xVariableLength, 32);
       assert x.length == 32;
       byte[] y = toUnsignedFixedLength(yVariableLength, 32);
       assert y.length == 32;

       return new Pair<>(x, y);
   }


    /**
     * Increment the credential use counter for this credential.
     *
     * @param credential The credential whose counter we want to increase.
     * @return The value of the counter before incrementing.
     */
    public int incrementCredentialUseCounter(PublicKeyCredentialSource credential) {

        final int[] currentCounter = {0};
        EditRecordResult rc = useRecordFile((info, fileData) -> {
            PwsRecord rec = fileData.getRecord(new String(credential.id, StandardCharsets.UTF_8));

            Integer cnt = fileData.getFidoKeyUseCounter(rec);
            if(cnt == null) {
                cnt = 0;
            }
            fileData.setFidoKeyUseCounter(cnt + 1, rec);

            currentCounter[0] = cnt;
            return new EditRecordResult(false, rec.isModified(), new PasswdLocation(rec, fileData));
        });

        if (rc != null) {
            activity.finishEditRecord(rc);
        }

        return currentCounter[0];
    }

    public KeyPair keyAgreementPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(CURVE_NAME);
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC");
        keyPairGen.initialize(ecGenParameterSpec);
        return keyPairGen.genKeyPair();
    }
}
