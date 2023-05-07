package net.tjado.webauthn.models;

import android.util.Base64;

public class AuthenticatorConfig {

    // "AAAAAAAAAAAAAAAAAAAAAA==" = 16 zero bytes
    public static final byte[] AAGUID = Base64.decode(
            "AAAAAAAAAAAAAAAAAAAAAA==",
            Base64.DEFAULT);
}
