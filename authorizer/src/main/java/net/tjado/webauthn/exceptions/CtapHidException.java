package net.tjado.webauthn.exceptions;

import android.util.Log;

public class CtapHidException extends Exception {
    public enum CtapHidError {
        None(0),
        InvalidCmd(1),
        InvalidPar(2),
        InvalidLen(3),
        InvalidSeq(4),
        MsgTimeout(5),
        ChannelBusy(6),
        InvalidCid(11),
        Other(127);

        public final byte code;
        CtapHidError(byte code) { this.code = code; }
        CtapHidError(int code) { this((byte)code); }
    }

    final public CtapHidError error;
    final public Integer channelId;

    public CtapHidException(CtapHidError error) {
        super();
        Log.e("CtapHidException", "Returning " + error.code);
        this.error = error;
        this.channelId = null;
    }

    public CtapHidException(CtapHidError error, int channelId) {
        super();
        Log.e("CtapHidException", "Returning " + error.code + " on " + channelId);
        this.error = error;
        this.channelId = channelId;
    }
}
