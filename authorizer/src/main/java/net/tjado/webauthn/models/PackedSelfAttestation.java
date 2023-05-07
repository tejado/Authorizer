package net.tjado.webauthn.models;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.ctap2.CtapSuccessOutputStream;
import net.tjado.webauthn.fido.ctap2.Messages;

public class PackedSelfAttestation extends AttestationObject {

    /**
     * Construct a new self-attestation attObj in packed format.
     *
     * @param authData  The authenticator data signed.
     * @param signature The signature over the concatenation of authenticatorData and
     *                  clientDataHash.
     */
    public PackedSelfAttestation(byte[] authData, byte[] signature) {
        this.authData = authData;
        this.fmt = FormatType.PACKED;
        this.signature = signature;
        this.certificate = null;
    }

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
