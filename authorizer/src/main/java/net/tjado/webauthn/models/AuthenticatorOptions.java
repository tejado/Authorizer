package net.tjado.webauthn.models;

import co.nstant.in.cbor.model.Map;
import net.tjado.webauthn.exceptions.CtapException;
import net.tjado.webauthn.fido.ctap2.Messages.RequestCommandCTAP2;

public abstract class AuthenticatorOptions {
    public abstract AuthenticatorOptions fromCBor(Map inputMap);
    public abstract void areWellFormed() throws CtapException;
    public final RequestCommandCTAP2 action;

    public AuthenticatorOptions(RequestCommandCTAP2 action) {
        this.action = action;
    }
}
