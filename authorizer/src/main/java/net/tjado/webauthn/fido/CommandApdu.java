package net.tjado.webauthn.fido;

import java.util.Arrays;

import net.tjado.webauthn.exceptions.ApduException;

public class CommandApdu {
    public final byte cla;
    public final byte ins;
    public final byte p1;
    public final byte p2;
    public final int le;
    public final byte[] data;

    public CommandApdu(byte[] bytes) throws ApduException {
        if (bytes.length < 4) {
            throw new ApduException(ApduException.StatusWord.WRONG_LENGTH);
        }
        cla = bytes[0];
        ins = bytes[1];
        p1 = bytes[2];
        p2 = bytes[3];

        int bodyLength = bytes.length - 4;
        if (bodyLength == 0) {
            // Case 1
            data = new byte[0];
            le = 0;
        } else if (bodyLength == 1) {
            // Case 2S
            data = new byte[0];
            int leRaw = bytes[4];
            le = (leRaw != 0) ? leRaw : 256;
        } else if (bodyLength == 1 + bytes[4]) {
            // Case 3S (bytes[4] != 0 implicit)
            data = Arrays.copyOfRange(bytes, 5, bytes.length);
            le = 0;
        } else if (bodyLength == 2 + bytes[4]) {
            // Case 4S (bytes[4] != 0 implicit)
            data = Arrays.copyOfRange(bytes, 5, bytes.length - 1);
            int leRaw = bytes[bytes.length - 1];
            le = (leRaw != 0) ? leRaw : 256;
        } else if (bodyLength == 3 && bytes[4] == 0) {
            // Case 2E
            data = new byte[0];
            int leRaw = (((int)bytes[5]) << 8) + (int)bytes[6];
            le = (leRaw != 0) ? leRaw : 65536;
        } else if (bodyLength > 3 &&
                bodyLength == 3 + ((int)bytes[5] << 8) + (int)bytes[6] &&
                bytes[4] == 0) {
            // Case 3E
            data = Arrays.copyOfRange(bytes, 7, bytes.length);
            le = 0;
        } else if (bodyLength > 5 &&
                bodyLength == 5 + ((int)bytes[5] << 8) + (int)bytes[6] &&
                bytes[4] == 0) {
            // Case 4E
            data = Arrays.copyOfRange(bytes, 7, bytes.length -2);
            int leRaw = ((int)bytes[bytes.length - 2] << 8) + (int)bytes[bytes.length - 1];
            le = (leRaw != 0) ? leRaw : 65536;
        } else {
            throw new ApduException(ApduException.StatusWord.WRONG_LENGTH);
        }
    }

    boolean headerEquals(byte[] header) {
        return header.length == 4 && cla == header[0] && ins == header[1] && p1 == header[2] && p2 == header[3];
    }

    public int getLc() {
        return data.length;
    }
}