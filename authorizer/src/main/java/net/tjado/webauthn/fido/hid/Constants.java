package net.tjado.webauthn.fido.hid;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import net.tjado.webauthn.exceptions.ApduException.StatusWord;
import net.tjado.webauthn.exceptions.CtapException;
import kotlin.UInt;

public class Constants {
    public static final int HID_REPORT_SIZE = 64;

    static final int INIT_PACKET_PAYLOAD_SIZE = HID_REPORT_SIZE - 7;
    static final int CONT_PACKET_PAYLOAD_SIZE = HID_REPORT_SIZE - 5;

    static final int MAX_PAYLOAD_LENGTH = (INIT_PACKET_PAYLOAD_SIZE + 128 * CONT_PACKET_PAYLOAD_SIZE);

    public static final int INIT_CMD_NONCE_LENGTH = 8;
    public static final byte CAPABILITY_WINK = 0x01;
    public static final byte CAPABILITY_CBOR = 0x04;
    public static final byte[] INIT_CMD_TRAILER = new byte[] {
            2,  // U2FHID protocol version
            0,  // Version - major
            0,  // Version - minor
            0,  // Version - build
            CAPABILITY_CBOR  // Capabilities flag
    };

    public static final byte BROADCAST_CHANNEL_ID = UInt.MAX_VALUE;
    public static final byte MESSAGE_TYPE_MASK = (byte)0x80;
    public static final byte MESSAGE_TYPE_INIT = (byte)0x80;

    public static final long HID_CONT_TIMEOUT_MS = 500L;
    public static final long HID_MSG_TIMEOUT_MS = 3000L;
    public static final long HID_USER_PRESENCE_TIMEOUT_MS = 60000L;
    public static final long HID_KEEPALIVE_INTERVAL_MS = 75L;

    static final long COMMAND_MASK = (byte)0x7F;

    public enum CtapHidCommand {
        Ping(0x01),
        Msg(0x03),
        Init(0x06),
        Cbor(0x10),
        Cancel(0x11),
        Keepalive(0x3b),
        Error(0x3f);

        public final byte code;
        private static final ImmutableMap<Byte, CtapHidCommand> REVERSE_MAP = Maps.uniqueIndex(
                Arrays.asList(CtapHidCommand.values()),
                CtapHidCommand::getCode
        );

        private byte getCode() {
            return this.code;
        }

        CtapHidCommand(byte code) {
            this.code = code;
        }
        CtapHidCommand(int code) {
            this((byte)code);
        }

        public static CtapHidCommand fromByte(byte code) {
            CtapHidCommand cmd = REVERSE_MAP.get(code);
            if (cmd == null) { throw new IndexOutOfBoundsException(); }
            return cmd;
        }

    }

    public enum CtapHidStatus{
        IDLE(0x0),
        PROCESSING(0x01),
        UPNEEDED(0x02);

        public final byte code;
        CtapHidStatus(byte code) { this.code = code; }
        CtapHidStatus(int code) { this((byte)code); }
    }

    static public final byte[] U2F_LEGACY_VERSION_COMMAND_APDU = new byte[] {0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    static public byte[] U2F_LEGACY_VERSION_RESPONSE;
    static public byte[] CTAP2_WRITEOUT_ERROR = new byte[] {CtapException.CtapError.OTHER.value};

    static {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write("U2F_V2".getBytes());
            baos.write(StatusWord.NO_ERROR.value);
        } catch (Exception ignored) {
            /* TODO: Handle this somehow */
        }

        U2F_LEGACY_VERSION_RESPONSE = baos.toByteArray();
    }
}
