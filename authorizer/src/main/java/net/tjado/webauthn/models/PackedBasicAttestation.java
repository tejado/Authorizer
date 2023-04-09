package net.tjado.webauthn.models;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.ctap2.CtapSuccessOutputStream;
import net.tjado.webauthn.fido.ctap2.Messages;
import net.tjado.webauthn.util.Pkcs8PemParser;
import net.tjado.webauthn.util.WebAuthnCryptography;

public class PackedBasicAttestation extends AttestationObject {

    private static final String ATTESTATION_CERTIFICATE =
            AuthenticatorCerts.WEBAUTHN_BATCH_ATTESTATION_CERTIFICATE;

    private static final String PRIVATE_KEY =
            AuthenticatorCerts.WEBAUTH_BATCH_ATTESTATION_SIGNING_KEY;

    /**
     * Construct a new basic-attestation attObj in packed format.
     *
     * @param authData  The authenticator data signed.
     * @param cryptoProvider Crypto utility loaded with necessary algos for signing
     * @param toSign Content to be signed with the batch key
     * @param signature Authenticator signature using the created credentials
     */
    public PackedBasicAttestation(byte[] authData,
                                  WebAuthnCryptography cryptoProvider,
                                  byte[] toSign,
                                  Signature signature)
            throws NullPointerException, VirgilException {

        this.authData = authData;
        this.fmt = FormatType.PACKED;

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
            this.signature = cryptoProvider.performSignature(pk, toSign, signature);
        } catch (VirgilException e) {
            throw new VirgilException("couldn't create basic attestation object" + e.toString());
        }
    }

    /**
     * Encode this basic-attestation attObj as the CBOR required by the WebAuthn spec
     * https://www.w3.org/TR/webauthn/#sctn-attestation
     * https://www.w3.org/TR/webauthn/#packed-attestation
     *
     * See here for specific details and differences to PackedSelfAttestation
     * https://fidoalliance.org/specs/fido-uaf-v1.0-ps-20141208/fido-uaf-protocol-v1.0-ps-20141208.html
     *
     * @return CBOR encoding of the attestation object as a byte array
     * @throws VirgilException
     */
    @Override
    public byte[] asCBOR() throws VirgilException {
        CtapSuccessOutputStream baos = new CtapSuccessOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(Messages.MAKE_CREDENTIAL_RESPONSE_AUTH_DATA, this.authData)
                    .put(Messages.MAKE_CREDENTIAL_RESPONSE_FMT, this.fmt)
                    .putMap(Messages.MAKE_CREDENTIAL_RESPONSE_ATT_STMT)
                    .put("alg", AlgType.ES256)
                    .put("sig", this.signature)
                    .putArray("x5c")
                    .add(this.certificate)
                    .end()
                    .end()
                    .end()
                    .build()
            );
        } catch (CborException e) {
            throw new VirgilException("couldn't serialize to cbor", e);
        }
        return baos.toByteArray();
    }
}
