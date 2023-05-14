package net.tjado.webauthn.fido.hid;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.tjado.passwdsafe.lib.Utils;
import net.tjado.webauthn.exceptions.CtapHidException;
import net.tjado.webauthn.exceptions.CtapHidException.CtapHidError;
import net.tjado.webauthn.fido.hid.Constants.CtapHidCommand;
import net.tjado.webauthn.fido.hid.Constants.CtapHidStatus;

public class Framing {

    public abstract static class Packet {
        public final int channelId;
        public final byte[] payload;

        public Packet(int channelId, byte[] payload) {
            this.channelId = channelId;
            this.payload = payload;
        }

        abstract byte[] toRawReport();

        public static Packet parse(byte[] bytes) throws CtapHidException {
            Log.d("Framing", "Parsing Packet: " + Utils.bytesToHexString(bytes));

            int reportOffset;
            if (bytes.length == (Constants.HID_REPORT_SIZE) + 1) {
                reportOffset = 1; // Linux (hidraw) includes the report ID
            } else if (bytes.length == Constants.HID_REPORT_SIZE || bytes.length == Constants.HID_REPORT_SIZE - 2) {
                reportOffset = 0; // Windows, iOS and macOS does not include the report ID
            } else {
                throw new CtapHidException(CtapHidError.InvalidLen, bytes.length);
            }

            int channelId = bytes2int(bytes, reportOffset);
            if ((bytes[reportOffset + 4] & Constants.MESSAGE_TYPE_MASK) == Constants.MESSAGE_TYPE_INIT) {
                // Initialization packet
                Log.i("Framing", "Parsing Init packet");
                CtapHidCommand cmd;
                try {
                    cmd = CtapHidCommand.fromByte((byte)(bytes[reportOffset + 4] & (byte)Constants.COMMAND_MASK));
                } catch (Exception e) {
                    throw new CtapHidException(CtapHidError.InvalidCmd, channelId);
                }
                short totalLength = bytes2short(bytes, reportOffset + 4 + 1);
                if (totalLength > Constants.MAX_PAYLOAD_LENGTH) {
                    throw new CtapHidException(CtapHidError.InvalidLen, channelId);
                }
                byte[] data = Arrays.copyOfRange(bytes, reportOffset + 4 + 1 + 2, bytes.length);
                return new InitPacket(channelId, cmd, totalLength, data);
            } else {
                // Continuation packet
                Log.i("Framing", "Parsing Continuation packet");
                byte seq = bytes[reportOffset + 4];
                byte[] data = Arrays.copyOfRange(bytes, reportOffset + 4 + 1, bytes.length);
                return new ContPacket(channelId, seq, data);
            }
        }
    }

    public static class InitPacket extends Packet {
        public final CtapHidCommand cmd;
        public final short totalLength;

        InitPacket(int channelId, CtapHidCommand cmd, short totalLength, byte[] payload) {
            super(channelId, payload);
            this.cmd = cmd;
            this.totalLength = totalLength;
        }

        @Override
        byte[] toRawReport() {
            if (payload.length > Constants.INIT_PACKET_PAYLOAD_SIZE) {
                throw new IllegalStateException();
            }
            byte[] padding = new byte[Constants.INIT_PACKET_PAYLOAD_SIZE - payload.length];

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(int2bytes(channelId));
                baos.write(cmd.code | Constants.MESSAGE_TYPE_INIT);
                baos.write(short2bytes(totalLength));
                baos.write(payload);
                baos.write(padding);
            } catch (Exception ignore) {
                return Constants.CTAP2_WRITEOUT_ERROR;
            }

            return baos.toByteArray();
        }
    }

    public static class ContPacket extends Packet {
        public final byte seq;

        ContPacket(int channelId, byte seq, byte[] payload) {
            super(channelId, payload);
            this.seq = seq;
        }

        @Override
        byte[] toRawReport() {
            if (payload.length > Constants.CONT_PACKET_PAYLOAD_SIZE) {
                return null;
            }

            byte[] padding = new byte[Constants.CONT_PACKET_PAYLOAD_SIZE - payload.length];

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(int2bytes(channelId));
                baos.write(seq);
                baos.write(payload);
                baos.write(padding);
            } catch (Exception ignore) {
                return Constants.CTAP2_WRITEOUT_ERROR;
            }

            return baos.toByteArray();
        }
    }

    public static class InMessage {
        public final int channelId;
        public final CtapHidCommand cmd;

        private final short totalLength;
        private byte[] _payload;
        private byte seq = 0;

        public InMessage(InitPacket packet) {
            channelId = packet.channelId;
            cmd = packet.cmd;
            totalLength = packet.totalLength;
            _payload = packet.payload;
        }

        private boolean isComplete() {
            if (_payload == null) return false;
            return _payload.length >= totalLength;
        }
        public byte[] getPayloadIfComplete() {
            if (isComplete()) {
                if (_payload.length > totalLength) {
                    _payload = Arrays.copyOfRange(_payload, 0, totalLength);
                }
                return _payload;
            } else {
                return null;
            }
        }

        public boolean append(ContPacket packet) throws CtapHidException {
            if (packet.channelId != channelId) {
                // Spurious continuation packets are dropped without error.
                Log.d("Framing", "Spurious continuation packet dropped.");
                return false;
            }
            if (isComplete() || packet.seq != seq) {
                throw new CtapHidException(CtapHidError.InvalidSeq, channelId);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(_payload);
                baos.write(packet.payload);
            } catch (Exception e) {
                throw new CtapHidException(CtapHidError.Other, channelId);
            }
            _payload = baos.toByteArray();
            seq++;
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (this.getClass() != o.getClass()) return false;

            InMessage object = (InMessage)o;

            if (channelId != object.channelId) return false;
            if (cmd.code != object.cmd.code) return false;

            byte[] ourPayload = getPayloadIfComplete();
            byte[] otherPayload = object.getPayloadIfComplete();
            if (otherPayload == null && ourPayload == null) {
                return true;
            }

            return Arrays.equals(ourPayload, otherPayload);
        }

        @NotNull
        @Override
        public String toString() {
            return String.format(Locale.US, "InMessage(cid=%d, cmd=%d, totalLength=%d payload=%s",
                                 channelId, cmd.code, totalLength, Arrays.toString(_payload));
        }
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public static abstract class OutMessage {
        final int channelId;

        public abstract Iterable<byte[]> toRawReports();

        OutMessage(int channelId) {
            this.channelId = channelId;
        }

        Iterable<byte[]> serializeResponse(CtapHidCommand cmd, byte[] payload) {
            if (payload.length > Constants.MAX_PAYLOAD_LENGTH) {
                return null;
            }

            short totalLength = (short)payload.length;
            List<byte[]> rawReports = new ArrayList<>(Arrays.asList(
                    new InitPacket(channelId,
                                   cmd,
                                   totalLength,
                                   Arrays.copyOfRange(payload, 0, Math.min(Constants.INIT_PACKET_PAYLOAD_SIZE, payload.length)))
                            .toRawReport()
            ));

            int offset = Constants.INIT_PACKET_PAYLOAD_SIZE;
            byte seq = 0;
            while (offset < payload.length) {
                int nextOffset = Math.min(offset + Constants.CONT_PACKET_PAYLOAD_SIZE, payload.length);
                byte[] newReport = new ContPacket(channelId,
                                                  seq,
                                                  Arrays.copyOfRange(payload, offset, nextOffset)
                                                ).toRawReport();
                rawReports.add(newReport);
                offset = nextOffset;
                seq++;
            }
            return rawReports;
        }
    }

    public static class PingResponse extends OutMessage {
        final byte[] payload;

        public PingResponse(int channelId, byte[] payload) {
            super(channelId);
            this.payload = payload;
        }

        @Override
        public Iterable<byte[]> toRawReports() {
            return serializeResponse(CtapHidCommand.Ping, payload);
        }
    }

    public static class MsgResponse extends OutMessage {
        final byte[] payload;

        public MsgResponse(int channelId, byte[] payload) {
            super(channelId);
            this.payload = payload;
        }

        @Override
        public Iterable<byte[]> toRawReports() {
            return serializeResponse(CtapHidCommand.Msg, payload);
        }
    }

    public static class CborResponse extends OutMessage {
        final byte[] payload;

        public CborResponse(int channelId, byte[] payload) {
            super(channelId);
            this.payload = payload;
        }

        @Override
        public Iterable<byte[]> toRawReports() {
            return serializeResponse(CtapHidCommand.Cbor, payload);
        }
    }

    public static class InitResponse extends OutMessage {
        final int newChannelId;
        final byte[] nonce;

        public InitResponse(int chanelId, byte[] nonce, int newChannelId) {
            super(chanelId);
            this.newChannelId = newChannelId;
            this.nonce = nonce;
        }

        @Override
        public Iterable<byte[]> toRawReports() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] payload;
            try {
                baos.write(nonce);
                baos.write(int2bytes(newChannelId));
                baos.write(Constants.INIT_CMD_TRAILER);
                payload = baos.toByteArray();
            } catch (Exception e) {
                payload = Constants.CTAP2_WRITEOUT_ERROR;
            }

            return Arrays.asList(
                    new InitPacket(
                            channelId,
                            CtapHidCommand.Init,
                            (short)payload.length,
                            payload
                    ).toRawReport());
        }
    }

    public static class ErrorResponse extends OutMessage {
        final CtapHidError error;

        public ErrorResponse(int channelId, CtapHidError error) {
            super(channelId);
            this.error = error;
        }

        @Override
        public Iterable<byte[]> toRawReports() {
            InitPacket packet = new InitPacket(
                    channelId,
                    CtapHidCommand.Keepalive,
                    (short)1,
                    new byte[]{error.code}
            );
            byte[] report = packet.toRawReport();
            return Arrays.asList(report);
        }
    }

    public static class KeepaliveResponse extends OutMessage {
        final CtapHidStatus status;

        public KeepaliveResponse(int channelId, CtapHidStatus status) {
            super(channelId);
            this.status = status;
        }

        @Override
        public Iterable<byte[]> toRawReports() {
            return Arrays.asList(
                    new InitPacket(
                            channelId,
                            CtapHidCommand.Keepalive,
                            (short)1,
                            new byte[]{status.code}
                    ).toRawReport());
        }
    }

    public interface U2fAuthnListener {
        void onRegistrationResponse();
        void onAuthenticationResponse();
    }

    public interface WebAuthnListener {
        void onCompleteMakeCredential();
        void onCompleteGetAssertion();
    }

    public interface SubmitReports {
        void submit(Iterable<byte[]> rawReports);
    }

    static byte[] short2bytes(short value) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte)((value >> 8) & 0xFF);
        bytes[1] = (byte)(value & 0xFF);
        return bytes;
    }

    static byte[] int2bytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    static public int bytes2int (byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes).getInt(offset);
    }

    static public short bytes2short (byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes).getShort(offset);
    }

}
