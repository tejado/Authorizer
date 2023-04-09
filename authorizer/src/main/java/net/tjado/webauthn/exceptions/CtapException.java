package net.tjado.webauthn.exceptions;

import android.util.Log;

public class CtapException extends Exception {
    public enum CtapError {
        INVALID_COMMAND(0x01),
        INVALID_PARAMETER(0x02),
        INVALID_LENGTH(0x03),
        CBOR_UNEXPECTED_TYPE(0x11),
        INVALID_CBOR(0x12),
        MISSING_PARAMETER(0x14),
        UNSUPPORTED_EXTENSION(0x16),
        CREDENTIAL_EXCLUDED(0x19),
        UNSUPPORTED_ALGORITHM(0x26),
        OPERATION_DENIED(0x27),
        KEYSTORE_FULL(0x28),
        UNSUPPORTED_OPTION(0x2B),
        INVALID_OPTION(0x2C),
        KEEP_ALIVE_CANCEL(0x2D),
        NO_CREDENTIALS(0x2E),
        USER_ACTION_TIMEOUT(0x2F),
        NOT_ALLOWED(0x30),
        PIN_INVALID(0x31),
        PIN_BLOCKED(0x32),
        PIN_AUTH_INVALID(0x33),
        PIN_AUTH_BLOCKED(0x34),
        PIN_NOT_SET(0x35),
        PIN_REQUIRED(0x36),
        PIN_POLICY_VIOLATION(0x37),
        PIN_TOKEN_EXPIRED(0x38),
        REQUEST_TOO_LARGE(0x39),
        OTHER(0x7F);

        public final byte value;

        CtapError(byte value) {
            this.value = value;
        }
        CtapError(int value) {
            this((byte)value);
        }
    }

    private final byte errorCode;
    public CtapException(CtapError code) {
        super();
        Log.e("CTAP_ERR", "Returning " + code.toString());
        errorCode = code.value;
    }

    public CtapException(CtapError code, String message) {
        super(message);
        Log.e("CTAP_ERR", message);
        errorCode = code.value;
    }

    public byte getErrorCode() {
        return errorCode;
    }
}
