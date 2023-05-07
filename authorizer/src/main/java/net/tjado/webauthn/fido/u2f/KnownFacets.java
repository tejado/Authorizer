package net.tjado.webauthn.fido.u2f;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tjado.webauthn.util.WebAuthnCryptography;

public class KnownFacets {

    private static final String TAG = "KnownFacets";

    private static final Map<String, Pair<String, String>> knownFacets = new HashMap<String, Pair<String, String>>() {{
            put(sha256("https://www.dropbox.com/u2f-app-id.json"),
                        new Pair<>("www.dropbox.com", "https://www.dropbox.com/u2f-app-id.json")); // U2F

            put(sha256("www.dropbox.com"),
                        new Pair<>("www.dropbox.com","www.dropbox.com")); // WebAuthn

            put(sha256("https://www.gstatic.com/securitykey/origins.json"),
                        new Pair<>("google.com", "https://www.gstatic.com/securitykey/origins.json")); // U2F

            put(sha256("google.com"),
                        new Pair<>("google.com", "google.com")); // WebAuthn

            put(sha256("webauthn.io"),
                        new Pair<>("webauthn.io", "webauthn.io"));

            put(sha256("demo.yubico.com"),
                        new Pair<>("demo.yubico.com", "demo.yubico.com"));

            put(sha256("https://github.com/u2f/trusted_facets"),
                        new Pair<>("github.com", "https://github.com/u2f/trusted_facets")); // U2F

            put(sha256("github.com"),
                        new Pair<>("github.com", "github.com")); // WebAuthn

            put(sha256("https://gitlab.com"),
                        new Pair<>("gitlab.com", "https://gitlab.com"));
    }};

    private static final List<byte[]> knownDummyRequests = new ArrayList<byte[]>() {{
        byte[] temp;
        add(new byte[32]); // Firefox challenge & appId

        temp = new byte[32];
        System.arraycopy("A".getBytes(), 0, temp, 0, "A".length()); // Chrome appId
        add(temp);

        temp = new byte[32];
        System.arraycopy("B".getBytes(), 0, temp, 0, "B".length()); // Chrome challenge
        add(temp);
    }};

    public static Pair<String, String> resolveAppIdHash(byte[] application) {
        String appId = Hex.bytesToStringUppercase(application);
        try {
            return knownFacets.get(appId);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isDummyRequest(byte[] application, byte[] challenge) {
        for (byte[] dummy: knownDummyRequests) {
            if (Arrays.equals(application, dummy) || Arrays.equals(challenge, dummy)) {
                return true;
            }
        }
        return false;
    }

    private static String sha256(String text) {
        try {
            return Hex.bytesToStringUppercase(WebAuthnCryptography.sha256(text));
        } catch (Exception e) {
            Log.e(TAG, "Failed to create known facet for " + text);
            return "";
        }
    }
}
