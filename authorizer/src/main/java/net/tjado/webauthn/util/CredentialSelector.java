package net.tjado.webauthn.util;

import java.util.List;

import net.tjado.webauthn.models.PublicKeyCredentialSource;

public interface CredentialSelector {
    PublicKeyCredentialSource selectFrom(List<PublicKeyCredentialSource> credentialList);
}
