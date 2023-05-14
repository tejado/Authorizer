package net.tjado.webauthn.models;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.security.KeyPair;
import java.security.SecureRandom;

import javax.crypto.SecretKey;

public class PublicKeyCredentialSource {
    public static final String type = "public-key";

    public byte[] id;

    public String rpId;
    public String rpName;
    public String u2fRpId;

    public byte[] userHandle;
    public String userName;
    public String userDisplayName;

    public KeyPair keyPair;
    public int keyUseCounter;
    public SecretKey hmacSecret;

    public String keyPairAlias;
    public String hmacSecretAlias;

    public String otherUI;
    public boolean generateHmacSecret;

    private static SecureRandom random;
    private static final String KEYPAIR_PREFIX = "webauthn-keypair-";
    private static final String HMAC_SECRET_PREFIX = "webauthn-hmac-secret-keypair-";

    /**
     * Construct a new PublicKeyCredentialSource. This is the canonical object that represents a
     * WebAuthn credential.
     *
     * @param rpId                  The relying party ID.
     * @param rpName                A human-readable name of the Relying Party
     * @param userHandle            The unique ID used by the RP to identify the user.
     * @param userName              A human-readable user name for the user.
     * @param userDisplayName       A human-readable display name for the user.
     * @param generateHmacSecret    Generate hmacSecret
     * @param u2fRpId               Native U2F domain
     */
    public PublicKeyCredentialSource(byte[] id, @NonNull String rpId, String rpName,
                                     byte[] userHandle, String userName, String userDisplayName, boolean generateHmacSecret,
                                     String u2fRpId, KeyPair keyPair, Integer keyUseCounter, SecretKey hmacSecret) {
        ensureRandomInitialized();

        this.id = id;
        this.userName = userName;
        this.userDisplayName = userDisplayName;
        this.userHandle = userHandle;

        this.rpId = rpId;
        this.rpName = rpName;
        this.u2fRpId = u2fRpId;

        this.keyPair = keyPair;

        if (keyUseCounter != null) {
            this.keyUseCounter = keyUseCounter;
        } else {
            this.keyUseCounter = 0;
        }

        this.hmacSecret = hmacSecret;

        this.generateHmacSecret = generateHmacSecret;
        if (generateHmacSecret) {
            this.hmacSecretAlias = HMAC_SECRET_PREFIX + Base64.encodeToString(id, Base64.NO_WRAP);
        } else {
            this.hmacSecretAlias = null;
        }
    }

    /**
     * Ensure the SecureRandom singleton has been initialized.
     */
    private void ensureRandomInitialized() {
        if (PublicKeyCredentialSource.random == null) {
            PublicKeyCredentialSource.random = new SecureRandom();
        }
    }
}
