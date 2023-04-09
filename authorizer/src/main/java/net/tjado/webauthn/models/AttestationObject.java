package net.tjado.webauthn.models;

import android.util.Base64;

import androidx.annotation.Nullable;

import java.util.Arrays;

import net.tjado.webauthn.exceptions.VirgilException;

public abstract class AttestationObject {
    public byte[] authData;
    public String fmt;
    protected byte[] signature;
    protected byte[] certificate;

    public static class AlgType {
        public final static long ES256 = -7L;
    }

    public static class FormatType {
        public final static String NONE = "none";

        public final static String PACKED = "packed";

        public final static String U2F_LEGACY = "u2f";
    }

    public abstract byte[] asCBOR() throws VirgilException;

    @Nullable
    public byte[] getSignature() { return this.signature; }

    @Nullable
    public byte[] getCertificate() { return this.certificate; }

    /**
     * Retrieves the credential_id field from the attestation object and converts it to a string
     * Figure 5 is helpful: https://www.w3.org/TR/webauthn/#attestation-object
     *
     * @return String credential id
     */
    public byte[] getCredentialId() {
        // The credential ID is stored within the attested credential data section of the attestation object
        // field lengths are as follows (in bytes):
        // rpId = 32, flags = 1, counter = 4, AAGUID = 16, L = 2, credential ID = L

        // first we retrieve L, which is at offset 53 (and is big-endian)
        int l = (this.authData[53] << 8) + this.authData[54];
        // then retrieve the credential id field from offset 55
        return Arrays.copyOfRange(this.authData, 55, 55 + l);
    }

    /**
     * Retrieves the credential_id field from the attestation object and converts it to a string
     * Figure 5 is helpful: https://www.w3.org/TR/webauthn/#attestation-object
     *
     * @return String credential id
     */
    public String getCredentialIdBase64() {
        return Base64.encodeToString(this.getCredentialId(), Base64.NO_WRAP);
    }
}
