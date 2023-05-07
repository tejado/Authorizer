package net.tjado.webauthn.fido.u2f;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import net.tjado.webauthn.exceptions.ApduException;
import net.tjado.webauthn.exceptions.ApduException.StatusWord;
import net.tjado.webauthn.fido.CommandApdu;

public class RawMessages {

    public enum AuthenticateControlByte {
        ENFORCE_USER_PRESENCE_AND_SIGN(0x03),
        CHECK_ONLY(0x07),
        DONT_ENFORCE_USER_PRESENCE_AND_SIGN(0x08);

        public final byte value;
        private static final ImmutableMap<Byte, AuthenticateControlByte> REVERSE_MAP = Maps.uniqueIndex(
                Arrays.asList(AuthenticateControlByte.values()),
                AuthenticateControlByte::getValue
        );

        private byte getValue() { return this.value; }

        AuthenticateControlByte(byte value) { this.value = value; }
        AuthenticateControlByte(int value) { this((byte)value); }

        public static AuthenticateControlByte fromByte(byte value) { return REVERSE_MAP.get(value); }
    }

    public enum RequestCommandU2F {
        REGISTER(0x01),
        AUTHENTICATE(0x02),
        VERSION(0x03);

        public final byte value;
        private static final ImmutableMap<Byte, RequestCommandU2F> REVERSE_MAP = Maps.uniqueIndex(
                Arrays.asList(RequestCommandU2F.values()),
                RequestCommandU2F::getValue
        );

        private byte getValue() { return this.value; }

        RequestCommandU2F(byte value) { this.value = value; }
        RequestCommandU2F(int value) { this((byte)value); }

        public static RequestCommandU2F fromByte(byte value) { return REVERSE_MAP.get(value); }
    }

    public static abstract class RequestU2F {  }

    public static RequestU2F parseU2Frequest(CommandApdu apdu) throws ApduException {
        RequestCommandU2F command;
        AuthenticateControlByte controlByte = null;
        byte[] challenge, application, keyHandle;

        switch (RequestCommandU2F.fromByte(apdu.ins)) {
            case REGISTER:
                // We accept any value for P1 since various real-world applications populate
                // this (e.g. with 0x80 if enterprise attestation is requested or with a value
                // mimicking the AuthenticateControlByte).
                if (apdu.p2 != 0x00) {
                    throw new ApduException(StatusWord.INCORRECT_PARAMETERS);
                }
                command = RequestCommandU2F.REGISTER;
                break;
            case AUTHENTICATE:
                switch (AuthenticateControlByte.fromByte(apdu.p1)) {
                    case ENFORCE_USER_PRESENCE_AND_SIGN:
                        controlByte = AuthenticateControlByte.ENFORCE_USER_PRESENCE_AND_SIGN;
                        break;
                    case CHECK_ONLY:
                        controlByte = AuthenticateControlByte.CHECK_ONLY;
                        break;
                    case DONT_ENFORCE_USER_PRESENCE_AND_SIGN:
                        controlByte = AuthenticateControlByte.DONT_ENFORCE_USER_PRESENCE_AND_SIGN;
                        break;
                    default:
                        throw new ApduException(StatusWord.INCORRECT_PARAMETERS);
                }
                if (apdu.p2 != 0x00) {
                    throw new ApduException(StatusWord.INCORRECT_PARAMETERS);
                }
                command = RequestCommandU2F.AUTHENTICATE;
                break;

            case VERSION:
                if (apdu.p1 != 0x00) {
                    throw new ApduException(StatusWord.INCORRECT_PARAMETERS);
                }
                if (apdu.p2 != 0x00) {
                    throw new ApduException(StatusWord.INCORRECT_PARAMETERS);
                }
                command = RequestCommandU2F.VERSION;
                break;

            default:
                throw new ApduException(StatusWord.INS_NOT_SUPPORTED);
        }

        if (apdu.cla != 0x00) {
            throw new ApduException(StatusWord.CLA_NOT_SUPPORTED);
        }

        switch (command) {
            case REGISTER:
                if (apdu.le == 0) {
                    throw new ApduException(StatusWord.WRONG_LENGTH);
                }
                if (apdu.getLc() != 32 + 32) {
                    throw  new ApduException(StatusWord.WRONG_LENGTH);
                }
                challenge = Arrays.copyOf(apdu.data, 32);
                application = Arrays.copyOfRange(apdu.data, 32, 64);

                return new RegistrationRequest(challenge, application);

            case AUTHENTICATE:
                if (apdu.le == 0) {
                    throw new ApduException(StatusWord.WRONG_LENGTH);
                }
                if (apdu.getLc() < 32 + 32 + 1) {
                    throw new ApduException(StatusWord.WRONG_LENGTH);
                }
                int keyHandleLength = apdu.data[64];
                if (apdu.getLc() != 32 + 32 + 1 + keyHandleLength) {
                    throw new ApduException(StatusWord.WRONG_LENGTH);
                }
                challenge = Arrays.copyOf(apdu.data, 32);
                application = Arrays.copyOfRange(apdu.data, 32, 64);
                keyHandle = Arrays.copyOfRange(apdu.data, 65, 65 + keyHandleLength);

                return new AuthenticationRequest(controlByte, challenge, application, keyHandle);

            case VERSION:
                if (apdu.getLc() != 0) {
                    throw new ApduException(StatusWord.WRONG_LENGTH);
                }
                // In order to pass the FIDO tests, we must not make any assumption on le here
                return new VersionRequest();

            default:
                throw new ApduException(StatusWord.INS_NOT_SUPPORTED);
        }
    }

    public static class RegistrationRequest extends RequestU2F {
        public final byte[] challenge;
        public final byte[] application;

        RegistrationRequest(byte[] challenge, byte[] application) {
            this.challenge = challenge;
            this.application = application;
        }
    }

    public static class AuthenticationRequest extends RequestU2F {
        public final AuthenticateControlByte controlByte;
        public final byte[] challenge;
        public final byte[] application;
        public final byte[] keyHandle;

        AuthenticationRequest(AuthenticateControlByte controlByte, byte[] challenge,
                              byte[] application, byte[] keyHandle) {
            this.controlByte = controlByte;
            this.challenge = challenge;
            this.application = application;
            this.keyHandle = keyHandle;
        }
    }

    public static class VersionRequest extends RequestU2F {}

    static public abstract class Response {
        public byte[] data;
        public final StatusWord statusWord = StatusWord.NO_ERROR;
        public Boolean userVerification;
    }

    static public class RegistrationResponse extends Response {
        public final byte[] userPublicKey;
        public final byte[] keyHandle;
        public final byte[] attestationCert;
        public final byte[] signature;

        public RegistrationResponse(byte[] userPublicKey, byte[] keyHandle, byte[] attestationCert,
                                    byte[] signature, boolean userVerification) {
            this.userPublicKey = userPublicKey;
            this.keyHandle = keyHandle;
            this.attestationCert = attestationCert;
            this.signature = signature;
            this.userVerification = userVerification;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write((byte)0x05);

            try {
                outputStream.write(userPublicKey);
                outputStream.write(keyHandle.length);
                outputStream.write(keyHandle);
                outputStream.write(attestationCert);
                outputStream.write(signature);
            } catch (Exception ignore) { }

            data = outputStream.toByteArray();
        }
    }

    static public class AuthenticationResponse extends Response {
        public final byte[] assertion;

        public AuthenticationResponse(byte[] assertion, boolean userVerification) {
            this.assertion = assertion;
            this.data = assertion;
            this.userVerification = userVerification;
        }
    }

    static public class VersionResponse extends Response {
        public VersionResponse() {
            userVerification = false;
            data = "U2F_V2".getBytes();
        }
    }
}
