package net.tjado.webauthn.fido;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import net.tjado.webauthn.exceptions.ApduException;

public class ResponseApdu {

    private final byte[] data;
    private final ApduException.StatusWord statusWord;
    private int pos = 0;

    public ResponseApdu(byte[] data, ApduException.StatusWord statusWord) {
        this.data = data;
        this.statusWord = statusWord;
    }

    public boolean hasNext() {
        return getRemainingBytes() > 0;
    }

    public byte[] next(int expectedLength) throws ApduException {
        int nextResponseLength = Math.min(expectedLength, getRemainingBytes());
        byte[] nextResponse = Arrays.copyOfRange(data, pos, pos + nextResponseLength);
        pos += nextResponseLength;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(nextResponse);
            if (hasNext()) {
                baos.write((byte)0x61);
                baos.write((getRemainingBytes() > 256) ? 0x00 : (byte)getRemainingBytes());
            } else {
                baos.write(statusWord.value);
            }
        } catch (Exception e) {
            throw new ApduException(ApduException.StatusWord.MEMORY_FAILURE);
        }

        return baos.toByteArray();
    }

    public byte[] next() throws ApduException {
        return next(getRemainingBytes());
    }

    private int getRemainingBytes() {
        return Math.max(data.length - pos, 0);
    }
}
