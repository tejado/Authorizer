package net.tjado.webauthn.fido.ctap2;

import java.io.ByteArrayOutputStream;

public class CtapSuccessOutputStream extends ByteArrayOutputStream {

    public CtapSuccessOutputStream() {
        super();
        // Always prepend a zero in the first position
        this.write(0);
    }
}
