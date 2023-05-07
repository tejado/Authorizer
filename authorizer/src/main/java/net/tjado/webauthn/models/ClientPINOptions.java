package net.tjado.webauthn.models;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.UnsignedInteger;
import net.tjado.webauthn.exceptions.CtapException;
import net.tjado.webauthn.exceptions.CtapException.CtapError;
import net.tjado.webauthn.fido.ctap2.Messages;
import net.tjado.webauthn.fido.ctap2.Messages.RequestCommandCTAP2;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class ClientPINOptions extends AuthenticatorOptions {
    public Long pinProtocol = null;
    public Long subCommand = null;
    public PublicKey keyAgreement = null;
    public byte[] pinAuth = null;
    public byte[] newPinEnc = null;
    public byte[] pinHashEnc = null;
    private ECParameterSpec agreementParams;

    static final List<Long> supportedProtocols = new ArrayList<>(Arrays.asList(1L));

    public ClientPINOptions() {
        super(RequestCommandCTAP2.ClientPIN);
    }

    public void areWellFormed() throws CtapException {
        // Currently there is only one pinProtocol version
        if (!pinProtocolSupported(pinProtocol)) {
            throw new CtapException(CtapError.PIN_AUTH_INVALID, "Unsupported pin protocol: "
                    + pinProtocol );
        }

        // Invalid subcommand
        if (subCommand < 1 || subCommand > 5) {
            throw new CtapException(CtapError.INVALID_PARAMETER);
        }

        if (pinAuth != null) {
            if (pinAuth.length != 16) {
                throw new CtapException(CtapError.INVALID_LENGTH,
                        "Received pinAuth length is not 16: " + pinAuth.length);
            }
        }

        if (newPinEnc != null) {
            if (newPinEnc.length < 64) {
                throw new CtapException(CtapError.INVALID_LENGTH,
                        "Received newPinEnc length is less than 64: " + newPinEnc.length);
            }
        }

        if (pinHashEnc != null) {
            if (pinHashEnc.length != 16) {
                throw new CtapException(CtapError.INVALID_LENGTH,
                        "Received pinHashEnc length is not 16: " + pinHashEnc.length);
            }
        }
    }

    static boolean pinProtocolSupported(Long pinProtocol) {
        return supportedProtocols.contains(pinProtocol);
    }

    static void pinOptionsWellFormed(Long pinProtocol, byte[] pinAuth) throws CtapException {
        if (pinAuth != null) {
            if (pinAuth.length != 16) {
                throw new CtapException(CtapError.INVALID_LENGTH,
                                        "Received pinAuth length is not 16: " + pinAuth.length);
            }
            if (pinProtocol == null) {
                throw new CtapException(CtapError.MISSING_PARAMETER,
                                        "pinAuth provided but pinProtocol is missing!");
            }
            if (!pinProtocolSupported(pinProtocol)) {
                throw new CtapException(CtapError.PIN_AUTH_INVALID,
                                        "Unsupported pin protocol: " + pinProtocol );
            }
        } else {
            if (pinProtocol != null) {
                throw new CtapException(CtapError.MISSING_PARAMETER,
                                        "pinProtocol provided byt pinAuth missing!");
            }
        }
    }

    @Override
    public ClientPINOptions fromCBor(Map inputMap) {

        DataItem index = new UnsignedInteger(Messages.CLIENT_PIN_PIN_PROTOCOL);
        pinProtocol = ((UnsignedInteger)inputMap.get(index)).getValue().longValue();
        index = new UnsignedInteger(Messages.CLIENT_PIN_SUB_COMMAND);
        subCommand = ((UnsignedInteger)inputMap.get(index)).getValue().longValue();

        try {
            index = new UnsignedInteger(Messages.CLIENT_PIN_KEY_AGREEMENT);
            Map keyAgreementMap = (Map)inputMap.get(index);

            index = new NegativeInteger(Messages.COSE_KEY_EC256_X);
            byte[] rawX = ((ByteString)keyAgreementMap.get(index)).getBytes();
            index = new NegativeInteger(Messages.COSE_KEY_EC256_Y);
            byte[] rawY = ((ByteString)keyAgreementMap.get(index)).getBytes();

            ECPoint point = new ECPoint(new BigInteger(1, rawX),
                                        new BigInteger(1, rawY));
            ECPublicKeySpec publicSpec = new ECPublicKeySpec(point, agreementParams);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            keyAgreement = keyFactory.generatePublic(publicSpec);
        } catch (Exception ignore) {}

        try {
            index = new UnsignedInteger(Messages.CLIENT_PIN_PIN_AUTH);
            pinAuth = ((ByteString)inputMap.get(index)).getBytes();
        } catch (Exception ignore) {}
        try {
            index = new UnsignedInteger(Messages.CLIENT_PIN_NEW_PIN_ENC);
            newPinEnc = ((ByteString)inputMap.get(index)).getBytes();
        } catch (Exception ignore) {}
        try {
            index = new UnsignedInteger(Messages.CLIENT_PIN_PIN_HASH_ENC);
            pinHashEnc = ((ByteString)inputMap.get(index)).getBytes();
        } catch (Exception ignore) {}

        return this;
    }

    public ClientPINOptions fromCBor(Map inputMap, ECParameterSpec agreementParams) {
        this.agreementParams = agreementParams;
        return fromCBor(inputMap);
    }
}
