package net.tjado.webauthn.models;

import android.util.Log;

import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.util.Pkcs8PemParser;
import net.tjado.webauthn.util.WebAuthnCryptography;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

public class U2fSelfAttestation extends AttestationObject {

    /**
     * Helper static method to pack in byte format the authenticator data
     * necessary for U2F use cases
     *
     * @param application byte array with registering application information
     * @param challenge byte array containing registering request challenge
     * @param credentialId byte array with the created credential identifier for later auth usage
     * @param serializedPubKey byte array serializing the created credential public key info
     * @return byte array of packed authenticator data information
     * @throws IOException
     */
    public static byte[] packU2fAuthData(byte[] application,
                                         byte[] challenge,
                                         byte[] credentialId,
                                         byte[] serializedPubKey) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[1]);
        baos.write(application);
        baos.write(challenge);
        baos.write(credentialId);
        baos.write(serializedPubKey);
        return baos.toByteArray();
    }

    /**
     * Construct an U2F self-attestation attObj
     *
     * @param authData  The authenticator data as serialized and packed by @link packU2fAuthData
     * @param cryptoProvider Crypto utility loaded with necessary algos for signing
     */
    public U2fSelfAttestation(byte[] authData, WebAuthnCryptography cryptoProvider, PublicKeyCredentialSource credentialSource)
            throws VirgilException {
        this.authData = authData;
        this.fmt = FormatType.U2F_LEGACY;
        this.certificate = null;

        KeyPair keyPair = credentialSource.keyPair;

        // Sign the content
        try {
            this.signature = cryptoProvider.performSignature(keyPair.getPrivate(), this.authData, null);
        } catch (VirgilException e) {
            throw new VirgilException("couldn't create basic attestation object" + e.toString());
        }
    }

    @Override
    public byte[] asCBOR() {
        // CBOR format not needed here
        // TODO: remove CBOR formatting from attestations - handle it externally using handler
        return new byte[0];
    }
}
