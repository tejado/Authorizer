package net.tjado.webauthn.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PublicKeyCredentialDescriptor {
    @SerializedName("type")
    public final String type;
    @SerializedName("id")
    public final byte[] id;
    @SerializedName("transports")
    public final List<String> transports;

    public PublicKeyCredentialDescriptor(String type, byte[] id, List<String> transports) {
        this.type = type;
        this.id = id;
        this.transports = transports;
    }
}
