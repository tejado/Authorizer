package net.tjado.webauthn.models;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.ctap2.CtapSuccessOutputStream;
import net.tjado.webauthn.fido.ctap2.Messages;

public final class DummyAttestation extends AttestationObject  {
    @Override
    public byte[] asCBOR() throws VirgilException {
        CtapSuccessOutputStream baos = new CtapSuccessOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(Messages.MAKE_CREDENTIAL_RESPONSE_AUTH_DATA, new byte[37])
                    .put(Messages.MAKE_CREDENTIAL_RESPONSE_FMT, Messages.AttestationType.SELF.format)
                    .putMap(Messages.MAKE_CREDENTIAL_RESPONSE_ATT_STMT)
                    .put("alg", Messages.COSE_ID_ES256)
                    .put("sig", new byte[0])
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
