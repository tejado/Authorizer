package net.tjado.webauthn.models;

import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.fragment.app.FragmentActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.UnsignedInteger;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.Number;
import net.tjado.webauthn.exceptions.CtapException;
import net.tjado.webauthn.exceptions.CtapException.CtapError;
import net.tjado.webauthn.fido.ctap2.Messages;
import net.tjado.webauthn.fido.ctap2.Messages.RequestCommandCTAP2;
import net.tjado.webauthn.util.Base64ByteArrayAdapter;
import net.tjado.webauthn.util.WioRequestDialog;
import rocks.xmpp.precis.PrecisProfile;
import rocks.xmpp.precis.PrecisProfiles;

public class MakeCredentialOptions extends AuthenticatorOptions {
    @SerializedName("clientDataHash")
    public byte[] clientDataHash;
    @SerializedName("rp")
    public final RpEntity rpEntity = new RpEntity();
    @SerializedName("user")
    public final UserEntity userEntity = new UserEntity();
    @SerializedName("requireResidentKey")
    public boolean requireResidentKey;
    @SerializedName("requireUserPresence")
    public boolean requireUserPresence;
    @SerializedName("requireUserVerification")
    public boolean requireUserVerification;
    @SerializedName("credTypesAndPubKeyAlgs")
    public List<Pair<String, Long>> credTypesAndPubKeyAlgs;
    @SerializedName("excludeCredentials")
    public List<PublicKeyCredentialDescriptor> excludeCredentialDescriptorList;
    @SerializedName("authenticatorExtensions")
    public List<AuthenticatorExtension> extensions;
    @SerializedName("pinAuth")
    public byte[] pinAuth;
    @SerializedName("pinProtocol")
    public Long pinProtocol;

    private Boolean dummy = null;

    private static final String TAG = "MakeCredentialOptions";

    public MakeCredentialOptions() {
        super(RequestCommandCTAP2.MakeCredential);
    }

    private static final List<Pair<String,String>> dummyTable = Arrays.asList(
            //          rpId,             userName
            new Pair<>(".dummy",          "dummy"         ),
            new Pair<>("SelectDevice",    "SelectDevice"  )
    );

    @Override
    public MakeCredentialOptions fromCBor(Map inputMap) {

        /* Client Data Hash */
        UnsignedInteger index = new UnsignedInteger(Messages.MAKE_CREDENTIAL_CLIENT_DATA_HASH);
        clientDataHash = ((ByteString)inputMap.get(index)).getBytes();

        /* Relying Party identity */
        index = new UnsignedInteger(Messages.MAKE_CREDENTIAL_RP);
        Map rp = (Map)inputMap.get(index);

        rpEntity.name = ((UnicodeString)rp.get(new UnicodeString("name"))).getString();
        rpEntity.id =  ((UnicodeString)rp.get(new UnicodeString("id"))).getString();
        try {
            rpEntity.iconUri =  ((UnicodeString)rp.get(new UnicodeString("icon"))).getString();
        } catch (Exception ignored) {}

        /* User identity */
        index = new UnsignedInteger(Messages.MAKE_CREDENTIAL_USER);
        Map user = (Map)inputMap.get(index);

        userEntity.id = ((ByteString)user.get(new UnicodeString("id"))).getBytes();
        userEntity.name = ((UnicodeString)user.get(new UnicodeString("name"))).getString();
        try {
            userEntity.displayName = ((UnicodeString)user
                                            .get(new UnicodeString("displayName"))).getString();
        } catch (Exception ignored) {}
        try {
            userEntity.iconUri = ((UnicodeString)user
                                            .get(new UnicodeString("icon"))).getString();
        } catch (Exception ignored) {}

        /* Dummy early return */
        dummyCheck();
        if (dummy) { return this; }

        /* Public Key Credential Parameters */
        index = new UnsignedInteger(Messages.MAKE_CREDENTIAL_PUB_KEY_CRED_PARAMS);
        Array pubKeyCredParams = (Array)inputMap.get(index);

        credTypesAndPubKeyAlgs = new ArrayList<>();
        for (DataItem pubKeyCredParam: pubKeyCredParams.getDataItems()) {
            Map cred = (Map)pubKeyCredParam;
            credTypesAndPubKeyAlgs.add(
                    new Pair<>(
                            ((UnicodeString) cred.get(new UnicodeString("type"))).getString(),
                            ((Number) cred.get(new UnicodeString("alg"))).getValue().longValue()
                    )
            );
        }

        /* Excluded Credential list */
        try {
            index = new UnsignedInteger(Messages.MAKE_CREDENTIAL_EXCLUDE_LIST);
            Array excludeList = (Array) inputMap.get(index);

            excludeCredentialDescriptorList = new ArrayList<>();
            for (DataItem cborCredential : excludeList.getDataItems()) {
                Map cborCred = (Map) cborCredential;
                excludeCredentialDescriptorList.add(
                        new PublicKeyCredentialDescriptor(
                                ((UnicodeString) cborCred.get(new UnicodeString("type"))).getString(),
                                ((ByteString) cborCred.get(new UnicodeString("id"))).getBytes(),
                                new ArrayList<>()
                        )
                );
            }
        } catch (Exception ignore) {}

        /* Registration extensions */
        try {
            extensions = AuthenticatorExtension.fromCbor(
                    (Map)inputMap.get(new UnsignedInteger(Messages.MAKE_CREDENTIAL_EXTENSIONS)),
                    action
            );
        } catch (Exception e) {
            extensions = new ArrayList<>();
        }

        /* Credential options */
        try {
            Map options = (Map)inputMap.get(new UnsignedInteger(Messages.MAKE_CREDENTIAL_OPTIONS));
            Boolean up = null, uv = null;
            try {
                up = (options.get(new UnicodeString("up"))).equals(SimpleValue.TRUE);
            } catch (Exception ignore) {}
            try {
                uv = (options.get(new UnicodeString("uv"))).equals(SimpleValue.TRUE);
            } catch (Exception ignore) {}

            if (up == null) {
                up = true;  // UP is true by default
            }
            if (uv == null) {
                uv = false;  // UV is false by default
            }

            requireUserPresence = up;
            requireUserVerification = uv;
            try {
                requireResidentKey = (options.get(new UnicodeString("rk"))).equals(SimpleValue.TRUE);
            } catch (Exception e) {
                requireResidentKey = false;
            }
        } catch (Exception e) {
            requireUserPresence = true;
            requireUserVerification = false;
            requireResidentKey = false;
        }

        /* PIN */
        try {
            index = new UnsignedInteger(Messages.MAKE_CREDENTIAL_PIN_AUTH);
            pinAuth = ((ByteString)inputMap.get(index)).getBytes();
        } catch (Exception ignored) {}
        try {
            index = new UnsignedInteger(Messages.MAKE_CREDENTIAL_PIN_PROTOCOL);
            pinProtocol = ((UnsignedInteger)inputMap.get(index)).getValue().longValue();
        } catch (Exception ignored) {}

        return this;
    }

    public void areWellFormed() throws CtapException {
        if (clientDataHash.length != 32) {
            throw new CtapException(CtapError.INVALID_LENGTH,
                    "Client data hash of length: " + clientDataHash.length);
        }

        try {
            PrecisProfile profile = PrecisProfiles.USERNAME_CASE_PRESERVED;
            rpEntity.name = profile.enforce(rpEntity.name);
            userEntity.displayName = profile.enforce(userEntity.displayName);
        } catch (Exception e) {
            //TODO: Revise this, Yubico u2f website and python examples are noncompliant...
            Log.e(TAG, "Suppressing PRECIS Profile enforcement...");
//            throw new CtapException(CtapError.OTHER,
//                                "User/entity name \"case preserved\" precis profile not fulfilled");
        }

        if (userEntity.id.length <= 0 || userEntity.id.length > 64) {
            throw new CtapException(CtapError.INVALID_LENGTH,
                    "User id with invalind length: " + userEntity.id.length);
        }

        ClientPINOptions.pinOptionsWellFormed(pinProtocol, pinAuth);
    }

    private void dummyCheck() {
        dummy = false;
        for (Pair pair: dummyTable) {
            if (rpEntity.id.equals(pair.first) && userEntity.name.equals(pair.second)) {
                dummy = true;
                break;
            }
        }
    }

    public boolean areDummys(FragmentActivity fragmentActivity) throws CtapException {
        final Semaphore sem = new Semaphore(0);
        final boolean[] permission = new boolean[1];

        if (dummy == null) {
            dummyCheck();
        }

        if (dummy) {
            WioRequestDialog.PromptCallback callback = new WioRequestDialog.PromptCallback() {
                @Override
                public void onResult(boolean result) {
                    sem.release();
                    permission[0] = result;
                }
            };

            WioRequestDialog dialog = WioRequestDialog.create(
                    "Verifying user presence",
                    "Accept to proceed",
                    callback);
            dialog.show(fragmentActivity);

            try {
                sem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new CtapException(CtapError.OTHER);
            }

            if (!permission[0]) {
                throw new CtapException(CtapError.OPERATION_DENIED);
            }
        }

        return dummy;
    }

    public static MakeCredentialOptions fromJSON(String json) {
        TypeToken<List<Pair<String, Long>>> credTypesType = new TypeToken<List<Pair<String, Long>>>() {
        };
        TypeToken<List<PublicKeyCredentialDescriptor>> excludeListType = new TypeToken<List<PublicKeyCredentialDescriptor>>() {
        };
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new Base64ByteArrayAdapter())
                .registerTypeAdapter(credTypesType.getType(), new CredTypesDeserializer())
                .registerTypeAdapter(excludeListType.getType(), new ExcludeCredentialListDeserializer())
                .create();
        return gson.fromJson(json, MakeCredentialOptions.class);
    }

    private static class CredTypesDeserializer implements JsonDeserializer<List<Pair<String, Long>>> {
        @Override
        public List<Pair<String, Long>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<Pair<String, Long>> credTypes = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                // all elements are arrays like ["public-key", "-7"]
                JsonArray pair = element.getAsJsonArray();
                String type = pair.get(0).getAsString();
                try {
                    long alg = Long.parseLong(pair.get(1).getAsString());
                    credTypes.add(new Pair<>(type, alg));
                } catch (NumberFormatException e) {
                    continue;
                }
            }
            return credTypes;
        }
    }

    private static class ExcludeCredentialListDeserializer implements JsonDeserializer<List<PublicKeyCredentialDescriptor>> {
        @Override
        public List<PublicKeyCredentialDescriptor> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<PublicKeyCredentialDescriptor> excludeList = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray()) {
                    // elements are JSON objects that take the form:
                    // {"type": "public-key", "id": "<base64-bytes>", "transports": ["usb", "nfc", "ble", "internal"] }
                    if (element.isJsonObject()) {
                        JsonObject entryObject = element.getAsJsonObject();
                        String type = entryObject.get("type").getAsString();
                        String idString = entryObject.get("id").getAsString();
                        byte[] id = Base64.decode(idString, Base64.NO_WRAP);
                        List<String> transports = new ArrayList<>();
                        // "transports" is an optional member
                        if (entryObject.has("transports")) {
                            for (JsonElement transport : entryObject.getAsJsonArray("transports")) {
                                transports.add(transport.getAsString());
                            }
                        }
                        excludeList.add(new PublicKeyCredentialDescriptor(type, id, transports));
                    }
                }
            }
            return excludeList;
        }
    }
}

