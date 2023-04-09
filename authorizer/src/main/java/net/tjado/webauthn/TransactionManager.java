package net.tjado.webauthn;

import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnsignedInteger;
import net.tjado.webauthn.exceptions.ApduException;
import net.tjado.webauthn.exceptions.CtapException;
import net.tjado.webauthn.exceptions.CtapHidException;
import net.tjado.webauthn.exceptions.VirgilException;
import net.tjado.webauthn.fido.CommandApdu;
import net.tjado.webauthn.fido.ResponseApdu;
import net.tjado.webauthn.fido.ctap2.Messages;
import net.tjado.webauthn.fido.hid.Constants;
import net.tjado.webauthn.fido.hid.Framing;
import net.tjado.webauthn.fido.u2f.RawMessages;
import net.tjado.webauthn.util.SelectCredentialDialogFragment;

public class TransactionManager {

    private final String TAG = "TransactionManager";

    private FragmentActivity activity;
    private Authenticator authenticator;

    private Framing.InMessage message = null;
    private CompletableFuture<Boolean> activeCborJob = null;
    private int freeChannelId = 1;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());

    public TransactionManager(FragmentActivity activity, Authenticator authenticator) {
        this.activity = activity;
        this.authenticator = authenticator;
    }

    private Framing.U2fAuthnListener u2fAuthnListener = null;
    private Framing.WebAuthnListener webAuthnListener = null;

    private void startTimeout(Framing.SubmitReports submit) {
        timeoutHandler.postDelayed(() -> {
            if (message != null) {
                submit.submit(new Framing.ErrorResponse(
                    message.channelId,
                    CtapHidException.CtapHidError.MsgTimeout
                ).toRawReports());
            }
            message = null;
        }, Constants.HID_CONT_TIMEOUT_MS);
    }

    private void haltTimeout() {
        timeoutHandler.removeCallbacksAndMessages(null);
    }

    private void resetTransaction() {
        haltTimeout();
        message = null;
        activeCborJob = null;
    }

    private void handleError(CtapHidException hidException, Framing.SubmitReports submit) {
        Log.w(TAG, "HID error: " + hidException.error.code);

        if (message != null && hidException.channelId != null) {
            int channelId = message.channelId;
            if (message.channelId == hidException.channelId) {
                resetTransaction();
            }
            submit.submit(new Framing.ErrorResponse(channelId  , hidException.error).toRawReports());
        }
    }

    private static class U2fContinuation {
        final Framing.InMessage message;
        final CompletableFuture<Boolean> confirmationRequest;
        final RawMessages.Response cont;

        U2fContinuation(Framing.InMessage message, CompletableFuture<Boolean>confirmationRequest, RawMessages.Response cont) {
            this.message = message;
            this.confirmationRequest = confirmationRequest;
            this.cont = cont;
        }
    }

    private U2fContinuation activeU2fConfirmation = null;
    private final Handler u2fRetryTimeoutHandler = new Handler(Looper.getMainLooper());
    private void rearmU2fRetryTimeout() {
        haltU2fRetryTimeout();
        u2fRetryTimeoutHandler.postDelayed(() -> {
            Log.w(TAG, "Request requiring user confirmation timed out");
//            resetU2fContinuation();
        }, Constants.HID_MSG_TIMEOUT_MS);
    }

    private void haltU2fRetryTimeout() {
        u2fRetryTimeoutHandler.removeCallbacksAndMessages(null);
    }

    private void resetU2fContinuation() {
        haltU2fRetryTimeout();
        if (activeU2fConfirmation != null && activeU2fConfirmation.confirmationRequest != null) {
            activeU2fConfirmation.confirmationRequest.cancel(true);
        }
        activeU2fConfirmation = null;
    }

    private boolean handleMessageIfComplete(Framing.SubmitReports submit) throws CtapHidException {
        if (message == null) return false;
        byte[] payload = message.getPayloadIfComplete();

        if (payload == null) return false;
        Log.i(TAG, "Handling complete " + message.cmd + " message");

        switch (message.cmd) {
            case Ping:
                submit.submit(new Framing.PingResponse(message.channelId, payload).toRawReports());
                break;
            case Msg:
                // We have an active user confirmation request. If the current message
                // is just a retry of the message that initiated the confirmation,
                // we reply with the status, otherwise we cancel the confirmation
                // request.
                if (activeU2fConfirmation != null) {
                    if (message.equals(activeU2fConfirmation.message)) {
                        if (!activeU2fConfirmation.confirmationRequest.isDone()) { // isActive
                            // Still waiting for user confirmation; let the client retry.
                            rearmU2fRetryTimeout();
                            submit.submit(new Framing.MsgResponse(
                                    message.channelId,
                                    ApduException.StatusWord.CONDITIONS_NOT_SATISFIED.value
                            ).toRawReports());
                        } else if (activeU2fConfirmation.confirmationRequest.isCancelled()) {
                            Log.w(TAG, "Confirmation request already cancelled");
                            resetU2fContinuation();
                        } else {
                            byte[] resposePayload;
                            boolean userAccepted;
                            try {
                                userAccepted = activeU2fConfirmation.confirmationRequest.get();
                            } catch (InterruptedException | ExecutionException e) {
                                userAccepted = false;
                            }

                            if (userAccepted) {
                                try {
                                    RawMessages.Response u2fResponse = activeU2fConfirmation.cont;
                                    // Call the callbacks here on the reply path
                                    // to avoid multiple triggers
                                    if (u2fResponse instanceof  RawMessages.RegistrationResponse) {
                                        if (u2fAuthnListener != null) u2fAuthnListener.onRegistrationResponse();
                                    } else if (u2fResponse instanceof RawMessages.AuthenticationResponse) {
                                        if (u2fAuthnListener != null) u2fAuthnListener.onAuthenticationResponse();
                                    } else {
                                        // do nothing
                                    }
                                    ResponseApdu u2fResponseApdu = new ResponseApdu(
                                            u2fResponse.data,
                                            u2fResponse.statusWord);
                                    resposePayload = u2fResponseApdu.next();
                                } catch (ApduException e) {
                                    Log.w(TAG,"Continued transaction failed with status" +
                                            Arrays.toString(e.getStatusWord()));
                                    resposePayload = e.getStatusWord();
                                }
                                submit.submit(
                                        new Framing.MsgResponse(
                                                message.channelId,
                                                resposePayload
                                        ).toRawReports());
                            }
                            resetU2fContinuation();
                        }
                        resetTransaction();
                        return true;
                    }
                    Log.i(TAG, "Received new message, cancelling user confirmation");
                    resetU2fContinuation();
                    // Fall through to usual message handling
                }

                byte[] responsePayload;
                try {
                    CommandApdu u2fRequestApdu = new CommandApdu(payload);
                    RawMessages.RequestU2F u2fRequest = RawMessages.parseU2Frequest(u2fRequestApdu);
                    RawMessages.Response u2fResponse = handleU2F(activity, u2fRequest);

                    if (!u2fResponse.userVerification) {
                        // No user presence check needed, continue right away
                        ResponseApdu u2fResponseApdu = new ResponseApdu(
                                u2fResponse.data,
                                u2fResponse.statusWord);
                        responsePayload = u2fResponseApdu.next();
                    } else {
                        // User presence check required; confirm asynchronously and return
                        // CONDITIONS_NOT_SATISFIED while waiting.
                        activeU2fConfirmation = new U2fContinuation(
                                message,
                                CompletableFuture.supplyAsync(() -> authenticator.U2FuserPresence(activity)),
                                u2fResponse);
                        rearmU2fRetryTimeout();
                        Log.i(TAG, "User confirmation required; expecting client to retry");
                        responsePayload = ApduException.StatusWord.CONDITIONS_NOT_SATISFIED.value;
                    }
                } catch (ApduException e) {
                    if (Arrays.equals(payload, Constants.U2F_LEGACY_VERSION_COMMAND_APDU)) {
                        responsePayload = Constants.U2F_LEGACY_VERSION_COMMAND_APDU;
                    } else {
                        byte[] payloadHeader = Arrays.copyOfRange(payload, 0, Math.min(payload.length, 4));
                        Log.w(TAG, "Transaction failed with status " +
                                Arrays.toString(e.getStatusWord()) +
                                ", request header was " + Arrays.toString(payloadHeader));
                        responsePayload = e.getStatusWord();
                    }
                }
                submit.submit(new Framing.MsgResponse(message.channelId, responsePayload).toRawReports());
                break;
            case Cbor:
                activeCborJob = CompletableFuture.supplyAsync(() -> {
                    CancellationSignal signal = new CancellationSignal();
                    try {
                        final CompletableFuture<Boolean> keepaliveJob = CompletableFuture.supplyAsync(
                                () -> {
                                    while (true) {
                                        try {
                                            Thread.sleep(Constants.HID_KEEPALIVE_INTERVAL_MS);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            continue;
                                        }
                                        Constants.CtapHidStatus status = Constants.CtapHidStatus.IDLE;
                                        switch (authenticator.getInternalStatus()) {
                                            case PROCESSING:
                                                status = Constants.CtapHidStatus.PROCESSING;
                                                break;
                                            case WAITING_FOR_UP:
                                                status = Constants.CtapHidStatus.UPNEEDED;
                                                break;
                                        }

                                        if (signal.isCanceled()) {
                                            return false;
                                        }
                                        submit.submit(new Framing.KeepaliveResponse(
                                                message.channelId,
                                                status).toRawReports()
                                        );
                                    }
                                }
                        , Executors.newSingleThreadExecutor());

                        byte[] ctapResponsePayload = handleCTAP2(activity, payload);
                        signal.cancel();
                        keepaliveJob.cancel(true);
                        submit.submit(
                                new Framing.CborResponse(message.channelId,
                                        ctapResponsePayload
                                ).toRawReports());
                    } catch (CancellationException e) {
                        submit.submit(
                                new Framing.CborResponse(message.channelId,
                                        new byte[] {CtapException.CtapError.KEEP_ALIVE_CANCEL.value}
                                ).toRawReports());
                    } finally {
                        resetTransaction();
                    }
                    return true; //Dummy CompletableFuture return
                });
                // Return early since we do not want to reset the transaction yet
                return true;
            case Init:
                throw new IllegalStateException("Init message should never make it to handleMessage");
            default:
                throw new CtapHidException(CtapHidException.CtapHidError.InvalidCmd, message.channelId);
        }
        resetTransaction();
        return true;
    }

    public void handleReport(byte[] bytes, Framing.SubmitReports submit) {
        try {
            Framing.Packet packet = Framing.Packet.parse(bytes);
            if (packet.channelId == 0) {
                throw new CtapHidException(CtapHidException.CtapHidError.InvalidCmd, packet.channelId);
            }
            if (packet instanceof Framing.InitPacket) {
                Framing.InitPacket initPacket = (Framing.InitPacket)packet;

                switch (initPacket.cmd) {
                    case Init:
                        if (initPacket.totalLength != (short)Constants.INIT_CMD_NONCE_LENGTH) {
                            throw new CtapHidException(CtapHidException.CtapHidError.InvalidLen, packet.channelId);
                        }
                        if (message != null && (initPacket.channelId == message.channelId)) {
                            // INIT command used to resync on the active channel.
                            resetTransaction();
                        }
                        int newChannelId;
                        if (initPacket.channelId == (int)Constants.BROADCAST_CHANNEL_ID) {
                            newChannelId = freeChannelId;
                            freeChannelId++;
                            if (freeChannelId == Constants.BROADCAST_CHANNEL_ID) {
                                freeChannelId = 1;
                            }
                        } else {
                            newChannelId = packet.channelId;
                        }
                        submit.submit(new Framing.InitResponse(
                                packet.channelId,
                                Arrays.copyOfRange(packet.payload, 0, Constants.INIT_CMD_NONCE_LENGTH),
                                newChannelId).toRawReports());
                        return;
                    case Cancel:
                        if (message == null) return;
                        if (message.channelId == packet.channelId) {
                            if (activeCborJob != null) activeCborJob.cancel(true);
                            Log.i(TAG, "Cancelling current transaction");
                            authenticator.cancelBiometricPrompt();
                            activeCborJob = null;
                        }
                        // Spurious cancels are silently ignored.
                        return;
                    default:
                        if (packet.channelId == Constants.BROADCAST_CHANNEL_ID) {
                            // Only INIT messages are allowed on the broadcast channel.
                            throw new CtapHidException(CtapHidException.CtapHidError.InvalidCid, packet.channelId);
                        }
                        if (message == null) {
                            message = new Framing.InMessage(initPacket);
                        } else {
                            // Received a second INIT packet, either on the same or another
                            // channel as the first.
                            if (message.channelId == packet.channelId) {
                                throw new CtapHidException(
                                        CtapHidException.CtapHidError.InvalidSeq,
                                        packet.channelId
                                );
                            } else {
                                throw new CtapHidException(
                                        CtapHidException.CtapHidError.ChannelBusy,
                                        packet.channelId
                                );
                            }
                        }
                }
            } else if (packet instanceof Framing.ContPacket) {
                if (packet.channelId == Constants.BROADCAST_CHANNEL_ID) {
                    // Only INIT messages are allowed on the broadcast channel.
                    throw new CtapHidException(CtapHidException.CtapHidError.InvalidCid, packet.channelId);
                }
                if (message != null && (!message.append((Framing.ContPacket)packet))) {
                    // Spurious continuation packets are dropped without timeout renewal.
                    return;
                }
            }
            haltTimeout();
            if (!handleMessageIfComplete(submit)) {
                startTimeout(submit);
            }
        } catch (CtapHidException hidException) {
            handleError(hidException, submit);
        }
    }

    public void updateActivity(FragmentActivity newActivity) {
        activity = newActivity;
    }

    public void registerListener(Framing.WebAuthnListener listener) {
        webAuthnListener = listener;
    }

    public void registerListener(Framing.U2fAuthnListener listener) {
        u2fAuthnListener = listener;
    }

    private byte[] handleCTAP2(FragmentActivity activity, byte[] rawRequest) {
//        context.status = AuthenticatorStatus.PROCESSING
        Messages.RequestCommandCTAP2 command = null;
        byte[] cborAnswer = new byte[0];
        authenticator.processing();
        try {
            if (rawRequest.length == 0)
                throw new CtapException(CtapException.CtapError.INVALID_LENGTH, "Empty CBOR request");
            if (rawRequest.length > 1 + Messages.MAX_CBOR_MSG_SIZE)
                throw new CtapException(CtapException.CtapError.REQUEST_TOO_LARGE, "CBOR request exceeds maximal size: ${rawRequest.size}");

            List <DataItem> requestMap = new CborDecoder(new ByteArrayInputStream(rawRequest)).decode();
            Map params;

            byte rawCommand = ((UnsignedInteger)requestMap.get(0)).getValue().byteValue();

            command = Messages.RequestCommandCTAP2.fromByte(rawCommand);
            if (command == null) {
                throw new CtapException(CtapException.CtapError.INVALID_COMMAND, "Unsupported command: $rawCommand");
            }



            switch (command) {
                case MakeCredential:
                    Log.i(TAG, "MakeCredential called");
                    try {
                        params = (Map)requestMap.get(1);
                    } catch (Exception e) {
                        throw new CtapException(CtapException.CtapError.INVALID_CBOR, "Invalid CBOR in MakeCredential request");
                    }
                    cborAnswer = authenticator.makeCredential(params, activity).asCBOR();
                    break;
                case GetAssertion:
                    Log.i(TAG, "GetAssertion called");
                    
                    try {
                        params = (Map) requestMap.get(1);
                    } catch (Exception e) {
                        throw new CtapException(CtapException.CtapError.INVALID_CBOR, "Invalid CBOR in GetAssertion request");
                    }

                    SelectCredentialDialogFragment credentialSelector = new SelectCredentialDialogFragment();
                    credentialSelector.populateFragmentActivity(activity);

                    cborAnswer = authenticator.getAssertion(params, credentialSelector, activity).asCBOR();
                    break;
                case GetNextAssertion:
                    Log.i(TAG, "GetNextAssertion called");
                    if (rawRequest.length != 1)
                        throw new CtapException(CtapException.CtapError.INVALID_LENGTH, "Non-empty params for GetNextAssertion");
                    cborAnswer = new byte[1];
                    break;
                case GetInfo:
                    Log.i(TAG, "GetInfo called");

                    if (rawRequest.length != 1)
                        throw new CtapException(CtapException.CtapError.INVALID_LENGTH, "Non-empty params for GetInfo");
                    cborAnswer = authenticator.getInfo().asCBOR();
                    break;
                case ClientPIN:
                    Log.i(TAG, "ClientPIN called");
                    try {
                        params = (Map)requestMap.get(1);
                    } catch (Exception e) {
                        throw new CtapException(CtapException.CtapError.INVALID_CBOR, "Invalid CBOR in MakeCredential request");
                    }
                    cborAnswer = authenticator.getPinResult(params, activity).asCBOR();
                    break;
                case Reset:
                    Log.i(TAG, "Reset called");
                    if (rawRequest.length != 1)
                        throw new CtapException(CtapException.CtapError.INVALID_LENGTH, "Non-empty params for Reset");
                    authenticator.resetAuthenticator(activity);
                    cborAnswer = new byte[1];
                    break;
                case Selection:
                    Log.i(TAG, "Selection called");
                    if (rawRequest.length != 1)
                        throw new CtapException(CtapException.CtapError.INVALID_LENGTH, "Non-empty params for Selection");
                    cborAnswer = new byte[1];
                    break;
            }
        } catch (CtapException e) {
            e.printStackTrace();
            //showToast(activity, "Error during operation!", Toast.LENGTH_LONG)
            cborAnswer = new byte[] {e.getErrorCode()};
        } catch (VirgilException e) {
            e.printStackTrace();
            //TODO: Add toast
            //showToast(activity, "Error during operation!", Toast.LENGTH_LONG)
            cborAnswer = new byte[] {CtapException.CtapError.OTHER.value};
        } catch (CborException e) {
            e.printStackTrace();
            cborAnswer = new byte[] {CtapException.CtapError.INVALID_CBOR.value};
        } finally {
            authenticator.idle();
        }

        // At this point call the callbacks async if we handled a credential or assertion
        if (command != null && webAuthnListener!= null) {
            switch (command) {
                case MakeCredential:
                    webAuthnListener.onCompleteMakeCredential();
                    break;
                case GetAssertion:
                    webAuthnListener.onCompleteGetAssertion();
                    break;
                case GetNextAssertion:
                    break;
                case GetInfo:
                    break;
                case ClientPIN:
                    break;
                case Reset:
                    break;
                case Selection:
                    break;
                default:
                    // Do nothing
                    break;
            }
        }

        return cborAnswer;
    }

    private RawMessages.Response handleU2F(FragmentActivity activity, RawMessages.RequestU2F req) throws ApduException {
        RawMessages.Response response;
        if (req instanceof RawMessages.RegistrationRequest) {
            Log.i(TAG, "Register request received");
            response = authenticator.registerRequest(
                    (RawMessages.RegistrationRequest)req, activity);
        } else if (req instanceof RawMessages.AuthenticationRequest) {
            Log.i(TAG, "Authenticate request received");
            response = authenticator.authenticationRequest(
                    (RawMessages.AuthenticationRequest)req, activity);
        } else if (req instanceof RawMessages.VersionRequest) {
            Log.i(TAG, "Version request received");
            response = new RawMessages.VersionResponse();
        } else {
            //TODO: handle this better
            response = new RawMessages.VersionResponse();
        }

        return response;
    }

}