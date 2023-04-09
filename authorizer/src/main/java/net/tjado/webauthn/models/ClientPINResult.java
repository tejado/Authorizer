package net.tjado.webauthn.models;

import android.util.Pair;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.MapBuilder;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.ctap2.CtapSuccessOutputStream;
import net.tjado.webauthn.fido.ctap2.Messages;

public class ClientPINResult extends AuthenticatorResult {
    public byte[] KeyAgreement_x;
    public byte[] KeyAgreement_y;
    public final byte[] pinToken;
    public final Long retries;

    public ClientPINResult(Pair<byte[], byte[]> KeyAgreement, byte[] pinToken, Long retries) {
        if (KeyAgreement != null) {
            this.KeyAgreement_x = KeyAgreement.first;
            this.KeyAgreement_y = KeyAgreement.second;
        }
        this.pinToken = pinToken;
        this.retries = retries;
    }

    @Override
    public byte[] asCBOR() throws VirgilException {
        CtapSuccessOutputStream baos = new CtapSuccessOutputStream();
        try {
            MapBuilder<CborBuilder> builder = new CborBuilder().addMap();

            if (KeyAgreement_x != null && KeyAgreement_y != null) {
                 builder.putMap(Messages.CLIENT_PIN_RESPONSE_KEY_AGREEMENT)
                        .put(Messages.COSE_KEY_EC256_X, KeyAgreement_x)
                        .put(Messages.COSE_KEY_EC256_Y, KeyAgreement_y)
                        .end();
            }

            if (pinToken != null) {
                builder.put(Messages.CLIENT_PIN_RESPONSE_PIN_TOKEN, pinToken);
            }

            if (retries != null) {
                builder.put(Messages.CLIENT_PIN_RESPONSE_RETRIES, retries);
            }

            new CborEncoder(baos).encode(builder.end().build());

        } catch (CborException e) {
            throw new VirgilException("couldn't serialize to cbor", e);
        }
        return baos.toByteArray();
    }

}
