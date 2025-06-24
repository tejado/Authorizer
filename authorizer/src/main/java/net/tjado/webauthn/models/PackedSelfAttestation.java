package net.tjado.webauthn.models;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.ctap2.CtapSuccessOutputStream;
import net.tjado.webauthn.fido.ctap2.Messages;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

public class PackedSelfAttestation extends AttestationObject {

    /**
     * Construct a new self-attestation attObj in packed format.
     *
     * @param authData  The authenticator data signed.
     * @param keyPair   Generated credential keypair
     * @param signature The signature over the concatenation of authenticatorData and
     *                  clientDataHash.
     */
    public PackedSelfAttestation(byte[] authData, KeyPair keyPair, byte[] signature) {
        this.authData = authData;
        this.fmt = FormatType.PACKED;
        this.signature = signature;

        try
        {
            Security.addProvider(new BouncyCastleProvider());

            // Subject
            X500Name subject = new X500Name("C=DE,O=Authorizer Passkey,OU=Authenticator Attestation,CN=Self-Signed Virtual FIDO2");

            // Validity
            Calendar calendar = Calendar.getInstance();
            Date notBefore = calendar.getTime();
            calendar.add(Calendar.YEAR, 10);
            Date notAfter = calendar.getTime();

            // Certificate builder
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subject,
                    BigInteger.valueOf(System.currentTimeMillis()),
                    notBefore,
                    notAfter,
                    subject,
                    keyPair.getPublic()
            );

            // Add Basic Constraints extension
            certBuilder.addExtension(
                    new ASN1ObjectIdentifier("2.5.29.19"), // OID for Basic Constraints
                    true, // critical
                    new BasicConstraints(false) // CA=false
            );

            // Signer
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .setProvider("AndroidOpenSSL").build(keyPair.getPrivate());

            // Build cert
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider("AndroidOpenSSL")
                    .getCertificate(certBuilder.build(signer));

            this.certificate = cert.getEncoded();
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }

    }

//    /**
//     * Construct a new self-attestation attObj in packed format.
//     *
//     * @param authData  The authenticator data signed.
//     * @param signature The signature over the concatenation of authenticatorData and
//     *                  clientDataHash.
//     */
//    public PackedSelfAttestation(byte[] authData, byte[] signature) {
//        this.authData = authData;
//        this.fmt = FormatType.PACKED;
//        this.signature = signature;
//        this.certificate = null;
//    }


    /**
     * Encode this self-attestation attObj as the CBOR required by the WebAuthn spec
     * https://www.w3.org/TR/webauthn/#sctn-attestation
     * https://www.w3.org/TR/webauthn/#packed-attestation
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
