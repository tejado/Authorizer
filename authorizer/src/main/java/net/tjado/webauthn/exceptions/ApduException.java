package net.tjado.webauthn.exceptions;

import android.util.Log;

import java.util.Arrays;

public class ApduException extends Exception {

    private static final String TAG = "ApduException";

    public enum StatusWord {
        NO_ERROR(new byte[]{(byte)0x90, (byte)0x00}),
        MEMORY_FAILURE(new byte[]{(byte)0x65, (byte)0x01}),
        CONDITIONS_NOT_SATISFIED(new byte[]{(byte)0x69, (byte)0x85}),
        WRONG_DATA(new byte[]{(byte)0x6A, (byte)0x80}),
        WRONG_LENGTH(new byte[]{(byte)0x67, (byte)0x00}),
        CLA_NOT_SUPPORTED(new byte[]{(byte)0x6E, (byte)0x00}),
        INS_NOT_SUPPORTED(new byte[]{(byte)0x6D, (byte)0x00}),
        INCORRECT_PARAMETERS(new byte[]{(byte)0x6A, (byte)0x00}),
        COMMAND_ABORTED(new byte[]{(byte)0x6F, (byte)0x00});

        public final byte[] value;
        StatusWord(byte[] value) { this.value = value; }
    }

    final private StatusWord statusWord;

    public ApduException(StatusWord statusWord) {
        super();
        Log.e(TAG, "Returning " + Arrays.toString(statusWord.value));
        this.statusWord = statusWord;
    }

    public byte[] getStatusWord() { return statusWord.value; }
}
