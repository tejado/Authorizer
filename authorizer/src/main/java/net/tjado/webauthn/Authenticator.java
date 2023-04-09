package net.tjado.webauthn;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt.CryptoObject;
import androidx.fragment.app.FragmentActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;

import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnicodeString;

import net.tjado.passwdsafe.R;
import net.tjado.webauthn.exceptions.ApduException;
import net.tjado.webauthn.exceptions.ApduException.StatusWord;
import net.tjado.webauthn.exceptions.CtapException;
import net.tjado.webauthn.exceptions.CtapException.CtapError;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.u2f.KnownFacets;
import net.tjado.webauthn.fido.u2f.RawMessages;
import net.tjado.webauthn.fido.u2f.RawMessages.Response;
import net.tjado.webauthn.models.AttestationObject;
import net.tjado.webauthn.models.AuthenticatorConfig;
import net.tjado.webauthn.models.AuthenticatorExtension;
import net.tjado.webauthn.models.ClientPINOptions;
import net.tjado.webauthn.models.ClientPINResult;
import net.tjado.webauthn.models.DummyAttestation;

import net.tjado.webauthn.models.GetAssertionOptions;
import net.tjado.webauthn.models.GetAssertionResult;
import net.tjado.webauthn.models.GetInfoResult;
import net.tjado.webauthn.models.ICredentialSafe;
import net.tjado.webauthn.models.MakeCredentialOptions;
import net.tjado.webauthn.models.NoneAttestation;
import net.tjado.webauthn.models.PackedSelfAttestation;
import net.tjado.webauthn.models.PublicKeyCredentialDescriptor;
import net.tjado.webauthn.models.PublicKeyCredentialSource;
import net.tjado.webauthn.models.U2fSelfAttestation;
import net.tjado.webauthn.util.ClientPinLocker;
import net.tjado.webauthn.util.CredentialSelector;
import net.tjado.webauthn.util.WebAuthnCryptography;
import net.tjado.webauthn.util.WioBiometricPrompt;
import net.tjado.webauthn.util.WioBiometricPrompt.PromptCallback;
import net.tjado.webauthn.util.WioRequestDialog;

@RequiresApi(api = Build.VERSION_CODES.P)
public final class Authenticator {
    private static final String TAG = "WebauthnAuthenticator";

    private static final byte[] AAGUID = AuthenticatorConfig.AAGUID;

    private static final Pair<String, Long> ES256_COSE = new Pair<>("public-key", (long) -7);
    ICredentialSafe credentialSafe;
    WebAuthnCryptography cryptoProvider;
    private final ClientPinLocker pinLocker;
    private AuthenticatorStatus internalStatus;

    /* PIN variables */
    private static final CharsetDecoder charsetDec = StandardCharsets.UTF_8.newDecoder();
    private static KeyPair authenticatorKeyAgreement;
    private static ECParameterSpec authenticatorKeySpec;
    private static int conPINmismatches = 0; // MUST reset during power cycle

    private PublicKeyCredentialSource selectedPreflightCredential;

    public enum AuthenticatorStatus {
        IDLE,
        PROCESSING,
        WAITING_FOR_UP
    }

    /**
     * Construct a WebAuthn authenticator backed by a credential safe and cryptography provider.
     *
     * @param ctx                    Application context for database creation
     * @param strongboxRequired      require that keys are stored in HSM
     */
    public Authenticator(Context ctx, boolean strongboxRequired, ICredentialSafe credentialSafe) throws VirgilException {

        this.credentialSafe = credentialSafe;
        this.cryptoProvider = new WebAuthnCryptography(this.credentialSafe);
        this.pinLocker = new ClientPinLocker(ctx, new byte[32], strongboxRequired);

        if (authenticatorKeyAgreement == null) {
            authenticatorKeySpec = null;
            try {
                authenticatorKeyAgreement = this.credentialSafe.keyAgreementPair();
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                throw new VirgilException("Failed to create authenticator agreement key pair", e);
            }
        }
        internalStatus = AuthenticatorStatus.IDLE;
    }

    public AuthenticatorStatus getInternalStatus() {
        return internalStatus;
    }

    void idle() {
        internalStatus = AuthenticatorStatus.IDLE;
    }

    void processing() {
        internalStatus = AuthenticatorStatus.PROCESSING;
    }

    private void upNeeded() {
        internalStatus = AuthenticatorStatus.WAITING_FOR_UP;
    }

    /*
     * CTAP2 commands
     */
    public GetInfoResult getInfo() {
        return new GetInfoResult(pinLocker.isPinSet(), AAGUID);
    }

    /**
     * Perform the authenticatorMakeCredential operation as defined by the WebAuthn spec: https://www.w3.org/TR/webauthn/#op-make-cred
     *
     * @param options The options / arguments to the authenticatorMakeCredential operation.
     * @param activity     The Main/UI context to be used to display a biometric prompt (if required)
     * @return an AttestationObject containing the new credential and attestation information
     * @throws VirgilException Generic error
     * @throws CtapException Error to be returned through the current transport
     */
    public AttestationObject makeCredential(Map options, FragmentActivity activity) throws VirgilException, CtapException {
        MakeCredentialOptions credentialOptions = new MakeCredentialOptions().fromCBor(options);

        return makeCredential(credentialOptions, activity);
    }

    /**
     * Perform the authenticatorMakeCredential operation as defined by the WebAuthn spec: https://www.w3.org/TR/webauthn/#op-make-cred
     *
     * @param options The options / arguments to the authenticatorMakeCredential operation.
     * @param activity     The Main/UI context to be used to display a biometric prompt (if required)
     * @return an AttestationObject containing the new credential and attestation information
     * @throws VirgilException Generic error
     * @throws CtapException Error to be returned through the current transport
     */
    public AttestationObject makeCredential(MakeCredentialOptions options, FragmentActivity activity) throws VirgilException, CtapException {
        // Check if all supplied parameters are syntactically well-formed and of the correct length.
        if (options.areDummys(activity)) {
            return new DummyAttestation();
        }

        options.areWellFormed();

        // 1 Check excludeCredentialDescriptorList for existing credentials for this RP
        if (options.excludeCredentialDescriptorList != null) {
            for (PublicKeyCredentialDescriptor descriptor : options.excludeCredentialDescriptorList) {
                // if we already have a credential identified by this id
                PublicKeyCredentialSource existingCredentialSource;
                existingCredentialSource = credentialSafe.getCredentialSourceById(descriptor.id);

                if (existingCredentialSource != null &&
                    existingCredentialSource.rpId.equals(options.rpEntity.id) &&
                    PublicKeyCredentialSource.type.equals(descriptor.type)) {
                    showDialog(activity, "This authenticator is excluded!", "");

                    throw new CtapException(CtapError.CREDENTIAL_EXCLUDED);
                }
            }
        }

        // 2. Check if we support a compatible credential type
        if (!options.credTypesAndPubKeyAlgs.contains(ES256_COSE)) {
            throw new CtapException(CtapError.UNSUPPORTED_ALGORITHM, "Only ES256 is supported");
        }

        // 3.1 Check requireResidentKey
        // Our authenticator will store resident keys regardless, so we can disregard the value of this parameter

        // 3.2 Check requireUserVerification
        // We always support user Verification

        // 4. Proccess extensions
        Map extensionOutput = AuthenticatorExtension.processAll(options.extensions,
                                                                authenticatorKeyAgreement,
                                                                null,
                                                                null);

        // 5-7 Pin Authentication
        PINverifyClientDataHash(options.pinAuth, options.clientDataHash, activity,(!options.requireUserVerification && isPinSet()));

        // As biometric credential store might get implemented in Authorizer in the future,
        // I will kep the following comment but it is NOT valid anymore.
        // NOTE: We are switching the order of Steps 8 and 9 because Android needs to have the credential
        //       created in order to use it in a biometric prompt
        //       We will delete the credential if the biometric prompt fails


        // Code commented out as we are not using biometric prompt for now. We will keep it for future reference.
        // For now, we use just a simple dialog prompt.
        // 8. Obtain user consent for creating a new credential
        // if we need to obtain user verification, create a biometric prompt for that
        // else just generate a new credential/attestation object
        /*boolean permission;
        if (options.requireUserVerification) {
            String subtitle = "";
            if (options.userEntity.name != null) {
                subtitle = activity.getString(R.string.credentials_userName, options.userEntity.name);
            }
            subtitle += activity.getString(R.string.credentials_userId, Base64.encodeToString(options.userEntity.id, Base64.NO_WRAP));

            // Always null, as we allow biometric users to use PIN/Pattern to unlock
            CryptoObject cryptoObject = null;
            if (credentialSafe.biometricSigningSupported) {
                biometricSignature = null;
                PrivateKey privateKey = credentialSource.keyPair.getPrivate();
                Signature signature = WebAuthnCryptography.generateSignatureObject(privateKey);
                cryptoObject = new CryptoObject(signature);
            }

            permission = showPrompt(activity,
                                    activity.getString(R.string.credentials_makeTitle, options.rpEntity.id),
                                    subtitle, cryptoObject);
        } else {*/
            //permission = showDialog(activity,
            showDialog(activity,
                    activity.getString(R.string.credentials_makeTitle, options.rpEntity.id),
                    activity.getString(R.string.credentials_makeSubtitle, options.userEntity.name, options.userEntity.displayName, options.rpEntity.id, options.rpEntity.name));
        //}

        /*if (!permission) {
            // TODO: Check why using the original credentialSource _does not_ delete the credential
            credentialSource = credentialSafe.getCredentialSourceById(credentialSource.id);
            credentialSafe.deleteCredential(credentialSource);
            throw new CtapException(CtapError.OPERATION_DENIED);
        }*/

        // 9. Generate a new credential
        boolean genHmacSec = extensionOutput.getKeys().contains(new UnicodeString("hmac-secret"));
        PublicKeyCredentialSource credentialSource = credentialSafe.generateCredential(
                options.rpEntity.id,
                options.rpEntity.name,
                options.userEntity.id,
                options.userEntity.name,
                options.userEntity.displayName,
                genHmacSec);

        // MakeCredentialOptions steps 9 through 13
        AttestationObject attestation = makeInternalCredential(options, credentialSource,
                                                               extensionOutput, biometricSignature);
        showToast(activity, "Successfully created credentials for " +
                options.rpEntity.id, Toast.LENGTH_SHORT);

        return attestation;
    }

    /**
     * Getter for the AAGUID associated with Authenticators created from this implementation
     * @return the AAGUID as byte array
     */
    public byte[] getAaguid() { return AAGUID; }

    /**
     * Complete steps 9 through 13 of the authenticatorMakeCredential operation as described in the spec:
     * https://www.w3.org/TR/webauthn/#op-make-cred
     *
     * @param options          The options / arguments to the authenticatorMakeCredential operation.
     * @param credentialSource The handle used to lookup the keypair for this credential
     * @return an AttestationObject containing the new credential and attestation information
     * @throws VirgilException Generic error
     * @throws CtapException Error to be returned through the current transport
     */
    public AttestationObject makeInternalCredential(MakeCredentialOptions options,
                                                    PublicKeyCredentialSource credentialSource,
                                                    Map extensionOutput) throws VirgilException, CtapException {
        return makeInternalCredential(options, credentialSource, extensionOutput, null);
    }

    /**
     * The second-half of the makeCredential process
     *
     * @param options          The options / arguments to the authenticatorMakeCredential operation.
     * @param credentialSource The handle used to lookup the keypair for this credential
     * @param signature        If not null, use this pre-authorized signature object for the signing operation
     * @return an AttestationObject containing the new credential and attestation information
     * @throws VirgilException Generic error
     * @throws CtapException Error to be returned through the current transport
     */
    public AttestationObject makeInternalCredential(MakeCredentialOptions options,
                                                    PublicKeyCredentialSource credentialSource,
                                                    Map extensionOutput,
                                                    Signature signature) throws VirgilException, CtapException {

        byte[] extensionBytes;
        try {
            if (extensionOutput != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new CborEncoder(baos).encode(extensionOutput);

                extensionBytes = baos.toByteArray();
            } else {
                extensionBytes = new byte[0];
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception occurred while serializing extensions", e);
            throw new CtapException(CtapError.OTHER);
        }
        // 10. Allocate a signature counter for the new credential, initialized at 0
        // It is created and initialized to 0 during creation in step 7

        // 11. Generate attested credential data
        byte[] attestedCredentialData = constructAttestedCredentialData(credentialSource);

        // 12. Create authenticatorData byte array
        byte[] rpIdHash = WebAuthnCryptography.sha256(options.rpEntity.id); // 32 bytes
        byte[] authenticatorData = constructAuthenticatorData(rpIdHash, attestedCredentialData,
                                                  0, true, extensionBytes);
                                                    // 141 bytes + extension

        // 13. Return attestation object
        return constructAttestationObject(authenticatorData, options.clientDataHash, credentialSource, signature);
    }

    /**
     * Perform the authenticatorGetAssertion operation as defined by the WebAuthn spec: https://www.w3.org/TR/webauthn/#op-get-assertion
     *
     * @param options            The options / arguments to the authenticatorGetAssertion operation.
     * @param credentialSelector A CredentialSelector object that can, if needed, prompt the user to select a credential
     * @param activity                The Main/UI context to be used to display a biometric prompt (if required)
     * @return a record class containing the output of the authenticatorGetAssertion operation.
     * @throws CtapException Error to be returned through the current transport
     * @throws VirgilException Generic error
     */
    public GetAssertionResult getAssertion(Map options, CredentialSelector credentialSelector, FragmentActivity activity) throws CtapException, VirgilException {
        GetAssertionOptions assertionOptions = new GetAssertionOptions().fromCBor(options);
        return getAssertion(assertionOptions, credentialSelector, activity);
    }

    /**
     * Perform the authenticatorGetAssertion operation as defined by the WebAuthn spec: https://www.w3.org/TR/webauthn/#op-get-assertion
     *
     * @param options            The options / arguments to the authenticatorGetAssertion operation.
     * @param credentialSelector A CredentialSelector object that can, if needed, prompt the user to select a credential
     * @param activity                The Main/UI context to be used to display a biometric prompt (if required)
     * @return a record class containing the output of the authenticatorGetAssertion operation.
     * @throws CtapException Error to be returned through the current transport
     * @throws VirgilException Generic error
     */
    public GetAssertionResult getAssertion(GetAssertionOptions options, CredentialSelector credentialSelector, FragmentActivity activity) throws CtapException, VirgilException {

        // 0. Check if all supplied parameters are well-formed
        options.areWellFormed();
        boolean preFlight = !options.requireUserPresence;

        // 2-3. Parse allowCredentialDescriptorList
        // we do this slightly out of order, see below.

        // 4-5. Get keys that match this relying party ID
        List<PublicKeyCredentialSource> credentials = this.credentialSafe.getKeysForEntity(options.rpId);

        // 2-3. Parse allowCredentialDescriptorList
        if (options.allowCredentialDescriptorList != null && options.allowCredentialDescriptorList.size() > 0) {
            List<PublicKeyCredentialSource> filteredCredentials = new ArrayList<>();
            Set<ByteBuffer> allowedCredentialIds = new HashSet<>();
            for (PublicKeyCredentialDescriptor descriptor: options.allowCredentialDescriptorList) {
                allowedCredentialIds.add(ByteBuffer.wrap(descriptor.id));
            }

            for (PublicKeyCredentialSource credential : credentials) {
                if (allowedCredentialIds.contains(ByteBuffer.wrap(credential.id))) {
                    filteredCredentials.add(credential);
                }
            }
            credentials = filteredCredentials;
        }

        // 6. Error if none exist
        if (credentials == null || credentials.size() == 0) {
            throw new CtapException(CtapError.NO_CREDENTIALS);
        }

        // pinAuth verification
        boolean uv = PINverifyClientDataHash(options.pinAuth, options.clientDataHash,
                                             activity, false);

        // 7. Allow the user to pick a specific credential, get verification
        PublicKeyCredentialSource selectedCredential;
        if(!preFlight && selectedPreflightCredential != null) {
            selectedCredential = selectedPreflightCredential;
        } else if (credentials.size() == 1) {
            selectedCredential = credentials.get(0);
        } else {
            selectedCredential = credentialSelector.selectFrom(credentials);
            if (selectedCredential == null) {
                throw new VirgilException("User did not select credential");
            }
        }

        // Process extensions if present
        Map extensionOutput = AuthenticatorExtension.processAll(options.extensions,
                                                                authenticatorKeyAgreement,
                                                                credentialSafe,
                                                                selectedCredential);

        String txSimpleAuth;
        DataItem index = new UnicodeString("txSimpleAuth");
        if (extensionOutput.getKeys().contains(index)) {
            txSimpleAuth = activity.getString(R.string.extensions_txsimpleAuth_suffix) +
                           ((UnicodeString)extensionOutput.get(index)).getString() +
                           "\n";
        } else {
            txSimpleAuth = "";
        }

        // get verification, if necessary
        boolean permission;
        if(preFlight) {
            permission = true;
            selectedPreflightCredential = selectedCredential;
        } else {
            permission = showDialog(activity, activity.getString(R.string.request_title, options.rpId), txSimpleAuth +
                activity.getString(
                    R.string.request_subtitleDialog,
                    selectedCredential.userName,
                    selectedCredential.userDisplayName,
                    selectedCredential.rpId,
                    selectedCredential.rpName
                )
            );
        }

        if (!permission) {
            throw new CtapException(CtapError.OPERATION_DENIED);
        }

        GetAssertionResult result = getInternalAssertion(options, selectedCredential,
                    biometricSignature, uv, extensionOutput, preFlight);
        showToast(activity, "Authenticated for " + options.rpId, Toast.LENGTH_SHORT);

        return result;
    }

    /**
     * The second half of the getAssertion process
     *
     * @param options            The options / arguments to the authenticatorGetAssertion operation.
     * @param selectedCredential The credential metadata we're using for this assertion
     * @return the credential assertion
     * @throws CtapException Error to be returned through the current transport
     * @throws VirgilException Generic error
     */
    public GetAssertionResult getInternalAssertion(GetAssertionOptions options,
                                                   PublicKeyCredentialSource selectedCredential,
                                                   boolean uv,
                                                   Map extensionOutput) throws CtapException, VirgilException {
        return getInternalAssertion(options, selectedCredential, null, uv, extensionOutput, false);
    }

    /**
     * The second half of the getAssertion process
     *
     * @param options            The options / arguments to the authenticatorGetAssertion operation.
     * @param selectedCredential The credential metadata we're using for this assertion
     * @param signature          If not null, use this pre-authorized signature object for the signing operation
     * @return the credential assertion
     * @throws CtapException Error to be returned through the current transport
     */
    public GetAssertionResult getInternalAssertion(GetAssertionOptions options,
                                                   PublicKeyCredentialSource selectedCredential,
                                                   Signature signature,
                                                   boolean uv,
                                                   Map extensionOutput,
                                                   boolean preFlight)
                                                   throws CtapException {
        Log.d(TAG, "getInternalAssertion: " + (preFlight ? "preFlight" : "real"));

        byte[] authenticatorData;
        byte[] signatureBytes;
        try {
            byte[] extensionBytes;
            if (extensionOutput != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new CborEncoder(baos).encode(extensionOutput);

                extensionBytes = baos.toByteArray();
            } else {
                extensionBytes = new byte[0];
            }

            // 9. Increment signature counter
            int authCounter = (preFlight) ? 0 : credentialSafe.incrementCredentialUseCounter(selectedCredential);

            // 10. Construct authenticatorData
            byte[] rpIdHash = null;
            if (selectedCredential.u2fRpId != null && !selectedCredential.u2fRpId.equals("")) {
                if (selectedCredential.u2fRpId.equals(selectedCredential.rpId)) {
                    // Unknown source, id is already hashed
                    rpIdHash = Base64.decode(selectedCredential.u2fRpId, Base64.NO_WRAP);
                } else {
                    // Known source, hash human legible domain name
                    rpIdHash = WebAuthnCryptography.sha256(selectedCredential.u2fRpId);
                }

            } else {
                rpIdHash = WebAuthnCryptography.sha256(options.rpId); // 32 bytes
            }

            authenticatorData = constructAuthenticatorData(rpIdHash, null,
                                                           authCounter, uv, extensionBytes);

            // 11. Sign the concatentation authenticatorData || hash
            ByteBuffer byteBuffer = ByteBuffer.allocate(authenticatorData.length + options.clientDataHash.length);
            byteBuffer.put(authenticatorData);
            byteBuffer.put(options.clientDataHash);
            byte[] toSign = byteBuffer.array();
            KeyPair keyPair = preFlight ? PasswdSafeCredentialBackend.generateNewES256KeyPairLocal() : selectedCredential.keyPair;
            signatureBytes = this.cryptoProvider.performSignature(keyPair.getPrivate(), toSign, preFlight ? null : signature);

            // 12. Throw UnknownError if any error occurs while generating the assertion signature
        } catch (Exception e) {
            Log.w(TAG, "Exception occurred while generating assertion", e);
            throw new CtapException(CtapError.OTHER);
        }

        // 13. Package up the results
        return new GetAssertionResult(
                selectedCredential.id,
                authenticatorData,
                signatureBytes,
                selectedCredential.userHandle
        );
    }

    /**
     *  General ClientPIN handler
     *
     * @param options            The options / arguments to the authenticatorClientPIN operation
     * @param activity                The context to display input prompts
     * @return the ClientPIN assertion
     * @throws CtapException Error to be returned through the current transport
     * @throws VirgilException Generic error
     */
    public ClientPINResult getPinResult(Map options, FragmentActivity activity)
                                                    throws CtapException, VirgilException {
        ClientPINOptions pinOptions = new ClientPINOptions().fromCBor(options, getKeyAgreementParamenters());

        return getPinResult(pinOptions, activity);
    }

    /**
     *  General ClientPIN handler
     *
     * @param options            The options / arguments to the authenticatorClientPIN operation
     * @param activity                The context to display input prompts
     * @return the ClientPIN assertion
     * @throws CtapException Error to be returned through the current transport
     * @throws VirgilException Generic error
     */
    public ClientPINResult getPinResult(ClientPINOptions options, FragmentActivity activity)
                                                     throws CtapException, VirgilException {
        options.areWellFormed();

        ClientPINResult result;
        byte[] sharedSecret, localPinAuth, pinTokenEnc;

        switch (options.subCommand.intValue()) {
            /* getRetries */
            case 1:
                Log.d(TAG, "getPIN subcommand -> getRetries");
                result = new ClientPINResult(null, null, pinLocker.getRetries());
                break;
            /* getKeyAgreement */
            case 2:
                Log.d(TAG, "getPIN subcommand -> getKeyAgreement");
                // Send public key aG
                Pair<byte[], byte[]> point = PasswdSafeCredentialBackend.cosePointEncode(authenticatorKeyAgreement.getPublic());

                result = new ClientPINResult(point,null, null);
                break;
            /* setPIN */
            case 3:
                Log.d(TAG, "getPIN subcommand -> setPIN");
                if (pinLocker.isPinSet()) {
                    throw new CtapException(CtapError.PIN_AUTH_INVALID, "The pin has already been set");
                }

                // Generate SHA-256((abG).x) shared secret
                sharedSecret = WebAuthnCryptography.generateSharedSecret(
                                    authenticatorKeyAgreement.getPrivate(),
                                    options.keyAgreement);
                // Authenticate PIN message
                localPinAuth = WebAuthnCryptography.encodeHmacSHA256(sharedSecret,
                                                                     options.newPinEnc);
                localPinAuth = Arrays.copyOfRange(localPinAuth, 0, 16);

                if (!Arrays.equals(options.pinAuth, localPinAuth)) {
                    throw new CtapException(CtapError.PIN_AUTH_INVALID,
                                            "Received and calculated pinAuth don't match");
                }

                PINdecryptAndStore(sharedSecret, options.newPinEnc);

                result = new ClientPINResult(null, null,
                                                          null);
                break;
            /* changePIN */
            case 4:
                Log.d(TAG, "getPIN subcommand -> changePIN");
                if (options.pinHashEnc == null || options.newPinEnc == null ||
                    options.pinAuth == null || options.keyAgreement == null) {
                    throw new CtapException(CtapError.MISSING_PARAMETER,
                                            "Change PIN missing required parameters");
                }

                if (pinLocker.getRetries() == 0) {
                    throw new CtapException(CtapError.PIN_BLOCKED,
                                            "No more PIN enter retries! PIN Blocked!");
                }

                sharedSecret = PINsharedSecret(options.keyAgreement);

                // Authenticate newPIN message
                localPinAuth = new byte[options.newPinEnc.length + options.pinHashEnc.length];
                System.arraycopy(options.newPinEnc, 0, localPinAuth, 0,
                                 options.newPinEnc.length);
                System.arraycopy(options.pinHashEnc, 0, localPinAuth,
                                 options.newPinEnc.length, options.pinHashEnc.length);

                localPinAuth = WebAuthnCryptography.encodeHmacSHA256(sharedSecret, localPinAuth);
                localPinAuth = Arrays.copyOfRange(localPinAuth, 0, 16);

                if (!Arrays.equals(options.pinAuth, localPinAuth)) {
                    throw new CtapException(CtapError.PIN_AUTH_INVALID,
                                            "Received and calculated pinAuth don't match");
                }

                PINverifyHashEnc(sharedSecret, options.pinHashEnc, activity);

                PINdecryptAndStore(sharedSecret, options.newPinEnc);

                result = new ClientPINResult(null,null,
                                                          null);

                break;
            /* getPINToken */
            case 5:
                Log.d(TAG, "getPIN subcommand -> getPINToken");
                if (options.keyAgreement == null || options.pinHashEnc == null) {
                    throw new CtapException(CtapError.MISSING_PARAMETER,
                                            "Change PIN missing required parameters");
                }
                if (pinLocker.getRetries() == 0) {
                    throw new CtapException(CtapError.PIN_BLOCKED,
                                            "No more PIN enter retries! PIN Blocked!");
                }

                sharedSecret = PINsharedSecret(options.keyAgreement);

                PINverifyHashEnc(sharedSecret, options.pinHashEnc, activity);

                pinTokenEnc = WebAuthnCryptography.encryptAES256_CBC(sharedSecret,
                                                                     pinLocker.getToken());

                result = new ClientPINResult(null, pinTokenEnc,null);
                break;

            default:
                throw new UnknownError();
        }

        return result;
    }

    /*
     * U2F Commands
     */
    private byte[] U2F_credId;
    public Response registerRequest(RawMessages.RegistrationRequest req,
                                    FragmentActivity activity) throws ApduException {
        boolean isDummy = false;
        if (KnownFacets.isDummyRequest(req.application, req.challenge)) {
            isDummy = true;
        }

        PublicKeyCredentialSource credentialSource;
        KeyPair keyPair;
        byte[] serializedKey, attestationCert;

        Pair<String, String> rpId = KnownFacets.resolveAppIdHash(req.application);
        if (rpId == null) {
            rpId = new Pair<>(Base64.encodeToString(req.application, Base64.NO_WRAP),
                              Base64.encodeToString(req.application, Base64.NO_WRAP));
        }

        try {
            credentialSource = credentialSafe.generateCredential(
                    rpId.first,                 // Save rpId in Webauthn query format
                    "U2F Server",
                    rpId.second);

            keyPair = credentialSource.keyPair;
            serializedKey = WebAuthnCryptography.serializePublicKey((ECPublicKey)keyPair.getPublic());
        } catch (VirgilException e) {
            Log.e(TAG, "Error in credential creation");
            e.printStackTrace();
            throw new ApduException(StatusWord.MEMORY_FAILURE);
        }

        byte[] u2fAuthData = new byte[0];
        try {
            u2fAuthData = U2fSelfAttestation.packU2fAuthData(
                    req.application,
                    req.challenge,
                    credentialSource.id,
                    serializedKey
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        U2fSelfAttestation attestationObj;
        try {
            attestationObj = new U2fSelfAttestation(u2fAuthData, this.cryptoProvider, credentialSource);
        } catch (VirgilException e) {
            e.printStackTrace();
            throw new ApduException(StatusWord.COMMAND_ABORTED);
        }

        if (isDummy) {
            // TODO: Check why using the original credentialSource _does not_ delete the credential
            //Delete dummy credentials
            credentialSource = credentialSafe.getCredentialSourceById(credentialSource.id);
            credentialSafe.deleteCredential(credentialSource);
        } else {
            U2F_credId = credentialSource.id;
            WioBiometricPrompt.registerCallback(new PromptCallback(true) {
                @Override
                public void onResult(boolean result, CryptoObject cryptoObject) {
                    if (!result && U2F_credId != null) {
                        PublicKeyCredentialSource credentialSource;
                        credentialSource = credentialSafe.getCredentialSourceById(U2F_credId);
                        credentialSafe.deleteCredential(credentialSource);
                    }

                    U2F_credId = null;
                }
            });
        }

        return new RawMessages.RegistrationResponse(
            serializedKey,
            credentialSource.id,
            Objects.requireNonNull(attestationObj.getCertificate()),
            Objects.requireNonNull(attestationObj.getSignature()),
            !isDummy //Return user verification "not dummy"
        );
    }

    public Response authenticationRequest(RawMessages.AuthenticationRequest req,
                                          FragmentActivity activity) throws ApduException {

        PublicKeyCredentialSource credentialSource = credentialSafe.getCredentialSourceById(
                                                                            req.keyHandle);

        boolean userVerification;
        if (KnownFacets.isDummyRequest(req.application, req.challenge)) {
            userVerification = false;
        }
        
        GetAssertionResult assertion;
        GetAssertionOptions options = new GetAssertionOptions();
        options.clientDataHash = req.challenge;

        try {
            switch (req.controlByte) {
                case CHECK_ONLY:
                    throw new ApduException(StatusWord.CONDITIONS_NOT_SATISFIED);
                case ENFORCE_USER_PRESENCE_AND_SIGN:
                    userVerification = true;
                    assertion = getInternalAssertion(options, credentialSource, false, null);
                    break;
                case DONT_ENFORCE_USER_PRESENCE_AND_SIGN:
                    //TODO: Change flag to "show" non present user
                    userVerification = false;
                    assertion = getInternalAssertion(options, credentialSource, false, null);
                    break;
                default:
                    throw new ApduException(StatusWord.INCORRECT_PARAMETERS);
            }
        } catch (VirgilException | CtapException e) {
            e.printStackTrace();
            throw new ApduException(StatusWord.MEMORY_FAILURE);
        }
        
        ByteBuffer buff = ByteBuffer.allocate(5 + assertion.signature.length);
        //Skip AGUUID and only take flags and key counter
        buff.put(Arrays.copyOfRange(assertion.authenticatorData, 32, 37));
        buff.put(assertion.signature);

        return new RawMessages.AuthenticationResponse(buff.array(), userVerification);
    }

    public boolean U2FuserPresence(FragmentActivity activity) {
        return showPrompt(activity,
                            activity.getString(R.string.u2f_registerTitle),
                            activity.getString(R.string.u2f_registerSubtitle),
                            null);
    }

    public void resetPin(FragmentActivity activity, PromptCallback callback) {

        WioBiometricPrompt.registerCallback(callback);
        WioBiometricPrompt.registerCallback(new PromptCallback(true) {
            @Override
            public void onResult(boolean result, CryptoObject cryptoObject) {
                if (result) {
                    pinLocker.resetPinLocker();
                }
            }
        });

        new WioBiometricPrompt(activity,
                                activity.getString(R.string.pin_resetTitle),
                                activity.getString(R.string.pin_resetSubtitle),
                                true);
    }

    public void resetAuthenticator(FragmentActivity activity) throws CtapException {
        boolean permission = showPrompt(activity,
                                        activity.getString(R.string.reset_requestTitle),
                                        activity.getString(R.string.reset_requestSubtitle),
                                        null);

        if (permission) {
            credentialSafe.deleteAllCredentials();
            pinLocker.resetPinLocker();
        } else {
            throw new CtapException(CtapError.OPERATION_DENIED);
        }
    }

    public Boolean credentialsInHardware() {
        return credentialSafe.credentialsInHardware();
    }

    public void cancelBiometricPrompt() {
        ListIterator iter = biometricPrompts.listIterator();
        while(iter.hasNext()) {
            WioBiometricPrompt bioPrompt = (WioBiometricPrompt)iter.next();
            if (bioPrompt == null) {
                iter.remove();
                continue;
            }
            iter.remove();
            bioPrompt.cancelAuthentication();
        }

        iter = requestDialogs.listIterator();
        while (iter.hasNext()) {
            WioRequestDialog requestDialog = (WioRequestDialog)iter.next();
            if (requestDialog == null) {
                iter.remove();
                continue;
            }
            iter.remove();
            requestDialog.dismiss();
        }
    }

    public void deleteAllCredentials(FragmentActivity activity, PromptCallback callback) {


        WioBiometricPrompt.registerCallback(callback);
        WioBiometricPrompt.registerCallback(new PromptCallback(true) {
            @Override
            public void onResult(boolean result, CryptoObject cryptoObject) {
                if (result) {
                    credentialSafe.deleteAllCredentials();
                }
            }
        });

        new WioBiometricPrompt(activity,
                                activity.getString(R.string.credentials_deleteAllTitle),
                                activity.getString(R.string.credentials_deleteAllSubtitle),
                                true,
                                null);

    }

     void deleteCredential(FragmentActivity activity,
                                 PublicKeyCredentialSource credentialSource) throws CtapException {
        String displayName;
        if (credentialSource.userDisplayName != null) {
            displayName = credentialSource.userDisplayName;
        } else {
            displayName = Arrays.toString(credentialSource.id);
        }
        boolean permission = showPrompt(activity,
                                        activity.getString(R.string.credentials_deleteTitle),
                                        activity.getString(R.string.credentials_deleteSubtitle,
                                                displayName, credentialSource.rpId),
                                        null);
        if (permission) {
            credentialSafe.deleteCredential(credentialSource);
        } else {
            throw new CtapException(CtapError.OPERATION_DENIED);
        }
    }

    public void deleteCredential(FragmentActivity activity,
                                 PublicKeyCredentialSource credentialSource,
                                 PromptCallback callback) {
        String displayName;
        if (credentialSource.userDisplayName != null) {
            displayName = credentialSource.userDisplayName;
        } else {
            displayName = Base64.encodeToString(credentialSource.userHandle, Base64.NO_WRAP);
            if (displayName.length() > 16) {
                displayName = displayName.substring(0, 16);
            }
        }

        WioBiometricPrompt.registerCallback(callback);
        WioBiometricPrompt.registerCallback(new PromptCallback(true) {
            @Override
            public void onResult(boolean result, CryptoObject cryptoObject) {
                if (result) {
                    credentialSafe.deleteCredential(credentialSource);
                }
            }
        });

        new WioBiometricPrompt(activity,
                                activity.getString(R.string.credentials_deleteTitle),
                                activity.getString(R.string.credentials_deleteSubtitle,
                                        displayName, credentialSource.rpId),
                          true, null);
    }

    public ECParameterSpec getKeyAgreementParamenters() {
        if (authenticatorKeySpec == null) {
            authenticatorKeySpec = ((ECPublicKey)authenticatorKeyAgreement.getPublic()).getParams();
        }
        return authenticatorKeySpec;
    }

    public List<PublicKeyCredentialSource> getAllCredentials() {
        return credentialSafe.getAllCredentials();
    }

    /**
     * Construct an attestedCredentialData object per the WebAuthn spec: https://www.w3.org/TR/webauthn/#sec-attested-credential-data
     *
     * @param credentialSource the PublicKeyCredentialSource associated with this credential
     * @return a byte array following the attestedCredentialData format from the WebAuthn spec
     * @throws VirgilException Generic error
     */
    private byte[] constructAttestedCredentialData(PublicKeyCredentialSource credentialSource) throws VirgilException {
        // | AAGUID | L | credentialId | credentialPublicKey |
        // |   16   | 2 |      32      |          n          |
        // total size: 50+n
        KeyPair keyPair = credentialSource.keyPair;
        byte[] encodedPublicKey = PasswdSafeCredentialBackend.coseEncodePublicKey(keyPair.getPublic());

        ByteBuffer credentialData = ByteBuffer.allocate(16 + 2 + credentialSource.id.length + encodedPublicKey.length);

        // AAGUID will be 16 bytes of zeroes
        assert 16 == AAGUID.length;
        credentialData.put(AAGUID);
        credentialData.position(16);
        credentialData.putShort((short) credentialSource.id.length); // L
        credentialData.put(credentialSource.id); // credentialId
        credentialData.put(encodedPublicKey);
        return credentialData.array();
    }

    /**
     * Construct an authenticatorData object per the WebAuthn spec: https://www.w3.org/TR/webauthn/#sec-authenticator-data
     *
     * @param rpIdHash               the SHA-256 hash of the rpId
     * @param attestedCredentialData byte array containing the attested credential data
     * @return a byte array that matches the authenticatorData format
     */
    private byte[] constructAuthenticatorData(byte[] rpIdHash, byte[] attestedCredentialData,
                                              int authCounter, boolean uv,
                                              byte[] extensions) {

        boolean validExtensions = extensions != null && extensions.length > 1;

        byte flags = 0x00;
        flags |= 0x01; // user present
        if (uv) {
            flags |= (0x01 << 2); // user verified
        }
        if (attestedCredentialData != null) {
            flags |= (0x01 << 6); // attested credential data included
        }
        if (validExtensions) {
            flags |= (0x01 << 7); // extension data included
        }

        // 32-byte hash + 1-byte flags + 4 bytes signCount = 37 bytes
        ByteBuffer authData = ByteBuffer.allocate(37 +
                (attestedCredentialData == null ? 0 : attestedCredentialData.length) +
                (validExtensions ? extensions.length : 0));

        authData.put(rpIdHash);
        authData.put(flags);
        authData.putInt(authCounter);
        if (attestedCredentialData != null) {
            authData.put(attestedCredentialData);
        }
        if (validExtensions) {
            authData.put(extensions);
        }
        return authData.array();
    }

    /**
     * Construct an AttestationObject per the WebAuthn spec: https://www.w3.org/TR/webauthn/#generating-an-attestation-object
     * We use either packed self-attestation or "none" attestation: https://www.w3.org/TR/webauthn/#attestation-formats
     * The signing procedure is documented here under `Signing Procedure`->4. : https://www.w3.org/TR/webauthn/#packed-attestation
     *
     * @param authenticatorData byte array containing the raw authenticatorData object
     * @param clientDataHash    byte array containing the sha256 hash of the client data object (request type, challenge, origin)
     * @param credentialSource  alias to lookup the key pair to be used to sign the attestation object
     * @return a well-formed AttestationObject structure
     */
    @SuppressLint("Assert")
    private AttestationObject constructAttestationObject(byte[] authenticatorData, byte[] clientDataHash, PublicKeyCredentialSource credentialSource, Signature signature) {
        // Our goal in this function is primarily to create a signature over the relevant data fields
        // From https://www.w3.org/TR/webauthn/#packed-attestation we can see that for self-signed attestation,
        // `sig` is generated by signing the concatenation of authenticatorData and clientDataHash
        // Once we have constructed `sig`, we create a new AttestationObject to contain the
        // authenticatorData and `sig`.
        // The AttestationObject has a .asCBOR() method that will properly construct the full,
        // encoded attestation object in a format that can be returned to the client/relying party
        // (shown in Figure 5 of the webauthn spec)

        // Concatenate authenticatorData so we can sign them.
        // "If self attestation is in use, the authenticator produces sig by concatenating
        // authenticatorData and clientDataHash, and signing the result using the credential
        // private key."
        ByteBuffer byteBuffer = ByteBuffer.allocate(clientDataHash.length + authenticatorData.length);
        byteBuffer.put(authenticatorData);
        byteBuffer.put(clientDataHash);
        byte[] toSign = byteBuffer.array();

//        // for testing purposes during development, make a sanity check that the authenticatorData and clientDataHash are the fixed lengths we expect
//        assert toSign.length == 141 + 32;

        // construct our attestation object (attestationObject.asCBOR() can be used to generate the raw object in calling function)
        // TODO: Discuss tradeoffs wrt none / packed attestation formats. Switching to none here because packed lacks support.

        // Self-attestation: grab our keypair for this credential
        AttestationObject attestationObject;
        KeyPair keyPair = credentialSource.keyPair;
        try {
            byte[] signatureBytes = this.cryptoProvider.performSignature(keyPair.getPrivate(), toSign, signature);
            attestationObject = new PackedSelfAttestation(authenticatorData, signatureBytes);
        } catch (VirgilException e) {
            Log.d(TAG, "Failed to create self-attestation statement - defaulting to NONE" + e);
            e.printStackTrace();
            attestationObject = new NoneAttestation(authenticatorData);
        }

        // Basic-attestation: leave it handle its own stuff
        /*
        AttestationObject attestationObject;
        try {
            attestationObject = new PackedBasicAttestation(authenticatorData, this.cryptoProvider, toSign, signature);
        } catch (VirgilException | NullPointerException e) {
            Log.d(TAG, "Failed to create basic attestation statement - defaulting to NONE" + e);
            e.printStackTrace();
            attestationObject = new NoneAttestation(authenticatorData);
        }
        */

        return attestationObject;
    }

    private byte[] PINsharedSecret(PublicKey keyAgreement) throws VirgilException {
        // Generate SHA-256((abG).x) shared secret
        return WebAuthnCryptography.generateSharedSecret(authenticatorKeyAgreement.getPrivate(),
                                                         keyAgreement);
    }

    /**
     * Verify PIN hash as per CTAP2 specification https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#gettingPinToken
     *
     * @param sharedSecret pin shared secret used to encrypt new pin
     * @param pinHashEnc encrypted utf8-encoded new pin
     * @throws VirgilException Generic error
     */
    private void PINverifyHashEnc(byte[] sharedSecret, byte[] pinHashEnc, FragmentActivity activity)
                                                            throws VirgilException, CtapException {
        // Verify pinHashEnc matches stored values
        byte[] compPinSHA = WebAuthnCryptography.decryptAES256_CBC(sharedSecret, pinHashEnc);

        if (!pinLocker.isPinMatch(compPinSHA)) {
            try {
                authenticatorKeyAgreement = this.credentialSafe.keyAgreementPair();
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                throw new VirgilException("Failed to create authenticator agreement key pair", e);
            }

            pinLocker.decrementPinRetries();

            if (pinLocker.getRetries() == 0) {
                throw new CtapException(CtapError.PIN_BLOCKED);
            }

            conPINmismatches++;
            if (conPINmismatches == 3) {
                showPrompt(activity,
                            activity.getString(R.string.pin_missed3Title),
                            activity.getString(R.string.pin_missed3Subtitle),
                            null);
                throw new CtapException(CtapError.PIN_AUTH_BLOCKED,
                        "3 consecutive PIN missmatches, authentication blocked! do a power cycle");
            } else {
                throw new CtapException(CtapError.PIN_INVALID);
            }
        } else {
            conPINmismatches = 0;
            pinLocker.setRetries(8L);
        }
    }

    /**
     *
     * @param pinAuth lower 16 bytes of hmac-sha256 (clientDataHash)
     * @param clientDataHash 32 byte client data hash
     * @throws VirgilException Generic error
     * @throws CtapException Error to be returned through the current transport
     */
    private boolean PINverifyClientDataHash(byte[] pinAuth, byte[] clientDataHash, FragmentActivity activity,
                                            boolean pinRequired) throws VirgilException, CtapException {
        if (pinAuth != null) {
            if (pinAuth.length == 0) {
                showPrompt(activity,
                            activity.getString(R.string.pin_nullPinTitle),
                            activity.getString(R.string.pin_nullPinSubtitle),
                            null);
                if (!pinLocker.isPinSet()) {
                    throw new CtapException(CtapError.PIN_NOT_SET);
                } else {
                    throw new CtapException(CtapError.PIN_INVALID);
                }
            }

            byte[] localPinAuth = WebAuthnCryptography.encodeHmacSHA256(pinLocker.getToken(),
                                                                        clientDataHash);
            localPinAuth = Arrays.copyOf(localPinAuth, 16);

            if (Arrays.equals(pinAuth, localPinAuth)) {
                conPINmismatches = 0;
                pinLocker.setRetries(8L);
                return true;
            }

            pinLocker.decrementPinRetries();
            if (pinLocker.getRetries() == 0) {
                throw new CtapException(CtapError.PIN_BLOCKED, "No more PIN retries!");
            }

            conPINmismatches++;
            if (conPINmismatches == 3) {
                showPrompt(activity,
                            activity.getString(R.string.pin_missed3Title),
                            activity.getString(R.string.pin_missed3Subtitle),
                            null);
                throw new CtapException(CtapError.PIN_AUTH_BLOCKED,
                                        "3 consecutive PIN missmatches, authentication blocked!" +
                                        "do a power cycle");
            } else {
                throw new CtapException(CtapError.PIN_AUTH_INVALID, "Wrong pin!");
            }
        }

        if (pinLocker.isPinSet()) {
            if (pinRequired)
                throw new CtapException(CtapError.PIN_REQUIRED);
            return false;
        }

        return true;
    }

    public boolean isPinSet() {
        return pinLocker.isPinSet();
    }

    /**
     * Method for managing internally (non-client CTAP command-triggered)
     * the change PIN operation of the authenticator ClientPin.
     *
     * @param activity FragmentActivity to run the biometric verification prompt attached to changing the PIN
     * @param newPin New PIN to set
     * @param oldPin Old PIN
     */
    public void selfSetPin(FragmentActivity activity, String newPin, @Nullable String oldPin, @Nullable PromptCallback callback) {
        if (activity != null && newPin != null && newPin.length() >= 4) {

            new Thread(() -> {
                 boolean acquiredPermission = showPrompt(activity,
                         activity.getString(R.string.pin_changeTitle),
                         activity.getString(R.string.pin_changeSubtitle),
                         null);
                 boolean isPinUpdated = false;
                 if (acquiredPermission) {
                     if (pinLocker.isPinSet()) {
                         // When PIN is set we must check the old PIN accordingly
                         if (oldPin == null || pinLocker.getRetries() == 0) {
                             // Invalid PIN or PIN is blocked
                             // TODO: Implement event messaging on completion operations for better UI display
                             if (callback != null) activity.runOnUiThread(() -> callback.onResult(false, null));
                             return;
                         }

                         try {
                             if (pinLocker.isPinMatch(Arrays.copyOf(WebAuthnCryptography.sha256(oldPin.getBytes()), 16))) {
                                 isPinUpdated = pinLocker.setRetries(8L)
                                         .lockPin(Arrays.copyOf(WebAuthnCryptography.sha256(newPin.getBytes()), 16));
                             }
                             pinLocker.decrementPinRetries();
                         } catch (VirgilException e) {
                             // Nothing to do here - just catch this exception for graceful handling
                         }
                     } else {
                         try {
                             isPinUpdated = pinLocker.setRetries(8L)
                                     .lockPin(Arrays.copyOf(WebAuthnCryptography.sha256(newPin.getBytes()), 16));
                         } catch (VirgilException e) {
                             // Nothing to do here - just catch this exception for graceful handling
                         }
                     }
                 }
                 if (callback != null) {
                     boolean finalIsPinUpdated = isPinUpdated;
                     // TODO: Implement event messaging on completion operations for better UI display
                     activity.runOnUiThread(() -> callback.onResult(finalIsPinUpdated, null));
                 }
            }).start();
        }
    }

    /**
     * Decrypt and store a new PIN per the CTAP2 spec https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#gettingPinToken
     *
     * @param sharedSecret pin shared secret used to encrypt new pin
     * @param newPinEnc encrypted new pin
     * @throws VirgilException Generic error
     */
    private void PINdecryptAndStore(byte[] sharedSecret, byte[] newPinEnc) throws VirgilException,
                                                                                  CtapException {
        byte[] newPin = WebAuthnCryptography.decryptAES256_CBC(sharedSecret, newPinEnc);

        int idx;
        for (idx = 0 ; idx < newPin.length ; ++idx)
            if (newPin[idx] == 0x00)
                break;

        try {
            newPin = Arrays.copyOf(newPin, idx);
            String pinString = charsetDec.decode(ByteBuffer.wrap(newPin)).toString();

            if (pinString.length() < 4) {
                throw new CtapException(CtapError.PIN_POLICY_VIOLATION,
                                        "newPin char length is shorter than 4");
            }

            pinLocker.setRetries(8L);
            pinLocker.lockPin(Arrays.copyOf(WebAuthnCryptography.sha256(newPin), 16));

        } catch (CharacterCodingException e) {
            throw new CtapException(CtapError.PIN_INVALID, "newPin could not be decoded!");
        }
    }

    private Signature biometricSignature;
    private final List<WioBiometricPrompt> biometricPrompts = new ArrayList<>();
    private final List<WioRequestDialog> requestDialogs = new ArrayList<>();

    private boolean showPrompt(FragmentActivity fragmentActivity, String title, String subtitle,
                               CryptoObject cryptoObject) {

        final boolean[] res = new boolean[1];
        final WioBiometricPrompt[] prompt = new WioBiometricPrompt[1];
        final Semaphore sem = new Semaphore(0);

        WioBiometricPrompt.registerCallback(new PromptCallback(true) {
            @Override
            public void onResult(boolean result, CryptoObject cryptoObject) {
                sem.release();
                res[0] = result;
                if (cryptoObject != null) {
                    biometricSignature = cryptoObject.getSignature();
                }
                biometricPrompts.remove(prompt[0]);
                processing();
            }
        });

        upNeeded();
        // Using array to declare a final reference before making a new biometric prompt
        prompt[0] = new WioBiometricPrompt(fragmentActivity, title, subtitle,
                                           true, cryptoObject);
        biometricPrompts.add(prompt[0]);
        try {
            sem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        // Using array to have a final reference of a primitive type
        return res[0];
    }

    private boolean showDialog(FragmentActivity fragmentActivity, String title, String message) {
        final boolean[] res = new boolean[1];
        final WioRequestDialog[] prompt = new WioRequestDialog[1];
        final Semaphore sem = new Semaphore(0);

        WioRequestDialog.PromptCallback callback = new WioRequestDialog.PromptCallback() {
            @Override
            public void onResult(boolean result) {
                sem.release();
                res[0] = result;
                requestDialogs.remove(prompt[0]);
                processing();
            }
        };

        upNeeded();
        prompt[0] = WioRequestDialog.create(title, message, callback);
        prompt[0].show(fragmentActivity);
        requestDialogs.add(prompt[0]);

        try {
            sem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        // Using array to have a final reference of a primitive type
        return res[0];
    }

    private void showToast(FragmentActivity activity, String msg, int duration) {
        Handler mainHandler = new Handler(activity.getMainLooper());

        mainHandler.post(() -> Toast.makeText(activity, msg, duration).show());
    }
}
