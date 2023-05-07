package net.tjado.webauthn.models;

import net.tjado.webauthn.exceptions.VirgilException;

public abstract class AuthenticatorResult {
    public abstract byte[] asCBOR() throws VirgilException;
}
