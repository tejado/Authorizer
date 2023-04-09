package net.tjado.webauthn.models;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.ctap2.CtapSuccessOutputStream;
import net.tjado.webauthn.fido.ctap2.Messages;

public class GetInfoResult extends AuthenticatorResult {

    public boolean clientPin;
    public byte[] aaguid;

    public GetInfoResult(boolean clientPin, byte[] aaguid) {
        this.clientPin = clientPin;
        this.aaguid = aaguid;
    }

    @Override
    public byte[] asCBOR() throws VirgilException {
        CtapSuccessOutputStream baos = new CtapSuccessOutputStream();
        try {
            MapBuilder<CborBuilder> builder = new CborBuilder().addMap();

            builder.putArray(Messages.GET_INFO_RESPONSE_VERSIONS)
                .add("FIDO_2_0")
                .add("U2F_V2")
                .end();

            ArrayBuilder<MapBuilder<CborBuilder>> arrayBuilder = builder.putArray(Messages.GET_INFO_RESPONSE_EXTENSIONS);
            for (String extension: AuthenticatorExtension.supportedExtension) {
                arrayBuilder.add(extension);
            }
            builder = arrayBuilder.end();

            builder
                .put(Messages.GET_INFO_RESPONSE_AAGUID, aaguid)
                .putMap(Messages.GET_INFO_RESPONSE_OPTIONS)
                    .put("plat", false)
                    .put("rk", true)
                    .put("clientPin", clientPin)
                    .put("up", true)
                    .put("uv", true)
                .end()
                .put(Messages.GET_INFO_RESPONSE_MAX_MSG_SIZE, Messages.MAX_CBOR_MSG_SIZE);

            arrayBuilder = builder.putArray(Messages.GET_INFO_RESPONSE_PIN_PROTOCOLS);

            for (Long pinProtocol: ClientPINOptions.supportedProtocols) {
                arrayBuilder.add(pinProtocol);
            }
            builder = arrayBuilder.end();

                // This value is chosen such that most credential lists will fit into a single
                // request while still staying well below the maximal message size when taking
                // the maximal credential ID length into account.
            builder
                .put(Messages.GET_INFO_RESPONSE_MAX_CREDENTIAL_COUNT_IN_LIST, 5L)
                // Our credential IDs consist of
                // * a signature (32 bytes)
                // * a nonce (32 bytes)
                // * a null byte (WebAuthn only)
                // * the rpName truncated to 64 UTF-16 code units (every UTF-16 code unit can be
                //   coded on at most three UTF-8 bytes)
                .put(Messages.GET_INFO_RESPONSE_MAX_CREDENTIAL_ID_LENGTH, 32 + 32 + 1 + 3 * 64)
                .putArray(Messages.GET_INFO_RESPONSE_TRANSPORTS)
                    .add("usb")
                .end()
                .putArray(Messages.GET_INFO_RESPONSE_ALGORITHMS)
                    .addMap()
                    .put("alg", -7L)
                    .put("type", "public-key")
                    .end()
                .end();

            new CborEncoder(baos).encode(builder.end().build());

        } catch (CborException e) {
            throw new VirgilException("couldn't serialize to cbor", e);
        }
        return baos.toByteArray();
    }
}
