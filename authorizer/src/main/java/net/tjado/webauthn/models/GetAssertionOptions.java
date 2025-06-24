package net.tjado.webauthn.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;
import net.tjado.webauthn.exceptions.CtapException;
import net.tjado.webauthn.exceptions.CtapException.CtapError;
import net.tjado.webauthn.fido.ctap2.Messages;
import net.tjado.webauthn.fido.ctap2.Messages.RequestCommandCTAP2;
import net.tjado.webauthn.util.Base64ByteArrayAdapter;

public class GetAssertionOptions extends AuthenticatorOptions {
    @SerializedName("rpId")
    public String rpId;
    @SerializedName("clientDataHash")
    public byte[] clientDataHash;
    @SerializedName("allowCredentialDescriptorList")
    public List<PublicKeyCredentialDescriptor> allowCredentialDescriptorList;
    @SerializedName("requireUserPresence")
    public boolean requireUserPresence;
    @SerializedName("requireUserVerification")
    public boolean requireUserVerification;
    @SerializedName("authenticatorExtensions")
    public List<AuthenticatorExtension> extensions;
    @SerializedName("pinAuth")
    public byte[] pinAuth;
    @SerializedName("pinProtocol")
    public Long pinProtocol;

    private static final String TAG = "GetAssertionOptions";

    public GetAssertionOptions() {
        super(RequestCommandCTAP2.GetAssertion);
    }

    @Override
    public GetAssertionOptions fromCBor(Map inputMap) {

        UnsignedInteger index;
        /* Excluded Credential list */
        try {
            index = new UnsignedInteger(Messages.GET_ASSERTION_ALLOW_LIST);
            Array excludeList = (Array) inputMap.get(index);

            allowCredentialDescriptorList = new ArrayList<>();
            for (DataItem cborCredential : excludeList.getDataItems()) {
                Map cborCred = (Map) cborCredential;
                allowCredentialDescriptorList.add(
                        new PublicKeyCredentialDescriptor(
                                ((UnicodeString) cborCred.get(new UnicodeString("type"))).getString(),
                                ((ByteString) cborCred.get(new UnicodeString("id"))).getBytes(),
                                new ArrayList<>()
                        )
                );
            }
        } catch (Exception ignore) {}

        /* Client Data Hash */
        index = new UnsignedInteger(Messages.GET_ASSERTION_CLIENT_DATA_HASH);
        clientDataHash = ((ByteString)inputMap.get(index)).getBytes();

        /* User requirements */
        try {
            Map options = (Map)inputMap.get(new UnsignedInteger(Messages.GET_ASSERTION_OPTIONS));
            Boolean up = null, uv = null;
            try {
                up = (options.get(new UnicodeString("up"))).equals(SimpleValue.TRUE);
            } catch (Exception ignore) {}
            try {
                uv = (options.get(new UnicodeString("uv"))).equals(SimpleValue.TRUE);
            } catch (Exception ignore) {}

            if (up == null && uv == null) {
                up = true;
                uv = false;
            } else if (up != null) {
                uv = !up;
            } else {
                up = !uv;
            }

            requireUserPresence = up;
            requireUserVerification = uv;
        } catch (Exception e) {
            requireUserPresence = true;
            requireUserVerification = false;
        }

        /* Relying Party */
        index = new UnsignedInteger(Messages.GET_ASSERTION_RP_ID);
        rpId = ((UnicodeString)inputMap.get(index)).getString();

        /* PIN */
        try {
            index = new UnsignedInteger(Messages.GET_ASSERTION_PIN_AUTH);
            pinAuth = ((ByteString)inputMap.get(index)).getBytes();
        } catch (Exception ignored) {}
        try {
            index = new UnsignedInteger(Messages.GET_ASSERTION_PIN_PROTOCOL);
            pinProtocol = ((UnsignedInteger)inputMap.get(index)).getValue().longValue();
        } catch (Exception ignored) {}

        /* Attestation extensions */
        try {
            extensions = AuthenticatorExtension.fromCbor(
                    (Map)inputMap.get(new UnsignedInteger(Messages.GET_ASSERTION_EXTENSIONS)),
                    action
            );
        } catch (Exception e) {
            extensions = new ArrayList<>();
        }

        return this;
    }

    public void areWellFormed() throws CtapException {
        if (clientDataHash.length != 32) {
            throw new CtapException(CtapError.INVALID_LENGTH,
                    "Client data hash of length: " + clientDataHash.length);
        }

        if (requireUserPresence == requireUserVerification) { // only one may be set
            throw new CtapException(CtapError.UNSUPPORTED_OPTION, "Both uv and up are " +
                        requireUserVerification);
        }

        ClientPINOptions.pinOptionsWellFormed(pinProtocol, pinAuth);
    }

    public static GetAssertionOptions fromJSON(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new Base64ByteArrayAdapter())
                .create();
        return gson.fromJson(json, GetAssertionOptions.class);
    }
}
