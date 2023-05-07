package net.tjado.webauthn.util;

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.models.ICredentialSafe;

public class WebAuthnCryptography {
    private final ICredentialSafe credentialSafe;

    private static final String TAG = "WebAuthnCrypto";

    public WebAuthnCryptography(ICredentialSafe safe) {
        this.credentialSafe = safe;
    }

    /**
     * Generate a signature object to be unlocked via biometric prompt
     * This signature object should be passed down to performSignature
     *
     * @return Signature that is generated
     */
    public static Signature generateSignatureObject(PrivateKey privateKey) throws VirgilException {
        Signature sig;
        try {
            sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(privateKey);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new VirgilException("couldn't perform signature: " + e.toString());
        }
        return sig;
    }

    /**
     * Perform a signature over an arbitrary byte array.
     *
     * @param sig        Signature object with which to perform the signature, or null to create it
     *                   on the fly.
     * @param privateKey The private key with which to sign.
     * @param data       The data to be signed.
     * @return A byte array representing the signature in ASN.1 DER Ecdsa-Sig-Value format.
     * @throws VirgilException
     */
    public byte[] performSignature(PrivateKey privateKey, byte[] data, Signature sig) throws VirgilException {
        try {
            if (sig == null) {
                sig = Signature.getInstance("SHA256withECDSA");
                sig.initSign(privateKey);
            }
            sig.update(data);
            return sig.sign();
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
            throw new VirgilException("couldn't perform signature: " + e.toString());
        }
    }

    /**
     * Verify a signature.
     *
     * @param publicKey The key with which to verify the signature.
     * @param data      The data that was signed.
     * @param signature The signature in ASN.1 DER Ecdsa-Sig-Value format.
     * @return true iff the signature is valid
     * @throws VirgilException
     */
    public boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signature) throws VirgilException {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new VirgilException("couldn't perform signature validation", e);
        }
    }

    /**
     * Generate a shared secret using private and public keys as SHA-256((abG).x)
     *
     * @param privateKey    Local private key
     * @param publicKey     Remote public key
     * @return  Byte array containing hashed shared secret
     * @throws VirgilException
     */
    public static byte[] generateSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws VirgilException {
        try {
            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(privateKey);
            agreement.doPhase(publicKey, true);

            return sha256(agreement.generateSecret());

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, "Error: " + e.getMessage());
            throw new VirgilException("couldn't generate shared secret", e);
        }
    }

    /**
     * Encode a message using HmaxSHA256 algorithm
     *
     * @param key byte array contining key
     * @param enc byte array containing message information
     * @return true iff auth == Hmac256(key, enc)
     * @throws VirgilException
     */
    public static byte[] encodeHmacSHA256(byte[] key, byte[] enc) throws VirgilException {
        SecretKey keySpec = new SecretKeySpec(key, "HmacSHA256");
        return encodeHmacSHA256(keySpec, enc);
    }

    /**
     * Encode a message using HmaxSHA256 algorithm
     *
     * @param key byte array contining key
     * @param enc byte array containing message information
     * @return true iff auth == Hmac256(key, enc)
     * @throws VirgilException
     */
    public static byte[] encodeHmacSHA256(SecretKey key, byte[] enc) throws VirgilException {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            hmacSHA256.init(key);

            return hmacSHA256.doFinal(enc);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new VirgilException("couldn't perform HMAC SHA256 encoding", e);
        }

    }

    /**
     *
     * @param key
     * @param data
     * @return
     * @throws VirgilException
     */
    public static byte[] encryptAES256_CBC(byte[] key, byte[] data) throws VirgilException {
        try{
            Cipher aes256 = Cipher.getInstance("AES_256/CBC/NoPadding");
            aes256.init(Cipher.ENCRYPT_MODE,
                        new SecretKeySpec(key, "AES"),
                        new IvParameterSpec(new byte[16]));

            return aes256.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | BadPaddingException |
                IllegalBlockSizeException e) {
            throw new VirgilException("could't perform AES256 CBC mode encryption");
        }
    }

    /**
     *
     * @param key
     * @param data
     * @return
     * @throws VirgilException
     */
    public static byte[] decryptAES256_CBC(byte[] key, byte[] data) throws VirgilException {
        try {
            Cipher aes256 = Cipher.getInstance("AES_256/CBC/NoPadding");
            aes256.init(Cipher.DECRYPT_MODE,
                        new SecretKeySpec(key, "AES"),
                        new IvParameterSpec(new byte[16]));

            return aes256.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | BadPaddingException |
                 IllegalBlockSizeException e) {
            throw new VirgilException("could't perform AES256 CBC mode decryption");
        }
    }

    /**
     * Hash a string with SHA-256.
     *
     * @param data The string to be hashed.
     * @return A byte array containing the hash.
     * @throws VirgilException
     */
    public static byte[] sha256(String data) throws VirgilException {
        return sha256(data.getBytes());
    }

    /**
     * Hash a string with SHA-256.
     *
     * @param data The byte array to be hashed.
     * @return A byte array containing the hash.
     * @throws VirgilException
     */
    public static byte[] sha256(byte[] data) throws VirgilException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new VirgilException("couldn't hash data", e);
        }
        md.update(data);
        return md.digest();
    }

    public static byte[] serializePublicKey(ECPublicKey key) {
        byte[] xRaw = key.getW().getAffineX().toByteArray();
        byte[] yRaw = key.getW().getAffineY().toByteArray();

        Log.d(TAG, "Serialized key lengths " + xRaw.length + " " + yRaw.length);

        ByteBuffer serialized = ByteBuffer.allocate(65);
        serialized.position(65 - yRaw.length);
        serialized.put(yRaw);
        serialized.position(65 - 32 - xRaw.length);
        serialized.put(xRaw);
        serialized.position(0);
        serialized.put((byte)0x04);

        return  serialized.array();
    }
}
