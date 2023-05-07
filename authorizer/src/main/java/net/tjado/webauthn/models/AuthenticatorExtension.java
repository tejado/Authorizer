package net.tjado.webauthn.models;

import android.util.Log;

import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;
import net.tjado.webauthn.exceptions.CtapException;
import net.tjado.webauthn.exceptions.CtapException.CtapError;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.ctap2.Messages;
import net.tjado.webauthn.fido.ctap2.Messages.RequestCommandCTAP2;
import net.tjado.webauthn.util.WebAuthnCryptography;

public class AuthenticatorExtension {
    final static private String TAG = "AuthenticatorExtension";
    final public String name;
    final public ExtensionInput input;
    final public RequestCommandCTAP2 action;

    public static final List<String> supportedExtension = new ArrayList<>(
        Arrays.asList("hmac-secret",
                      "txAuthSimple")
    );

    public AuthenticatorExtension(String name, ExtensionInput input, RequestCommandCTAP2 action) {
        this.name = name;
        this.input = input;
        this.action = action;
    }

    public static List<AuthenticatorExtension> fromCbor(Map extensions,
                                                        @NonNull RequestCommandCTAP2 action) throws CtapException, VirgilException {
        if (extensions == null) {
            return new ArrayList<>();
        }

        if (action != RequestCommandCTAP2.MakeCredential && action != RequestCommandCTAP2.GetAssertion) {
            throw new VirgilException(
                    "Extensions only available during credential registration and auth");
        }

        final List<AuthenticatorExtension> extensionList = new ArrayList<>();

        for (DataItem entry : extensions.getKeys()) {
            String key = ((UnicodeString)entry).getString();
            DataItem extension = extensions.get(entry);
            if (!supportedExtension.contains(key)) {
                Log.d(TAG, "Ignoring unsupported/unknown extension: " + key);
                continue;
            }
            Map extensionMap;
            DataItem index;

            ExtensionInput input;
            switch (key) {
                case "hmac-secret":
                    if (action == RequestCommandCTAP2.MakeCredential) {
                        if (!extension.equals(SimpleValue.TRUE)) {
                            throw new CtapException(CtapError.INVALID_PARAMETER,
                                        "Input should be true for hmac-secret make credential");
                        }
                        input = new NoInput();

                    } else /*if (action == RequestCommand.GetAssertion)*/ {
                        extensionMap = (Map)extension;

                        index = new UnsignedInteger(Messages.HMAC_SECRET_KEY_AGREEMENT);
                        Map agreement = (Map)extensionMap.get(index);

                        index = new NegativeInteger(Messages.COSE_KEY_EC256_X);
                        byte[] rawX = ((ByteString)agreement.get(index)).getBytes();
                        index = new NegativeInteger(Messages.COSE_KEY_EC256_Y);
                        byte[] rawY =((ByteString)agreement.get(index)).getBytes();

                        index = new UnsignedInteger(Messages.HMAC_SECRET_SALT_ENC);
                        byte[] saltEnc = ((ByteString)extensionMap.get(index)).getBytes();
                        index = new UnsignedInteger(Messages.HMAC_SECRET_SALT_AUTH);
                        byte[] saltAuth = ((ByteString)extensionMap.get(index)).getBytes();

                        ECPoint publicPoint = new ECPoint(new BigInteger(1, rawX),
                                                          new BigInteger(1, rawY));

                        input = new HmacSecretInput(saltEnc, saltAuth, publicPoint);
                    }
                    break;

                case "txAuthSimple":
                    if (action != RequestCommandCTAP2.GetAssertion) {
                        throw new CtapException(CtapError.UNSUPPORTED_EXTENSION);
                    }
                    String prompt = ((UnicodeString)extension).getString();
                    input = new TxSimpleAuthInput(prompt);
                    break;

                default:
                    // This statement should never be reached
                    throw new CtapException(CtapError.UNSUPPORTED_EXTENSION);
            }

            extensionList.add(new AuthenticatorExtension(key, input, action));
        }

        return extensionList;
    }

    public DataItem process(KeyPair keyAgreement, ICredentialSafe credentialSafe,
                             PublicKeyCredentialSource credentialSource) throws VirgilException, CtapException {
        if (name == null || input == null) {
            throw new VirgilException("Processing called with null parameters!");
        }

        if (action != RequestCommandCTAP2.MakeCredential && action != RequestCommandCTAP2.GetAssertion) {
            throw new VirgilException(
                    "Extensions only available during credential registration and auth");
        }

        switch (name) {
            case "hmac-secret":
                if (action == RequestCommandCTAP2.MakeCredential) {
                    if (!(input instanceof NoInput)) {
                        throw new VirgilException("Extension type and input don't match!");
                    }
                    return SimpleValue.TRUE;
                } else /*if (action == RequestCommand.GetAssertion)*/ {
                    if (!(input instanceof HmacSecretInput)) {
                        throw new VirgilException("Extension type and input don't match!");
                    }

                    if (credentialSource == null || credentialSource.hmacSecretAlias == null) {
                        throw new CtapException(CtapError.NO_CREDENTIALS, "Missing hmac secret");
                    }

                    ECParameterSpec parameterSpec = ((ECPublicKey)keyAgreement.getPublic()).getParams();
                    ECPublicKeySpec publicSpec = new ECPublicKeySpec(
                                                    ((HmacSecretInput) input).keyAgreement,
                                                    parameterSpec);
                    KeyFactory keyFactory;
                    PublicKey publicKey;

                    try {
                        keyFactory = KeyFactory.getInstance("EC");
                        publicKey = keyFactory.generatePublic(publicSpec);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        e.printStackTrace();
                        throw new VirgilException("Could not generate Public Key for hmac-secret");
                    }
                    byte[] sharedSecret = WebAuthnCryptography.generateSharedSecret(
                                                                keyAgreement.getPrivate(),
                                                                publicKey);
                    byte[] localSaltAuth = WebAuthnCryptography.encodeHmacSHA256(
                                                                sharedSecret,
                                                                ((HmacSecretInput) input).saltEnc);
                    localSaltAuth = Arrays.copyOf(localSaltAuth, 16);

                    if (!Arrays.equals(localSaltAuth, ((HmacSecretInput) input).saltAuth)) {
                        throw new CtapException(CtapError.INVALID_PARAMETER, "Invalid saltAuth");
                    }

                    byte[] salt = WebAuthnCryptography.decryptAES256_CBC(sharedSecret,
                                                                ((HmacSecretInput) input).saltEnc);

                    if (salt.length != 32 && salt.length != 64) {
                        throw new CtapException(CtapError.INVALID_PARAMETER, "Invalid saltAuth length");
                    }

                    SecretKey key = credentialSource.hmacSecret;
                    byte[] output = null;

                    if (salt.length == 32) {
                        byte[] hmac_out = WebAuthnCryptography.encodeHmacSHA256(key, salt);
                        output = WebAuthnCryptography.encryptAES256_CBC(sharedSecret, hmac_out);

                    } else /* if (salt.length == 64) */ {
                        byte[] hmac_out_1 = WebAuthnCryptography.encodeHmacSHA256(key,
                                                Arrays.copyOf(salt, 32));
                        byte[] hmac_out_2 = WebAuthnCryptography.encodeHmacSHA256(key,
                                                Arrays.copyOfRange(salt, 32, 64));
                        output = new byte[64];

                        System.arraycopy(hmac_out_1,0, output, 0 , 32);
                        System.arraycopy(hmac_out_2,0, output, 32, 32);
                    }

                    return new ByteString(output);
                }

            case "txAuthSimple":
                if (!(input instanceof TxSimpleAuthInput)) {
                    throw new VirgilException("Extension type and input don't match!");
                }
                if (action != RequestCommandCTAP2.GetAssertion) {
                    throw new CtapException(CtapError.UNSUPPORTED_EXTENSION);
                }

                return new UnicodeString(((TxSimpleAuthInput)input).displayString);

            default:
                throw new CtapException(CtapError.UNSUPPORTED_EXTENSION);
        }
    }

    public static Map processAll(List<AuthenticatorExtension> extensions,
                                                    KeyPair keyAgreement,
                                                    ICredentialSafe credentialSafe,
                                                    PublicKeyCredentialSource credentialSource) {
        Map outputs = new Map();

        if (extensions == null) {
            return outputs;
        }

        for (AuthenticatorExtension extension: extensions) {
            DataItem res;
            try{
                res = extension.process(keyAgreement, credentialSafe, credentialSource);
            } catch (Exception e) {
                Log.d(TAG, "Skipping extension due to: ");
                e.printStackTrace();
                continue;
            }

            outputs.put(new UnicodeString(extension.name), res);
        }

        return outputs;
    }

    private static abstract class ExtensionInput {}

    private static class NoInput extends ExtensionInput {}

    private static class TxSimpleAuthInput extends ExtensionInput {
        final String displayString;

        TxSimpleAuthInput(String displayString) {
            this.displayString = displayString;
        }
    }

    private static class HmacSecretInput extends ExtensionInput {
        final byte[] saltEnc;
        final byte[] saltAuth;
        final ECPoint keyAgreement;

        HmacSecretInput(byte[] saltEnc, byte[] saltAuth, ECPoint keyAgreement) {
            this.saltEnc = saltEnc;
            this.saltAuth = saltAuth;
            this.keyAgreement = keyAgreement;
        }
    }
}
