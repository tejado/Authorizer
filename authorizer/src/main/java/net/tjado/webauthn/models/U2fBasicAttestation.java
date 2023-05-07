package net.tjado.webauthn.models;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.util.Pkcs8PemParser;
import net.tjado.webauthn.util.WebAuthnCryptography;

@RequiresApi(api = Build.VERSION_CODES.O)
public class U2fBasicAttestation extends AttestationObject {

    private static final String ATTESTATION_CERTIFICATE =
            AuthenticatorCerts.U2F_AUTHENTICATION_BATCH_CERTIFICATE;

    private static final String PRIVATE_KEY =
            AuthenticatorCerts.U2F_AUTHENTICATION_BATCH_SIGNING_KEY;

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
    public U2fBasicAttestation(byte[] authData, WebAuthnCryptography cryptoProvider)
            throws VirgilException {
        this.authData = authData;
        this.fmt = FormatType.U2F_LEGACY;

        // Retrieve the batch private key
        PrivateKey pk;
        try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            pk = Pkcs8PemParser.toPrivateKey(PRIVATE_KEY, kf);
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new VirgilException("couldn't extract batch key from PEM format" + e.toString());
        }

        // Retrieve the certificate
        X509Certificate x509Cert;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            x509Cert = (X509Certificate) Pkcs8PemParser.toCertificate(ATTESTATION_CERTIFICATE, cf);
            this.certificate = x509Cert.getEncoded();
        } catch (IOException | CertificateException e) {
            throw new VirgilException("couldn't extract batch cert from PEM format" + e.toString());
        }

        // Sign the content
        try {
            this.signature = cryptoProvider.performSignature(pk, this.authData, null);
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
