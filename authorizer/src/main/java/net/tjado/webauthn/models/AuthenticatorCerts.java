package net.tjado.webauthn.models;

class AuthenticatorCerts {
    public static final String WEBAUTHN_BATCH_ATTESTATION_CERTIFICATE =
            "-----BEGIN CERTIFICATE-----\n" +
            "-----END CERTIFICATE-----\n";

    public static final String WEBAUTH_BATCH_ATTESTATION_SIGNING_KEY =
            "-----BEGIN PRIVATE KEY-----\n" +
            "-----END PRIVATE KEY-----\n";

    public static final String U2F_AUTHENTICATION_BATCH_CERTIFICATE =
            "-----BEGIN CERTIFICATE-----\n" +
            "-----END CERTIFICATE-----\n";

    public static final String U2F_AUTHENTICATION_BATCH_SIGNING_KEY =
            "-----BEGIN PRIVATE KEY-----\n" +
            "-----END PRIVATE KEY-----\n";
}
