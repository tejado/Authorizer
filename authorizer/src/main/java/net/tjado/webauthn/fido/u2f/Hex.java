package net.tjado.webauthn.fido.u2f;

public class Hex {
    private static final char[] zzgw = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] zzgx = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytesToStringUppercase(byte[] bArr) {
        return bytesToStringUppercase(bArr, false);
    }

    public static String bytesToStringUppercase(byte[] bArr, boolean z) {
        int length = bArr.length;
        StringBuilder stringBuilder = new StringBuilder(length << 1);
        int i = 0;
        while (i < length && (!z || i != length - 1 || (bArr[i] & 255) != 0)) {
            stringBuilder.append(zzgw[(bArr[i] & 240) >>> 4]);
            stringBuilder.append(zzgw[bArr[i] & 15]);
            i++;
        }
        return stringBuilder.toString();
    }

    public static byte[] stringToBytes(String str) throws IllegalArgumentException {
        int length = str.length();
        if (length % 2 == 0) {
            byte[] bArr = new byte[(length / 2)];
            int i = 0;
            while (i < length) {
                int i2 = i + 2;
                bArr[i / 2] = (byte) Integer.parseInt(str.substring(i, i2), 16);
                i = i2;
            }
            return bArr;
        }
        throw new IllegalArgumentException("Hex string has odd number of characters");
    }

    public static String zza(byte[] bArr) {
        int i = 0;
        char[] cArr = new char[(bArr.length << 1)];
        int i2 = 0;
        while (true) {
            int i3 = i;
            if (i3 >= bArr.length) {
                return new String(cArr);
            }
            i = bArr[i3] & 255;
            int i4 = i2 + 1;
            cArr[i2] = zzgx[i >>> 4];
            i2 = i4 + 1;
            cArr[i4] = zzgx[i & 15];
            i = i3 + 1;
        }
    }
}
