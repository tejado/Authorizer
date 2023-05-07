package net.tjado.webauthn.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.ctap2.CtapSuccessOutputStream;
import net.tjado.webauthn.fido.ctap2.Messages;
import net.tjado.webauthn.util.Base64ByteArrayAdapter;

public class GetAssertionResult extends AuthenticatorResult {
    @SerializedName("selected_credential_id")
    public byte[] selectedCredentialId;
    @SerializedName("authenticator_data")
    public byte[] authenticatorData;
    @SerializedName("signature")
    public byte[] signature;
    @SerializedName("selected_credential_user_handle")
    public byte[] selectedCredentialUserHandle;

    public GetAssertionResult(byte[] selectedCredentialId, byte[] authenticatorData, byte[] signature, byte[] selectedCredentialUserHandle) {
        this.selectedCredentialId = selectedCredentialId;
        this.authenticatorData = authenticatorData;
        this.signature = signature;
        this.selectedCredentialUserHandle = selectedCredentialUserHandle;
    }

    public String toJson() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new Base64ByteArrayAdapter())
                .disableHtmlEscaping()
                .create();
        return gson.toJson(this);
    }

    @Override
    public byte[] asCBOR() throws VirgilException {
        CtapSuccessOutputStream baos = new CtapSuccessOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                .addMap()
                .putMap(Messages.GET_ASSERTION_RESPONSE_CREDENTIAL)
                .put("type", "public-key")
                .put("id", selectedCredentialId)
                .end()
                .put(Messages.GET_ASSERTION_RESPONSE_AUTH_DATA, this.authenticatorData)
                .put(Messages.GET_ASSERTION_RESPONSE_SIGNATURE, this.signature)
                .putMap(Messages.GET_ASSERTION_RESPONSE_USER)
                .put("id", this.selectedCredentialUserHandle)
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
